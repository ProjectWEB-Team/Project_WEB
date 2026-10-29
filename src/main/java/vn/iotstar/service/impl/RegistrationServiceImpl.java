package vn.iotstar.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.iotstar.config.OtpProperties;
import vn.iotstar.dto.auth.*;
import vn.iotstar.entity.*;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.repository.*;
import vn.iotstar.service.ReceiverLinkService;
import vn.iotstar.service.RegistrationService;
import vn.iotstar.service.auth.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class RegistrationServiceImpl implements RegistrationService {
    private final UserRepository users;
    private final PhoneOtpChallengeRepository challenges;
    private final PasswordEncoder encoder;
    private final OtpDelivery delivery;
    private final Clock clock;
    private final OtpProperties policy;
    private final ReceiverLinkService receiverLinks;
    private final SecureRandom random = new SecureRandom();
    public RegistrationServiceImpl(UserRepository users, PhoneOtpChallengeRepository challenges,
                                   PasswordEncoder encoder, OtpDelivery delivery, Clock clock, OtpProperties policy,
                                   ReceiverLinkService receiverLinks) {
        this.users = users; this.challenges = challenges; this.encoder = encoder;
        this.delivery = delivery; this.clock = clock; this.policy = policy; this.receiverLinks = receiverLinks;
        policy.validate();
    }
    @Override public RegistrationChallengeResponse register(RegisterUserRequest request) {
        String username = required(request.getUsername(), 50);
        String name = required(request.getFullName(), 100);
        String email = required(request.getEmail(), 100).toLowerCase(Locale.ROOT);
        String phone = VietnamPhoneNormalizer.normalize(request.getPhone());
        String password = request.getPassword();
        if (!username.matches("[A-Za-z0-9_.-]{3,50}") || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
                || password == null || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw invalidRegistration();
        User pending = users.findByPhoneForUpdate(phone).orElse(null);
        if (pending != null) {
            // Losing a browser session must not strand an unfinished registration.
            // Resume only the same credentials; never overwrite the stored identity.
            if (pending.getRole() != UserRole.ROLE_USER || !"INACTIVE".equals(pending.getStatus())
                    || pending.getGoogleSubject() != null || pending.isPhoneVerified()
                    || !pending.getUsername().equals(username) || !pending.getEmail().equals(email)
                    || !encoder.matches(password, pending.getPassword())) throw invalidRegistration();
            PhoneOtpChallenge existing = challenges.findByUserIdForUpdate(pending.getId()).orElseThrow(this::invalidRegistration);
            if (!"REGISTRATION".equals(existing.getPurpose()) || existing.getConsumedAt() != null) throw invalidRegistration();
            return issue(existing, "REGISTRATION");
        }
        if (users.existsByUsername(username) || users.existsByPhone(phone) || users.existsByEmail(email)) throw alreadyUsed();
        User user = users.saveAndFlush(User.registerPending(username, encoder.encode(password), name, phone, email, clock.instant()));
        String code = newCode();
        PhoneOtpChallenge challenge = PhoneOtpChallenge.create(user, "REGISTRATION", encoder.encode(code), clock.instant(), policy);
        challenges.saveAndFlush(challenge);
        delivery.send(challenge.getId(), phone, code, challenge.getExpiresAt());
        return response(challenge);
    }
    @Override public boolean verify(VerifyPhoneOtpRequest request) {
        Optional<User> activated = verifyBound(request, "REGISTRATION");
        // Same transaction as activation: late recipients get their earlier shipments atomically.
        activated.ifPresent(receiverLinks::linkVerifiedReceiver);
        return activated.isPresent();
    }
    @Override public Optional<Long> verifyLogin(VerifyPhoneOtpRequest request) {
        return verifyBound(request, "LOGIN").map(User::getId);
    }
    private Optional<User> verifyBound(VerifyPhoneOtpRequest request, String purpose) {
        if (request == null || request.getChallengeId() == null) return Optional.empty();
        PhoneOtpChallenge challenge = challenges.findByIdForUpdate(request.getChallengeId()).orElse(null);
        Instant now = clock.instant();
        if (challenge == null || !challenge.usable(purpose, now, policy.getMaxAttempts())) return Optional.empty();
        User user = challenge.getUser();
        if (user.getRole() != UserRole.ROLE_USER
                || ("REGISTRATION".equals(purpose) && !"INACTIVE".equals(user.getStatus()))
                || (!"REGISTRATION".equals(purpose) && (!user.isEnabled() || !user.isPhoneVerified()))) return Optional.empty();
        if (request.getCode() == null || !request.getCode().matches("[0-9]{6}")
                || !encoder.matches(request.getCode(), challenge.getCodeHash())) {
            challenge.reject();
            // Return normally so the failed-attempt counter commits instead of rolling back.
            return Optional.empty();
        }
        challenge.consume(now); user.verifyPhone(now);
        return Optional.of(user);
    }
    @Override public RegistrationChallengeResponse requestLogin(PhoneLoginRequest request) {
        return requestPhoneChallenge(request, "LOGIN");
    }
    @Override public RegistrationChallengeResponse requestPasswordReset(PhoneLoginRequest request) {
        return requestPhoneChallenge(request, "PASSWORD_RESET");
    }
    @Override public boolean checkResetOtp(UUID challengeId, String code) {
        if (challengeId == null || code == null || !code.matches("[0-9]{6}")) return false;
        PhoneOtpChallenge challenge = challenges.findById(challengeId).orElse(null);
        Instant now = clock.instant();
        if (challenge == null || !challenge.usable("PASSWORD_RESET", now, policy.getMaxAttempts())) return false;
        if (!encoder.matches(code, challenge.getCodeHash())) {
            challenge.reject();
            challenges.saveAndFlush(challenge);
            return false;
        }
        return true;
    }
    @Override public boolean resetPassword(VerifyPhoneOtpRequest request, String newPassword) {
        if (newPassword == null || newPassword.length() < 8 || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) return false;
        return verifyBound(request, "PASSWORD_RESET").map(user -> { user.changePassword(encoder.encode(newPassword)); return true; })
                .orElse(false);
    }
    private RegistrationChallengeResponse requestPhoneChallenge(PhoneLoginRequest request, String purpose) {
        String phone = VietnamPhoneNormalizer.normalize(request.getPhone());
        User user = users.findByPhoneForUpdate(phone).filter(User::isEnabled)
                .filter(User::isAccountNonLocked).filter(User::isPhoneVerified).orElse(null);
        // Phone OTP is a customer capability; internal roles keep their provisioned login path.
        // A Google account's unverified contact phone must never become a public login identifier.
        if (user == null || user.getRole() != UserRole.ROLE_USER) {
            return new RegistrationChallengeResponse(UUID.randomUUID(), clock.instant().plus(policy.getLifetime()),
                    "*******" + phone.substring(phone.length() - 3));
        }
        PhoneOtpChallenge challenge = challenges.findByUserIdForUpdate(user.getId()).orElse(null);
        if (challenge != null) return issue(challenge, purpose);
        String code = newCode();
        challenge = PhoneOtpChallenge.create(user, purpose, encoder.encode(code), clock.instant(), policy);
        challenges.saveAndFlush(challenge);
        delivery.send(challenge.getId(), phone, code, challenge.getExpiresAt());
        return response(challenge);
    }
    @Override public RegistrationChallengeResponse resend(UUID challengeId) {
        if (challengeId == null) throw unavailable();
        PhoneOtpChallenge challenge = challenges.findByIdForUpdate(challengeId).orElseThrow(this::unavailable);
        User user = challenge.getUser();
        if (challenge.getConsumedAt() != null || !user.isAccountNonLocked() || user.getRole() != UserRole.ROLE_USER
                || ("REGISTRATION".equals(challenge.getPurpose()) && !"INACTIVE".equals(user.getStatus()))
                || (!"REGISTRATION".equals(challenge.getPurpose()) && (!user.isEnabled() || !user.isPhoneVerified()))) throw unavailable();
        return issue(challenge, challenge.getPurpose());
    }
    private RegistrationChallengeResponse issue(PhoneOtpChallenge challenge, String purpose) {
        String code;
        // Resending must invalidate the old digits even in the rare random collision.
        do { code = newCode(); } while (encoder.matches(code, challenge.getCodeHash()));
        challenge.issue(purpose, encoder.encode(code), clock.instant(), policy);
        challenges.flush();
        delivery.send(challenge.getId(), challenge.getUser().getPhone(), code, challenge.getExpiresAt());
        return response(challenge);
    }
    private RegistrationChallengeResponse response(PhoneOtpChallenge c) {
        String phone = c.getUser().getPhone();
        return new RegistrationChallengeResponse(c.getId(), c.getExpiresAt(), "*******" + phone.substring(phone.length() - 3));
    }
    private String newCode() { return String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000)); }
    private String required(String text, int max) {
        if (text == null || text.isBlank() || text.strip().length() > max) throw invalidRegistration();
        return text.strip();
    }
    private RegistrationException alreadyUsed() {
        return new RegistrationException("Số điện thoại, email hoặc tên đăng nhập đã được sử dụng. Nếu bạn đã có tài khoản, hãy đăng nhập.");
    }
    private RegistrationException invalidRegistration() {
        return new RegistrationException("Không thể đăng ký với thông tin này. Vui lòng kiểm tra lại.");
    }
    private RegistrationException unavailable() {
        return new RegistrationException("Không thể gửi mã. Vui lòng kiểm tra thông tin hoặc thử lại sau.");
    }
}
