package vn.iotstar.service.impl;

import jakarta.validation.Validator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.config.OtpProperties;
import vn.iotstar.dto.auth.RegisterUserRequest;
import vn.iotstar.entity.*;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.repository.*;
import vn.iotstar.service.auth.AccountEmailSender;
import vn.iotstar.service.auth.VietnamPhoneNormalizer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

@Service
@Transactional
@ConditionalOnProperty(name = "uteexpress.mail.enabled", havingValue = "true")
public class EmailAccountService {
    private final UserRepository users;
    private final EmailOtpChallengeRepository challenges;
    private final PasswordEncoder encoder;
    private final AccountEmailSender mail;
    private final Clock clock;
    private final OtpProperties policy;
    private final Validator validator;
    private final SecureRandom random = new SecureRandom();

    public EmailAccountService(UserRepository users, EmailOtpChallengeRepository challenges, PasswordEncoder encoder,
                               AccountEmailSender mail, Clock clock, OtpProperties policy, Validator validator) {
        this.users = users; this.challenges = challenges; this.encoder = encoder; this.mail = mail;
        this.clock = clock; this.policy = policy; this.validator = validator;
        policy.validate();
    }
    public record Challenge(UUID id, String maskedEmail) { }
    public record EmailStatus(String maskedEmail, boolean verified) { }

    @Transactional(readOnly = true)
    public EmailStatus status(Long userId) {
        User user = users.findById(userId).orElseThrow(this::invalid);
        if (!customer(user) || !user.isEnabled()) throw invalid();
        return new EmailStatus(mask(user.getEmail()), user.isEmailVerified());
    }

    public Challenge register(RegisterUserRequest request) {
        if (request == null || !validator.validate(request).isEmpty() || !validPassword(request.getPassword())) throw invalid();
        String email = normalizeEmail(request.getEmail());
        String phone = VietnamPhoneNormalizer.normalize(request.getPhone());
        User user = users.findByEmailForUpdate(email).orElse(null);
        if (user != null) {
            if (!customer(user) || !"INACTIVE".equals(user.getStatus()) || user.getGoogleSubject() != null
                    || user.isEmailVerified() || user.isPhoneVerified() || !user.getPhone().equals(phone)
                    || !user.getUsername().equals(request.getUsername())
                    || !encoder.matches(request.getPassword(), user.getPassword())) throw invalid();
            EmailOtpChallenge pending = challenges.findByUserIdForUpdate(user.getId()).orElseThrow(this::invalid);
            if (!"REGISTRATION".equals(pending.getPurpose()) || pending.getConsumedAt() != null) throw invalid();
            return issue(user, "REGISTRATION", pending);
        }
        if (users.existsByUsername(request.getUsername()) || users.existsByPhone(phone)) throw invalid();
        user = users.saveAndFlush(User.registerPending(request.getUsername(), encoder.encode(request.getPassword()),
                request.getFullName().strip(), phone, email, clock.instant()));
        return issue(user, "REGISTRATION", null);
    }

    public Challenge requestPasswordReset(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        Challenge decoy = new Challenge(UUID.randomUUID(), mask(email));
        User user = users.findByEmailForUpdate(email).orElse(null);
        if (!customer(user) || !user.isEnabled() || !user.isEmailVerified()) return decoy;
        try {
            return issue(user, "PASSWORD_RESET", challenges.findByUserIdForUpdate(user.getId()).orElse(null));
        } catch (RegistrationException exception) {
            // Unknown, throttled and undeliverable addresses receive the same public result.
            return decoy;
        }
    }

    public Challenge requestVerification(Long userId) {
        User user = users.findByIdForUpdate(userId).orElseThrow(this::invalid);
        if (!customer(user) || !user.isEnabled() || user.isEmailVerified()) throw invalid();
        return issue(user, "VERIFY_EMAIL", challenges.findByUserIdForUpdate(userId).orElse(null));
    }

    public Challenge resend(UUID id, String purpose, Long currentUserId) {
        EmailOtpChallenge challenge = id == null ? null : challenges.findByIdForUpdate(id).orElse(null);
        if (challenge == null || !purpose.equals(challenge.getPurpose()) || challenge.getConsumedAt() != null
                || !eligible(challenge.getUser(), purpose, currentUserId)) throw invalid();
        return issue(challenge.getUser(), purpose, challenge);
    }

    /** Checks the code without consuming it, so the reset page can ask for the new password only after a correct code. */
    public boolean check(UUID id, String code, String purpose, Long currentUserId) {
        return proof(id, code, purpose, currentUserId) != null;
    }

    public boolean verify(UUID id, String code, String purpose, Long currentUserId, String newPassword) {
        if ("PASSWORD_RESET".equals(purpose) && !validPassword(newPassword)) return false;
        EmailOtpChallenge challenge = proof(id, code, purpose, currentUserId);
        if (challenge == null) return false;
        Instant now = clock.instant();
        challenge.consume(now);
        if ("PASSWORD_RESET".equals(purpose)) challenge.getUser().changePassword(encoder.encode(newPassword));
        else challenge.getUser().verifyEmail(now);
        return true;
    }

    private EmailOtpChallenge proof(UUID id, String code, String purpose, Long currentUserId) {
        if (id == null) return null;
        EmailOtpChallenge challenge = challenges.findByIdForUpdate(id).orElse(null);
        if (challenge == null || !challenge.usable(purpose, clock.instant(), policy.getMaxAttempts())
                || !eligible(challenge.getUser(), purpose, currentUserId)) return null;
        if (code == null || !code.matches("[0-9]{6}") || !encoder.matches(code, challenge.getCodeHash())) {
            challenge.reject();
            return null;
        }
        return challenge;
    }

    private boolean eligible(User user, String purpose, Long currentUserId) {
        if (!customer(user)) return false;
        return switch (purpose) {
            case "REGISTRATION" -> "INACTIVE".equals(user.getStatus()) && user.getGoogleSubject() == null;
            case "PASSWORD_RESET" -> user.isEnabled() && user.isEmailVerified();
            case "VERIFY_EMAIL" -> user.isEnabled() && !user.isEmailVerified()
                    && currentUserId != null && currentUserId.equals(user.getId());
            default -> false;
        };
    }
    private Challenge issue(User user, String purpose, EmailOtpChallenge challenge) {
        String code;
        do { code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000)); }
        while (challenge != null && encoder.matches(code, challenge.getCodeHash()));
        if (challenge == null) {
            challenge = EmailOtpChallenge.create(user, purpose, encoder.encode(code), clock.instant(), policy);
            challenges.saveAndFlush(challenge);
        } else {
            challenge.issue(purpose, encoder.encode(code), clock.instant(), policy);
            challenges.flush();
        }
        mail.sendOtp(challenge.getEmail(), code, challenge.getExpiresAt());
        return new Challenge(challenge.getId(), mask(challenge.getEmail()));
    }
    private boolean customer(User user) { return user != null && user.getRole() == UserRole.ROLE_USER && user.isAccountNonLocked(); }
    private boolean validPassword(String value) {
        return value != null && value.length() >= 8 && value.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
    private String normalizeEmail(String value) {
        if (value == null || value.strip().length() > 100 || !value.strip().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw invalid();
        return value.strip().toLowerCase(Locale.ROOT);
    }
    private String mask(String email) { return email.substring(0, 1) + "***" + email.substring(email.indexOf('@')); }
    private RegistrationException invalid() { return new RegistrationException("Không thể thực hiện với thông tin này. Vui lòng kiểm tra lại."); }
}
