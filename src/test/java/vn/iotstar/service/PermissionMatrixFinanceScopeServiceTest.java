package vn.iotstar.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.dto.delivery.DeliveryCollectionRequest;
import vn.iotstar.dto.delivery.DeliveryFailureRequest;
import vn.iotstar.dto.delivery.DeliveryAssignmentRequest;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.Incident;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.realtime.NotificationEventPublisher;
import vn.iotstar.repository.*;
import vn.iotstar.service.impl.FinanceAccess;
import vn.iotstar.service.impl.FinanceReconciliationServiceImpl;
import vn.iotstar.service.impl.ShipmentFinanceServiceImpl;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HARD-01 finance-scope and last-mile assignment cells (D-01) not already covered; see 06-MATRIX-SERVICE-OWNERSHIP.md. */
class PermissionMatrixFinanceScopeServiceTest {
    private final User admin = new User(100L, "admin", "hash", UserRole.ROLE_ADMIN, "ACTIVE");
    private final User outsideManager = new User(20L, "manager", "hash", UserRole.ROLE_MANAGER, "ACTIVE");
    private final User unplacedManager = new User(21L, "unplaced", "hash", UserRole.ROLE_MANAGER, "ACTIVE");
    private final User shipperUser = new User(10L, "shipper", "hash", UserRole.ROLE_SHIPPER, "ACTIVE");
    private final User hubStaff = new User(11L, "staff", "hash", UserRole.ROLE_HUB_STAFF, "ACTIVE");
    private final Hub hub = hub(5L);

    private final ManagerOperationsRepository managers = mock(ManagerOperationsRepository.class);
    private final EmployeeProfileRepository employees = mock(EmployeeProfileRepository.class);
    private final FinanceAccess access = new FinanceAccess(managers, employees);
    private final CodTransactionRepository cods = mock(CodTransactionRepository.class);
    private final FinanceReconciliationRepository reconciliations = mock(FinanceReconciliationRepository.class);
    private final WalletRepository wallets = mock(WalletRepository.class);
    private final WalletTransactionRepository transactions = mock(WalletTransactionRepository.class);
    private final FinanceReconciliationServiceImpl reconciliation = new FinanceReconciliationServiceImpl(reconciliations,
            mock(ShipperReconciliationItemRepository.class), cods, mock(PaymentRepository.class), wallets, transactions,
            mock(IncidentRepository.class), mock(ShipmentRepository.class), mock(UserRepository.class),
            mock(ShipmentEventRepository.class), mock(NotificationEventPublisher.class), access, Clock.systemDefaultZone());

    private final ShipmentRepository shipments = mock(ShipmentRepository.class);
    private final IncidentRepository incidents = mock(IncidentRepository.class);
    private final WalletLedgerService ledger = mock(WalletLedgerService.class);
    private final RefundSettlementOutboxRepository outbox = mock(RefundSettlementOutboxRepository.class);
    private final ShipmentFinanceServiceImpl finance = new ShipmentFinanceServiceImpl(mock(PaymentRepository.class), shipments,
            incidents, mock(ShipmentEventRepository.class), mock(UserRepository.class), ledger,
            mock(NotificationEventPublisher.class), outbox, transactions, access, Clock.systemDefaultZone());

