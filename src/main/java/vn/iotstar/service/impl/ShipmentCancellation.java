package vn.iotstar.service.impl;

import org.springframework.stereotype.Component;
import vn.iotstar.dto.realtime.ShipmentCreatedEventResponse;
import vn.iotstar.entity.RefundSettlementOutbox;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentAssignment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.User;
import vn.iotstar.realtime.ShipmentEventPublisher;
import vn.iotstar.repository.PaymentRepository;
import vn.iotstar.repository.RefundSettlementOutboxRepository;
import vn.iotstar.repository.ShipmentAssignmentRepository;
import vn.iotstar.repository.ShipmentEventRepository;

import java.time.Clock;
import java.time.LocalDateTime;

/** Shared cancellation step for sender, coordinator and timeout paths; runs inside the caller's transaction. */
@Component
public class ShipmentCancellation {
    private final ShipmentAssignmentRepository assignmentRepository;
    private final PaymentRepository paymentRepository;
    private final RefundSettlementOutboxRepository outboxRepository;
    private final ShipmentEventRepository eventRepository;
    private final ShipmentEventPublisher publisher;
    private final Clock clock;

    public ShipmentCancellation(ShipmentAssignmentRepository assignmentRepository, PaymentRepository paymentRepository,
                                RefundSettlementOutboxRepository outboxRepository, ShipmentEventRepository eventRepository,
                                ShipmentEventPublisher publisher, Clock clock) {
        this.assignmentRepository = assignmentRepository;
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.eventRepository = eventRepository;
        this.publisher = publisher;
        this.clock = clock;
    }

    /** Caller holds the shipment row lock and has checked the state; a null actor marks an automated event. */
    public void cancel(Shipment shipment, String reason, User actor) {
        LocalDateTime now = ServerTime.now(clock);
        shipment.cancel(reason);
        assignmentRepository.findFirstByShipmentIdAndAssignmentTypeAndStatus(shipment.getId(),
                ShipmentAssignment.PICKUP, ShipmentAssignment.ASSIGNED).ifPresent(assignment -> {
            assignment.cancel();
            notify(assignment.getAssignee().getUsername(), shipment, now);
        });
        // Only a sender-paid fee creates an obligation; unpaid cash or receiver-paid fees were never collected.
        paymentRepository.findFirstByShipmentIdAndPaymentType(shipment.getId(), "SHIPPING_FEE")
                .filter(p -> "PAID".equals(p.getStatus()) && "SENDER".equals(p.getPayerType()))
                .filter(p -> !outboxRepository.existsByPaymentId(p.getId()))
                .ifPresent(p -> outboxRepository.save(RefundSettlementOutbox.pending(shipment, p, reason, now)));
        eventRepository.save(ShipmentEvent.of(shipment, actor, "SHIPMENT_CANCELLED", "Cancelled: " + reason));
        notify(shipment.getSender().getUsername(), shipment, now);
    }

    private void notify(String username, Shipment shipment, LocalDateTime now) {
        publisher.publishToUserAfterCommit(username, new ShipmentCreatedEventResponse(shipment.getTrackingNumber(),
                shipment.getCurrentStatus(), "SHIPMENT_CANCELLED", now));
    }
}
