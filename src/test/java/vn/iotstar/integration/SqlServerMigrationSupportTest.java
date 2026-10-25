package vn.iotstar.integration;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SqlServerMigrationSupportTest {
    @Test void canonicalExtractionRetainsConstraintsAndNeverRedirectsTheConnection() throws Exception {
        String sql = SqlServerMigrationSupport.canonicalDefinitions(Files.readString(Path.of("docs/schema.sql")));
        assertThat(sql).startsWith("CREATE TABLE dbo.users").contains("FOREIGN KEY", "UNIQUE", "CREATE OR ALTER TRIGGER")
                .doesNotContain("CREATE DATABASE", "USE [ute_express]");
        assertThatThrownBy(() -> SqlServerMigrationSupport.canonicalDefinitions("CREATE TABLE dbo.users ();\nUSE other;"))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void wrongDatabaseIsRejectedUsingServerIdentityBeforeAnyMutation() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet result = mock(ResultSet.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT DB_NAME()")).thenReturn(result);
        when(result.next()).thenReturn(true);
        when(result.getString(1)).thenReturn("ute_express", "uteexpress_phase05_test_local");
        assertThatThrownBy(() -> SqlServerMigrationSupport.requireDisposableDatabase(connection)).isInstanceOf(SQLException.class);
        assertThat(SqlServerMigrationSupport.requireDisposableDatabase(connection)).isEqualTo("uteexpress_phase05_test_local");
        verify(statement, never()).execute(anyString());
    }
    @Test void orderedManifestResolvesEveryResourceWithoutDemoSeed() throws Exception {
        var scripts = SqlServerMigrationSupport.phase05Manifest();
        assertThat(scripts).hasSize(16).doesNotHaveDuplicates();
        assertThat(scripts.indexOf("db/phase-05-route-key-alignment.sql"))
                .isGreaterThan(scripts.indexOf("db/phase-04-return-schema.sql"))
                .isLessThan(scripts.indexOf("db/phase-05-custody-alignment.sql"));
        assertThat(scripts.getFirst()).contains("phase-01-shipment-events");
        for (String script : scripts) {
            assertThat(new org.springframework.core.io.ClassPathResource(script).exists()).as(script).isTrue();
            assertThat(script).doesNotContain("pricing-seed");
        }
    }
}
