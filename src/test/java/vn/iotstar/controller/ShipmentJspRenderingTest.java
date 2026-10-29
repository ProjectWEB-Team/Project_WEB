package vn.iotstar.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.dto.shipment.QuoteResponse;
import vn.iotstar.dto.shipment.ShipmentDetailResponse;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.ShipmentService;

import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Exercises JSP compilation and real session/CSRF form round trips without a database. */
@SpringBootTest(classes = ShipmentJspRenderingTest.RenderingApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.address=127.0.0.1",
                "spring.mvc.view.prefix=/WEB-INF/views/", "spring.mvc.view.suffix=.jsp",
                "uteexpress.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "spring.main.banner-mode=off"
        })
class ShipmentJspRenderingTest {
    private static final BigDecimal FEE = new BigDecimal("35000");
    @LocalServerPort private int port;
    @Autowired private PasswordEncoder passwordEncoder;
    @MockBean private ShipmentService shipments;
    @MockBean private UserDetailsService users;
    @MockBean private JwtService jwt;
    private HttpClient browser;

    @BeforeEach
    void signInWithAFreshBrowserSession() throws Exception {
        browser = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
        when(users.loadUserByUsername("wizard-test")).thenReturn(User.withUsername("wizard-test")
                .password(passwordEncoder.encode("fixture-password")).roles("USER").build());
        HttpResponse<String> login = postWithCsrf("/login", html("/login"),
                Map.of("username", "wizard-test", "password", "fixture-password"));
        assertThat(login.statusCode()).isEqualTo(302);
        assertThat(login.headers().firstValue("Location").map(URI::create).map(URI::getPath))
                .hasValue("/user/shipments/create");
    }

    @Test
    void initialFormRendersAllStepsWithCsrfAndRequiresAQuoteBeforeCreation() throws Exception {
        String form = html("/user/shipments/create");
        assertThat(form).contains("action=\"/user/shipments\"", "data-has-quote=\"false\"",
                "id=\"shipment-step-1\"", "id=\"shipment-step-2\"", "id=\"shipment-step-3\"",
                "id=\"shipment-step-4\"", "id=\"shipment-quote-button\"", "/assets/js/shipment-wizard.js")
                .doesNotContain("id=\"shipment-create-button\"", "name=\"quoteReference\"");
        assertThat(form).contains("id=\"senderProvinceCode\"", "id=\"senderWardCode\"", "id=\"receiverProvinceCode\"", "id=\"receiverWardCode\"",
                "value=\"79\"", "value=\"01\"", "/assets/js/shipment-address.js")
                .doesNotContain("name=\"fromRegion\"", "name=\"toRegion\"", "name=\"receiverDistrict\"",
                        "senderWardSearch", "receiverWardSearch");
        assertThat(form).contains("id=\"ute-contact\"", "mailto:khangfithcmute@gmail.com", "tel:+84911195500",
                "24162091@student.hcmute.edu.vn", "24162144@student.hcmute.edu.vn");
        assertThat(form).contains("class=\"nav-link active\" href=\"/user/shipments/create\" aria-current=\"page\"")
                .doesNotContain("class=\"nav-link active\" href=\"/user/shipments\"");
        hiddenValue(form, "_csrf");
        Files.writeString(Path.of("target/shipment-wizard-empty.html"), form);
        verifyNoInteractions(shipments);
    }

