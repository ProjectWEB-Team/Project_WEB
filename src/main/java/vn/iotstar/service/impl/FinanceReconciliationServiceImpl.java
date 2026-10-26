package vn.iotstar.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.finance.*;
import vn.iotstar.dto.realtime.NotificationEventResponse;
import vn.iotstar.entity.*;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.realtime.NotificationEventPublisher;
import vn.iotstar.repository.*;
import vn.iotstar.service.FinanceReconciliationService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class FinanceReconciliationServiceImpl implements FinanceReconciliationService {
    public static final String COD="COD_TRANSACTION";
    public static final String RECEIVER_FEE="RECEIVER_CASH_SHIPPING_FEE_PAYMENT";
    public static final String OTHER_FEE="SHIPPER_CASH_FEE_PAYMENT";
    private static final Set<String> FEE_TYPES=Set.of("SHIPPING_FEE","RETURN_FEE","FORWARD_FEE_ON_RETURN","WEIGHT_SURCHARGE");
    private final FinanceReconciliationRepository reconciliations;
    private final ShipperReconciliationItemRepository items;
    private final CodTransactionRepository cods;
    private final PaymentRepository payments;
    private final WalletRepository wallets;
    private final WalletTransactionRepository transactions;
    private final IncidentRepository incidents;
    private final ShipmentRepository shipments;
    private final UserRepository users;
    private final ShipmentEventRepository events;
    private final NotificationEventPublisher notifications;
    private final FinanceAccess access;
    private final Clock clock;

    public FinanceReconciliationServiceImpl(FinanceReconciliationRepository reconciliations, ShipperReconciliationItemRepository items,
            CodTransactionRepository cods, PaymentRepository payments, WalletRepository wallets, WalletTransactionRepository transactions,
            IncidentRepository incidents, ShipmentRepository shipments, UserRepository users, ShipmentEventRepository events,
            NotificationEventPublisher notifications, FinanceAccess access, Clock clock) {
        this.reconciliations=reconciliations; this.items=items; this.cods=cods; this.payments=payments;
        this.wallets=wallets; this.transactions=transactions; this.incidents=incidents; this.shipments=shipments;
        this.users=users; this.events=events; this.notifications=notifications; this.access=access; this.clock=clock;
    }

    @Override public ShipperReconciliation reconcile(ReconciliationSubmissionRequest request) {
        FinanceAccess.Scope scope=access.current();
        if (request==null || request.getShipperId()==null || request.getItems()==null || request.getItems().isEmpty() || request.getItems().size()>100)
            throw new IllegalArgumentException("Select between 1 and 100 authoritative shift items");
        EmployeeProfile shipper=access.shipper(scope,request.getShipperId());
        LocalDate date=date(request.getReconciliationDate());
        String method=method(request.getCollectionMethod());
        String evidence=request.getSettlementEvidence();
        if ((evidence!=null && evidence.length()>255) || (!"CASH".equals(method) && (evidence==null || evidence.isBlank())))
            throw new IllegalArgumentException("Record verified settlement evidence for electronic COD");
        Set<String> keys=new HashSet<>();
        for (var row:request.getItems()) {
            if (row==null || row.getSourceId()==null || row.getSourceId()<=0 || row.getShipmentId()==null || row.getShipmentId()<=0 || row.getSourceType()==null || !Set.of(COD,RECEIVER_FEE,OTHER_FEE).contains(row.getSourceType())
                    || row.getActualAmount()==null || row.getActualAmount().signum()<0 || (row.getNote()!=null && row.getNote().length()>255)
                    || !keys.add(row.getSourceType()+":"+row.getSourceId())) throw new IllegalArgumentException("Invalid or duplicate reconciliation item");
        }
        // Stable shipment-first ordering agrees with collection, refund and loss settlement.
        request.getItems().stream().map(ReconciliationSubmissionRequest.ItemRequest::getShipmentId).distinct().sorted()
                .forEach(id -> shipments.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Shipment")));
        List<Source> sources=new ArrayList<>();
        for (var row:request.getItems().stream().sorted(Comparator.comparing(ReconciliationSubmissionRequest.ItemRequest::getSourceType)
                .thenComparing(ReconciliationSubmissionRequest.ItemRequest::getSourceId)).toList()) {
            if (items.findBySourceTypeAndSourceId(row.getSourceType(),row.getSourceId()).isPresent())
                throw new IllegalStateException("Source is already reconciled");
            Source source;
            if (COD.equals(row.getSourceType())) {
                CodTransaction cod=cods.findByIdForUpdate(row.getSourceId()).orElseThrow(() -> new ResourceNotFoundException("COD source"));
                validateCod(cod,shipper.getUser().getId(),date,method);
                source=new Source(row,cod.getShipment(),cod.getAmount(),cod);
            } else {
                if (!"CASH".equals(method)) throw new IllegalArgumentException("Electronic reconciliation accepts COD sources only");
                Payment payment=payments.findByIdForUpdate(row.getSourceId()).orElseThrow(() -> new ResourceNotFoundException("Payment source"));
                validatePayment(payment,shipper.getUser().getId(),date);
                if (!paymentSourceType(payment).equals(row.getSourceType())) throw new IllegalArgumentException("Payment source type does not match");
                source=new Source(row,payment.getShipment(),payment.getAmount(),null);
            }
            if (!Objects.equals(source.shipment().getId(),row.getShipmentId())
                    || (row.getExpectedAmount()!=null && row.getExpectedAmount().compareTo(source.expected())!=0))
                throw new IllegalArgumentException("Submitted shipment or expected amount does not match the source");
            sources.add(source);
        }
        BigDecimal expected=sources.stream().map(Source::expected).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal actual=sources.stream().map(s -> s.row().getActualAmount()).reduce(BigDecimal.ZERO,BigDecimal::add);
        User manager=users.getReferenceById(scope.actor().userId());
        ShipperReconciliation reconciliation=ShipperReconciliation.create(shipper.getUser(),manager,date,expected,actual);
        reconciliation.assignHub(shipper.getHubId());
        reconciliation.recordSettlement(method,evidence);
        reconciliation=reconciliations.save(reconciliation);
        boolean discrepancy=false;
        for (Source source:sources) {
            var row=source.row();
            ShipperReconciliationItem item=ShipperReconciliationItem.create(reconciliation,source.shipment(),row.getSourceType(),row.getSourceId(),
                    source.expected(),row.getActualAmount(),row.getNote());
            items.save(item);
            boolean matched="MATCHED".equals(item.getStatus());
            discrepancy |= !matched;
            if (source.cod()!=null) {
                source.cod().setStatus(matched ? "READY_FOR_PAYOUT" : "HOLD_DISPUTE");
                source.cod().setReconciledAt(ServerTime.now(clock));
            }
            if (!matched) incidents.save(Incident.create(source.shipment().getId(),null,shipper.getHubId(),
                    "SHORTAGE".equals(item.getStatus()) ? "MONEY_SHORTAGE" : "MONEY_OVERAGE",
                    "Reconciliation item " + row.getSourceType()+":"+row.getSourceId()+" requires review",manager.getId()));
            events.save(ShipmentEvent.of(source.shipment(),manager,
                    source.cod()!=null ? (matched ? "COD_RECONCILED" : "COD_DISPUTE") : (matched ? "SHIPPING_FEE_RECONCILED" : "FEE_DISPUTE"),
                    matched ? "Collected source reconciled" : "Collected source discrepancy requires review"));
        }
        reconciliation.complete(discrepancy);
        return reconciliation;
    }

    private String method(String method) {
        if (method==null || !Set.of("CASH","VNPAY","TRANSFER").contains(method)) throw new IllegalArgumentException("Unsupported collection method");
        return method;
    }
    private void validateCod(CodTransaction cod,Long shipperId,LocalDate date,String method) {
        if (cod.getCollectedByShipper()==null || !Objects.equals(cod.getCollectedByShipper().getId(),shipperId)
                || !method.equals(cod.getMethod()) || !"COLLECTED".equals(cod.getStatus()) || cod.getCollectedAt()==null
                || !date.equals(cod.getCollectedAt().toLocalDate()) || cod.getAmount()==null || cod.getAmount().signum()<=0
                || cod.getShipment().getCurrentStatus()!=ShipmentStatus.DELIVERED
                || cod.getShipment().getCodAmount()==null || cod.getAmount().compareTo(cod.getShipment().getCodAmount())!=0)
            throw new IllegalArgumentException("COD is not an eligible collected source for this shipper, method and date");
    }
    private void validatePayment(Payment payment,Long shipperId,LocalDate date) {
        if (!Objects.equals(payment.getCollectedByUserId(),shipperId) || !"CASH".equals(payment.getMethod())
                || !"PAID".equals(payment.getStatus()) || payment.getPaidAt()==null || !date.equals(payment.getPaidAt().toLocalDate())
                || !FEE_TYPES.contains(payment.getPaymentType()) || !Set.of("SENDER","RECEIVER").contains(payment.getPayerType())
                || (!"SHIPPING_FEE".equals(payment.getPaymentType()) && !"SENDER".equals(payment.getPayerType()))
                || payment.getAmount()==null || payment.getAmount().signum()<=0 || payment.getShipment().getCurrentStatus()==ShipmentStatus.CANCELLED)
            throw new IllegalArgumentException("Payment is not an eligible collected cash source for this shipper and date");
    }
    private String paymentSourceType(Payment payment) {
        return "SHIPPING_FEE".equals(payment.getPaymentType()) && "RECEIVER".equals(payment.getPayerType()) ? RECEIVER_FEE : OTHER_FEE;
    }
    private record Source(ReconciliationSubmissionRequest.ItemRequest row,Shipment shipment,BigDecimal expected,CodTransaction cod) { }
    private LocalDate date(LocalDate supplied) { return supplied==null ? ServerTime.now(clock).toLocalDate() : supplied; }

    @Override public ShipperReconciliationItem payoutReadyCod(Long id) {
        FinanceAccess.Scope scope=access.current();
        CodTransaction reference=cods.findById(id).orElseThrow(() -> new ResourceNotFoundException("COD source"));
        Shipment shipment=shipments.findByIdForUpdate(reference.getShipment().getId()).orElseThrow(() -> new ResourceNotFoundException("Shipment"));
        CodTransaction cod=cods.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("COD source"));
        ShipperReconciliationItem item=items.findBySourceTypeAndSourceId(COD,id).orElseThrow(() -> new IllegalStateException("No reconciled source"));
        scope.requireHub(item.getReconciliation().getHubId());
        var header=item.getReconciliation();
        if (!Objects.equals(header.getCollectionMethod(),cod.getMethod()) || (!"CASH".equals(cod.getMethod())
                && (header.getSettlementEvidence()==null || header.getSettlementEvidence().isBlank())))
            throw new IllegalStateException("COD has no matching verified settlement record");
        if (!"MATCHED".equals(item.getStatus()) || !Objects.equals(item.getShipment().getId(),shipment.getId())
                || !Objects.equals(cod.getShipment().getId(),shipment.getId())
                || !Objects.equals(item.getReconciliation().getShipper().getId(),cod.getCollectedByShipper().getId())
                || cod.getAmount().compareTo(item.getExpectedAmount())!=0
                || cod.getAmount().compareTo(item.getActualAmount())!=0 || cod.getAmount().signum()<=0)
            throw new IllegalStateException("COD payout does not match its reconciled source");
        var existing=transactions.findFirstByTransactionTypeAndReferenceTypeAndReferenceId("COD_PAYOUT","SHIPMENT",shipment.getId());
        if ("PAID_TO_SENDER".equals(cod.getStatus())) {
            WalletTransaction tx=existing.orElseThrow(() -> new IllegalStateException("Payout has no wallet evidence"));
            if (!Objects.equals(tx.getUserId(),shipment.getSender().getId()) || tx.getAmount().compareTo(cod.getAmount())!=0)
                throw new IllegalStateException("Payout wallet evidence does not match");
            return item;
        }
        if (!"READY_FOR_PAYOUT".equals(cod.getStatus()) || existing.isPresent()) throw new IllegalStateException("COD is not ready for a new payout");
        Wallet wallet=wallets.findByUserIdForUpdate(shipment.getSender().getId()).orElseGet(() -> wallets.save(Wallet.createForUser(shipment.getSender().getId())));
        BigDecimal before=wallet.getBalance(); wallet.credit(cod.getAmount());
        transactions.save(WalletTransaction.create(shipment.getSender().getId(),"COD_PAYOUT",cod.getAmount(),before,wallet.getBalance(),shipment.getId(),"SHIPMENT","Reconciled COD payout"));
        cod.setStatus("PAID_TO_SENDER"); cod.setPayoutAt(ServerTime.now(clock));
        events.save(ShipmentEvent.of(shipment,users.getReferenceById(scope.actor().userId()),"COD_PAYOUT_COMPLETED","Reconciled COD paid to sender"));
        notifications.publishToUserAfterCommit(shipment.getSender().getUsername(),new NotificationEventResponse("COD_PAYOUT_COMPLETED","SHIPMENT#"+shipment.getId(),"Your COD payout is complete"));
        return item;
    }

    @Override @Transactional(readOnly=true) public List<ReconciliationSummaryResponse> getReconciliationsByShipper(Long shipperId) {
        FinanceAccess.Scope scope=access.current(); access.shipper(scope,shipperId);
        return reconciliations.findTop100ByShipperIdOrderByIdDesc(shipperId).stream().filter(r -> scope.contains(r.getHubId()))
                .map(r -> new ReconciliationSummaryResponse(r.getId(),r.getReconciliationDate(),r.getExpectedAmount(),r.getActualAmount(),r.getDifferenceAmount(),r.getStatus(),r.getCollectionMethod())).toList();
    }
    @Override @Transactional(readOnly=true) public List<ReconciliationItemResponse> getReconciliationItems(Long id) {
        FinanceAccess.Scope scope=access.current();
        ShipperReconciliation reconciliation=reconciliations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reconciliation"));
        scope.requireHub(reconciliation.getHubId());
        return items.findTop100ByReconciliationIdOrderByIdAsc(id).stream().map(i -> new ReconciliationItemResponse(i.getShipment().getId(),
                i.getShipment().getTrackingNumber(),i.getSourceType(),i.getSourceId(),i.getExpectedAmount(),i.getActualAmount(),i.getDifferenceAmount(),i.getStatus())).toList();
    }
    @Override @Transactional(readOnly=true) public List<ShiftExpectedItemResponse> getShiftExpectedItems(Long shipperId,LocalDate suppliedDate) {
        return getShiftExpectedItems(shipperId,suppliedDate,"CASH");
    }
    @Override @Transactional(readOnly=true) public List<ShiftExpectedItemResponse> getShiftExpectedItems(Long shipperId,LocalDate suppliedDate,String collectionMethod) {
        FinanceAccess.Scope scope=access.current(); access.shipper(scope,shipperId);
        LocalDate date=date(suppliedDate);
        String method=method(collectionMethod);
        List<ShiftExpectedItemResponse> result=new ArrayList<>();
        for (CodTransaction cod:cods.findByCollectedByShipperIdAndStatus(shipperId,"COLLECTED")) {
            if (!method.equals(cod.getMethod()) || cod.getCollectedAt()==null || !date.equals(cod.getCollectedAt().toLocalDate())
                    || cod.getShipment().getCurrentStatus()!=ShipmentStatus.DELIVERED || items.findBySourceTypeAndSourceId(COD,cod.getId()).isPresent()) continue;
            validateCod(cod,shipperId,date,method);
            result.add(response(cod.getShipment(),COD,cod.getId(),cod.getAmount(),cod.getStatus()));
        }
        if ("CASH".equals(method)) for (Payment payment:payments.findCashShiftPayments(shipperId,date.atStartOfDay(),date.plusDays(1).atStartOfDay())) {
            if (!FEE_TYPES.contains(payment.getPaymentType()) || payment.getShipment().getCurrentStatus()==ShipmentStatus.CANCELLED
                    || items.findBySourceTypeAndSourceId(paymentSourceType(payment),payment.getId()).isPresent()) continue;
            validatePayment(payment,shipperId,date);
            result.add(response(payment.getShipment(),paymentSourceType(payment),payment.getId(),payment.getAmount(),payment.getStatus()));
        }
        return result.stream().limit(100).toList();
    }
    @Override @Transactional(readOnly=true) public List<ShiftExpectedItemResponse> getReadyPayouts(Long shipperId) {
        FinanceAccess.Scope scope=access.current(); access.shipper(scope,shipperId);
        return cods.findByCollectedByShipperIdAndStatus(shipperId,"READY_FOR_PAYOUT").stream()
                .filter(c -> items.findBySourceTypeAndSourceId(COD,c.getId()).filter(i -> "MATCHED".equals(i.getStatus()) && scope.contains(i.getReconciliation().getHubId())).isPresent())
                .limit(100).map(c -> response(c.getShipment(),COD,c.getId(),c.getAmount(),c.getStatus())).toList();
    }
    private ShiftExpectedItemResponse response(Shipment s,String type,Long id,BigDecimal amount,String status) {
        return new ShiftExpectedItemResponse(s.getId(),s.getTrackingNumber(),type,id,amount,status,"Recorded collection");
    }
}
