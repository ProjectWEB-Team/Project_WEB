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
    private final LastMileAccess access;
    private final ShipmentFeeService fees;

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
                                       StorageService storageService, LastMileAccess access, ShipmentFeeService fees) {
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
        this.access=access; this.fees=fees;
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

        var profile = access.require(UserRole.ROLE_SHIPPER);
        if (!profile.getHubId().equals(shipment.getDestinationHubId()) || shipment.isOnHold() || !"FORWARD".equals(shipment.getDirection()))
            throw new AccessDeniedException("Shipment is not assigned to this delivery hub");
        if (ShipmentStatus.OUT_FOR_DELIVERY.equals(shipment.getCurrentStatus())) {
            if (!"IN_PROGRESS".equals(activeAssignment.getStatus()) || shipment.getCurrentHubId()!=null) throw new IllegalStateException("Invalid outbound custody");
            return toTaskResponse(shipment, activeAssignment);
        }
        access.requireAt(shipment,profile);
        if (shipment.getCurrentStatus()!=ShipmentStatus.AT_HUB || !"ASSIGNED".equals(activeAssignment.getStatus()))
            throw new IllegalStateException("Parcel is not ready for outbound scan");
        if(request.getHubId()!=null && !profile.getHubId().equals(request.getHubId())) throw new AccessDeniedException("Wrong scan hub");
        access.requireForwardRetryReady(shipment);
        CodTransaction waitingCod=codTransactionRepository.findByShipmentIdForUpdate(shipment.getId()).orElse(null);
        if(waitingCod!=null) {
            if(!"WAITING_COLLECTION".equals(waitingCod.getStatus()) || waitingCod.getAmount()==null
                    || waitingCod.getAmount().compareTo(zero(shipment.getCodAmount()))!=0)
                throw new IllegalStateException("COD obligation is not ready for reassignment");
            waitingCod.assignWaitingCollector(shipper);
        }
        shipment.setCurrentHubId(null);
        shipment.transitionTo(ShipmentStatus.OUT_FOR_DELIVERY);
        activeAssignment.setStatus("IN_PROGRESS");
        if (activeAssignment.getAcceptedAt() == null) {
            activeAssignment.setAcceptedAt(LocalDateTime.now());
        }

        shipmentRepository.save(shipment);
        shipmentAssignmentRepository.save(activeAssignment);

        // Open the COD record for collection as soon as the parcel goes out.
        if (shipment.getCodAmount() != null && shipment.getCodAmount().compareTo(BigDecimal.ZERO) > 0
                && waitingCod==null) {
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

        requireDeliveryCustody(shipment,activeAssignment,actor);
        String method=request.getCodPaymentMethod();
        String feeMethod=request.getShippingFeePaymentMethod();
        BigDecimal codDue=zero(shipment.getCodAmount());
        BigDecimal feeDue="RECEIVER".equals(shipment.getFeePayer())?zero(shipment.getShippingFee()):BigDecimal.ZERO;
        if (codDue.signum()>0 && (method==null || !Set.of("CASH","VNPAY","TRANSFER").contains(method))) throw new IllegalArgumentException("Unsupported COD method");
        if (feeDue.signum()>0 && (feeMethod==null || !Set.of("CASH","VNPAY").contains(feeMethod))) throw new IllegalArgumentException("Unsupported receiver fee method");
        exactAmount(request.getCodAmountCollected(),codDue);
        exactAmount(request.getShippingFeeCollected(),feeDue);
        var cod=codTransactionRepository.findByShipmentIdForUpdate(shipment.getId()).orElse(null);
        var payment=paymentRepository.findFirstByShipmentIdAndPayerTypeAndPaymentType(shipment.getId(),"RECEIVER","SHIPPING_FEE").orElse(null);
        if(codDue.signum()>0 && cod!=null) {
            if(cod.getAmount()==null || cod.getAmount().compareTo(codDue)!=0 || cod.getCollectedByShipper()==null || !actor.userId().equals(cod.getCollectedByShipper().getId()))
                throw new IllegalStateException("COD source does not match this task");
            if(!Set.of("WAITING_COLLECTION","COLLECTED").contains(cod.getStatus())
                    || "COLLECTED".equals(cod.getStatus()) && (!method.equals(cod.getMethod()) || cod.getCollectedAt()==null))
                throw new IllegalStateException("COD collection conflicts with recorded evidence");
        }
        if(feeDue.signum()>0 && payment!=null) {
            if(payment.getAmount()==null || payment.getAmount().compareTo(feeDue)!=0 || !Set.of("UNPAID","PENDING","PAID").contains(payment.getStatus()))
                throw new IllegalStateException("Shipping collection does not match obligation");
            if("PAID".equals(payment.getStatus()) && (!feeMethod.equals(payment.getMethod()) || !actor.userId().equals(payment.getCollectedByUserId()) || payment.getPaidAt()==null))
                throw new IllegalStateException("Shipping collection conflicts with recorded evidence");
        }
        if(codDue.signum()>0 && (cod==null || "WAITING_COLLECTION".equals(cod.getStatus()))) {
            if(cod==null) cod=CodTransaction.createWaiting(shipment,shipper,codDue,method);
            cod.collect(method,shipper); codTransactionRepository.save(cod);
            shipmentEventRepository.save(ShipmentEvent.create(shipment,shipper,"COD_COLLECTED","COD collection recorded"));
        }
        if(feeDue.signum()>0 && (payment==null || !"PAID".equals(payment.getStatus()))) {
            if(payment==null) payment=Payment.create(shipment,"RECEIVER","SHIPPING_FEE",feeDue,feeMethod,"UNPAID");
            payment.collect(feeMethod,actor.userId(),"DELIVERY-FEE-"+shipment.getId()); paymentRepository.save(payment);
            shipmentEventRepository.save(ShipmentEvent.create(shipment,shipper,"RECEIVER_FEE_COLLECTED","Shipping collection recorded"));
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
            if (!"COMPLETED".equals(activeAssignment.getStatus())) throw new AccessDeniedException("Invalid completed task");
            return toTaskResponse(shipment, activeAssignment);
        }
        requireDeliveryCustody(shipment,activeAssignment,actor);

        if (!ShipmentStatus.OUT_FOR_DELIVERY.equals(shipment.getCurrentStatus())) {
            throw new IllegalStateException("Shipment must be OUT_FOR_DELIVERY to confirm delivered. Current: " + shipment.getCurrentStatus());
        }

        // Per D-03: Must collect COD before DELIVERED
        if (shipment.getCodAmount() != null && shipment.getCodAmount().compareTo(BigDecimal.ZERO) > 0) {
            Optional<CodTransaction> cod = codTransactionRepository.findByShipmentId(shipment.getId());
            if (cod.isEmpty() || !"COLLECTED".equals(cod.get().getStatus()) || cod.get().getAmount()==null
                    || cod.get().getAmount().compareTo(shipment.getCodAmount())!=0 || cod.get().getCollectedAt()==null
                    || cod.get().getCollectedByShipper()==null || !actor.userId().equals(cod.get().getCollectedByShipper().getId())
                    || !Set.of("CASH","VNPAY","TRANSFER").contains(cod.get().getMethod())) {
                throw new IllegalStateException("COD collection required before delivery confirmation");
            }
        }

        // Per D-03: Must collect receiver-paid shipping fee before DELIVERED
        if ("RECEIVER".equalsIgnoreCase(shipment.getFeePayer())
                && shipment.getShippingFee() != null && shipment.getShippingFee().compareTo(BigDecimal.ZERO) > 0) {
            Optional<Payment> payment = paymentRepository.findFirstByShipmentIdAndPayerTypeAndPaymentType(
                    shipment.getId(), "RECEIVER", "SHIPPING_FEE");
            if (payment.isEmpty() || !"PAID".equals(payment.get().getStatus()) || payment.get().getAmount()==null
                    || payment.get().getAmount().compareTo(shipment.getShippingFee())!=0
                    || !actor.userId().equals(payment.get().getCollectedByUserId())) {
                throw new IllegalStateException("Receiver shipping fee collection required before delivery confirmation");
            }
            ShipmentFeeService.requirePaidEvidence(payment.get());
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

        requireDeliveryCustody(shipment,activeAssignment,actor);
        if (codTransactionRepository.findByShipmentId(shipment.getId()).filter(c->"COLLECTED".equals(c.getStatus())).isPresent()
                || paymentRepository.findFirstByShipmentIdAndPayerTypeAndPaymentType(shipment.getId(),"RECEIVER","SHIPPING_FEE")
                .filter(p->"PAID".equals(p.getStatus())).isPresent())
            throw new IllegalStateException("Recorded collection must be reconciled before reporting failure");
        if("RESCHEDULED".equals(request.getReason()) && (request.getRescheduledDate()==null || !request.getRescheduledDate().isAfter(access.today())))
            throw new IllegalArgumentException("Select a future rescheduled date");
        String reason = request.getReason();
        Set<String> group1 = Set.of("NO_ANSWER", "PAYMENT_NOT_AVAILABLE", "RESCHEDULED", "OTHER");
        Set<String> group2 = Set.of("RECEIVER_REFUSED", "WRONG_ADDRESS");

        if (!group1.contains(reason) && !group2.contains(reason)) {
            throw new IllegalArgumentException("Invalid failure reason: " + reason);
        }

        long attemptCount = lastMileDeliveryRepository.countByShipmentId(shipment.getId());
        int nextAttempt = (int) attemptCount + 1;
        LocalDate today = access.today();

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
                // The shipper still has the parcel until inbound staff scan its physical return.
                shipment.transitionTo(ShipmentStatus.FAILED);
                shipment.setFailReason(reason);
                shipmentRepository.save(shipment);
                activeAssignment.setStatus("COMPLETED");
                activeAssignment.setCompletedAt(LocalDateTime.now());
                shipmentAssignmentRepository.save(activeAssignment);
                shipmentEventRepository.save(ShipmentEvent.create(shipment,shipper,"DELIVERY_ATTEMPT_FAILED","Delivery attempt failed; physical return pending"));
                if("RESCHEDULED".equals(reason)) shipmentEventRepository.save(ShipmentEvent.create(shipment,shipper,"DELIVERY_RESCHEDULED","Delivery rescheduled"));

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
        if(returnFee==null) returnFee=new BigDecimal("25000.00");
        if(returnFee.signum()<0) throw new IllegalStateException("Invalid return fee policy");
        shipment.setReturnFee(returnFee);
    }

    @Override
    public ReturnTaskResponse processReturnHandover(ReturnHandoverRequest request) {
        if(!"SELF_PICKUP".equals(request.getHandoverType())) throw new IllegalArgumentException("Use assigned return-delivery workflow");
        var actor=AuthenticatedActor.requireCurrent();
        var profile=access.require(UserRole.ROLE_HUB_STAFF,"INBOUND","GENERAL_STAFF");
        Shipment s=shipmentRepository.findByIdForUpdate(request.getShipmentId()).orElseThrow(()->new ResourceNotFoundException("Shipment"));
        if(!"RETURN".equals(s.getDirection()) || s.getOriginHubId()==null || !profile.getHubId().equals(s.getOriginHubId())) throw new AccessDeniedException("Wrong origin hub");
        if(s.getCurrentStatus()==ShipmentStatus.RETURNED_TO_SENDER) return toReturnResponse(s,true);
        access.requireAt(s,profile);
        if(s.getCurrentStatus()!=ShipmentStatus.RETURN_TO_SENDER || s.getReturnArrivedAtOriginAt()==null || shipmentRepository.hasActiveContainerCustody(s.getId())
                || shipmentAssignmentRepository.existsByShipmentIdAndStatusIn(s.getId(),List.of("ASSIGNED","ACCEPTED","IN_PROGRESS")))
            throw new IllegalStateException("Parcel is not available for sender pickup");
        if(request.getIdentityProof()==null || request.getIdentityProof().isBlank() || request.getIdentityProof().length()>255)
            throw new IllegalArgumentException("Record the verified sender identity reference");
        fees.requireReturnSettled(s);
        s.transitionTo(ShipmentStatus.RETURNED_TO_SENDER); s.setReturnedAt(LocalDateTime.now()); s.setCurrentHubId(null);
        shipmentEventRepository.save(ShipmentEvent.create(s,profile.getUser(),"RETURNED_TO_SENDER","Verified sender pickup at origin hub"));
        return toReturnResponse(s,true);
    }

    @Override
    public ReturnTaskResponse scanReturnDelivery(Long shipmentId) {
        var actor=AuthenticatedActor.requireCurrent(); var profile=access.require(UserRole.ROLE_SHIPPER);
        var s=shipmentRepository.findByIdForUpdate(shipmentId).orElseThrow(()->new ResourceNotFoundException("Shipment"));
        var a=returnAssignment(s,actor);
        if(s.isOnHold() || !"RETURN".equals(s.getDirection()) || !profile.getHubId().equals(s.getOriginHubId())) throw new AccessDeniedException("Wrong return task");
        if(s.getCurrentStatus()==ShipmentStatus.OUT_FOR_DELIVERY && "IN_PROGRESS".equals(a.getStatus()) && s.getCurrentHubId()==null) return toReturnResponse(s,true);
        access.requireAt(s,profile);
        if(s.getCurrentStatus()!=ShipmentStatus.RETURN_TO_SENDER || s.getReturnArrivedAtOriginAt()==null || shipmentRepository.hasActiveContainerCustody(s.getId()) || !"ASSIGNED".equals(a.getStatus()))
            throw new IllegalStateException("Return is not ready for outbound scan");
        a.setStatus("IN_PROGRESS");a.setAcceptedAt(LocalDateTime.now());s.setCurrentHubId(null);s.transitionTo(ShipmentStatus.OUT_FOR_DELIVERY);
        shipmentEventRepository.save(ShipmentEvent.create(s,profile.getUser(),"RETURN_OUT_FOR_DELIVERY","Return parcel scanned to assigned shipper"));
        return toReturnResponse(s,true);
    }

    @Override
    public ReturnTaskResponse confirmReturnDelivery(Long shipmentId) {
        var actor=AuthenticatedActor.requireCurrent();var profile=access.require(UserRole.ROLE_SHIPPER);
        var s=shipmentRepository.findByIdForUpdate(shipmentId).orElseThrow(()->new ResourceNotFoundException("Shipment"));
        var a=returnAssignment(s,actor);
        if(!"RETURN".equals(s.getDirection()) || !profile.getHubId().equals(s.getOriginHubId())) throw new AccessDeniedException("Wrong return task");
        if(s.getCurrentStatus()==ShipmentStatus.RETURNED_TO_SENDER && "COMPLETED".equals(a.getStatus())) return toReturnResponse(s,true);
        if(s.isOnHold() || s.getCurrentHubId()!=null || s.getCurrentStatus()!=ShipmentStatus.OUT_FOR_DELIVERY || !"IN_PROGRESS".equals(a.getStatus()) || a.getAcceptedAt()==null)
            throw new IllegalStateException("Return delivery requires scanned outbound custody");
        fees.requireReturnSettled(s);
        s.transitionTo(ShipmentStatus.RETURNED_TO_SENDER);s.setReturnedAt(LocalDateTime.now());a.complete(LocalDateTime.now());
        shipmentEventRepository.save(ShipmentEvent.create(s,profile.getUser(),"RETURNED_TO_SENDER","Return delivered by assigned shipper"));
        return toReturnResponse(s,true);
    }

    @Override
    public void receiveFailedDelivery(Long shipmentId,String trackingNumber) {
        var profile=access.require(UserRole.ROLE_HUB_STAFF,"INBOUND","GENERAL_STAFF");
        var s=shipmentRepository.findByIdForUpdate(shipmentId).orElseThrow(()->new ResourceNotFoundException("Shipment"));
        if(s.isOnHold() || !profile.getHubId().equals(s.getDestinationHubId()) || !s.getTrackingNumber().equals(trackingNumber)) throw new AccessDeniedException("Invalid returned parcel scan");
        if(profile.getHubId().equals(s.getCurrentHubId()) && shipmentEventRepository.existsByShipmentIdAndEventType(s.getId(),"RETURNED_TO_HUB")) return;
        if(s.getCurrentHubId()!=null || !(s.getCurrentStatus()==ShipmentStatus.FAILED || s.getCurrentStatus()==ShipmentStatus.RETURN_TO_SENDER))
            throw new IllegalStateException("Parcel is not awaiting physical return");
        var task=shipmentAssignmentRepository.findByShipmentIdOrderByAssignedAtDesc(s.getId()).stream().filter(a->"DELIVERY".equals(a.getAssignmentType())).findFirst()
                .orElseThrow(()->new IllegalStateException("No preceding delivery custody"));
        if(!"COMPLETED".equals(task.getStatus()) || task.getAcceptedAt()==null) throw new IllegalStateException("No completed failed delivery task");
        s.receiveAt(profile.getHub());
        shipmentEventRepository.save(ShipmentEvent.arrivedAtHub(s,profile.getUser(),profile.getHub()));
        shipmentEventRepository.save(ShipmentEvent.of(s,profile.getUser(),"RETURNED_TO_HUB","Physical return from shipper received"));
        if(s.getReturnArrivedAtOriginAt()!=null && !shipmentEventRepository.existsByShipmentIdAndEventType(s.getId(),"RETURNED_AT_ORIGIN_HUB"))
            shipmentEventRepository.save(ShipmentEvent.of(s,profile.getUser(),"RETURNED_AT_ORIGIN_HUB","Return parcel received at origin"));
    }

    private ShipmentAssignment returnAssignment(Shipment s,AuthenticatedActor actor) {
        return shipmentAssignmentRepository.findByShipmentIdOrderByAssignedAtDesc(s.getId()).stream()
                .filter(a->"RETURN_DELIVERY".equals(a.getAssignmentType()) && actor.userId().equals(a.getAssignee().getId()))
                .findFirst().orElseThrow(()->new AccessDeniedException("No assigned return task"));
    }
    private void requireDeliveryCustody(Shipment s,ShipmentAssignment a,AuthenticatedActor actor) {
        var profile=access.require(UserRole.ROLE_SHIPPER);
        if(s.isOnHold() || !"FORWARD".equals(s.getDirection()) || !profile.getHubId().equals(s.getDestinationHubId())
                || s.getCurrentHubId()!=null || !"DELIVERY".equals(a.getAssignmentType()) || !"IN_PROGRESS".equals(a.getStatus()) || a.getAcceptedAt()==null
                || !actor.userId().equals(a.getAssignee().getId())) throw new AccessDeniedException("Physical delivery custody required");
    }
    private static BigDecimal zero(BigDecimal a){return a==null?BigDecimal.ZERO:a;}
    private static void exactAmount(BigDecimal supplied,BigDecimal expected){
        if(expected.signum()<0 || zero(supplied).compareTo(expected)!=0) throw new IllegalArgumentException("Collection must match authoritative amount");
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryTaskResponse> getAssignedDeliveryTasks() {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        var profile=access.require(UserRole.ROLE_SHIPPER);
        List<ShipmentAssignment> assignments = shipmentAssignmentRepository.findByAssigneeIdAndStatusIn(
                actor.userId(), List.of("ASSIGNED", "ACCEPTED", "IN_PROGRESS"));

        return assignments.stream()
                .filter(a -> "DELIVERY".equals(a.getAssignmentType()))
                .filter(a -> profile.getHubId().equals(a.getShipment().getDestinationHubId()) && "FORWARD".equals(a.getShipment().getDirection()))
                .map(a -> toTaskResponse(a.getShipment(), a))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryTaskResponse getDeliveryTask(Long shipmentId) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        var profile=access.require(UserRole.ROLE_SHIPPER);
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));
        if(!profile.getHubId().equals(shipment.getDestinationHubId()) || !"FORWARD".equals(shipment.getDirection())) throw new AccessDeniedException("Wrong delivery hub");

        List<ShipmentAssignment> assignments = shipmentAssignmentRepository.findByShipmentIdOrderByAssignedAtDesc(shipment.getId());
        ShipmentAssignment assignment = assignments.stream()
                .filter(a -> a.getAssignee().getId().equals(actor.userId()) && "DELIVERY".equals(a.getAssignmentType()))
                .filter(a -> List.of("ASSIGNED", "ACCEPTED", "IN_PROGRESS").contains(a.getStatus()))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Not assigned to this shipment"));

        return toTaskResponse(shipment, assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnTaskResponse> getAssignedReturnTasks() {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        var profile=access.require(UserRole.ROLE_SHIPPER);
        List<ShipmentAssignment> assignments = shipmentAssignmentRepository.findByAssigneeIdAndStatusIn(
                actor.userId(), List.of("ASSIGNED", "ACCEPTED", "IN_PROGRESS"));

        return assignments.stream()
                .filter(a -> "RETURN_DELIVERY".equals(a.getAssignmentType()))
                .filter(a -> profile.getHubId().equals(a.getShipment().getOriginHubId()) && "RETURN".equals(a.getShipment().getDirection()))
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
        BigDecimal outstanding=fees.returnOutstanding(s);
        boolean returnFeePaid=outstanding.signum()==0;
        return new ReturnTaskResponse(
                s.getId(), s.getTrackingNumber(), s.getCurrentStatus().name(),
                s.getDirection(), s.getOriginHubId(), s.getSenderName(),
                s.getSenderPhone(), returnFee, outstanding, returnFeePaid,
                canCollect && returnFeePaid && !s.isOnHold() && s.getCurrentStatus()==ShipmentStatus.OUT_FOR_DELIVERY && s.getCurrentHubId()==null);
    }
}

