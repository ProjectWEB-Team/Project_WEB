package vn.iotstar.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.service.auth.OtpDelivery;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.time.Clock;

@Configuration
@EnableConfigurationProperties(OtpProperties.class)
public class OtpConfiguration {
    @Bean @ConditionalOnMissingBean
    public Clock applicationClock() { return Clock.systemUTC(); }

    @Bean @ConditionalOnMissingBean(OtpDelivery.class)
    public OtpDelivery otpDelivery(OtpProperties properties, Environment environment) {
        properties.validate();
        if ("local-inbox".equals(properties.getDeliveryMode())) {
            if (!environment.acceptsProfiles(Profiles.of("otp-local")))
                throw new IllegalStateException("Local OTP delivery requires the otp-local profile");
            Path inbox = Path.of(".codex", "otp-inbox").toAbsolutePath().normalize();
            return (id, phone, code, expiry) -> {
                try {
                    Files.createDirectories(inbox);
                    Path temporary = Files.createTempFile(inbox, "otp-", ".tmp");
                    try {
                        Files.writeString(temporary, "Development simulation only\nCode: " + code
                                + "\nExpires (UTC): " + expiry + "\nPhone suffix: " + phone.substring(phone.length() - 3) + "\n", StandardCharsets.UTF_8);
                        Files.move(temporary, inbox.resolve(id + ".txt"), StandardCopyOption.REPLACE_EXISTING);
                    } finally { Files.deleteIfExists(temporary); }
                } catch (IOException exception) {
                    throw new RegistrationException("Không thể gửi mã lúc này. Vui lòng thử lại sau.");
                }
            };
        }
        if (!"disabled".equals(properties.getDeliveryMode())) throw new IllegalArgumentException("Unsupported OTP delivery mode");
        return (id, phone, code, expiry) -> {
            throw new RegistrationException("Gửi mã điện thoại chưa khả dụng. Vui lòng chọn cách đăng nhập khác hoặc thử lại sau.");
        };
    }
}
