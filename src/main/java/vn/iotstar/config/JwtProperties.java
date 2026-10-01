package vn.iotstar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "uteexpress.jwt")
public class JwtProperties {
    private String secret = "development-only-uteexpress-jwt-secret-change-before-production-2026";
    private long expirationSeconds = 1800;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getExpirationSeconds() { return expirationSeconds; }
    public void setExpirationSeconds(long expirationSeconds) { this.expirationSeconds = expirationSeconds; }
}
