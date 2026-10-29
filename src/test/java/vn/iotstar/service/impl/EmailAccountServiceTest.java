package vn.iotstar.service.impl;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.config.OtpProperties;
import vn.iotstar.dto.auth.RegisterUserRequest;
import vn.iotstar.entity.*;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.repository.*;
import vn.iotstar.service.auth.AccountEmailSender;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailAccountServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-28T00:00:00Z");
    private static ValidatorFactory validation;
    @Mock UserRepository users;
    @Mock EmailOtpChallengeRepository challenges;
    @Mock AccountEmailSender mail;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final OtpProperties policy = new OtpProperties();
    private EmailAccountService service;
    @BeforeAll static void initValidation() { validation = Validation.buildDefaultValidatorFactory(); }
    @AfterAll static void closeValidation() { validation.close(); }
    @BeforeEach void setup() { service = at(NOW); }
    private EmailAccountService at(Instant time) {
        return new EmailAccountService(users, challenges, encoder, mail, Clock.fixed(time, ZoneOffset.UTC), policy, validation.getValidator());
    }
    private User pending() {
        User user = User.registerPending("sender", encoder.encode("Password123!"), "Sender", "+84901234567", "a@example.test", NOW);
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }
    private RegisterUserRequest form() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setUsername("sender"); request.setPassword("Password123!"); request.setFullName("Sender");
        request.setPhone("0901234567"); request.setEmail("A@Example.test");
        return request;
    }
    private EmailOtpChallenge challenge(User user, String purpose) {
        EmailOtpChallenge challenge = EmailOtpChallenge.create(user, purpose, encoder.encode("123456"), NOW, policy);
        when(challenges.findByIdForUpdate(challenge.getId())).thenReturn(Optional.of(challenge));
        return challenge;
    }
    @Test void registrationNormalizesAndSendsOnlyHashedProofToStorage() {
        when(users.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        var result = service.register(form());
        ArgumentCaptor<EmailOtpChallenge> saved = ArgumentCaptor.forClass(EmailOtpChallenge.class);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(challenges).saveAndFlush(saved.capture());
        verify(mail).sendOtp(eq("a@example.test"), code.capture(), eq(NOW.plusSeconds(300)));
        assertThat(code.getValue()).matches("[0-9]{6}");
        assertThat(encoder.matches(code.getValue(), saved.getValue().getCodeHash())).isTrue();
        assertThat(saved.getValue().getUser().isEnabled()).isFalse();
        assertThat(saved.getValue().getUser().getPhone()).isEqualTo("+84901234567");
        assertThat(result.toString()).doesNotContain(code.getValue(), "a@example.test", "+84901234567");
    }
    @Test void verificationActivatesOnlyEmailAndCannotBeReplayed() {
        User user = pending(); var challenge = challenge(user, "REGISTRATION");
        assertThat(service.verify(challenge.getId(), "123456", "REGISTRATION", null, null)).isTrue();
        assertThat(user.isEnabled()).isTrue(); assertThat(user.isEmailVerified()).isTrue(); assertThat(user.isPhoneVerified()).isFalse();
        assertThat(service.verify(challenge.getId(), "123456", "REGISTRATION", null, null)).isFalse();
    }
    @Test void wrongCodesCommitAttemptCounterAndLockOut() {
        var challenge = challenge(pending(), "REGISTRATION");
        for (int i = 0; i < policy.getMaxAttempts(); i++) assertThat(service.verify(challenge.getId(), "654321", "REGISTRATION", null, null)).isFalse();
        assertThat(challenge.getFailedAttempts()).isEqualTo(5);
        assertThat(service.verify(challenge.getId(), "123456", "REGISTRATION", null, null)).isFalse();
    }
    @Test void checkingACodeDoesNotConsumeItButWrongCodesStillCount() {
        User user = pending(); user.verifyEmail(NOW); var challenge = challenge(user, "PASSWORD_RESET");
        assertThat(service.check(challenge.getId(), "654321", "PASSWORD_RESET", null)).isFalse();
        assertThat(challenge.getFailedAttempts()).isEqualTo(1);
        assertThat(service.check(challenge.getId(), "123456", "PASSWORD_RESET", null)).isTrue();
        assertThat(service.check(challenge.getId(), "123456", "PASSWORD_RESET", null)).isTrue();
        assertThat(service.verify(challenge.getId(), "123456", "PASSWORD_RESET", null, "NewPassword123!")).isTrue();
    }
    @Test void expiryAndWrongPurposeCannotActivate() {
        var challenge = challenge(pending(), "REGISTRATION");
        assertThat(service.verify(challenge.getId(), "123456", "PASSWORD_RESET", null, "NewPass123!")).isFalse();
        assertThat(at(NOW.plusSeconds(300)).verify(challenge.getId(), "123456", "REGISTRATION", null, null)).isFalse();
        assertThat(challenge.getUser().isEnabled()).isFalse();
    }
    @Test void lockedStaffAndChangedEmailCannotUseProof() {
        User user = pending(); var challenge = challenge(user, "REGISTRATION");
        user.changeStatus("LOCKED");
        assertThat(service.verify(challenge.getId(), "123456", "REGISTRATION", null, null)).isFalse();
        user.changeStatus("INACTIVE"); user.changeRole(UserRole.ROLE_ADMIN);
        assertThat(service.verify(challenge.getId(), "123456", "REGISTRATION", null, null)).isFalse();
        user.changeRole(UserRole.ROLE_USER); ReflectionTestUtils.setField(user, "email", "changed@example.test");
        assertThat(service.verify(challenge.getId(), "123456", "REGISTRATION", null, null)).isFalse();
    }
    @Test void existingPhoneOnlyAccountCannotResetThroughUnverifiedEmail() {
        User user = pending(); user.verifyPhone(NOW);
        when(users.findByEmailForUpdate(user.getEmail())).thenReturn(Optional.of(user));
        var response = service.requestPasswordReset(user.getEmail());
        assertThat(response.id()).isNotNull(); verifyNoInteractions(mail, challenges);
    }
    @Test void unknownAndStaffAddressesHaveDecoyChallenges() {
        var unknown = service.requestPasswordReset("unknown@example.test");
        User staff = pending(); staff.verifyEmail(NOW); staff.changeRole(UserRole.ROLE_ADMIN);
        when(users.findByEmailForUpdate(staff.getEmail())).thenReturn(Optional.of(staff));
        assertThat(service.requestPasswordReset(staff.getEmail()).id()).isNotEqualTo(unknown.id());
        verifyNoInteractions(mail, challenges);
    }
    @Test void passwordResetDoesNotVerifyPhoneAndRejectsWeakPasswordAndReplay() {
        User user = pending(); user.verifyEmail(NOW); var challenge = challenge(user, "PASSWORD_RESET");
        assertThat(service.verify(challenge.getId(), "123456", "PASSWORD_RESET", null, "short")).isFalse();
        assertThat(service.verify(challenge.getId(), "123456", "PASSWORD_RESET", null, "é".repeat(37))).isFalse();
        assertThat(service.verify(challenge.getId(), "123456", "PASSWORD_RESET", null, "NewPassword123!")).isTrue();
        assertThat(encoder.matches("NewPassword123!", user.getPassword())).isTrue(); assertThat(user.isPhoneVerified()).isFalse();
        assertThat(service.verify(challenge.getId(), "123456", "PASSWORD_RESET", null, "OtherPassword!")).isFalse();
    }
    @Test void signedInVerificationRequiresTheSameCustomer() {
        User user = pending(); user.verifyPhone(NOW); var challenge = challenge(user, "VERIFY_EMAIL");
        assertThat(service.verify(challenge.getId(), "123456", "VERIFY_EMAIL", null, null)).isFalse();
        assertThat(service.verify(challenge.getId(), "123456", "VERIFY_EMAIL", 8L, null)).isFalse();
        assertThat(service.verify(challenge.getId(), "123456", "VERIFY_EMAIL", 7L, null)).isTrue();
        assertThat(user.isEmailVerified()).isTrue();
    }
    @Test void resendEnforcesCooldownAndInvalidatesOldDigits() {
        var challenge = challenge(pending(), "REGISTRATION");
        assertThatThrownBy(() -> service.resend(challenge.getId(), "REGISTRATION", null)).isInstanceOf(RegistrationException.class);
        verifyNoInteractions(mail);
        var later = at(NOW.plusSeconds(60)); later.resend(challenge.getId(), "REGISTRATION", null);
        assertThat(later.verify(challenge.getId(), "123456", "REGISTRATION", null, null)).isFalse();
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mail).sendOtp(eq(challenge.getEmail()), code.capture(), eq(challenge.getExpiresAt()));
        assertThat(later.verify(challenge.getId(), code.getValue(), "REGISTRATION", null, null)).isTrue();
    }
    @Test void sendWindowCannotBeResetBySwitchingPurpose() {
        User user = pending(); user.verifyEmail(NOW);
        EmailOtpChallenge challenge = EmailOtpChallenge.create(user, "PASSWORD_RESET", "hash", NOW, policy);
        for (int i = 1; i < 5; i++) challenge.issue("PASSWORD_RESET", "hash", NOW.plusSeconds(60L * i), policy);
        assertThatThrownBy(() -> challenge.issue("VERIFY_EMAIL", "hash", NOW.plusSeconds(300), policy)).isInstanceOf(RegistrationException.class);
        challenge.issue("PASSWORD_RESET", "hash", NOW.plusSeconds(3600), policy);
        assertThat(challenge.getSendCount()).isEqualTo(1);
    }
    @Test void lostRegistrationSessionResumesOnlyMatchingCredentials() {
        User user = pending();
        EmailOtpChallenge challenge = EmailOtpChallenge.create(user, "REGISTRATION", encoder.encode("123456"), NOW, policy);
        when(users.findByEmailForUpdate(user.getEmail())).thenReturn(Optional.of(user));
        when(challenges.findByUserIdForUpdate(7L)).thenReturn(Optional.of(challenge));
        assertThat(at(NOW.plusSeconds(60)).register(form()).id()).isEqualTo(challenge.getId());
        var wrong = form(); wrong.setPassword("SomebodyElse!");
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        at(NOW.plusSeconds(120)).register(wrong);
        verify(users).delete(user); // different details replace the unfinished sign-up instead of being blocked by it
        verify(users, times(1)).saveAndFlush(any());
    }
    @Test void invalidAndDuplicateRegistrationNeverSendsMail() {
        var invalid = form(); invalid.setPassword("é".repeat(37));
        assertThatThrownBy(() -> service.register(invalid)).isInstanceOf(RegistrationException.class);
        when(users.existsByPhone("+84901234567")).thenReturn(true);
        assertThatThrownBy(() -> service.register(form())).isInstanceOf(RegistrationException.class);
        verifyNoInteractions(mail); verify(users, never()).saveAndFlush(any());
    }
}
