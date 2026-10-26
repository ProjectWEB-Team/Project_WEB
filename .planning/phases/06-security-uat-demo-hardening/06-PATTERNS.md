# Phase 06: Security + UAT + Demo Hardening - Pattern Map

**Mapped:** 2026-10-26
**Files analyzed:** 9 (new/modified groups)
**Analogs found:** 8 / 9 (all analog paths are git-tracked under src/ and docs/)

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `src/test/java/vn/iotstar/controller/*SecurityTest.java` (matrix gap-fill, new/extended per controller) | test | request-response | `controller/HubRoutingControllerSecurityTest.java` | exact |
| `src/test/java/vn/iotstar/security/PermissionMatrix*Test.java` (service-layer contextual deny) | test | request-response | `integration/Phase05ManagerScopeSqlServerIntegrationTest.java` + `Phase05SqlServerFixtureSupport` | role-match |
| `src/test/java/vn/iotstar/config/WebSocketConfigTest.java` (extend: all destinations, notification dest, CONNECT/SEND) | test | event-driven | same file | exact |
| `src/test/java/vn/iotstar/realtime/StompMultiSession*Test.java` (new) | test | pub-sub | `realtime/StompShipmentEventPublisherTest.java`, `OperationalRealtimePublisherTest.java` | exact |
| `src/main/java/vn/iotstar/config/LogisticsProperties.java` (add `Demo`/flags) | config | transform | itself (`Delivery.attemptDayLock`, `Timeout`) | exact |
| `src/main/resources/application-demo.properties` (add 2 flags) | config | config | itself | exact |
| `service/impl/HubOperationsServiceImpl.java` (bypass hook in `resolveSingleStaticRule`/`blocked`) | service | CRUD | `LastMileDeliveryServiceImpl` line ~344 (flag read) | role-match |
| `service/impl/LinehaulTripServiceImpl.java` (auto transit simulation) | service | CRUD | itself `startTrip`/`arriveTrip` | exact |
| `docs/PHASE_06_DEMO_SCRIPT.md`, `docs/PHASE_06_UAT_RESULTS.md` (new) | docs | n/a | `docs/PHASE_05_MANUAL_TEST_GUIDE.md`, `docs/PHASE_05_VERIFICATION.md` | exact |
| Flag-inert tests (`DemoFlagsTest`) | test | transform | `service/LinehaulTripServiceImplTest`, `service/HubRoutingServiceImplTest` | role-match |

## Pattern Assignments

### Controller/JSP/API security tests (test, request-response)
**Analog:** `src/test/java/vn/iotstar/controller/HubRoutingControllerSecurityTest.java` (lines 30-84). Per matrix cell: anonymous (redirect to login), wrong role (403), right role (200), POST without CSRF (403, `verify(service, never())`), POST with `csrf()` (PRG redirect).
```java
@WebMvcTest(HubOperationsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class HubRoutingControllerSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private HubOperationsService hubOperationsService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    @Test void anonymousUserCannotOpenHubWorkspace() throws Exception {
        mockMvc.perform(get("/hub/sorter")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login")); }
    @Test @WithMockUser(roles = "HUB_STAFF")
    void sorterPostWithoutCsrfIsRejectedBeforeService() throws Exception {
        mockMvc.perform(post("/hub/sorter").param("shipmentId", "101")).andExpect(status().isForbidden());
        verify(hubOperationsService, never()).sortAtHub(any()); }
```
Other existing analogs (extend, do not duplicate; 19 `*SecurityTest` files in `controller/`, plus `security/SecurityConfigTest` for the `/api/**` JWT chain with no CSRF, `security/GoogleOAuthRouteSecurityTest`). API chain pattern: `@WebMvcTest(AuthApiController.class) @Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})` (`SecurityConfigTest` lines 33-45), real `JwtService` token via `jwtService.issueToken(user)`.
Matrix tip: one parametrized test per controller (`@ParameterizedTest` over role x route) is shorter than N copy-pasted methods.

### Service-layer contextual deny (hub scope, position, assignment, location, ownership)
**Analog:** `integration/Phase05ManagerScopeSqlServerIntegrationTest.java` lines 9-28, extending `Phase05SqlServerFixtureSupport`, gated by:
```java
@org.junit.jupiter.api.condition.EnabledIf(value="vn.iotstar.integration.Phase05SqlServerFixtureSupport#configured", disabledReason="REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
...
actor("S"); assertThatThrownBy(()->receipts.missingCase(incident)).isInstanceOf(RuntimeException.class);
```
Use the dedicated verification DB only (D-06). Unit-level denial analog (Mockito, no DB): `service/LinehaulTripServiceImplTest`. Denial exceptions to assert: `HubOperationAccessDeniedException`, `ShipmentAccessDeniedException`. Service denial checks to cover, from `LinehaulTripServiceImpl` lines 177-194: `trip.isAssignedTo(actor.userId())`, `actor.hasRole(ROLE_LINEHAUL_DRIVER)`, `"LINEHAUL_DRIVER".equals(profile.getPosition())`, `handover.acceptsDriver(...)`, `sameHub(...)`.

