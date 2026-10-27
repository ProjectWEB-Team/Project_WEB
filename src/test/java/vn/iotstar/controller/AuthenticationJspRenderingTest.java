package vn.iotstar.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.util.HtmlUtils;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.dto.auth.RegistrationChallengeResponse;
import vn.iotstar.security.AccountSessionAuthenticator;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.security.VerifiedGoogleIdentity;
import vn.iotstar.service.GoogleAccountService;
import vn.iotstar.service.PhoneSessionService;
import vn.iotstar.service.RegistrationService;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Real HTTP + Jasper rendering, isolated from SQL Server and all external providers. */
@SpringBootTest(classes = AuthenticationJspRenderingTest.RenderingApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.address=127.0.0.1",
                "server.servlet.session.cookie.name=UTEEXPRESS_SESSION",
                "spring.mvc.view.prefix=/WEB-INF/views/", "spring.mvc.view.suffix=.jsp",
                "uteexpress.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "spring.main.banner-mode=off"
        })
class AuthenticationJspRenderingTest {
    private static final Instant NOW = Instant.parse("2026-10-22T08:00:00Z");
    private static final Pattern CSRF = Pattern.compile("<input[^>]*name=\"_csrf\"[^>]*value=\"([^\"]+)\"");
    @LocalServerPort private int port;
    @Autowired private SessionTracker sessionTracker;
    @MockBean private RegistrationService registrationService;
    @MockBean private PhoneSessionService phoneSessionService;
    @MockBean private GoogleAccountService googleAccounts;
    @MockBean private AccountSessionAuthenticator accountSessions;
    @MockBean private UserDetailsService users;
    @MockBean private JwtService jwt;
    private CookieManager cookies;
    private HttpClient browser;

    @BeforeEach
    void freshBrowser() {
        cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        browser = HttpClient.newBuilder().cookieHandler(cookies)
                .connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Test
    void homePageRendersHeaderSidebarAndFooter() throws Exception {
        for (String path : new String[]{"/", "/home"}) {
            assertThat(html(path)).contains(
                    "<title>UTEExpress - Hệ thống Quản lý Chuỗi Giao nhận Logistics HCMUTE</title>",
                    "aria-label=\"Điều hướng chính\"", "ute-landing-hero", "name=\"trackingNumber\"", "<footer", "</html>");
        }
        verifyNoInteractions(registrationService, phoneSessionService, googleAccounts);
    }

    @Test
    void publicAuthPagesCompileAndRenderRealCsrfFields() throws Exception {
        String login = html("/login");
        assertThat(login).contains("Đăng nhập bằng mã OTP điện thoại", "name=\"password\"")
                .doesNotContain("/oauth2/authorization/google");
        csrf(login);
        csrf(html("/register"));
        csrf(html("/auth/phone-login"));
        assertThat(post("/register", Map.of()).statusCode()).isEqualTo(403);
        verifyNoInteractions(registrationService, phoneSessionService, googleAccounts);
    }

    @Test
    void registrationValidationNeverRendersRejectedPasswordAndEscapesContactFields() throws Exception {
        String rejectedPassword = "NeverEchoRejectedPassword" + "Z".repeat(80);
        String namePayload = "<script>alert(1)</script>";
        HttpResponse<String> response = postWithCsrf("/register", html("/register"), Map.of(
                "username", "", "password", rejectedPassword, "fullName", namePayload,
                "phone", "0901234567", "email", "owner@example.test"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("invalid-feedback", "&lt;script&gt;alert(1)&lt;/script&gt;")
                .doesNotContain(rejectedPassword, namePayload);
        csrf(response.body());
        verifyNoInteractions(registrationService, phoneSessionService);
    }

    @Test
    void phoneChallengeRendersFromServerSessionAndDoesNotEchoRejectedOtp() throws Exception {
        when(registrationService.requestLogin(any())).thenReturn(new RegistrationChallengeResponse(
                UUID.randomUUID(), NOW.plusSeconds(300), "+84******567"));
        HttpResponse<String> requested = postWithCsrf("/auth/phone-login", html("/auth/phone-login"),
                Map.of("phone", "0901234567"));
        assertThat(requested.statusCode()).isEqualTo(302);
        assertThat(requested.headers().firstValue("Location").map(URI::create).map(URI::getPath))
                .hasValue("/auth/verify-phone");

        String verification = html("/auth/verify-phone");
        assertThat(verification).contains("+84******567", "Xác thực và đăng nhập");
        HttpResponse<String> rejected = postWithCsrf("/auth/verify-phone", verification,
                Map.of("code", "secret-not-six-digits"));
        assertThat(rejected.statusCode()).isEqualTo(200);
        assertThat(HtmlUtils.htmlUnescape(rejected.body())).contains("Nhập mã gồm 6 chữ số.")
                .doesNotContain("secret-not-six-digits");
        verify(registrationService, never()).verifyLogin(any());
        verifyNoInteractions(phoneSessionService);
    }

    @Test
    void googleCompletionJspEscapesTrustedIdentityAndRendersCsrfWithoutExternalGoogle() throws Exception {
        html("/login");
        String sessionId = cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals("UTEEXPRESS_SESSION"))
                .findFirst().orElseThrow().getValue();
        HttpSession session = sessionTracker.sessions.get(sessionId);
        assertThat(session).isNotNull();
        session.setAttribute(VerifiedGoogleIdentity.SESSION_ATTRIBUTE,
                new VerifiedGoogleIdentity("test-subject", "owner@example.test",
                        "Owner <script>alert(1)</script>", NOW));

        String completion = html("/auth/google/complete");
        assertThat(completion).contains("owner@example.test", "Owner &lt;script&gt;alert(1)&lt;/script&gt;")
                .doesNotContain("<script>alert(1)</script>");
        csrf(completion);
        verifyNoInteractions(googleAccounts, accountSessions);
    }

    private String html(String path) throws Exception {
        HttpResponse<String> response = browser.send(HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(30)).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).as("Rendered GET %s", path).isEqualTo(200);
        assertThat(response.body()).contains("<!DOCTYPE html>").doesNotContain("<%@", "<form:form");
        return response.body();
    }

    private HttpResponse<String> postWithCsrf(String path, String formHtml, Map<String, String> fields) throws Exception {
        Map<String, String> body = new LinkedHashMap<>(fields);
        body.put("_csrf", csrf(formHtml));
        return post(path, body);
    }

    private HttpResponse<String> post(String path, Map<String, String> fields) throws Exception {
        String body = fields.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return browser.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String csrf(String html) {
        Matcher matcher = CSRF.matcher(html);
        assertThat(matcher.find()).as("JSP renders a nonempty CSRF hidden field").isTrue();
        return matcher.group(1);
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    @TestComponent
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, HomeController.class,
            RegistrationController.class, GoogleRegistrationController.class, AuthenticationViewAdvice.class})
    static class RenderingApplication {
        @Bean Clock clock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
        @Bean SessionTracker sessionTracker() { return new SessionTracker(); }
        @Bean ServletListenerRegistrationBean<SessionTracker> sessionListener(SessionTracker tracker) {
            return new ServletListenerRegistrationBean<>(tracker);
        }
    }

    static class SessionTracker implements HttpSessionListener {
        private final Map<String, HttpSession> sessions = new ConcurrentHashMap<>();
        @Override public void sessionCreated(HttpSessionEvent event) {
            sessions.put(event.getSession().getId(), event.getSession());
        }
        @Override public void sessionDestroyed(HttpSessionEvent event) {
            sessions.remove(event.getSession().getId());
        }
    }
}
