package vn.iotstar.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.iotstar.entity.User;

/** Displays Google entry points only when its optional client configuration is present. */
@ControllerAdvice
public class AuthenticationViewAdvice {
    private final ObjectProvider<ClientRegistrationRepository> registrations;
    private final boolean emailEnabled;
    private final ObjectProvider<vn.iotstar.service.AvatarService> avatars;

    public AuthenticationViewAdvice(ObjectProvider<ClientRegistrationRepository> registrations,
                                    @Value("${uteexpress.mail.enabled:false}") boolean emailEnabled,
                                    ObjectProvider<vn.iotstar.service.AvatarService> avatars) {
        this.registrations = registrations;
        this.emailEnabled = emailEnabled;
        this.avatars = avatars;
    }

    @ModelAttribute("userAvatarUrl")
    public String userAvatarUrl(Authentication authentication) {
        var service = avatars.getIfAvailable();
        if (service != null && authentication != null && authentication.getPrincipal() instanceof User user && user.getId() != null) {
            return service.urlFor(user.getId()).orElse(null);
        }
        return null;
    }

    @ModelAttribute("googleLoginEnabled")
    public boolean googleLoginEnabled() {
        ClientRegistrationRepository repository = registrations.getIfAvailable();
        return repository != null && repository.findByRegistrationId("google") != null;
    }
    @ModelAttribute("emailLoginEnabled")
    public boolean emailLoginEnabled() { return emailEnabled; }

    @ModelAttribute("currentUser")
    public User currentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    @ModelAttribute("userDisplayName")
    public String userDisplayName(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            if (user.getFullName() != null && !user.getFullName().isBlank()) {
                return user.getFullName().trim();
            }
        }
        return authentication != null && authentication.getName() != null ? authentication.getName() : "";
    }
}
