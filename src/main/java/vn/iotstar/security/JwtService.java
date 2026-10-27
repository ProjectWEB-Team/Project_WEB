package vn.iotstar.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import vn.iotstar.config.JwtProperties;
import vn.iotstar.entity.User;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final JwtProperties properties;
    public JwtService(JwtProperties properties) { this.properties = properties; }
    public String issueToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getUsername()).claim("role", user.getRole().name())
                .claim("securityVersion", user.getSecurityVersion()).issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.getExpirationSeconds()))).signWith(signingKey()).compact();
    }
    public String extractUsername(String token) { return claims(token).getSubject(); }
    public boolean isValid(String token, User user) {
        try {
            Claims claims = claims(token);
            Long version = claims.get("securityVersion", Long.class);
            return user.isEnabled() && user.isAccountNonLocked() && user.getUsername().equals(claims.getSubject())
                    && version != null && version == user.getSecurityVersion()
                    && user.getRole().name().equals(claims.get("role", String.class)) && claims.getExpiration().after(new Date());
        }
        catch (JwtException | IllegalArgumentException exception) { return false; }
    }
    private Claims claims(String token) { return Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token).getPayload(); }
    private SecretKey signingKey() { return Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret())); }
}
