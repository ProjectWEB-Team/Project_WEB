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
import vn.iotstar.dto.hub.DestinationReceiptCommand;
import vn.iotstar.dto.hub.DestinationReconciliationCommand;
import vn.iotstar.entity.*;
import vn.iotstar.repository.*;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.impl.DestinationReceiptServiceImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DestinationReceiptServiceImplTest {
    @Mock private DestinationReceiptRepository receiptRepository;
    @Mock private HubOperationsRepository hubOperationsRepository;
    @Mock private ShipmentEventRepository shipmentEventRepository;
    @Mock private OperationalIncidentRepository incidentRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @InjectMocks private DestinationReceiptServiceImpl service;
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void destinationInboundReceiptIsIdempotentAndRequiresDriverArrival() {
        User inbound = new User(11L, "inbound", "hash", UserRole.ROLE_HUB_STAFF, "ACTIVE");
        User driver = new User(12L, "driver", "hash", UserRole.ROLE_LINEHAUL_DRIVER, "ACTIVE");
        Hub origin = hub(1L); Hub destination = hub(2L); authenticate(inbound);
        EmployeeProfile profile = EmployeeProfile.active(inbound, destination, "INBOUND");
        when(hubOperationsRepository.findActiveProfileByUserId(11L)).thenReturn(Optional.of(profile));
        Handover handover = acceptedArrivedHandover(origin, destination, driver);
        ParcelContainer container = ParcelContainer.open("C-1", origin, destination, inbound); ReflectionTestUtils.setField(container, "id", 30L);
        Shipment shipment = Shipment.create("T-1", inbound, "S", "1", "A", "R", "2", "B"); ReflectionTestUtils.setField(shipment, "id", 40L);
        ContainerItem item = ContainerItem.scanned(container, shipment);
        when(receiptRepository.findArrivedHandoverForUpdate(20L)).thenReturn(Optional.of(handover));
        when(receiptRepository.findHandoverContainer(20L, 30L)).thenReturn(Optional.of(HandoverContainer.expected(handover, container)));
        when(receiptRepository.findItemForUpdate(30L, 40L)).thenReturn(Optional.of(item));
        when(receiptRepository.findHandoverContainers(20L)).thenReturn(java.util.List.of());
        when(userRepository.getReferenceById(11L)).thenReturn(inbound);

        assertThat(service.receiveItem(new DestinationReceiptCommand(20L, 30L, 40L)).alreadyReceived()).isFalse();
        assertThat(service.receiveItem(new DestinationReceiptCommand(20L, 30L, 40L)).alreadyReceived()).isTrue();
        assertThat(item.isReceived()).isTrue();
        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.AT_HUB);
        verify(shipmentEventRepository, times(1)).save(any(ShipmentEvent.class));
        assertThat(handover.getStatus()).isEqualTo("ACCEPTED");
    }

    @Test void destinationReceiptCreatesCanonicalMissingParcelIncidentForOnlyTheMissingShipment() {
        User inbound = new User(11L, "inbound", "hash", UserRole.ROLE_HUB_STAFF, "ACTIVE");
        User driver = new User(12L, "driver", "hash", UserRole.ROLE_LINEHAUL_DRIVER, "ACTIVE");
        Hub origin = hub(1L); Hub destination = hub(2L); authenticate(inbound);
        EmployeeProfile profile = EmployeeProfile.active(inbound, destination, "INBOUND");
        when(hubOperationsRepository.findActiveProfileByUserId(11L)).thenReturn(Optional.of(profile));
        Handover handover = acceptedArrivedHandover(origin, destination, driver);
        ParcelContainer container = ParcelContainer.open("C-1", origin, destination, inbound);
        ReflectionTestUtils.setField(container, "id", 30L);
        Shipment shipment = Shipment.create("T-1", inbound, "S", "1", "A", "R", "2", "B");
        ReflectionTestUtils.setField(shipment, "id", 40L);
        ContainerItem missingItem = ContainerItem.scanned(container, shipment);
        when(receiptRepository.findArrivedHandoverForUpdate(20L)).thenReturn(Optional.of(handover));
        when(receiptRepository.findHandoverContainer(20L, 30L)).thenReturn(Optional.of(HandoverContainer.expected(handover, container)));
        when(receiptRepository.findItemsForContainerForUpdate(30L)).thenReturn(java.util.List.of(missingItem));
        when(receiptRepository.findHandoverContainers(20L)).thenReturn(java.util.List.of());
        when(userRepository.getReferenceById(11L)).thenReturn(inbound);
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> {
            Incident incident = invocation.getArgument(0);
            ReflectionTestUtils.setField(incident, "id", 77L);
            return incident;
        });

        service.reconcileContainer(new DestinationReconciliationCommand(20L, 30L, java.util.List.of()));

        ArgumentCaptor<Incident> incidentCaptor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(incidentCaptor.capture());
        Incident incident = incidentCaptor.getValue();
        assertThat(missingItem.isMissing()).isTrue();
        assertThat(incident.getShipmentId()).isEqualTo(40L);
        assertThat(incident.getTripId()).isEqualTo(21L);
        assertThat(incident.getHubId()).isEqualTo(2L);
        assertThat(incident.getReportedBy()).isEqualTo(11L);
        assertThat(incident.getType()).isEqualTo("MISSING_PARCEL");
        assertThat(incident.getStatus()).isEqualTo("INVESTIGATING");
        assertThat(incident.getDescription()).contains("receipt:handover:20:container:30:shipment:40");
    }

    private static Handover acceptedArrivedHandover(Hub origin, Hub destination, User driver) {
        Handover handover = Handover.pending(origin, destination, EmployeeProfile.active(driver, origin, "OUTBOUND"), driver, 1, null);
        ReflectionTestUtils.setField(handover, "id", 20L); handover.recordContainerScan();
        TransportTrip trip = TransportTrip.accepted(handover, driver); ReflectionTestUtils.setField(trip, "id", 21L); trip.start(driver); trip.arrive(driver); handover.linkTransportTrip(trip);
        return handover;
    }
    private static Hub hub(Long id) { Hub hub = Hub.active("H" + id, "H" + id, 1, "HUB", "S", "P"); ReflectionTestUtils.setField(hub, "id", id); return hub; }
    private static void authenticate(User user) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())); }
}
