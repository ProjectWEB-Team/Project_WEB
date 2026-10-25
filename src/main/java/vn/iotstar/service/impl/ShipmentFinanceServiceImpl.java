package vn.iotstar.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.finance.LossCompensationCandidate;
import vn.iotstar.dto.finance.RefundCandidateResponse;
import vn.iotstar.dto.realtime.NotificationEventResponse;
import vn.iotstar.entity.*;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.realtime.NotificationEventPublisher;
import vn.iotstar.repository.*;
import vn.iotstar.service.ShipmentFinanceService;
import vn.iotstar.service.WalletLedgerService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional
public class ShipmentFinanceServiceImpl implements ShipmentFinanceService {
    private static final Set<String> REFUNDABLE=Set.of("SHIPPING_FEE", "WEIGHT_SURCHARGE");
    private final PaymentRepository payments;
    private final ShipmentRepository shipments;
    private final IncidentRepository incidents;
    private final ShipmentEventRepository events;
    private final UserRepository users;
    private final WalletLedgerService ledger;
    private final NotificationEventPublisher notifications;
    private final RefundSettlementOutboxRepository outbox;
    private final WalletTransactionRepository transactions;
    private final FinanceAccess access;
    private final Clock clock;

    public ShipmentFinanceServiceImpl(PaymentRepository payments, ShipmentRepository shipments, IncidentRepository incidents,
            ShipmentEventRepository events, UserRepository users, WalletLedgerService ledger,
            NotificationEventPublisher notifications, RefundSettlementOutboxRepository outbox,
            WalletTransactionRepository transactions, FinanceAccess access, Clock clock) {
        this.payments=payments; this.shipments=shipments; this.incidents=incidents; this.events=events;
        this.users=users; this.ledger=ledger; this.notifications=notifications; this.outbox=outbox;
        this.transactions=transactions; this.access=access; this.clock=clock;
    }

    @Override public Payment refundShipmentPayment(Long paymentId, String reason) { return refund(paymentId,access.current()); }
    @Override public void settlePendingRefund(Long paymentId) { refund(paymentId,null); }

    /** Same lock order as fee/cancellation: shipment -> payment -> obligation -> wallet. */
    private Payment refund(Long paymentId, FinanceAccess.Scope scope) {
        Payment reference=payments.findById(paymentId).orElseThrow(() -> new ResourceNotFoundException("Payment"));
        Shipment shipment=shipments.findByIdForUpdate(reference.getShipment().getId()).orElseThrow(() -> new ResourceNotFoundException("Shipment"));
        if (scope!=null) scope.requireHub(shipment.getOriginHubId());
        Payment payment=payments.findByIdForUpdate(paymentId).orElseThrow(() -> new ResourceNotFoundException("Payment"));
        RefundSettlementOutbox obligation=outbox.findByPaymentIdForUpdate(paymentId).orElseThrow(() -> new IllegalStateException("No persisted refund obligation"));
        if (!Objects.equals(payment.getShipment().getId(),shipment.getId()) || shipment.getCurrentStatus()!=ShipmentStatus.CANCELLED
                || shipment.getPickedUpAt()!=null || shipment.getCurrentHubId()!=null
                || !"SENDER".equals(payment.getPayerType()) || !REFUNDABLE.contains(payment.getPaymentType())
                || !Objects.equals(obligation.getShipmentId(),shipment.getId())
                || !Objects.equals(obligation.getSenderUserId(),shipment.getSender().getId())
                || !Objects.equals(obligation.getCancelReason(),shipment.getCancelReason())
                || payment.getAmount()==null || payment.getAmount().signum()<=0
                || obligation.getAmount()==null || obligation.getAmount().compareTo(payment.getAmount())!=0
                || !Set.of("PENDING_WALLET_CREDIT","SETTLED").contains(obligation.getStatus()))
            throw new IllegalStateException("Refund obligation does not match the cancelled shipment and payment");
        if ("REFUNDED".equals(payment.getStatus())) {
            requireCredit("REFUND","PAYMENT",payment.getId(),shipment.getSender().getId(),payment.getAmount());
            obligation.settle(ServerTime.now(clock));
            return payment;
        }
        if (!"PAID".equals(payment.getStatus()) || payment.getPaidAt()==null || "SETTLED".equals(obligation.getStatus()))
            throw new IllegalStateException("Only an actually paid, unsettled obligation can be refunded");
        ledger.creditRefund(shipment.getSender().getId(),payment.getAmount(),payment.getId(),shipment.getId(),"Cancelled shipment fee refund");
        payment.setStatus("REFUNDED"); payment.setRefundedAt(ServerTime.now(clock));
        if ("SHIPPING_FEE".equals(payment.getPaymentType())) shipment.setPaymentStatus("REFUNDED");
        obligation.settle(ServerTime.now(clock));
        User actor=scope==null ? null : users.getReferenceById(scope.actor().userId());
        events.save(ShipmentEvent.of(shipment,actor,"REFUND_COMPLETED","Paid fee refunded to the sender wallet"));
        notifyOwner(shipment,"REFUND_COMPLETED","PAYMENT#"+payment.getId(),"A shipment fee refund is complete");
        return payment;
    }

