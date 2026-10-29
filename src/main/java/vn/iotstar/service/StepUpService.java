package vn.iotstar.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.iotstar.config.OtpProperties;
import vn.iotstar.entity.User;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.service.auth.AccountEmailSender;

import java.io.Serializable;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Re-checks identity before a phone, email or password change. Password when the account has one;
 * plus an OTP to the verified email when the account is linked to both a phone and an email (or Google).
 * State lives in the HTTP session, so it needs no table and ends with the session.
 */
@Service
public class StepUpService {
    static final String PENDING = "stepUpPending", UNLOCKED = "stepUpUntil", FAILS = "stepUpFails";
    private static final Duration UNLOCK_FOR = Duration.ofMinutes(10);
    private static final int MAX_PASSWORD_FAILS = 5;
    private final PasswordEncoder encoder;
    private final ObjectProvider<AccountEmailSender> mail;
    private final Clock clock;
    private final OtpProperties policy;
    private final SecureRandom random = new SecureRandom();

    public StepUpService(PasswordEncoder encoder, ObjectProvider<AccountEmailSender> mail, Clock clock, OtpProperties policy) {
        this.encoder = encoder; this.mail = mail; this.clock = clock; this.policy = policy;
    }

    private record Pending(String hash, Instant expires, Instant sentAt, int attempts) implements Serializable { }

    public boolean needsPassword(User user) { return user.getGoogleSubject() == null; }

    public boolean needsEmailOtp(User user) {
        return user.isEmailVerified() && (user.getGoogleSubject() != null || user.isPhoneVerified());
    }

    public boolean isUnlocked(HttpSession session) {
        return session != null && session.getAttribute(UNLOCKED) instanceof Instant until && clock.instant().isBefore(until);
    }

    /** @return true when an email code was sent and must be entered; false when the session is already unlocked. */
    public boolean start(HttpSession session, User user, String password) {
        if (needsPassword(user)) {
            int fails = session.getAttribute(FAILS) instanceof Integer n ? n : 0;
            if (fails >= MAX_PASSWORD_FAILS) throw new RegistrationException("Nhập sai quá nhiều lần. Vui lòng đăng nhập lại.");
            if (password == null || !encoder.matches(password, user.getPassword())) {
                session.setAttribute(FAILS, fails + 1);
                throw new RegistrationException("Mật khẩu không chính xác.");
            }
            session.removeAttribute(FAILS);
        }
        if (!needsEmailOtp(user)) { unlock(session); return false; }
        sendCode(session, user);
        return true;
    }

    public void resend(HttpSession session, User user) {
        if (!(session.getAttribute(PENDING) instanceof Pending)) throw new RegistrationException("Phiên xác minh đã hết hạn.");
        sendCode(session, user);
    }

    public boolean verify(HttpSession session, String code) {
        if (!(session.getAttribute(PENDING) instanceof Pending p)) return false;
        Instant now = clock.instant();
        if (code == null || !code.matches("[0-9]{6}") || !now.isBefore(p.expires()) || p.attempts() >= policy.getMaxAttempts()) return false;
        if (!encoder.matches(code, p.hash())) {
            session.setAttribute(PENDING, new Pending(p.hash(), p.expires(), p.sentAt(), p.attempts() + 1));
            return false;
        }
        unlock(session);
        return true;
    }

    private void sendCode(HttpSession session, User user) {
        AccountEmailSender sender = mail.getIfAvailable();
        if (sender == null) throw new RegistrationException("Dịch vụ gửi Email OTP chưa được kích hoạt trên hệ thống.");
        Instant now = clock.instant();
        if (session.getAttribute(PENDING) instanceof Pending p && now.isBefore(p.sentAt().plus(policy.getResendCooldown()))) {
            throw new RegistrationException("Vui lòng chờ một lúc trước khi gửi lại mã.");
        }
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        Instant expires = now.plus(policy.getLifetime());
        session.setAttribute(PENDING, new Pending(encoder.encode(code), expires, now, 0));
        sender.sendOtp(user.getEmail(), code, expires);
    }

    private void unlock(HttpSession session) {
        session.removeAttribute(PENDING);
        session.setAttribute(UNLOCKED, clock.instant().plus(UNLOCK_FOR));
    }
}
