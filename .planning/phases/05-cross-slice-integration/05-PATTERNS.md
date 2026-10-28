# Phase 05: Cross-slice Integration - Pattern Map

**Mapped:** 2026-10-25  
**Files analyzed:** 8 proposed Phase-05 artifacts  
**Analogs found:** 8 / 8

## File Classification

| New/modified file | Role | Data flow | Closest tracked analog | Match quality |
|---|---|---|---|---|
| `src/test/java/vn/iotstar/integration/Phase05SqlServerFixtureSupport.java` | test utility | batch / SQL fixture | `src/test/java/vn/iotstar/integration/SqlServerMigrationSupport.java` | exact utility role |
| `src/test/java/vn/iotstar/integration/Phase05NormalJourneySqlServerIntegrationTest.java` | integration test | request-response / persisted workflow | `src/test/java/vn/iotstar/integration/Phase04FinanceSqlServerIntegrationTest.java` | strong end-state SQL assertion match |
| `src/test/java/vn/iotstar/integration/Phase05CustodyReturnSqlServerIntegrationTest.java` | integration test | custody / batch | `src/test/java/vn/iotstar/integration/Phase03CustodySqlServerIntegrationTest.java` | exact SQL Server guard/cleanup match |
| `src/test/java/vn/iotstar/integration/Phase05FinanceExceptionSqlServerIntegrationTest.java` | integration test | finance transaction | `src/test/java/vn/iotstar/integration/Phase04FinanceSqlServerIntegrationTest.java` | exact finance fixture/ledger match |
| `src/test/java/vn/iotstar/service/Phase05ManagementScopeContractTest.java` | service test | request-response / scoped aggregate | `src/test/java/vn/iotstar/service/ManagerOperationsServiceImplTest.java` | exact server-derived scope match |
| `src/test/java/vn/iotstar/realtime/Phase05RealtimeContractTest.java` | realtime test | pub-sub | `src/test/java/vn/iotstar/realtime/OperationalRealtimePublisherTest.java` | exact after-commit safe-payload match |
| `src/test/java/vn/iotstar/config/Phase05WebSocketScopeTest.java` | security/config test | session/STOMP handshake | `src/test/java/vn/iotstar/config/WebSocketEndpointSecurityTest.java` | exact endpoint security match |
| `docs/PHASE_05_UAT.md` | manual UAT evidence | request-response / pub-sub | `docs/PHASE_04_HANDOFF.md` | documentation handoff match |

## Pattern Assignments

### SQL Server migration and fixture support

**Analog:** `src/test/java/vn/iotstar/integration/SqlServerMigrationSupport.java` (lines 14-35)

```java
static void apply(DataSource dataSource, String... classpathScripts) throws SQLException, IOException {
    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
        for (String classpathScript : classpathScripts) {
            String[] batches = read(classpathScript).split("(?im)^\\s*GO\\s*$");
            for (String batch : batches) {
                if (!batch.strip().isEmpty()) statement.execute(batch.strip());
            }
        }
    }
}
```

**Apply to:** `Phase05SqlServerFixtureSupport` and all Phase-05 real SQL Server tests.

- Use an explicit forward-only ordered classpath script list; `ddl-auto=none` means tests must never rely on Hibernate schema creation.
- Seed unique values from one per-test suffix and remove child rows before parents. Keep cleanup in `@AfterEach`, mirroring `Phase03CustodySqlServerIntegrationTest` lines 35-48 and `Phase04FinanceSqlServerIntegrationTest` lines 89-121.
- Preserve the condition gate pattern below; a disabled test means `BLOCKED_BY_ENVIRONMENT`, not successful verification.

### Real SQL Server guard, seed, and persisted assertions

**Primary analog:** `src/test/java/vn/iotstar/integration/Phase03CustodySqlServerIntegrationTest.java`

```java
@EnabledIf(value = "sqlServerVerificationConfigured",
        disabledReason = "REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("sqlserver-verification")
class Phase03CustodySqlServerIntegrationTest {
    @BeforeEach void seed() throws Exception { /* migration then unique SQL fixture */ }
    @AfterEach void cleanup() { /* dependent records before parent records */ }
    static boolean sqlServerVerificationConfigured() {
        return has("UTEEXPRESS_SQLSERVER_URL")
            && has("UTEEXPRESS_SQLSERVER_USERNAME")
            && has("UTEEXPRESS_SQLSERVER_PASSWORD")
            && has("UTEEXPRESS_JWT_SECRET");
    }
}
```

**Secondary analog:** `src/test/java/vn/iotstar/integration/Phase04FinanceSqlServerIntegrationTest.java` (lines 31-184)

```java
ShipperReconciliation rec = reconciliationService.reconcile(request);
assertThat(rec.getStatus()).isEqualTo("COMPLETED");
reconciliationService.payoutReadyCod(codTxId);
assertThat(jdbcTemplate.queryForObject(
        "SELECT balance FROM dbo.wallets WHERE user_id = ?", BigDecimal.class, senderUserId))
    .isEqualByComparingTo(new BigDecimal("300000.00"));
```

**Apply to:** normal journey, custody/return, and finance-exception journey tests.

- Prefer public service calls for canonical business actions; use `JdbcTemplate` only for fixture setup, cleanup, and independent persisted SQL Server assertions.
- Assert each handoff's canonical `current_status`, `shipment_events.event_type`, `current_hub_id`/custody fact, and applicable payment/COD/wallet ledger fact.
- Duplicate-command and rejected-precondition checks must assert no additional persisted event/custody/ledger row, not merely an exception response.

