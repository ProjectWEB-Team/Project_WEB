package vn.iotstar.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import vn.iotstar.dto.shipment.CreateShipmentRequest;
import vn.iotstar.dto.shipment.ShipmentDetailResponse;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.service.ShipmentService;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs only against a user-provisioned SQL Server database. It assumes
 * docs/schema.sql and the Phase-01 event_type reconciliation script were applied.
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
    private Long testUserId;
    private Long testShipmentId;

    @BeforeEach
    void applyCurrentForwardMigrations() throws Exception {
        SqlServerMigrationSupport.apply(dataSource,
                "db/phase-01-shipment-events-event-type.sql",
                "db/phase-03-hub-routing-schema.sql");
    }

    @AfterEach
    void removeVerificationData() {
        SecurityContextHolder.clearContext();
        if (testShipmentId != null) {
            jdbcTemplate.update("DELETE FROM dbo.shipment_events WHERE shipment_id = ?", testShipmentId);
            jdbcTemplate.update("DELETE FROM dbo.shipments WHERE id = ?", testShipmentId);
        }
        if (testUserId != null) {
            jdbcTemplate.update("DELETE FROM dbo.users WHERE id = ?", testUserId);
        }
    }

    @Test
    void persistsAndReadsBackTheCanonicalShipmentAndCreationEvent() {
        String suffix = String.valueOf(System.nanoTime());
        jdbcTemplate.update("INSERT INTO dbo.users (username, password, full_name, phone, email, role, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                "phase01-" + suffix, "test-hash", "Phase One User", "09" + suffix.substring(Math.max(0, suffix.length() - 8)),
                "phase01-" + suffix + "@example.test", "ROLE_USER", "ACTIVE");
        testUserId = jdbcTemplate.queryForObject("SELECT id FROM dbo.users WHERE username = ?", Long.class, "phase01-" + suffix);
        User actor = new User(testUserId, "phase01-" + suffix, "test-hash", UserRole.ROLE_USER, "ACTIVE");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, null, actor.getAuthorities()));

        testShipmentId = shipmentService.create(request());
        ShipmentDetailResponse detail = shipmentService.getDetail(testShipmentId);

        assertThat(detail.currentStatus()).isEqualTo(ShipmentStatus.CREATED);
        assertThat(detail.events()).singleElement().satisfies(event -> {
            assertThat(event.eventType()).isEqualTo("SHIPMENT_CREATED");
            assertThat(event.occurredAt()).isNotNull();
        });
        assertThat(jdbcTemplate.queryForObject("SELECT event_type FROM dbo.shipment_events WHERE shipment_id = ?", String.class, testShipmentId))
                .isEqualTo("SHIPMENT_CREATED");
        assertThat(jdbcTemplate.queryForObject("SELECT current_status FROM dbo.shipments WHERE id = ?", String.class, testShipmentId))
                .isEqualTo("CREATED");
        assertThat(jdbcTemplate.queryForObject("SELECT created_at FROM dbo.shipments WHERE id = ?", Object.class, testShipmentId)).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT actor_user_id FROM dbo.shipment_events WHERE shipment_id = ?", Long.class, testShipmentId))
                .isEqualTo(testUserId);

        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO dbo.shipments "
                        + "(tracking_number, sender_user_id, sender_name, sender_phone, sender_address, receiver_name, receiver_phone, receiver_address, "
                        + "pickup_method, fee_payer, inspection_policy, payment_status, current_status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                detail.trackingNumber(), testUserId, "Duplicate", "0900000011", "A", "Receiver", "0900000012", "B",
                "SELF_DROPOFF", "SENDER", "NO_INSPECTION", "UNPAID", "CREATED"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO dbo.shipment_events (shipment_id, actor_user_id, event_type, description) VALUES (?, ?, ?, ?)",
                -1L, testUserId, "SHIPMENT_CREATED", "Invalid foreign key"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

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
        request.setSenderName("Phase One User");
        request.setSenderPhone("0900000001");
        request.setSenderAddress("1 Verification Street");
        request.setReceiverName("Verification Receiver");
        request.setReceiverPhone("0900000002");
        request.setReceiverAddress("2 Verification Street");
        return request;
    }
}
