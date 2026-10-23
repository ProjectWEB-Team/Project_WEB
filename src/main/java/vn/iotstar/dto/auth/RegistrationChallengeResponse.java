package vn.iotstar.dto.auth;

import java.time.Instant;
import java.util.UUID;

public record RegistrationChallengeResponse(UUID challengeId, Instant expiresAt, String maskedPhone) {
    public UUID getChallengeId() { return challengeId; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getMaskedPhone() { return maskedPhone; }
}
