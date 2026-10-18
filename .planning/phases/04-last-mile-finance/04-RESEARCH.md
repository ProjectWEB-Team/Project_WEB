# Phase 04: Last-mile & Finance - Research

**Researched:** 2026-10-13  
**Domain:** Last-mile delivery, returns, COD, reconciliation, wallet ledger, withdrawal, scoped JSP, private STOMP, and SQL Server verification  
**Confidence:** HIGH for canonical requirements and current-code observations; MEDIUM for implementation choices delegated to this phase

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Delivery failure uses the Workflow's two reason groups. Every failed attempt creates a `delivery_attempt` with reason and evidence; `NO_ANSWER`, `PAYMENT_NOT_AVAILABLE`, `RESCHEDULED`, and `OTHER` follow the 3-attempts/3-days rule, while `RECEIVER_REFUSED` and `WRONG_ADDRESS` fast-fail into `RETURN_TO_SENDER`. Do not add a ShipmentStatus for sorting, attempts, or failure detail; retain the fixed 13 states and append operational events. — **Reversibility:** one-way — changing persisted reason/state semantics requires migration and breaks canonical Workflow tracking contracts.
- **D-02:** An assigned last-mile shipper must scan each shipment assigned to that shipper at the applicable delivery hub before it can enter `OUT_FOR_DELIVERY`. Role alone is insufficient: Service authorization combines role, hub, assignment, resource, and current state.
- **D-03:** If COD and/or receiver-paid shipping fee is due, the Service must record valid collection before `CONFIRM DELIVERY` changes the shipment to `DELIVERED`; the collection becomes an input to the shipper's reconciliation. — **Reversibility:** costly — reversing ordering permits an already-delivered shipment without a traceable financial obligation.
- **D-04:** Support both canonical return completion modes: sender collection at the origin hub after return-fee handling, and coordinator-assigned `direction=RETURN` home delivery. Both retain scope checks, events/audit, fee conditions, and only reach `RETURNED_TO_SENDER` when valid. — **Reversibility:** costly — omitting either Workflow path breaks the documented return contract.
- **D-05:** The Phase-04 schema includes a nullable `evidence_url`/attachment reference on `delivery_attempts` to meet the Workflow requirement for a failed-delivery verification photo. Persist a reference/metadata, not raw image binary in SQL Server; use a configurable implementation detail for storage. This fills a field-level canonical-source gap without changing business rules.
- **D-06:** COD follows the canonical lifecycle `WAITING_COLLECTION → COLLECTED → RECONCILED → READY_FOR_PAYOUT → PAID_TO_SENDER`; use `HOLD_DISPUTE` for a discrepant item and `CANCELLED` where a return starts before collection. COD state is financial data, not a new ShipmentStatus. — **Reversibility:** one-way — this is a persisted lifecycle and payout contract.
- **D-07:** Enforce canonical liability: paid outbound shipping fee is not refunded after a shipment is in transit; sender pays return fee; when a receiver was to pay but refuses, sender owes the outstanding shipping fee plus return fee. Keep the shipment in `RETURN_TO_SENDER` and block handover until payment/approved exception conditions are met.
- **D-08:** Refund and compensation credit `wallets.balance` and append an atomic `wallet_transactions` ledger entry with `REFUND` or `COMPENSATION` and its canonical reference. Do not represent financial balance only on `shipments` or only as an event. — **Reversibility:** one-way — ledger/balance consistency is a canonical financial integrity contract.
- **D-09:** Every financial action is idempotent at the Service transaction boundary: validate permitted current state/reference, update balance or financial state, and append ledger/record atomically with appropriate concurrency protection. JSP button disabling is UX only, not financial protection.
- **D-10:** Reconciliation discrepancy is resolved item by item. `MATCHED` items proceed to payout; each `DISCREPANCY` item is held in `HOLD_DISPUTE` and produces a `MONEY_SHORTAGE` or `MONEY_OVERAGE` financial incident. No `shipper_debts` table is permitted. — **Reversibility:** one-way — ADR-006 explicitly fixes the discrepancy model.
- **D-11:** A withdrawal request validates available balance and atomically moves `amount` from `wallets.balance` to `wallets.locked_balance`, creates `PENDING`, and appends `WITHDRAWAL_LOCK`. Insufficient available balance fails without a request. — **Reversibility:** one-way — removing the lock violates the anti-double-spending contract.
- **D-12:** Only `ROLE_ADMIN` reviews withdrawals. Approval decreases `locked_balance`, marks the request `COMPLETED`, and appends `WITHDRAWAL_DEDUCT`; rejection requires `rejection_reason`, restores `balance`, marks `REJECTED`, and appends `WITHDRAWAL_REFUND`. Demo approval does not call a real bank.
- **D-13:** Create a wallet lazily when a user first becomes eligible for a canonical financial action (for example COD payout, refund, compensation, or wallet payment). Enforce exactly one wallet per user; do not create wallets for every role indiscriminately.
- **D-14:** Provide dedicated JSP workspaces by operational responsibility. A shipper sees only assigned delivery/return tasks at the applicable hub; Manager sees only scoped hub/shipper work; Admin has only canonical financial review scope. Service-level policies enforce authorization; JSP is presentation only.
- **D-15:** STOMP delivers only private, action-required events to scoped recipients: assignment/task updates, wallet-owner payout/withdrawal status, and manager/admin review work. Do not broadcast shipment address, COD, or financial details to all staff. — **Reversibility:** costly — broad destinations can create unrecoverable privacy leakage.
- **D-16:** Apply least-privilege personal-data display. An assigned shipper receives only contact/address data needed for the active task; access ends when assignment/scope ends. Other lists, manager/admin screens, and realtime payloads expose only data required for their authorized operation.

