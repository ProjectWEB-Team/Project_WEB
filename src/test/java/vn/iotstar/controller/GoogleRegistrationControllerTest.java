package vn.iotstar.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.dto.auth.GoogleRegistrationRequest;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.security.AccountSessionAuthenticator;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.security.VerifiedGoogleIdentity;
import vn.iotstar.service.GoogleAccountService;

import java.time.Clock;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(GoogleRegistrationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class GoogleRegistrationControllerTest {
    private static final Instant NOW = Instant.parse("2026-10-22T08:00:00Z");
    @Autowired private MockMvc mvc;
    @MockBean private GoogleAccountService accounts;
    @MockBean private AccountSessionAuthenticator sessions;
    @MockBean private Clock clock;
    @MockBean private UserDetailsService users;
    @MockBean private JwtService jwt;

    @Test
    void browserCannotInventGoogleIdentityWithoutServerProof() throws Exception {
        mvc.perform(post("/auth/google/complete").with(csrf())
                        .param("fullName", "Owner").param("phone", "0901234567")
                        .param("googleSubject", "attacker-sub").param("email", "victim@example.test"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?googleError=true"));
        verifyNoInteractions(accounts, sessions);
    }

    @Test
    void proofAloneDoesNotGrantApplicationAccess() throws Exception {
        mvc.perform(get("/user/shipments/create").session(proofSession()))
                .andExpect(status().is3xxRedirection());
        verifyNoInteractions(accounts, sessions);
    }

    @Test
    void completionRetainsCsrfProtection() throws Exception {
        mvc.perform(post("/auth/google/complete").session(proofSession())
                        .param("fullName", "Owner").param("phone", "0901234567"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(accounts, sessions);
    }

    @Test
    void expiredProofCannotCompleteRegistration() throws Exception {
        when(clock.instant()).thenReturn(NOW.plusSeconds(600));
        mvc.perform(post("/auth/google/complete").session(proofSession()).with(csrf())
                        .param("fullName", "Owner").param("phone", "0901234567"))
                .andExpect(redirectedUrl("/login?googleError=true"));
        verifyNoInteractions(accounts, sessions);
    }

    @Test
    void validCompletionUsesServerIdentityAndConsumesProof() throws Exception {
        when(clock.instant()).thenReturn(NOW);
        User user = new User(7L, "google-user", "hash", UserRole.ROLE_USER, "ACTIVE");
        when(accounts.register(any(), any())).thenReturn(user);
        MockHttpSession session = proofSession();

        mvc.perform(post("/auth/google/complete").session(session).with(csrf())
                        .param("fullName", "Owner").param("phone", "0901234567")
                        .param("googleSubject", "attacker-sub").param("email", "victim@example.test"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/user/shipments/create"));

        verify(accounts).register(eq(identity()), any(GoogleRegistrationRequest.class));
        verify(sessions).signIn(eq(user), any(), any());
        org.assertj.core.api.Assertions.assertThat(session.getAttribute(VerifiedGoogleIdentity.SESSION_ATTRIBUTE)).isNull();
    }

    @Test
    void requiredContactValidationKeepsTheForm() throws Exception {
        when(clock.instant()).thenReturn(NOW);
        mvc.perform(post("/auth/google/complete").session(proofSession()).with(csrf())
                        .param("fullName", "").param("phone", ""))
                .andExpect(status().isOk()).andExpect(view().name("auth/google-complete"))
                .andExpect(model().attributeHasFieldErrors("form", "fullName", "phone"));
        verifyNoInteractions(accounts, sessions);
    }

    private MockHttpSession proofSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(VerifiedGoogleIdentity.SESSION_ATTRIBUTE, identity());
        return session;
    }

    private VerifiedGoogleIdentity identity() {
        return new VerifiedGoogleIdentity("google-sub-123", "owner@example.test", "Owner", NOW);
    }
}
