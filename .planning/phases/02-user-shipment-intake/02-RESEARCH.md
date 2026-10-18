# Phase 02: User & Shipment Intake - Research

**Researched:** 2026-10-12  
**Domain:** Account verification, shipment intake, first-mile assignment, privacy-aware tracking  
**Confidence:** HIGH for canonical rules and current-state seams; MEDIUM for OTP persistence and demo-payment mechanics where the canonical docs leave implementation detail to the team.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** A sender must have an account. Self-registration creates only `ROLE_USER`, follows phone OTP verification before becoming active, and uses the existing session/JWT authentication foundations. `ROLE_SHIPPER` and operational roles are not self-selectable. — **Reversibility:** one-way — changing persisted role assignment later needs a data migration and conflicts with ADR-002.
- **D-02:** A receiver account is optional. Shipment creation normalizes the receiver phone, auto-links `receiver_user_id` when a matching user exists, and always persists receiver name/phone/address snapshots so later profile edits do not rewrite shipment history. — **Reversibility:** costly — undoing link/snapshot semantics breaks privacy and historical-readback contracts.
- **D-03:** A linked recipient starts with masked sender/address/item information. Full detail unlock requires the full tracking number or receiver OTP; public tracking never exposes contacts, full addresses, employee/driver data, incidents, finance, or audit data. — **Reversibility:** costly — privacy leakage cannot be safely recalled once exposed.
- **D-04:** Shipment creation uses a server-validated JSP/Bootstrap multi-step wizard: sender/receiver information → parcel/service information → quote and fee-payer/payment selection → first-mile method. The final request still crosses Controller → DTO → Service → Repository/JPA → SQL Server. — **Reversibility:** costly — changing a public form flow affects validation, PRG, tests, and user-facing drafts.
- **D-05:** Phase 02 implements internal demo payment initiation: persist the canonical payment record and explicit payment-state transition, then allow a controlled demo confirmation. Do not integrate live VNPay or a bank provider, and do not use wallet settlement, which belongs to Phase 04. — **Reversibility:** costly — payment state/history must remain compatible with later real-provider and finance flows.
- **D-06:** The sender chooses `HOME_PICKUP` or `SELF_DROPOFF` at the final wizard step after payment handling, then receives the corresponding detail/instruction view. Timeout and cancellation follow the Workflow and record canonical reasons rather than creating new statuses. — **Reversibility:** costly — first-mile method changes affect task, deadline, event, and cancellation semantics.
- **D-07:** Phase 02 includes a minimal coordinator action that assigns a pickup task to a `ROLE_SHIPPER`; a shipper can read and complete only its assigned pickup work. This is sufficient for the intake vertical slice and must not absorb Phase-03 hub hierarchy, routing, or advanced custody operations. — **Reversibility:** costly — assignment data scope is a shared contract with later operational slices.
- **D-08:** Public tracking is a separate guest search page/API: a visitor enters a tracking number and sees only canonical public tracking fields. Do not create a directly shareable tracking URL as the primary public boundary. — **Reversibility:** reversible.

### the agent's Discretion

- Choose exact JSP layout, DTO names, server-side validation grouping, demo-payment confirmation mechanics, and WebSocket event payloads, provided each preserves the locked Workflow, data-scope, and Phase-02 boundaries.
- Use the completed Phase-01 exception and STOMP seams rather than creating a competing architecture.

### Deferred Ideas (OUT OF SCOPE)

- Live VNPay/bank-provider integration — V2 only.
- Hub hierarchy, static routing rules, bag/container/custody/handover/trip/incident flows — Phase 03, owned by Vạn.
- Last-mile delivery, returns, COD settlement, standalone wallet/ledger, reconciliation, and withdrawal — Phase 04, owned by Phát.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research support |
|---|---|---|
| INTAKE-01 | Sender/recipient register, authenticate, and receive only permitted account access. | Registration/OTP boundary, BCrypt, role immutability, session/JWT regression path. |
| INTAKE-02 | Sender creates a full shipment with canonical fields and initial state. | Shipment field expansion, snapshot/link rules, DTO validation, state/event rules. |
| INTAKE-03 | Sender obtains a Workflow-defined fee and chooses allowed payer/payment path. | `pricing_policies`, `payments`, canonical payer/method/status model, demo-only confirmation. |
| INTAKE-04 | Sender/assigned shipper completes first-mile method with assignment, timeout, cancellation, and race handling. | `shipment_assignments`, `pickup_attempts`, optimistic concurrency and timeout boundary. |
| INTAKE-05 | Sender tracks own shipment; eligible recipient is auto-linked and sees canonical masking. | Owner/recipient/public projections, phone normalization, full-tracking/OTP unlock, private STOMP seam. |
</phase_requirements>

