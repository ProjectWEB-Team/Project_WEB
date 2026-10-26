package vn.iotstar.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Real SQL Server schema gate for linehaul transitions; skipped only when required environment is absent. */
@EnabledIf(value = "sqlServerVerificationConfigured", disabledReason = "REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("sqlserver-verification")
class Phase03TripSqlServerIntegrationTest {
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Test void realSqlServerAppliesLinehaulTripAndIncidentSchema() throws Exception {
        SqlServerMigrationSupport.apply(dataSource,
                "db/migration/V2__phase_01_shipment_events_event_type.sql",
                "db/migration/V6__phase_03_hub_routing_schema.sql",
                "db/migration/V7__phase_03_container_handover_schema.sql",
                "db/migration/V8__phase_03_linehaul_trip_schema.sql");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys.tables WHERE name IN ('transport_trips', 'incidents')", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.incidents') " +
                "AND name IN ('shipment_id', 'trip_id', 'hub_id', 'type', 'description', 'reported_by', 'status', 'created_at', 'resolved_at')", Integer.class)).isEqualTo(9);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.incidents') " +
                "AND name IN ('transport_trip_id', 'actor_user_id', 'incident_type', 'note', 'evidence_reference')", Integer.class)).isZero();
    }
    static boolean sqlServerVerificationConfigured() { return has("UTEEXPRESS_SQLSERVER_URL") && has("UTEEXPRESS_SQLSERVER_USERNAME") && has("UTEEXPRESS_SQLSERVER_PASSWORD") && has("UTEEXPRESS_JWT_SECRET"); }
    private static boolean has(String name) { String value = System.getenv(name); return value != null && !value.isBlank(); }
}
