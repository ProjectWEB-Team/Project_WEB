# Phase 04: Last-mile & Finance - Pattern Map

**Mapped:** 2026-10-13  
**Authority used:** `ARCHITECTURE_DECISIONS.md` > `UTEExpress_Workflow.md` > `ERD.md` > `PROJECT_SPEC_KNOWLEDGE_BASE.md` > `ENGINEERING_STANDARDS.md` > supplementary engineering knowledge  
**Current-state rule:** codebase patterns below are implementation patterns only. They do not override the Phase-04 canonical domain decisions in `04-CONTEXT.md`.

## File Classification

| New/modified file group | Role | Data flow | Closest tracked analog | Match quality |
|---|---|---|---|---|
| `entity/DeliveryAttempt*`, delivery extensions to assignment/shipment | model | CRUD / event-driven | `entity/Shipment.java`, `entity/ShipmentEvent.java` | role-match |
| `entity/Wallet*`, `WalletTransaction*`, `CodTransaction*`, `Payment*` finance extensions | model | transactional CRUD | `entity/Shipment.java` | role-match |
| `entity/ShipperReconciliation*`, `Incident*`, `WithdrawalRequest*` | model | transactional CRUD | `entity/ShipmentEvent.java` | partial |
| Phase-04 repositories for attempts, wallet, ledger, COD, reconciliation, withdrawal, incident | repository | CRUD / locking query | `repository/ShipmentEventRepository.java` | role-match |
| `service/LastMileService` + implementation | service | state transition / event-driven | `service/ShipmentService.java`, `service/impl/ShipmentServiceImpl.java` | role-match |
| `service/FinanceService`/`WalletService`/`ReconciliationService`/`WithdrawalService` + implementations | service | atomic financial mutation | `service/impl/ShipmentServiceImpl.java` | partial |
| Phase-04 request/response DTOs | DTO | request-response | `dto/shipment/CreateShipmentRequest.java`, `ShipmentDetailResponse.java` | exact |
| shipper/manager/admin/wallet browser controllers | controller | request-response | `controller/ShipmentController.java` | role-match |
| Phase-04 JSP workspaces and forms | view | request-response | `views/shipment/create.jsp`, `views/shipment/detail.jsp` | role-match |
| Phase-04 private action notification DTO/publisher extension | realtime component | pub-sub | `realtime/StompShipmentEventPublisher.java` | role-match |
| security route additions and data-scope policy seam | config / authorization | request-response | `config/SecurityConfig.java`, `security/AuthenticatedActor.java` | partial |
| SQL Server Phase-04 migration / reconciliation script | migration | DDL / data integrity | `resources/db/phase-01-shipment-events-event-type.sql` and `docs/schema.sql` | partial |
| service, MVC/security, STOMP, and real-SQL-Server Phase-04 tests | test | unit / request-response / integration | `ShipmentServiceTest`, `ShipmentRouteSecurityTest`, `StompShipmentEventPublisherTest`, `ShipmentSqlServerIntegrationTest` | role-match |

## Pattern Assignments

### Phase-04 entities and enum types (model, transactional CRUD)

**Tracked analogs:**

- `src/main/java/vn/iotstar/entity/Shipment.java` (lines 20-71)
- `src/main/java/vn/iotstar/entity/ShipmentEvent.java` (lines 18-40)

**Copy the mapping conventions:** SQL Server `IDENTITY` ids, explicit `@Table`/`@Column` names, `NVARCHAR` strings, `DATETIME2(0)` timestamps, `EnumType.STRING`, `LAZY` `@ManyToOne`, and private fields with controlled factory/mutation methods. `Shipment` uses the exact existing project style:

```java
@Entity
@Table(name = "shipments")
public class Shipment implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false,
            length = 50, columnDefinition = "NVARCHAR(50)")
    private ShipmentStatus currentStatus;
}
```

**Apply to:** `Wallet`, `WalletTransaction`, `CodTransaction`, `DeliveryAttempt`, `ShipperReconciliation`, `ShipperReconciliationItem`, `WithdrawalRequest`, and Phase-04 financial incident mapping (or extensions to the Phase-03 incident entity). Add `evidence_url` as a nullable delivery-attempt reference column as locked by D-05; do not store an image blob.