## Summary

Phase 02 must replace the Phase-01 tracer defaults with a complete intake aggregate while preserving the existing service transaction boundary, `AuthenticatedActor` identity seam, JSP-session plus JWT-API split, SQL Server-only persistence, and post-commit private STOMP publishing. The current code is deliberately minimal: its shipment factory fixes `SELF_DROPOFF`, `SENDER`, `NO_INSPECTION`, and `UNPAID`, and its detail service authorizes only the sender. Those are **IMPLEMENTATION_DRIFT / Phase-01 defaults**, not Phase-02 requirements. [VERIFIED: `src/main/java/vn/iotstar/entity/Shipment.java:54-94`; `src/main/java/vn/iotstar/service/impl/ShipmentServiceImpl.java:40-68`]

The phase can proceed without Phase 03/04 if it models only intake records and first-mile pickup assignment. It must not accept a parcel into a hub, run routing/custody, settle a wallet, or deliver/return a shipment. A successful online-demo payment moves the existing payment record from `PENDING` to `PAID`; it must use a canonical stored payment method such as `VNPAY` while clearly presenting the action as a simulated confirmation, not an external provider callback. This preserves later provider integration without inventing a `DEMO` payment method. [VERIFIED: `docs/UTEExpress_Workflow.md:2749-2787`; `docs/UTEExpress_Workflow.md:1949-1975`; D-05 in `02-CONTEXT.md`]

**Primary recommendation:** Plan four vertical work packages in dependency order: account/OTP foundation → full intake/quote/payment → pickup/cancellation/timeout → sender/recipient/public tracking and realtime, with a SQL Server migration and real-environment verification included in the first package that needs each new table/column.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|---|---|---|---|
| Registration and OTP activation | API / Backend | Database / Storage | Controllers accept DTOs; services own password hashing, OTP lifecycle, and activation transaction. |
| JSP intake wizard | Frontend Server (SSR) | API / Backend | JSP renders Bootstrap steps and CSRF forms; service recalculates authoritative quote and persists the final request. |
| Quote and demo payment state | API / Backend | Database / Storage | Fee and state transitions must be server-derived, transactional, and auditable. |
| Pickup task and cancellation race | API / Backend | Database / Storage | Assignment scope, optimistic lock, attempt history, and state/event updates cannot be trusted to the browser. |
| Sender/recipient/public tracking | API / Backend | Frontend Server (SSR) | The service produces separate safe DTO projections; JSP/API only render them. |
| Private shipment notifications | API / Backend | Browser / Client | Existing post-commit publisher sends owner-safe DTOs to authenticated STOMP users. |

## Canonical Contract to Preserve

### Domain invariants

