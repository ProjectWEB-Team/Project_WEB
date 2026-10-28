package vn.iotstar.service.auth;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import vn.iotstar.exception.RegistrationException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Sends the application's existing OTP through SpeedSMS; verification stays in RegistrationService. */
public final class SpeedSmsOtpDelivery implements OtpDelivery {
    private static final String ENDPOINT = "https://api.speedsms.vn/index.php/sms/send";
    private static final String DEFAULT_TEMPLATE = "UTEExpress: Ma xac thuc cua ban la {code}. Hieu luc {minutes} phut. Khong chia se ma nay.";
    private static final String UNAVAILABLE = "Không thể gửi mã lúc này. Vui lòng thử lại sau.";
    private final RestClient client;
    private final String accessToken;
    private final String sender;
    private final String template;
    private final Clock clock;

    public SpeedSmsOtpDelivery(RestClient client, String accessToken, String sender, String template, Clock clock) {
        if (accessToken == null || accessToken.isBlank() || accessToken.length() > 512
                || accessToken.chars().anyMatch(c -> c <= 32 || c >= 127 || c == ':')) {
            throw new IllegalArgumentException("Set SPEEDSMS_ACCESS_TOKEN privately before enabling SMS");
        }
        if (sender == null || !Set.of("Verify", "Notify").contains(sender)) {
            throw new IllegalArgumentException("SPEEDSMS_SENDER must be Verify or Notify for this SMS integration");
        }
        this.template = template == null || template.isBlank() ? DEFAULT_TEMPLATE : template;
        String sample = this.template.replace("{code}", "123456").replace("{minutes}", "10");
        // One GSM-7 segment avoids accidental multipart charges; templates must also be approved by the provider.
        if (!this.template.contains("{code}") || sample.length() > 160 || !sample.matches("[A-Za-z0-9 .,!?;:()'/-]+")) {
            throw new IllegalArgumentException("SMS template must contain {code} and fit one plain-text SMS (160 characters)");
        }
        this.client = client;
        this.accessToken = accessToken;
        this.sender = sender;
        this.clock = clock;
    }

    @Override
    public void send(UUID challengeId, String normalizedPhone, String code, Instant expiresAt) {
        if (code == null || !code.matches("[0-9]{6}") || expiresAt == null) throw new RegistrationException(UNAVAILABLE);
        long seconds = Duration.between(clock.instant(), expiresAt).getSeconds();
        if (seconds <= 0 || seconds > 600) throw new RegistrationException(UNAVAILABLE);
        String phone = VietnamPhoneNormalizer.normalize(normalizedPhone).substring(1);
        String content = template.replace("{code}", code).replace("{minutes}", Long.toString((seconds + 59) / 60));
        try {
            // Do not retry an uncertain paid send: the provider may already have accepted it.
            JsonNode result = client.post().uri(ENDPOINT)
                    .headers(headers -> headers.setBasicAuth(accessToken, "x"))
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .body(Map.of("to", List.of(phone), "content", content, "sms_type", 4, "sender", sender))
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (request, response) -> {
                        throw new RegistrationException(UNAVAILABLE);
                    }).body(JsonNode.class);
            if (result == null || !"success".equals(result.path("status").asText())
                    || !"00".equals(result.path("code").asText())
                    || result.path("data").path("totalSMS").asInt(-1) != 1
                    || !result.path("data").path("invalidPhone").isArray()
                    || !result.path("data").path("invalidPhone").isEmpty()) {
                throw new RegistrationException(UNAVAILABLE);
            }
        } catch (RestClientException exception) {
            // Provider bodies and transport errors can contain a token, phone number or OTP.
            throw new RegistrationException(UNAVAILABLE);
        }
    }
}