### STOMP interceptor matrix (test, event-driven)
**Analog:** `src/test/java/vn/iotstar/config/WebSocketConfigTest.java` (whole file, 55 lines). Package-private `AuthenticatedShipmentStompInterceptor` is reachable only from package `vn.iotstar.config`, so put interceptor tests there.
```java
private final WebSocketConfig.AuthenticatedShipmentStompInterceptor interceptor =
        new WebSocketConfig.AuthenticatedShipmentStompInterceptor();
private Message<byte[]> message(StompCommand command, String destination) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
    accessor.setUser(new UsernamePasswordAuthenticationToken("owner", null));
    if (destination != null) accessor.setDestination(destination);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
}
assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.SEND, "/app/shipments"), null))
        .isInstanceOf(AccessDeniedException.class);
```
Source under test (`WebSocketConfig.java` 46-68): rules are (a) CONNECT/SUBSCRIBE/SEND need `accessor.getUser()`, (b) SEND always denied, (c) SUBSCRIBE only to `USER_SHIPMENT_DESTINATION`, `USER_OPERATION_DESTINATION`, `USER_NOTIFICATION_DESTINATION`. Existing test misses: notification destination allow, authenticated CONNECT allow, unauthenticated SUBSCRIBE/SEND deny, `/queue/*` and `/topic/*` non-user destinations deny. Handshake: `config/WebSocketEndpointSecurityTest` (anonymous -> login redirect; `@WithMockUser(roles="USER")` -> 400).

### Multi-session STOMP delivery (test, pub-sub) - D-04
**Analog:** `realtime/StompShipmentEventPublisherTest.java` (lines 20-60, 70-82). Mockito `SimpMessagingTemplate`, manual `TransactionSynchronizationManager.initSynchronization()`, fire `afterCommit` by hand, capture with `ArgumentCaptor`, assert per-user recipient via `convertAndSendToUser(eq("owner"), eq(DEST), payload)`.
```java
@ExtendWith(MockitoExtension.class)
class StompShipmentEventPublisherTest {
    @Mock private SimpMessagingTemplate messagingTemplate;
    @BeforeEach void start() { publisher = new StompShipmentEventPublisher(messagingTemplate);
        TransactionSynchronizationManager.initSynchronization(); }
    @AfterEach void clear() { if (TransactionSynchronizationManager.isSynchronizationActive())
        TransactionSynchronizationManager.clearSynchronization(); }
    ...
    publisher.publishAfterCommit(actor, event);
    verifyNoInteractions(messagingTemplate);                       // nothing before commit / on rollback
    TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    verify(messagingTemplate).convertAndSendToUser(eq("owner"), eq(StompShipmentEventPublisher.USER_SHIPMENT_DESTINATION), payloadCaptor.capture());
    assertThat(ShipmentCreatedEventResponse.class.getRecordComponents()).extracting(c -> c.getName()).containsExactly(...); // minimal payload
```
Lazy harness: simulate N sessions as N users against one mocked template and assert `verify(template, never()).convertAndSendToUser(eq("unrelated"), any(), any())`; for rollback, clear synchronization without calling `afterCommit` and `verifyNoInteractions`. Also reuse `OperationalRealtimePublisherTest` (recipient filtering: assigned driver, both-end managers, inactive users) and `StompNotificationEventPublisherTest` as the extend-not-duplicate baseline. No real broker/WebSocket client needed (D-07).

### `LogisticsProperties.java` + `application-demo.properties` (config)
**Analog:** `config/LogisticsProperties.java` lines 15-53; bind under existing `uteexpress` prefix (D-09), nested static class with safe defaults:
```java
@ConfigurationProperties(prefix = "uteexpress") @Validated
public class LogisticsProperties {
    private final Delivery delivery = new Delivery();
    ...
    public static class Delivery {
        /** Whether at most one failed attempt is accepted per calendar day. */
        private boolean attemptDayLock = true;
        public boolean isAttemptDayLock() { return attemptDayLock; }
        public void setAttemptDayLock(boolean v) { this.attemptDayLock = v; }
    }
```
Add e.g. `Demo { boolean fastTrackRouteBypass=false; boolean autoLinehaulTransitSimulation=false; }` -> keys `uteexpress.demo.fast-track-route-bypass`, `uteexpress.demo.auto-linehaul-transit-simulation`. Defaults false = safe in prod. Registered via `@EnableConfigurationProperties({JwtProperties.class, LogisticsProperties.class})` in `SecurityConfig.java:33`, so no new registration. To also accept Workflow `app.demo.*`, add a Spring relaxed alias only if cheap; otherwise document the mapping (D-09).
`application-demo.properties` style (Vietnamese comment line + key, lines 19-20):
```properties
# Cho phep thu giao lai nhieu luot trong cung ngay de demo kich ban nhanh
uteexpress.delivery.attempt-day-lock=false
```
Append the two flags `=true` in same style. Gotcha: existing keys `uteexpress.timeout.pickup-*`/`dropoff-*` are read via `@Value` (`IntakeTimeoutServiceImpl.java` 32-35: `@Value("${uteexpress.timeout.pickup-hours:48}") long pickupHours`), not via `LogisticsProperties.Timeout`. Both styles work; prefer `LogisticsProperties` injection for the new flags (as `LastMileDeliveryServiceImpl` does: field `private final LogisticsProperties logisticsProperties;` constructor-injected; use at line 344 `if (logisticsProperties.getDelivery().isAttemptDayLock()) {...}`).

