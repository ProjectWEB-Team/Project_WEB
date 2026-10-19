package vn.iotstar.integration;

import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

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

    private static String read(String classpathScript) throws IOException {
        ClassPathResource resource = new ClassPathResource(classpathScript);
        try (InputStream input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