### the agent's Discretion

- Choose concrete JSP layouts, DTO/service/repository names, attachment storage adapter, event destination names, validation messages, and safe notification payload shapes, provided they preserve the locked finance, privacy, role, and data-scope decisions.
- Choose SQL-Server-compatible locking/JPA implementation and focused automated/manual verification methods. The implementation must prove correctness under duplicate/concurrent financial action attempts without replacing SQL Server or bypassing the Service boundary.

### Deferred Ideas (OUT OF SCOPE)

- Live bank payout, Open Banking, and live VNPay/refund integration — V2 only.
- End-to-end orchestration across the already-planned intake and operations implementations — Phase 05.
- System-wide security/UAT/demo completion work — Phase 06.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|---|---|---|
| LAST-01 | Assigned last-mile shipper records only authorized delivery work and valid outcomes. | Contextual service checks, delivery-attempt lifecycle, filter-enabled route tests, and event audit. |
| LAST-02 | Retry/return workflow and ownership-scoped returned-shipment handling. | Two failure groups, retry limits, return modes, fee gate, and custody requirements. |
| FIN-01 | Shipping-fee, COD, refund, return-fee, and compensation records match canonical states. | Separate payment/COD sources, ledger references, and ordered financial transitions. |
| FIN-02 | Separate wallets with balance/locked balance, ledger, and anti-double-spending. | Unique wallet ownership, one transaction boundary, idempotency and concurrency tests. |
| FIN-03 | Scoped reconciliation, item-level incidents, and no `shipper_debts`. | Source-specific reconciliation items, `MONEY_SHORTAGE`/`MONEY_OVERAGE`, SQL proof. |
| FIN-04 | Owner withdrawal and Admin approval/rejection with correct balance transitions. | Owner/Admin service scope, pending-state locks, ledger snapshots, and race tests. |
</phase_requirements>

## Summary

Phase 04 is a Person-C vertical slice, not a finance-only data layer. It must carry each permitted operation through JSP/JSTL + Bootstrap, controller, DTO, service, repository/JPA, SQL Server, service-level contextual authorization, private STOMP where action is required, and tests. [VERIFIED: docs/TEAM_OWNERSHIP.md:5-11] The canonical decision hierarchy fixes the wallet model, six roles, event/state distinction, SQL Server, JSP/JSTL frontend, constructor injection, and contextual authorization; implementation must extend rather than redesign them. [VERIFIED: docs/ARCHITECTURE_DECISIONS.md:1-151]

