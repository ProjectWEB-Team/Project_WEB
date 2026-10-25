package vn.iotstar.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.dto.operations.AuditCustodyReference;
import vn.iotstar.dto.operations.OperationsAuditTimelineResponse;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.HubOperationAccessDeniedException;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.repository.OperationsAuditRepository;
import vn.iotstar.service.impl.OperationsAuditServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationalAuditServiceImplTest {
    @Mock private OperationsAuditRepository operationsAuditRepository;
    @InjectMocks private OperationsAuditServiceImpl service;
    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test void managerReceivesOnlyMinimalTimelineForShipmentInOwnSubtree() {
        User manager = user(20L, UserRole.ROLE_MANAGER); User hubStaff = user(21L, UserRole.ROLE_HUB_STAFF);
        Hub root = hub(1L, "ROOT"); Hub descendant = hub(2L, "CHILD"); Shipment shipment = shipment(501L, descendant);
        authenticate(manager);
        when(operationsAuditRepository.findShipmentWithCurrentHub(501L)).thenReturn(Optional.of(shipment));
        when(operationsAuditRepository.findActiveProfileByUserId(20L)).thenReturn(Optional.of(EmployeeProfile.active(manager, root, "MANAGER")));
        when(operationsAuditRepository.isHubWithinSubtree(1L, 2L)).thenReturn(true);
        when(operationsAuditRepository.findCustodyReferences(501L)).thenReturn(List.of(new AuditCustodyReference(33L, 44L, 55L, 99L)));
        when(operationsAuditRepository.findEventsForShipment(501L)).thenReturn(List.of(ShipmentEvent.arrivedAtHub(shipment, hubStaff, descendant)));

        OperationsAuditTimelineResponse response = service.getTimeline(501L);

        assertThat(response.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.eventType()).isEqualTo("ARRIVED_AT_HUB");
            assertThat(entry.hubCode()).isEqualTo("CHILD");
            assertThat(entry.actorRole()).isEqualTo("ROLE_HUB_STAFF");
            assertThat(entry.containerId()).isEqualTo(33L);
            assertThat(entry.handoverId()).isEqualTo(44L);
            assertThat(entry.tripId()).isEqualTo(55L);
        });
    }

    @Test void siblingOrOutsideSubtreeIsSafeNotFoundAndNoEventsAreRead() {
        User manager = user(20L, UserRole.ROLE_MANAGER); Hub root = hub(1L, "ROOT"); Shipment shipment = shipment(502L, hub(3L, "SIBLING"));
        authenticate(manager);
        when(operationsAuditRepository.findShipmentWithCurrentHub(502L)).thenReturn(Optional.of(shipment));
        when(operationsAuditRepository.findCustodyReferences(502L)).thenReturn(List.of());
        when(operationsAuditRepository.findActiveProfileByUserId(20L)).thenReturn(Optional.of(EmployeeProfile.active(manager, root, "MANAGER")));
        when(operationsAuditRepository.isHubWithinSubtree(1L, 3L)).thenReturn(false);
        assertThatThrownBy(() -> service.getTimeline(502L)).isInstanceOf(ResourceNotFoundException.class);
        verify(operationsAuditRepository, never()).findEventsForShipment(anyLong());
    }

    @Test void nonOperationalUserIsDeniedBeforeShipmentLookup() {
        authenticate(user(31L, UserRole.ROLE_USER));
        assertThatThrownBy(() -> service.getTimeline(503L)).isInstanceOf(HubOperationAccessDeniedException.class);
        verifyNoInteractions(operationsAuditRepository);
    }
    @Test void automatedRefundAndTimeoutRemainVisibleAsSystemEvents() {
        authenticate(user(1L,UserRole.ROLE_ADMIN));Shipment shipment=shipment(506L,hub(5L,"H"));shipment.setCurrentHubId(null);
        when(operationsAuditRepository.findShipmentWithCurrentHub(506L)).thenReturn(Optional.of(shipment));
        when(operationsAuditRepository.findEventsForShipment(506L)).thenReturn(List.of(ShipmentEvent.of(shipment,null,"SHIPMENT_CANCELLED","timeout"),ShipmentEvent.of(shipment,null,"REFUND_COMPLETED","settled")));
        assertThat(service.getTimeline(506L).entries()).extracting(vn.iotstar.dto.operations.OperationsAuditTimelineEntry::eventType).containsExactly("SHIPMENT_CANCELLED","REFUND_COMPLETED");
        assertThat(service.getTimeline(506L).entries()).allSatisfy(e->assertThat(e.actorRole()).isEqualTo("SYSTEM"));
    }

    @Test void assignedDriverCanReadOnlyOwnCustodyTimeline() {
        User driver = user(40L, UserRole.ROLE_LINEHAUL_DRIVER); Shipment shipment = shipment(504L, hub(4L, "DEST")); authenticate(driver);
        shipment.setCurrentHubId(null); shipment.transitionTo(vn.iotstar.entity.ShipmentStatus.IN_TRANSIT);
        when(operationsAuditRepository.findShipmentWithCurrentHub(504L)).thenReturn(Optional.of(shipment));
        when(operationsAuditRepository.findActiveCustodyReferencesForDriver(504L, 40L)).thenReturn(List.of(new AuditCustodyReference(7L, 8L, 9L, 40L)));
        when(operationsAuditRepository.findEventsForShipment(504L)).thenReturn(List.of());
        assertThat(service.getTimeline(504L).entries()).isEmpty();
    }

    @Test void historicalDriverCannotReadTimelineAfterASecondDriverOwnsTheCurrentCustody() {
        User historicalDriver = user(40L, UserRole.ROLE_LINEHAUL_DRIVER);
        Shipment shipment = shipment(505L, hub(5L, "SECOND-LEG-HUB"));
        shipment.setCurrentHubId(null); shipment.transitionTo(vn.iotstar.entity.ShipmentStatus.IN_TRANSIT);
        authenticate(historicalDriver);
        when(operationsAuditRepository.findShipmentWithCurrentHub(505L)).thenReturn(Optional.of(shipment));
        when(operationsAuditRepository.findActiveCustodyReferencesForDriver(505L, 40L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getTimeline(505L)).isInstanceOf(ResourceNotFoundException.class);

        verify(operationsAuditRepository, never()).findEventsForShipment(505L);
        verify(operationsAuditRepository, never()).findCustodyReferences(505L);
    }

    private static Shipment shipment(Long id, Hub hub) { User sender = user(99L, UserRole.ROLE_USER); Shipment shipment = Shipment.create("TRK-" + id, sender, "Sender", "0900", "A", "Receiver", "0901", "B"); shipment.receiveAt(hub); ReflectionTestUtils.setField(shipment, "id", id); return shipment; }
    private static Hub hub(Long id, String code) { Hub hub = Hub.active(code, code, 1, "HUB", "SOUTH", "HCM"); ReflectionTestUtils.setField(hub, "id", id); return hub; }
    private static User user(Long id, UserRole role) { return new User(id, role.name().toLowerCase() + id, "hash", role, "ACTIVE"); }
    private static void authenticate(User user) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())); }
}
