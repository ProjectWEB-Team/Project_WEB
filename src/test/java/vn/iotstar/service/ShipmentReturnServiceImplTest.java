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
import vn.iotstar.config.LogisticsProperties;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.impl.ShipmentReturnServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentReturnServiceImplTest {

    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private ShipmentEventRepository shipmentEventRepository;
    @Mock
    private UserRepository userRepository;

    private LogisticsProperties logisticsProperties;
    private ShipmentReturnServiceImpl returnService;

    private User manager;
    private User sender;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        logisticsProperties = new LogisticsProperties();
        returnService = new ShipmentReturnServiceImpl(
                shipmentRepository, shipmentEventRepository, userRepository, logisticsProperties);

        manager = new User(100L, "manager_test", "hash", UserRole.ROLE_MANAGER, "ACTIVE");
        sender = new User(1L, "sender_test", "hash", UserRole.ROLE_USER, "ACTIVE");

        shipment = Shipment.create("TRK777888", sender, "Sender", "0901", "Address 1", "Receiver", "0902", "Address 2");
        shipment.setId(1L);
        shipment.transitionTo(ShipmentStatus.RETURN_TO_SENDER);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(manager, null, manager.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Role check: a regular user cannot receive a return at the hub")
    void regularUserCannotReceiveReturn() {
        User regularUser = new User(2L, "user_reg", "hash", UserRole.ROLE_USER, "ACTIVE");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(regularUser, null, regularUser.getAuthorities()));

        assertThatThrownBy(() -> returnService.markReturnArrivedAtOriginHub(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Origin hub receipt: records the arrival event that starts the grace period")
    void markReturnArrivedAtOriginHubRecordsEvent() {
        when(shipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(100L)).thenReturn(Optional.of(manager));

        returnService.markReturnArrivedAtOriginHub(1L);

        verify(shipmentEventRepository).save(any(ShipmentEvent.class));
    }

    @Test
    @DisplayName("Origin hub receipt: rejects a shipment that is not on its way back")
    void markReturnArrivedRejectsWrongStatus() {
        shipment.transitionTo(ShipmentStatus.OUT_FOR_DELIVERY);
        when(shipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> returnService.markReturnArrivedAtOriginHub(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be RETURN_TO_SENDER");
    }

    @Test
    @DisplayName("Grace period: a parcel that arrived long ago is overdue")
    void overdueReturnedShipmentIsDetected() {
        ShipmentEvent arrival = mock(ShipmentEvent.class);
        when(arrival.getCreatedAt()).thenReturn(LocalDateTime.now().minusDays(10));
        when(shipmentEventRepository.findFirstByShipmentIdAndEventTypeOrderByCreatedAtDesc(
                1L, ShipmentReturnServiceImpl.EVENT_RETURNED_AT_ORIGIN_HUB)).thenReturn(Optional.of(arrival));
        when(shipmentRepository.findByCurrentStatus(ShipmentStatus.RETURN_TO_SENDER))
                .thenReturn(List.of(shipment));

        List<Shipment> overdue = returnService.findOverdueReturnedShipments(LocalDateTime.now());

        assertThat(overdue).containsExactly(shipment);
    }

    @Test
    @DisplayName("Grace period: a recently arrived parcel is not overdue")
    void recentReturnIsNotOverdue() {
        ShipmentEvent arrival = mock(ShipmentEvent.class);
        when(arrival.getCreatedAt()).thenReturn(LocalDateTime.now().minusHours(2));
        when(shipmentEventRepository.findFirstByShipmentIdAndEventTypeOrderByCreatedAtDesc(
                1L, ShipmentReturnServiceImpl.EVENT_RETURNED_AT_ORIGIN_HUB)).thenReturn(Optional.of(arrival));
        when(shipmentRepository.findByCurrentStatus(ShipmentStatus.RETURN_TO_SENDER))
                .thenReturn(List.of(shipment));

        assertThat(returnService.findOverdueReturnedShipments(LocalDateTime.now())).isEmpty();
    }

    @Test
    @DisplayName("Grace period: the demo profile shortens the grace period to minutes")
    void demoProfileShortensGracePeriod() {
        logisticsProperties.getTimeout().setDisposedGraceDays(7);
        logisticsProperties.getTimeout().setDisposedGraceMinutes(2);

        ShipmentEvent arrival = mock(ShipmentEvent.class);
        when(arrival.getCreatedAt()).thenReturn(LocalDateTime.now().minusMinutes(5));
        when(shipmentEventRepository.findFirstByShipmentIdAndEventTypeOrderByCreatedAtDesc(
                1L, ShipmentReturnServiceImpl.EVENT_RETURNED_AT_ORIGIN_HUB)).thenReturn(Optional.of(arrival));
        when(shipmentRepository.findByCurrentStatus(ShipmentStatus.RETURN_TO_SENDER))
                .thenReturn(List.of(shipment));

        assertThat(returnService.findOverdueReturnedShipments(LocalDateTime.now())).containsExactly(shipment);
    }

    @Test
    @DisplayName("Disposal: a manager closes an overdue return as a disposed exception")
    void disposeOverdueReturnClosesShipment() {
        when(shipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(100L)).thenReturn(Optional.of(manager));
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Shipment result = returnService.disposeOverdueReturn(1L, "Sender never collected the parcel");

        assertThat(result.getCurrentStatus()).isEqualTo(ShipmentStatus.CLOSED_EXCEPTION);
        assertThat(result.getClosureReason()).isEqualTo("DISPOSED");
        verify(shipmentEventRepository).save(any(ShipmentEvent.class));
    }

    @Test
    @DisplayName("Disposal: a regular user cannot dispose a returned parcel")
    void regularUserCannotDispose() {
        User regularUser = new User(2L, "user_reg", "hash", UserRole.ROLE_USER, "ACTIVE");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(regularUser, null, regularUser.getAuthorities()));

        assertThatThrownBy(() -> returnService.disposeOverdueReturn(1L, "Not allowed"))
                .isInstanceOf(AccessDeniedException.class);
    }
}