**Primary recommendation:** Execute the existing ten plans in wave order, using service-owned transactions with an SQL-Server-compatible row-lock or optimistic-version strategy plus database unique constraints for every money/custody replay boundary. The strategy choice is discretionary, but the transaction, idempotency, and real-SQL verification obligations are locked. [VERIFIED: .planning/phases/04-last-mile-finance/04-CONTEXT.md:28-36]

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|---|---|---|---|
| Delivery scan, collection, attempt, retry, and return command | API / Backend | Browser / Client | Service resolves actor, assignment, hub, shipment state, and financial obligation; JSP only presents a validated action. [VERIFIED: docs/ARCHITECTURE_DECISIONS.md:146-151] |
| Wallet balance/locked-balance and ledger mutation | API / Backend | Database / Storage | One service transaction owns state validation, balance mutation, ledger write, rollback, and replay protection. [VERIFIED: .planning/phases/04-last-mile-finance/04-CONTEXT.md:28-36] |
| Reconciliation and discrepancy resolution | API / Backend | Database / Storage | Manager scope, source eligibility, item outcome, COD lifecycle, and incident creation are business rules, not repository or JSP concerns. [VERIFIED: docs/UTEExpress_Workflow.md:1799-1870] |
| Scoped workspaces | Frontend Server (SSR) | API / Backend | JSP renders already-authorized DTO projections; the service remains the authorization authority. [VERIFIED: .planning/phases/04-last-mile-finance/04-CONTEXT.md:41-42] |
| Private post-commit action alerts | API / Backend | Browser / Client | Services emit only legal actions; the publisher sends minimal DTOs to server-derived private recipients after commit. [VERIFIED: src/main/java/vn/iotstar/realtime/StompShipmentEventPublisher.java:12-34] |
| Phase evidence and migration | Database / Storage | API / Backend | SQL Server is canonical and `ddl-auto=none`, so explicit migration and a real SQL Server test are required. [VERIFIED: src/main/resources/application.properties:13-32] |

## Project Constraints (from AGENTS.md)

- Prefix shell commands with `rtk`. [VERIFIED: C:/Users/Admin/.codex/RTK.md:1-20]

## Standard Stack

### Core

| Library / Platform | Version in current project | Purpose | Direction |
|---|---:|---|---|
| Spring Boot | `3.3.4` | MVC, security, JPA, transaction runtime, and test context | Reuse; do not replace. [VERIFIED: pom.xml:9-14] |
| Spring Data JPA | Spring Boot managed | Entity/repository persistence | Keep repositories as persistence boundaries only. [VERIFIED: pom.xml:28-31] |
| Microsoft SQL Server JDBC | Spring Boot managed runtime dependency | Canonical database connection | SQL Server only; no alternate database test substitute. [VERIFIED: pom.xml:75-79] |
| JSP/JSTL + Bootstrap | Existing WAR/JSP stack | Browser workspace rendering | Keep session + CSRF protection for browser routes. [VERIFIED: docs/ARCHITECTURE_DECISIONS.md:55-60] |
| Spring WebSocket/STOMP | Spring Boot managed | Private action notification transport | Reuse the existing authenticated `/ws` and `/user/queue` foundation. [VERIFIED: src/main/java/vn/iotstar/config/WebSocketConfig.java:20-62] |

### Supporting

