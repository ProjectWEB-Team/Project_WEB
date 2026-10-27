package vn.iotstar.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Displays Google entry points only when its optional client configuration is present. */
@ControllerAdvice
public class AuthenticationViewAdvice {
    private final ObjectProvider<ClientRegistrationRepository> registrations;
    private final boolean emailEnabled;

    public AuthenticationViewAdvice(ObjectProvider<ClientRegistrationRepository> registrations,
                                    @Value("${uteexpress.mail.enabled:false}") boolean emailEnabled) {
        this.registrations = registrations;
        this.emailEnabled = emailEnabled;
    }

    @ModelAttribute("googleLoginEnabled")
    public boolean googleLoginEnabled() {
        ClientRegistrationRepository repository = registrations.getIfAvailable();
        return repository != null && repository.findByRegistrationId("google") != null;
    }
    @ModelAttribute("emailLoginEnabled")
    public boolean emailLoginEnabled() { return emailEnabled; }
}