**Do not copy the Phase-01 shipment factory defaults.** `SELF_DROPOFF`, `SENDER`, `NO_INSPECTION`, and `UNPAID` are explicitly Phase-01 tracer defaults, not Phase-04 financial/domain rules.

**Canonical model constraints:** `wallets.user_id` is unique and wallets own `balance` plus `locked_balance` (ADR-001). COD, reconciliation, and withdrawal statuses are persisted finance states, not additions to the fixed `ShipmentStatus` enum. Shipment operational history continues through `shipment_events.event_type` (ADR-003/014).

### Phase-04 repositories (repository, CRUD / locking query)

**Tracked analog:** `src/main/java/vn/iotstar/repository/ShipmentEventRepository.java` (lines 1-11).

```java
public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, Long> {
    List<ShipmentEvent> findByShipmentIdOrderByCreatedAtAsc(Long shipmentId);
}
```

**Apply to:** query methods that express only persistence needs (for example assigned active delivery work, attempts ordered by time, COD records for a reconciliation period, and owner withdrawal requests). Repositories must not decide retry eligibility, role/hub/assignment scope, money movement, or payout transitions.

**No exact analog for SQL Server concurrency locking.** Use a repository query/locking mechanism compatible with JPA and SQL Server, but let an `@Transactional` service orchestrate it. Planner must require a test that proves duplicate/concurrent financial requests do not produce duplicate ledger entries or a negative available balance.

### Last-mile, finance, reconciliation, and withdrawal services (service, transactional state transition)

**Tracked analog:** `src/main/java/vn/iotstar/service/impl/ShipmentServiceImpl.java` (lines 22-64).

```java
@Override
@Transactional
public Long create(CreateShipmentRequest request) {
    AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
    User sender = userRepository.getReferenceById(actor.userId());
    Shipment savedShipment = shipmentRepository.save(shipment);
    ShipmentEvent savedEvent = shipmentEventRepository.save(
            ShipmentEvent.shipmentCreated(savedShipment, sender));
    shipmentEventPublisher.publishAfterCommit(actor, event);
    return savedShipment.getId();
}

@Override
@Transactional(readOnly = true)
public ShipmentDetailResponse getDetail(Long shipmentId) {
    AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
    Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment"));
    if (!shipment.getSender().getId().equals(actor.userId())) {
        throw new ShipmentAccessDeniedException();
    }
    // DTO mapping occurs before returning to the controller.
}
```

**Apply to every Phase-04 command:** derive the actor server-side first, load only records needed for that actor's scope, validate workflow preconditions, mutate the aggregate(s), persist the operational event and/or finance record, then schedule a safe private notification after commit.

**Service-specific required adaptations:**

- Delivery service checks `ROLE_SHIPPER` + assignment + applicable hub + current shipment state before a scan, collection, attempt, or successful delivery. It creates attempts and events atomically; `RESCHEDULED` requires a date; group-1 enforces at most one failed attempt/day and at most three/three-days; group-2 fast-fails to return. Never create a new shipment status for attempt/sorting detail.
- Finance service records money due before changing the shipment to `DELIVERED`; it owns COD lifecycle transitions and return-fee liability checks.
- Wallet/reconciliation/withdrawal services use one write transaction to check the permitted state/reference, update balance/locked balance, persist the finance state plus exactly one ledger entry, and reject replays. Locking/idempotency belongs here, never in JSP or controller code.
- Manager reconciliation resolves every item independently: matched COD can proceed to payout; a shortage/overage item stays `HOLD_DISPUTE` and creates the canonical financial incident. Do not introduce `shipper_debts`.
- Admin-only withdrawal review approves by consuming `locked_balance`, or rejects with a nonblank reason and restores available balance. No external bank integration belongs in this phase.

### Contextual authorization (security seam, request-response)

**Tracked analog:** `src/main/java/vn/iotstar/security/AuthenticatedActor.java` (lines 10-18).

