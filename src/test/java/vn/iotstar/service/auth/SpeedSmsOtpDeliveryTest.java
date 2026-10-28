package vn.iotstar.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import vn.iotstar.exception.RegistrationException;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SpeedSmsOtpDeliveryTest {
    private final Instant now = Instant.parse("2026-10-28T00:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private MockRestServiceServer server;
    private RestClient client;
    private SpeedSmsOtpDelivery delivery;

    @BeforeEach void setup() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = builder.build();
        delivery = new SpeedSmsOtpDelivery(client, "test-token", "Verify", "", clock);
    }

    @Test void sendsOneOtpAsAuthenticatedJsonAndKeepsSecretsOutOfTheUrl() {
        server.expect(requestTo("https://api.speedsms.vn/index.php/sms/send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic dGVzdC10b2tlbjp4"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"to":["84901234567"],"sms_type":4,"sender":"Verify",
                         "content":"UTEExpress: Ma xac thuc cua ban la 123456. Hieu luc 5 phut. Khong chia se ma nay."}
                        """, true))
                .andRespond(withSuccess("""
                        {"status":"success","code":"00","data":{"tranId":42,"totalSMS":1,"invalidPhone":[]}}
                        """, MediaType.APPLICATION_JSON));
        send();
        server.verify();
    }

    @Test void usesTheApprovedTemplateAndNormalizesADomesticNumber() {
        var custom = new SpeedSmsOtpDelivery(client, "test-token", "Notify", "Ma UTEExpress: {code} ({minutes} phut).", clock);
        server.expect(requestTo("https://api.speedsms.vn/index.php/sms/send"))
                .andExpect(jsonPath("$.content").value("Ma UTEExpress: 123456 (2 phut)."))
                .andExpect(jsonPath("$.to[0]").value("84901234567"))
                .andExpect(jsonPath("$.sender").value("Notify"))
                .andRespond(withSuccess("{\"status\":\"success\",\"code\":\"00\",\"data\":{\"totalSMS\":1,\"invalidPhone\":[]}}", MediaType.APPLICATION_JSON));
        custom.send(UUID.randomUUID(), "0901234567", "123456", now.plusSeconds(61));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"status\":\"error\",\"code\":\"300\",\"message\":\"test-token 123456 +84901234567\"}",
            "{\"status\":\"success\",\"code\":\"00\",\"data\":{\"totalSMS\":1,\"invalidPhone\":[\"84901234567\"]}}",
            "{\"status\":\"success\",\"code\":\"00\",\"data\":{\"totalSMS\":0,\"invalidPhone\":[]}}",
            "{\"status\":\"success\",\"code\":\"00\",\"data\":{\"totalSMS\":2,\"invalidPhone\":[]}}",
            "{\"status\":\"success\",\"code\":\"00\"}", "{}", "null", "not-json", ""
    })
    void rejectsProviderErrorsOrIncompleteAcceptanceWithoutReturningPrivateDetails(String response) {
        server.expect(requestTo("https://api.speedsms.vn/index.php/sms/send"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        assertUnavailable();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {302, 401, 429, 500})
    void refusesRedirectsAndHttpFailuresWithoutRetrying(int status) {
        server.expect(requestTo("https://api.speedsms.vn/index.php/sms/send"))
                .andRespond(withStatus(HttpStatusCode.valueOf(status)).body("test-token 123456 +84901234567"));
        assertUnavailable();
        server.verify();
    }

    @Test void transportFailureDoesNotRetryOrExposeItsCause() {
        server.expect(requestTo("https://api.speedsms.vn/index.php/sms/send"))
                .andRespond(withException(new IOException("test-token 123456 +84901234567")));
        assertUnavailable();
        server.verify();
    }

    @Test void expiredOrInvalidChallengesNeverReachTheProvider() {
        assertThatThrownBy(() -> delivery.send(UUID.randomUUID(), "+84901234567", "123456", now))
                .isInstanceOf(RegistrationException.class);
        assertThatThrownBy(() -> delivery.send(UUID.randomUUID(), "+84901234567", "123456", now.plusSeconds(601)))
                .isInstanceOf(RegistrationException.class);
        assertThatThrownBy(() -> delivery.send(UUID.randomUUID(), "+84901234567", "bad", now.plusSeconds(60)))
                .isInstanceOf(RegistrationException.class);
        assertThatThrownBy(() -> delivery.send(UUID.randomUUID(), "+12025550123", "123456", now.plusSeconds(60)))
                .isInstanceOf(RegistrationException.class);
        server.verify();
    }

    @Test void rejectsMissingCredentialsUnsupportedSendersAndMultipartTemplatesAtStartup() {
        assertThatThrownBy(() -> new SpeedSmsOtpDelivery(client, "", "Verify", "", clock)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SpeedSmsOtpDelivery(client, "test-token\n", "Verify", "", clock)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SpeedSmsOtpDelivery(client, "test-token", "UTEExpress", "", clock)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SpeedSmsOtpDelivery(client, "test-token", "Verify", "No code", clock)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SpeedSmsOtpDelivery(client, "test-token", "Verify", "x".repeat(155) + "{code}", clock)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SpeedSmsOtpDelivery(client, "test-token", "Verify", "Mã OTP {code}", clock)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SpeedSmsOtpDelivery(client, "test-token", "Verify", "OTP {code} {unsupported}", clock)).isInstanceOf(IllegalArgumentException.class);
        server.verify();
    }

    private void send() {
        delivery.send(UUID.randomUUID(), "+84901234567", "123456", now.plusSeconds(300));
    }
    private void assertUnavailable() {
        assertThatThrownBy(this::send).isInstanceOf(RegistrationException.class)
                .hasMessage("Không thể gửi mã lúc này. Vui lòng thử lại sau.").hasNoCause();
    }
}
