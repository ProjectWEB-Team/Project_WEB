package vn.iotstar.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.dto.firstmile.PendingPickupResponse;
import vn.iotstar.dto.firstmile.PickupTaskResponse;
import vn.iotstar.dto.firstmile.ShipperOptionResponse;
import vn.iotstar.dto.shipment.ShipmentSummaryResponse;
import vn.iotstar.dto.tracking.IncomingShipmentResponse;
import vn.iotstar.dto.tracking.PublicTrackingResponse;
import vn.iotstar.dto.tracking.RecipientShipmentResponse;
import vn.iotstar.dto.tracking.TrackingEventResponse;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.FirstMileService;
import vn.iotstar.service.ShipmentService;
import vn.iotstar.service.TrackingService;

import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** Compiles and renders the Phase-02 list, tracking, recipient, coordinator and shipper JSPs through Jasper. */
@SpringBootTest(classes = IntakeJspRenderingTest.RenderingApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.address=127.0.0.1",
                "spring.mvc.view.prefix=/WEB-INF/views/", "spring.mvc.view.suffix=.jsp",
                "uteexpress.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "spring.main.banner-mode=off"
        })
class IntakeJspRenderingTest {
    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 23, 9, 0);
    @LocalServerPort private int port;
    @Autowired private PasswordEncoder passwordEncoder;
    @MockBean private ShipmentService shipments;
    @MockBean private TrackingService tracking;
    @MockBean private FirstMileService firstMile;
    @MockBean private UserDetailsService users;
    @MockBean private JwtService jwt;
    private HttpClient browser;

    @BeforeEach
    void signIn() throws Exception {
        browser = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
        when(users.loadUserByUsername("render-test")).thenReturn(User.withUsername("render-test")
                .password(passwordEncoder.encode("fixture-password")).roles("USER", "HUB_STAFF", "SHIPPER").build());
        String csrf = csrf(html("/login"));
        String body = "username=render-test&password=fixture-password&_csrf=" + URLEncoder.encode(csrf, StandardCharsets.UTF_8);
        HttpResponse<String> login = browser.send(HttpRequest.newBuilder(uri("/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).isEqualTo(302);
    }

    @Test
    void senderListRendersRowsAndEscapesNames() throws Exception {
        when(shipments.listMine(eq(null), anyInt())).thenReturn(new PageImpl<>(List.of(new ShipmentSummaryResponse(42L,
                "UTX-1", "<b>Receiver</b>", ShipmentStatus.WAITING_FOR_HANDOVER, new BigDecimal("25000"), "UNPAID", AT))));
        assertThat(html("/user/shipments")).contains("UTX-1", "&lt;b&gt;Receiver&lt;/b&gt;", "25,000", "/user/shipments/42");
    }

    @Test
    void guestTrackingRendersTimelineWithoutLogin() throws Exception {
        when(tracking.track("UTX-1")).thenReturn(new PublicTrackingResponse("UTX-1", ShipmentStatus.PICKED_UP, "Hub Thu Duc",
                List.of(new TrackingEventResponse("Shipper đã lấy hàng", AT))));
        HttpClient guest = HttpClient.newHttpClient();
        String page = guest.send(HttpRequest.newBuilder(uri("/tracking?code=UTX-1")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
        assertThat(page).contains("PICKED_UP", "Hub Thu Duc", "Shipper đã lấy hàng");
    }

    @Test
    void recipientPagesRenderMaskedListAndUnlockForm() throws Exception {
        when(tracking.incoming(anyInt())).thenReturn(new PageImpl<>(List.of(
                new IncomingShipmentResponse(42L, "****1234", "N***", ShipmentStatus.WAITING_FOR_HANDOVER, AT))));
        when(tracking.incomingDetail(42L, false)).thenReturn(new RecipientShipmentResponse(42L, false, "****1234",
                ShipmentStatus.WAITING_FOR_HANDOVER, "N***", "T***", null, null, List.of()));
        assertThat(html("/user/incoming")).contains("****1234", "N***");
        assertThat(html("/user/incoming/42")).contains("/user/incoming/42/unlock", "name=\"trackingNumber\"", "_csrf");
    }

    @Test
    void coordinatorAndShipperPagesRenderTheirForms() throws Exception {
        when(firstMile.pendingPickups()).thenReturn(List.of(new PendingPickupResponse(42L, "UTX-1", "Sender", "0900000001", "1 Street", AT)));
        when(firstMile.shippersOfMyHub()).thenReturn(List.of(new ShipperOptionResponse(30L, "Shipper A")));
        when(firstMile.myPickupTasks()).thenReturn(List.of(new PickupTaskResponse(5L, "UTX-1", "Sender", "0900000001",
                "1 Street", new BigDecimal("25000"), AT, 1)));
        assertThat(html("/hub/pickups")).contains("/hub/pickups/42/assign", "Shipper A", "/hub/pickups/42/service-failure");
        assertThat(html("/shipper/tasks")).contains("/shipper/tasks/5/complete", "/shipper/tasks/5/attempt", "25,000");
    }

    private String html(String path) throws Exception {
        HttpResponse<String> response = browser.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(30)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).as("Rendered GET %s", path).isEqualTo(200);
        assertThat(response.body()).contains("<!DOCTYPE html>").doesNotContain("<%@");
        return response.body();
    }

    private static String csrf(String html) {
        Matcher matcher = Pattern.compile("<input[^>]*name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    @TestComponent
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, HomeController.class, ShipmentController.class,
            TrackingController.class, IncomingShipmentController.class, HubPickupController.class,
            ShipperTaskController.class, AuthenticationViewAdvice.class})
    static class RenderingApplication {}
}