### Scoped management aggregation

**Service analog:** `src/main/java/vn/iotstar/service/impl/ManagerOperationsServiceImpl.java` (lines 20-50)

```java
AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
Scope scope = resolveScope(actor);
List<Long> hubIds = scope.hubIds();
return new ManagerOperationsDashboardResponse(
        scope.label(), scope.systemWide(),
        managerOperationsRepository.countInboundShipments(hubIds),
        managerOperationsRepository.countActionableIncidents(hubIds));
```

**Test analog:** `src/test/java/vn/iotstar/service/ManagerOperationsServiceImplTest.java` (lines 28-61)

```java
authenticate(user(71L, UserRole.ROLE_MANAGER));
when(managerOperationsRepository.findHubSubtreeIds(11L)).thenReturn(List.of(11L, 12L));
service.getDashboard();
verify(managerOperationsRepository).countActiveTrips(List.of(11L, 12L));
```

**Controller boundary analog:** `src/main/java/vn/iotstar/controller/ManagerOperationsController.java` (lines 12-23) and `src/test/java/vn/iotstar/controller/ManagerOperationsControllerSecurityTest.java` (lines 24-34).

- Manager/Admin aggregation must stay in a `@Transactional(readOnly = true)` service method using `AuthenticatedActor.requireCurrent()`.
- Tests must prove scope filtering and denied non-Manager/non-Admin access before a DTO is returned; no repository access belongs in a controller.

### Private STOMP safe-summary publication

**Publisher analog:** `src/main/java/vn/iotstar/realtime/OperationalRealtimePublisher.java` (lines 20-67)

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void publishAfterCommit(OperationalActionNotification notification) {
    if (!ALLOWED_TYPES.contains(notification.type())) return;
    OperationalActionEventResponse payload =
            new OperationalActionEventResponse(notification.type(), notification.diagnosticReference());
    recipients(notification).forEach(user ->
            messagingTemplate.convertAndSendToUser(user.getUsername(), USER_OPERATION_DESTINATION, payload));
}
```

**Test analog:** `src/test/java/vn/iotstar/realtime/OperationalRealtimePublisherTest.java` (lines 20-52).

- Retain an allow-listed type, private user destination, after-commit listener, and DTO containing only action type/reference (or an equally safe summary).
- A Phase-05 test may extend an existing allowed event only with matching destination/scope tests; it must not introduce a broadcast destination or client `SEND` path.

### STOMP/session boundary

**Config analog:** `src/main/java/vn/iotstar/config/WebSocketConfig.java` (lines 18-58)

```java
if (accessor.getUser() == null) {
    throw new AccessDeniedException("Authenticated STOMP connection required");
}
if (command == StompCommand.SUBSCRIBE
        && !USER_SHIPMENT_DESTINATION.equals(accessor.getDestination())
        && !USER_OPERATION_DESTINATION.equals(accessor.getDestination())
        && !USER_NOTIFICATION_DESTINATION.equals(accessor.getDestination())) {
    throw new AccessDeniedException("STOMP subscription is outside the current actor scope");
}
```

**Test analog:** `src/test/java/vn/iotstar/config/WebSocketEndpointSecurityTest.java` (lines 21-35).

- Keep `/ws` on session authentication; do not alter the stateless JWT `/api/**` chain or disable CSRF.
- Browser UAT uses authenticated session login, then an allowed private subscription; unauthenticated handshake redirects to login and arbitrary destinations are denied.

## Shared Patterns

### Server-derived identity and data scope

**Source:** `src/main/java/vn/iotstar/service/impl/ManagerOperationsServiceImpl.java`

Use `AuthenticatedActor.requireCurrent()` in every cross-slice service test/execution path. Do not seed a client-controlled owner/hub identity as a substitute for the actor; fixtures establish facts, services establish authorization.

### Transaction and persistence proof

**Source:** `src/test/java/vn/iotstar/integration/Phase04FinanceSqlServerIntegrationTest.java`

Service calls express workflow behavior. `JdbcTemplate` independently reads SQL Server rows for status, event type, custody, unique references, wallet balance, and ledger cardinality. This preserves the layered boundary while giving direct database evidence.

### Security regression scope

**Source:** `src/test/java/vn/iotstar/config/WebSocketEndpointSecurityTest.java` and `src/test/java/vn/iotstar/controller/ManagerOperationsControllerSecurityTest.java`

Route tests establish login/role boundaries; service tests establish contextual hub/assignment scope. Both are required because neither one can replace the other.

## No Analog Found

| Proposed artifact | Reason |
|---|---|
| Unified Phase-05 cross-slice fixture builder | No existing fixture composes all Phase 01-04 scripts; compose `SqlServerMigrationSupport` with the cleanup patterns above. |
| Full normal journey SQL Server test | Existing integration tests are slice-focused; compose existing service operations and persisted assertions rather than copying a synthetic new workflow. |
| Cross-slice Manager/Admin projection | Existing `ManagerOperationsServiceImpl` is operations-only; reuse its actor/scope pattern and introduce only a minimum DTO/service projection if existing scoped services cannot provide the required work items. |

## Metadata

**Analog search scope:** `src/main/java/vn/iotstar`, `src/test/java/vn/iotstar`, `src/main/resources/db`, `docs`  
**Tracked analogs verified:** 7 source paths via `git ls-files`  
**Pattern extraction date:** 2026-10-25
