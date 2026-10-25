package vn.iotstar.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.dto.auth.RegisterUserRequest;
import vn.iotstar.dto.auth.RegistrationChallengeResponse;
import vn.iotstar.dto.auth.VerifyPhoneOtpRequest;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.PhoneSessionService;
import vn.iotstar.service.RegistrationService;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uses the real session/CSRF boundary and attacks request-supplied challenge identity/purpose. */
@WebMvcTest(RegistrationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class RegistrationControllerTest {
    private static final UUID CHALLENGE = UUID.fromString("d815745d-e2a5-41cb-9d48-4ba2d74b63a2");
    @Autowired private MockMvc mockMvc;
    @MockBean private RegistrationService registrationService;
    @MockBean private PhoneSessionService phoneSessionService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private JwtService jwtService;

    @Test
    void anonymousVisitorCanOpenRegistrationAndPhoneLogin() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk()).andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registerUserRequest"));
        mockMvc.perform(get("/auth/phone-login"))
                .andExpect(status().isOk()).andExpect(view().name("auth/phone-login"));
    }

    @Test
    void everyAuthenticationStateChangeStillRequiresCsrf() throws Exception {
        for (String path : new String[]{"/register", "/auth/phone-login", "/auth/verify-phone", "/auth/verify-phone/resend"}) {
            mockMvc.perform(post(path)).andExpect(status().isForbidden());
        }
        verifyNoInteractions(registrationService, phoneSessionService);
    }

    @Test
    void invalidRegistrationKeepsFieldFeedbackButClearsPassword() throws Exception {
        MvcResult result = mockMvc.perform(post("/register").with(csrf())
                        .param("password", "NeverEchoThisPassword"))
                .andExpect(status().isOk()).andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerUserRequest", "username", "phone"))
                .andReturn();
        RegisterUserRequest form = (RegisterUserRequest) result.getModelAndView().getModel().get("registerUserRequest");
        assertThat(form.getPassword()).isEmpty();
        verifyNoInteractions(registrationService);
    }

    @Test
    void successfulSignupStoresOnlyServerChallengeAndUsesPrg() throws Exception {
        when(registrationService.register(any())).thenReturn(challenge(CHALLENGE));
        MvcResult result = mockMvc.perform(post("/register").with(csrf())
                        .param("username", "new-user").param("password", "LocalTestPassword123!")
                        .param("fullName", "Nguyen Van A").param("phone", "0901234567")
                        .param("email", "sender@example.test").param("role", "ROLE_ADMIN")
                        .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/auth/verify-phone"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session.getAttribute(RegistrationController.CHALLENGE_ID)).isEqualTo(CHALLENGE);
        assertThat(session.getAttribute(RegistrationController.CHALLENGE_PURPOSE)).isEqualTo("REGISTRATION");
        assertThat(session.getAttribute(RegistrationController.CHALLENGE_PHONE)).isEqualTo("+84******567");
        assertThat(session.getAttribute("password")).isNull();
        verifyNoInteractions(phoneSessionService);
    }

    @Test
    void concurrentDuplicateRegistrationShowsGenericFeedbackAndClearsPassword() throws Exception {
        when(registrationService.register(any())).thenThrow(
                new DataIntegrityViolationException("SQL duplicate key in users for sender@example.test"));
        MvcResult result = mockMvc.perform(post("/register").with(csrf())
                        .param("username", "new-user").param("password", "LocalTestPassword123!")
                        .param("fullName", "Nguyen Van A").param("phone", "0901234567")
                        .param("email", "sender@example.test"))
                .andExpect(status().isOk()).andExpect(view().name("auth/register"))
                .andExpect(model().attribute("error", "Không thể gửi mã. Kiểm tra thông tin hoặc thử lại sau."))
                .andReturn();
        RegisterUserRequest form = (RegisterUserRequest) result.getModelAndView().getModel().get("registerUserRequest");
        assertThat(form.getPassword()).isEmpty();
        assertThat(result.getRequest().getSession(false).getAttribute(RegistrationController.CHALLENGE_ID)).isNull();
        verifyNoInteractions(phoneSessionService);
    }

    @Test
    void verificationIgnoresPostedChallengeAndPurposeAndNeverLogsInRegistration() throws Exception {
        when(registrationService.verify(any())).thenReturn(true);
        MockHttpSession session = session("REGISTRATION");
        mockMvc.perform(post("/auth/verify-phone").session(session).with(csrf())
                        .param("code", "123456").param("challengeId", UUID.randomUUID().toString())
                        .param("purpose", "LOGIN").param("userId", "99"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?registered=true"));
        ArgumentCaptor<VerifyPhoneOtpRequest> request = ArgumentCaptor.forClass(VerifyPhoneOtpRequest.class);
        verify(registrationService).verify(request.capture());
        assertThat(request.getValue().getChallengeId()).isEqualTo(CHALLENGE);
        assertThat(request.getValue().getCode()).isEqualTo("123456");
        assertThat(session.getAttribute(RegistrationController.CHALLENGE_ID)).isNull();
        verify(registrationService, never()).verifyLogin(any());
        verifyNoInteractions(phoneSessionService);
    }

    @Test
    void missingSessionCannotVerifyOrResendARequestSuppliedChallenge() throws Exception {
        mockMvc.perform(post("/auth/verify-phone").with(csrf()).param("code", "123456")
                        .param("challengeId", CHALLENGE.toString()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(post("/auth/verify-phone/resend").with(csrf()).param("challengeId", CHALLENGE.toString()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        verifyNoInteractions(registrationService, phoneSessionService);
    }

    @Test
    void malformedCodeNeverCallsVerificationAndIsClearedFromForm() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/verify-phone").session(session("REGISTRATION")).with(csrf())
                        .param("code", "secret-not-six-digits"))
                .andExpect(status().isOk()).andExpect(view().name("auth/verify-phone"))
                .andExpect(model().attributeHasFieldErrors("otpForm", "code"))
                .andReturn();
        RegistrationController.PhoneOtpForm form = (RegistrationController.PhoneOtpForm) result.getModelAndView().getModel().get("otpForm");
        assertThat(form.getCode()).isEmpty();
        verifyNoInteractions(registrationService);
    }

    @Test
    void wrongCodeRendersGenericFeedbackWithoutAuthenticating() throws Exception {
        when(registrationService.verify(any())).thenReturn(false);
        MvcResult result = mockMvc.perform(post("/auth/verify-phone").session(session("REGISTRATION")).with(csrf())
                        .param("code", "654321"))
                .andExpect(status().isOk()).andExpect(view().name("auth/verify-phone"))
                .andExpect(model().attributeExists("error")).andReturn();
        RegistrationController.PhoneOtpForm form = (RegistrationController.PhoneOtpForm) result.getModelAndView().getModel().get("otpForm");
        assertThat(form.getCode()).isEmpty();
        verifyNoInteractions(phoneSessionService);
    }

    @Test
    void phoneLoginChallengePurposeComesFromTheServer() throws Exception {
        when(registrationService.requestLogin(any())).thenReturn(challenge(CHALLENGE));
        MvcResult result = mockMvc.perform(post("/auth/phone-login").with(csrf())
                        .param("phone", "0901234567").param("purpose", "REGISTRATION"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/auth/verify-phone"))
                .andReturn();
        assertThat(result.getRequest().getSession(false).getAttribute(RegistrationController.CHALLENGE_PURPOSE)).isEqualTo("LOGIN");
    }

    @Test
    void loginUsesOnlyTheUserVerifiedByTheService() throws Exception {
        when(registrationService.verifyLogin(any())).thenReturn(Optional.of(7L));
        MockHttpSession session = session("LOGIN");
        mockMvc.perform(post("/auth/verify-phone").session(session).with(csrf())
                        .param("code", "123456").param("userId", "999"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/user/shipments/create"));
        verify(phoneSessionService).signIn(eq(7L), any(), any());
        verify(registrationService, never()).verify(any());
        assertThat(session.getAttribute(RegistrationController.CHALLENGE_ID)).isNull();
    }

    @Test
    void accountBlockedAfterVerificationReturnsToLoginWithoutReusableProof() throws Exception {
        when(registrationService.verifyLogin(any())).thenReturn(Optional.of(7L));
        doThrow(new BadCredentialsException("Account unavailable"))
                .when(phoneSessionService).signIn(eq(7L), any(), any());
        MockHttpSession session = session("LOGIN");
        mockMvc.perform(post("/auth/verify-phone").session(session).with(csrf()).param("code", "123456"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?phoneError=true"));
        assertThat(session.getAttribute(RegistrationController.CHALLENGE_ID)).isNull();
    }

    @Test
    void resendUsesSessionChallengeAndReplacesItWithoutChangingPurpose() throws Exception {
        UUID replacement = UUID.randomUUID();
        when(registrationService.resend(CHALLENGE)).thenReturn(challenge(replacement));
        MockHttpSession session = session("LOGIN");
        mockMvc.perform(post("/auth/verify-phone/resend").session(session).with(csrf())
                        .param("challengeId", UUID.randomUUID().toString()).param("purpose", "REGISTRATION"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/auth/verify-phone"))
                .andExpect(flash().attributeExists("notice"));
        assertThat(session.getAttribute(RegistrationController.CHALLENGE_ID)).isEqualTo(replacement);
        assertThat(session.getAttribute(RegistrationController.CHALLENGE_PURPOSE)).isEqualTo("LOGIN");
    }

    private MockHttpSession session(String purpose) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(RegistrationController.CHALLENGE_ID, CHALLENGE);
        session.setAttribute(RegistrationController.CHALLENGE_PHONE, "+84******567");
        session.setAttribute(RegistrationController.CHALLENGE_PURPOSE, purpose);
        return session;
    }

    private RegistrationChallengeResponse challenge(UUID id) {
        return new RegistrationChallengeResponse(id, Instant.parse("2026-10-22T10:10:00Z"), "+84******567");
    }
}
