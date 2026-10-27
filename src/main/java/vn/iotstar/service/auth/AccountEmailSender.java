package vn.iotstar.service.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import vn.iotstar.exception.RegistrationException;
import java.time.Instant;

@Service
@ConditionalOnProperty(name = "uteexpress.mail.enabled", havingValue = "true")
public class AccountEmailSender {
    private static final Logger log = LoggerFactory.getLogger(AccountEmailSender.class);
    private final JavaMailSender sender;
    private final String from;
    public AccountEmailSender(JavaMailSender sender, @Value("${uteexpress.mail.from:${spring.mail.username}}") String from) {
        this.sender = sender; this.from = from;
        if (from == null || from.isBlank()) throw new IllegalArgumentException("SMTP sender is required");
    }
    public void sendOtp(String email, String code, Instant expiresAt) {
        try {
            send(email, "UTEExpress - Mã xác thực", "Mã xác thực của bạn: " + code
                    + "\nMã có hiệu lực đến " + expiresAt + " (UTC), chỉ dùng một lần."
                    + "\nKhông chia sẻ mã với người khác. Nếu bạn không yêu cầu, hãy bỏ qua email này.");
        } catch (MailException exception) {
            log.warn("OTP email delivery failed ({})", exception.getClass().getSimpleName());
            throw new RegistrationException("Chưa gửi được email. Vui lòng thử lại sau.");
        }
    }
    public void send(String email, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(email); message.setSubject(subject); message.setText(body);
        sender.send(message);
    }
}
