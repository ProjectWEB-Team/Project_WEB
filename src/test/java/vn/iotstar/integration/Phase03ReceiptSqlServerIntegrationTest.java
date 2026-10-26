package vn.iotstar.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Real SQL Server-only proof that receipt provenance columns and allowed incident types are installed. */
@EnabledIf(value = "sqlServerVerificationConfigured", disabledReason = "REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("sqlserver-verification")
class Phase03ReceiptSqlServerIntegrationTest {
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test void realSqlServerInstallsDestinationReceiptEvidenceSchema() throws Exception {
        SqlServerMigrationSupport.apply(dataSource,
                "db/migration/V2__phase_01_shipment_events_event_type.sql",
                "db/migration/V6__phase_03_hub_routing_schema.sql",
                "db/migration/V7__phase_03_container_handover_schema.sql",
                "db/migration/V8__phase_03_linehaul_trip_schema.sql",
                "db/migration/V9__phase_03_destination_receipt_schema.sql");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.container_items') AND name IN ('received_at', 'received_by_user_id')", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.incidents') " +
                "AND name IN ('shipment_id', 'trip_id', 'hub_id', 'type', 'reported_by', 'status')", Integer.class)).isEqualTo(6);
    }

    static boolean sqlServerVerificationConfigured() { return has("UTEEXPRESS_SQLSERVER_URL") && has("UTEEXPRESS_SQLSERVER_USERNAME") && has("UTEEXPRESS_SQLSERVER_PASSWORD") && has("UTEEXPRESS_JWT_SECRET"); }
    private static boolean has(String name) { String value = System.getenv(name); return value != null && !value.isBlank(); }
}