- The stored roles are exactly: `"ROLE_USER, ROLE_SHIPPER, ROLE_HUB_STAFF, ROLE_LINEHAUL_DRIVER, ROLE_MANAGER, ROLE_ADMIN"`; registration must persist only `ROLE_USER` and must not bind a role from the HTTP request. [VERIFIED: `src/main/java/vn/iotstar/entity/UserRole.java:4-10`; `docs/ARCHITECTURE_DECISIONS.md` ADR-002]
- The stored shipment statuses are exactly: `"CREATED, WAITING_FOR_PAYMENT, WAITING_FOR_HANDOVER, PICKED_UP, AT_HUB, IN_TRANSIT, OUT_FOR_DELIVERY, FAILED, DELIVERED, RETURN_TO_SENDER, RETURNED_TO_SENDER, CANCELLED, CLOSED_EXCEPTION"`. Assignment, quote, payment, pickup and timeout actions append `shipment_events.event_type`; they never add a status. [VERIFIED: `src/main/java/vn/iotstar/entity/ShipmentStatus.java:7-20`; `docs/UTEExpress_Workflow.md:1978-2015`; ADR-003/ADR-014]
- Shipment history uses `shipment_events.event_type`, distinct from `shipments.current_status`. The canonical `shipment_events` fields are `shipment_id`, `hub_id`, `actor_user_id`, `event_type`, `description`, and `created_at`. [VERIFIED: `docs/UTEExpress_Workflow.md:2501-2521`; `src/main/java/vn/iotstar/entity/ShipmentEvent.java`]
- Sender and receiver details are immutable shipment snapshots. Auto-link is a normalized-phone lookup that writes `receiver_user_id` if a matching account exists while retaining `receiver_name`, `receiver_phone`, and `receiver_address`. [VERIFIED: `docs/UTEExpress_Workflow.md:202-251`; `docs/UTEExpress_Workflow.md:2380-2479`]
- `HOME_PICKUP` and `SELF_DROPOFF` are first-mile methods; a sender cancellation is allowed in `CREATED` / `WAITING_FOR_HANDOVER`, and after a pickup assignment must be concurrency-safe against the shipper completing pickup. Direct cancellation is forbidden once the parcel is `PICKED_UP` or later. [VERIFIED: `docs/UTEExpress_Workflow.md:742-904`]

### Routing and security invariants

- Browser/JSP routes retain a session + CSRF chain; `/api/**` remains a stateless JWT chain. Current route security already protects `/user/**`, `/shipper/**`, and `/ws/**`; new routes must join these families rather than create a third chain. [VERIFIED: `src/main/java/vn/iotstar/config/SecurityConfig.java:43-86`]
- Derive actor identity only through `AuthenticatedActor.requireCurrent()`. It extracts the authenticated `User` from the server SecurityContext, so user, assignee, sender, and receiver IDs must never be accepted as client-provided authority. [VERIFIED: `src/main/java/vn/iotstar/security/AuthenticatedActor.java:10-19`]
- Service methods own authorization and transactions. Controllers call service interfaces and DTOs only; repositories are never injected into a controller. [VERIFIED: `docs/ENGINEERING_STANDARDS.md` §§1.2–1.6, 7.2, 8.7; `src/main/java/vn/iotstar/controller/ShipmentController.java`]

## Current-State Reusable Seams

| Seam | Current evidence | Phase-02 use |
|---|---|---|
| Session login + JWT API | Two ordered chains, form login, CSRF, `/api/**` JWT, route rules. [VERIFIED: `SecurityConfig.java:43-86`] | Add registration/public tracking only to explicitly public locations; preserve `/user/**` / `/shipper/**` restrictions. |
| Server-derived identity | `AuthenticatedActor.requireCurrent()` gets `User` principal. [VERIFIED: `AuthenticatedActor.java:10-19`] | Sender ownership, receiver view, coordinator/shipper assignment scope. |
| Shipment service / atomic event | One `@Transactional` creation service persists shipment then `SHIPMENT_CREATED`; post-commit STOMP publisher. [VERIFIED: `ShipmentServiceImpl.java:40-51`] | Extend rather than duplicate into intake services; make each state+event+payment/assignment change one service transaction. |
| Safe exceptions | Separate web/API advice maps not-found, forbidden, validation, unexpected errors to safe outcomes. [VERIFIED: `WebExceptionHandler.java:11-37`; `ApiExceptionHandler.java:13-44`] | Add domain exceptions without leaking phone, OTP, payment references, or account existence. |
| JSP/CSRF conventions | `form:form`, `jakarta.tags.core`, escaped output, POST/redirect detail. [VERIFIED: `create.jsp:1-35`; `detail.jsp:1-33`] | Multi-step wizard and tracking views must keep server validation and PRG. |
| SQL Server contract | Main config has SQL Server environment variables and `ddl-auto=none`. [VERIFIED: `application.properties:13-34`] | Supply explicit SQL Server migration scripts; do not rely on Hibernate schema creation. |
| Realtime capability | Authenticated STOMP `/ws`, user queue `/queue/shipments`, publish only after commit. [VERIFIED: `WebSocketConfig.java`; `StompShipmentEventPublisher.java`] | Publish safe intake/assignment updates only to entitled user(s), after commit. |

