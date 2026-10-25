package vn.iotstar.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.entity.Payment;
import vn.iotstar.entity.RefundSettlementOutbox;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentAssignment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.realtime.ShipmentEventPublisher;
import vn.iotstar.repository.PaymentRepository;
import vn.iotstar.repository.RefundSettlementOutboxRepository;
import vn.iotstar.repository.ShipmentAssignmentRepository;
import vn.iotstar.repository.ShipmentEventRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentCancellationTest {
    @Mock private ShipmentAssignmentRepository assignmentRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RefundSettlementOutboxRepository outboxRepository;
    @Mock private ShipmentEventRepository eventRepository;
    @Mock private ShipmentEventPublisher publisher;
    private ShipmentCancellation cancellation;
    private final User sender = new User(7L, "sender", "hash", UserRole.ROLE_USER, "ACTIVE");
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        cancellation = new ShipmentCancellation(assignmentRepository, paymentRepository, outboxRepository, eventRepository,
                publisher, Clock.fixed(Instant.parse("2026-10-23T03:00:00Z"), ZoneId.systemDefault()));
        shipment = Shipment.create("UTX-ABC", sender, "Sender", "0900000001", "1 Street", "Receiver", "+84900000002", "2 Street");
        shipment.applyIntake(null, "HCM", "Q1", "HOME_PICKUP", "SENDER", "NO_INSPECTION", 500, 10, 10, 10,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("25000"), "PAID", ShipmentStatus.WAITING_FOR_HANDOVER,
                LocalDateTime.of(2026, 10, 22, 9, 0));
        ReflectionTestUtils.setField(shipment, "id", 42L);
    }

    @Test
    void paidSenderFeeCreatesOnePendingObligationAndKeepsPaymentPaid() {
        Payment paid = payment("SENDER", "VNPAY", "PAID");
        when(paymentRepository.findFirstByShipmentIdAndPaymentType(42L, "SHIPPING_FEE")).thenReturn(Optional.of(paid));

        cancellation.cancel(shipment, "SENDER_REQUEST", sender);

        ArgumentCaptor<RefundSettlementOutbox> outbox = ArgumentCaptor.forClass(RefundSettlementOutbox.class);
        verify(outboxRepository).save(outbox.capture());
        assertThat(outbox.getValue().getPaymentId()).isEqualTo(9L);
        assertThat(outbox.getValue().getAmount()).isEqualByComparingTo("25000");
        assertThat(outbox.getValue().getStatus()).isEqualTo("PENDING_WALLET_CREDIT");
        assertThat(paid.getStatus()).isEqualTo("PAID");
        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.CANCELLED);
        assertThat(shipment.getCancelReason()).isEqualTo("SENDER_REQUEST");
        verify(eventRepository).save(any(ShipmentEvent.class));
    }

    @Test
    void retryDoesNotDuplicateObligationAndUncollectedFeesCreateNone() {
        when(paymentRepository.findFirstByShipmentIdAndPaymentType(42L, "SHIPPING_FEE"))
                .thenReturn(Optional.of(payment("SENDER", "VNPAY", "PAID")));
        when(outboxRepository.existsByPaymentId(9L)).thenReturn(true);
        cancellation.cancel(shipment, "PICKUP_TIMEOUT", null);

        when(paymentRepository.findFirstByShipmentIdAndPaymentType(42L, "SHIPPING_FEE"))
                .thenReturn(Optional.of(payment("SENDER", "CASH", "UNPAID")));
        cancellation.cancel(shipment, "PICKUP_TIMEOUT", null);

        when(paymentRepository.findFirstByShipmentIdAndPaymentType(42L, "SHIPPING_FEE"))
                .thenReturn(Optional.of(payment("RECEIVER", "CASH", "UNPAID")));
        cancellation.cancel(shipment, "DROPOFF_TIMEOUT", null);

        verify(outboxRepository, never()).save(any());
    }

    @Test
    void cancelsActivePickupTaskAndNotifiesAssignedShipperAndSender() {
        User shipper = new User(30L, "shipper", "hash", UserRole.ROLE_SHIPPER, "ACTIVE");
        ShipmentAssignment task = ShipmentAssignment.pickup(shipment, shipper, LocalDateTime.of(2026, 10, 22, 10, 0));
        when(assignmentRepository.findFirstByShipmentIdAndAssignmentTypeAndStatus(42L, "PICKUP", "ASSIGNED"))
                .thenReturn(Optional.of(task));

        cancellation.cancel(shipment, "SENDER_REQUEST", sender);

        assertThat(task.isActive()).isFalse();
        verify(publisher).publishToUserAfterCommit(eq("shipper"), any());
        verify(publisher).publishToUserAfterCommit(eq("sender"), any());
    }

    private Payment payment(String payer, String method, String status) {
        Payment payment = Payment.shippingFee(shipment, payer, new BigDecimal("25000"), method, status);
        ReflectionTestUtils.setField(payment, "id", 9L);
        return payment;
    }
}
