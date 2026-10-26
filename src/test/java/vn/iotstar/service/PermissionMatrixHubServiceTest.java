package vn.iotstar.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.config.LogisticsProperties;
import vn.iotstar.dto.hub.CorrectHandoverCommand;
import vn.iotstar.dto.hub.CreateContainerCommand;
import vn.iotstar.dto.hub.CreateHandoverCommand;
import vn.iotstar.dto.hub.DestinationReceiptCommand;
import vn.iotstar.dto.hub.InboundShipmentCommand;
import vn.iotstar.dto.hub.ScanHandoverContainerCommand;
import vn.iotstar.dto.hub.SortShipmentCommand;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Handover;
import vn.iotstar.entity.HandoverContainer;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.HubRoutingRule;
import vn.iotstar.entity.ParcelContainer;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.HubOperationAccessDeniedException;
import vn.iotstar.repository.*;
import vn.iotstar.service.impl.ContainerHandoverServiceImpl;
import vn.iotstar.service.impl.DestinationReceiptServiceImpl;
import vn.iotstar.service.impl.HubOperationsServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** HARD-01 role x profile x position x hub x location cells for hub-side services (plan 06-05). */
class PermissionMatrixHubServiceTest {
    private final HubOperationsRepository hubOps = mock(HubOperationsRepository.class);
    private final ShipmentEventRepository events = mock(ShipmentEventRepository.class);
    private final RoutingExceptionRepository routingExceptions = mock(RoutingExceptionRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final HubOperationsServiceImpl hub = new HubOperationsServiceImpl(hubOps, events, routingExceptions, users,
            publisher, null, null, null, null, new LogisticsProperties());

    private final Hub hub11 = hub(11L, "HUB-A");
    private final Hub hub12 = hub(12L, "SOC-LA");
    private final User staff = new User(7L, "staff", "hash", UserRole.ROLE_HUB_STAFF, "ACTIVE");
    private final User sender = new User(8L, "sender", "hash", UserRole.ROLE_USER, "ACTIVE");
    private final Shipment shipment = Shipment.create("UTX-1", sender, "S", "1", "A", "R", "2", "Can Tho");

    PermissionMatrixHubServiceTest() {
        ReflectionTestUtils.setField(shipment, "id", 101L);
        ReflectionTestUtils.setField(shipment, "pickupMethod", "HOME_PICKUP");
        ReflectionTestUtils.setField(shipment, "currentStatus", ShipmentStatus.PICKED_UP);
        ReflectionTestUtils.setField(shipment, "currentHub", hub11);
        shipment.setDestinationGrouping("SOUTH", "Can Tho");
        when(hubOps.findShipmentByIdForUpdate(101L)).thenReturn(Optional.of(shipment));
        when(users.getReferenceById(7L)).thenReturn(staff);
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    // --- HubOperationsServiceImpl ---

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = "ROLE_HUB_STAFF", mode = EnumSource.Mode.EXCLUDE)
    void onlyHubStaffMayReceiveOrSort(UserRole role) {
        LastMileTestFixture.login(new User(7L, "actor", "hash", role, "ACTIVE"));
        assertThatThrownBy(() -> hub.receiveAtHub(InboundShipmentCommand.pickupReceipt(101L))).isInstanceOf(HubOperationAccessDeniedException.class);
        assertThatThrownBy(() -> hub.sortAtHub(new SortShipmentCommand(101L))).isInstanceOf(HubOperationAccessDeniedException.class);
        verify(hubOps, never()).findShipmentByIdForUpdate(anyLong());
        verifyNoInteractions(events);
    }

    @Test void hubStaffWithoutAnActiveProfileIsDenied() {
        LastMileTestFixture.login(staff);
        when(hubOps.findActiveProfileByUserId(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> hub.receiveAtHub(InboundShipmentCommand.pickupReceipt(101L))).isInstanceOf(HubOperationAccessDeniedException.class);
        assertThatThrownBy(() -> hub.sortAtHub(new SortShipmentCommand(101L))).isInstanceOf(HubOperationAccessDeniedException.class);
        verifyNoInteractions(events);
    }

    @ParameterizedTest
    @ValueSource(strings = {"COORDINATOR", "OUTBOUND", "SORTER"})
    void receiptNeedsTheInboundOrGeneralPosition(String position) {
        as(position);
        assertThatThrownBy(() -> hub.receiveAtHub(InboundShipmentCommand.pickupReceipt(101L))).isInstanceOf(HubOperationAccessDeniedException.class);
        verifyNoInteractions(events);
    }

    @ParameterizedTest
    @ValueSource(strings = {"COORDINATOR", "OUTBOUND", "INBOUND"})
    void sortingNeedsTheSorterOrGeneralPosition(String position) {
        ReflectionTestUtils.setField(shipment, "currentStatus", ShipmentStatus.AT_HUB);
        as(position);
        assertThatThrownBy(() -> hub.sortAtHub(new SortShipmentCommand(101L))).isInstanceOf(HubOperationAccessDeniedException.class);
        verify(hubOps, never()).saveRoute(any());
        verifyNoInteractions(events);
    }

    @Test void heldParcelsCannotBeReceivedOrSorted() {
        shipment.setOnHold(true);
        as("GENERAL_STAFF");
        assertThatThrownBy(() -> hub.receiveAtHub(InboundShipmentCommand.pickupReceipt(101L))).isInstanceOf(HubOperationAccessDeniedException.class);
        ReflectionTestUtils.setField(shipment, "currentStatus", ShipmentStatus.AT_HUB);
        assertThatThrownBy(() -> hub.sortAtHub(new SortShipmentCommand(101L))).isInstanceOf(HubOperationAccessDeniedException.class);
        verifyNoInteractions(events);
    }

    @Test void parcelInTheWrongStatusCannotBeSorted() {
        as("SORTER");
        assertThatThrownBy(() -> hub.sortAtHub(new SortShipmentCommand(101L))).isInstanceOf(HubOperationAccessDeniedException.class);
        verifyNoInteractions(events);
    }

    @Test void generalStaffAtTheParcelsHubCanReceiveThenSort() {
        as("GENERAL_STAFF");
        hub.receiveAtHub(InboundShipmentCommand.pickupReceipt(101L));
        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.AT_HUB);

        when(hubOps.findActiveRules(11L, "SOUTH", "Can Tho")).thenReturn(List.of(HubRoutingRule.active(hub11, "SOUTH", "Can Tho", hub12)));
        when(hubOps.saveRoute(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(hub.sortAtHub(new SortShipmentCommand(101L)).nextHubCode()).isEqualTo("SOC-LA");

        var saved = org.mockito.ArgumentCaptor.forClass(ShipmentEvent.class);
        verify(events, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(ShipmentEvent::getEventType).containsExactly("ARRIVED_AT_HUB", "SORTING_COMPLETED");
    }

    @Test void administratorReadsAnyRouteDiagnosticButACustomerCannot() {
        when(hubOps.findShipmentByIdWithHub(101L)).thenReturn(Optional.of(shipment));
        LastMileTestFixture.login(new User(100L, "admin", "hash", UserRole.ROLE_ADMIN, "ACTIVE"));
        assertThat(hub.getRouteDiagnostic(101L).diagnosticCode()).isEqualTo("NO_ACTIVE_STATIC_RULE");
        LastMileTestFixture.login(sender);
        assertThatThrownBy(() -> hub.getRouteDiagnostic(101L)).isInstanceOf(HubOperationAccessDeniedException.class);
    }

    // --- ContainerHandoverServiceImpl ---

    private final ContainerHandoverRepository custody = mock(ContainerHandoverRepository.class);
    private final ContainerHandoverServiceImpl handovers = new ContainerHandoverServiceImpl(custody, hubOps, events, users, publisher);
    private final User driver = new User(9L, "driver", "hash", UserRole.ROLE_LINEHAUL_DRIVER, "ACTIVE");

    @ParameterizedTest
    @ValueSource(strings = {"INBOUND", "SORTER", "COORDINATOR"})
    void custodyActionsNeedTheOutboundOrGeneralPosition(String position) {
        as(position);
        assertThatThrownBy(() -> handovers.createContainer(new CreateContainerCommand("BAG-1", 12L))).isInstanceOf(HubOperationAccessDeniedException.class);
        assertThatThrownBy(() -> handovers.createHandover(new CreateHandoverCommand(12L, 9L, List.of(1L)))).isInstanceOf(HubOperationAccessDeniedException.class);
        assertThatThrownBy(() -> handovers.correctByCancellationAndRecreation(new CorrectHandoverCommand(30L, List.of(1L))))
                .isInstanceOf(HubOperationAccessDeniedException.class);
        verify(custody, never()).persist(any());
    }

    @Test void nonStaffRolesCannotCreateCustody() {
        LastMileTestFixture.login(driver);
        assertThatThrownBy(() -> handovers.createContainer(new CreateContainerCommand("BAG-1", 12L))).isInstanceOf(HubOperationAccessDeniedException.class);
        assertThatThrownBy(() -> handovers.createHandover(new CreateHandoverCommand(12L, 9L, List.of(1L)))).isInstanceOf(HubOperationAccessDeniedException.class);
        verify(custody, never()).persist(any());
    }

    @Test void handoverToAUserWhoIsNotADriverIsDenied() {
        as("OUTBOUND");
        when(custody.findHubById(12L)).thenReturn(Optional.of(hub12));
        when(users.findById(8L)).thenReturn(Optional.of(sender));
        assertThatThrownBy(() -> handovers.createHandover(new CreateHandoverCommand(12L, 8L, List.of(1L)))).isInstanceOf(HubOperationAccessDeniedException.class);
        verify(custody, never()).persist(any());
    }

    @Test void handoverToADriverWithoutAnActiveDriverProfileIsDenied() {
        as("OUTBOUND");
        when(custody.findHubById(12L)).thenReturn(Optional.of(hub12));
        when(users.findById(9L)).thenReturn(Optional.of(driver));
        when(hubOps.findActiveProfileByUserId(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handovers.createHandover(new CreateHandoverCommand(12L, 9L, List.of(1L)))).isInstanceOf(HubOperationAccessDeniedException.class);
        when(hubOps.findActiveProfileByUserId(9L)).thenReturn(Optional.of(EmployeeProfile.active(driver, hub11, "OUTBOUND")));
        assertThatThrownBy(() -> handovers.createHandover(new CreateHandoverCommand(12L, 9L, List.of(1L)))).isInstanceOf(HubOperationAccessDeniedException.class);
        verify(custody, never()).persist(any());
    }

    @Test void outboundStaffOfAnotherHubCannotCorrectAHandover() {
        Hub other = hub(13L, "HUB-B");
        LastMileTestFixture.login(staff);
        when(hubOps.findActiveProfileByUserId(7L)).thenReturn(Optional.of(EmployeeProfile.active(staff, other, "OUTBOUND")));
        Handover handover = Handover.pending(hub11, hub12, EmployeeProfile.active(staff, hub11, "OUTBOUND"), driver, 1, null);
        when(custody.findHandoverForUpdate(30L)).thenReturn(Optional.of(handover));

        assertThatThrownBy(() -> handovers.correctByCancellationAndRecreation(new CorrectHandoverCommand(30L, List.of(1L))))
                .isInstanceOf(HubOperationAccessDeniedException.class);
        assertThat(handover.getStatus()).isEqualTo("PENDING_ACCEPTANCE");
        verify(custody, never()).persist(any());
    }

    @Test void onlyTheNamedDriverCanScanAndAcceptAHandover() {
        Handover handover = Handover.pending(hub11, hub12, EmployeeProfile.active(staff, hub11, "OUTBOUND"), driver, 1, null);
        ReflectionTestUtils.setField(handover, "id", 30L);
        ParcelContainer bag = ParcelContainer.open("BAG", hub11, hub12, staff);
        bag.seal();
        ReflectionTestUtils.setField(bag, "id", 1L);
        when(custody.findHandoverForUpdate(30L)).thenReturn(Optional.of(handover));
        when(custody.findHandoverContainer(30L, 1L)).thenReturn(Optional.of(HandoverContainer.expected(handover, bag)));

        LastMileTestFixture.login(staff);
        assertThatThrownBy(() -> handovers.scanAndAccept(new ScanHandoverContainerCommand(30L, 1L))).isInstanceOf(HubOperationAccessDeniedException.class);
        LastMileTestFixture.login(new User(10L, "other-driver", "hash", UserRole.ROLE_LINEHAUL_DRIVER, "ACTIVE"));
        assertThatThrownBy(() -> handovers.scanAndAccept(new ScanHandoverContainerCommand(30L, 1L))).isInstanceOf(HubOperationAccessDeniedException.class);
        assertThat(handover.getAcceptedContainers()).isZero();

        LastMileTestFixture.login(driver);
        assertThat(handovers.scanAndAccept(new ScanHandoverContainerCommand(30L, 1L)).accepted()).isTrue();
    }

    // --- DestinationReceiptServiceImpl ---

    private final ManagerOperationsRepository managers = mock(ManagerOperationsRepository.class);
    private final OperationalIncidentRepository incidents = mock(OperationalIncidentRepository.class);
    private final DestinationReceiptRepository receipts = mock(DestinationReceiptRepository.class);
    private final DestinationReceiptServiceImpl destination = new DestinationReceiptServiceImpl(receipts, hubOps, events,
            incidents, users, publisher, managers);

    @ParameterizedTest
    @ValueSource(strings = {"OUTBOUND", "SORTER", "COORDINATOR"})
    void destinationReceiptNeedsTheInboundOrGeneralPosition(String position) {
        as(position);
        assertThatThrownBy(() -> destination.receiveItem(new DestinationReceiptCommand(30L, 1L, 101L))).isInstanceOf(HubOperationAccessDeniedException.class);
        verifyNoInteractions(receipts, events);
    }

    @Test void missingParcelQueueIsForManagersAndAdministratorsOnly() {
        for (User actor : List.of(staff, sender)) {
            LastMileTestFixture.login(actor);
            assertThatThrownBy(destination::missingCases).isInstanceOf(HubOperationAccessDeniedException.class);
        }
        verifyNoInteractions(incidents);
        when(managers.findAllActiveHubIds()).thenReturn(List.of(11L));
        LastMileTestFixture.login(new User(100L, "admin", "hash", UserRole.ROLE_ADMIN, "ACTIVE"));
        assertThat(destination.missingCases()).isEmpty();
    }

    private void as(String position) {
        LastMileTestFixture.login(staff);
        when(hubOps.findActiveProfileByUserId(7L)).thenReturn(Optional.of(EmployeeProfile.active(staff, hub11, position)));
    }

    private static Hub hub(Long id, String code) {
        Hub hub = Hub.active(code, code, 1, "DELIVERY_HUB", "SOUTH", "Binh Duong");
        ReflectionTestUtils.setField(hub, "id", id);
        return hub;
    }
}
