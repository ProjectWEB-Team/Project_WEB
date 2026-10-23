package vn.iotstar.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Payment;
import vn.iotstar.entity.PickupAttempt;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentAssignment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.ShipmentAccessDeniedException;
import vn.iotstar.realtime.ShipmentEventPublisher;
import vn.iotstar.repository.EmployeeProfileRepository;
import vn.iotstar.repository.PaymentRepository;
import vn.iotstar.repository.PickupAttemptRepository;
import vn.iotstar.repository.ShipmentAssignmentRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirstMileServiceImplTest {
    private static final long HUB = 1L;
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private ShipmentAssignmentRepository assignmentRepository;
    @Mock private PickupAttemptRepository attemptRepository;
    @Mock private EmployeeProfileRepository profileRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ShipmentEventRepository eventRepository;
    @Mock private ShipmentEventPublisher publisher;
    @Mock private ShipmentCancellation cancellation;
    private FirstMileServiceImpl service;

    private final User sender = user(7L, "sender", UserRole.ROLE_USER);
    private final User coordinatorUser = user(20L, "coord", UserRole.ROLE_HUB_STAFF);
    private final User shipperUser = user(30L, "shipper", UserRole.ROLE_SHIPPER);

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-23T03:00:00Z"), ZoneId.systemDefault());
        service = new FirstMileServiceImpl(shipmentRepository, assignmentRepository, attemptRepository, profileRepository,
                paymentRepository, eventRepository, publisher, cancellation, clock);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assignsSameHubShipperAndBindsShipmentToCoordinatorHub() {
        login(coordinatorUser, profile(coordinatorUser, HUB, "COORDINATOR"));
        Shipment shipment = homePickup();
        when(shipmentRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(shipment));
        when(profileRepository.findFirstByUserId(30L)).thenReturn(Optional.of(profile(shipperUser, HUB, "PICKUP_SHIPPER")));

        service.assignPickup(42L, 30L);

        assertThat(shipment.getOriginHubId()).isEqualTo(HUB);
        verify(assignmentRepository).save(any(ShipmentAssignment.class));
        ArgumentCaptor<ShipmentEvent> event = ArgumentCaptor.forClass(ShipmentEvent.class);
        verify(eventRepository).save(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo("PICKUP_TASK_ASSIGNED");
        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.WAITING_FOR_HANDOVER);
    }

    @Test
    void rejectsShipperFromAnotherHub() {
        login(coordinatorUser, profile(coordinatorUser, HUB, "COORDINATOR"));
        when(shipmentRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(homePickup()));
        when(profileRepository.findFirstByUserId(30L)).thenReturn(Optional.of(profile(shipperUser, 2L, "PICKUP_SHIPPER")));

        assertThatThrownBy(() -> service.assignPickup(42L, 30L)).isInstanceOf(IllegalArgumentException.class);
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void rejectsCoordinatorWithWrongPositionOrOtherHubShipment() {
        login(coordinatorUser, profile(coordinatorUser, HUB, "SORTER"));
        assertThatThrownBy(() -> service.assignPickup(42L, 30L)).isInstanceOf(ShipmentAccessDeniedException.class);

        login(coordinatorUser, profile(coordinatorUser, HUB, "COORDINATOR"));
        Shipment otherHub = homePickup();
        otherHub.assignOriginHub(2L);
        when(shipmentRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(otherHub));
        assertThatThrownBy(() -> service.assignPickup(42L, 30L)).isInstanceOf(ShipmentAccessDeniedException.class);
    }

    @Test
    void completionCollectsSenderCashOnceAndMarksPickedUp() {
        login(shipperUser, profile(shipperUser, HUB, "PICKUP_SHIPPER"));
        Shipment shipment = homePickup();
        shipment.assignOriginHub(HUB);
        ShipmentAssignment task = task(shipment);
        Payment cash = Payment.shippingFee(shipment, "SENDER", new BigDecimal("25000"), "CASH", "UNPAID");
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(task));
        when(shipmentRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(shipment));
        when(paymentRepository.findFirstByShipmentIdAndPaymentType(42L, "SHIPPING_FEE")).thenReturn(Optional.of(cash));

        service.completePickup(5L, " utx-abc ");

        assertThat(cash.getStatus()).isEqualTo("PAID");
        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.PICKED_UP);
        assertThat(shipment.getPaymentStatus()).isEqualTo("PAID");
        assertThat(shipment.getCurrentHubId()).isEqualTo(HUB);
        assertThat(task.isActive()).isFalse();
        verify(attemptRepository).save(any(PickupAttempt.class));
        verify(publisher).publishToUserAfterCommit(eq("sender"), any());
    }

    @Test
    void completionLosesToAnEarlierCancellationAndRejectsWrongScan() {
        login(shipperUser, profile(shipperUser, HUB, "PICKUP_SHIPPER"));
        Shipment shipment = homePickup();
        shipment.assignOriginHub(HUB);
        ShipmentAssignment task = task(shipment);
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(task));
        when(shipmentRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> service.completePickup(5L, "UTX-OTHER")).isInstanceOf(IllegalArgumentException.class);

        shipment.cancel("SENDER_REQUEST");
        assertThatThrownBy(() -> service.completePickup(5L, "UTX-ABC")).isInstanceOf(IllegalArgumentException.class);
        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.CANCELLED);
    }

    @Test
    void anotherShipperCannotTouchTheTask() {
        User other = user(31L, "other", UserRole.ROLE_SHIPPER);
        login(other, profile(other, HUB, "PICKUP_SHIPPER"));
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(task(homePickup())));

        assertThatThrownBy(() -> service.recordFailedAttempt(5L, "FAILED_NO_ANSWER", null))
                .isInstanceOf(ShipmentAccessDeniedException.class);
    }

    @Test
    void serviceFailureCancelsThroughSharedCancellation() {
        login(coordinatorUser, profile(coordinatorUser, HUB, "GENERAL_STAFF"));
        Shipment shipment = homePickup();
        when(shipmentRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(shipment));

        service.reportServiceFailure(42L, "Xe hỏng");

        verify(cancellation).cancel(shipment, "PICKUP_SERVICE_FAILURE", coordinatorUser);
        assertThatThrownBy(() -> service.reportServiceFailure(42L, " ")).isInstanceOf(IllegalArgumentException.class);
        verify(eventRepository).save(any(ShipmentEvent.class));
        verify(publisher, never()).publishToUserAfterCommit(anyString(), any());
    }

    private Shipment homePickup() {
        Shipment shipment = Shipment.create("UTX-ABC", sender, "Sender", "0900000001", "1 Street", "Receiver",
                "+84900000002", "2 Street");
        shipment.applyIntake(null, "HCM", "Q1", "HOME_PICKUP", "SENDER", "NO_INSPECTION", 500, 10, 10, 10,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("25000"), "UNPAID", ShipmentStatus.WAITING_FOR_HANDOVER,
                LocalDateTime.of(2026, 10, 22, 9, 0));
        ReflectionTestUtils.setField(shipment, "id", 42L);
        return shipment;
    }

    private ShipmentAssignment task(Shipment shipment) {
        ShipmentAssignment task = ShipmentAssignment.pickup(shipment, shipperUser, LocalDateTime.of(2026, 10, 22, 10, 0));
        ReflectionTestUtils.setField(task, "id", 5L);
        return task;
    }

    private void login(User user, EmployeeProfile profile) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(profileRepository.findFirstByUserId(user.getId())).thenReturn(Optional.of(profile));
    }

    private static EmployeeProfile profile(User user, Long hubId, String position) {
        return new EmployeeProfile(user.getId(), user, hubId, position, "ACTIVE");
    }

    private static User user(Long id, String username, UserRole role) {
        return new User(id, username, "hash", role, "ACTIVE");
    }
}
