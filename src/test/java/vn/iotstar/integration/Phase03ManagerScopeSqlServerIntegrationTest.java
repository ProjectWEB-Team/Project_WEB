package vn.iotstar.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.repository.ManagerOperationsRepository;
import vn.iotstar.repository.OperationsAuditRepository;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Real SQL Server proof that the manager subtree CTE cannot include a sibling hub. */
@EnabledIf(value = "sqlServerVerificationConfigured", disabledReason = "REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("sqlserver-verification")
class Phase03ManagerScopeSqlServerIntegrationTest {
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ManagerOperationsRepository managerOperationsRepository;
    @Autowired private OperationsAuditRepository operationsAuditRepository;
    private String suffix;
    private Long rootId;
    private Long descendantId;
    private Long siblingId;

    @BeforeEach
    void applyMigrationAndSeedHierarchy() throws Exception {
        SqlServerMigrationSupport.apply(dataSource,
                "db/phase-01-shipment-events-event-type.sql",
                "db/phase-03-hub-routing-schema.sql");
        // Legacy databases may retain hubs.code at 20 characters; keep all generated test codes safely below it.
        suffix = Long.toUnsignedString(System.nanoTime(), 36);
        rootId = insertHub("P3M-R-" + suffix, null);
        descendantId = insertHub("P3M-D-" + suffix, rootId);
        siblingId = insertHub("P3M-S-" + suffix, null);
    }

    @AfterEach
    void cleanup() {
        if (suffix != null) jdbcTemplate.update("DELETE FROM dbo.hubs WHERE code LIKE ?", "P3M-%" + suffix);
    }

    @Test
    @Transactional(readOnly = true)
    void recursiveSqlServerScopeIncludesOnlyRootAndDescendants() {
        List<Long> scope = managerOperationsRepository.findHubSubtreeIds(rootId);
        assertThat(scope).contains(rootId, descendantId).doesNotContain(siblingId);
        assertThat(managerOperationsRepository.findAllActiveHubIds()).contains(rootId, descendantId, siblingId);
        assertThat(operationsAuditRepository.isHubWithinSubtree(rootId, descendantId)).isTrue();
        assertThat(operationsAuditRepository.isHubWithinSubtree(rootId, siblingId)).isFalse();
        assertThat(operationsAuditRepository.findShipmentWithCurrentHub(-1L)).isEmpty();
        assertThat(operationsAuditRepository.findEventsForShipment(-1L)).isEmpty();
        assertThat(operationsAuditRepository.findCustodyReferences(-1L)).isEmpty();
    }

    private Long insertHub(String code, Long parentHubId) {
        jdbcTemplate.update("INSERT INTO dbo.hubs (code, name, level, type, parent_hub_id, region, province, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                code, code, 1, "HUB", parentHubId, "SOUTH", "HCM", "ACTIVE");
        return jdbcTemplate.queryForObject("SELECT id FROM dbo.hubs WHERE code = ?", Long.class, code);
    }

    static boolean sqlServerVerificationConfigured() {
        return hasValue("UTEEXPRESS_SQLSERVER_URL") && hasValue("UTEEXPRESS_SQLSERVER_USERNAME")
                && hasValue("UTEEXPRESS_SQLSERVER_PASSWORD") && hasValue("UTEEXPRESS_JWT_SECRET");
    }
    private static boolean hasValue(String name) { String value = System.getenv(name); return value != null && !value.isBlank(); }
}