```java
public static AuthenticatedActor requireCurrent() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof User user)) {
        throw new AccessDeniedException("Authenticated UTEExpress user required");
    }
    return new AuthenticatedActor(user.getId(), user.getUsername(), Set.of(user.getRole()));
}
```

**Apply to:** all service commands and scoped reads. Request DTOs must never include trusted `userId`, shipper id, manager id, hub id, balance, or owner-id fields. The service combines actor role with assignment, task direction, resource owner, hub/position information, and current shipment location as the workflow requires. UI visibility is not authorization.

### Browser controllers and JSP workspace routes (controller/view, request-response)

**Tracked controller analog:** `src/main/java/vn/iotstar/controller/ShipmentController.java` (lines 15-45).

```java
@Controller
@RequestMapping("/user/shipments")
public class ShipmentController {
    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute CreateShipmentRequest request,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) return "shipment/create";
        Long shipmentId = shipmentService.create(request);
        return "redirect:/user/shipments/" + shipmentId;
    }
}
```

**Tracked JSP analogs:**

- `src/main/webapp/WEB-INF/views/shipment/create.jsp` (lines 16-32) uses `<form:form>`, Spring form binding, `<form:errors>`, and Bootstrap.
- `src/main/webapp/WEB-INF/views/shipment/detail.jsp` (lines 16-27) uses `<c:out>` and `<c:forEach>` for safe display.

```jsp
<form:form method="post" action="${pageContext.request.contextPath}/user/shipments"
           modelAttribute="createShipmentRequest" class="card shadow-sm">
    <form:input id="senderName" path="senderName" cssClass="form-control" htmlEscape="true"/>
    <form:errors path="senderName" cssClass="text-danger small"/>
</form:form>
```

**Apply to:** dedicated shipper delivery/return-task pages, owner wallet and withdrawal pages, manager reconciliation pages, and admin withdrawal review pages. Use request/response DTOs and PRG redirects. JSPs render only already-authorized DTO data; they do not calculate collection, fee liability, retry eligibility, reconciliation differences, or scope.

### Session/JWT route security and CSRF (config, request-response)

**Tracked analog:** `src/main/java/vn/iotstar/config/SecurityConfig.java`.

```java
http.securityMatcher("/api/**")
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/shipper/**").hasAnyRole("SHIPPER", "ADMIN")
                .anyRequest().authenticated());

http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .csrf(Customizer.withDefaults())
        .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                .requestMatchers("/ws", "/ws/**").authenticated()
                .requestMatchers("/shipper/**").hasAnyRole("ADMIN", "SHIPPER")
                .requestMatchers("/manager/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated());
```

**Apply to:** narrowly add actual Phase-04 browser/API routes only if the plan adds new route families. Preserve session+CSRF for JSP POSTs and stateless JWT for `/api/**`; do not make `/ws/**` public, weaken the existing API chain, or rely on route authorization instead of service data scope.

### Exception translation (exception, request-response)

**Tracked analogs:**

- `src/main/java/vn/iotstar/exception/WebExceptionHandler.java` (lines 13-38) returns safe 400/403/404/500 JSPs.
- `src/main/java/vn/iotstar/exception/ApiExceptionHandler.java` (lines 15-44) maps validation, access, not-found, and unexpected errors to generic JSON bodies.

```java
@ExceptionHandler(ShipmentAccessDeniedException.class)
@ResponseStatus(HttpStatus.FORBIDDEN)
public String forbidden(ShipmentAccessDeniedException exception) {
    return "error/403";
}

@ExceptionHandler(Exception.class)
public ResponseEntity<ApiErrorResponse> unexpected(Exception exception) {
    return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
}
```

**Apply to:** explicit business exceptions such as invalid delivery transition, duplicate financial action, insufficient balance, and unauthorised task/resource access. Map them to user-safe web/API results and never reveal financial internals, account details, evidence storage paths, or stack traces.

### Private STOMP notifications (component, pub-sub)

**Tracked analogs:**

- `src/main/java/vn/iotstar/realtime/StompShipmentEventPublisher.java` (lines 19-32)
- `src/main/java/vn/iotstar/config/WebSocketConfig.java` (lines 21-60)

