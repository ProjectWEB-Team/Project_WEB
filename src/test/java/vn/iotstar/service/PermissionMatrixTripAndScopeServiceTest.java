package vn.iotstar.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.config.LogisticsProperties;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Handover;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.TransportTrip;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.entity.Vehicle;
import vn.iotstar.exception.HubOperationAccessDeniedException;
import vn.iotstar.exception.ShipmentAccessDeniedException;
import vn.iotstar.realtime.ShipmentEventPublisher;
import vn.iotstar.repository.*;
import vn.iotstar.service.impl.FirstMileServiceImpl;
import vn.iotstar.service.impl.LinehaulTripServiceImpl;
import vn.iotstar.service.impl.ManagerOperationsServiceImpl;
import vn.iotstar.service.impl.ShipmentCancellation;
import vn.iotstar.service.impl.ShipmentReturnServiceImpl;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** HARD-01 cells for trip, manager-scope, first-mile, delivery-assignment and return services (plan 06-05). */
class PermissionMatrixTripAndScopeServiceTest {
    private final User admin = new User(100L, "admin", "hash", UserRole.ROLE_ADMIN, "ACTIVE");
    private final User customer = new User(1L, "customer", "hash", UserRole.ROLE_USER, "ACTIVE");
    private final User staff = new User(7L, "staff", "hash", UserRole.ROLE_HUB_STAFF, "ACTIVE");
    private final User driver = new User(9L, "driver", "hash", UserRole.ROLE_LINEHAUL_DRIVER, "ACTIVE");
    private final User shipper = new User(30L, "shipper", "hash", UserRole.ROLE_SHIPPER, "ACTIVE");
    private final Hub hub = hub(1L);

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    // --- LinehaulTripServiceImpl ---

