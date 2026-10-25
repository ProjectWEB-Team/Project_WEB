package vn.iotstar.service.impl;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.finance.ReconciliationSubmissionRequest;
import vn.iotstar.dto.finance.ShiftExpectedItemResponse;
import vn.iotstar.dto.realtime.NotificationEventResponse;
import vn.iotstar.entity.CodTransaction;
import vn.iotstar.entity.Incident;
import vn.iotstar.entity.Payment;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.ShipperReconciliation;
import vn.iotstar.entity.ShipperReconciliationItem;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.entity.Wallet;
import vn.iotstar.entity.WalletTransaction;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.realtime.NotificationEventPublisher;
import vn.iotstar.repository.CodTransactionRepository;
import vn.iotstar.repository.FinanceReconciliationRepository;
import vn.iotstar.repository.IncidentRepository;
import vn.iotstar.repository.PaymentRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.repository.ShipperReconciliationItemRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.repository.WalletRepository;
import vn.iotstar.repository.WalletTransactionRepository;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.FinanceReconciliationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FinanceReconciliationServiceImpl implements FinanceReconciliationService {

    private final FinanceReconciliationRepository reconciliationRepository;
    private final ShipperReconciliationItemRepository itemRepository;
    private final CodTransactionRepository codTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final IncidentRepository incidentRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    public FinanceReconciliationServiceImpl(FinanceReconciliationRepository reconciliationRepository,
                                           ShipperReconciliationItemRepository itemRepository,
                                           CodTransactionRepository codTransactionRepository,
                                           PaymentRepository paymentRepository,
                                           WalletRepository walletRepository,
                                           WalletTransactionRepository walletTransactionRepository,
                                           IncidentRepository incidentRepository,
                                           ShipmentRepository shipmentRepository,
                                           UserRepository userRepository,
                                           ShipmentEventRepository shipmentEventRepository,
                                           NotificationEventPublisher notificationEventPublisher) {
        this.reconciliationRepository = reconciliationRepository;
        this.itemRepository = itemRepository;
        this.codTransactionRepository = codTransactionRepository;
        this.paymentRepository = paymentRepository;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.incidentRepository = incidentRepository;
        this.shipmentRepository = shipmentRepository;
        this.userRepository = userRepository;
        this.shipmentEventRepository = shipmentEventRepository;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    @Override
    public ShipperReconciliation reconcile(ReconciliationSubmissionRequest request) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (!actor.hasRole(UserRole.ROLE_MANAGER) && !actor.hasRole(UserRole.ROLE_ADMIN)) {
            throw new AccessDeniedException("ROLE_MANAGER or ROLE_ADMIN required for reconciliation");
        }

        User manager = userRepository.findById(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found: " + actor.userId()));

        User shipper = userRepository.findById(request.getShipperId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipper not found: " + request.getShipperId()));

        LocalDate recDate = request.getReconciliationDate() != null ? request.getReconciliationDate() : LocalDate.now();

        BigDecimal totalExpected = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;

        for (ReconciliationSubmissionRequest.ItemRequest ir : request.getItems()) {
            if (ir.getExpectedAmount() != null) {
                totalExpected = totalExpected.add(ir.getExpectedAmount());
            }
            if (ir.getActualAmount() != null) {
                totalActual = totalActual.add(ir.getActualAmount());
            }
        }

        ShipperReconciliation reconciliation = ShipperReconciliation.create(
                shipper, manager, recDate, totalExpected, totalActual);
        reconciliation = reconciliationRepository.save(reconciliation);

        boolean hasDiscrepancy = false;

        for (ReconciliationSubmissionRequest.ItemRequest ir : request.getItems()) {
            // Check unique source constraint
            Optional<ShipperReconciliationItem> existingItem = itemRepository.findBySourceTypeAndSourceId(
                    ir.getSourceType(), ir.getSourceId());
            if (existingItem.isPresent()) {
                throw new IllegalStateException("Source already reconciled: " + ir.getSourceType() + " #" + ir.getSourceId());
            }

            Shipment shipment = shipmentRepository.findById(ir.getShipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + ir.getShipmentId()));

            ShipperReconciliationItem item = ShipperReconciliationItem.create(
                    reconciliation, shipment, ir.getSourceType(), ir.getSourceId(),
                    ir.getExpectedAmount(), ir.getActualAmount(), ir.getNote());
            itemRepository.save(item);

            boolean isMatched = "MATCHED".equals(item.getStatus());
            if (!isMatched) {
                hasDiscrepancy = true;
            }

            if ("COD_TRANSACTION".equalsIgnoreCase(ir.getSourceType())) {
                CodTransaction cod = codTransactionRepository.findByIdForUpdate(ir.getSourceId())
                        .orElseThrow(() -> new ResourceNotFoundException("COD transaction not found: " + ir.getSourceId()));

                if (isMatched) {
                    // The COD becomes reconciled and ready for payout. The wallet
                    // credit is a separate, explicit payout step.
                    cod.setStatus("READY_FOR_PAYOUT");
                    cod.setReconciledAt(LocalDateTime.now());
                    codTransactionRepository.save(cod);

                    shipmentEventRepository.save(ShipmentEvent.create(
                            shipment, manager, "COD_RECONCILED", "COD reconciled and ready for payout"));
                } else {
                    // Discrepancy item: hold in HOLD_DISPUTE
                    cod.setStatus("HOLD_DISPUTE");
                    cod.setReconciledAt(LocalDateTime.now());
                    codTransactionRepository.save(cod);

                    String incidentType = "SHORTAGE".equals(item.getStatus()) ? "MONEY_SHORTAGE" : "MONEY_OVERAGE";
                    Incident incident = Incident.create(
                            shipment.getId(), null, shipment.getCurrentHubId(), incidentType,
                            "Reconciliation discrepancy for COD: expected " + ir.getExpectedAmount() + ", actual " + ir.getActualAmount(),
                            manager.getId());
                    incidentRepository.save(incident);

                    shipmentEventRepository.save(ShipmentEvent.create(
                            shipment, manager, "COD_DISPUTE", "COD discrepancy held in dispute: " + incidentType));
                }
            } else if ("RECEIVER_CASH_SHIPPING_FEE_PAYMENT".equalsIgnoreCase(ir.getSourceType())) {
                Payment payment = paymentRepository.findByIdForUpdate(ir.getSourceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + ir.getSourceId()));

                if (isMatched) {
                    // Receiver cash shipping fee is verified and accounted for.
                    // Strictly NO COD_PAYOUT or wallet credit.
                    shipmentEventRepository.save(ShipmentEvent.create(
                            shipment, manager, "SHIPPING_FEE_RECONCILED", "Receiver cash shipping fee reconciled"));
                } else {
                    String incidentType = "SHORTAGE".equals(item.getStatus()) ? "MONEY_SHORTAGE" : "MONEY_OVERAGE";
                    Incident incident = Incident.create(
                            shipment.getId(), null, shipment.getCurrentHubId(), incidentType,
                            "Reconciliation discrepancy for receiver cash fee: expected " + ir.getExpectedAmount() + ", actual " + ir.getActualAmount(),
                            manager.getId());
                    incidentRepository.save(incident);

                    shipmentEventRepository.save(ShipmentEvent.create(
                            shipment, manager, "FEE_DISPUTE", "Receiver cash fee discrepancy: " + incidentType));
                }
            }
        }

        // The shift status is decided by the items, not by the totals, so a
        // shortage compensated by an overage still counts as a discrepancy.
        reconciliation.complete(hasDiscrepancy);
        reconciliation = reconciliationRepository.save(reconciliation);

        return reconciliation;
    }

    @Override
    public ShipperReconciliationItem payoutReadyCod(Long codTransactionId) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (!actor.hasRole(UserRole.ROLE_MANAGER) && !actor.hasRole(UserRole.ROLE_ADMIN)) {
            throw new AccessDeniedException("ROLE_MANAGER or ROLE_ADMIN required to pay out reconciled COD");
        }

        CodTransaction cod = codTransactionRepository.findByIdForUpdate(codTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException("COD transaction not found: " + codTransactionId));

        if (!"READY_FOR_PAYOUT".equals(cod.getStatus())) {
            if ("PAID_TO_SENDER".equals(cod.getStatus())) {
                return itemRepository.findBySourceTypeAndSourceId("COD_TRANSACTION", codTransactionId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Reconciliation item not found for COD: " + codTransactionId));
            }
            throw new IllegalStateException("Only reconciled COD can be paid out. Current: " + cod.getStatus());
        }

        Shipment shipment = cod.getShipment();
        User manager = userRepository.findById(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found: " + actor.userId()));

        Long senderId = shipment.getSender().getId();
        Wallet wallet = walletRepository.findByUserIdForUpdate(senderId)
                .orElseGet(() -> walletRepository.save(Wallet.createForUser(senderId)));

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.credit(cod.getAmount());
        BigDecimal balanceAfter = wallet.getBalance();
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.create(
                senderId, "COD_PAYOUT", cod.getAmount(),
                balanceBefore, balanceAfter, shipment.getId(), "SHIPMENT",
                "COD payout for shipment " + shipment.getTrackingNumber());
        walletTransactionRepository.save(tx);

        cod.setStatus("PAID_TO_SENDER");
        cod.setPayoutAt(LocalDateTime.now());
        codTransactionRepository.save(cod);

        shipmentEventRepository.save(ShipmentEvent.create(
                shipment, manager, "COD_PAYOUT_COMPLETED", "COD paid out to sender wallet"));

        notificationEventPublisher.publishAfterCommit(actor, new NotificationEventResponse(
                "COD_PAYOUT_COMPLETED", shipment.getTrackingNumber(), shipment.getCurrentStatus().name(),
                cod.getAmount().toPlainString(), "SHIPMENT#" + shipment.getId(),
                "COD has been credited to your e-wallet", LocalDateTime.now()));

        return itemRepository.findBySourceTypeAndSourceId("COD_TRANSACTION", codTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation item not found for COD: " + codTransactionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipperReconciliation> getReconciliationsByShipper(Long shipperId) {
        return reconciliationRepository.findByShipperId(shipperId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipperReconciliationItem> getReconciliationItems(Long reconciliationId) {
        return itemRepository.findByReconciliationId(reconciliationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftExpectedItemResponse> getShiftExpectedItems(Long shipperId, LocalDate reconciliationDate) {
        LocalDate shiftDate = reconciliationDate != null ? reconciliationDate : LocalDate.now();
        List<ShiftExpectedItemResponse> items = new ArrayList<>();

        for (CodTransaction cod : codTransactionRepository.findByCollectedByShipperIdAndStatus(shipperId, "COLLECTED")) {
            if (cod.getCollectedAt() == null || !cod.getCollectedAt().toLocalDate().equals(shiftDate)) {
                continue;
            }
            items.add(new ShiftExpectedItemResponse(
                    cod.getShipment().getId(), cod.getShipment().getTrackingNumber(),
                    "COD_TRANSACTION", cod.getId(), cod.getAmount(), cod.getStatus(),
                    "Cash on delivery collected by the shipper"));
        }

        for (Payment payment : paymentRepository.findByStatusAndPaymentTypeAndPayerType("PAID", "SHIPPING_FEE", "RECEIVER")) {
            if (payment.getPaidAt() == null || !payment.getPaidAt().toLocalDate().equals(shiftDate)) {
                continue;
            }
            items.add(new ShiftExpectedItemResponse(
                    payment.getShipment().getId(), payment.getShipment().getTrackingNumber(),
                    "RECEIVER_CASH_SHIPPING_FEE_PAYMENT", payment.getId(), payment.getAmount(), payment.getStatus(),
                    "Receiver cash shipping fee collected by the shipper"));
        }

        return items;
    }
}