    PermissionMatrixFinanceScopeServiceTest() {
        Hub outside = hub(99L);
        when(managers.findActiveProfileByUserId(20L)).thenReturn(Optional.of(EmployeeProfile.active(outsideManager, outside, "MANAGER")));
        when(managers.findHubSubtreeIds(99L)).thenReturn(List.of(99L));
        when(managers.findActiveProfileByUserId(21L)).thenReturn(Optional.empty());
        when(employees.findFirstByUserId(10L)).thenReturn(Optional.of(EmployeeProfile.active(shipperUser, hub, "SHIPPER")));
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    // --- FinanceReconciliationServiceImpl reads by shipper ---

    @Test void managerOutsideTheShipperSubtreeCannotReadHistoryOrReadyPayouts() {
        LastMileTestFixture.login(outsideManager);
        assertThatThrownBy(() -> reconciliation.getReconciliationsByShipper(10L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reconciliation.getReadyPayouts(10L)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(reconciliations, cods);
    }

    @Test void managerWithoutAnActiveProfileAndOperationalRolesAreDenied() {
        for (User actor : List.of(unplacedManager, shipperUser, hubStaff)) {
            LastMileTestFixture.login(actor);
            assertThatThrownBy(() -> reconciliation.getReadyPayouts(10L)).as(actor.getUsername()).isInstanceOf(AccessDeniedException.class);
        }
        verifyNoInteractions(cods);
    }

    @Test void administratorReadsAnyShipperHistoryAndReadyPayouts() {
        LastMileTestFixture.login(admin);
        assertThat(reconciliation.getReconciliationsByShipper(10L)).isEmpty();
        assertThat(reconciliation.getReadyPayouts(10L)).isEmpty();
    }

    // --- ShipmentFinanceServiceImpl compensation and candidate lists ---

    @Test void managerOutsideTheIncidentHubCannotCompensateALoss() {
        Shipment shipment = Shipment.create("T", admin, "S", "1", "A", "R", "2", "B");
        shipment.setId(1L);
        Incident incident = Incident.create(1L, 7L, 5L, "MISSING_PARCEL", "missing", 100L);
        incident.setId(9L);
        when(shipments.findByIdForUpdate(1L)).thenReturn(Optional.of(shipment));
        when(incidents.findByIdForUpdate(9L)).thenReturn(Optional.of(incident));
        LastMileTestFixture.login(outsideManager);

        assertThatThrownBy(() -> finance.compensateLostShipment(1L, 9L, "confirmed")).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(ledger);
    }

    @Test void operationalRolesCannotListOrPriceFinanceCandidates() {
        for (User actor : List.of(shipperUser, hubStaff, unplacedManager)) {
            LastMileTestFixture.login(actor);
            assertThatThrownBy(finance::listPendingRefundCandidates).as(actor.getUsername()).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(finance::listConfirmedLossCandidates).as(actor.getUsername()).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> finance.calculateLostCompensation(mock(Shipment.class))).as(actor.getUsername())
                    .isInstanceOf(AccessDeniedException.class);
        }
        verifyNoInteractions(outbox, incidents);
    }

    @Test void administratorListsFinanceCandidates() {
        LastMileTestFixture.login(admin);
        assertThat(finance.listPendingRefundCandidates()).isEmpty();
        assertThat(finance.listConfirmedLossCandidates()).isEmpty();
    }

    // --- LastMileDeliveryServiceImpl assignment and custody ---

    @Test void anUnassignedShipperAtTheSameHubCannotActOnSomeoneElsesDelivery() {
        LastMileTestFixture f = new LastMileTestFixture();
        f.outbound();
        User other = otherShipper(f);
        int events = f.history.size();
        LastMileTestFixture.login(other);

        assertThatThrownBy(() -> f.service.recordCollection(new DeliveryCollectionRequest(10L, BigDecimal.ZERO, null, null)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> f.service.confirmDelivery(10L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> f.service.recordFailure(new DeliveryFailureRequest(10L, "CUSTOMER_UNREACHABLE", "n", null, null)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> f.service.getDeliveryTask(10L)).isInstanceOf(AccessDeniedException.class);

        assertThat(f.history).hasSize(events);
        assertThat(f.deliveries).isEmpty();
        assertThat(f.service.getAssignedDeliveryTasks()).isEmpty();
    }

    @Test void theAssignedShipperReadsTheirDeliveryTask() {
        LastMileTestFixture f = new LastMileTestFixture();
        f.outbound();
        when(f.assignments.findByAssigneeIdAndStatusIn(2L, List.of("ASSIGNED", "ACCEPTED", "IN_PROGRESS"))).thenReturn(f.tasks);

        assertThat(f.service.getDeliveryTask(10L)).isNotNull();
        assertThat(f.service.getAssignedDeliveryTasks()).hasSize(1);
    }

    @Test void nonShipperRolesCannotReadShipperTaskLists() {
        LastMileTestFixture f = new LastMileTestFixture();
        for (User actor : List.of(f.sender, f.inbound)) {
            LastMileTestFixture.login(actor);
            assertThatThrownBy(f.service::getAssignedDeliveryTasks).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(f.service::getAssignedReturnTasks).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> f.service.getDeliveryTask(10L)).isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test void anUnassignedShipperCannotScanOrConfirmAReturnDelivery() {
        LastMileTestFixture f = new LastMileTestFixture();
        f.returnAtOrigin();
        LastMileTestFixture.login(f.coordinator);
        f.dispatch.assign(new DeliveryAssignmentRequest(10L, 2L));
        User other = otherShipper(f);
        int events = f.history.size();
        LastMileTestFixture.login(other);

        assertThatThrownBy(() -> f.service.scanReturnDelivery(10L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> f.service.confirmReturnDelivery(10L)).isInstanceOf(AccessDeniedException.class);
        assertThat(f.history).hasSize(events);

        LastMileTestFixture.login(f.shipper);
        f.service.scanReturnDelivery(10L);
        assertThat(f.history).hasSize(events + 1);
    }

    private static User otherShipper(LastMileTestFixture f) {
        User other = new User(6L, "other", "hash", UserRole.ROLE_SHIPPER, "ACTIVE");
        when(f.profiles.findFirstByUserId(6L)).thenReturn(Optional.of(EmployeeProfile.active(other, f.hub, "SHIPPER")));
        when(f.assignments.findByAssigneeIdAndStatusIn(any(), any())).thenReturn(List.of());
        return other;
    }

    private static Hub hub(Long id) {
        Hub hub = Hub.active("H" + id, "Hub " + id, 1, "DELIVERY_HUB", "SOUTH", "P");
        ReflectionTestUtils.setField(hub, "id", id);
        return hub;
    }

}