## Required Data and Schema Work

### Existing canonical tables to map

| Table | Phase-02 use | Canonical fields/constraints |
|---|---|---|
| `users` | sender account and optional receiver lookup | Identity `BIGINT IDENTITY`; unique `username`, `phone`, `email`; role and status. [VERIFIED: `docs/schema.sql:20-30`] |
| `pricing_policies` | authoritative server-side quote | service/regions, weight thresholds/prices, insurance/COD percentages, active status. [VERIFIED: `docs/schema.sql:72-86`] |
| `shipments` | final intake contract | receiver link/snapshots, parcel dimensions/value, service/fee/COD, payment/first-mile, state/reasons/deadline/timestamps. [VERIFIED: `docs/schema.sql:107-151`] |
| `shipment_events` | timeline/audit | event data as above; apply existing event-type reconciliation before Phase-02 data. [VERIFIED: `src/main/resources/db/phase-01-shipment-events-event-type.sql`] |
| `payments` | demo shipping-fee payment history | shipment FK, payer, type, amount, canonical method/status, reference and timestamps. [VERIFIED: `docs/schema.sql:310-323`] |
| `shipment_assignments` | pickup task | shipment/assignee FKs, `PICKUP` type, canonical assignment status/timestamps. [VERIFIED: `docs/schema.sql:181-192`; `docs/UTEExpress_Workflow.md:2549-2579`] |
| `pickup_attempts` | first-mile pickup evidence | assignment/shipment/shipper FKs, attempt count/time, canonical result/note. [VERIFIED: `docs/schema.sql:195-207`] |

### Additions that Phase 02 must explicitly design and migrate

1. **OTP verification persistence is required by D-01 but is not field-specified by the canonical schema.** Use one project-owned SQL Server table/entity for a hashed OTP challenge, normalized phone, pending registration payload or registration reference, expiry, verified/consumed timestamp, and bounded attempts/resends. The planner must add a `checkpoint:decision` recording its exact name, retention, resend and lockout policy before migration. This is supplementary implementation detail, not a new role or business workflow. [VERIFIED: `02-CONTEXT.md` D-01; `docs/UTEExpress_Workflow.md:145-166`; `docs/schema.sql:20-438`]
2. **Optimistic concurrency is explicitly required for cancellation versus pickup.** Add a SQL Server-compatible version column plus JPA `@Version` to `shipments`, then translate an optimistic-lock failure to a safe conflict/refresh result. The exact column name/type should be recorded in the migration. [VERIFIED: `docs/UTEExpress_Workflow.md:871-904`]
3. Expand the current `Shipment` mapping to all Phase-02 Workflow fields rather than retaining Phase-01 fixed defaults. The current entity lacks receiver link, region/district, dimensions, declared value, prices, COD, hubs, direction, reasons, deadline, and lifecycle timestamps. [VERIFIED: `Shipment.java:54-94`; `docs/UTEExpress_Workflow.md:2380-2479`]
4. Bring `User` JPA constraints in line with the existing schema when editing registration: current mapping lacks non-null/unique phone and required full name/email mapping and does not implement `Serializable`; schema requires all three identity fields. [VERIFIED: `User.java:11-48`; `docs/schema.sql:20-30`; `PROJECT_SPEC_KNOWLEDGE_BASE.md` §3.1]

## Recommended Service Flows

### 1. Registration and activation (INTAKE-01)

```text
Registration DTO (no role/status/verified flag)
  → validate uniqueness + normalize phone
  → BCrypt password hash
  → persist pending OTP challenge / deliver OTP through JavaMailSender seam
  → verify OTP in a service transaction
  → persist ACTIVE User(role=ROLE_USER) once, or activate an INACTIVE pre-registration record
  → session form login or existing /api/auth/login obtains identity
```

