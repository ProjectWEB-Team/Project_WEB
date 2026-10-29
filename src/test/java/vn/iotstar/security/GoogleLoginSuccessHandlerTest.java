package vn.iotstar.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.service.GoogleAccountService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleLoginSuccessHandlerTest {
    private static final Instant NOW = Instant.parse("2026-10-22T08:00:00Z");
    @Mock private GoogleAccountService accounts;
    @Mock private AccountSessionAuthenticator sessions;
    private GoogleLoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GoogleLoginSuccessHandler(accounts, sessions, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void unverifiedGoogleEmailIsRejectedWithoutAccountLookup() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication(false));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?googleError=true");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(accounts, sessions);
    }

    @Test
    void newGoogleIdentityGetsAnAccountWithoutAskingForAPhone() throws Exception {
        User account = new User(8L, "g_new", "hash", UserRole.ROLE_USER, "ACTIVE");
        when(accounts.findActiveAccount(any())).thenReturn(Optional.empty());
        when(accounts.register(any())).thenReturn(account);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication(true));

        verify(sessions).signIn(eq(account), eq(request), eq(response));
        assertThat(response.getRedirectedUrl()).isEqualTo("/user/shipments/create");
    }

    @Test
    void googleEmailOwnedByAnotherAccountReportsEmailError() throws Exception {
        when(accounts.findActiveAccount(any())).thenReturn(Optional.empty());
        when(accounts.register(any())).thenThrow(vn.iotstar.exception.RegistrationException.emailTaken());
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication(true));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?googleError=email");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(sessions);
    }

    @Test
    void linkedGoogleAccountUsesNormalApplicationPrincipal() throws Exception {
        User account = new User(7L, "google-user", "hash", UserRole.ROLE_USER, "ACTIVE");
        when(accounts.findActiveAccount(any())).thenReturn(Optional.of(account));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication(true));

        verify(sessions).signIn(eq(account), eq(request), eq(response));
        assertThat(response.getRedirectedUrl()).isEqualTo("/user/shipments/create");
        assertThat(request.getSession().getAttribute(VerifiedGoogleIdentity.SESSION_ATTRIBUTE)).isNull();
    }

    @Test
    void accountLookupFailureCannotLeaveGooglePrincipalAuthenticated() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationToken authentication = authentication(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());
        when(accounts.findActiveAccount(any())).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> handler.onAuthenticationSuccess(request, response, authentication))
                .isInstanceOf(IllegalStateException.class);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getSession().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                .isNull();
        verifyNoInteractions(sessions);
    }

    private OAuth2AuthenticationToken authentication(boolean verifiedEmail) {
        OidcIdToken token = OidcIdToken.withTokenValue("test-id-token")
                .subject("google-sub-123").issuer("https://accounts.google.com")
                .issuedAt(NOW.minusSeconds(10)).expiresAt(NOW.plusSeconds(600))
                .claim("email", "owner@example.test").claim("email_verified", verifiedEmail)
                .claim("name", "Owner").build();
        DefaultOidcUser user = new DefaultOidcUser(List.of(), token);
        return new OAuth2AuthenticationToken(user, user.getAuthorities(), "google");
    }
}
