package vn.iotstar.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import java.util.Set;

/** Server-derived identity passed to services for contextual authorization. */
public record AuthenticatedActor(Long userId, String username, Set<UserRole> roles) {
    public static AuthenticatedActor requireCurrent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User user)) {
            throw new AccessDeniedException("Authenticated UTEExpress user required");
        }
        return new AuthenticatedActor(user.getId(), user.getUsername(), Set.of(user.getRole()));
    }
    public boolean hasRole(UserRole role) { return roles.contains(role); }
}
