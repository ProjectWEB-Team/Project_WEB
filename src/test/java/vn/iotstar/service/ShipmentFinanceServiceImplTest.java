package vn.iotstar.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.iotstar.entity.Incident;
import vn.iotstar.entity.Payment;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.realtime.NotificationEventPublisher;
import vn.iotstar.repository.IncidentRepository;
import vn.iotstar.repository.PaymentRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.impl.ShipmentFinanceServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentFinanceServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private IncidentRepository incidentRepository;
    @Mock
    private ShipmentEventRepository shipmentEventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletLedgerService walletLedgerService;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    private ShipmentFinanceServiceImpl financeService;

    private User manager;
    private User sender;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        financeService = new ShipmentFinanceServiceImpl(
                paymentRepository,
                shipmentRepository,
                incidentRepository,
                shipmentEventRepository,
                userRepository,
                walletLedgerService,
                notificationEventPublisher
        );

        manager = new User(100L, "manager_test", "hash", UserRole.ROLE_MANAGER, "ACTIVE");
        sender = new User(1L, "sender_test", "hash", UserRole.ROLE_USER, "ACTIVE");

        shipment = Shipment.create("TRK555666", sender, "Sender", "0901", "Address 1", "Receiver", "0902", "Address 2");
        shipment.setId(1L);
        shipment.setShippingFee(new BigDecimal("35000.00"));
        shipment.setDeclaredValue(new BigDecimal("500000.00"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(manager, null, manager.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Role check: A regular user cannot refund a payment")
    void regularUserCannotRefund() {
        User regularUser = new User(2L, "user_reg", "hash", UserRole.ROLE_USER, "ACTIVE");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(regularUser, null, regularUser.getAuthorities()));

        assertThatThrownBy(() -> financeService.refundShipmentPayment(50L, "Cancelled"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ROLE_ADMIN or ROLE_MANAGER required");
    }

    @Test
    @DisplayName("Refund: credits the payer wallet, marks the payment refunded and the shipment payment status")
    void refundCreditsWallet() {
        shipment.transitionTo(ShipmentStatus.CANCELLED);
        Payment payment = Payment.create(shipment, "SENDER", "SHIPPING_FEE",
                new BigDecimal("35000.00"), "VNPAY", "PAID");
        payment.setId(50L);

        when(paymentRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payment result = financeService.refundShipmentPayment(50L, "Sender requested cancellation");

        assertThat(result.getStatus()).isEqualTo("REFUNDED");
        assertThat(result.getRefundedAt()).isNotNull();
        assertThat(shipment.getPaymentStatus()).isEqualTo("REFUNDED");

        verify(walletLedgerService).creditRefund(eq(1L), eq(new BigDecimal("35000.00")), eq(50L), eq(1L),
                any());
        verify(shipmentEventRepository).save(any());
    }

    @Test
    @DisplayName("Refund: a payment of a shipment that is not cancelled is rejected")
    void refundRequiresCancelledShipment() {
        Payment payment = Payment.create(shipment, "SENDER", "SHIPPING_FEE",
                new BigDecimal("35000.00"), "VNPAY", "PAID");
        payment.setId(50L);
        when(paymentRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> financeService.refundShipmentPayment(50L, "Too early"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only cancelled shipments can be refunded");
    }

    @Test
    @DisplayName("Refund: an already refunded payment is returned unchanged")
    void refundIsIdempotent() {
        shipment.transitionTo(ShipmentStatus.CANCELLED);
        Payment payment = Payment.create(shipment, "SENDER", "SHIPPING_FEE",
                new BigDecimal("35000.00"), "VNPAY", "REFUNDED");
        payment.setId(50L);
        when(paymentRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(payment));

        Payment result = financeService.refundShipmentPayment(50L, "Retry");

        assertThat(result.getStatus()).isEqualTo("REFUNDED");
        verify(walletLedgerService, never()).creditRefund(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Compensation: a lost shipment closes as CLOSED_EXCEPTION and credits the sender")
    void compensateLostShipment() {
        shipment.setFeePayer("SENDER");
        shipment.transitionTo(ShipmentStatus.IN_TRANSIT);
        shipment.setOnHold(true);

        Incident incident = Incident.create(1L, null, 5L, "MISSING_PARCEL",
                "Parcel not found after investigation", 100L);
        incident.setId(9L);

        when(shipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(shipment));
        when(incidentRepository.findById(9L)).thenReturn(Optional.of(incident));
        when(userRepository.findById(100L)).thenReturn(Optional.of(manager));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Shipment result = financeService.compensateLostShipment(1L, 9L, "Confirmed lost");

        assertThat(result.getCurrentStatus()).isEqualTo(ShipmentStatus.CLOSED_EXCEPTION);
        assertThat(result.getClosureReason()).isEqualTo("LOST");
        assertThat(incident.getStatus()).isEqualTo("RESOLVED");

        // declared value 500.000 + forward fee 35.000 paid by the sender
        verify(walletLedgerService).creditCompensation(eq(1L), eq(new BigDecimal("535000.00")), eq(9L), any());
    }

    @Test
    @DisplayName("Compensation: a receiver payer gets the declared value only")
    void compensationForReceiverPayer() {
        shipment.setFeePayer("RECEIVER");
        shipment.transitionTo(ShipmentStatus.IN_TRANSIT);

        Incident incident = Incident.create(1L, null, 5L, "MISSING_PARCEL", "Lost", 100L);
        incident.setId(9L);

        when(shipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(shipment));
        when(incidentRepository.findById(9L)).thenReturn(Optional.of(incident));
        when(userRepository.findById(100L)).thenReturn(Optional.of(manager));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        financeService.compensateLostShipment(1L, 9L, "Confirmed lost");

        verify(walletLedgerService).creditCompensation(eq(1L), eq(new BigDecimal("500000.00")), eq(9L), any());
    }

    @Test
    @DisplayName("Pending refunds: only paid shipping fees of cancelled shipments are listed")
    void pendingRefundCandidates() {
        Payment paidCancelled = Payment.create(shipment, "SENDER", "SHIPPING_FEE",
                new BigDecimal("35000.00"), "VNPAY", "PAID");
        shipment.transitionTo(ShipmentStatus.CANCELLED);

        when(paymentRepository.findByStatusAndPaymentType("PAID", "SHIPPING_FEE"))
                .thenReturn(List.of(paidCancelled));

        List<Payment> candidates = financeService.listPendingRefundCandidates();

        assertThat(candidates).containsExactly(paidCancelled);
    }
}
