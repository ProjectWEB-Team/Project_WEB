package vn.iotstar.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "UTEEXPRESS_CI_DATABASE", matches = "true")
class CiDatabaseMigrationTest {
    @Test void everyMigrationAppliesToAnEmptyDisposableDatabaseAndIsRepeatable() throws Exception {
        String url = System.getenv("UTEEXPRESS_SQLSERVER_URL");
        String username = System.getenv("UTEEXPRESS_SQLSERVER_USERNAME");
        String password = System.getenv("UTEEXPRESS_SQLSERVER_PASSWORD");
        try (var connection = DriverManager.getConnection(url, username, password)) {
            assertThat(SqlServerMigrationSupport.requireDisposableDatabase(connection)).startsWith("uteexpress_phase05_test_");
        }
        var flyway = Flyway.configure().dataSource(url, username, password).locations("classpath:db/migration").load();
        flyway.migrate();
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }
}