    @Override public Shipment compensateLostShipment(Long shipmentId, Long incidentId, String reason) {
        FinanceAccess.Scope scope=access.current();
        if (reason==null || reason.isBlank() || reason.length()>255) throw new IllegalArgumentException("A bounded decision reason is required");
        Shipment shipment=shipments.findByIdForUpdate(shipmentId).orElseThrow(() -> new ResourceNotFoundException("Shipment"));
        Incident incident=incidents.findByIdForUpdate(incidentId).orElseThrow(() -> new ResourceNotFoundException("Incident"));
        scope.requireHub(incident.getHubId());
        if (!Objects.equals(incident.getShipmentId(),shipmentId) || !"MISSING_PARCEL".equals(incident.getType())
                || !"CONFIRMED_LOST".equals(incident.getResolutionOutcome()) || incident.getTripId()==null
                || incident.getInvestigatedBy()==null || incident.getInvestigatedAt()==null
                || incident.getResolutionEvidence()==null || incident.getResolutionEvidence().isBlank()
                || incidents.countMissingCustody(shipmentId,incident.getTripId(),incident.getHubId())!=1)
            throw new IllegalStateException("A matching confirmed-loss investigation and missing custody are required");
        BigDecimal declared=shipment.getDeclaredValue()==null ? BigDecimal.ZERO : shipment.getDeclaredValue();
        if (declared.signum()<0) throw new IllegalStateException("Invalid declared value");
        if (shipment.getCurrentStatus()==ShipmentStatus.CLOSED_EXCEPTION && "LOST".equals(shipment.getClosureReason())) {
            if (!"RESOLVED".equals(incident.getStatus()) || incident.getCompensatedAt()==null)
                throw new IllegalStateException("Loss was closed through another incident");
            if (declared.signum()>0) requireCredit("COMPENSATION","INCIDENT",incidentId,shipment.getSender().getId(),declared);
            return shipment;
        }
        if (!"INVESTIGATING".equals(incident.getStatus()) || !shipment.isOnHold() || shipment.getCurrentHubId()!=null
                || !(shipment.getCurrentStatus()==ShipmentStatus.IN_TRANSIT || shipment.getCurrentStatus()==ShipmentStatus.RETURN_TO_SENDER))
            throw new IllegalStateException("Shipment is not held for the confirmed missing-parcel outcome");
        List<Payment> feePayments=payments.findByShipmentIdForUpdate(shipmentId).stream().filter(this::paidSenderForwardFee).toList();
        // Zero declared value and no prepaid sender fee still need a terminal loss decision; never invent a credit.
        if (declared.signum()>0) ledger.creditCompensation(shipment.getSender().getId(),declared,incidentId,"Confirmed parcel loss");
        for (Payment payment:feePayments) {
            ledger.creditRefund(shipment.getSender().getId(),payment.getAmount(),payment.getId(),shipmentId,"Paid forward fee refund for confirmed loss");
            payment.setStatus("REFUNDED"); payment.setRefundedAt(ServerTime.now(clock));
            if ("SHIPPING_FEE".equals(payment.getPaymentType())) shipment.setPaymentStatus("REFUNDED");
        }
        shipment.transitionTo(ShipmentStatus.CLOSED_EXCEPTION); shipment.setClosureReason("LOST");
        incident.recordCompensation(ServerTime.now(clock));
        events.save(ShipmentEvent.of(shipment,users.getReferenceById(scope.actor().userId()),"COMPENSATION_CREDITED",reason.trim()));
        notifyOwner(shipment,"COMPENSATION_CREDITED","INCIDENT#"+incidentId,"Your loss compensation decision is complete");
        return shipment;
    }

