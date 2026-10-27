package vn.iotstar.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import vn.iotstar.config.OtpProperties;
import vn.iotstar.dto.auth.*;
import vn.iotstar.entity.*;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.repository.*;
import vn.iotstar.service.ReceiverLinkService;
import vn.iotstar.service.auth.OtpDelivery;

import java.time.*;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {
    @Mock UserRepository users;
    @Mock PhoneOtpChallengeRepository challenges;
    @Mock OtpDelivery delivery;
    @Mock ReceiverLinkService links;
    private final Instant now = Instant.parse("2026-10-22T06:00:00Z");
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private RegistrationServiceImpl service;

    @BeforeEach void setup() {
        service = serviceAt(now);
    }
    private RegistrationServiceImpl serviceAt(Instant time) {
        return new RegistrationServiceImpl(users, challenges, encoder, delivery,
                Clock.fixed(time, ZoneOffset.UTC), new OtpProperties(), links);
    }
    private RegisterUserRequest request() {
        RegisterUserRequest r = new RegisterUserRequest();
        r.setUsername("sender01"); r.setPassword("Password123!"); r.setFullName("Nguyen Van A");
        r.setPhone("0901234567"); r.setEmail("Sender@Example.com"); return r;
    }
    private PhoneOtpChallenge registrationChallenge() {
        User user = User.registerPending("sender01", encoder.encode("Password123!"), "Sender", "+84901234567", "a@example.com", now);
        return PhoneOtpChallenge.create(user, "REGISTRATION", encoder.encode("123456"), now, new OtpProperties());
    }
    private VerifyPhoneOtpRequest proof(PhoneOtpChallenge challenge, String code) {
        VerifyPhoneOtpRequest p = new VerifyPhoneOtpRequest(); p.setChallengeId(challenge.getId()); p.setCode(code); return p;
    }

    @Test void registrationPersistsOnlyInactiveUserWithNormalizedPhoneAndHashedSecrets() {
        when(users.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        RegistrationChallengeResponse response = service.register(request());
        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(user.capture());
        assertThat(user.getValue().getRole()).isEqualTo(UserRole.ROLE_USER);
        assertThat(user.getValue().isEnabled()).isFalse();
        assertThat(user.getValue().getPhone()).isEqualTo("+84901234567");
        assertThat(encoder.matches("Password123!", user.getValue().getPassword())).isTrue();
        ArgumentCaptor<PhoneOtpChallenge> challenge = ArgumentCaptor.forClass(PhoneOtpChallenge.class);
        verify(challenges).saveAndFlush(challenge.capture());
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(delivery).send(eq(response.challengeId()), eq("+84901234567"), code.capture(), eq(response.expiresAt()));
        assertThat(code.getValue()).matches("[0-9]{6}");
        assertThat(encoder.matches(code.getValue(), challenge.getValue().getCodeHash())).isTrue();
        assertThat(response.toString()).doesNotContain(code.getValue(), "+84901234567");
    }
    @Test void duplicateIdentityHasGenericFailureAndDoesNotSend() {
        when(users.existsByPhone("+84901234567")).thenReturn(true);
        assertThatThrownBy(() -> service.register(request())).isInstanceOf(RegistrationException.class);
        verifyNoInteractions(delivery); verify(users, never()).saveAndFlush(any());
    }
    @Test void lostSessionCanResumeWithMatchingCredentialsWithoutOverwritingAccount() {
        PhoneOtpChallenge c = registrationChallenge();
        when(users.findByPhoneForUpdate("+84901234567")).thenReturn(Optional.of(c.getUser()));
        when(challenges.findByUserIdForUpdate(c.getUser().getId())).thenReturn(Optional.of(c));
        RegisterUserRequest r = request(); r.setEmail("A@Example.com"); r.setFullName("Changed name");
        RegistrationChallengeResponse response = serviceAt(now.plusSeconds(60)).register(r);
        assertThat(response.challengeId()).isEqualTo(c.getId());
        assertThat(c.getSendCount()).isEqualTo(2);
        assertThat(c.getUser().getFullName()).isEqualTo("Sender");
        assertThat(c.getUser().isEnabled()).isFalse();
        verify(users, never()).saveAndFlush(any());
        verify(delivery).send(eq(c.getId()), eq("+84901234567"), anyString(), eq(c.getExpiresAt()));
    }
    @Test void resumingPendingRegistrationStillEnforcesCooldown() {
        PhoneOtpChallenge c = registrationChallenge();
        when(users.findByPhoneForUpdate("+84901234567")).thenReturn(Optional.of(c.getUser()));
        when(challenges.findByUserIdForUpdate(c.getUser().getId())).thenReturn(Optional.of(c));
        RegisterUserRequest r = request(); r.setEmail("a@example.com");
        assertThatThrownBy(() -> service.register(r)).isInstanceOf(RegistrationException.class);
        assertThat(c.getSendCount()).isEqualTo(1);
        verifyNoInteractions(delivery);
    }
    @Test void differentPasswordCannotResumeOrChangePendingAccount() {
        PhoneOtpChallenge c = registrationChallenge();
        when(users.findByPhoneForUpdate("+84901234567")).thenReturn(Optional.of(c.getUser()));
        RegisterUserRequest r = request(); r.setEmail("a@example.com"); r.setPassword("DifferentPassword!");
        assertThatThrownBy(() -> serviceAt(now.plusSeconds(60)).register(r)).isInstanceOf(RegistrationException.class);
        verifyNoInteractions(challenges, delivery);
    }
    @Test void differentContactCannotResumePendingAccount() {
        when(users.findByPhoneForUpdate("+84901234567")).thenReturn(Optional.of(registrationChallenge().getUser()));
        assertThatThrownBy(() -> serviceAt(now.plusSeconds(60)).register(request())).isInstanceOf(RegistrationException.class);
        verifyNoInteractions(challenges, delivery);
    }
    @Test void correctOtpActivatesAndVerifiesOnlyPhone() {
        PhoneOtpChallenge c = registrationChallenge();
        when(challenges.findByIdForUpdate(c.getId())).thenReturn(Optional.of(c));
        assertThat(service.verify(proof(c,"123456"))).isTrue();
        assertThat(c.getUser().isEnabled()).isTrue(); assertThat(c.getUser().isPhoneVerified()).isTrue();
        assertThat(c.getUser().isEmailVerified()).isFalse(); assertThat(c.getConsumedAt()).isEqualTo(now);
        assertThat(service.verify(proof(c,"123456"))).isFalse();
        verify(links, times(1)).linkVerifiedReceiver(c.getUser());
    }
    @Test void wrongCodePersistsAttemptAndLockoutRejectsEvenCorrectCode() {
        PhoneOtpChallenge c = registrationChallenge();
        when(challenges.findByIdForUpdate(c.getId())).thenReturn(Optional.of(c));
        for (int i=0;i<5;i++) assertThat(service.verify(proof(c,"000000"))).isFalse();
        assertThat(c.getFailedAttempts()).isEqualTo(5);
        assertThat(service.verify(proof(c,"123456"))).isFalse();
        assertThat(c.getUser().isEnabled()).isFalse();
        verifyNoInteractions(links);
    }
    @Test void exactExpiryCannotActivate() {
        PhoneOtpChallenge c = registrationChallenge();
        when(challenges.findByIdForUpdate(c.getId())).thenReturn(Optional.of(c));
        assertThat(serviceAt(c.getExpiresAt()).verify(proof(c,"123456"))).isFalse();
        assertThat(c.getUser().isEnabled()).isFalse();
    }
    @Test void unknownOpaqueChallengeCannotActivate() {
        VerifyPhoneOtpRequest p = new VerifyPhoneOtpRequest(); p.setChallengeId(UUID.randomUUID()); p.setCode("123456");
        assertThat(service.verify(p)).isFalse(); verifyNoInteractions(users, delivery);
    }
    @Test void codeFromAnotherPhoneCannotActivateThisAccount() {
        PhoneOtpChallenge c = registrationChallenge();
        User other = User.registerPending("sender02", "hash", "Other", "+84907654321", "other@example.com", now);
        PhoneOtpChallenge otherChallenge = PhoneOtpChallenge.create(other, "REGISTRATION", encoder.encode("654321"), now, new OtpProperties());
        when(challenges.findByIdForUpdate(otherChallenge.getId())).thenReturn(Optional.of(otherChallenge));
        assertThat(service.verify(proof(otherChallenge, "123456"))).isFalse();
        assertThat(other.isEnabled()).isFalse();
        assertThat(c.getUser().isEnabled()).isFalse();
        assertThat(otherChallenge.getFailedAttempts()).isEqualTo(1);
    }
    @Test void resendCooldownDoesNotIssueOrDeliverAnotherCode() {
        PhoneOtpChallenge c = registrationChallenge();
        when(challenges.findByIdForUpdate(c.getId())).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.resend(c.getId())).isInstanceOf(RegistrationException.class);
        verifyNoInteractions(delivery);
    }
    @Test void resendInvalidatesOldCodeAndPreservesHourlyLimit() {
        PhoneOtpChallenge c = registrationChallenge();
        when(challenges.findByIdForUpdate(c.getId())).thenReturn(Optional.of(c));
        serviceAt(now.plusSeconds(60)).resend(c.getId());
        assertThat(c.getSendCount()).isEqualTo(2);
        assertThat(serviceAt(now.plusSeconds(61)).verify(proof(c,"123456"))).isFalse();
        for (int i=2;i<=4;i++) serviceAt(now.plusSeconds(i*60L)).resend(c.getId());
        assertThatThrownBy(() -> serviceAt(now.plusSeconds(300)).resend(c.getId())).isInstanceOf(RegistrationException.class);
        assertThat(c.getSendCount()).isEqualTo(5);
    }
    @Test void registrationChallengeCannotBeReusedAsLoginProof() {
        PhoneOtpChallenge c = registrationChallenge();
        when(challenges.findByIdForUpdate(c.getId())).thenReturn(Optional.of(c));
        assertThat(service.verifyLogin(proof(c,"123456"))).isEmpty();
        assertThat(c.getConsumedAt()).isNull(); assertThat(c.getUser().isEnabled()).isFalse();
    }
    @Test void inactiveAccountGetsOpaqueResponseWithoutSendingLoginCode() {
        when(users.findByPhoneForUpdate("+84901234567")).thenReturn(Optional.of(registrationChallenge().getUser()));
        PhoneLoginRequest r = new PhoneLoginRequest(); r.setPhone("0901234567");
        assertThat(service.requestLogin(r).maskedPhone()).isEqualTo("*******567");
        verifyNoInteractions(delivery);
    }
    @Test void googleUnverifiedContactPhoneCannotBeUsedForLogin() {
        User google = User.googleAccount("g_user", "unknown", "Google User", "+84901234567", "google@example.com", "subject", now);
        when(users.findByPhoneForUpdate("+84901234567")).thenReturn(Optional.of(google));
        PhoneLoginRequest r = new PhoneLoginRequest(); r.setPhone("0901234567");
        assertThat(service.requestLogin(r).challengeId()).isNotNull();
        verifyNoInteractions(challenges, delivery);
    }
    @Test void verifiedPhoneCanLoginOnceWithBoundOtp() {
        User user = registrationChallenge().getUser(); user.verifyPhone(now);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 42L);
        PhoneOtpChallenge c = PhoneOtpChallenge.create(user, "LOGIN", encoder.encode("123456"), now, new OtpProperties());
        when(challenges.findByIdForUpdate(c.getId())).thenReturn(Optional.of(c));
        assertThat(service.verifyLogin(proof(c,"123456"))).contains(42L);
        assertThat(service.verifyLogin(proof(c,"123456"))).isEmpty();
        verifyNoInteractions(links);
    }
    @Test void passwordResetChangesHashOnceAndRejectsShortOrLoginProofs() {
        User user = registrationChallenge().getUser(); user.verifyPhone(now);
        String before = user.getPassword();
        PhoneOtpChallenge login = PhoneOtpChallenge.create(user, "LOGIN", encoder.encode("123456"), now, new OtpProperties());
        when(challenges.findByIdForUpdate(login.getId())).thenReturn(Optional.of(login));
        assertThat(service.resetPassword(proof(login,"123456"), "NewPassword123")).isFalse();
        PhoneOtpChallenge reset = PhoneOtpChallenge.create(user, "PASSWORD_RESET", encoder.encode("654321"), now, new OtpProperties());
        when(challenges.findByIdForUpdate(reset.getId())).thenReturn(Optional.of(reset));
        assertThat(service.resetPassword(proof(reset,"654321"), "short")).isFalse();
        assertThat(service.resetPassword(proof(reset,"654321"), "NewPassword123")).isTrue();
        assertThat(user.getPassword()).isNotEqualTo(before);
        assertThat(encoder.matches("NewPassword123", user.getPassword())).isTrue();
        assertThat(service.resetPassword(proof(reset,"654321"), "OtherPassword123")).isFalse();
    }
    @Test void unknownPhoneGetsDecoyResetChallengeWithoutDelivery() {
        PhoneLoginRequest r = new PhoneLoginRequest(); r.setPhone("0901234567");
        assertThat(service.requestPasswordReset(r).maskedPhone()).isEqualTo("*******567");
        verifyNoInteractions(delivery);
    }
    @Test void deliveryFailureDoesNotReturnSuccessfulChallenge() {
        when(users.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        doThrow(new RegistrationException("Không thể gửi mã lúc này.")).when(delivery).send(any(),any(),any(),any());
        assertThatThrownBy(() -> service.register(request())).isInstanceOf(RegistrationException.class);
    }
}
