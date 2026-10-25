package vn.iotstar.service.impl;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.UserRole;
import vn.iotstar.repository.EmployeeProfileRepository;
import vn.iotstar.repository.ManagerOperationsRepository;
import vn.iotstar.security.AuthenticatedActor;

import java.util.List;

/** One server-derived finance scope for commands, work lists and detail reads. */
@Component
public class FinanceAccess {
    private final ManagerOperationsRepository managers;
    private final EmployeeProfileRepository employees;

    public FinanceAccess(ManagerOperationsRepository managers, EmployeeProfileRepository employees) {
        this.managers = managers;
        this.employees = employees;
    }

    public Scope current() {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (actor.hasRole(UserRole.ROLE_ADMIN)) return new Scope(actor, true, List.of());
        if (!actor.hasRole(UserRole.ROLE_MANAGER)) throw denied();
        EmployeeProfile profile = managers.findActiveProfileByUserId(actor.userId()).orElseThrow(FinanceAccess::denied);
        if (profile.getHub() == null || !"ACTIVE".equals(profile.getHub().getStatus())) throw denied();
        return new Scope(actor, false, managers.findHubSubtreeIds(profile.getHub().getId()));
    }

    public EmployeeProfile shipper(Scope scope, Long userId) {
        EmployeeProfile profile = employees.findFirstByUserId(userId).orElseThrow(FinanceAccess::denied);
        if (!profile.isActive() || profile.getUser() == null || profile.getUser().getRole() != UserRole.ROLE_SHIPPER
                || !"ACTIVE".equals(profile.getUser().getStatus()) || profile.getHub() == null
                || !"ACTIVE".equals(profile.getHub().getStatus())) throw denied();
        scope.requireHub(profile.getHubId());
        return profile;
    }

    public record Scope(AuthenticatedActor actor, boolean admin, List<Long> hubIds) {
        public boolean contains(Long hubId) { return admin || (hubId != null && hubIds.contains(hubId)); }
        public void requireHub(Long hubId) { if (!contains(hubId)) throw denied(); }
    }

    private static AccessDeniedException denied() { return new AccessDeniedException("Finance resource is outside the permitted scope"); }
}