    private boolean paidSenderForwardFee(Payment payment) {
        return "PAID".equals(payment.getStatus()) && "SENDER".equals(payment.getPayerType()) && (REFUNDABLE.contains(payment.getPaymentType()) || "FORWARD_FEE_ON_RETURN".equals(payment.getPaymentType()))
                && payment.getPaidAt()!=null && payment.getAmount()!=null && payment.getAmount().signum()>0;
    }

    private void requireCredit(String type,String referenceType,Long referenceId,Long owner,BigDecimal amount) {
        WalletTransaction tx=transactions.findFirstByTransactionTypeAndReferenceTypeAndReferenceId(type,referenceType,referenceId)
                .orElseThrow(() -> new IllegalStateException("Settled source has no wallet evidence"));
        if (!Objects.equals(tx.getUserId(),owner) || tx.getAmount().compareTo(amount)!=0
                || tx.getBalanceAfter().subtract(tx.getBalanceBefore()).compareTo(amount)!=0)
            throw new IllegalStateException("Wallet evidence does not match the settled source");
    }
    private void notifyOwner(Shipment shipment,String type,String reference,String message) {
        notifications.publishToUserAfterCommit(shipment.getSender().getUsername(),new NotificationEventResponse(type,reference,message));
    }

    @Override @Transactional(readOnly=true) public List<RefundCandidateResponse> listPendingRefundCandidates() {
        FinanceAccess.Scope scope=access.current();
        return outbox.findTop100ByStatusOrderByIdAsc("PENDING_WALLET_CREDIT").stream()
                .map(o -> payments.findById(o.getPaymentId()).orElse(null)).filter(Objects::nonNull)
                .filter(p -> scope.contains(p.getShipment().getOriginHubId()))
                .map(p -> new RefundCandidateResponse(p.getId(),p.getShipment().getId(),p.getShipment().getTrackingNumber(),p.getAmount())).toList();
    }
    @Override @Transactional(readOnly=true) public List<LossCompensationCandidate> listConfirmedLossCandidates() {
        FinanceAccess.Scope scope=access.current();
        return incidents.findTop100ByTypeAndStatusAndResolutionOutcomeOrderByIdAsc("MISSING_PARCEL","INVESTIGATING","CONFIRMED_LOST").stream()
                .filter(i -> scope.contains(i.getHubId())).map(i -> shipments.findById(i.getShipmentId())
                        .filter(s -> s.isOnHold() && s.getCurrentHubId()==null)
                        .map(s -> new LossCompensationCandidate(i.getId(),s.getId(),s.getTrackingNumber())).orElse(null))
                .filter(Objects::nonNull).toList();
    }
    @Override @Transactional(readOnly=true) public BigDecimal calculateLostCompensation(Shipment shipment) {
        FinanceAccess.Scope scope=access.current();
        if (incidents.findByShipmentId(shipment.getId()).stream().noneMatch(i -> scope.contains(i.getHubId()) && "CONFIRMED_LOST".equals(i.getResolutionOutcome())))
            throw new org.springframework.security.access.AccessDeniedException("Confirmed loss is outside scope");
        return (shipment.getDeclaredValue()==null ? BigDecimal.ZERO : shipment.getDeclaredValue())
                .add(payments.findByShipmentId(shipment.getId()).stream().filter(this::paidSenderForwardFee).map(Payment::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add));
    }
}
