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
                "db/phase-01-shipment-events-event-type.sql",
                "db/phase-03-hub-routing-schema.sql",
                "db/phase-03-container-handover-schema.sql",
                "db/phase-03-linehaul-trip-schema.sql");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys.tables WHERE name IN ('transport_trips', 'incidents')", Integer.class)).isEqualTo(2);
    }
    static boolean sqlServerVerificationConfigured() { return has("UTEEXPRESS_SQLSERVER_URL") && has("UTEEXPRESS_SQLSERVER_USERNAME") && has("UTEEXPRESS_SQLSERVER_PASSWORD") && has("UTEEXPRESS_JWT_SECRET"); }
    private static boolean has(String name) { String value = System.getenv(name); return value != null && !value.isBlank(); }
}
