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
                "db/phase-01-shipment-events-event-type.sql",
                "db/phase-03-hub-routing-schema.sql",
                "db/phase-03-container-handover-schema.sql",
                "db/phase-03-linehaul-trip-schema.sql",
                "db/phase-03-destination-receipt-schema.sql");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.container_items') AND name IN ('received_at', 'received_by_user_id')", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.incidents') " +
                "AND name IN ('shipment_id', 'trip_id', 'hub_id', 'type', 'reported_by', 'status')", Integer.class)).isEqualTo(6);
    }

    static boolean sqlServerVerificationConfigured() { return has("UTEEXPRESS_SQLSERVER_URL") && has("UTEEXPRESS_SQLSERVER_USERNAME") && has("UTEEXPRESS_SQLSERVER_PASSWORD") && has("UTEEXPRESS_JWT_SECRET"); }
    private static boolean has(String name) { String value = System.getenv(name); return value != null && !value.isBlank(); }
}
