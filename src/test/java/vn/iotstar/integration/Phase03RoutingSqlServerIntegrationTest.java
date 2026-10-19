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
import vn.iotstar.dto.hub.SortShipmentCommand;
import vn.iotstar.dto.hub.SortingDecisionResponse;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.RouteConfigurationException;
import vn.iotstar.service.HubOperationsService;

import javax.sql.DataSource;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Real SQL Server proof only; it is condition-skipped when no provisioned environment is supplied. */
@EnabledIf(value = "sqlServerVerificationConfigured", disabledReason = "REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("sqlserver-verification")
class Phase03RoutingSqlServerIntegrationTest {
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private HubOperationsService hubOperationsService;
    private String suffix;
    private Long actorId;
    private Long hubAId;
    private Long hubBId;
    private Long shipmentId;
    private Long unmatchedShipmentId;
    private Long concurrentShipmentId;

    @BeforeEach
    void applyMigrationAndSeed() throws Exception {
        SqlServerMigrationSupport.apply(dataSource,
                "db/phase-01-shipment-events-event-type.sql",
                "db/phase-03-hub-routing-schema.sql");
        suffix = String.valueOf(System.nanoTime());
        actorId = insertUser("phase03-staff-" + suffix, "ROLE_HUB_STAFF");
        hubAId = insertHub("P3-A-" + suffix, "Phase 03 Hub A", "Binh Duong");
        hubBId = insertHub("P3-B-" + suffix, "Phase 03 SOC B", "Long An");
        jdbcTemplate.update("INSERT INTO dbo.employee_profiles (user_id, employee_code, hub_id, position, status) VALUES (?, ?, ?, ?, ?)",
                actorId, "P3-" + suffix, hubAId, "SORTER", "ACTIVE");
        jdbcTemplate.update("INSERT INTO dbo.hub_routing_rules (current_hub_id, destination_region, destination_province, next_hub_id, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, SYSDATETIME(), SYSDATETIME())",
                hubAId, "SOUTH", "Can Tho", hubBId, "ACTIVE");
        shipmentId = insertShipment("P3-ROUTE-" + suffix, "SOUTH", "Can Tho");
        unmatchedShipmentId = insertShipment("P3-NOROUTE-" + suffix, "NORTH", "Ha Noi");
        concurrentShipmentId = insertShipment("P3-CONCURRENT-" + suffix, "SOUTH", "Can Tho");
        User actor = new User(actorId, "phase03-staff-" + suffix, "test-hash", UserRole.ROLE_HUB_STAFF, "ACTIVE");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null, actor.getAuthorities()));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        if (suffix == null) return;
        jdbcTemplate.update("DELETE FROM dbo.shipment_events WHERE shipment_id IN (?, ?, ?)", shipmentId, unmatchedShipmentId, concurrentShipmentId);
        jdbcTemplate.update("DELETE FROM dbo.shipment_routes WHERE shipment_id IN (?, ?, ?)", shipmentId, unmatchedShipmentId, concurrentShipmentId);
        jdbcTemplate.update("DELETE FROM dbo.shipments WHERE id IN (?, ?, ?)", shipmentId, unmatchedShipmentId, concurrentShipmentId);
        jdbcTemplate.update("DELETE FROM dbo.users WHERE username LIKE ?", "phase03-sender-P3-%" + suffix);
        jdbcTemplate.update("DELETE FROM dbo.hub_routing_rules WHERE current_hub_id = ?", hubAId);
        jdbcTemplate.update("DELETE FROM dbo.employee_profiles WHERE user_id = ?", actorId);
        jdbcTemplate.update("DELETE FROM dbo.hubs WHERE id IN (?, ?)", hubAId, hubBId);
        jdbcTemplate.update("DELETE FROM dbo.users WHERE id = ?", actorId);
    }

    @Test
    void persistsExactlyOneCurrentStaticHopAndRollsBackUnmatchedRoute() {
        SortingDecisionResponse first = hubOperationsService.sortAtHub(new SortShipmentCommand(shipmentId));
        SortingDecisionResponse repeat = hubOperationsService.sortAtHub(new SortShipmentCommand(shipmentId));

        assertThat(first.nextHubCode()).isEqualTo("P3-B-" + suffix);
        assertThat(repeat.reused()).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.shipment_routes WHERE shipment_id = ?", Integer.class, shipmentId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT hub_id FROM dbo.shipment_routes WHERE shipment_id = ?", Long.class, shipmentId)).isEqualTo(hubBId);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.shipment_events WHERE shipment_id = ? AND event_type = 'SORTING_COMPLETED'", Integer.class, shipmentId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.shipment_routes WHERE shipment_id = ? AND current_hub_id = ?", Integer.class, shipmentId, hubAId)).isEqualTo(1);
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO dbo.shipment_routes (shipment_id, hub_id, current_hub_id, sequence_number, status, route_decision_key, created_at) VALUES (?, ?, ?, ?, ?, ?, SYSDATETIME())",
                shipmentId, hubBId, hubAId, 1, "PENDING", "shipment:" + shipmentId + ":hub:" + hubAId))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(foreignKeyExists("FK_shipments_current_hub")).isTrue();
        assertThat(foreignKeyExists("FK_shipment_events_hub")).isTrue();
        assertThat(foreignKeyExists("FK_shipment_routes_current_hub")).isTrue();

        assertThatThrownBy(() -> hubOperationsService.sortAtHub(new SortShipmentCommand(unmatchedShipmentId)))
                .isInstanceOf(RouteConfigurationException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.shipment_routes WHERE shipment_id = ?", Integer.class, unmatchedShipmentId)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.shipment_events WHERE shipment_id = ?", Integer.class, unmatchedShipmentId)).isZero();
    }

    @Test
    void concurrentSorterRequestsConvergeOnOneRouteAndOneEvent() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try {
            Callable<SortingDecisionResponse> sort = () -> {
                barrier.await(10, TimeUnit.SECONDS);
                User actor = new User(actorId, "phase03-staff-" + suffix, "test-hash", UserRole.ROLE_HUB_STAFF, "ACTIVE");
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null, actor.getAuthorities()));
                try {
                    return hubOperationsService.sortAtHub(new SortShipmentCommand(concurrentShipmentId));
                } finally {
                    SecurityContextHolder.clearContext();
                }
            };
            Future<SortingDecisionResponse> first = executor.submit(sort);
            Future<SortingDecisionResponse> second = executor.submit(sort);

            assertThat(first.get(20, TimeUnit.SECONDS).nextHubCode()).isEqualTo("P3-B-" + suffix);
            assertThat(second.get(20, TimeUnit.SECONDS).nextHubCode()).isEqualTo("P3-B-" + suffix);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.shipment_routes WHERE shipment_id = ?", Integer.class, concurrentShipmentId)).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.shipment_events WHERE shipment_id = ? AND event_type = 'SORTING_COMPLETED'", Integer.class, concurrentShipmentId)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean foreignKeyExists(String name) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys.foreign_keys WHERE name = ?", Integer.class, name);
        return count != null && count == 1;
    }

    private Long insertUser(String username, String role) {
        jdbcTemplate.update("INSERT INTO dbo.users (username, password, full_name, phone, email, role, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                username, "test-hash", "Phase Three Staff", uniquePhone(username), username + "@example.test", role, "ACTIVE");
        return jdbcTemplate.queryForObject("SELECT id FROM dbo.users WHERE username = ?", Long.class, username);
    }

    private String uniquePhone(String username) {
        return String.format("09%08d", Math.floorMod(username.hashCode(), 100_000_000));
    }

    private Long insertHub(String code, String name, String province) {
        jdbcTemplate.update("INSERT INTO dbo.hubs (code, name, level, type, region, province, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                code, name, 1, "DELIVERY_HUB", "SOUTH", province, "ACTIVE");
        return jdbcTemplate.queryForObject("SELECT id FROM dbo.hubs WHERE code = ?", Long.class, code);
    }

    private Long insertShipment(String trackingNumber, String region, String province) {
        Long senderId = insertUser("phase03-sender-" + trackingNumber, "ROLE_USER");
        jdbcTemplate.update("INSERT INTO dbo.shipments (tracking_number, sender_user_id, sender_name, sender_phone, sender_address, receiver_name, receiver_phone, receiver_address, pickup_method, fee_payer, inspection_policy, payment_status, current_status, current_hub_id, destination_region, destination_province, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                trackingNumber, senderId, "Sender", "0900000001", "Address", "Receiver", "0900000002", "Address", "SELF_DROPOFF", "SENDER", "NO_INSPECTION", "UNPAID", "AT_HUB", hubAId, region, province, 0L);
        return jdbcTemplate.queryForObject("SELECT id FROM dbo.shipments WHERE tracking_number = ?", Long.class, trackingNumber);
    }

    static boolean sqlServerVerificationConfigured() {
        return hasValue("UTEEXPRESS_SQLSERVER_URL") && hasValue("UTEEXPRESS_SQLSERVER_USERNAME")
                && hasValue("UTEEXPRESS_SQLSERVER_PASSWORD") && hasValue("UTEEXPRESS_JWT_SECRET");
    }

    private static boolean hasValue(String name) {
        String value = System.getenv(name);
        return value != null && !value.isBlank();
    }
}
