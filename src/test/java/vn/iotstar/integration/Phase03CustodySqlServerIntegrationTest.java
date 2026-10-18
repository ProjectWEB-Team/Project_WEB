package vn.iotstar.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Real SQL Server-only guard for sealed container membership and handover reconciliation. */
@EnabledIf(value = "sqlServerVerificationConfigured", disabledReason = "REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("sqlserver-verification")
class Phase03CustodySqlServerIntegrationTest {
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbcTemplate;
    private String suffix;
    private Long originHubId; private Long destinationHubId; private Long driverId; private Long senderId; private Long ownerId; private Long shipmentId; private Long containerId; private Long handoverId;

    @BeforeEach void seed() throws Exception {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("db/phase-01-shipment-events-event-type.sql")), false, false, ScriptUtils.DEFAULT_COMMENT_PREFIX, "GO", ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER, ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
            ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("db/phase-03-hub-routing-schema.sql")), false, false, ScriptUtils.DEFAULT_COMMENT_PREFIX, "GO", ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER, ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
            ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("db/phase-03-container-handover-schema.sql")), false, false, ScriptUtils.DEFAULT_COMMENT_PREFIX, "GO", ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER, ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
        } finally { DataSourceUtils.releaseConnection(connection, dataSource); }
        suffix = String.valueOf(System.nanoTime());
        originHubId = hub("P3-ORIGIN-" + suffix); destinationHubId = hub("P3-DEST-" + suffix);
        senderId = user("phase03-outbound-" + suffix, "ROLE_HUB_STAFF"); driverId = user("phase03-driver-" + suffix, "ROLE_LINEHAUL_DRIVER");
        jdbcTemplate.update("INSERT INTO dbo.employee_profiles (user_id, employee_code, hub_id, position, status) VALUES (?, ?, ?, ?, ?)", senderId, "P3-" + suffix, originHubId, "OUTBOUND", "ACTIVE");
        shipmentId = shipment();
        containerId = container("BAG-" + suffix);
        jdbcTemplate.update("INSERT INTO dbo.container_items (container_id, shipment_id, status, scanned_at) VALUES (?, ?, ?, SYSDATETIME())", containerId, shipmentId, "IN_CONTAINER");
        handoverId = handover("PENDING_ACCEPTANCE", 1, 0);
        jdbcTemplate.update("INSERT INTO dbo.handover_containers (handover_id, container_id, status) VALUES (?, ?, ?)", handoverId, containerId, "EXPECTED");
    }

    @AfterEach void cleanup() {
        if (suffix == null) return;
        jdbcTemplate.update("DELETE FROM dbo.handover_containers WHERE handover_id IN (SELECT id FROM dbo.handovers WHERE from_hub_id IN (?, ?))", originHubId, destinationHubId);
        jdbcTemplate.update("DELETE FROM dbo.handovers WHERE from_hub_id IN (?, ?)", originHubId, destinationHubId);
        jdbcTemplate.update("DELETE FROM dbo.container_items WHERE container_id IN (SELECT id FROM dbo.parcel_containers WHERE origin_hub_id IN (?, ?))", originHubId, destinationHubId);
        jdbcTemplate.update("DELETE FROM dbo.parcel_containers WHERE origin_hub_id IN (?, ?)", originHubId, destinationHubId);
        jdbcTemplate.update("DELETE FROM dbo.shipment_events WHERE shipment_id = ?", shipmentId);
        jdbcTemplate.update("DELETE FROM dbo.shipment_routes WHERE shipment_id = ?", shipmentId);
        jdbcTemplate.update("DELETE FROM dbo.shipments WHERE id = ?", shipmentId);
        jdbcTemplate.update("DELETE FROM dbo.employee_profiles WHERE user_id = ?", senderId);
        jdbcTemplate.update("DELETE FROM dbo.users WHERE id IN (?, ?, ?)", senderId, driverId, ownerId);
        jdbcTemplate.update("DELETE FROM dbo.hubs WHERE id IN (?, ?)", originHubId, destinationHubId);
    }

    @Test void sqlServerEnforcesCustodyCountsCompetingClaimsAndSealedMembership() {
        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE dbo.handovers SET status = ?, accepted_containers = ? WHERE id = ?", "ACCEPTED", 0, handoverId)).isInstanceOf(DataIntegrityViolationException.class);
        jdbcTemplate.update("UPDATE dbo.handover_containers SET status = ?, scanned_at = SYSDATETIME() WHERE handover_id = ? AND container_id = ?", "SCANNED", handoverId, containerId);
        jdbcTemplate.update("UPDATE dbo.handovers SET status = ?, accepted_containers = ?, accepted_at = SYSDATETIME() WHERE id = ?", "ACCEPTED", 1, handoverId);
        assertThat(jdbcTemplate.queryForObject("SELECT accepted_containers FROM dbo.handovers WHERE id = ?", Integer.class, handoverId)).isEqualTo(1);
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM dbo.container_items WHERE container_id = ? AND shipment_id = ?", containerId, shipmentId)).isInstanceOf(DataIntegrityViolationException.class);
        Long competing = handover("PENDING_ACCEPTANCE", 1, 0);
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO dbo.handover_containers (handover_id, container_id, status) VALUES (?, ?, ?)", competing, containerId, "EXPECTED")).isInstanceOf(DataIntegrityViolationException.class);
        jdbcTemplate.update("UPDATE dbo.handovers SET status = ?, cancelled_at = SYSDATETIME() WHERE id = ?", "CANCELLED", handoverId);
        Long replacement = handover("PENDING_ACCEPTANCE", 1, 0);
        jdbcTemplate.update("UPDATE dbo.handovers SET replaces_handover_id = ? WHERE id = ?", handoverId, replacement);
        jdbcTemplate.update("UPDATE dbo.handovers SET replacement_handover_id = ? WHERE id = ?", replacement, handoverId);
        assertThat(jdbcTemplate.queryForObject("SELECT replaces_handover_id FROM dbo.handovers WHERE id = ?", Long.class, replacement)).isEqualTo(handoverId);
    }

    private Long hub(String code) { jdbcTemplate.update("INSERT INTO dbo.hubs (code, name, level, type, region, province, status) VALUES (?, ?, ?, ?, ?, ?, ?)", code, code, 1, "HUB", "SOUTH", "Can Tho", "ACTIVE"); return jdbcTemplate.queryForObject("SELECT id FROM dbo.hubs WHERE code = ?", Long.class, code); }
    private Long user(String username, String role) { jdbcTemplate.update("INSERT INTO dbo.users (username, password, full_name, phone, email, role, status) VALUES (?, ?, ?, ?, ?, ?, ?)", username, "hash", username, "0900000000", username + "@example.test", role, "ACTIVE"); return jdbcTemplate.queryForObject("SELECT id FROM dbo.users WHERE username = ?", Long.class, username); }
    private Long shipment() { ownerId = user("phase03-owner-" + suffix, "ROLE_USER"); jdbcTemplate.update("INSERT INTO dbo.shipments (tracking_number, sender_user_id, sender_name, sender_phone, sender_address, receiver_name, receiver_phone, receiver_address, pickup_method, fee_payer, inspection_policy, payment_status, current_status, current_hub_id, destination_region, destination_province, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", "P3-CUSTODY-" + suffix, ownerId, "Sender", "1", "A", "Receiver", "2", "B", "SELF_DROPOFF", "SENDER", "NO_INSPECTION", "UNPAID", "AT_HUB", originHubId, "SOUTH", "Can Tho", 0L); return jdbcTemplate.queryForObject("SELECT id FROM dbo.shipments WHERE tracking_number = ?", Long.class, "P3-CUSTODY-" + suffix); }
    private Long container(String code) { jdbcTemplate.update("INSERT INTO dbo.parcel_containers (container_code, origin_hub_id, destination_hub_id, status, created_at, version) VALUES (?, ?, ?, ?, SYSDATETIME(), ?)", code, originHubId, destinationHubId, "OPEN", 0L); return jdbcTemplate.queryForObject("SELECT id FROM dbo.parcel_containers WHERE container_code = ?", Long.class, code); }
    private Long handover(String status, int expected, int accepted) { jdbcTemplate.update("INSERT INTO dbo.handovers (from_hub_id, to_hub_id, sender_employee_id, receiver_driver_id, expected_containers, accepted_containers, status, created_at, version) VALUES (?, ?, (SELECT id FROM dbo.employee_profiles WHERE user_id = ?), ?, ?, ?, ?, SYSDATETIME(), ?)", originHubId, destinationHubId, senderId, driverId, expected, accepted, status, 0L); return jdbcTemplate.queryForObject("SELECT MAX(id) FROM dbo.handovers WHERE from_hub_id = ?", Long.class, originHubId); }
    static boolean sqlServerVerificationConfigured() { return has("UTEEXPRESS_SQLSERVER_URL") && has("UTEEXPRESS_SQLSERVER_USERNAME") && has("UTEEXPRESS_SQLSERVER_PASSWORD") && has("UTEEXPRESS_JWT_SECRET"); }
    private static boolean has(String name) { String value = System.getenv(name); return value != null && !value.isBlank(); }
}
