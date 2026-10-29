package vn.iotstar.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.UserProfileService;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@WebMvcTest(UserProfileController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UserProfileControllerTest {
    @Autowired MockMvc mvc;
    @MockBean UserProfileService profileService;
    @MockBean UserDetailsService userDetailsService;
    @MockBean JwtService jwt;

    private User sampleUser() {
        User user = User.staff("customer", "hash", "Khách hàng mẫu", "+84987654321", "customer@example.com", UserRole.ROLE_USER, Instant.now());
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private void stubSessionCheck(User user) {
        when(userDetailsService.loadUserByUsername(user.getUsername())).thenReturn(user);
    }

    @Test
    void unauthenticatedUserIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/user/profile")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void authenticatedUserCanViewProfile() throws Exception {
        User user = sampleUser();
        stubSessionCheck(user);
        when(profileService.getUser(anyLong())).thenReturn(user);

        mvc.perform(get("/user/profile").with(authentication(
                new UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(view().name("user/profile"))
                .andExpect(model().attributeExists("profileUser"));
    }

    @Test
    void postEndpointsRequireCsrf() throws Exception {
        mvc.perform(post("/user/profile/update-info").with(authentication(
                new UsernamePasswordAuthenticationToken(sampleUser(), "password", sampleUser().getAuthorities()))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/user/profile/phone/send-otp").with(authentication(
                new UsernamePasswordAuthenticationToken(sampleUser(), "password", sampleUser().getAuthorities()))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/user/profile/phone/verify-otp").with(authentication(
                new UsernamePasswordAuthenticationToken(sampleUser(), "password", sampleUser().getAuthorities()))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/user/profile/email/send-otp").with(authentication(
                new UsernamePasswordAuthenticationToken(sampleUser(), "password", sampleUser().getAuthorities()))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/user/profile/email/verify-otp").with(authentication(
                new UsernamePasswordAuthenticationToken(sampleUser(), "password", sampleUser().getAuthorities()))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/user/profile/change-password").with(authentication(
                new UsernamePasswordAuthenticationToken(sampleUser(), "password", sampleUser().getAuthorities()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateFullNameCallsServiceAndRedirects() throws Exception {
        User user = sampleUser();
        stubSessionCheck(user);
        when(profileService.getUser(anyLong())).thenReturn(user);

        mvc.perform(post("/user/profile/update-info")
                        .with(authentication(
                                new UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities()))).with(csrf())
                        .param("fullName", "Tên Mới"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/profile"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(profileService).updateFullName(eq(user.getId()), eq("Tên Mới"));
    }

    @Test
    void sendPhoneOtpSetsSessionAndRedirects() throws Exception {
        User user = sampleUser();
        stubSessionCheck(user);
        UUID challengeId = UUID.randomUUID();
        when(profileService.getUser(anyLong())).thenReturn(user);
        when(profileService.requestPhoneVerification(anyLong(), any())).thenReturn(challengeId);

        mvc.perform(post("/user/profile/phone/send-otp")
                        .with(authentication(
                                new UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities()))).with(csrf())
                        .param("phone", "0912345678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/profile#phone-section"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(profileService).requestPhoneVerification(eq(user.getId()), eq("0912345678"));
    }

    @Test
    void verifyPhoneOtpSucceedsWithValidCode() throws Exception {
        User user = sampleUser();
        stubSessionCheck(user);
        UUID challengeId = UUID.randomUUID();
        when(profileService.getUser(anyLong())).thenReturn(user);
        when(profileService.verifyPhoneOtp(anyLong(), eq(challengeId), eq("123456"))).thenReturn(true);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(UserProfileController.PHONE_CHALLENGE_ID, challengeId);

        mvc.perform(post("/user/profile/phone/verify-otp")
                        .with(authentication(
                                new UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities()))).with(csrf()).session(session)
                        .param("code", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/profile#phone-section"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(profileService).verifyPhoneOtp(eq(user.getId()), eq(challengeId), eq("123456"));
    }

    @Test
    void sendEmailOtpSetsSessionAndRedirects() throws Exception {
        User user = sampleUser();
        stubSessionCheck(user);
        UUID challengeId = UUID.randomUUID();
        when(profileService.getUser(anyLong())).thenReturn(user);
        when(profileService.requestEmailVerification(anyLong(), any())).thenReturn(challengeId);

        mvc.perform(post("/user/profile/email/send-otp")
                        .with(authentication(
                                new UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities()))).with(csrf())
                        .param("email", "newemail@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/profile#email-section"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(profileService).requestEmailVerification(eq(user.getId()), eq("newemail@example.com"));
    }

    @Test
    void verifyEmailOtpSucceedsWithValidCode() throws Exception {
        User user = sampleUser();
        stubSessionCheck(user);
        UUID challengeId = UUID.randomUUID();
        when(profileService.getUser(anyLong())).thenReturn(user);
        when(profileService.verifyEmailOtp(anyLong(), eq(challengeId), eq("654321"))).thenReturn(true);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(UserProfileController.EMAIL_CHALLENGE_ID, challengeId);

        mvc.perform(post("/user/profile/email/verify-otp")
                        .with(authentication(
                                new UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities()))).with(csrf()).session(session)
                        .param("code", "654321"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/profile#email-section"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(profileService).verifyEmailOtp(eq(user.getId()), eq(challengeId), eq("654321"));
    }

    @Test
    void changePasswordRejectsMismatchedConfirmation() throws Exception {
        User user = sampleUser();
        stubSessionCheck(user);

        mvc.perform(post("/user/profile/change-password")
                        .with(authentication(
                                new UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities()))).with(csrf())
                        .param("currentPassword", "oldPass123!")
                        .param("newPassword", "newPass123!")
                        .param("confirmPassword", "differentPass!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/profile#security-section"))
                .andExpect(flash().attributeExists("passwordError"));

        verifyNoInteractions(profileService);
    }
}

