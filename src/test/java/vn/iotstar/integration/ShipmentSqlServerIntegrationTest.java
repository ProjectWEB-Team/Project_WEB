package vn.iotstar.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.iotstar.dto.shipment.CreateShipmentRequest;
import vn.iotstar.dto.shipment.ShipmentDetailResponse;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.service.ShipmentService;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs only against a user-provisioned SQL Server database. It assumes
 * docs/schema.sql and the event_type, account and handover-wait migrations were applied.
 * No test transaction: service calls must really commit. Every fixture belongs to this test only.
 */
@EnabledIf(value = "sqlServerVerificationConfigured", disabledReason = "REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
// Keep the normal servlet/MVC infrastructure so the production SecurityConfig
// can initialize its MVC request matchers; no real HTTP server is started.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("sqlserver-verification")
class ShipmentSqlServerIntegrationTest {
    @Autowired private ShipmentService shipmentService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private vn.iotstar.repository.ShipmentEventRepository shipmentEvents;
    private final String suffix = UUID.randomUUID().toString().replace("-", "");
    private final String username = "shipment-it-" + suffix;
    private final String email = username + "@example.test";
    private final String serviceCode = "SHIP_IT_" + suffix;
    private Long testUserId;
    private Long testShipmentId;
    private User actor;
    private String senderPhone;
    private String receiverPhone;
    private final String deliveryDistrict = "SHIP_IT_" + suffix;
    private final String deliveryHubCode = "SIT_" + suffix.substring(0, 12);

    @BeforeEach
    void createIsolatedSenderAndPricingPolicy() throws Exception {
        applyCurrentForwardMigrations();
        senderPhone = unusedPhone();
        jdbcTemplate.update("INSERT INTO dbo.users (username, password, full_name, phone, email, role, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                username, "test-hash", "Shipment Test User", senderPhone, email, "ROLE_USER", "ACTIVE");
        testUserId = jdbcTemplate.queryForObject("SELECT id FROM dbo.users WHERE username = ? AND email = ?",
                Long.class, username, email);
        receiverPhone = unusedPhone();
        jdbcTemplate.update("INSERT INTO dbo.hubs (code, name, level, type, region, province, district, status) "
                + "VALUES (?, ?, 1, 'DELIVERY_HUB', 'SOUTH', 'HCM', ?, 'ACTIVE')", deliveryHubCode, "Intake verification hub", deliveryDistrict);
        // Direct service tests use a unique service code so no customer's ACTIVE price row is changed.
        jdbcTemplate.update("INSERT INTO dbo.pricing_policies (service_code, from_region, to_region, base_weight_g, "
                        + "base_price, extra_weight_step_g, extra_weight_price, insurance_rate_percent, cod_fee_percent, status) "
                        + "VALUES (?, 'SOUTH', 'SOUTH', 500, 30000, 500, 5000, 1, 2, 'ACTIVE')", serviceCode);
        actor = new User(testUserId, username, "test-hash", UserRole.ROLE_USER, "ACTIVE");
        authenticate();
    }

    void applyCurrentForwardMigrations() throws Exception {
        SqlServerMigrationSupport.apply(dataSource,
                "db/migration/V2__phase_01_shipment_events_event_type.sql",
                "db/migration/V6__phase_03_hub_routing_schema.sql", "db/migration/V14__phase_05_intake_service_code.sql");
    }

    @AfterEach
    void removeVerificationData() {
        SecurityContextHolder.clearContext();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            // Find by the exact test identity, even when setup or create() failed before returning its ID.
            for (Long id : jdbcTemplate.queryForList("SELECT id FROM dbo.users WHERE username = ? AND email = ?",
                    Long.class, username, email)) {
                jdbcTemplate.update("DELETE FROM dbo.payments WHERE shipment_id IN "
                        + "(SELECT id FROM dbo.shipments WHERE sender_user_id = ?)", id);
                jdbcTemplate.update("DELETE FROM dbo.shipment_events WHERE actor_user_id = ? OR shipment_id IN "
                        + "(SELECT id FROM dbo.shipments WHERE sender_user_id = ?)", id, id);
                jdbcTemplate.update("DELETE FROM dbo.shipments WHERE sender_user_id = ?", id);
                jdbcTemplate.update("DELETE FROM dbo.users WHERE id = ? AND username = ? AND email = ?", id, username, email);
            }
            jdbcTemplate.update("DELETE FROM dbo.pricing_policies WHERE service_code = ? AND from_region = 'SOUTH' AND to_region = 'SOUTH'",
                    serviceCode);
            jdbcTemplate.update("DELETE FROM dbo.hubs WHERE code = ? AND district = ?", deliveryHubCode, deliveryDistrict);
        });
    }

    @Test
    void persistsAndReadsBackTheCanonicalShipmentAndCreationEvent() {
        testShipmentId = shipmentService.create(request());
        ShipmentDetailResponse detail = shipmentService.getDetail(testShipmentId);

        assertThat(detail.currentStatus()).isEqualTo(ShipmentStatus.WAITING_FOR_PAYMENT);
        assertThat(detail.waitingForHandoverAt()).isNull();
        assertThat(detail.receiverPhone()).isEqualTo(receiverPhone);
        assertThat(detail.shippingFee()).isEqualByComparingTo("30000");
        assertThat(detail.events()).singleElement().satisfies(event -> {
            assertThat(event.eventType()).isEqualTo("SHIPMENT_CREATED");
            assertThat(event.occurredAt()).isNotNull();
        });
        assertThat(jdbcTemplate.queryForObject("SELECT event_type FROM dbo.shipment_events WHERE shipment_id = ?", String.class, testShipmentId))
                .isEqualTo("SHIPMENT_CREATED");
        assertThat(jdbcTemplate.queryForObject("SELECT current_status FROM dbo.shipments WHERE id = ?", String.class, testShipmentId))
                .isEqualTo("WAITING_FOR_PAYMENT");
        assertThat(jdbcTemplate.queryForObject("SELECT created_at FROM dbo.shipments WHERE id = ?", Object.class, testShipmentId)).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT actor_user_id FROM dbo.shipment_events WHERE shipment_id = ?", Long.class, testShipmentId))
                .isEqualTo(testUserId);

        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO dbo.shipments "
                        + "(tracking_number, sender_user_id, sender_name, sender_phone, sender_address, receiver_name, receiver_phone, receiver_address, "
                        + "pickup_method, fee_payer, inspection_policy, payment_status, current_status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                detail.trackingNumber(), testUserId, "Duplicate", senderPhone, "A", "Receiver", receiverPhone, "B",
                "SELF_DROPOFF", "SENDER", "NO_INSPECTION", "UNPAID", "CREATED"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO dbo.shipment_events (shipment_id, actor_user_id, event_type, description) VALUES (?, ?, ?, ?)",
                -1L, testUserId, "SHIPMENT_CREATED", "Invalid foreign key"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "SENDER,VNPAY,WAITING_FOR_PAYMENT,PENDING",
            "SENDER,CASH,WAITING_FOR_HANDOVER,UNPAID",
            "RECEIVER,CASH,WAITING_FOR_HANDOVER,UNPAID"
    })
    void persistsThePermittedPaymentBranches(String payer, String method, ShipmentStatus shipmentStatus, String paymentStatus) {
        CreateShipmentRequest form = request();
        form.setFeePayer(payer);
        form.setPaymentMethod(method);
        form.setWeightGrams(501);
        form.setDeclaredValue(new BigDecimal("100000"));
        form.setCodAmount(new BigDecimal("200000"));
        testShipmentId = shipmentService.create(form);

        ShipmentDetailResponse detail = shipmentService.getDetail(testShipmentId);
        PaymentSnapshot before = payment();
        assertThat(detail.currentStatus()).isEqualTo(shipmentStatus);
        assertThat(detail.paymentStatus()).isEqualTo(paymentStatus);
        assertThat(detail.feePayer()).isEqualTo(payer);
        assertThat(detail.paymentMethod()).isEqualTo(method);
        assertThat(detail.shippingFee()).isEqualByComparingTo("40000");
        assertThat(before.payer()).isEqualTo(payer);
        assertThat(before.method()).isEqualTo(method);
        assertThat(before.status()).isEqualTo(paymentStatus);
        assertThat(before.amount()).isEqualByComparingTo(detail.shippingFee());
        assertThat(before.reference()).isNull();
        assertThat(before.paidAt()).isNull();
        if (shipmentStatus == ShipmentStatus.WAITING_FOR_HANDOVER) {
            assertThat(detail.waitingForHandoverAt()).isEqualTo(detail.createdAt());
        } else {
            assertThat(detail.waitingForHandoverAt()).isNull();
        }

        if ("VNPAY".equals(method)) {
            shipmentService.confirmPayment(testShipmentId);
            PaymentSnapshot paid = payment();
            assertThat(paid.status()).isEqualTo("PAID");
            assertThat(paid.reference()).startsWith("DEMO-");
            assertThat(paid.paidAt()).isNotNull();
            LocalDateTime waitStartedAt = shipmentService.getDetail(testShipmentId).waitingForHandoverAt();
            assertThat(waitStartedAt).isNotNull().isAfterOrEqualTo(detail.createdAt());
            assertThat(shipmentService.getDetail(testShipmentId).currentStatus()).isEqualTo(ShipmentStatus.WAITING_FOR_HANDOVER);
            assertThat(shipmentService.getDetail(testShipmentId).paymentStatus()).isEqualTo("PAID");
            shipmentService.confirmPayment(testShipmentId);
            assertThat(shipmentService.getDetail(testShipmentId).waitingForHandoverAt()).isEqualTo(waitStartedAt);
            assertThat(payment()).isEqualTo(paid);
            assertThat(paymentConfirmationCount()).isEqualTo(1);
        } else {
            assertThatThrownBy(() -> shipmentService.confirmPayment(testShipmentId)).isInstanceOf(IllegalArgumentException.class);
            assertThat(payment()).isEqualTo(before);
            assertThat(shipmentService.getDetail(testShipmentId).currentStatus()).isEqualTo(shipmentStatus);
            assertThat(paymentConfirmationCount()).isZero();
        }
    }

    @Test
    void concurrentPaymentConfirmationsCommitOneEventAndKeepTheFirstReference() throws Exception {
        testShipmentId = shipmentService.create(request());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        List<Future<PaymentSnapshot>> results = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                results.add(executor.submit(() -> {
                    authenticate(); // SecurityContextHolder is thread-bound; each request needs its own identity.
                    try {
                        ready.countDown();
                        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                        TransactionTemplate transaction = new TransactionTemplate(transactions);
                        transaction.setTimeout(10);
                        transaction.executeWithoutResult(status -> shipmentService.confirmPayment(testShipmentId));
                        return payment();
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            PaymentSnapshot first = results.get(0).get(15, TimeUnit.SECONDS);
            assertThat(results.get(1).get(15, TimeUnit.SECONDS)).isEqualTo(first);
            assertThat(first.status()).isEqualTo("PAID");
            assertThat(first.reference()).startsWith("DEMO-");
            assertThat(first.paidAt()).isNotNull();
            assertThat(paymentConfirmationCount()).isEqualTo(1);
            assertThat(shipmentService.getDetail(testShipmentId).currentStatus()).isEqualTo(ShipmentStatus.WAITING_FOR_HANDOVER);
            assertThat(shipmentService.getDetail(testShipmentId).paymentStatus()).isEqualTo("PAID");
            LocalDateTime waitStartedAt = shipmentService.getDetail(testShipmentId).waitingForHandoverAt();
            assertThat(waitStartedAt).isNotNull();
            shipmentService.confirmPayment(testShipmentId);
            assertThat(shipmentService.getDetail(testShipmentId).waitingForHandoverAt()).isEqualTo(waitStartedAt);
            assertThat(payment()).isEqualTo(first);
            assertThat(paymentConfirmationCount()).isEqualTo(1);
        } finally {
            start.countDown();
            results.forEach(result -> result.cancel(true));
            executor.shutdownNow();
            assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void sameSecondPersistedEventsRetainTheirInsertionOrder() {
        testShipmentId = shipmentService.create(request());
        shipmentService.confirmPayment(testShipmentId);
        // Deliberately exercise SQL Server DATETIME2(0) ties without manufacturing journey state.
        jdbcTemplate.update("UPDATE dbo.shipment_events SET created_at = ? WHERE shipment_id = ?",
                LocalDateTime.of(2026, 1, 1, 12, 0, 0), testShipmentId);
        List<Long> expected = jdbcTemplate.queryForList("SELECT id FROM dbo.shipment_events WHERE shipment_id = ? ORDER BY id", Long.class, testShipmentId);
        List<Long> actual = new TransactionTemplate(transactions).execute(status -> shipmentEvents
                .findByShipmentIdOrderByCreatedAtAsc(testShipmentId).stream().map(vn.iotstar.entity.ShipmentEvent::getId).toList());
        assertThat(expected).hasSizeGreaterThanOrEqualTo(2);
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    private PaymentSnapshot payment() {
        // queryForObject also fails if creation accidentally inserted more than one shipping-fee row.
        return jdbcTemplate.queryForObject("SELECT payer_type, method, status, amount, transaction_ref, paid_at FROM dbo.payments "
                        + "WHERE shipment_id = ? AND payment_type = 'SHIPPING_FEE'",
                (row, number) -> new PaymentSnapshot(row.getString("payer_type"), row.getString("method"),
                        row.getString("status"), row.getBigDecimal("amount"), row.getString("transaction_ref"),
                        row.getObject("paid_at", LocalDateTime.class)), testShipmentId);
    }

    private int paymentConfirmationCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.shipment_events WHERE shipment_id = ? AND event_type = 'PAYMENT_CONFIRMED'",
                Integer.class, testShipmentId);
    }

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, null, actor.getAuthorities()));
    }

    private String unusedPhone() {
        String phone;
        do {
            phone = "+849" + String.format(Locale.ROOT, "%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        } while (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.users WHERE phone = ?", Integer.class, phone) != 0);
        return phone;
    }

    private record PaymentSnapshot(String payer, String method, String status, BigDecimal amount,
                                   String reference, LocalDateTime paidAt) { }

    static boolean sqlServerVerificationConfigured() {
        return hasValue("UTEEXPRESS_SQLSERVER_URL") && hasValue("UTEEXPRESS_SQLSERVER_USERNAME")
                && hasValue("UTEEXPRESS_SQLSERVER_PASSWORD");
    }

    private static boolean hasValue(String name) {
        String value = System.getenv(name);
        return value != null && !value.isBlank();
    }

    private CreateShipmentRequest request() {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setSenderName("Shipment Test User");
        request.setSenderPhone(senderPhone);
        request.setSenderAddress("1 Verification Street");
        request.setReceiverName("Verification Receiver");
        request.setReceiverPhone(receiverPhone);
        request.setReceiverAddress("2 Verification Street");
        request.setReceiverProvince("HCM");
        request.setReceiverDistrict(deliveryDistrict);
        request.setServiceCode(serviceCode);
        request.setFromRegion("SOUTH");
        request.setToRegion("SOUTH");
        request.setWeightGrams(500);
        request.setLengthCm(10);
        request.setWidthCm(10);
        request.setHeightCm(10);
        return request;
    }
}
