package vn.iotstar.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.dto.operations.ManagerOperationsDashboardResponse;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.HubOperationAccessDeniedException;
import vn.iotstar.repository.ManagerOperationsRepository;
import vn.iotstar.service.impl.ManagerOperationsServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerOperationsServiceImplTest {
    @Mock private ManagerOperationsRepository managerOperationsRepository;
    @InjectMocks private ManagerOperationsServiceImpl service;

    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test void managerUsesOnlyServerDerivedHubSubtreeForEveryKpi() {
        User manager = user(71L, UserRole.ROLE_MANAGER); Hub root = hub(11L, "SOC-A"); authenticate(manager);
        when(managerOperationsRepository.findActiveProfileByUserId(71L)).thenReturn(Optional.of(EmployeeProfile.active(manager, root, "MANAGER")));
        when(managerOperationsRepository.findHubSubtreeIds(11L)).thenReturn(List.of(11L, 12L));
        stubCounts(3, 4, 5, 6, 7, 8, 9);

        ManagerOperationsDashboardResponse dashboard = service.getDashboard();

        assertThat(dashboard.scopeLabel()).isEqualTo("SOC-A");
        assertThat(dashboard.systemWide()).isFalse();
        assertThat(dashboard.inboundCount()).isEqualTo(3);
        assertThat(dashboard.actionableIncidentCount()).isEqualTo(9);
        ArgumentCaptor<List<Long>> scope = ArgumentCaptor.forClass(List.class);
        verify(managerOperationsRepository).countInboundShipments(scope.capture());
        assertThat(scope.getValue()).containsExactly(11L, 12L);
        verify(managerOperationsRepository).countActiveTrips(List.of(11L, 12L));
        verify(managerOperationsRepository).countBlockedRoutes(List.of(11L, 12L));
    }

    @Test void inactiveOrNonManagerActorCannotReachDashboard() {
        User manager = user(72L, UserRole.ROLE_MANAGER); authenticate(manager);
        when(managerOperationsRepository.findActiveProfileByUserId(72L)).thenReturn(Optional.empty());
        assertThatThrownBy(service::getDashboard).isInstanceOf(HubOperationAccessDeniedException.class);

        authenticate(user(73L, UserRole.ROLE_HUB_STAFF));
        assertThatThrownBy(service::getDashboard).isInstanceOf(HubOperationAccessDeniedException.class);
        verify(managerOperationsRepository, never()).findHubSubtreeIds(anyLong());
    }

    @Test void adminUsesExplicitSystemWideProjection() {
        authenticate(user(99L, UserRole.ROLE_ADMIN));
        when(managerOperationsRepository.findAllActiveHubIds()).thenReturn(List.of(11L, 12L, 99L));
        stubCounts(1, 1, 1, 1, 1, 1, 1);
        assertThat(service.getDashboard().systemWide()).isTrue();
        verify(managerOperationsRepository).findAllActiveHubIds();
        verify(managerOperationsRepository, never()).findActiveProfileByUserId(anyLong());
    }

    private void stubCounts(long inbound, long outbound, long backlog, long handovers, long trips, long blocked, long incidents) {
        when(managerOperationsRepository.countInboundShipments(anyList())).thenReturn(inbound);
        when(managerOperationsRepository.countOutboundContainers(anyList())).thenReturn(outbound);
        when(managerOperationsRepository.countSortingBacklog(anyList())).thenReturn(backlog);
        when(managerOperationsRepository.countActiveHandovers(anyList())).thenReturn(handovers);
        when(managerOperationsRepository.countActiveTrips(anyList())).thenReturn(trips);
        when(managerOperationsRepository.countBlockedRoutes(anyList())).thenReturn(blocked);
        when(managerOperationsRepository.countActionableIncidents(anyList())).thenReturn(incidents);
    }

    @Test void scopedWorkItemListsAndDetailsNeverQueryAnUnscopedProjection() {
        User manager = user(71L, UserRole.ROLE_MANAGER); authenticate(manager);
        when(managerOperationsRepository.findActiveProfileByUserId(71L)).thenReturn(Optional.of(EmployeeProfile.active(manager, hub(11L, "SOC-A"), "MANAGER")));
        when(managerOperationsRepository.findHubSubtreeIds(11L)).thenReturn(List.of(11L, 12L));
        var item = new vn.iotstar.dto.operations.OperationalWorkItemResponse("MISSING", 4L, 101L, 12L, "Missing parcel requires investigation");
        when(managerOperationsRepository.findWorkItems(List.of(11L, 12L))).thenReturn(List.of(item));
        when(managerOperationsRepository.findWorkItem(List.of(11L, 12L), "MISSING", 4L)).thenReturn(Optional.of(item));
        when(managerOperationsRepository.findWorkItem(List.of(11L, 12L), "MISSING", 99L)).thenReturn(Optional.empty());
        assertThat(service.getWorkItems()).containsExactly(item);
        assertThat(service.getWorkItem("MISSING", 4L)).isEqualTo(item);
        assertThatThrownBy(() -> service.getWorkItem("MISSING", 99L)).isInstanceOf(HubOperationAccessDeniedException.class);
        assertThatThrownBy(() -> service.getWorkItem("PAYMENT", 4L)).isInstanceOf(HubOperationAccessDeniedException.class);
        verify(managerOperationsRepository, never()).findWorkItem(anyList(), eq("PAYMENT"), anyLong());
    }

    @Test void reassigningAHandoverIsScopedToTheManagersHubsAndNeverTouchesOtherOnes() {
        User manager = user(71L, UserRole.ROLE_MANAGER); authenticate(manager);
        when(managerOperationsRepository.findActiveProfileByUserId(71L)).thenReturn(Optional.of(EmployeeProfile.active(manager, hub(11L, "SOC-A"), "MANAGER")));
        when(managerOperationsRepository.findHubSubtreeIds(11L)).thenReturn(List.of(11L, 12L));
        when(managerOperationsRepository.reassignDriver(List.of(11L, 12L), 5L, 30L)).thenReturn(6L);
        when(managerOperationsRepository.reassignDriver(List.of(11L, 12L), 9L, 30L)).thenReturn(null);

        assertThat(service.reassignHandoverDriver(5L, 30L)).isEqualTo(6L);
        assertThatThrownBy(() -> service.reassignHandoverDriver(9L, 30L)).isInstanceOf(HubOperationAccessDeniedException.class);
        assertThatThrownBy(() -> service.reassignHandoverDriver(5L, null)).isInstanceOf(HubOperationAccessDeniedException.class);
    }

    @Test void nonManagerCannotReadWorkItemDetailsOrList() {
        authenticate(user(7L, UserRole.ROLE_USER));
        assertThatThrownBy(service::getWorkItems).isInstanceOf(HubOperationAccessDeniedException.class);
        assertThatThrownBy(() -> service.getWorkItem("MISSING", 1L)).isInstanceOf(HubOperationAccessDeniedException.class);
        verifyNoInteractions(managerOperationsRepository);
    }
    private static User user(Long id, UserRole role) { return new User(id, role.name().toLowerCase(), "hash", role, "ACTIVE"); }
    private static Hub hub(Long id, String code) { Hub hub = Hub.active(code, code, 1, "SOC", "SOUTH", "HCM"); ReflectionTestUtils.setField(hub, "id", id); return hub; }
    private static void authenticate(User user) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())); }
}
