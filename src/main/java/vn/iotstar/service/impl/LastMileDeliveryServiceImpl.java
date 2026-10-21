package vn.iotstar.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.config.LogisticsProperties;
import vn.iotstar.dto.delivery.DeliveryCollectionRequest;
import vn.iotstar.dto.delivery.DeliveryFailureRequest;
import vn.iotstar.dto.delivery.DeliveryScanRequest;
import vn.iotstar.dto.delivery.DeliveryTaskResponse;
import vn.iotstar.dto.realtime.NotificationEventResponse;
import vn.iotstar.dto.returnflow.ReturnHandoverRequest;
import vn.iotstar.dto.returnflow.ReturnTaskResponse;
import vn.iotstar.entity.CodTransaction;
import vn.iotstar.entity.DeliveryAttempt;
import vn.iotstar.entity.Payment;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentAssignment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.realtime.NotificationEventPublisher;
import vn.iotstar.repository.CodTransactionRepository;
import vn.iotstar.repository.LastMileDeliveryRepository;
import vn.iotstar.repository.PaymentRepository;
import vn.iotstar.repository.ShipmentAssignmentRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.LastMileDeliveryService;
import vn.iotstar.service.StorageService;
import vn.iotstar.service.WalletLedgerService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class LastMileDeliveryServiceImpl implements LastMileDeliveryService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentAssignmentRepository shipmentAssignmentRepository;
    private final LastMileDeliveryRepository lastMileDeliveryRepository;
    private final CodTransactionRepository codTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationEventPublisher notificationEventPublisher;
    private final WalletLedgerService walletLedgerService;
    private final LogisticsProperties logisticsProperties;
    private final StorageService storageService;

    public LastMileDeliveryServiceImpl(ShipmentRepository shipmentRepository,
                                       ShipmentAssignmentRepository shipmentAssignmentRepository,
                                       LastMileDeliveryRepository lastMileDeliveryRepository,
                                       CodTransactionRepository codTransactionRepository,
                                       PaymentRepository paymentRepository,
                                       ShipmentEventRepository shipmentEventRepository,
                                       UserRepository userRepository,
                                       ApplicationEventPublisher eventPublisher,
                                       NotificationEventPublisher notificationEventPublisher,
                                       WalletLedgerService walletLedgerService,
                                       LogisticsProperties logisticsProperties,
                                       StorageService storageService) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentAssignmentRepository = shipmentAssignmentRepository;
        this.lastMileDeliveryRepository = lastMileDeliveryRepository;
        this.codTransactionRepository = codTransactionRepository;
        this.paymentRepository = paymentRepository;
        this.shipmentEventRepository = shipmentEventRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.notificationEventPublisher = notificationEventPublisher;
        this.walletLedgerService = walletLedgerService;
        this.logisticsProperties = logisticsProperties;
        this.storageService = storageService;
    }

    @Override
    public DeliveryTaskResponse scanForDelivery(DeliveryScanRequest request) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (!actor.hasRole(UserRole.ROLE_SHIPPER)) {
            throw new AccessDeniedException("ROLE_SHIPPER required to scan for delivery");
        }

        Shipment shipment = shipmentRepository.findByIdForUpdate(request.getShipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + request.getShipmentId()));

        User shipper = userRepository.findById(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + actor.userId()));

        // Check active assignment
        List<ShipmentAssignment> assignments = shipmentAssignmentRepository.findByShipmentIdOrderByAssignedAtDesc(shipment.getId());
        ShipmentAssignment activeAssignment = assignments.stream()
                .filter(a -> a.getAssignee().getId().equals(actor.userId()) && "DELIVERY".equals(a.getAssignmentType()))
                .filter(a -> "ASSIGNED".equals(a.getStatus()) || "ACCEPTED".equals(a.getStatus()) || "IN_PROGRESS".equals(a.getStatus()))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Shipper is not assigned to deliver shipment: " + shipment.getId()));

        // Idempotent check
        if (ShipmentStatus.OUT_FOR_DELIVERY.equals(shipment.getCurrentStatus())) {
            return toTaskResponse(shipment, activeAssignment);
        }

        if (!ShipmentStatus.AT_HUB.equals(shipment.getCurrentStatus())) {
            throw new IllegalStateException("Shipment must be AT_HUB to scan into OUT_FOR_DELIVERY. Current status: " + shipment.getCurrentStatus());
        }

        if (request.getHubId() != null && shipment.getCurrentHubId() != null
                && !request.getHubId().equals(shipment.getCurrentHubId())) {
            throw new AccessDeniedException("Shipment is not at the scanning hub");
        }

        shipment.transitionTo(ShipmentStatus.OUT_FOR_DELIVERY);
        activeAssignment.setStatus("IN_PROGRESS");
        if (activeAssignment.getAcceptedAt() == null) {
            activeAssignment.setAcceptedAt(LocalDateTime.now());
        }

        shipmentRepository.save(shipment);
        shipmentAssignmentRepository.save(activeAssignment);

        // Open the COD record for collection as soon as the parcel goes out.
        if (shipment.getCodAmount() != null && shipment.getCodAmount().compareTo(BigDecimal.ZERO) > 0
                && codTransactionRepository.findByShipmentId(shipment.getId()).isEmpty()) {
            codTransactionRepository.save(
                    CodTransaction.createWaiting(shipment, shipper, shipment.getCodAmount(), "CASH"));
        }

        shipmentEventRepository.save(ShipmentEvent.create(
                shipment, shipper, "OUT_FOR_DELIVERY", "Shipment scanned out for delivery by " + shipper.getUsername()));

        notificationEventPublisher.publishAfterCommit(actor, new NotificationEventResponse(
                "OUT_FOR_DELIVERY", shipment.getTrackingNumber(), shipment.getCurrentStatus().name(),
                null, "SHIPMENT#" + shipment.getId(),
                "Your parcel is out for delivery", LocalDateTime.now()));

        return toTaskResponse(shipment, activeAssignment);
    }

    @Override
    public DeliveryTaskResponse recordCollection(DeliveryCollectionRequest request) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (!actor.hasRole(UserRole.ROLE_SHIPPER)) {
            throw new AccessDeniedException("ROLE_SHIPPER required to record collection");
        }

        Shipment shipment = shipmentRepository.findByIdForUpdate(request.getShipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + request.getShipmentId()));

        User shipper = userRepository.findById(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + actor.userId()));

        ShipmentAssignment activeAssignment = shipmentAssignmentRepository.findFirstByShipmentIdAndAssigneeIdAndStatus(
                shipment.getId(), actor.userId(), "IN_PROGRESS")
                .orElseThrow(() -> new AccessDeniedException("Shipper has no in-progress delivery task for shipment: " + shipment.getId()));

        if (!ShipmentStatus.OUT_FOR_DELIVERY.equals(shipment.getCurrentStatus())) {
            throw new IllegalStateException("Shipment must be OUT_FOR_DELIVERY to record collection");
        }

        // 1. Process COD if due
        if (shipment.getCodAmount() != null && shipment.getCodAmount().compareTo(BigDecimal.ZERO) > 0) {
            Optional<CodTransaction> existingCod = codTransactionRepository.findByShipmentIdForUpdate(shipment.getId());
            if (existingCod.isEmpty()) {
                BigDecimal collectedAmount = request.getCodAmountCollected() != null
                        ? request.getCodAmountCollected() : shipment.getCodAmount();
                CodTransaction codTx = CodTransaction.createCollected(
                        shipment, shipper, collectedAmount, request.getPaymentMethod());
                codTransactionRepository.save(codTx);

                shipmentEventRepository.save(ShipmentEvent.create(
                        shipment, shipper, "COD_COLLECTED", "COD collected: " + collectedAmount + " by " + shipper.getUsername()));
            } else if (!"COLLECTED".equals(existingCod.get().getStatus())) {
                CodTransaction codTx = existingCod.get();
                codTx.setStatus("COLLECTED");
                codTx.setCollectedAt(LocalDateTime.now());
                codTransactionRepository.save(codTx);

                shipmentEventRepository.save(ShipmentEvent.create(
                        shipment, shipper, "COD_COLLECTED", "COD status set to COLLECTED by " + shipper.getUsername()));
            }
        }

        // 2. Process Receiver Shipping Fee if due
        if ("RECEIVER".equalsIgnoreCase(shipment.getFeePayer())
                && shipment.getShippingFee() != null && shipment.getShippingFee().compareTo(BigDecimal.ZERO) > 0) {
            Optional<Payment> existingPayment = paymentRepository.findFirstByShipmentIdAndPayerTypeAndPaymentType(
                    shipment.getId(), "RECEIVER", "SHIPPING_FEE");
            if (existingPayment.isEmpty()) {
                BigDecimal feeAmount = request.getShippingFeeCollected() != null
                        ? request.getShippingFeeCollected() : shipment.getShippingFee();
                Payment p = Payment.create(shipment, "RECEIVER", "SHIPPING_FEE", feeAmount, "CASH", "PAID");
                paymentRepository.save(p);

                shipmentEventRepository.save(ShipmentEvent.create(
                        shipment, shipper, "RECEIVER_FEE_COLLECTED", "Receiver shipping fee collected by " + shipper.getUsername()));
            } else if (!"PAID".equals(existingPayment.get().getStatus())) {
                Payment p = existingPayment.get();
                p.setStatus("PAID");
                p.setPaidAt(LocalDateTime.now());
                paymentRepository.save(p);

                shipmentEventRepository.save(ShipmentEvent.create(
                        shipment, shipper, "RECEIVER_FEE_COLLECTED", "Receiver shipping fee confirmed PAID by " + shipper.getUsername()));
            }
        }

        return toTaskResponse(shipment, activeAssignment);
    }

    @Override
    public DeliveryTaskResponse confirmDelivery(Long shipmentId) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (!actor.hasRole(UserRole.ROLE_SHIPPER)) {
            throw new AccessDeniedException("ROLE_SHIPPER required to confirm delivery");
        }

        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));

        User shipper = userRepository.findById(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + actor.userId()));

        List<ShipmentAssignment> assignments = shipmentAssignmentRepository.findByShipmentIdOrderByAssignedAtDesc(shipment.getId());
        ShipmentAssignment activeAssignment = assignments.stream()
                .filter(a -> a.getAssignee().getId().equals(actor.userId()) && "DELIVERY".equals(a.getAssignmentType()))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Shipper is not assigned to deliver shipment: " + shipment.getId()));

        // Idempotency: if already delivered, verify same actor
        if (ShipmentStatus.DELIVERED.equals(shipment.getCurrentStatus())) {
            return toTaskResponse(shipment, activeAssignment);
        }

        if (!ShipmentStatus.OUT_FOR_DELIVERY.equals(shipment.getCurrentStatus())) {
            throw new IllegalStateException("Shipment must be OUT_FOR_DELIVERY to confirm delivered. Current: " + shipment.getCurrentStatus());
        }

        // Per D-03: Must collect COD before DELIVERED
        if (shipment.getCodAmount() != null && shipment.getCodAmount().compareTo(BigDecimal.ZERO) > 0) {
            Optional<CodTransaction> cod = codTransactionRepository.findByShipmentId(shipment.getId());
            if (cod.isEmpty() || !"COLLECTED".equals(cod.get().getStatus())) {
                throw new IllegalStateException("COD collection required before delivery confirmation");
            }
        }

        // Per D-03: Must collect receiver-paid shipping fee before DELIVERED
        if ("RECEIVER".equalsIgnoreCase(shipment.getFeePayer())
                && shipment.getShippingFee() != null && shipment.getShippingFee().compareTo(BigDecimal.ZERO) > 0) {
            Optional<Payment> payment = paymentRepository.findFirstByShipmentIdAndPayerTypeAndPaymentType(
                    shipment.getId(), "RECEIVER", "SHIPPING_FEE");
            if (payment.isEmpty() || !"PAID".equals(payment.get().getStatus())) {
                throw new IllegalStateException("Receiver shipping fee collection required before delivery confirmation");
            }
        }

        shipment.transitionTo(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(LocalDateTime.now());
        shipment.setPaymentStatus("PAID");
        shipmentRepository.save(shipment);

        activeAssignment.setStatus("COMPLETED");
        activeAssignment.setCompletedAt(LocalDateTime.now());
        shipmentAssignmentRepository.save(activeAssignment);

        long count = lastMileDeliveryRepository.countByShipmentId(shipment.getId());
        DeliveryAttempt attempt = DeliveryAttempt.create(
                activeAssignment, shipment, shipper, (int) count + 1, LocalDate.now(),
                "DELIVERED", "Delivered successfully", null, null);
        lastMileDeliveryRepository.save(attempt);

        shipmentEventRepository.save(ShipmentEvent.create(
                shipment, shipper, "DELIVERED", "Shipment delivered by " + shipper.getUsername()));

        notificationEventPublisher.publishAfterCommit(actor, new NotificationEventResponse(
                "DELIVERED", shipment.getTrackingNumber(), shipment.getCurrentStatus().name(),
                shipment.getCodAmount() != null ? shipment.getCodAmount().toPlainString() : null,
                "SHIPMENT#" + shipment.getId(),
                "Your parcel has been delivered", LocalDateTime.now()));

        return toTaskResponse(shipment, activeAssignment);
    }

    @Override
    public DeliveryTaskResponse recordFailure(DeliveryFailureRequest request) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (!actor.hasRole(UserRole.ROLE_SHIPPER)) {
            throw new AccessDeniedException("ROLE_SHIPPER required to record failure");
        }

        Shipment shipment = shipmentRepository.findByIdForUpdate(request.getShipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + request.getShipmentId()));

        User shipper = userRepository.findById(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + actor.userId()));

        List<ShipmentAssignment> assignments = shipmentAssignmentRepository.findByShipmentIdOrderByAssignedAtDesc(shipment.getId());
        ShipmentAssignment activeAssignment = assignments.stream()
                .filter(a -> a.getAssignee().getId().equals(actor.userId()) && "DELIVERY".equals(a.getAssignmentType()))
                .filter(a -> "IN_PROGRESS".equals(a.getStatus()) || "ASSIGNED".equals(a.getStatus()))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Shipper has no active assignment for shipment: " + shipment.getId()));

        if (!ShipmentStatus.OUT_FOR_DELIVERY.equals(shipment.getCurrentStatus())) {
            throw new IllegalStateException("Shipment must be OUT_FOR_DELIVERY to record failure");
        }

        String reason = request.getReason();
        Set<String> group1 = Set.of("NO_ANSWER", "PAYMENT_NOT_AVAILABLE", "RESCHEDULED", "OTHER");
        Set<String> group2 = Set.of("RECEIVER_REFUSED", "WRONG_ADDRESS");

        if (!group1.contains(reason) && !group2.contains(reason)) {
            throw new IllegalArgumentException("Invalid failure reason: " + reason);
        }

        long attemptCount = lastMileDeliveryRepository.countByShipmentId(shipment.getId());
        int nextAttempt = (int) attemptCount + 1;
        LocalDate today = LocalDate.now();

        // Check 1 failure per day limit
        if (logisticsProperties.getDelivery().isAttemptDayLock()) {
            long todayAttempts = lastMileDeliveryRepository.countByShipmentIdAndAttemptDate(shipment.getId(), today);
            if (todayAttempts > 0) {
                // Already attempted today - enforce 1 attempt per day rule
                throw new IllegalStateException("Maximum one delivery attempt per day permitted");
            }
        }

        DeliveryAttempt attempt = DeliveryAttempt.create(
                activeAssignment, shipment, shipper, nextAttempt, today, reason,
                request.getNote(), resolveEvidenceUrl(request), request.getRescheduledDate());
        lastMileDeliveryRepository.save(attempt);

        if (group2.contains(reason)) {
            // Group 2 fast-fail into FAILED then RETURN_TO_SENDER
            shipment.transitionTo(ShipmentStatus.FAILED);
            shipment.setFailReason(reason);
            shipmentEventRepository.save(ShipmentEvent.create(
                    shipment, shipper, "DELIVERY_FAILED", "Fast-fail: " + reason));

            shipment.transitionTo(ShipmentStatus.RETURN_TO_SENDER);
            shipment.setDirection("RETURN");
            applyReturnFee(shipment);
            shipmentRepository.save(shipment);

            activeAssignment.setStatus("COMPLETED");
            activeAssignment.setCompletedAt(LocalDateTime.now());
            shipmentAssignmentRepository.save(activeAssignment);

            // Cancel any waiting COD
            codTransactionRepository.findByShipmentId(shipment.getId()).ifPresent(cod -> {
                if ("WAITING_COLLECTION".equals(cod.getStatus())) {
                    cod.setStatus("CANCELLED");
                    codTransactionRepository.save(cod);
                }
            });

            shipmentEventRepository.save(ShipmentEvent.create(
                    shipment, shipper, "RETURN_TO_SENDER", "Transitioned to RETURN_TO_SENDER: " + reason));

        } else {
            // Group 1
            if (nextAttempt >= logisticsProperties.getDelivery().getMaxAttempts()) {
                // Exhausted the configured number of attempts
                shipment.transitionTo(ShipmentStatus.FAILED);
                shipment.setFailReason("DELIVERY_ATTEMPTS_EXHAUSTED");
                shipmentEventRepository.save(ShipmentEvent.create(
                        shipment, shipper, "DELIVERY_FAILED", "Delivery attempts exhausted ("
                                + logisticsProperties.getDelivery().getMaxAttempts() + " attempts)"));

                shipment.transitionTo(ShipmentStatus.RETURN_TO_SENDER);
                shipment.setDirection("RETURN");
                applyReturnFee(shipment);
                shipmentRepository.save(shipment);

                activeAssignment.setStatus("COMPLETED");
                activeAssignment.setCompletedAt(LocalDateTime.now());
                shipmentAssignmentRepository.save(activeAssignment);

                codTransactionRepository.findByShipmentId(shipment.getId()).ifPresent(cod -> {
                    if ("WAITING_COLLECTION".equals(cod.getStatus())) {
                        cod.setStatus("CANCELLED");
                        codTransactionRepository.save(cod);
                    }
                });

                shipmentEventRepository.save(ShipmentEvent.create(
                        shipment, shipper, "RETURN_TO_SENDER", "Exhausted 3 attempts, marked RETURN_TO_SENDER"));
            } else {
                // Temporary failure: custody returns to delivery hub (AT_HUB)
                shipment.transitionTo(ShipmentStatus.AT_HUB);
                shipment.setFailReason(reason);
                shipmentRepository.save(shipment);

                activeAssignment.setStatus("COMPLETED");
                activeAssignment.setCompletedAt(LocalDateTime.now());
                shipmentAssignmentRepository.save(activeAssignment);

                // Create legal rescheduled assignment for retry
                ShipmentAssignment rescheduledAssignment = ShipmentAssignment.create(
                        shipment, shipper, "DELIVERY");
                rescheduledAssignment.setStatus("ASSIGNED");
                shipmentAssignmentRepository.save(rescheduledAssignment);

                shipmentEventRepository.save(ShipmentEvent.create(
                        shipment, shipper, "DELIVERY_ATTEMPT_FAILED", "Attempt " + nextAttempt + " failed: " + reason));
                shipmentEventRepository.save(ShipmentEvent.create(
                        shipment, shipper, "RETURNED_TO_HUB", "Custody returned to hub"));
                if ("RESCHEDULED".equals(reason) && request.getRescheduledDate() != null) {
                    shipmentEventRepository.save(ShipmentEvent.create(
                            shipment, shipper, "DELIVERY_RESCHEDULED", "Rescheduled for " + request.getRescheduledDate()));
                }

                notificationEventPublisher.publishAfterCommit(actor, new NotificationEventResponse(
                        "DELIVERY_FAILED", shipment.getTrackingNumber(), shipment.getCurrentStatus().name(),
                        null, "SHIPMENT#" + shipment.getId(),
                        "Delivery attempt failed: " + reason, LocalDateTime.now()));
            }
        }

        return toTaskResponse(shipment, activeAssignment);
    }

    /**
     * Resolves the evidence reference of a failed attempt. A freshly uploaded
     * photo is stored first; otherwise the reference supplied with the request
     * is kept.
     */
    private String resolveEvidenceUrl(DeliveryFailureRequest request) {
        if (request.getEvidence() != null && !request.getEvidence().isEmpty()) {
            String storedFilename = storageService.buildStorageFilename(request.getEvidence(), "delivery-evidence");
            return storageService.store(request.getEvidence(), storedFilename);
        }
        return request.getEvidenceUrl();
    }

    /**
     * Applies the return fee the sender owes when a parcel travels back.
     *
     * <p>The Workflow keeps the forward fee and the return fee separate: a sender
     * who already paid the forward fee only owes the return fee, while a sender
     * whose receiver was the payer ends up owing both. The priced value comes
     * from the shipment; the configured default only applies when the pricing
     * policy of the intake slice has not filled it in yet.
     */
    private void applyReturnFee(Shipment shipment) {
        BigDecimal returnFee = shipment.getReturnFee() != null
                ? shipment.getReturnFee()
                : logisticsProperties.getFinance().getDefaultReturnFee();
        shipment.setReturnFee(returnFee);
    }

    @Override
    public ReturnTaskResponse processReturnHandover(ReturnHandoverRequest request) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        Shipment shipment = shipmentRepository.findByIdForUpdate(request.getShipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + request.getShipmentId()));

        User currentUser = userRepository.findById(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + actor.userId()));

        if (!ShipmentStatus.RETURN_TO_SENDER.equals(shipment.getCurrentStatus())) {
            throw new IllegalStateException("Shipment must be in RETURN_TO_SENDER to hand over. Current: " + shipment.getCurrentStatus());
        }

        // Validate origin hub custody
        if (shipment.getOriginHubId() != null && shipment.getCurrentHubId() != null
                && !shipment.getOriginHubId().equals(shipment.getCurrentHubId())) {
            throw new IllegalStateException("Return package has not arrived at origin hub");
        }

        // Sender verification for SELF_PICKUP
        if ("SELF_PICKUP".equalsIgnoreCase(request.getHandoverType())) {
            if (!actor.userId().equals(shipment.getSender().getId()) && !actor.hasRole(UserRole.ROLE_HUB_STAFF) && !actor.hasRole(UserRole.ROLE_MANAGER)) {
                throw new AccessDeniedException("Only the sender or hub staff can process self-pickup");
            }
        }

        // Validate return liability settlement (D-07)
        BigDecimal returnFee = shipment.getReturnFee() != null ? shipment.getReturnFee() : BigDecimal.ZERO;
        boolean returnFeeDue = returnFee.compareTo(BigDecimal.ZERO) > 0;
        if (returnFeeDue) {
            Optional<Payment> returnPayment = paymentRepository.findFirstByShipmentIdAndPaymentTypeAndStatus(
                    shipment.getId(), "RETURN_FEE", "PAID");
            if (returnPayment.isEmpty() && (request.getSettledReturnFee() == null || request.getSettledReturnFee().compareTo(returnFee) < 0)) {
                throw new IllegalStateException("Return fee of " + returnFee + " must be settled before handover");
            }
        }

        // Check if unpaid receiver shipping fee was due (sender now liable)
        if ("RECEIVER".equalsIgnoreCase(shipment.getFeePayer()) && shipment.getShippingFee() != null) {
            Optional<Payment> originalFee = paymentRepository.findFirstByShipmentIdAndPayerTypeAndPaymentType(
                    shipment.getId(), "RECEIVER", "SHIPPING_FEE");
            if (originalFee.isPresent() && !"PAID".equals(originalFee.get().getStatus())) {
                throw new IllegalStateException("Unpaid shipping fee must be settled by sender before return handover");
            }
        }

        // Record the return fee payment so the financial history reflects what
        // the sender handed in at the counter.
        if (returnFeeDue) {
            recordReturnFeePayment(shipment, currentUser, request, returnFee);
        }

        shipment.transitionTo(ShipmentStatus.RETURNED_TO_SENDER);
        shipment.setReturnedAt(LocalDateTime.now());
        shipmentRepository.save(shipment);

        shipmentEventRepository.save(ShipmentEvent.create(
                shipment, currentUser, "RETURNED_TO_SENDER",
                "Returned parcel handed over to sender via " + request.getHandoverType()));

        return toReturnResponse(shipment, true);
    }

    /**
     * Records the return fee the sender paid. A cash payment is stored on the
     * payment ledger, an e-wallet payment additionally debits the wallet.
     */
    private void recordReturnFeePayment(Shipment shipment, User currentUser, ReturnHandoverRequest request,
                                        BigDecimal returnFee) {
        Optional<Payment> existing = paymentRepository.findFirstByShipmentIdAndPaymentTypeAndStatus(
                shipment.getId(), "RETURN_FEE", "PAID");
        if (existing.isPresent()) {
            return;
        }

        String method = request.getPaymentMethod() != null ? request.getPaymentMethod() : "CASH";
        BigDecimal settled = request.getSettledReturnFee() != null ? request.getSettledReturnFee() : returnFee;

        paymentRepository.save(Payment.create(shipment, "SENDER", "RETURN_FEE", settled, method, "PAID"));

        if ("E_WALLET".equalsIgnoreCase(method)) {
            walletLedgerService.recordFeeDebit(shipment.getSender().getId(), settled, "RETURN_FEE_PAYMENT",
                    shipment.getId(), "Return fee paid from e-wallet for shipment " + shipment.getTrackingNumber());
        }

        shipment.setPaymentStatus("PAID");
        shipmentRepository.save(shipment);

        shipmentEventRepository.save(ShipmentEvent.create(
                shipment, currentUser, "RETURN_FEE_PAID",
                "Return fee of " + settled + " settled by " + currentUser.getUsername()));
    }

    @Override
    public ReturnTaskResponse confirmReturnDelivery(Long shipmentId) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (!actor.hasRole(UserRole.ROLE_SHIPPER)) {
            throw new AccessDeniedException("ROLE_SHIPPER required to confirm return delivery");
        }

        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));

        User shipper = userRepository.findById(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + actor.userId()));

        ShipmentAssignment returnAssignment = shipmentAssignmentRepository.findFirstByShipmentIdAndAssigneeIdAndStatus(
                shipment.getId(), actor.userId(), "IN_PROGRESS")
                .orElseGet(() -> shipmentAssignmentRepository.findFirstByShipmentIdAndAssigneeIdAndStatus(
                        shipment.getId(), actor.userId(), "ASSIGNED")
                        .orElseThrow(() -> new AccessDeniedException("Shipper has no return delivery assignment for shipment: " + shipmentId)));

        if (!"RETURN_DELIVERY".equals(returnAssignment.getAssignmentType())) {
            throw new AccessDeniedException("Assignment is not RETURN_DELIVERY");
        }

        if (!ShipmentStatus.RETURN_TO_SENDER.equals(shipment.getCurrentStatus())) {
            throw new IllegalStateException("Shipment must be in RETURN_TO_SENDER to confirm return delivery");
        }

        shipment.transitionTo(ShipmentStatus.RETURNED_TO_SENDER);
        shipment.setReturnedAt(LocalDateTime.now());
        shipmentRepository.save(shipment);

        returnAssignment.setStatus("COMPLETED");
        returnAssignment.setCompletedAt(LocalDateTime.now());
        shipmentAssignmentRepository.save(returnAssignment);

        shipmentEventRepository.save(ShipmentEvent.create(
                shipment, shipper, "RETURNED_TO_SENDER", "Return delivery completed by " + shipper.getUsername()));

        return toReturnResponse(shipment, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryTaskResponse> getAssignedDeliveryTasks() {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        List<ShipmentAssignment> assignments = shipmentAssignmentRepository.findByAssigneeIdAndStatusIn(
                actor.userId(), List.of("ASSIGNED", "ACCEPTED", "IN_PROGRESS"));

        return assignments.stream()
                .filter(a -> "DELIVERY".equals(a.getAssignmentType()))
                .map(a -> toTaskResponse(a.getShipment(), a))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryTaskResponse getDeliveryTask(Long shipmentId) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));

        List<ShipmentAssignment> assignments = shipmentAssignmentRepository.findByShipmentIdOrderByAssignedAtDesc(shipment.getId());
        ShipmentAssignment assignment = assignments.stream()
                .filter(a -> a.getAssignee().getId().equals(actor.userId()))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Not assigned to this shipment"));

        return toTaskResponse(shipment, assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnTaskResponse> getAssignedReturnTasks() {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        List<ShipmentAssignment> assignments = shipmentAssignmentRepository.findByAssigneeIdAndStatusIn(
                actor.userId(), List.of("ASSIGNED", "ACCEPTED", "IN_PROGRESS"));

        return assignments.stream()
                .filter(a -> "RETURN_DELIVERY".equals(a.getAssignmentType()))
                .map(a -> toReturnResponse(a.getShipment(), true))
                .collect(Collectors.toList());
    }

    private DeliveryTaskResponse toTaskResponse(Shipment s, ShipmentAssignment a) {
        boolean codCollected = false;
        if (s.getCodAmount() != null && s.getCodAmount().compareTo(BigDecimal.ZERO) > 0) {
            Optional<CodTransaction> cod = codTransactionRepository.findByShipmentId(s.getId());
            codCollected = cod.isPresent() && "COLLECTED".equals(cod.get().getStatus());
        } else {
            codCollected = true;
        }

        boolean feeCollected = false;
        if ("RECEIVER".equalsIgnoreCase(s.getFeePayer())
                && s.getShippingFee() != null && s.getShippingFee().compareTo(BigDecimal.ZERO) > 0) {
            Optional<Payment> payment = paymentRepository.findFirstByShipmentIdAndPayerTypeAndPaymentType(
                    s.getId(), "RECEIVER", "SHIPPING_FEE");
            feeCollected = payment.isPresent() && "PAID".equals(payment.get().getStatus());
        } else {
            feeCollected = true;
        }

        boolean eligible = ShipmentStatus.OUT_FOR_DELIVERY.equals(s.getCurrentStatus()) && codCollected && feeCollected;

        return new DeliveryTaskResponse(
                a.getId(), s.getId(), s.getTrackingNumber(),
                a.getStatus(), s.getCurrentStatus().name(),
                s.getReceiverName(), s.getReceiverPhone(), s.getReceiverAddress(),
                s.getCodAmount(), s.getShippingFee(), s.getFeePayer(),
                codCollected, feeCollected, eligible);
    }

    private ReturnTaskResponse toReturnResponse(Shipment s, boolean canCollect) {
        BigDecimal returnFee = s.getReturnFee() != null ? s.getReturnFee() : BigDecimal.ZERO;
        boolean returnFeePaid = true;
        if (returnFee.compareTo(BigDecimal.ZERO) > 0) {
            returnFeePaid = paymentRepository.findFirstByShipmentIdAndPaymentTypeAndStatus(
                    s.getId(), "RETURN_FEE", "PAID").isPresent();
        }

        return new ReturnTaskResponse(
                s.getId(), s.getTrackingNumber(), s.getCurrentStatus().name(),
                s.getDirection(), s.getOriginHubId(), s.getSenderName(),
                s.getSenderPhone(), returnFee, returnFee, returnFeePaid, canCollect);
    }
}

