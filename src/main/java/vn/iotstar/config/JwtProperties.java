package vn.iotstar.config;

import io.jsonwebtoken.io.Decoders;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "uteexpress.jwt")
@Validated
public class JwtProperties {
    @NotBlank(message = "UTEEXPRESS_JWT_SECRET must be configured")
    private String secret;

    @Min(value = 1, message = "UTEEXPRESS_JWT_EXPIRATION_SECONDS must be positive")
    private long expirationSeconds = 1800;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getExpirationSeconds() { return expirationSeconds; }
    public void setExpirationSeconds(long expirationSeconds) { this.expirationSeconds = expirationSeconds; }

    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("UTEEXPRESS_JWT_SECRET must be configured as a Base64-encoded HS256 key");
        }
        try {
            if (Decoders.BASE64.decode(secret).length < 32) {
                throw new IllegalStateException("UTEEXPRESS_JWT_SECRET must decode to at least 32 bytes for HS256");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("UTEEXPRESS_JWT_SECRET must be valid Base64", exception);
        }
    }
}
