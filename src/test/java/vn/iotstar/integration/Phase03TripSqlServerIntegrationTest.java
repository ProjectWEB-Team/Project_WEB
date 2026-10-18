package vn.iotstar.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/** Real SQL Server schema gate for linehaul transitions; skipped only when required environment is absent. */
@EnabledIf(value = "sqlServerVerificationConfigured", disabledReason = "REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("sqlserver-verification")
class Phase03TripSqlServerIntegrationTest {
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Test void realSqlServerAppliesLinehaulTripAndIncidentSchema() throws Exception {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try { ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("db/phase-03-linehaul-trip-schema.sql")), false, false, ScriptUtils.DEFAULT_COMMENT_PREFIX, "GO", ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER, ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER); }
        finally { DataSourceUtils.releaseConnection(connection, dataSource); }
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys.tables WHERE name IN ('transport_trips', 'incidents')", Integer.class)).isEqualTo(2);
    }
    static boolean sqlServerVerificationConfigured() { return has("UTEEXPRESS_SQLSERVER_URL") && has("UTEEXPRESS_SQLSERVER_USERNAME") && has("UTEEXPRESS_SQLSERVER_PASSWORD") && has("UTEEXPRESS_JWT_SECRET"); }
    private static boolean has(String name) { String value = System.getenv(name); return value != null && !value.isBlank(); }
}