| Facility | Purpose | When to Use |
|---|---|---|
| `@Transactional` | Groups workflow validation, aggregate mutation, event/ledger persistence, and rollback. | Every delivery custody or financial command. [VERIFIED: src/main/java/vn/iotstar/service/impl/ShipmentServiceImpl.java:41-84] |
| `AuthenticatedActor.requireCurrent()` | Server-derived user identity for service scope policy. | Every protected service command/read; never trust request owner, hub, balance, or assignee IDs. [VERIFIED: src/main/java/vn/iotstar/security/AuthenticatedActor.java:10-18] |
| Spring Security filter chains | Session+CSRF browser boundary and stateless JWT `/api/**` boundary. | Preserve the split; narrowly add Phase-04 routes only. [VERIFIED: src/main/java/vn/iotstar/config/SecurityConfig.java:42-84] |
| SQL Server migration scripts | Explicit physical-schema reconciliation/change delivery. | Required because Hibernate auto-DDL is disabled. [VERIFIED: src/main/resources/application.properties:27-32] |

### Alternatives Considered

| Instead of | Could Use | Decision |
|---|---|---|
| SQL Server real test | H2/MySQL/PostgreSQL | Rejected: ADR-010 and Phase-04 context require SQL Server, and a skipped real test is BLOCKED rather than PASS. [VERIFIED: docs/ARCHITECTURE_DECISIONS.md:73-78] |
| Broadcast topic for finance/task updates | Private `/user` STOMP destination | Rejected: D-15 forbids broad address/COD/finance broadcasts. [VERIFIED: .planning/phases/04-last-mile-finance/04-CONTEXT.md:41-42] |
| New ShipmentStatus for attempts/reconciliation | `shipment_events.event_type` plus finance-state records | Rejected: the fixed state set and event distinction are ADR decisions. [VERIFIED: docs/ARCHITECTURE_DECISIONS.md:27-35] |
| External bank/VNPay payout in withdrawal approval | Demo-domain completion only | Deferred to V2. [VERIFIED: .planning/phases/04-last-mile-finance/04-CONTEXT.md:78-80] |

**Installation:** No new external package is justified by Phase 04. [VERIFIED: .planning/phases/04-last-mile-finance/04-CONTEXT.md:43-48]

## Package Legitimacy Audit

No package installation is planned; no package-legitimacy check is applicable.

## Architecture Patterns

### System Architecture Diagram

```text
Authenticated browser (session + CSRF) or JWT API
        |
        v
Controller -> validated request DTO -> Service interface / ServiceImpl
                                         |
             server-derived actor + assignment/hub/owner/location scope check
                                         |
                                         v
          SQL Server transaction: lock + validate state/reference
              |                 |                     |
              v                 v                     v
     shipment/event       COD/payment/attempt     wallet/ledger/reconciliation
              \                 |                     /
               \----------------+--------------------/
                                |
                             commit
                                |
                                v
         minimal private STOMP action notification -> authorized recipient
```

The current application already demonstrates the intended seam: its shipment service derives the actor, persists a shipment and `SHIPMENT_CREATED` event inside a transaction, then schedules a private notification after commit. [VERIFIED: src/main/java/vn/iotstar/service/impl/ShipmentServiceImpl.java:41-84]

### Pattern 1: Service-owned contextual command

**Use:** Controller accepts a request DTO, validates it, and delegates once. The service obtains the authenticated actor, loads/locks every affected record, checks the workflow state plus role/hub/assignment/ownership, persists records, and returns a DTO/result. [VERIFIED: docs/ARCHITECTURE_DECISIONS.md:146-151]

```java
// Supplementary implementation skeleton; names and lock mechanism remain discretionary.
@Transactional
public Result execute(Command request) {
    Actor actor = actorProvider.requireCurrent();
    Aggregate aggregate = repository.loadForMutation(request.stableResourceId());
    policy.assertAllowed(actor, aggregate);
    transition.apply(aggregate);
    ledgerOrEventRepository.save(...);
    return Result.from(aggregate);
}
```

The skeleton must not make client IDs authoritative and must preserve the server-derived identity pattern. [VERIFIED: src/main/java/vn/iotstar/security/AuthenticatedActor.java:10-18]

### Pattern 2: Finance state is not shipment state