- Do not signal whether a phone/username exists in a way that differs between failed requests.
- A self-registered account cannot create `ROLE_SHIPPER` or an employee profile.
- Use a test double for `JavaMailSender`; never assert an actual email delivery in Maven tests. The existing mail dependency is already present, so no library installation is needed. [VERIFIED: `pom.xml`; `application.properties:63-72`]

### 2. Full intake, quote, and demo payment (INTAKE-02 / INTAKE-03)

```text
authenticated sender + final wizard DTO
  → normalize receiver phone; lookup receiver by phone
  → validate parcel/service/addresses/fee payer/payment choice
  → calculate quote from active pricing policy on server (never trust browser total)
  → create shipment snapshot + receiver_user_id when eligible
  → create shipping-fee payment record
  → append creation/intake event(s)
  → for sender online-demo: PENDING + WAITING_FOR_PAYMENT
  → controlled demo confirmation: PAID + WAITING_FOR_HANDOVER
  → for receiver payer / cash collection: canonical unpaid path + WAITING_FOR_HANDOVER
  → PRG to method-specific instruction/detail
```

Use the canonical payment values `"UNPAID, PENDING, PAID, FAILED, REFUNDED"` and methods `"CASH, VNPAY, E_WALLET"`; do not persist an invented payment status/method such as `DEMO`. [VERIFIED: `docs/UTEExpress_Workflow.md:1949-1975`; `docs/UTEExpress_Workflow.md:2749-2787`]

### 3. First-mile flow (INTAKE-04)

```text
coordinator (authorized) assigns PICKUP to ROLE_SHIPPER
  → persist assignment(ASSIGNED) + pickup-assigned event; status stays WAITING_FOR_HANDOVER
assigned shipper reads own work only
  → accepts / completes pickup
  → record pickup_attempt; update shipment to PICKED_UP on success + event
sender cancellation
  → allowed while CREATED/WAITING_FOR_HANDOVER
  → after assignment: optimistic version check; cancel assignment only if pickup not committed
timeout scheduler
  → HOME_PICKUP: CANCELLED + PICKUP_TIMEOUT (or PICKUP_SERVICE_FAILURE)
  → SELF_DROPOFF: CANCELLED + DROPOFF_TIMEOUT
```

No Phase-02 endpoint may transition `AT_HUB`, create a route, sort, bag, hand over, run a trip, or settle/refund a wallet. Refund/wallet posting is Phase 04; the Phase-02 cancellation record preserves evidence for it. [VERIFIED: `docs/UTEExpress_Workflow.md:742-904`; `TEAM_EXECUTION_PLAN.md` Person A boundary]

### 4. Privacy-safe reads (INTAKE-05 / D-03 / D-08)

| Viewer | Allowed projection | Service gate |
|---|---|---|
| Sender | own shipment details and permitted timeline | `sender_user_id == AuthenticatedActor.userId` |
| Linked receiver, default | masked sender name, masked address, no item detail | normalized linked phone / `receiver_user_id`; never request user ID from browser |
| Linked receiver, unlocked | full recipient-permitted detail | correct full tracking number or server-verified receiver OTP |
| Guest public search | tracking number, current status, current hub, timeline, estimated delivery only | tracking number search; no contact/address/staff/finance/audit fields |
| Assigned shipper | assigned pickup task only | role `ROLE_SHIPPER` **and** active assignment.assignee equals actor |

Use separate response DTOs for these views. Do not reuse the existing sender-full `ShipmentDetailResponse` for recipient or public views. [VERIFIED: `docs/UTEExpress_Workflow.md:2159-2176`; `ShipmentDetailResponse.java`; D-03/D-08]

## Implementation Drift and Required Correction

