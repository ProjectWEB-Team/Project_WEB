package vn.iotstar.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.operations.ManagerOperationsDashboardResponse;
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