The fixed shipment enum is quoted verbatim: `CREATED, WAITING_FOR_PAYMENT, WAITING_FOR_HANDOVER, PICKED_UP, AT_HUB, IN_TRANSIT, OUT_FOR_DELIVERY, FAILED, DELIVERED, RETURN_TO_SENDER, RETURNED_TO_SENDER, CANCELLED, CLOSED_EXCEPTION`. [VERIFIED: src/main/java/vn/iotstar/entity/ShipmentStatus.java:7-21] COD, reconciliation, payment, and withdrawal lifecycles must be stored in their own finance records; operational history uses `shipment_events.event_type`. [VERIFIED: docs/ARCHITECTURE_DECISIONS.md:27-35]

### Pattern 3: Immutable-audit / post-commit notification ordering

Persist the operational event and financial records in the business transaction. Register private STOMP publication only after a valid commit; no success notification may be emitted after rollback. The existing publisher deliberately rejects calls with no active transaction. [VERIFIED: src/main/java/vn/iotstar/realtime/StompShipmentEventPublisher.java:19-34]

### Current-code alignment and implementation drift

| Finding | Evidence | Planning consequence |
|---|---|---|
| Phase-04 entities and repositories do not yet exist. | Current source has only `User`, `Shipment`, `ShipmentEvent`, and their repositories. [VERIFIED: src/main/java/vn/iotstar/entity/Shipment.java:22-124] | Add the canonical Phase-04 mappings/scripts; do not let the Phase-01 tracer model redefine the full target schema. |
| The Phase-01 entity maps `shipment_events.event_type`. | `ShipmentEvent` explicitly maps `@Column(name = "event_type")`. [VERIFIED: src/main/java/vn/iotstar/entity/ShipmentEvent.java:20-72] | Preserve this contract in every Phase-04 migration/event assertion. |
| `docs/schema.sql` still declares legacy `shipment_events.status`. | The legacy DDL calls the column `status`; the Phase-01 migration copies it forward into `event_type`. [VERIFIED: docs/schema.sql:143-156; src/main/resources/db/phase-01-shipment-events-event-type.sql:1-25] | This is resolved implementation drift under ADR-003, not permission to recreate `status` semantics. |
| Browser and API security are intentionally separate. | Browser chain is session+CSRF; `/api/**` is stateless and uses the JWT filter. [VERIFIED: src/main/java/vn/iotstar/config/SecurityConfig.java:42-84] | Phase-04 JSP routes use the browser chain; do not weaken `/api/**` or `/ws`. |
| Existing STOMP only permits authenticated private shipment subscription and blocks client sends. | The interceptor accepts only the defined user shipment destination and rejects `SEND`. [VERIFIED: src/main/java/vn/iotstar/config/WebSocketConfig.java:36-62] | Extend narrowly with authorized private action destinations; never add a public/broadcast shortcut. |

## Don't Hand-Roll

| Problem | Do Not Build | Use Instead | Why |
|---|---|---|---|
| Authentication identity | Request-supplied user/shipper/manager identifiers | `AuthenticatedActor.requireCurrent()` | Client data cannot be the authority boundary. [VERIFIED: src/main/java/vn/iotstar/security/AuthenticatedActor.java:10-18] |
| Money replay defense | JSP disabled-button or in-memory flag | Service transaction plus database uniqueness/locking and replay tests | D-09 requires atomic service-level idempotency. [VERIFIED: .planning/phases/04-last-mile-finance/04-CONTEXT.md:28-30] |
| Finance audit | Balance-only mutation or shipment-event-only accounting | `wallets` plus `wallet_transactions` snapshots/references | ADR-001 fixes the separate financial aggregate. [VERIFIED: docs/ARCHITECTURE_DECISIONS.md:5-11] |
| Delivery-state expansion | Convenience statuses for failed attempt, return direction, or reconciliation | Existing `ShipmentStatus` plus `event_type`, direction, and finance records | ADR-014 fixes the status set. [VERIFIED: docs/ARCHITECTURE_DECISIONS.md:123-144] |
| Payout integration | Real bank/Open Banking/VNPay call in Phase 04 | Admin demo completion and ledger outcome only | Explicitly deferred. [VERIFIED: .planning/phases/04-last-mile-finance/04-CONTEXT.md:78-80] |