| Current implementation | Why it is insufficient | Phase-02 correction |
|---|---|---|
| `Shipment.create` hard-codes first-mile/payment/inspection values. | Phase 02 lets sender select full canonical intake fields. | Replace factory/default-only request with service-validated intake aggregate; preserve Phase-01 test compatibility only where behaviour remains valid. |
| `ShipmentService.getDetail` permits only sender. | Linked recipient masking/unlock and guest projection are required. | Keep sender service method and add distinct recipient/public service methods and DTOs. |
| `UserRepository` can only find username. | Registration uniqueness and receiver auto-link need phone/email lookup. | Add precise repository queries, never expose entities directly. |
| No registration/OTP/payment/assignment entities/services/repositories. | D-01, D-05, D-07 cannot be met. | Add vertical-layer artifacts and migration scripts. |
| `ShipmentRepository` has no owner/receiver/tracking queries. | Cannot page scoped lists or protect lookup paths efficiently. | Add role-specific, paginated query methods / projections; avoid `findAll`. |
| No scheduling enabled in boot class. | 48-hour timeout rule and demo profile cannot execute. | Add a narrowly scoped timeout scheduler/configuration, with production defaults and existing demo override. |

## Standard Stack

| Component | Use | Reason |
|---|---|---|
| Spring Boot MVC + JSP/JSTL + Bootstrap | Browser wizard, form validation, PRG and tracking pages | Locked by ADR-007 and current code. |
| Spring Security form session + CSRF | JSP registration/intake/tracking actions | Current web chain already implements this boundary. |
| Existing JJWT 0.12.6 | API authentication only | Preserve Phase-01 `/api/**` stack; do not replace it. |
| Spring Data JPA + SQL Server driver | entities, repositories, explicit SQL Server migrations | Locked by ADR-010 and `ddl-auto=none`. |
| Existing Spring Mail / `JavaMailSender` | OTP dispatch seam | Existing dependency; mock it in tests. |
| Existing Spring WebSocket/STOMP | private, after-commit intake notifications | Reuse post-commit user-queue publisher. |

**Installation:** None. Phase 02 should not add a provider SDK, OTP library, alternative database, SPA framework, queue, cache, or wallet library.

## Architecture Patterns

### System architecture diagram

```text
Guest / authenticated browser (JSP + Bootstrap + CSRF)
        │ registration | wizard | tracking | pickup action
        ▼
MVC Controller ──DTO + BindingResult──► Service interface / ServiceImpl
                                           │ actor = AuthenticatedActor.requireCurrent()
                                           ├── quote / OTP / privacy / state transition policy
                                           ├── @Transactional state + event + payment/assignment persistence
                                           ▼
                                  JPA Repositories ──► SQL Server
                                           │ after commit
                                           ▼
                             authenticated private STOMP user queue

JWT client ──Bearer token──► existing /api/** chain ──► same service boundary
```

### Pattern 1: Server-authoritative wizard

Keep interim step data as request/form state; the final server operation recalculates quote, obtains the authenticated sender, normalizes the receiver phone, and validates all legal combinations. HTML required fields or an earlier quote never authorize the final persistence operation.

### Pattern 2: State/event separation

One service transaction changes one legal `ShipmentStatus` and appends an operational `event_type`; statuses are never used as a task lifecycle or timeline replacement. Publish STOMP only after the transaction commits.

### Pattern 3: Projection-specific read DTOs

Build explicit sender, masked-recipient, unlocked-recipient, public-tracking, pickup-task, and realtime DTOs. Field omission is the enforcement mechanism; JSP masking alone is insufficient.

### Pattern 4: Data-scope guard at service boundary

Every receipt, assignment, completion, cancellation, recipient read, and tracking unlock calls a service method that derives actor identity server-side and checks ownership/assignment. Controller path variables and form fields are identifiers to locate a candidate resource, never authorization proof.

## Don't Hand-Roll

| Problem | Do not build | Use instead |
|---|---|---|
| Password security | custom hash or plaintext comparison | existing BCrypt `PasswordEncoder` |
| Browser/API authentication | a new auth mechanism | existing form session and JJWT chains |
| Transaction coordination | controller-level multi-save logic | `@Transactional` service methods |
| CSRF protection | custom token scheme | existing Spring Security CSRF support and JSP `_csrf` fields |
| Database schema generation | automatic Hibernate creation | reviewed SQL Server migration scripts; `ddl-auto=none` |
| Realtime delivery | a separate WebSocket protocol | existing authenticated STOMP endpoint/user queue |

## Common Pitfalls

