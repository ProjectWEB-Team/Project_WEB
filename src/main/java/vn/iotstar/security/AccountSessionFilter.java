package vn.iotstar.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.iotstar.entity.User;
import java.io.IOException;
import java.util.Objects;

/** Rechecks the database so password resets and administrative revocation affect every session. */
public class AccountSessionFilter extends OncePerRequestFilter {
    private final UserDetailsService users;
    public AccountSessionFilter(UserDetailsService users) { this.users = users; }
    public static boolean matches(User snapshot, UserDetails current) {
        return current instanceof User user && user.isEnabled() && user.isAccountNonLocked()
                && Objects.equals(snapshot.getId(), user.getId()) && snapshot.getSecurityVersion() == user.getSecurityVersion()
                && snapshot.getRole() == user.getRole() && Objects.equals(snapshot.getPassword(), user.getPassword());
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof User snapshot) {
            boolean valid;
            try { valid = matches(snapshot, users.loadUserByUsername(snapshot.getUsername())); }
            catch (AuthenticationException exception) { valid = false; }
            if (!valid) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) session.invalidate();
            }
        }
        chain.doFilter(request, response);
    }
}