## Common Pitfalls

### Financial replay or partial mutation

**Risk:** A repeated request can persist a second collection, ledger credit, withdrawal lock, or inconsistent balance after a later write fails. [VERIFIED: .planning/phases/04-last-mile-finance/04-CONTEXT.md:28-30]  
**Avoid:** Lock/reload relevant state, assert the canonical state/reference, mutate and write the ledger in one service transaction, and add a database unique key for each stable financial source/reference. The concrete JPA locking style is discretionary and must be SQL-Server compatible. [ASSUMED]

### Conflating cash accountability with COD payout

**Risk:** Receiver-paid cash shipping fee is collected and reconciled but is not sender COD payout. [VERIFIED: docs/UTEExpress_Workflow.md:1799-1878]  
**Avoid:** Persist source type/id on the supplementary reconciliation-item extension; payout only matched COD items. This fills a field-level gap without replacing the authoritative payment/COD tables. [VERIFIED: .planning/phases/04-last-mile-finance/04-CONTEXT.md:34-35]

### Group-1 failure leaves parcel illegally out for delivery

**Risk:** A temporary failed attempt may be retried without return-to-hub custody or reschedule constraints. [VERIFIED: docs/UTEExpress_Workflow.md:1328-1384]  
**Avoid:** Append audit events, return custody to the delivery hub, make the old assignment non-actionable, and create/reactivate the scoped retry path before another scan. [VERIFIED: .planning/phases/04-last-mile-finance/04-02-PLAN.md:39-49]

### Scope/PII leak through a workspace or notification

**Risk:** A role-filtered query or broad STOMP broadcast exposes another shipper's task, address, COD, bank, or evidence reference. [VERIFIED: .planning/phases/04-last-mile-finance/04-CONTEXT.md:41-42]  
**Avoid:** Authorize every service projection by actor + hub + assignment/owner + resource state, minimize DTO fields, and derive the recipient server-side after commit. [VERIFIED: docs/ARCHITECTURE_DECISIONS.md:146-151]

## Code Examples

### Private action notification shape

The existing safe publisher route is a private user destination, not a broadcast: `registry.enableSimpleBroker("/queue")`, `registry.setUserDestinationPrefix("/user")`, and `messagingTemplate.convertAndSendToUser(...)`. [VERIFIED: src/main/java/vn/iotstar/config/WebSocketConfig.java:24-27; src/main/java/vn/iotstar/realtime/StompShipmentEventPublisher.java:27-32]

```java
// Supplementary shape; payload must contain only action, stable id, and status.
record FinanceActionNotification(String action, Long resourceId, String status) {}

// Publish only after the finance/delivery transaction commits.
publisher.publishAfterCommit(serverResolvedRecipient, notification);
```

Do not include recipient-controlled target, address, phone, COD, balance, bank data, raw entity graphs, or `evidence_url`. [VERIFIED: .planning/phases/04-last-mile-finance/04-CONTEXT.md:41-42]

### SQL Server verification pattern

The existing integration test uses `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)`, conditionally enables only when SQL Server environment variables exist, uses `JdbcTemplate` assertions, and cleans up test rows. [VERIFIED: src/test/java/vn/iotstar/integration/ShipmentSqlServerIntegrationTest.java:26-95]

```text
apply Phase-04 migration(s)
seed only documented upstream contract fixtures
execute one transactional service command
assert SQL Server rows, foreign keys, unique-source/reference behavior, and cleanup
report BLOCKED_BY_ENVIRONMENT if the real variables are unavailable
```

## Environment Availability