1. **Treating a pickup assignment as a new ShipmentStatus.** Keep `WAITING_FOR_HANDOVER`; persist assignment status and an event instead.
2. **Trusting a posted sender/assignee/receiver user ID or fee.** Derive sender/assignee from `AuthenticatedActor`, receiver link from normalized phone, and fee from active pricing policy.
3. **Using the sender detail DTO for public/recipient reads.** That leaks contact and address data. Create field-minimal projections and test denied/omitted fields.
4. **Creating a `DEMO` payment method or skipping payment history.** D-05 requires canonical payment persistence plus explicit state transition; simulate confirmation of an existing canonical method.
5. **Making hub acceptance part of self-dropoff.** Phase 02 can provide instructions and expiry; actual `AT_HUB` acceptance is Phase 03.
6. **Cancelling after a pickup succeeded.** Use an optimistic version and re-read the status/assignment in the same transaction; return a safe conflict if it changed.
7. **Using `ddl-auto=update` to avoid migrations.** It contradicts current `ddl-auto=none` and makes SQL Server drift unreviewable.
8. **Testing only with mocked repositories.** Add a condition-enabled real SQL Server integration test for new tables/columns; a skipped test is BLOCKED, not PASS.

## Validation Architecture

### Test framework

| Property | Value |
|---|---|
| Framework | JUnit 5, AssertJ, Mockito, Spring MockMvc, Spring Security Test |
| Quick run | `mvn "-Dtest=<target-test-class>" test` |
| Full suite | `mvn test` |
| Database gate | Targeted SQL Server profile/integration command with real environment variables |

### Requirement → test map

| Requirement | Automated evidence | Essential cases |
|---|---|---|
| INTAKE-01 | service + filter-enabled MVC/API tests | only `ROLE_USER` self-registration, BCrypt hash, OTP expiry/one-time use, invalid OTP generic outcome, session and JWT login regressions |
| INTAKE-02 | service/unit + MVC validation + SQL Server integration | server sender identity, snapshots/linking, all valid fields, exact status/event invariant, invalid DTO no persistence |
| INTAKE-03 | quote/payment service tests | active policy calculation, client total ignored, PENDING→PAID controlled demo transition, payer/method invalid combinations denied |
| INTAKE-04 | service concurrency tests + MVC security + SQL Server integration | coordinator assignment, shipper sees only own task, `PICKED_UP` atomicity, cancellation race/conflict, 48h timeout records canonical reason |
| INTAKE-05 | service/MVC/API security tests | sender full detail, masked receiver, unlock, public projection omission, cross-user denial, post-commit safe event |

### Required test additions

- Keep all Phase-01 login/JWT/session/CSRF, 6-role, 13-status, event-type, STOMP and SQL Server tests green.
- Add a real-filter-chain test for public tracking and registration versus protected `/user/**` / `/shipper/**` routes.
- Add repository/service tests proving User B cannot read User A data before any sensitive data/event query is made.
- Add SQL Server integration evidence for migration constraints, payment/assignment/attempt FK relationships, unique tracking, and event-type persistence. Mark it `BLOCKED_BY_ENVIRONMENT` whenever the real variables/database are unavailable.
- Add manual UAT for registration OTP, one representative sender-online HOME_PICKUP journey, one SELF_DROPOFF expiry, recipient masking/unlock, public tracking omission, assignment scope, and a STOMP notification.

## Security Domain

| ASVS area | Phase-02 control |
|---|---|
| V2 Authentication | BCrypt; OTP challenge expiry/one-time consume; generic failures; no self-selected operational role. |
| V3 Session Management | retain session fixation-safe Spring form login, CSRF, logout, JSESSIONID/UTEEXPRESS_SESSION hygiene; API stays stateless. |
| V4 Access Control | `AuthenticatedActor`, sender/recipient/assignee checks at service boundary; separate response projections. |
| V5 Input Validation | Bean Validation DTOs plus service validation of state, quote, phone normalization, and assignment legality. |
| V6 Cryptography | no plaintext password/OTP persistence; reuse BCrypt for passwords and an approved one-way verification representation for OTP material. |

## Non-goals and Scope Fence

