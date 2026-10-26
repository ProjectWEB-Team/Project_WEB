package vn.iotstar.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.dto.hub.*;
import vn.iotstar.entity.*;
import vn.iotstar.repository.*;
import vn.iotstar.service.impl.*;
import vn.iotstar.exception.HubOperationAccessDeniedException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Composes departure and physical receipt; test fixtures never advance a parcel between these services. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LinehaulCustodyCompletionTest {
    @Mock LinehaulOperationsRepository operations;
    @Mock HubOperationsRepository profiles;
    @Mock DestinationReceiptRepository receipts;
    @Mock OperationalIncidentRepository incidents;
    @Mock ShipmentEventRepository events;
    @Mock UserRepository users;
    @Mock ManagerOperationsRepository managers;
    @Mock ApplicationEventPublisher publisher;
    LinehaulTripServiceImpl trips;
    DestinationReceiptServiceImpl inbound;
    User driver, staff, manager;
    Hub origin, destination;
    Vehicle vehicle;
    Handover handover;
    ParcelContainer container;
    HandoverContainer membership;
    Shipment first, second;
    ContainerItem item1, item2;
    Incident savedIncident;

    @BeforeEach void setup() {
        trips = new LinehaulTripServiceImpl(operations, profiles, incidents, events, users, publisher, new vn.iotstar.config.LogisticsProperties());
        inbound = new DestinationReceiptServiceImpl(receipts, profiles, events, incidents, users, publisher, managers);
        origin = hub(1L); destination = hub(2L);
        driver = user(9L, UserRole.ROLE_LINEHAUL_DRIVER); staff = user(11L, UserRole.ROLE_HUB_STAFF); manager = user(12L, UserRole.ROLE_MANAGER);
        when(profiles.findActiveProfileByUserId(9L)).thenReturn(Optional.of(EmployeeProfile.active(driver, origin, "LINEHAUL_DRIVER")));
        when(profiles.findActiveProfileByUserId(11L)).thenReturn(Optional.of(EmployeeProfile.active(staff, destination, "INBOUND")));
        when(managers.findActiveProfileByUserId(12L)).thenReturn(Optional.of(EmployeeProfile.active(manager, destination, "MANAGER")));
        when(managers.findHubSubtreeIds(2L)).thenReturn(List.of(2L));
        when(users.getReferenceById(anyLong())).thenAnswer(i -> i.<Long>getArgument(0) == 9L ? driver : i.<Long>getArgument(0) == 11L ? staff : manager);
        vehicle = Vehicle.available("51C-123", "TRUCK", 1000); id(vehicle, 50L);
        when(operations.findVehicleForUpdate(50L)).thenReturn(Optional.of(vehicle));
        handover = Handover.pending(origin, destination, EmployeeProfile.active(staff, origin, "OUTBOUND"), driver, 1, null);
        id(handover, 20L); handover.recordContainerScan();
        container = ParcelContainer.open("BAG", origin, destination, staff); id(container, 30L); container.seal();
        membership = HandoverContainer.expected(handover, container); membership.markScanned();
        first = parcel(40L); second = parcel(41L);
        item1 = ContainerItem.scanned(container, first); item2 = ContainerItem.scanned(container, second);
        when(operations.findHandoverForUpdate(20L)).thenReturn(Optional.of(handover));
        doAnswer(i -> { id(i.getArgument(0), 21L); return null; }).when(operations).persist(any());
        when(operations.findTripForUpdate(21L)).thenAnswer(i -> Optional.ofNullable(handover.getTransportTrip()));
        when(operations.findContainersForUpdate(20L)).thenReturn(List.of(membership));
        when(operations.findManifestItemsForUpdate(20L)).thenReturn(List.of(item1, item2));
        when(operations.findShipmentsForHandover(20L)).thenReturn(List.of(first, second));
        when(receipts.findArrivedHandoverForUpdate(20L)).thenReturn(Optional.of(handover));
        when(receipts.findHandoverContainer(20L, 30L)).thenReturn(Optional.of(membership));
        when(receipts.findHandoverContainers(20L)).thenReturn(List.of(membership));
        when(receipts.findItemsForContainerForUpdate(30L)).thenReturn(List.of(item1, item2));
        when(receipts.findItemForUpdate(30L, 40L)).thenReturn(Optional.of(item1));
        when(receipts.findItemForUpdate(30L, 41L)).thenReturn(Optional.of(item2));
        when(incidents.save(any())).thenAnswer(i -> { savedIncident = i.getArgument(0); id(savedIncident, 77L); return savedIncident; });
        when(incidents.findByIdForUpdate(77L)).thenAnswer(i -> Optional.ofNullable(savedIncident));
        when(incidents.findById(77L)).thenAnswer(i -> Optional.ofNullable(savedIncident));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void departureReceiptCompletesAllAggregatesAndReplaysWithoutSecondReceipt() {
        arrive();
        assertThat(first.getCurrentStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(first.getCurrentHubId()).isNull();
        auth(staff);
        inbound.receiveItem(new DestinationReceiptCommand(20L, 30L, 40L));
        assertThat(handover.getTransportTrip().getStatus()).isEqualTo("ARRIVED");
        assertThat(container.getStatus()).isEqualTo("LOADED");
        inbound.receiveItem(new DestinationReceiptCommand(20L, 30L, 41L));
        assertThat(container.getStatus()).isEqualTo("UNPACKED_FULL");
        assertThat(membership.getStatus()).isEqualTo("DELIVERED");
        assertThat(handover.getStatus()).isEqualTo("COMPLETED");
        assertThat(handover.getTransportTrip().getStatus()).isEqualTo("COMPLETED");
        assertThat(vehicle.getStatus()).isEqualTo("AVAILABLE");
        assertThat(first.getCurrentHubId()).isEqualTo(2L);
        clearInvocations(events);
        assertThat(inbound.receiveItem(new DestinationReceiptCommand(20L, 30L, 40L)).alreadyReceived()).isTrue();
        verifyNoInteractions(events);
    }

    @Test void heldManifestMemberPreventsEveryDepartureMutation() {
        auth(driver); trips.acceptHandover(20L, 50L); second.setOnHold(true); clearInvocations(events);
        assertThatThrownBy(() -> trips.startTrip(21L)).isInstanceOf(HubOperationAccessDeniedException.class);
        assertThat(first.getCurrentStatus()).isEqualTo(ShipmentStatus.AT_HUB);
        assertThat(first.getCurrentHubId()).isEqualTo(1L);
        assertThat(handover.getStatus()).isEqualTo("ACCEPTED");
        assertThat(handover.getTransportTrip().getStatus()).isEqualTo("SCHEDULED");
        assertThat(container.getStatus()).isEqualTo("SEALED");
        verifyNoInteractions(events);
    }

    @Test void vehicleMustBeExplicitAndAvailableAndCannotBeChangedByReplay() {
        auth(driver);
        assertThatThrownBy(() -> trips.acceptHandover(20L)).isInstanceOf(IllegalArgumentException.class);
        vehicle.reserve();
        assertThatThrownBy(() -> trips.acceptHandover(20L, 50L)).isInstanceOf(HubOperationAccessDeniedException.class);
        vehicle.release(); trips.acceptHandover(20L, 50L);
        assertThatThrownBy(() -> trips.acceptHandover(20L, 51L)).isInstanceOf(HubOperationAccessDeniedException.class);
        assertThat(handover.getTransportTrip().getVehicle()).isSameAs(vehicle);
    }

    @Test void returnDirectionAndStatusSurviveDepartureAndPhysicalReceipt() {
        first.setDirection("RETURN"); first.transitionTo(ShipmentStatus.RETURN_TO_SENDER);
        arrive();
        assertThat(first.getCurrentStatus()).isEqualTo(ShipmentStatus.RETURN_TO_SENDER);
        auth(staff); inbound.receiveItem(new DestinationReceiptCommand(20L, 30L, 40L));
        assertThat(first.getCurrentStatus()).isEqualTo(ShipmentStatus.RETURN_TO_SENDER);
        assertThat(first.getDirection()).isEqualTo("RETURN");
        assertThat(first.getCurrentHubId()).isEqualTo(2L);
    }

    @Test void missingHoldsOnlyAffectedParcelAndFoundPreservesDiscrepancyEvidence() {
        missing();
        assertThat(first.isOnHold()).isFalse(); assertThat(second.isOnHold()).isTrue();
        assertThat(second.getCurrentStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(container.getStatus()).isEqualTo("UNPACKED_WITH_DISCREPANCY");
        assertThat(handover.getStatus()).isEqualTo("COMPLETED_WITH_DISCREPANCY");
        assertThat(handover.getTransportTrip().getStatus()).isEqualTo("COMPLETED_WITH_DISCREPANCY");
        assertThatThrownBy(() -> inbound.receiveItem(new DestinationReceiptCommand(20L, 30L, 41L))).isInstanceOf(HubOperationAccessDeniedException.class);
        auth(manager); inbound.resolveMissing(resolution("FOUND"));
        assertThat(savedIncident.getStatus()).isEqualTo("RESOLVED");
        assertThat(savedIncident.getResolutionOutcome()).isEqualTo("FOUND");
        assertThat(second.getCurrentStatus()).isEqualTo(ShipmentStatus.AT_HUB);
        assertThat(second.getCurrentHubId()).isEqualTo(2L); assertThat(second.isOnHold()).isFalse();
        assertThat(item2.isMissing()).isTrue(); assertThat(item2.isRecovered()).isTrue();
        assertThat(container.getStatus()).isEqualTo("UNPACKED_WITH_DISCREPANCY");
        clearInvocations(events); inbound.resolveMissing(resolution("FOUND")); verifyNoInteractions(events);
    }

    @Test void confirmedLossStaysHeldUntilAtomicFinanceSettlementAndRejectsOutcomeRewrite() {
        missing(); auth(manager); inbound.resolveMissing(resolution("CONFIRMED_LOST"));
        assertThat(savedIncident.getStatus()).isEqualTo("INVESTIGATING");
        assertThat(savedIncident.getResolutionOutcome()).isEqualTo("CONFIRMED_LOST");
        assertThat(savedIncident.getInvestigatedBy()).isEqualTo(manager.getId());
        assertThat(savedIncident.getInvestigatedAt()).isNotNull();
        assertThat(second.isOnHold()).isTrue(); assertThat(second.getCurrentStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThatThrownBy(() -> inbound.resolveMissing(resolution("FOUND"))).isInstanceOf(IllegalStateException.class);
    }

    @Test void wrongIncidentShipmentAndCrossHubManagerCannotResolve() {
        missing(); auth(manager);
        assertThatThrownBy(() -> inbound.resolveMissing(new MissingParcelResolutionCommand(77L, 20L, 30L, 40L, 2L, "FOUND", "camera checked"))).isInstanceOf(HubOperationAccessDeniedException.class);
        when(managers.findHubSubtreeIds(2L)).thenReturn(List.of(99L));
        assertThatThrownBy(() -> inbound.resolveMissing(resolution("FOUND"))).isInstanceOf(HubOperationAccessDeniedException.class);
        assertThat(savedIncident.getResolutionOutcome()).isNull(); assertThat(second.isOnHold()).isTrue();
    }

    @Test void unknownReconciliationScanIsRejectedBeforeAnyReceiptOrIncidentMutation() {
        arrive(); auth(staff); clearInvocations(events, incidents);
        assertThatThrownBy(() -> inbound.reconcileContainer(new DestinationReconciliationCommand(20L, 30L, List.of(40L, 999L)))).isInstanceOf(HubOperationAccessDeniedException.class);
        assertThat(item1.isReceived()).isFalse(); assertThat(item2.isMissing()).isFalse(); verifyNoInteractions(events, incidents);
    }

    @Test void destinationManagerReadsMissingIncidentDespiteNoCurrentPhysicalHub() {
        missing(); assertThat(second.getCurrentHubId()).isNull(); auth(manager);
        assertThat(inbound.missingCase(77L).shipmentId()).isEqualTo(41L);
        when(managers.findHubSubtreeIds(2L)).thenReturn(List.of(99L));
        assertThatThrownBy(() -> inbound.missingCase(77L)).isInstanceOf(HubOperationAccessDeniedException.class);
    }

    @Test void findingOneParcelDoesNotReleaseAnotherUnresolvedIncidentHold() {
        missing(); auth(manager);
        when(incidents.existsByShipmentIdAndStatusInAndIdNot(eq(41L), anyCollection(), eq(77L))).thenReturn(true);
        inbound.resolveMissing(resolution("FOUND")); assertThat(second.isOnHold()).isTrue();
    }

    private void arrive() { auth(driver); trips.acceptHandover(20L, 50L); trips.startTrip(21L); trips.arriveTrip(21L); }
    private void missing() { arrive(); auth(staff); inbound.reconcileContainer(new DestinationReconciliationCommand(20L, 30L, List.of(40L))); }
    private MissingParcelResolutionCommand resolution(String outcome) { return new MissingParcelResolutionCommand(77L, 20L, 30L, 41L, 2L, outcome, "camera checked"); }
    private Shipment parcel(long id) { Shipment s=Shipment.create("T-"+id, staff, "S", "1", "A", "R", "2", "B"); id(s,id); s.receiveAt(origin); return s; }
    private static Hub hub(long id) { Hub h=Hub.active("H"+id,"Hub"+id,1,"DELIVERY_HUB","S","P"); id(h,id); return h; }
    private static User user(long id,UserRole role) { return new User(id,"user"+id,"hash",role,"ACTIVE"); }
    private static void id(Object entity,long id) { ReflectionTestUtils.setField(entity,"id",id); }
    private static void auth(User u) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u,null,u.getAuthorities())); }
}