### Demo flag hooks (service, CRUD)
- **fast-track-route-bypass:** `HubOperationsServiceImpl.resolveSingleStaticRule` (lines ~139-154) returns `blocked(...)` (creates routing exception, `ROUTE_LOOKUP_BLOCKED` event, throws `RouteConfigurationException`) when rules are missing/ambiguous. Hook = flag check before `blocked`, only when flag on and `selected.size() != 1` fall back per Workflow §101; keep the strict path untouched when off. Needs `LogisticsProperties` constructor injection (follow `LastMileDeliveryServiceImpl` ctor). Do not skip event writes (`ShipmentEvent.sortingCompleted`).
- **auto-linehaul-transit-simulation:** `LinehaulTripServiceImpl.startTrip` (lines ~100-137: `trip.start(driver)`, `handover.depart()`, status `IN_TRANSIT`, `ShipmentEvent.linehaulTripStarted`) and `arriveTrip` (139-156: `trip.arrive(...)`, `appendShipmentEvents(handover, driver, "ARRIVE")`, publishes `OperationalActionNotification("DESTINATION_ARRIVAL"|"INBOUND_READY_CONTAINER", ...)`). Simulation should call the same `arriveTrip` path (same transitions, events, notifications) after start instead of adding a parallel state path, to keep the 13 statuses/events intact. Keep `@Transactional` and `AuthenticatedActor.requireCurrent()` guards.
- **Inert-outside-demo tests:** copy `service/LinehaulTripServiceImplTest` and `service/HubRoutingServiceImplTest` setup; build `new LogisticsProperties()` (defaults) and assert strict behavior (`RouteConfigurationException` thrown, no auto-arrive); then set flag true and assert bypass. Optionally `ApplicationContextRunner` with `application-demo.properties` to show the profile only flips the flags.

### Demo script + UAT result template (docs)
**Analogs:** `docs/PHASE_05_MANUAL_TEST_GUIDE.md` (Vietnamese; numbered sections: 1 prep code/DB/accounts, data prerequisites table `| Thanh phan | Dieu kien |`, role table, per-step checklists; states it is a checklist, not UAT result) and `docs/PHASE_05_VERIFICATION.md` lines 89-104 (status heading `NOT_RUN`, table `| Step | Actor/action | Expected observation to record |`, "Record actor IDs/roles, timestamp, URL/action, server-derived resource reference, observed state"). Copy: run with `--spring.profiles.active=demo,local`; primary scenario Workflow §106, exception §107; add columns `Result (PASS/FAIL/NOT_RUN) | Observed | Evidence` defaulting to `NOT_RUN` (D-05: never mark unobserved as passed). Also reference the dedicated verification DB rule from `docs/PHASE_05_SQL_RESULTS.md`.

## Shared Patterns

### Contextual authorization (ADR-015)
**Source:** `service/impl/LinehaulTripServiceImpl.java` 177-194, `AuthenticatedActor.requireCurrent()`; denial = `HubOperationAccessDeniedException`/`ShipmentAccessDeniedException`. **Apply to:** every matrix negative test (assert exception type + `verify(repo, never()).save(...)` when mockable). No `@PreAuthorize` (D-02).

### Test annotations
`@WebMvcTest(X.class) @Import({SecurityConfig.class, JwtAuthenticationFilter.class})` + `@MockBean JwtService`, `UserDetailsService`; `@WithMockUser(roles="...")` (no `ROLE_` prefix); `csrf()` from `SecurityMockMvcRequestPostProcessors`. **Apply to:** all controller security tests.

### Real SQL gating
`@EnabledIf("vn.iotstar.integration.Phase05SqlServerFixtureSupport#configured")` and extend `Phase05SqlServerFixtureSupport`. **Apply to:** any new financial/data-scope SQL case (wallet `balance`/`locked_balance`, ledger once-only).

### Safe feature flag
Config bean with default-safe boolean, flipped only in `application-demo.properties`. **Apply to:** both demo flags.

## No Analog Found

| File | Role | Reason |
|---|---|---|
| Real multi-client STOMP session test (actual WebSocket client) | test | No embedded-broker/`WebSocketStompClient` test exists; recommended approach is the Mockito per-user template pattern above, which has precedent |
| Permission matrix file (e.g. `06-PERMISSION-MATRIX.md`) | docs | No prior matrix artifact; use `PHASE_05_VERIFICATION.md` table style |

## Metadata

**Analog search scope:** `src/main/java/vn/iotstar/{config,service/impl}`, `src/test/java/vn/iotstar/{controller,config,realtime,security,integration}`, `src/main/resources`, `docs/`
**Files scanned:** ~30 (spot reads; `.planning/codebase/*` ignored as stale)
**Pattern extraction date:** 2026-10-26
