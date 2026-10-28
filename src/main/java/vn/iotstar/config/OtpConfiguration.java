package vn.iotstar.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.service.auth.OtpDelivery;
import vn.iotstar.service.auth.SpeedSmsOtpDelivery;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(OtpProperties.class)
public class OtpConfiguration {
    @Bean @ConditionalOnMissingBean
    public Clock applicationClock() { return Clock.systemUTC(); }

    @Bean @ConditionalOnMissingBean(OtpDelivery.class)
    public OtpDelivery otpDelivery(OtpProperties properties, Environment environment, Clock clock) {
        properties.validate();
        if (environment.acceptsProfiles(Profiles.of("otp-local"))
                && (environment.acceptsProfiles(Profiles.of("sms")) || "speedsms".equals(properties.getDeliveryMode()))) {
            throw new IllegalStateException("Do not combine real SMS delivery with the otp-local profile");
        }
        if ("speedsms".equals(properties.getDeliveryMode())) {
            var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NEVER).build();
            var requests = new JdkClientHttpRequestFactory(http);
            requests.setReadTimeout(Duration.ofSeconds(10));
            return new SpeedSmsOtpDelivery(RestClient.builder().requestFactory(requests).build(),
                    environment.getProperty("uteexpress.otp.speedsms.access-token", ""),
                    environment.getProperty("uteexpress.otp.speedsms.sender", "Verify"),
                    environment.getProperty("uteexpress.otp.speedsms.message-template", ""), clock);
        }
        if ("local-inbox".equals(properties.getDeliveryMode())) {
            if (!environment.acceptsProfiles(Profiles.of("otp-local")))
                throw new IllegalStateException("Local OTP delivery requires the otp-local profile");
            Path inbox = Path.of(".local", "otp-inbox").toAbsolutePath().normalize();
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
            throw new RegistrationException("Đăng ký và đăng nhập bằng mã điện thoại đang tạm đóng. Vui lòng dùng tài khoản đã có.");
        };
    }
}
