package vn.iotstar.entity;

import jakarta.persistence.*;
import vn.iotstar.config.OtpProperties;
import vn.iotstar.exception.RegistrationException;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_otp_challenges")
public class EmailOtpChallenge {
    @Id @Column(columnDefinition = "uniqueidentifier") private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true) private User user;
    @Column(nullable = false, length = 100) private String email;
    @Column(nullable = false, length = 20) private String purpose;
    @Column(name = "code_hash", nullable = false, length = 255) private String codeHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "consumed_at") private Instant consumedAt;
    @Column(name = "failed_attempts", nullable = false) private int failedAttempts;
    @Column(name = "last_sent_at", nullable = false) private Instant lastSentAt;
    @Column(name = "send_window_started_at", nullable = false) private Instant sendWindowStartedAt;
    @Column(name = "send_count", nullable = false) private int sendCount;
    @Version private long version;

    protected EmailOtpChallenge() { }
    public static EmailOtpChallenge create(User user, String purpose, String hash, Instant now, OtpProperties policy) {
        EmailOtpChallenge challenge = new EmailOtpChallenge();
        challenge.id = UUID.randomUUID(); challenge.user = user; challenge.sendWindowStartedAt = now;
        challenge.issue(purpose, hash, now, policy);
        return challenge;
    }
    public void issue(String purpose, String hash, Instant now, OtpProperties policy) {
        if (lastSentAt != null && now.isBefore(lastSentAt.plus(policy.getResendCooldown()))) throw limited();
        if (!now.isBefore(sendWindowStartedAt.plus(policy.getSendWindow()))) {
            sendWindowStartedAt = now; sendCount = 0;
        }
        if (sendCount >= policy.getMaxSends()) throw limited();
        this.purpose = purpose; email = user.getEmail(); codeHash = hash;
        expiresAt = now.plus(policy.getLifetime()); consumedAt = null; failedAttempts = 0;
        lastSentAt = now; sendCount++;
    }
    public boolean usable(String requiredPurpose, Instant now, int maxAttempts) {
        return purpose.equals(requiredPurpose) && email.equals(user.getEmail()) && consumedAt == null
                && now.isBefore(expiresAt) && failedAttempts < maxAttempts && user.isAccountNonLocked();
    }
    public void reject() { failedAttempts++; }
    public void consume(Instant now) { consumedAt = now; }
    private RegistrationException limited() { return new RegistrationException("Chưa thể gửi lại mã. Vui lòng thử lại sau."); }
    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getEmail() { return email; }
    public String getPurpose() { return purpose; }
    public String getCodeHash() { return codeHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public int getFailedAttempts() { return failedAttempts; }
    public int getSendCount() { return sendCount; }
}
