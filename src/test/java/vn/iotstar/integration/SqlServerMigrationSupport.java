package vn.iotstar.integration;

import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/** Executes SQL Server resource batches separated by standalone GO lines. */
final class SqlServerMigrationSupport {
    private SqlServerMigrationSupport() {
    }

    static void apply(DataSource dataSource, String... classpathScripts) throws SQLException, IOException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String classpathScript : classpathScripts) {
                String script = read(classpathScript);
                String[] batches = script.split("(?im)^\\s*GO\\s*$");
                for (int index = 0; index < batches.length; index++) {
                    String batch = batches[index];
                    String sql = batch.strip();
                    if (!sql.isEmpty()) {
                        try {
                            statement.execute(sql);
                        } catch (SQLException exception) {
                            throw new SQLException("Migration failed in " + classpathScript
                                    + " batch " + (index + 1), exception);
                        }
                    }
                }
            }
        }
    }

    static List<String> phase05Manifest() throws IOException {
        return read("db/phase-05-migration-manifest.txt").lines().map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#")).toList();
    }

    static void applyPhase05(DataSource source) throws SQLException, IOException {
        apply(source, phase05Manifest().toArray(String[]::new));
    }

    /** Never infer disposal permission from a URL substring or execute CREATE DATABASE/USE. */
    static String requireDisposableDatabase(Connection connection) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery("SELECT DB_NAME()")) {
            if (!result.next()) throw new SQLException("Cannot determine database identity");
            String database = result.getString(1);
            if (database == null || !database.matches("(?i)uteexpress_phase05_test_[a-z0-9_]+"))
                throw new SQLException("Refusing setup: dedicated database name must be uteexpress_phase05_test_<suffix>");
            return database;
        }
    }

    /** Bootstrap ONLY an empty disposable database. Existing schemas are never dropped or partially rebuilt. */
    static void bootstrapCanonicalIfEmpty(DataSource source) throws SQLException, IOException {
        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            requireDisposableDatabase(connection);
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM sys.tables WHERE is_ms_shipped = 0")) {
                result.next();
                if (result.getInt(1) != 0) return;
            }
            String schema = canonicalDefinitions(Files.readString(Path.of("docs/schema.sql")));
            connection.setAutoCommit(false);
            try {
                for (String batch : schema.split("(?im)^\\s*GO\\s*$"))
                    if (!batch.isBlank()) statement.execute(batch);
                connection.commit();
            } catch (SQLException failure) {
                connection.rollback();
                throw failure;
            }
        }
    }

    static String canonicalDefinitions(String schema) {
        int tables = schema.indexOf("CREATE TABLE dbo.users");
        if (tables < 0) throw new IllegalArgumentException("Canonical users table boundary is missing");
        String definitions = schema.substring(tables);
        if (Pattern.compile("(?im)^\\s*(USE\\s|CREATE\\s+DATABASE\\b|DROP\\s)").matcher(definitions).find())
            throw new IllegalArgumentException("Unsafe database-level statement in canonical definitions");
        return definitions;
    }

    private static String read(String classpathScript) throws IOException {
        ClassPathResource resource = new ClassPathResource(classpathScript);
        try (InputStream input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