```java
if (!TransactionSynchronizationManager.isSynchronizationActive()) {
    throw new IllegalStateException("Shipment realtime publication requires an active transaction");
}
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override
    public void afterCommit() {
        messagingTemplate.convertAndSendToUser(actor.username(),
                USER_SHIPMENT_DESTINATION, event);
    }
});
```

```java
registry.enableSimpleBroker("/queue");
registry.setUserDestinationPrefix("/user");
registry.addEndpoint("/ws");
```

**Apply to:** private action-required assignment/task updates for the assignee, wallet-owner payout/withdrawal outcomes, and manager/admin review work. Create minimal notification DTOs with no address, phone, COD amount, bank account, or broad finance detail. Preserve authenticated private user destinations and permit only authorized subscriptions.

### SQL Server migrations and real-environment tests (migration/test, DDL / integration)

**Tracked config:** `src/main/resources/application.properties` (lines 13-16, 29, 32) binds only environment-driven SQL Server and has `spring.jpa.hibernate.ddl-auto=none`.

```properties
spring.datasource.url=${UTEEXPRESS_SQLSERVER_URL}
spring.datasource.username=${UTEEXPRESS_SQLSERVER_USERNAME}
spring.datasource.password=${UTEEXPRESS_SQLSERVER_PASSWORD}
spring.jpa.hibernate.ddl-auto=none
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServerDialect
```

**Tracked test analog:** `src/test/java/vn/iotstar/integration/ShipmentSqlServerIntegrationTest.java` (lines 18-94) is an `@SpringBootTest(webEnvironment = MOCK)` that is conditionally enabled only when real SQL Server variables exist; it uses `JdbcTemplate` to prove persistence, uniqueness, foreign-key behaviour, and cleanup.

**Apply to:** a dedicated, idempotent SQL Server migration/reconciliation script and a conditionally enabled real-SQL-Server Phase-04 integration test. The migration must reconcile the old `shipment_events.status` schema definition to canonical `event_type` if needed and add nullable `delivery_attempts.evidence_url`; it must not use H2/MySQL/PostgreSQL. Test must be reported `BLOCKED_BY_ENVIRONMENT` when variables are absent, never PASS from a skip.

### Tests (unit, MVC/security, realtime, SQL Server integration)

**Tracked analogs:**

- `src/test/java/vn/iotstar/service/ShipmentServiceTest.java` (lines 52-140): Mockito unit tests authenticate a real `User` principal, capture persisted entities, assert events, and verify authorization denies before event read.
- `src/test/java/vn/iotstar/controller/ShipmentRouteSecurityTest.java` (lines 37-84): filter-enabled `@WebMvcTest`, `@WithMockUser`, and `.with(csrf())` assert real JSP session/CSRF boundaries.
- `src/test/java/vn/iotstar/realtime/StompShipmentEventPublisherTest.java` (lines 41-69): verifies no send before commit, exact private destination/payload, and rejection outside a transaction.

**Required Phase-04 additions:** tests for assignment/hub/resource denial before sensitive detail returns; every canonical delivery failure group and retry limit; collect-before-deliver; return fee gate and both return-completion modes; exact COD lifecycle; item-level reconciliation split; wallet lazy creation; duplicate/concurrent finance rejection; withdrawal lock/approve/reject balance and ledger transitions; and safe private-STOMP recipient/payload. Keep regression coverage for Phase-01 security, 13 statuses, and `event_type`.

## Shared Patterns

### Layering and constructor injection

**Source:** `ShipmentController.java` lines 15-45 and `ShipmentServiceImpl.java` lines 22-64.  
**Apply to:** every Phase-04 controller/service pair.

- Controller accepts DTO + validation, calls one service boundary, and chooses a JSP/redirect/HTTP response.
- Service interface and `*Impl` implementation own business transitions, authorization, and `@Transactional` scope.
- Repository is injected only into services. No controller-to-repository calls or field injection.

### Server-derived actor and data scope