- No live VNPay/bank callback, bank payout, external provider SDK, wallet settlement/refund posting, COD settlement, reconciliation, withdrawal, return, delivery, hub acceptance, routing, container, custody, handover, trip, or incident flow.
- No additional roles, no role/permission table, no new ShipmentStatus, no `shipment_events.status` semantics, no H2/MySQL/PostgreSQL, no Thymeleaf/SPA, no controller-to-repository calls, and no direct entity binding/response.
- Phase 02 may persist first-mile evidence and event history needed for later slices; it does not implement later operational ownership.

## Open Questions / Planning Checkpoints

1. **OTP persistence / rate limits / retention** — the Workflow mandates phone OTP, but no canonical field-level table is specified. Record a human decision checkpoint before creating the table/migration; do not treat the absence as a prohibition. [MEDIUM]
2. **Demo payment confirmation authority** — D-05 permits a controlled demo confirmation but does not name its actor/route. Restrict it to a clearly labelled, authenticated sender-owned confirmation in this phase unless the leader explicitly chooses another canonical role; do not expose an unauthenticated callback. [MEDIUM]
3. **Coordinator scope before Phase 03** — D-07 requires a minimal coordinator action, while employee profile/hub administration is owned later. Plan a narrow seed/test-only coordinator context and service guard without building hub management/routing; document the temporary data prerequisite. [MEDIUM]
4. **Receiver OTP reuse versus registration OTP** — share verification infrastructure only if challenge purpose and authorization are explicitly separated; otherwise use purpose-scoped records so registration verification cannot unlock tracking detail. [MEDIUM]

## Environment Availability

| Dependency | Required by | Status | Fallback |
|---|---|---|---|
| SQL Server + environment credentials | migration and real persistence verification | Environment-gated; current application requires variables | No alternate DB permitted; mark test BLOCKED when absent |
| SMTP credentials | real OTP delivery | configuration seam exists but delivery credentials are environment-owned | Mock `JavaMailSender` in automated tests; manual OTP UAT requires configured SMTP |
| JWT secret | Spring context/API regression | required through `UTEEXPRESS_JWT_SECRET` | no fallback; use test configuration only |

## Assumptions Log

| # | Claim | Risk if wrong |
|---|---|---|
| A1 | A purpose-scoped, hashed OTP persistence table is the smallest compliant implementation detail for D-01/D-03. | A different approved storage/retention policy may be required before migration. |
| A2 | A demo-confirmed payment should retain canonical `VNPAY` method and canonical status values instead of storing `DEMO`. | The team may need an ADR if it wants a distinct persisted method. |
| A3 | Coordinator test/seed identity can be limited to Phase-02 assignment without implementing employee/hub management. | Production scope needs Phase-03 profile/hub data and must not be bypassed. |

## Sources

### Primary

- `docs/ARCHITECTURE_DECISIONS.md` — roles, event/state separation, field authority, SQL Server, JSP, constructor injection, contextual authorization.
- `docs/UTEExpress_Workflow.md` — account, privacy, intake, payment, first-mile, security matrix, database fields, statuses, public tracking, demo timers.
- `docs/ERD.md` and `docs/schema.sql` — relational model and SQL Server column/FK constraints.
- `docs/ENGINEERING_STANDARDS.md` — three-tier boundaries, validation, JSP/JSTL, JPA transaction and security constraints.
- `02-CONTEXT.md`, `REQUIREMENTS.md`, `TEAM_OWNERSHIP.md`, `TEAM_EXECUTION_PLAN.md` — locked Phase-02 scope, requirements, ownership, reviews.

### Current-state evidence

- Phase-01 entities, repositories, services, controllers, security, exception, realtime, JSP, and integration tests under `src/main` / `src/test`.

## Metadata

**Confidence breakdown:**
- Canonical contracts and scope: HIGH — read highest-authority docs and Phase-02 context.
- Current seams / implementation drift: HIGH — read current source and Phase-01 tests.
- OTP storage and demo-confirmation implementation: MEDIUM — required outcomes are locked, internal field/actor mechanics remain discretionary.

**Research date:** 2026-10-12  
**Valid until:** Phase-02 planning completes or a canonical decision changes.
