package vn.iotstar.service.impl;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.realtime.NotificationEventResponse;
import vn.iotstar.entity.Incident;
import vn.iotstar.entity.Payment;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.realtime.NotificationEventPublisher;
import vn.iotstar.repository.IncidentRepository;
import vn.iotstar.repository.PaymentRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.ShipmentFinanceService;
import vn.iotstar.service.WalletLedgerService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reference-safe financial adjustments for the e-wallet.
 *
 * <p>Every credit is guarded by the reference key of the wallet ledger, so a
 * retried request can never pay the same obligation twice.
 */
@Service
@Transactional
public class ShipmentFinanceServiceImpl implements ShipmentFinanceService {

    private final PaymentRepository paymentRepository;
    private final ShipmentRepository shipmentRepository;
    private final IncidentRepository incidentRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final UserRepository userRepository;
    private final WalletLedgerService walletLedgerService;
    private final NotificationEventPublisher notificationEventPublisher;

    public ShipmentFinanceServiceImpl(PaymentRepository paymentRepository,
                                       ShipmentRepository shipmentRepository,
                                       IncidentRepository incidentRepository,
                                       ShipmentEventRepository shipmentEventRepository,
                                       UserRepository userRepository,
                                       WalletLedgerService walletLedgerService,
                                       NotificationEventPublisher notificationEventPublisher) {
        this.paymentRepository = paymentRepository;
        this.shipmentRepository = shipmentRepository;
        this.incidentRepository = incidentRepository;
        this.shipmentEventRepository = shipmentEventRepository;
        this.userRepository = userRepository;
        this.walletLedgerService = walletLedgerService;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    @Override
    public Payment refundShipmentPayment(Long paymentId, String reason) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (!actor.hasRole(UserRole.ROLE_ADMIN) && !actor.hasRole(UserRole.ROLE_MANAGER)) {
            throw new AccessDeniedException("ROLE_ADMIN or ROLE_MANAGER required to refund a payment");
        }

        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if ("REFUNDED".equals(payment.getStatus())) {
            return payment;
        }
        if (!"PAID".equals(payment.getStatus())) {
            throw new IllegalStateException("Only PAID payments can be refunded. Current: " + payment.getStatus());
        }

        Shipment shipment = payment.getShipment();
        if (!ShipmentStatus.CANCELLED.equals(shipment.getCurrentStatus())) {
            throw new IllegalStateException("Only cancelled shipments can be refunded. Current: "
                    + shipment.getCurrentStatus());
        }

        User payer = userRepository.findById(shipment.getSender().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        walletLedgerService.creditRefund(payer.getId(), payment.getAmount(), payment.getId(), shipment.getId(),
                "Refund for payment #" + payment.getId() + (reason != null ? " - " + reason : ""));

        payment.setStatus("REFUNDED");
        payment.setRefundedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        shipment.setPaymentStatus("REFUNDED");
        shipmentRepository.save(shipment);

        shipmentEventRepository.save(ShipmentEvent.create(
                shipment, payer, "REFUND_COMPLETED",
                "Shipping fee refunded to wallet for shipment " + shipment.getTrackingNumber()));

        notificationEventPublisher.publishAfterCommit(actor, new NotificationEventResponse(
                "REFUND_COMPLETED", shipment.getTrackingNumber(), shipment.getCurrentStatus().name(),
                payment.getAmount().toPlainString(), "PAYMENT#" + payment.getId(),
                "Shipping fee refunded to your e-wallet", LocalDateTime.now()));

        return payment;
    }

    @Override
    public Shipment compensateLostShipment(Long shipmentId, Long incidentId, String reason) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (!actor.hasRole(UserRole.ROLE_ADMIN) && !actor.hasRole(UserRole.ROLE_MANAGER)) {
            throw new AccessDeniedException("ROLE_ADMIN or ROLE_MANAGER required to compensate a lost shipment");
        }

        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));

        if (ShipmentStatus.CLOSED_EXCEPTION.equals(shipment.getCurrentStatus())
                && "LOST".equals(shipment.getClosureReason())) {
            return shipment;
        }

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentId));

        User manager = userRepository.findById(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found: " + actor.userId()));

        BigDecimal compensation = calculateLostCompensation(shipment);
        if (compensation.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Shipment has no compensable value");
        }

        User sender = userRepository.findById(shipment.getSender().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        walletLedgerService.creditCompensation(sender.getId(), compensation, incident.getId(),
                "Compensation for lost shipment " + shipment.getTrackingNumber());

        shipment.transitionTo(ShipmentStatus.CLOSED_EXCEPTION);
        shipment.setClosureReason("LOST");
        shipment = shipmentRepository.save(shipment);

        incident.setStatus("RESOLVED");
        incident.setResolvedAt(LocalDateTime.now());
        incidentRepository.save(incident);

        shipmentEventRepository.save(ShipmentEvent.create(
                shipment, manager, "COMPENSATION_CREDITED",
                "Compensation credited for lost shipment " + shipment.getTrackingNumber()));

        notificationEventPublisher.publishAfterCommit(actor, new NotificationEventResponse(
                "COMPENSATION_CREDITED", shipment.getTrackingNumber(), shipment.getCurrentStatus().name(),
                compensation.toPlainString(), "INCIDENT#" + incident.getId(),
                "Compensation credited to your e-wallet", LocalDateTime.now()));

        return shipment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> listPendingRefundCandidates() {
        return paymentRepository.findByStatusAndPaymentType("PAID", "SHIPPING_FEE").stream()
                .filter(payment -> ShipmentStatus.CANCELLED.equals(payment.getShipment().getCurrentStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateLostCompensation(Shipment shipment) {
        BigDecimal compensation = shipment.getDeclaredValue() != null
                ? shipment.getDeclaredValue()
                : BigDecimal.ZERO;

        boolean senderPaidForwardFee = "SENDER".equalsIgnoreCase(shipment.getFeePayer())
                && shipment.getShippingFee() != null
                && shipment.getShippingFee().compareTo(BigDecimal.ZERO) > 0;

        if (senderPaidForwardFee) {
            compensation = compensation.add(shipment.getShippingFee());
        }
        return compensation;
    }
}