    @Test
    void addressCatalogServesAllProvincesAndWardsFromTheBundledSource() throws Exception {
        var response = browser.send(HttpRequest.newBuilder(uri("/user/shipments/address-catalog")).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).isEqualTo(200);
        var catalog = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body());
        assertThat(catalog.size()).isEqualTo(34);
        int wards = 0;
        for (var province : catalog) wards += province.get("wards").size();
        assertThat(wards).isEqualTo(3321);
    }

    @Test
    void quotedFormEscapesSubmittedValuesAndPostsItsServerQuoteReferenceToCreate() throws Exception {
        when(shipments.quote(any())).thenReturn(new QuoteResponse(FEE));
        when(shipments.create(any(), eq(FEE))).thenReturn(42L);
        Map<String, String> fields = validFields();
        fields.put("senderName", "Owner <b>example</b>");
        fields.put("receiverAddress", "2 Receiver Street & Annex");
        HttpResponse<String> quote = postWithCsrf("/user/shipments/quote", html("/user/shipments/create"), fields);

        assertThat(quote.statusCode()).isEqualTo(200);
        assertThat(quote.body()).contains("data-has-quote=\"true\"", "id=\"shipment-quote\"",
                "id=\"shipment-create-button\"", "Owner &lt;b&gt;example&lt;/b&gt;",
                "2 Receiver Street &amp; Annex")
                .doesNotContain("Owner <b>example</b>");
        assertThat(quote.body()).contains("35.000 đ");
        assertThat(quote.body()).contains("class=\"nav-link active\" href=\"/user/shipments/create\" aria-current=\"page\"")
                .doesNotContain("class=\"nav-link active\" href=\"/user/shipments\"");
        assertThat(quote.body()).containsPattern("<option[^>]*value=\"79\"[^>]*selected=\"selected\"")
                .containsPattern("<option[^>]*value=\"26740\"[^>]*selected=\"selected\"");
        fields.put("quoteReference", hiddenValue(quote.body(), "quoteReference"));
        Files.writeString(Path.of("target/shipment-wizard-quote.html"), quote.body());

        HttpResponse<String> created = postWithCsrf("/user/shipments", quote.body(), fields);
        assertThat(created.statusCode()).isEqualTo(302);
        assertThat(created.headers().firstValue("Location").map(URI::create).map(URI::getPath))
                .hasValue("/user/shipments/42");
        verify(shipments).create(any(), eq(FEE));
    }

    @Test
    void invalidNumbersRenderFieldFeedbackAndPreserveOtherInputsWithoutCallingService() throws Exception {
        Map<String, String> fields = validFields();
        fields.put("weightGrams", "not-a-number");
        fields.put("lengthCm", "0");
        HttpResponse<String> response = postWithCsrf("/user/shipments/quote", html("/user/shipments/create"), fields);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("id=\"weightGrams.errors\"", "id=\"lengthCm.errors\"",
                "is-invalid", "value=\"Receiver\"", "data-has-quote=\"false\"")
                .doesNotContain("id=\"shipment-create-button\"");
        hiddenValue(response.body(), "_csrf");
        verifyNoInteractions(shipments);
    }

    @Test
    void missingPricingPolicyRendersAnEscapedGlobalErrorWithAnEditableForm() throws Exception {
        when(shipments.quote(any())).thenThrow(new IllegalArgumentException("Chưa có bảng giá <STANDARD>."));
        HttpResponse<String> response = postWithCsrf("/user/shipments/quote", html("/user/shipments/create"), validFields());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("id=\"shipment-form-errors\"", "Chưa có bảng giá &lt;STANDARD&gt;.",
                "value=\"Receiver\"", "id=\"shipment-quote-button\"")
                .doesNotContain("Chưa có bảng giá <STANDARD>.", "id=\"shipment-create-button\"");
        hiddenValue(response.body(), "_csrf");
    }

    @ParameterizedTest
    @CsvSource({
            "WAITING_FOR_PAYMENT,PENDING,VNPAY,SENDER,SELF_DROPOFF,Thanh toán cước để đơn",
            "WAITING_FOR_HANDOVER,UNPAID,CASH,SENDER,HOME_PICKUP,Cước tiền mặt thu khi bàn giao",
            "WAITING_FOR_HANDOVER,UNPAID,CASH,RECEIVER,SELF_DROPOFF,Cước phí sẽ được thu từ người nhận"
    })
    void detailRendersTheApplicablePaymentAndHandoverInstructions(ShipmentStatus status, String paymentStatus,
            String paymentMethod, String payer, String pickupMethod, String expectedInstruction) throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 23, 9, 0);
        when(shipments.getDetail(42L)).thenReturn(new ShipmentDetailResponse(42L, "UTX-FIXTURE", status,
                "Owner", "0900000001", "1 Owner Street", "Receiver", "0900000002", "2 Receiver Street",
                createdAt, List.of(), FEE, paymentStatus, paymentMethod, pickupMethod, payer,
                status == ShipmentStatus.WAITING_FOR_HANDOVER ? createdAt : null));

        String detail = html("/user/shipments/42");
        assertThat(detail).contains("UTX-FIXTURE", expectedInstruction);
        assertThat(detail).contains("class=\"nav-link active\" href=\"/user/shipments\" aria-current=\"page\"")
                .doesNotContain("class=\"nav-link active\" href=\"/user/shipments/create\"");
        if (status == ShipmentStatus.WAITING_FOR_PAYMENT) {
            assertThat(detail).contains("action=\"/user/shipments/42/confirm-payment\"");
            hiddenValue(detail, "_csrf");
        } else {
            assertThat(detail).doesNotContain("/confirm-payment");
        }
    }

    private Map<String, String> validFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("senderProvinceCode", "79");
        fields.put("senderWardCode", "26740");
        fields.put("receiverProvinceCode", "79");
        fields.put("receiverWardCode", "26740");
        fields.put("senderName", "Owner");
        fields.put("senderPhone", "0900000001");
        fields.put("senderAddress", "1 Owner Street");
        fields.put("receiverName", "Receiver");
        fields.put("receiverPhone", "0900000002");
        fields.put("receiverAddress", "2 Receiver Street");
        fields.put("receiverProvince", "HCM");
        fields.put("receiverDistrict", "Q1");
        fields.put("fromRegion", "SOUTH");
        fields.put("toRegion", "SOUTH");
        fields.put("weightGrams", "800");
        fields.put("lengthCm", "10");
        fields.put("widthCm", "10");
        fields.put("heightCm", "10");
        return fields;
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
        body.put("_csrf", hiddenValue(formHtml, "_csrf"));
        String encoded = body.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return browser.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encoded)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String hiddenValue(String html, String name) {
        Matcher matcher = Pattern.compile("<input[^>]*name=\"" + Pattern.quote(name) + "\"[^>]*value=\"([^\"]+)\"")
                .matcher(html);
        assertThat(matcher.find()).as("JSP renders a nonempty %s hidden field", name).isTrue();
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
            ShipmentController.class, AuthenticationViewAdvice.class})
    static class RenderingApplication {}
}
