package vn.iotstar.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.dto.driver.TripDelayCommand;
import vn.iotstar.dto.realtime.OperationalActionNotification;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Handover;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.Incident;
import vn.iotstar.entity.TransportTrip;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.HubOperationAccessDeniedException;
import vn.iotstar.repository.HubOperationsRepository;
import vn.iotstar.repository.LinehaulOperationsRepository;
import vn.iotstar.repository.OperationalIncidentRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.impl.LinehaulTripServiceImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinehaulTripServiceImplTest {
    @Mock private LinehaulOperationsRepository operationsRepository;
    @Mock private HubOperationsRepository hubOperationsRepository;
    @Mock private OperationalIncidentRepository incidentRepository;
    @Mock private ShipmentEventRepository shipmentEventRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @InjectMocks private LinehaulTripServiceImpl service;
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void assignedDriverFollowsAcceptedStartArriveWithoutCompletingHandover() {
        User driver = user(9L, "driver"); Hub origin = hub(1L); Hub destination = hub(2L);
        Handover handover = acceptedHandover(origin, destination, driver); authenticate(driver);
        when(hubOperationsRepository.findActiveProfileByUserId(9L)).thenReturn(Optional.of(EmployeeProfile.active(driver, origin, "LINEHAUL_DRIVER")));
        when(operationsRepository.findHandoverForUpdate(30L)).thenReturn(Optional.of(handover));
        when(userRepository.getReferenceById(9L)).thenReturn(driver);
        doAnswer(invocation -> { ReflectionTestUtils.setField(invocation.<Object>getArgument(0), "id", 40L); return null; }).when(operationsRepository).persist(any());

        assertThat(service.acceptHandover(30L).status()).isEqualTo("ACCEPTED");
        TransportTrip trip = handover.getTransportTrip();
        when(operationsRepository.findTripForUpdate(40L)).thenReturn(Optional.of(trip));
        assertThat(service.startTrip(40L).status()).isEqualTo("STARTED");
        assertThat(service.arriveTrip(40L).status()).isEqualTo("ARRIVED");
        assertThat(handover.getStatus()).isEqualTo("ACCEPTED");
        ArgumentCaptor<OperationalActionNotification> notifications = ArgumentCaptor.forClass(OperationalActionNotification.class);
        verify(applicationEventPublisher, times(2)).publishEvent(notifications.capture());
        assertThat(notifications.getAllValues()).extracting(OperationalActionNotification::type)
                .containsExactly("DESTINATION_ARRIVAL", "INBOUND_READY_CONTAINER");
    }

    @Test void otherDriverCannotStartAssignedTripAndDelayIsBoundedToActiveTrip() {
        User driver = user(9L, "driver"); User other = user(10L, "other"); Hub origin = hub(1L); Hub destination = hub(2L);
        Handover handover = acceptedHandover(origin, destination, driver); ReflectionTestUtils.setField(handover, "id", 30L);
        TransportTrip trip = TransportTrip.accepted(handover, driver); ReflectionTestUtils.setField(trip, "id", 40L); handover.linkTransportTrip(trip);
        authenticate(other);
        when(hubOperationsRepository.findActiveProfileByUserId(10L)).thenReturn(Optional.of(EmployeeProfile.active(other, origin, "LINEHAUL_DRIVER")));
        when(operationsRepository.findTripForUpdate(40L)).thenReturn(Optional.of(trip));
        assertThatThrownBy(() -> service.startTrip(40L)).isInstanceOf(HubOperationAccessDeniedException.class);
        authenticate(driver);
        when(hubOperationsRepository.findActiveProfileByUserId(9L)).thenReturn(Optional.of(EmployeeProfile.active(driver, origin, "LINEHAUL_DRIVER")));
        when(userRepository.getReferenceById(9L)).thenReturn(driver);
        trip.start(driver);
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> { ReflectionTestUtils.setField(i.<Incident>getArgument(0), "id", 77L); return i.<Incident>getArgument(0); });
        assertThat(service.reportTripDelay(new TripDelayCommand(40L, "Weather closure", "INC-1"))).isEqualTo(77L);
        assertThatThrownBy(() -> service.reportTripDelay(new TripDelayCommand(40L, " ", null))).isInstanceOf(HubOperationAccessDeniedException.class);
    }
    private static User user(Long id, String name) { return new User(id, name, "hash", UserRole.ROLE_LINEHAUL_DRIVER, "ACTIVE"); }
    private static Hub hub(Long id) { Hub hub = Hub.active("H" + id, "H" + id, 1, "HUB", "SOUTH", "Can Tho"); ReflectionTestUtils.setField(hub, "id", id); return hub; }
    private static Handover acceptedHandover(Hub origin, Hub destination, User driver) { Handover h = Handover.pending(origin, destination, EmployeeProfile.active(driver, origin, "OUTBOUND"), driver, 1, null); ReflectionTestUtils.setField(h, "id", 30L); h.recordContainerScan(); return h; }
    private static void authenticate(User user) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())); }
}
