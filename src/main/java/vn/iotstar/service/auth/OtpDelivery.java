package vn.iotstar.service.auth;

import java.time.Instant;
import java.util.UUID;

/** Provider boundary. Never log the code or place it in a normal HTTP response. */
public interface OtpDelivery {
    void send(UUID challengeId, String normalizedPhone, String code, Instant expiresAt);
}
