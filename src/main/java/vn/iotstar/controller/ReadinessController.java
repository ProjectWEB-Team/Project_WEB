package vn.iotstar.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/** Minimal deployment probe: a reachable database and the artifact's build identity, without connection details. */
@RestController
public class ReadinessController {
    private final JdbcTemplate jdbc;
    private final String commit;
    public ReadinessController(JdbcTemplate jdbc, ObjectProvider<BuildProperties> builds) {
        this.jdbc = jdbc;
        BuildProperties build = builds.getIfAvailable();
        this.commit = build == null || build.get("commit") == null ? "local" : build.get("commit");
    }
    @GetMapping("/health/ready")
    public ResponseEntity<Map<String,String>> readiness() {
        try {
            if (!Integer.valueOf(1).equals(jdbc.queryForObject("SELECT 1", Integer.class))) throw new IllegalStateException();
            return ResponseEntity.ok(Map.of("status", "UP", "commit", commit));
        } catch (RuntimeException failure) {
            return ResponseEntity.status(503).body(Map.of("status", "DOWN"));
        }
    }
}