    private final LinehaulOperationsRepository operations = mock(LinehaulOperationsRepository.class);
    private final HubOperationsRepository hubOps = mock(HubOperationsRepository.class);
    private final ShipmentEventRepository events = mock(ShipmentEventRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final LinehaulTripServiceImpl trips = new LinehaulTripServiceImpl(operations, hubOps,
            mock(OperationalIncidentRepository.class), events, mock(UserRepository.class), publisher, new LogisticsProperties());

    @Test void vehicleListIsForActiveLinehaulDriversOnly() {
        for (User actor : List.of(staff, customer, admin)) {
            LastMileTestFixture.login(actor);
            assertThatThrownBy(trips::availableVehicles).isInstanceOf(HubOperationAccessDeniedException.class);
        }
        LastMileTestFixture.login(driver);
        when(hubOps.findActiveProfileByUserId(9L)).thenReturn(Optional.of(EmployeeProfile.active(driver, hub, "OUTBOUND")));
        assertThatThrownBy(trips::availableVehicles).isInstanceOf(HubOperationAccessDeniedException.class);
        verify(operations, never()).findAvailableVehicles();

        when(hubOps.findActiveProfileByUserId(9L)).thenReturn(Optional.of(EmployeeProfile.active(driver, hub, "LINEHAUL_DRIVER")));
        assertThat(trips.availableVehicles()).isEmpty();
    }

    @Test void onlyTheAssignedDriverCanArriveATrip() {
        Handover handover = Handover.pending(hub, hub(2L), EmployeeProfile.active(staff, hub, "OUTBOUND"), driver, 1, null);
        Vehicle vehicle = Vehicle.available("51C-1", "TRUCK", 1000);
        vehicle.reserve();
        TransportTrip trip = TransportTrip.accepted(handover, driver, vehicle);
        ReflectionTestUtils.setField(trip, "id", 40L);
        trip.start(driver);
        when(operations.findTripForUpdate(40L)).thenReturn(Optional.of(trip));
        User other = new User(10L, "other", "hash", UserRole.ROLE_LINEHAUL_DRIVER, "ACTIVE");
        LastMileTestFixture.login(other);
        when(hubOps.findActiveProfileByUserId(10L)).thenReturn(Optional.of(EmployeeProfile.active(other, hub, "LINEHAUL_DRIVER")));

        assertThatThrownBy(() -> trips.arriveTrip(40L)).isInstanceOf(HubOperationAccessDeniedException.class);
        assertThat(trip.getStatus()).isEqualTo("IN_TRANSIT");
        verifyNoInteractions(events, publisher);
    }

    // --- ManagerOperationsServiceImpl ---

    private final ManagerOperationsRepository managers = mock(ManagerOperationsRepository.class);
    private final ManagerOperationsServiceImpl dashboard = new ManagerOperationsServiceImpl(managers);

    @Test void dashboardIsForManagersAndAdministratorsOnly() {
        for (User actor : List.of(staff, customer, shipper, driver)) {
            LastMileTestFixture.login(actor);
            assertThatThrownBy(dashboard::getDashboard).isInstanceOf(HubOperationAccessDeniedException.class);
        }
        verify(managers, never()).countInboundShipments(any());

        when(managers.findAllActiveHubIds()).thenReturn(List.of(1L, 2L));
        LastMileTestFixture.login(admin);
        assertThat(dashboard.getDashboard().systemWide()).isTrue();
        verify(managers).countInboundShipments(List.of(1L, 2L));
    }

    @Test void managerDashboardCountsOnlyTheirSubtree() {
        User manager = new User(20L, "manager", "hash", UserRole.ROLE_MANAGER, "ACTIVE");
        when(managers.findActiveProfileByUserId(20L)).thenReturn(Optional.of(EmployeeProfile.active(manager, hub, "MANAGER")));
        when(managers.findHubSubtreeIds(1L)).thenReturn(List.of(1L));
        LastMileTestFixture.login(manager);

        assertThat(dashboard.getDashboard().systemWide()).isFalse();
        verify(managers).countInboundShipments(List.of(1L));
        verify(managers, never()).findAllActiveHubIds();
    }

    // --- FirstMileServiceImpl ---

    private final ShipmentRepository shipments = mock(ShipmentRepository.class);
    private final EmployeeProfileRepository profiles = mock(EmployeeProfileRepository.class);
    private final ShipmentCancellation cancellation = mock(ShipmentCancellation.class);
    private final FirstMileServiceImpl firstMile = new FirstMileServiceImpl(shipments, mock(ShipmentAssignmentRepository.class),
            mock(PickupAttemptRepository.class), profiles, mock(PaymentRepository.class), events,
            mock(ShipmentEventPublisher.class), cancellation, Clock.systemDefaultZone());

    @ParameterizedTest
    @ValueSource(strings = {"INBOUND", "SORTER", "OUTBOUND"})
    void pickupQueueShippersAndServiceFailureNeedACoordinator(String position) {
        LastMileTestFixture.login(staff);
        when(profiles.findFirstByUserId(7L)).thenReturn(Optional.of(EmployeeProfile.active(staff, hub, position)));

        assertThatThrownBy(firstMile::pendingPickups).isInstanceOf(ShipmentAccessDeniedException.class);
        assertThatThrownBy(firstMile::shippersOfMyHub).isInstanceOf(ShipmentAccessDeniedException.class);
        assertThatThrownBy(() -> firstMile.reportServiceFailure(1L, "no vehicle")).isInstanceOf(ShipmentAccessDeniedException.class);
        verify(shipments, never()).findPendingPickups(anyLong(), any());
        verifyNoInteractions(cancellation);
    }

    @Test void coordinatorReadsTheQueueOfTheirOwnHub() {
        LastMileTestFixture.login(staff);
        when(profiles.findFirstByUserId(7L)).thenReturn(Optional.of(EmployeeProfile.active(staff, hub, "COORDINATOR")));
        assertThat(firstMile.pendingPickups()).isEmpty();
        verify(shipments).findPendingPickups(eq(1L), any());
    }

    @Test void pickupTaskListIsForShippersOnly() {
        for (User actor : List.of(staff, customer)) {
            LastMileTestFixture.login(actor);
            when(profiles.findFirstByUserId(actor.getId())).thenReturn(Optional.of(EmployeeProfile.active(actor, hub, "SHIPPER")));
            assertThatThrownBy(firstMile::myPickupTasks).isInstanceOf(ShipmentAccessDeniedException.class);
        }
        LastMileTestFixture.login(shipper);
        when(profiles.findFirstByUserId(30L)).thenReturn(Optional.of(EmployeeProfile.active(shipper, hub, "SHIPPER")));
        assertThat(firstMile.myPickupTasks()).isEmpty();
    }

    // --- LastMileAssignmentService ---

    @ParameterizedTest
    @ValueSource(strings = {"INBOUND", "SORTER", "OUTBOUND"})
    void deliveryAssignmentScreensNeedACoordinator(String position) {
        LastMileTestFixture f = new LastMileTestFixture();
        User clerk = new User(5L, "clerk", "hash", UserRole.ROLE_HUB_STAFF, "ACTIVE");
        when(f.profiles.findFirstByUserId(5L)).thenReturn(Optional.of(EmployeeProfile.active(clerk, f.hub, position)));
        LastMileTestFixture.login(clerk);

        assertThatThrownBy(f.dispatch::candidates).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(f.dispatch::shippers).isInstanceOf(AccessDeniedException.class);
        verify(f.shipments, never()).findAssignableLastMile(anyLong(), any());
    }

    @Test void coordinatorSeesOnlyTheirHubsAssignableParcels() {
        LastMileTestFixture f = new LastMileTestFixture();
        LastMileTestFixture.login(f.coordinator);
        assertThat(f.dispatch.candidates()).isEmpty();
        verify(f.shipments).findAssignableLastMile(eq(100L), any());
    }

    // --- ShipmentReturnServiceImpl ---

    @Test void disposalCandidatesAreForManagersAndAdministratorsOnly() {
        LastMileTestFixture f = new LastMileTestFixture();
        ShipmentReturnServiceImpl returns = new ShipmentReturnServiceImpl(f.shipments, f.events, f.users, f.config,
                f.assignments, managers, f.access, Clock.systemDefaultZone());
        for (User actor : List.of(f.sender, f.shipper, f.inbound, driver)) {
            LastMileTestFixture.login(actor);
            assertThatThrownBy(returns::disposalCandidates).isInstanceOf(AccessDeniedException.class);
        }
        when(managers.findAllActiveHubIds()).thenReturn(List.of(100L));
        LastMileTestFixture.login(admin);
        assertThat(returns.disposalCandidates()).isEmpty();
    }

    private static Hub hub(Long id) {
        Hub hub = Hub.active("H" + id, "Hub " + id, 1, "DELIVERY_HUB", "SOUTH", "P");
        ReflectionTestUtils.setField(hub, "id", id);
        return hub;
    }
}