| Dependency | Required By | Available in repository | Fallback |
|---|---|---:|---|
| SQL Server credentials via `UTEEXPRESS_SQLSERVER_URL`, `UTEEXPRESS_SQLSERVER_USERNAME`, `UTEEXPRESS_SQLSERVER_PASSWORD` | Real persistence proof | Not inferable from source; profile is environment-gated | Report `BLOCKED_BY_ENVIRONMENT`; do not substitute a database. [VERIFIED: src/test/java/vn/iotstar/integration/ShipmentSqlServerIntegrationTest.java:79-85] |
| `UTEEXPRESS_JWT_SECRET` | Boot security configuration in real test context | Not inferable from source | Supply outside repository; never hard-code it. [VERIFIED: src/main/resources/application.properties:49-50] |
| Phase-02 payment and Phase-03 assignment/hub/incident contracts | Full Phase-04 end-to-end flow | Not implemented in current Phase-01 source | Use documented fixture contracts or report `BLOCKED_BY_UPSTREAM_CONTRACT`; do not duplicate upstream slices. [VERIFIED: .planning/phases/04-last-mile-finance/04-PATTERNS.md:190-196] |

## Validation Architecture

| Property | Value |
|---|---|
| Framework | JUnit 5, Spring Boot Test, Mockito, Spring Security Test. [VERIFIED: pom.xml:124-133] |
| Quick run | Focused `rtk mvn -Dtest=<Phase04Tests> test` commands already assigned per PLAN. [VERIFIED: .planning/phases/04-last-mile-finance/04-01-PLAN.md:47-48] |
| Full suite | `rtk mvn test`. [VERIFIED: .planning/phases/04-last-mile-finance/04-10-PLAN.md:44-45] |
| Schema/static check | `rtk git diff --check`; no Git writes. [VERIFIED: .planning/phases/04-last-mile-finance/04-10-PLAN.md:69-70] |

### Phase Requirements → Test Map

| Requirement | Essential automated coverage | Real/manual gate |
|---|---|---|
| LAST-01 | Assigned-scan, duty collection before delivery, duplicate command, route/CSRF, event audit. | SQL Server shipment/attempt/COD rows. |
| LAST-02 | All failure reasons, daily/three-day limits, return-to-hub/reschedule, two return handovers, scope denial. | SQL Server custody and state/event trace. |
| FIN-01 | Payment-versus-COD source separation, payout, return fee, refund/compensation reference tests. | SQL Server source/reconciliation/ledger rows. |
| FIN-02 | Lazy wallet, balance snapshots, duplicate/concurrent lock or credit rejection. | SQL Server uniqueness and transaction rollback. |
| FIN-03 | Manager hub scope, item-level matched/discrepant resolution, incident provenance, no debt table. | SQL Server foreign keys and `HOLD_DISPUTE` outcome. |
| FIN-04 | Owner withdrawal, insufficient/cross-owner rejection, Admin approve/reject races and ledger entries. | SQL Server wallet/request/ledger invariants. |

### Wave 0 Gaps

- Phase-04 entity, repository, DTO, service, controller, JSP, SQL migration, and test classes are intentionally absent from the current codebase; plans 04-01 through 04-10 own their introduction. [VERIFIED: .planning/phases/04-last-mile-finance/04-PATTERNS.md:185-202]
- A real SQL Server run is mandatory evidence; a condition-skipped test is never a PASS. [VERIFIED: .planning/phases/04-last-mile-finance/04-10-PLAN.md:35-36]

## Security Domain

### Applicable ASVS Categories

| Category | Applies | Phase-04 control |
|---|---|---|
| V2 Authentication | Yes | Preserve server-derived principal for session/JWT boundary. [VERIFIED: src/main/java/vn/iotstar/security/AuthenticatedActor.java:10-18] |
| V3 Session Management | Yes | Preserve session + CSRF browser chain and protected `/ws`. [VERIFIED: src/main/java/vn/iotstar/config/SecurityConfig.java:63-84] |
| V4 Access Control | Yes | Service-level role, assignment, hub, resource, owner, and location scope checks. [VERIFIED: docs/ARCHITECTURE_DECISIONS.md:146-151] |
| V5 Input Validation | Yes | DTO validation, PRG, safe errors, and no direct entity binding. [VERIFIED: .planning/phases/04-last-mile-finance/04-PATTERNS.md:109-132] |
| V6 Cryptography | Yes | Preserve existing authentication/JWT configuration; do not expose secrets or bank data. [VERIFIED: src/main/resources/application.properties:49-50] |

