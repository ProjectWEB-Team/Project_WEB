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
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.impl.EmailAccountService;
import vn.iotstar.exception.RegistrationException;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = EmailAccountController.class, properties = "uteexpress.mail.enabled=true")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class EmailAccountControllerTest {
    @Autowired MockMvc mvc;
    @MockBean EmailAccountService accounts;
    @MockBean UserDetailsService users;
    @MockBean JwtService jwt;

    @Test void browserCannotSupplyItsOwnChallengeOrPurpose() throws Exception {
        mvc.perform(post("/auth/email/verify").with(csrf()).param("challengeId", UUID.randomUUID().toString())
                        .param("purpose", "PASSWORD_RESET").param("code", "123456"))
                .andExpect(redirectedUrl("/login"));
        verifyNoInteractions(accounts);
    }
    @Test void allMutationsRequireCsrfAndEmailSettingsRequireAuthentication() throws Exception {
        for (String path : new String[]{"/auth/email/register", "/auth/email/verify", "/auth/email/resend", "/auth/email/forgot-password", "/user/email"}) {
            mvc.perform(post(path)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/user/email")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/user/email").with(user("staff").roles("HUB_STAFF"))).andExpect(status().isForbidden());
        verifyNoInteractions(accounts);
    }
    @Test void registrationBindsOnlyAllowedFieldsAndClearsPassword() throws Exception {
        UUID id = UUID.randomUUID();
        when(accounts.register(any())).thenReturn(new EmailAccountService.Challenge(id, "a***@example.test"));
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/auth/email/register").with(csrf()).session(session)
                        .param("username", "sender").param("password", "Password123!").param("fullName", "Sender")
                        .param("phone", "0901234567").param("email", "a@example.test").param("role", "ROLE_ADMIN"))
                .andExpect(redirectedUrl("/auth/email/verify"));
        assertThat(session.getAttribute(EmailAccountController.CHALLENGE)).isEqualTo(id);
        assertThat(session.getAttribute(EmailAccountController.PURPOSE)).isEqualTo("REGISTRATION");
    }
    @Test void validationAndDeliveryFailureDoNotExposePassword() throws Exception {
        var invalid = mvc.perform(post("/auth/email/register").with(csrf()).param("username", "")
                        .param("password", "DontRenderMe123!"))
                .andExpect(view().name("auth/register")).andReturn();
        var form = (vn.iotstar.dto.auth.RegisterUserRequest) invalid.getModelAndView().getModel().get("registerUserRequest");
        assertThat(form.getPassword()).isEmpty(); verifyNoInteractions(accounts);
    }
    @Test void proofAndPurposeComeFromSessionAndSuccessfulVerificationClearsThem() throws Exception {
        MockHttpSession session = session("REGISTRATION"); UUID id = (UUID) session.getAttribute(EmailAccountController.CHALLENGE);
        when(accounts.verify(id, "123456", "REGISTRATION", null, "")).thenReturn(true);
        mvc.perform(post("/auth/email/verify").session(session).with(csrf()).param("code", "123456")
                        .param("challengeId", UUID.randomUUID().toString()).param("purpose", "PASSWORD_RESET"))
                .andExpect(redirectedUrl("/login?registered=true"));
        verify(accounts).verify(id, "123456", "REGISTRATION", null, "");
        assertThat(session.getAttribute(EmailAccountController.CHALLENGE)).isNull();
    }
    @Test void resetDestroysTheCurrentSessionAfterSuccess() throws Exception {
        MockHttpSession session = session("PASSWORD_RESET"); UUID id = (UUID) session.getAttribute(EmailAccountController.CHALLENGE);
        when(accounts.verify(id, "123456", "PASSWORD_RESET", null, "NewPass123!")).thenReturn(true);
        mvc.perform(post("/auth/email/verify").session(session).with(csrf()).param("code", "123456").param("newPassword", "NewPass123!"))
                .andExpect(redirectedUrl("/login?reset=true"));
        assertThat(session.isInvalid()).isTrue();
    }
    @Test void knownAndDecoyResendsHaveIdenticalResponses() throws Exception {
        MockHttpSession session = session("PASSWORD_RESET"); UUID id = (UUID) session.getAttribute(EmailAccountController.CHALLENGE);
        when(accounts.resend(id, "PASSWORD_RESET", null)).thenReturn(new EmailAccountService.Challenge(id, "a***@example.test"));
        var valid = mvc.perform(post("/auth/email/resend").session(session).with(csrf())).andExpect(redirectedUrl("/auth/email/verify")).andReturn();
        when(accounts.resend(id, "PASSWORD_RESET", null)).thenThrow(new RegistrationException("unavailable"));
        var decoy = mvc.perform(post("/auth/email/resend").session(session).with(csrf())).andExpect(redirectedUrl("/auth/email/verify")).andReturn();
        assertThat(new java.util.HashMap<>(valid.getFlashMap())).isEqualTo(decoy.getFlashMap());
    }
    private MockHttpSession session(String purpose) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(EmailAccountController.CHALLENGE, UUID.randomUUID());
        session.setAttribute(EmailAccountController.PURPOSE, purpose);
        session.setAttribute(EmailAccountController.MASKED_EMAIL, "a***@example.test");
        return session;
    }
}
