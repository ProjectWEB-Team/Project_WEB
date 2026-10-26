package vn.iotstar.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.config.LogisticsProperties;
import vn.iotstar.dto.driver.TripDelayCommand;
import vn.iotstar.dto.realtime.OperationalActionNotification;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Handover;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.Incident;
import vn.iotstar.entity.TransportTrip;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.Vehicle;
import vn.iotstar.entity.ParcelContainer;
import vn.iotstar.entity.HandoverContainer;
import vn.iotstar.entity.ContainerItem;
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
    private final LogisticsProperties logisticsProperties = new LogisticsProperties();
    private LinehaulTripServiceImpl service;
    @BeforeEach void createService() {
        service = new LinehaulTripServiceImpl(operationsRepository, hubOperationsRepository, incidentRepository,
                shipmentEventRepository, userRepository, applicationEventPublisher, logisticsProperties);
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void assignedDriverFollowsAcceptedStartArriveWithoutCompletingHandover() {
        User driver = user(9L, "driver"); Hub origin = hub(1L); Hub destination = hub(2L);
        Handover handover = acceptedHandover(origin, destination, driver); authenticate(driver);
        when(hubOperationsRepository.findActiveProfileByUserId(9L)).thenReturn(Optional.of(EmployeeProfile.active(driver, origin, "LINEHAUL_DRIVER")));
        when(operationsRepository.findHandoverForUpdate(30L)).thenReturn(Optional.of(handover));
        Vehicle vehicle = vehicle();
        when(operationsRepository.findVehicleForUpdate(50L)).thenReturn(Optional.of(vehicle));
        when(userRepository.getReferenceById(9L)).thenReturn(driver);
        Shipment parcel = Shipment.create("T-DEPART", driver, "S", "1", "A", "R", "2", "B");
        ReflectionTestUtils.setField(parcel, "id", 70L);
        parcel.receiveAt(origin);
        when(operationsRepository.findShipmentsForHandover(30L)).thenReturn(java.util.List.of(parcel));
        ParcelContainer container = ParcelContainer.open("BAG", origin, destination, driver); container.seal();
        HandoverContainer membership = HandoverContainer.expected(handover, container); membership.markScanned();
        when(operationsRepository.findContainersForUpdate(30L)).thenReturn(java.util.List.of(membership));
        when(operationsRepository.findManifestItemsForUpdate(30L)).thenReturn(java.util.List.of(ContainerItem.scanned(container, parcel)));
        doAnswer(invocation -> { ReflectionTestUtils.setField(invocation.<Object>getArgument(0), "id", 40L); return null; }).when(operationsRepository).persist(any());

        assertThat(service.acceptHandover(30L, 50L).status()).isEqualTo("SCHEDULED");
        TransportTrip trip = handover.getTransportTrip();
        when(operationsRepository.findTripForUpdate(40L)).thenReturn(Optional.of(trip));
        assertThat(service.startTrip(40L).status()).isEqualTo("IN_TRANSIT");
        assertThat(parcel.getCurrentStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(parcel.getCurrentHubId()).isNull();
        assertThat(vehicle.getStatus()).isEqualTo("IN_USE");
        assertThat(service.startTrip(40L).status()).isEqualTo("IN_TRANSIT");
        assertThat(service.arriveTrip(40L).status()).isEqualTo("ARRIVED");
        assertThat(handover.getStatus()).isEqualTo("IN_TRANSIT");
        ArgumentCaptor<OperationalActionNotification> notifications = ArgumentCaptor.forClass(OperationalActionNotification.class);
        verify(applicationEventPublisher, times(2)).publishEvent(notifications.capture());
        assertThat(notifications.getAllValues()).extracting(OperationalActionNotification::type)
                .containsExactly("DESTINATION_ARRIVAL", "INBOUND_READY_CONTAINER");
    }

    @Test void otherDriverCannotStartAssignedTripAndDelayIsBoundedToActiveTrip() {
        User driver = user(9L, "driver"); User other = user(10L, "other"); Hub origin = hub(1L); Hub destination = hub(2L);
        Handover handover = acceptedHandover(origin, destination, driver); ReflectionTestUtils.setField(handover, "id", 30L);
        TransportTrip trip = TransportTrip.accepted(handover, driver, vehicle()); ReflectionTestUtils.setField(trip, "id", 40L); handover.linkTransportTrip(trip);
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
        ArgumentCaptor<Incident> incidentCaptor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(incidentCaptor.capture());
        Incident incident = incidentCaptor.getValue();
        assertThat(incident.getShipmentId()).isNull();
        assertThat(incident.getTripId()).isEqualTo(40L);
        assertThat(incident.getHubId()).isEqualTo(1L);
        assertThat(incident.getReportedBy()).isEqualTo(9L);
        assertThat(incident.getType()).isEqualTo("TRIP_DELAY");
        assertThat(incident.getStatus()).isEqualTo("OPEN");
        assertThat(incident.getDescription()).contains("Weather closure", "INC-1");
        assertThatThrownBy(() -> service.reportTripDelay(new TripDelayCommand(40L, " ", null))).isInstanceOf(HubOperationAccessDeniedException.class);
    }

    // --- Demo auto linehaul transit simulation (Workflow section 101, D-08) ---

    @Test void withoutTheFlagStartOnlyDepartsAndEmitsNoArrival() {
        Ready r = readyTrip();
        assertThat(service.startTrip(40L).status()).isEqualTo("IN_TRANSIT");
        assertThat(eventTypes()).containsExactly("LINEHAUL_TRIP_STARTED");
        verifyNoInteractions(applicationEventPublisher);
        assertThat(service.arriveTrip(40L).status()).isEqualTo("ARRIVED");
    }

    @Test void withTheFlagStartPerformsTheNormalArrivalInTheSameCall() {
        Ready r = readyTrip();
        logisticsProperties.getDemo().setAutoLinehaulTransitSimulation(true);

        assertThat(service.startTrip(40L).status()).isEqualTo("ARRIVED");

        assertThat(r.trip().getStatus()).isEqualTo("ARRIVED");
        assertThat(eventTypes()).containsExactly("LINEHAUL_TRIP_STARTED", "LINEHAUL_ARRIVED");
        ArgumentCaptor<OperationalActionNotification> notifications = ArgumentCaptor.forClass(OperationalActionNotification.class);
        verify(applicationEventPublisher, times(2)).publishEvent(notifications.capture());
        assertThat(notifications.getAllValues()).extracting(OperationalActionNotification::type)
                .containsExactly("DESTINATION_ARRIVAL", "INBOUND_READY_CONTAINER");
        assertThat(notifications.getAllValues()).allMatch(n -> n.currentHubId().equals(2L));
    }

    @Test void simulatedArrivalKeepsCustodyOpenForDestinationReceipt() {
        Ready r = readyTrip();
        logisticsProperties.getDemo().setAutoLinehaulTransitSimulation(true);

        service.startTrip(40L);

        assertThat(r.trip().getHandover().getStatus()).isEqualTo("IN_TRANSIT");
        assertThat(r.parcel().getCurrentStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(r.parcel().getCurrentHubId()).isNull();
        assertThat(r.vehicle().getStatus()).isEqualTo("IN_USE");
    }

    @Test void simulationNeverLetsAnUnassignedDriverStartTheTrip() {
        Ready r = readyTrip();
        logisticsProperties.getDemo().setAutoLinehaulTransitSimulation(true);
        User other = user(10L, "other");
        authenticate(other);
        when(hubOperationsRepository.findActiveProfileByUserId(10L)).thenReturn(Optional.of(EmployeeProfile.active(other, hub(1L), "LINEHAUL_DRIVER")));

        assertThatThrownBy(() -> service.startTrip(40L)).isInstanceOf(HubOperationAccessDeniedException.class);
        verifyNoInteractions(shipmentEventRepository, applicationEventPublisher);
        assertThat(r.trip().getStatus()).isEqualTo("SCHEDULED");
    }

    @Test void repeatedStartAfterSimulatedArrivalEmitsNothingNew() {
        readyTrip();
        logisticsProperties.getDemo().setAutoLinehaulTransitSimulation(true);
        service.startTrip(40L);
        clearInvocations(shipmentEventRepository, applicationEventPublisher);

        assertThat(service.startTrip(40L).status()).isEqualTo("ARRIVED");
        verifyNoInteractions(shipmentEventRepository, applicationEventPublisher);
    }

    private java.util.List<String> eventTypes() {
        ArgumentCaptor<vn.iotstar.entity.ShipmentEvent> events = ArgumentCaptor.forClass(vn.iotstar.entity.ShipmentEvent.class);
        verify(shipmentEventRepository, atLeastOnce()).save(events.capture());
        return events.getAllValues().stream().map(vn.iotstar.entity.ShipmentEvent::getEventType).toList();
    }

    /** A scheduled trip with one sealed, scanned container holding one parcel at the origin hub. */
    private Ready readyTrip() {
        User driver = user(9L, "driver"); Hub origin = hub(1L); Hub destination = hub(2L);
        Handover handover = acceptedHandover(origin, destination, driver);
        Vehicle vehicle = vehicle(); vehicle.reserve();
        TransportTrip trip = TransportTrip.accepted(handover, driver, vehicle); ReflectionTestUtils.setField(trip, "id", 40L);
        handover.linkTransportTrip(trip);
        Shipment parcel = Shipment.create("T-DEMO", driver, "S", "1", "A", "R", "2", "B");
        ReflectionTestUtils.setField(parcel, "id", 70L);
        parcel.receiveAt(origin);
        ParcelContainer container = ParcelContainer.open("BAG", origin, destination, driver); container.seal();
        HandoverContainer membership = HandoverContainer.expected(handover, container); membership.markScanned();
        authenticate(driver);
        lenient().when(hubOperationsRepository.findActiveProfileByUserId(9L)).thenReturn(Optional.of(EmployeeProfile.active(driver, origin, "LINEHAUL_DRIVER")));
        when(operationsRepository.findTripForUpdate(40L)).thenReturn(Optional.of(trip));
        lenient().when(operationsRepository.findContainersForUpdate(30L)).thenReturn(java.util.List.of(membership));
        lenient().when(operationsRepository.findManifestItemsForUpdate(30L)).thenReturn(java.util.List.of(ContainerItem.scanned(container, parcel)));
        lenient().when(operationsRepository.findShipmentsForHandover(30L)).thenReturn(java.util.List.of(parcel));
        lenient().when(userRepository.getReferenceById(9L)).thenReturn(driver);
        return new Ready(trip, parcel, vehicle);
    }
    private record Ready(TransportTrip trip, Shipment parcel, Vehicle vehicle) { }
    private static User user(Long id, String name) { return new User(id, name, "hash", UserRole.ROLE_LINEHAUL_DRIVER, "ACTIVE"); }
    private static Vehicle vehicle() { Vehicle v = Vehicle.available("51C-123", "TRUCK", 1000); ReflectionTestUtils.setField(v, "id", 50L); return v; }
    private static Hub hub(Long id) { Hub hub = Hub.active("H" + id, "H" + id, 1, "HUB", "SOUTH", "Can Tho"); ReflectionTestUtils.setField(hub, "id", id); return hub; }
    private static Handover acceptedHandover(Hub origin, Hub destination, User driver) { Handover h = Handover.pending(origin, destination, EmployeeProfile.active(driver, origin, "OUTBOUND"), driver, 1, null); ReflectionTestUtils.setField(h, "id", 30L); h.recordContainerScan(); return h; }
    private static void authenticate(User user) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())); }
}
