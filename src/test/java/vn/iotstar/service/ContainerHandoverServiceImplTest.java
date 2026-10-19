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
import vn.iotstar.dto.hub.LoadContainerCommand;
import vn.iotstar.dto.hub.CorrectHandoverCommand;
import vn.iotstar.dto.hub.ScanHandoverContainerCommand;
import vn.iotstar.dto.realtime.OperationalActionNotification;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Handover;
import vn.iotstar.entity.HandoverContainer;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.ParcelContainer;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.ShipmentRoute;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.repository.ContainerHandoverRepository;
import vn.iotstar.repository.HubOperationsRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.impl.ContainerHandoverServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** Service-level proofs for container custody, server-side scopes and accepted-scan idempotency. */
@ExtendWith(MockitoExtension.class)
class ContainerHandoverServiceImplTest {
    @Mock private ContainerHandoverRepository custodyRepository;
    @Mock private HubOperationsRepository hubOperationsRepository;
    @Mock private ShipmentEventRepository shipmentEventRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @InjectMocks private ContainerHandoverServiceImpl service;

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test
    void openContainerStartsWithCanonicalCustodyStatus() {
        Hub origin = hub(11L, "HUB-A");
        ParcelContainer container = ParcelContainer.open("BAG-001", origin, hub(12L, "SOC-B"), user(7L, "outbound", UserRole.ROLE_HUB_STAFF));
        assertThat(container.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void outboundStaffLoadsOnlyCurrentRoutedShipmentAndRecordsOperationalEvidence() {
        User staff = user(7L, "outbound", UserRole.ROLE_HUB_STAFF);
        User sender = user(8L, "sender", UserRole.ROLE_USER);
        Hub origin = hub(11L, "HUB-A"); Hub destination = hub(12L, "SOC-B");
        Shipment shipment = Shipment.create("UTE-1", sender, "S", "1", "A", "R", "2", "B");
        ReflectionTestUtils.setField(shipment, "id", 101L); ReflectionTestUtils.setField(shipment, "currentStatus", ShipmentStatus.AT_HUB); ReflectionTestUtils.setField(shipment, "currentHub", origin);
        ParcelContainer container = ParcelContainer.open("BAG-001", origin, destination, staff); ReflectionTestUtils.setField(container, "id", 201L);
        authenticate(staff);
        when(hubOperationsRepository.findActiveProfileByUserId(7L)).thenReturn(Optional.of(EmployeeProfile.active(staff, origin, "OUTBOUND")));
        when(custodyRepository.findContainerForUpdate(201L)).thenReturn(Optional.of(container));
        when(custodyRepository.findShipmentForUpdate(101L)).thenReturn(Optional.of(shipment));
        when(custodyRepository.findPendingRouteForShipmentAtHub(101L, 11L)).thenReturn(Optional.of(ShipmentRoute.decided(shipment, origin, destination, "shipment:101:hub:11")));
        when(custodyRepository.containerAlreadyContains(201L, 101L)).thenReturn(false);
        when(userRepository.getReferenceById(7L)).thenReturn(staff);

        service.loadShipment(new LoadContainerCommand(201L, 101L));

        verify(custodyRepository).persist(any(vn.iotstar.entity.ContainerItem.class));
        ArgumentCaptor<ShipmentEvent> events = ArgumentCaptor.forClass(ShipmentEvent.class);
        verify(shipmentEventRepository, times(2)).save(events.capture());
        assertThat(events.getAllValues()).extracting(ShipmentEvent::getEventType).containsExactly("BAG_CREATED", "OUTBOUND_SCANNED");
    }

    @Test
    void outboundLoadingUsesOnlyThePendingRouteForTheShipmentCurrentHubOnSecondHop() {
        User staff = user(7L, "outbound", UserRole.ROLE_HUB_STAFF);
        User sender = user(8L, "sender", UserRole.ROLE_USER);
        Hub hubA = hub(11L, "HUB-A"); Hub hubB = hub(12L, "HUB-B"); Hub hubC = hub(13L, "HUB-C");
        Shipment shipment = Shipment.create("UTE-TWO-HOP", sender, "S", "1", "A", "R", "2", "B");
        ReflectionTestUtils.setField(shipment, "id", 101L);
        shipment.receiveAt(hubB);
        ParcelContainer container = ParcelContainer.open("BAG-B-C", hubB, hubC, staff);
        ReflectionTestUtils.setField(container, "id", 201L);
        ShipmentRoute oldHop = ShipmentRoute.decided(shipment, hubA, hubB, "shipment:101:hub:11");
        ShipmentRoute currentHop = ShipmentRoute.decided(shipment, hubB, hubC, "shipment:101:hub:12");
        authenticate(staff);
        when(hubOperationsRepository.findActiveProfileByUserId(7L)).thenReturn(Optional.of(EmployeeProfile.active(staff, hubB, "OUTBOUND")));
        when(custodyRepository.findContainerForUpdate(201L)).thenReturn(Optional.of(container));
        when(custodyRepository.findShipmentForUpdate(101L)).thenReturn(Optional.of(shipment));
        when(custodyRepository.findPendingRouteForShipmentAtHub(101L, 12L)).thenReturn(Optional.of(currentHop));
        when(custodyRepository.containerAlreadyContains(201L, 101L)).thenReturn(false);
        when(userRepository.getReferenceById(7L)).thenReturn(staff);

        service.loadShipment(new LoadContainerCommand(201L, 101L));

        verify(custodyRepository).findPendingRouteForShipmentAtHub(101L, 12L);
        verify(custodyRepository, never()).findPendingRouteForShipmentAtHub(101L, 11L);
        assertThat(currentHop.getStatus()).isEqualTo("LOADED");
        assertThat(oldHop.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void assignedDriverMustScanTheExpectedContainerSetAndRepeatIsIdempotent() {
        User staff = user(7L, "outbound", UserRole.ROLE_HUB_STAFF);
        User driver = user(9L, "driver", UserRole.ROLE_LINEHAUL_DRIVER);
        Hub origin = hub(11L, "HUB-A"); Hub destination = hub(12L, "SOC-B");
        Handover handover = Handover.pending(origin, destination, EmployeeProfile.active(staff, origin, "OUTBOUND"), driver, 1, null);
        ReflectionTestUtils.setField(handover, "id", 301L);
        ParcelContainer container = ParcelContainer.open("BAG-001", origin, destination, staff); ReflectionTestUtils.setField(container, "id", 201L);
        HandoverContainer expected = HandoverContainer.expected(handover, container);
        authenticate(driver);
        when(custodyRepository.findHandoverForUpdate(301L)).thenReturn(Optional.of(handover));
        when(custodyRepository.findHandoverContainer(301L, 201L)).thenReturn(Optional.of(expected));

        assertThat(service.scanAndAccept(new ScanHandoverContainerCommand(301L, 201L)).accepted()).isTrue();
        assertThat(service.scanAndAccept(new ScanHandoverContainerCommand(301L, 201L)).acceptedContainers()).isEqualTo(1);

        ArgumentCaptor<OperationalActionNotification> notification = ArgumentCaptor.forClass(OperationalActionNotification.class);
        verify(applicationEventPublisher, times(1)).publishEvent(notification.capture());
        assertThat(notification.getValue().type()).isEqualTo("HANDOVER_ACCEPTED");
    }

    @Test
    void acceptedCustodyIsCorrectedThroughCancellationAndReplacementNotMutation() {
        User staff = user(7L, "outbound", UserRole.ROLE_HUB_STAFF);
        User driver = user(9L, "driver", UserRole.ROLE_LINEHAUL_DRIVER);
        Hub origin = hub(11L, "HUB-A"); Hub destination = hub(12L, "SOC-B");
        EmployeeProfile profile = EmployeeProfile.active(staff, origin, "OUTBOUND");
        Handover original = Handover.pending(origin, destination, profile, driver, 1, null);
        ReflectionTestUtils.setField(original, "id", 301L); original.recordContainerScan();
        ParcelContainer replacementContainer = ParcelContainer.open("BAG-NEW", origin, destination, staff);
        ReflectionTestUtils.setField(replacementContainer, "id", 202L);
        authenticate(staff);
        when(hubOperationsRepository.findActiveProfileByUserId(7L)).thenReturn(Optional.of(profile));
        when(custodyRepository.findHandoverForUpdate(301L)).thenReturn(Optional.of(original));
        when(custodyRepository.findContainerForUpdate(202L)).thenReturn(Optional.of(replacementContainer));
        when(custodyRepository.findActiveCustodyForContainer(202L)).thenReturn(List.of());

        service.correctByCancellationAndRecreation(new CorrectHandoverCommand(301L, List.of(202L)));

        assertThat(original.getStatus()).isEqualTo("CANCELLED");
        assertThat(original.getReplacementHandover()).isNotNull();
        assertThat(original.getReplacementHandover().getReplacesHandover()).isSameAs(original);
        assertThat(replacementContainer.getStatus()).isEqualTo("SEALED");
        verify(custodyRepository).persist(any(HandoverContainer.class));
    }

    private static User user(Long id, String username, UserRole role) { return new User(id, username, "hash", role, "ACTIVE"); }
    private static Hub hub(Long id, String code) { Hub hub = Hub.active(code, code, 1, "HUB", "SOUTH", "Can Tho"); ReflectionTestUtils.setField(hub, "id", id); return hub; }
    private static void authenticate(User user) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())); }
}
