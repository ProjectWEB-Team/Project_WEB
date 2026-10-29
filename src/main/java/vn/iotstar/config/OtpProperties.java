package vn.iotstar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "uteexpress.otp")
public class OtpProperties {
    private Duration lifetime = Duration.ofMinutes(5);
    private Duration resendCooldown = Duration.ofSeconds(60);
    private Duration sendWindow = Duration.ofHours(1);
    private int maxAttempts = 5;
    private int maxSends = 5;
    private String deliveryMode = "disabled";
    private String inboxDir = ".local/otp-sms";
    public Duration getLifetime() { return lifetime; }
    public void setLifetime(Duration value) { lifetime = value; }
    public Duration getResendCooldown() { return resendCooldown; }
    public void setResendCooldown(Duration value) { resendCooldown = value; }
    public Duration getSendWindow() { return sendWindow; }
    public void setSendWindow(Duration value) { sendWindow = value; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int value) { maxAttempts = value; }
    public int getMaxSends() { return maxSends; }
    public void setMaxSends(int value) { maxSends = value; }
    public String getDeliveryMode() { return deliveryMode; }
    public void setDeliveryMode(String value) { deliveryMode = value; }
    public String getInboxDir() { return inboxDir; }
    public void setInboxDir(String value) { inboxDir = value; }
    public void validate() {
        if (lifetime == null || lifetime.isZero() || lifetime.isNegative() || lifetime.compareTo(Duration.ofMinutes(10)) > 0
                || resendCooldown == null || resendCooldown.compareTo(Duration.ofSeconds(30)) < 0
                || sendWindow == null || sendWindow.compareTo(Duration.ofMinutes(10)) < 0
                || maxAttempts < 1 || maxAttempts > 10 || maxSends < 1 || maxSends > 10) {
            throw new IllegalArgumentException("Invalid OTP lifetime/rate-limit configuration");
        }
    }
}
