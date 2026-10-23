package vn.iotstar.security;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

/** Constructed only from a validated Google OIDC ID token, never from a form. */
public record VerifiedGoogleIdentity(String subject, String email, String fullName, Instant verifiedAt)
        implements Serializable {
    public static final String SESSION_ATTRIBUTE = VerifiedGoogleIdentity.class.getName();
    public static final Duration VALIDITY = Duration.ofMinutes(10);

    public boolean isCurrent(Instant now) {
        return verifiedAt != null && !verifiedAt.isAfter(now)
                && now.isBefore(verifiedAt.plus(VALIDITY));
    }
}