**Source:** `AuthenticatedActor.java` lines 10-18.  
**Apply to:** all mutations and scoped reads, including STOMP recipient determination. Never trust client-provided actor, assignee, user, hub, manager, or balance data.

### Event versus shipment-state boundary

**Source:** `Shipment.java` lines 67-68 and `ShipmentEvent.java` lines 34-40.  
**Apply to:** delivery scan, assignment, attempted delivery, collection, return, reconciliation, payout, and withdrawal audit. Preserve exactly the canonical 13 statuses; append operation events with `event_type`.

### Safe mutation then post-commit realtime

**Source:** `ShipmentServiceImpl.java` lines 41-52 and `StompShipmentEventPublisher.java` lines 22-32.  
**Apply to:** notifications only after all related database mutations commit. A rollback cannot emit a success/action event.

## Current-Code Drift / Planning Hazards

1. **Phase-04 model absent:** the source currently contains only Phase-01 `User`, `Shipment`, and `ShipmentEvent` entities/repositories. `wallets`, COD, delivery attempts, assignments, reconciliation, withdrawal, and incidents are logical canonical model requirements but have no current Java mapping. This is implementation work, not permission to redesign their schema.
2. **Parallel-slice seam unavailable in current source:** Phase-02 assignment/payment and Phase-03 hub/employee/incident models are not implemented in this checkout. Phase-04 plans must state their assumed canonical contracts and avoid replacement/duplicate entity designs; Phase 05 performs full cross-slice integration.
3. **Schema conflict to repair deliberately:** `docs/schema.sql` still shows `shipment_events.status`, while ADR-003/Workflow and the actual Phase-01 mapping use `shipment_events.event_type`. Any Phase-04 migration must preserve the higher-authority `event_type` contract rather than resurrect `status`.
4. **D-05 schema gap:** canonical Workflow's delivery-attempt field list has no evidence column, but the Phase-04 approved context requires nullable `evidence_url`/attachment reference. Add it through an explicit SQL-Server-compatible migration; do not store image binary.
5. **Current `Shipment` is only a tracer:** it has no public accessors/mapping for financial, current-hub, receiver-link, assignment, or operational fields Phase 04 consumes. Extend through canonical Phase-02/03 contracts rather than using reflection, raw controller SQL, or client supplied data.
6. **Existing STOMP guard is intentionally narrow:** it currently permits only `/user/queue/shipments` subscription and blocks client sends. Phase-04 should extend it only for narrowly scoped private destinations and only with authorization tests; it must not replace it with a broad broadcast.
7. **Physical schema policy:** `ddl-auto=none` means Phase-04 persistence cannot rely on Hibernate auto-DDL. SQL Server migration/reconciliation and real-environment verification are required.

## No Exact Analog Found

| File/function area | Role | Why no exact current analog | Planning direction |
|---|---|---|---|
| atomic wallet balance/locked-balance update with pessimistic/optimistic guard | service/repository | no financial aggregate exists | use service transaction pattern above; select an SQL-Server-compatible lock/version approach and prove concurrency/idempotency |
| lifecycle-rich COD/reconciliation/withdrawal services | service | current service only creates/reads a Phase-01 shipment | preserve interface/implementation, actor, DTO, and transaction patterns while following Workflow sections 60, 66-70, 73, 95-97.1 |
| delivery retry and fast-fail policy | service | no delivery/task model exists | implement in service using canonical attempts/dates/reasons; do not extend `ShipmentStatus` |
| finance JSP workspaces | view | only shipment create/detail JSPs exist | use Bootstrap/Spring form/JSTL safe-output conventions; render scoped DTOs only |
| evidence attachment storage adapter | component | no upload/storage adapter exists | store a nullable reference only; resolve storage mechanics as supplementary detail without raw SQL image blobs |

## Metadata

**Analog search scope:** `src/main/java`, `src/main/webapp`, `src/test/java`, `src/main/resources`, canonical `docs/`, and Phase-01/02 planning context.  
**Tracked analog gate:** verified every named source analog with `git ls-files`.  
**Files classified:** 12 file groups.  
**Pattern extraction date:** 2026-10-13.
