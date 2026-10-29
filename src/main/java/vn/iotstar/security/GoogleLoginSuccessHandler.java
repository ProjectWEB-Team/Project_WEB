package vn.iotstar.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import vn.iotstar.entity.User;
import vn.iotstar.exception.GoogleAccountException;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.service.GoogleAccountService;

import java.io.IOException;
import java.time.Clock;
import java.util.Optional;

/** Spring validates the OIDC signature/issuer/audience before this handler runs. */
@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final GoogleAccountService accounts;
    private final AccountSessionAuthenticator sessionAuthenticator;
    private final Clock clock;
    private final HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    public GoogleLoginSuccessHandler(GoogleAccountService accounts,
                                     AccountSessionAuthenticator sessionAuthenticator, Clock clock) {
        this.accounts = accounts;
        this.sessionAuthenticator = sessionAuthenticator;
        this.clock = clock;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth)
                || !"google".equals(oauth.getAuthorizedClientRegistrationId())
                || !(oauth.getPrincipal() instanceof OidcUser principal)) {
            reject(request, response);
            return;
        }
        OidcIdToken idToken = principal.getIdToken();
        String issuer = idToken.getIssuer() == null ? "" : idToken.getIssuer().toString();
        if (!Boolean.TRUE.equals(idToken.getEmailVerified())
                || !("https://accounts.google.com".equals(issuer) || "accounts.google.com".equals(issuer))) {
            reject(request, response);
            return;
        }
        VerifiedGoogleIdentity identity = new VerifiedGoogleIdentity(idToken.getSubject(), idToken.getEmail(),
                idToken.getFullName(), clock.instant());
        // The framework's OAuth principal is not an application account. Remove it before any
        // account lookup so even a database failure cannot leave an onboarding user authenticated.
        request.getSession().removeAttribute(VerifiedGoogleIdentity.SESSION_ATTRIBUTE);
        clearAuthentication(request, response);
        try {
            User account = accounts.findActiveAccount(identity).orElseGet(() -> accounts.register(identity));
            sessionAuthenticator.signIn(account, request, response);
            response.sendRedirect(request.getContextPath() + "/user/shipments/create");
        } catch (RegistrationException exception) {
            clearAuthentication(request, response);
            response.sendRedirect(request.getContextPath() + "/login?googleError=email");
        } catch (GoogleAccountException exception) {
            reject(request, response);
        }
    }

    public void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getSession(false) != null) {
            request.getSession(false).removeAttribute(VerifiedGoogleIdentity.SESSION_ATTRIBUTE);
        }
        clearAuthentication(request, response);
        response.sendRedirect(request.getContextPath() + "/login?googleError=true");
    }

    private void clearAuthentication(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        contexts.saveContext(SecurityContextHolder.createEmptyContext(), request, response);
    }
}
