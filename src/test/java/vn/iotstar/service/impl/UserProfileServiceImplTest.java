package vn.iotstar.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.config.OtpProperties;
import vn.iotstar.entity.EmailOtpChallenge;
import vn.iotstar.entity.PhoneOtpChallenge;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.repository.EmailOtpChallengeRepository;
import vn.iotstar.repository.PhoneOtpChallengeRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.ReceiverLinkService;
import vn.iotstar.service.auth.AccountEmailSender;
import vn.iotstar.service.auth.OtpDelivery;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {
    private static final Instant NOW = Instant.parse("2026-10-29T10:00:00Z");

    @Mock UserRepository users;
    @Mock PhoneOtpChallengeRepository phoneChallenges;
    @Mock EmailOtpChallengeRepository emailChallenges;
    @Mock OtpDelivery delivery;
    @Mock ObjectProvider<AccountEmailSender> mailSenderProvider;
    @Mock AccountEmailSender mailSender;
    @Mock ReceiverLinkService receiverLinks;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final OtpProperties policy = new OtpProperties();
    private UserProfileServiceImpl service;

    @BeforeEach
    void setup() {
        lenient().when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        service = new UserProfileServiceImpl(
                users, phoneChallenges, emailChallenges, encoder, delivery,
                mailSenderProvider, Clock.fixed(NOW, ZoneOffset.UTC), policy, receiverLinks
        );
    }

    private User sampleUser(Long id) {
        User user = User.staff("cust1", encoder.encode("OldPassword123!"), "Nguyễn Văn A", "+84901234567", "cust1@example.com", UserRole.ROLE_USER, NOW);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void updateFullNameSucceedsWithValidName() {
        User user = sampleUser(1L);
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));

        service.updateFullName(1L, "Trần Thị B");

        assertThat(user.getFullName()).isEqualTo("Trần Thị B");
        verify(users).save(user);
    }

    @Test
    void updateFullNameFailsWithShortName() {
        assertThatThrownBy(() -> service.updateFullName(1L, "A"))
                .isInstanceOf(RegistrationException.class)
                .hasMessageContaining("Họ và tên phải từ 2 đến 100 ký tự");
    }

    @Test
    void changePasswordSucceedsWithCorrectCurrentPassword() {
        User user = sampleUser(1L);
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));

        service.changePassword(1L, "OldPassword123!", "NewSecret123!");

        assertThat(encoder.matches("NewSecret123!", user.getPassword())).isTrue();
        verify(users).save(user);
    }

    @Test
    void changePasswordFailsWithWrongCurrentPassword() {
        User user = sampleUser(1L);
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.changePassword(1L, "WrongPassword!", "NewSecret123!"))
                .isInstanceOf(RegistrationException.class)
                .hasMessageContaining("Mật khẩu hiện tại không chính xác");
    }

    @Test
    void requestPhoneVerificationSendsOtp() {
        User user = sampleUser(1L);
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(phoneChallenges.findByUserIdForUpdate(1L)).thenReturn(Optional.empty());

        UUID challengeId = service.requestPhoneVerification(1L, "0912345678");

        assertThat(challengeId).isNotNull();
        verify(phoneChallenges).saveAndFlush(any());
        verify(delivery).send(eq(challengeId), eq("+84912345678"), anyString(), any());
    }

    @Test
    void requestPhoneVerificationRejectsDuplicatePhone() {
        User user = sampleUser(1L);
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        User owner = sampleUser(2L); owner.verifyPhone(NOW);
        when(users.findByPhone("+84912345678")).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> service.requestPhoneVerification(1L, "0912345678"))
                .isInstanceOf(RegistrationException.class)
                .hasMessageContaining("tài khoản khác");
    }

    @Test
    void unverifiedClaimOnTheNumberIsReleasedWhenTheRealOwnerVerifiesIt() {
        User user = sampleUser(1L);
        User squatter = User.staff("squat", "h", "S", "+84912345678", "s@example.com", vn.iotstar.entity.UserRole.ROLE_USER, NOW);
        org.springframework.test.util.ReflectionTestUtils.setField(squatter, "id", 2L);
        String code = "123456";
        PhoneOtpChallenge challenge = PhoneOtpChallenge.create(user, "VERIFY_PHONE", "+84912345678", encoder.encode(code), NOW, policy);
        when(users.findByPhone("+84912345678")).thenReturn(Optional.of(squatter));
        when(phoneChallenges.findByIdForUpdate(challenge.getId())).thenReturn(Optional.of(challenge));

        assertThat(service.verifyPhoneOtp(1L, challenge.getId(), code)).isTrue();

        assertThat(squatter.getPhone()).isNull();
        assertThat(user.getPhone()).isEqualTo("+84912345678");
    }

    @Test
    void verifyPhoneOtpUpdatesPhoneAndMarksVerified() {
        User user = sampleUser(1L);
        String code = "123456";
        PhoneOtpChallenge challenge = PhoneOtpChallenge.create(user, "VERIFY_PHONE", "+84912345678", encoder.encode(code), NOW, policy);
        when(phoneChallenges.findByIdForUpdate(challenge.getId())).thenReturn(Optional.of(challenge));

        boolean ok = service.verifyPhoneOtp(1L, challenge.getId(), code);

        assertThat(ok).isTrue();
        assertThat(user.getPhone()).isEqualTo("+84912345678");
        assertThat(user.isPhoneVerified()).isTrue();
        verify(receiverLinks).linkVerifiedReceiver(user);
        verify(phoneChallenges).delete(challenge);
        verify(users).save(user);
    }

    @Test
    void requestEmailVerificationSendsOtp() {
        User user = sampleUser(1L);
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(users.existsByEmail(anyString())).thenReturn(false);
        when(emailChallenges.findByUserIdForUpdate(1L)).thenReturn(Optional.empty());

        UUID challengeId = service.requestEmailVerification(1L, "newemail@example.com");

        assertThat(challengeId).isNotNull();
        verify(emailChallenges).saveAndFlush(any());
        verify(mailSender).sendOtp(eq("newemail@example.com"), anyString(), any());
    }

    @Test
    void verifyEmailOtpUpdatesEmailAndMarksVerified() {
        User user = sampleUser(1L);
        String code = "654321";
        EmailOtpChallenge challenge = EmailOtpChallenge.create(user, "VERIFY_EMAIL", "newemail@example.com", encoder.encode(code), NOW, policy);
        when(emailChallenges.findByIdForUpdate(challenge.getId())).thenReturn(Optional.of(challenge));

        boolean ok = service.verifyEmailOtp(1L, challenge.getId(), code);

        assertThat(ok).isTrue();
        assertThat(user.getEmail()).isEqualTo("newemail@example.com");
        assertThat(user.isEmailVerified()).isTrue();
        verify(emailChallenges).delete(challenge);
        verify(users).save(user);
    }
}

