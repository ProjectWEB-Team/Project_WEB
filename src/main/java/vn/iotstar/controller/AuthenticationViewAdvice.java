package vn.iotstar.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Displays Google entry points only when its optional client configuration is present. */
@ControllerAdvice
public class AuthenticationViewAdvice {
    private final ObjectProvider<ClientRegistrationRepository> registrations;

    public AuthenticationViewAdvice(ObjectProvider<ClientRegistrationRepository> registrations) {
        this.registrations = registrations;
    }

    @ModelAttribute("googleLoginEnabled")
    public boolean googleLoginEnabled() {
        ClientRegistrationRepository repository = registrations.getIfAvailable();
        return repository != null && repository.findByRegistrationId("google") != null;
    }
}
