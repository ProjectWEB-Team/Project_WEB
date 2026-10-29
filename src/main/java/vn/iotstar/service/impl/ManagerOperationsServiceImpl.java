package vn.iotstar.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.operations.ManagerOperationsDashboardResponse;
import vn.iotstar.dto.operations.OperationalWorkItemResponse;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.HubOperationAccessDeniedException;
import vn.iotstar.repository.ManagerOperationsRepository;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.ManagerOperationsService;

import java.util.List;

/** Server-derived supervisor scope and read-only KPI transaction boundary. */
@Service
public class ManagerOperationsServiceImpl implements ManagerOperationsService {
    private final ManagerOperationsRepository managerOperationsRepository;

    public ManagerOperationsServiceImpl(ManagerOperationsRepository managerOperationsRepository) {
        this.managerOperationsRepository = managerOperationsRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ManagerOperationsDashboardResponse getDashboard() {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        Scope scope = resolveScope(actor);
        List<Long> hubIds = scope.hubIds();
        if (hubIds.isEmpty()) return new ManagerOperationsDashboardResponse(scope.label(), scope.systemWide(), 0, 0, 0, 0, 0, 0, 0);
        return new ManagerOperationsDashboardResponse(
                scope.label(), scope.systemWide(),
                managerOperationsRepository.countInboundShipments(hubIds),
                managerOperationsRepository.countOutboundContainers(hubIds),
                managerOperationsRepository.countSortingBacklog(hubIds),
                managerOperationsRepository.countActiveHandovers(hubIds),
                managerOperationsRepository.countActiveTrips(hubIds),
                managerOperationsRepository.countBlockedRoutes(hubIds),
                managerOperationsRepository.countActionableIncidents(hubIds));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperationalWorkItemResponse> getWorkItems() {
        Scope scope = resolveScope(AuthenticatedActor.requireCurrent());
        if (scope.hubIds().isEmpty()) return List.of();
        return managerOperationsRepository.findWorkItems(scope.hubIds());
    }

    @Override
    @Transactional(readOnly = true)
    public OperationalWorkItemResponse getWorkItem(String kind, Long id) {
        Scope scope = resolveScope(AuthenticatedActor.requireCurrent());
        if (id == null || id <= 0 || !List.of("ROUTE", "MISSING", "HANDOVER").contains(kind) || scope.hubIds().isEmpty())
            throw new HubOperationAccessDeniedException();
        return managerOperationsRepository.findWorkItem(scope.hubIds(), kind, id)
                .orElseThrow(HubOperationAccessDeniedException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getHandoverDetail(Long id) {
        Scope scope = resolveScope(AuthenticatedActor.requireCurrent());
        if (id == null || id <= 0 || scope.hubIds().isEmpty()) throw new HubOperationAccessDeniedException();
        return managerOperationsRepository.handoverDetail(scope.hubIds(), id).orElseThrow(HubOperationAccessDeniedException::new);
    }

    @Override
    @Transactional
    public Long reassignHandoverDriver(Long handoverId, Long driverUserId) {
        Scope scope = resolveScope(AuthenticatedActor.requireCurrent());
        if (handoverId == null || driverUserId == null || scope.hubIds().isEmpty()) throw new HubOperationAccessDeniedException();
        Long created = managerOperationsRepository.reassignDriver(scope.hubIds(), handoverId, driverUserId);
        if (created == null) throw new HubOperationAccessDeniedException();
        return created;
    }

    private Scope resolveScope(AuthenticatedActor actor) {
        if (actor.hasRole(UserRole.ROLE_ADMIN)) {
            List<Long> allHubIds = managerOperationsRepository.findAllActiveHubIds();
            return new Scope("All active operations", true, allHubIds);
        }
        if (!actor.hasRole(UserRole.ROLE_MANAGER)) {
            throw new HubOperationAccessDeniedException();
        }
        EmployeeProfile profile = managerOperationsRepository.findActiveProfileByUserId(actor.userId())
                .orElseThrow(HubOperationAccessDeniedException::new);
        List<Long> hubIds = managerOperationsRepository.findHubSubtreeIds(profile.getHub().getId());
        if (hubIds.isEmpty()) {
            throw new HubOperationAccessDeniedException();
        }
        return new Scope(profile.getHub().getCode(), false, hubIds);
    }

    private record Scope(String label, boolean systemWide, List<Long> hubIds) { }
}