### Known Threat Patterns

| Pattern | STRIDE | Required mitigation |
|---|---|---|
| Client selects someone else's task, hub, wallet, or recipient | Spoofing / Elevation | Derive actor and recipient server-side; service enforces context. [VERIFIED: .planning/phases/04-last-mile-finance/04-CONTEXT.md:19-22] |
| Replayed financial/custody request | Tampering / Repudiation | Transaction, state/reference check, unique key, lock/version, and audit record. [VERIFIED: .planning/phases/04-last-mile-finance/04-CONTEXT.md:28-30] |
| Broad finance/task notification | Information disclosure | Private minimal notification after commit only. [VERIFIED: .planning/phases/04-last-mile-finance/04-CONTEXT.md:41-42] |
| Legacy status column reintroduced | Tampering | Migration reconciles `event_type`; tests assert event semantics and fixed shipment status enum. [VERIFIED: src/main/resources/db/phase-01-shipment-events-event-type.sql:1-25] |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|---|---|---|
| A1 | JPA pessimistic row locking or optimistic versioning can be selected as the SQL Server-compatible concrete implementation, subject to focused concurrency proof. | Summary / pitfalls | Incorrect mapping could cause deadlocks or allow a replay; select and test in execution. |
| A2 | A source discriminator/reference on reconciliation items is needed to distinguish COD from receiver-cash fee accountability because the canonical fields do not model the distinction explicitly. | Common Pitfalls | Schema decision must remain additive and traceable; do not overwrite canonical payment/COD data. |

## Open Questions

1. **Upstream contract availability**
   - What is known: Phase-04 requires Phase-02 payment and Phase-03 assignment/hub/incident contracts, but current application code contains only Phase-01 entities. [VERIFIED: .planning/phases/04-last-mile-finance/04-PATTERNS.md:185-196]
   - Recommendation: Plans should use documented fixture seams and report `BLOCKED_BY_UPSTREAM_CONTRACT` for a real E2E proof until those slices exist; Phase 05 remains the orchestration phase.
2. **Concurrency mechanism selection**
   - What is known: D-09 requires idempotency at the service transaction boundary but leaves the SQL-Server/JPA lock style to discretion. [VERIFIED: .planning/phases/04-last-mile-finance/04-CONTEXT.md:43-48]
   - Recommendation: choose one mechanism per aggregate, add unique constraints for stable business references, and test concurrent/retry behavior before treating money results as green.

## Sources

### Primary (HIGH confidence)

- `docs/ARCHITECTURE_DECISIONS.md` — accepted architecture constraints and conflict resolution.
- `docs/UTEExpress_Workflow.md` — last-mile, returns, finance workflows and field-level data definitions.
- `docs/ERD.md` — logical relationships among shipment, assignment, delivery attempts, wallet, COD, reconciliation, and withdrawal.
- `.planning/phases/04-last-mile-finance/04-CONTEXT.md` — locked Phase-04 decisions and scope fences.
- Current code and tests under `src/main` and `src/test` — current-state seams only.

### Secondary (MEDIUM confidence)

- `.planning/phases/04-last-mile-finance/04-PATTERNS.md` and `04-01-PLAN.md` through `04-10-PLAN.md` — implementation alignment and verification decomposition, not higher-authority domain rules.

## Metadata

**Confidence breakdown:**
- Canonical domain, boundaries, and stack: HIGH — direct authoritative documents read this session.
- Current-code alignment: HIGH — actual source/config/tests read this session.
- Locking/concurrency implementation choice: MEDIUM — explicitly delegated by context and must be proven by tests.

**Research date:** 2026-10-13  
**Valid until:** Phase-04 execution begins or a canonical document changes.
