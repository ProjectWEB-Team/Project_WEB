package vn.iotstar.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.iotstar.dto.delivery.DeliveryFailureRequest;
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
import vn.iotstar.repository.CodTransactionRepository;
import vn.iotstar.repository.LastMileDeliveryRepository;
import vn.iotstar.repository.PaymentRepository;
import vn.iotstar.repository.ShipmentAssignmentRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.impl.LastMileDeliveryServiceImpl;
import vn.iotstar.config.LogisticsProperties;
import vn.iotstar.realtime.NotificationEventPublisher;
import vn.iotstar.service.StorageService;
import vn.iotstar.service.WalletLedgerService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LastMileReturnServiceImplTest {

    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private ShipmentAssignmentRepository shipmentAssignmentRepository;
    @Mock
    private LastMileDeliveryRepository lastMileDeliveryRepository;
    @Mock
    private CodTransactionRepository codTransactionRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ShipmentEventRepository shipmentEventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;
    @Mock
    private WalletLedgerService walletLedgerService;
    @Mock
    private StorageService storageService;

    private final LogisticsProperties logisticsProperties = new LogisticsProperties();

    private LastMileDeliveryServiceImpl deliveryService;

    private User shipper;
    private User sender;
    private Shipment shipment;
    private ShipmentAssignment assignment;

    @BeforeEach
    void setUp() {
        deliveryService = new LastMileDeliveryServiceImpl(
                shipmentRepository,
                shipmentAssignmentRepository,
                lastMileDeliveryRepository,
                codTransactionRepository,
                paymentRepository,
                shipmentEventRepository,
                userRepository,
                eventPublisher,
                notificationEventPublisher,
                walletLedgerService,
                logisticsProperties,
                storageService
        );

        shipper = new User(10L, "shipper_test", "hash", UserRole.ROLE_SHIPPER, "ACTIVE");
        sender = new User(1L, "sender_test", "hash", UserRole.ROLE_USER, "ACTIVE");

        shipment = Shipment.create("TRK789012", sender, "Sender", "0901", "Address 1", "Receiver", "0902", "Address 2");
        shipment.setId(1L);
        shipment.transitionTo(ShipmentStatus.OUT_FOR_DELIVERY);
        shipment.setCurrentHubId(100L);
        shipment.setOriginHubId(100L);
        shipment.setDestinationHubId(100L);
        shipment.setCodAmount(new BigDecimal("150000.00"));
        shipment.setReturnFee(new BigDecimal("20000.00"));

        assignment = ShipmentAssignment.create(shipment, shipper, "DELIVERY");
        assignment.setStatus("IN_PROGRESS");

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(shipper, null, shipper.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Group-1 failure: 1st attempt returns custody to delivery hub (AT_HUB) and schedules retry")
    void group1FirstFailureReturnsToHub() {
        when(shipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(10L)).thenReturn(Optional.of(shipper));
        when(shipmentAssignmentRepository.findByShipmentIdOrderByAssignedAtDesc(any())).thenReturn(List.of(assignment));
        when(lastMileDeliveryRepository.countByShipmentId(any())).thenReturn(0L);
        when(lastMileDeliveryRepository.countByShipmentIdAndAttemptDate(any(), any())).thenReturn(0L);

        DeliveryFailureRequest request = new DeliveryFailureRequest(
                1L, "NO_ANSWER", "Customer did not answer phone", "http://evidence.jpg", null);
        deliveryService.recordFailure(request);

        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.AT_HUB);
        assertThat(shipment.getFailReason()).isEqualTo("NO_ANSWER");
        assertThat(assignment.getStatus()).isEqualTo("COMPLETED");

        verify(lastMileDeliveryRepository).save(any(DeliveryAttempt.class));
        verify(shipmentAssignmentRepository, times(2)).save(any(ShipmentAssignment.class)); // Rescheduled assignment
        verify(shipmentEventRepository, atLeast(2)).save(any(ShipmentEvent.class));
    }

    @Test
    @DisplayName("Group-1 failure: 3rd attempt exhausts retries and transitions to RETURN_TO_SENDER")
    void group1ThirdFailureExhaustsToReturn() {
        when(shipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(10L)).thenReturn(Optional.of(shipper));
        when(shipmentAssignmentRepository.findByShipmentIdOrderByAssignedAtDesc(any())).thenReturn(List.of(assignment));
        when(lastMileDeliveryRepository.countByShipmentId(any())).thenReturn(2L); // 2 previous attempts
        when(lastMileDeliveryRepository.countByShipmentIdAndAttemptDate(any(), any())).thenReturn(0L);

        CodTransaction waitingCod = CodTransaction.createWaiting(shipment, shipper, new BigDecimal("150000.00"), "CASH");
        when(codTransactionRepository.findByShipmentId(any())).thenReturn(Optional.of(waitingCod));

        DeliveryFailureRequest request = new DeliveryFailureRequest(
                1L, "NO_ANSWER", "Third attempt no answer", "http://evidence.jpg", null);
        deliveryService.recordFailure(request);

        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.RETURN_TO_SENDER);
        assertThat(shipment.getFailReason()).isEqualTo("DELIVERY_ATTEMPTS_EXHAUSTED");
        assertThat(shipment.getDirection()).isEqualTo("RETURN");
        assertThat(waitingCod.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("Group-2 failure: RECEIVER_REFUSED fast-fails to FAILED then RETURN_TO_SENDER immediately")
    void group2FastFailToReturn() {
        when(shipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(10L)).thenReturn(Optional.of(shipper));
        when(shipmentAssignmentRepository.findByShipmentIdOrderByAssignedAtDesc(any())).thenReturn(List.of(assignment));
        when(lastMileDeliveryRepository.countByShipmentId(any())).thenReturn(0L);
        when(lastMileDeliveryRepository.countByShipmentIdAndAttemptDate(any(), any())).thenReturn(0L);

        CodTransaction waitingCod = CodTransaction.createWaiting(shipment, shipper, new BigDecimal("150000.00"), "CASH");
        when(codTransactionRepository.findByShipmentId(any())).thenReturn(Optional.of(waitingCod));

        DeliveryFailureRequest request = new DeliveryFailureRequest(
                1L, "RECEIVER_REFUSED", "Receiver refused parcel", "http://refusal.jpg", null);
        deliveryService.recordFailure(request);

        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.RETURN_TO_SENDER);
        assertThat(shipment.getFailReason()).isEqualTo("RECEIVER_REFUSED");
        assertThat(shipment.getDirection()).isEqualTo("RETURN");
        assertThat(waitingCod.getStatus()).isEqualTo("CANCELLED");
        verify(shipmentEventRepository, atLeast(2)).save(any(ShipmentEvent.class));
    }

    @Test
    @DisplayName("Origin hub self-pickup: Requires return fee settlement before RETURNED_TO_SENDER")
    void originHubSelfPickupRequiresFeeSettlement() {
        // Switch user to sender
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(sender, null, sender.getAuthorities()));

        shipment.transitionTo(ShipmentStatus.RETURN_TO_SENDER);
        when(shipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(paymentRepository.findFirstByShipmentIdAndPaymentTypeAndStatus(any(), eq("RETURN_FEE"), eq("PAID")))
                .thenReturn(Optional.empty()); // Unpaid return fee

        ReturnHandoverRequest request = new ReturnHandoverRequest(
                1L, "SELF_PICKUP", "CITIZEN_ID_12345", null, "Self pickup note");

        assertThatThrownBy(() -> deliveryService.processReturnHandover(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Return fee of 20000.00 must be settled before handover");
    }

    @Test
    @DisplayName("Origin hub self-pickup: Succeeds when return fee is settled")
    void originHubSelfPickupSuccessWhenFeeSettled() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(sender, null, sender.getAuthorities()));

        shipment.transitionTo(ShipmentStatus.RETURN_TO_SENDER);
        when(shipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));

        ReturnHandoverRequest request = new ReturnHandoverRequest(
                1L, "SELF_PICKUP", "CITIZEN_ID_12345", new BigDecimal("20000.00"), "Self pickup note");

        ReturnTaskResponse response = deliveryService.processReturnHandover(request);

        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.RETURNED_TO_SENDER);
        assertThat(shipment.getReturnedAt()).isNotNull();
        // The return fee payment and the handover are both recorded.
        verify(shipmentEventRepository, times(2)).save(any(ShipmentEvent.class));
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("Assigned RETURN delivery: Succeeds and transitions to RETURNED_TO_SENDER")
    void assignedReturnDeliverySuccess() {
        shipment.transitionTo(ShipmentStatus.RETURN_TO_SENDER);
        ShipmentAssignment returnAssignment = ShipmentAssignment.create(shipment, shipper, "RETURN_DELIVERY");
        returnAssignment.setStatus("IN_PROGRESS");

        when(shipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(10L)).thenReturn(Optional.of(shipper));
        when(shipmentAssignmentRepository.findFirstByShipmentIdAndAssigneeIdAndStatus(1L, 10L, "IN_PROGRESS"))
                .thenReturn(Optional.of(returnAssignment));

        ReturnTaskResponse response = deliveryService.confirmReturnDelivery(1L);

        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.RETURNED_TO_SENDER);
        assertThat(returnAssignment.getStatus()).isEqualTo("COMPLETED");
        verify(shipmentEventRepository).save(any(ShipmentEvent.class));
    }
}
