# Phase 03: Hub & Linehaul Operations - Research

**Researched:** 2026-10-12  
**Domain:** Controlled hub operations, static inter-hub routing, container custody, and linehaul transport on Spring Boot/JSP/JPA/SQL Server  
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Sorting and static route matrix

- **D-01:** Sorting derives `next_hub` only from canonical `hub_routing_rules` using current hub plus destination region/province. Hub Staff never choose a next hub manually. — **Reversibility:** costly — replacing the static canonical matrix changes the route-planning contract and later integration behavior.
- **D-02:** If no matching routing rule exists, block the scan and create neither route nor container assignment. Only the current Hub/SOC Manager and Admin see/configure the actionable error; Hub Staff receive safe blocked feedback only. — **Reversibility:** costly — allowing a manual fallback would bypass the canonical routing authority and corrupt custody routing.
- **D-03:** A successful sorter scan shows only next Hub/SOC, destination grouping, and container readiness. A repeated scan is idempotent: it creates no duplicate route, event, or container item and returns the existing route. — **Reversibility:** costly — duplicate operational records break event/audit and container-custody evidence.

#### Containers, handovers, and discrepancy evidence

- **D-04:** Before accepting a handover, the driver scans every bag/container, reconciles `expected_containers` against actual scans, and the system records both actors and timestamps. Handover accountability is container-level, not individual-parcel-level. — **Reversibility:** costly — weakening the scan/reconciliation record defeats the canonical custody evidence.
- **D-05:** Once a container enters a handover it is logically sealed. Corrections require auditable handover cancellation/recreation, never direct silent addition/removal. — **Reversibility:** costly — post-handover mutation would make custody and event history unreliable.
- **D-06:** At destination unbagging, valid containers/parcels continue through the workflow. A confirmed missing item becomes `container_items.status = MISSING` with a `MISSING_PARCEL` incident; only that shipment follows its canonical exception path. Do not block every container or create a new ShipmentStatus. — **Reversibility:** costly — missing-parcel evidence, incident linkage, and the fixed state/event model are cross-slice contracts.
- **D-07:** Handover realtime is role- and hub-scoped: relevant Outbound Staff see acceptance, the assigned Driver sees assigned handover/trip updates, and destination Inbound Staff see arrival/readiness. Do not broadcast internal custody details broadly. — **Reversibility:** costly — broadening delivery would leak operational data across hub/assignment boundaries.

#### Assigned linehaul lifecycle

- **D-08:** Driver lifecycle is explicit: Accept Handover → Start Trip → Arrive. Demo mode may shorten elapsed transit only where Workflow permits; it still records the same canonical events and audit trail. — **Reversibility:** costly — automatic completion would erase accountable custody transitions.
- **D-09:** Driver arrival does not finish physical receipt. Destination Inbound Staff must scan/unbag/reconcile containers before the handover/trip reaches its canonical completed or discrepancy outcome. — **Reversibility:** costly — unilateral driver completion conflicts with the canonical destination-receipt workflow.
- **D-10:** An assigned driver may report `TRIP_DELAY` with bounded evidence/note. `MISSING_PARCEL` is created only from destination inbound/unbag reconciliation evidence, not a driver assertion. — **Reversibility:** costly — incident provenance controls operational accountability.
- **D-11:** A linehaul driver sees and acts only on their assigned handovers/trips and may start only after acceptance, with allowed origin/destination hub scope. Role alone never grants access to another driver's work. — **Reversibility:** costly — this extends the canonical service-level contextual authorization seam.

#### Position workspaces, realtime, and audit visibility

- **D-12:** Inbound, Sorter, Outbound, and Driver each use a dedicated workspace/queue. Managers use a separate supervisory dashboard. Views and actions enforce role, active position, hub scope, assignment, shipment location, and resource ownership at the service boundary. — **Reversibility:** costly — a shared unrestricted operations surface would require later security/data-scope redesign.
- **D-13:** STOMP/WebSocket sends only action-required events: accepted handover, destination arrival, inbound-ready container, new incident, and blocked route. It does not publish every scan/sort/container change. — **Reversibility:** reversible — event subscription/payload presentation can evolve while keeping authorization intact.
- **D-14:** The Manager dashboard prioritizes scoped hub/SOC KPIs: inbound/outbound, sorting backlog, active handovers/trips, blocked routes, and actionable incidents. It is not a global unrestricted dashboard. — **Reversibility:** reversible — dashboard layout can change without changing data scope.
- **D-15:** Authorized shipment detail exposes a scoped audit timeline with event type, time, hub, actor role, and container/handover/trip reference. It does not expose data outside the viewer's scope. — **Reversibility:** costly — audit disclosure rules are an operational privacy/security contract.

### the agent's Discretion

- Choose JSP layout, DTO/entity/service/repository names, route prefixes, SQL Server-compatible locking strategy, and exact STOMP destinations/payloads, provided they preserve the locked Workflow, data scope, and existing Phase-01 security/realtime seams.
- Use canonical event and incident names/fields from Workflow/ERD; use supplementary engineering knowledge only to fill technical gaps and never to override canonical business decisions.

### Deferred Ideas (OUT OF SCOPE)

- Phase 04 owns last-mile delivery, returns, COD settlement, wallet/ledger operations, reconciliation, and withdrawal.
- Phase 05 owns end-to-end cross-slice normal/exception integration beyond Phase-03 boundaries.
- Live payment/bank providers, GPS tracking, AI routing, and other V2 infrastructure remain out of scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|---|---|---|
| OPS-01 | Authorized operations staff work in the hub hierarchy and derive the next hop from `hub_routing_rules`. | Static-rule lookup, route idempotency, hub/position scope, and SQL Server migration guidance. |
| OPS-02 | Authorized staff receive inbound shipments and record sorting/outbound events without extending shipment state. | Event/state contract, workspace guards, and scan transaction design. |
| OPS-03 | Authorized staff and drivers operate containers, items, handovers, and unbagging with accountable custody. | Container-level sealing, reconciliation, and destination receipt invariants. |
| OPS-04 | Assigned drivers operate allowed trips and record arrival or discrepancy outcomes. | Assignment/origin/destination checks and the accept → start → arrive → receipt lifecycle. |
| OPS-05 | Authorized roles record missing-parcel and related incidents without corrupting history. | Provenance rules, missing-item isolation, immutable events, and scoped notification/audit behavior. |
</phase_requirements>

## Summary

Phase 03 is a vertical operations slice: scan-oriented JSP workspaces and selected JWT field APIs call DTO-only controllers, transactional services, repositories/JPA, and SQL Server. The canonical workflow distinguishes shipment state from operational history: inbound, sorting, bagging, outbound, and arrival append `shipment_events.event_type`; they must not add state values. [CITED: docs/ARCHITECTURE_DECISIONS.md ADR-003/ADR-014; docs/UTEExpress_Workflow.md §§13-15, 74, 85]

The primary risk is custody corruption under retries and concurrent actions. Treat sorting as an idempotent route decision; treat accepted handover containers as sealed; and complete a trip only after destination inbound reconciliation. A missing parcel isolates one `container_items` row and one shipment exception path while other received parcels proceed. [CITED: docs/UTEExpress_Workflow.md §§34-39, 89-92, 98, 99.2; docs/UTEExpress_Workflow.md §3020-3029]

**Primary recommendation:** Implement three end-to-end, transactionally guarded capabilities in order: inbound/sort route decision, outbound container + driver handover, then destination receipt/unbag + incident handling; expose each through its scoped JSP queue and reuse the Phase-01 service, security, exception, and post-commit STOMP seams. [VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:9-13, 82-96]

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|---|---|---|---|
| Inbound/sorter/outbound work queues | Frontend Server (SSR) | API / Backend | JSP exposes only an eligible queue; the service makes the authoritative scan, location, position, and state decision. [CITED: docs/UTEExpress_Workflow.md §§12-16, 79] |
| Static next-hop routing and idempotent route creation | API / Backend | Database / Storage | The route decision is a server-side lookup of the static matrix with uniqueness/idempotency enforced persistently. [CITED: docs/ARCHITECTURE_DECISIONS.md ADR-005; docs/UTEExpress_Workflow.md §§39, 85.1] |
| Container, handover, and trip custody | API / Backend | Database / Storage | Atomic multi-row transitions and audit references belong in service transactions and SQL Server, not client state. [CITED: docs/UTEExpress_Workflow.md §§35-39, 89-92] |
| Operational dashboards and audit timeline | Frontend Server (SSR) | API / Backend | UI renders a scoped projection; service filters according to hub hierarchy/resource scope before returning it. [CITED: docs/UTEExpress_Workflow.md §§19-24, 75-76; docs/ARCHITECTURE_DECISIONS.md ADR-015] |
| Action-required realtime messages | API / Backend | Browser / Client | Services publish safe DTOs after commit; clients subscribe only to permitted user/hub queues. [VERIFIED: src/main/java/vn/iotstar/realtime/StompShipmentEventPublisher.java:11-35; VERIFIED: src/main/java/vn/iotstar/config/WebSocketConfig.java:17-55] |

## Canonical Operational Contract

### Fixed identities, positions, state, and history

The only stored roles are the following exact values. `GUEST` is not stored and no role/permission tables are allowed. [VERIFIED: src/main/java/vn/iotstar/entity/UserRole.java:3-11]

```text
DATA_R6K2Q9VP_START
ROLE_USER
ROLE_SHIPPER
ROLE_HUB_STAFF
ROLE_LINEHAUL_DRIVER
ROLE_MANAGER
ROLE_ADMIN
DATA_R6K2Q9VP_END
```

The exact persisted shipment-state set is fixed at 13 values; Phase 03 must use events for operational actions. [VERIFIED: src/main/java/vn/iotstar/entity/ShipmentStatus.java:3-21]

```text
DATA_N4J7X1MC_START
CREATED
WAITING_FOR_PAYMENT
WAITING_FOR_HANDOVER
PICKED_UP
AT_HUB
IN_TRANSIT
OUT_FOR_DELIVERY
FAILED
DELIVERED
RETURN_TO_SENDER
RETURNED_TO_SENDER
CANCELLED
CLOSED_EXCEPTION
DATA_N4J7X1MC_END
```

`ROLE_HUB_STAFF` is narrowed by employee position: `INBOUND`, `SORTER`, `OUTBOUND`, `COORDINATOR`, or `GENERAL_STAFF`; the driver’s profile position is `LINEHAUL_DRIVER`. A manager’s level comes from the assigned hub’s level, not a new manager role. [CITED: docs/UTEExpress_Workflow.md §§12, 19-25, 82]

Use the action guard below at every service entry point. Frontend hiding, route matchers, and an actor-supplied hub identifier are insufficient. [CITED: docs/README.md §4; docs/ARCHITECTURE_DECISIONS.md ADR-015]

| Action | Required role / position | Scope and state preconditions |
|---|---|---|
| inbound scan | Hub Staff / `INBOUND` or authorized `GENERAL_STAFF` | Actor’s active employee profile is at the shipment’s receiving/current hub; record arrival and move eligible shipment to `AT_HUB`. [CITED: docs/UTEExpress_Workflow.md §13] |
| sorter scan | Hub Staff / `SORTER` or `GENERAL_STAFF` | Shipment is `AT_HUB` at actor hub; use rule `(current_hub_id, destination_region, destination_province)` only. [CITED: docs/UTEExpress_Workflow.md §§14, 34, 39, 85.1] |
| create/load outbound container | Hub Staff / `OUTBOUND` or `GENERAL_STAFF` | Actor hub is container origin; all items are compatible with that container’s next hub and remain traceable. [CITED: docs/UTEExpress_Workflow.md §§15, 35, 89-90] |
| accept/start/arrive linehaul | Linehaul Driver | Driver is `receiver_driver_id` and `driver_user_id`; handover is accepted before start; trip origin/destination match handover. [CITED: docs/UTEExpress_Workflow.md §§17-18, 91-92] |
| unbag/reconcile destination | Hub Staff / `INBOUND` or `GENERAL_STAFF` | Actor at handover/trip destination; only receipt reconciliation can complete custody. [CITED: docs/UTEExpress_Workflow.md §§37-38] |
| configuration/error diagnosis | Manager scoped to current hub/subtree, or Admin | Staff receive safe blocked feedback; Manager/Admin can inspect a missing static-rule condition, not override it manually. [VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:21-23] |

### Route, event, and incident rules

1. Inbound records `ARRIVED_AT_HUB` and results in `AT_HUB`; sorting records `SORTING_COMPLETED` while retaining `AT_HUB`; container/outbound activity records `BAG_CREATED` / `OUTBOUND_SCANNED`. [CITED: docs/UTEExpress_Workflow.md §§13-15, 74-75]
2. Sorter lookup is static only: current hub plus destination region, with province taking precedence when the matching rule specifies a non-null province. It produces the next hop and the corresponding route sequence; it is not a dynamic route algorithm. [CITED: docs/UTEExpress_Workflow.md §§39, 85.1; docs/ARCHITECTURE_DECISIONS.md ADR-005]
3. An unmatched rule is a blocked business result, not a retryable null/default route. It must atomically create no `shipment_routes`, event, or container membership; expose the detailed configuration signal only to scoped Manager/Admin. [VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:21-23]
4. `TRIP_DELAY` is a bounded driver-reported incident. `MISSING_PARCEL` may be created only from destination unbag reconciliation evidence, with the item marked missing; it is not an additional `ShipmentStatus`. [VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:29, 34-37; CITED: docs/UTEExpress_Workflow.md §§37-38, 98]

## Standard Stack

### Core

| Library / platform | Version | Purpose | Why standard |
|---|---:|---|---|
| Spring Boot | `3.3.4` | MVC, service transactions, JPA, security and test runtime | Existing project parent; Phase 03 extends rather than replaces it. [VERIFIED: pom.xml:8-11] |
| Spring MVC + JSP/JSTL + Bootstrap | existing project stack | SSR operations workspaces and PRG forms | ADR-007 fixes JSP/JSTL + Bootstrap as primary frontend. [CITED: docs/ARCHITECTURE_DECISIONS.md ADR-007] |
| Spring Data JPA / Hibernate | existing project stack | Entity/repository persistence | Canonical layered stack with service-owned transaction boundaries. [CITED: docs/ARCHITECTURE_DECISIONS.md ADR-011; docs/ENGINEERING_STANDARDS.md §§1, 7.2] |
| Microsoft SQL Server JDBC | existing project dependency | Sole production persistence target | ADR-010 fixes SQL Server; H2/MySQL/PostgreSQL cannot substitute for real-environment verification. [CITED: docs/ARCHITECTURE_DECISIONS.md ADR-010; VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:89-92] |
| Spring Security + JJWT | existing project dependencies | Browser session/CSRF and stateless API JWT | Preserve the two-chain split. [VERIFIED: src/main/java/vn/iotstar/config/SecurityConfig.java:43-85] |
| Spring WebSocket/STOMP | existing project dependency | Scoped action-required notifications after commit | Reuse the authenticated STOMP seam rather than polling or broad topic broadcast. [VERIFIED: src/main/java/vn/iotstar/config/WebSocketConfig.java:20-55; VERIFIED: src/main/java/vn/iotstar/realtime/StompShipmentEventPublisher.java:11-35] |

### Supporting

No new external package is required or recommended for Phase 03. Use JPA locking/unique constraints, Bean Validation, Spring transactions, and the existing Spring test stack. [CITED: docs/ENGINEERING_STANDARDS.md §§4, 7.2, 8]

**Installation:** None — do not alter the dependency graph for this phase. [VERIFIED: pom.xml:34-149]

## Package Legitimacy Audit

No external package installation is in scope; the package-legitimacy gate is therefore not applicable. Existing dependencies are current-code inputs, not Phase-03 recommendations. [VERIFIED: pom.xml:34-149]

## Data Model and SQL Server Plan

### Required canonical entities and constraints

| Entity / migration responsibility | Canonical fields / relation | Required Phase-03 constraint or behavior |
|---|---|---|
| `hubs` and `employee_profiles` | Hub hierarchy; staff base hub and position | Reuse them to resolve current hub, employee status, position, manager scope, and allowed origin/destination. Do not invent separate role records. [CITED: docs/UTEExpress_Workflow.md §§81-82; docs/ERD.md §1] |
| `hub_routing_rules` | `current_hub_id`, `destination_region`, nullable `destination_province`, `next_hub_id`, status and timestamps | Add the canonical static matrix table because the current SQL schema has hubs but no `hub_routing_rules` definition; constrain active lookup to one deterministic rule per current-hub/region/province combination and index lookup keys. The rule maps only a next hop. [CITED: docs/UTEExpress_Workflow.md §85.1; VERIFIED: docs/schema.sql:44-69] |
| `shipment_routes` | shipment, hub, sequence, status, arrival/departure | Ensure a sorter retry cannot append another identical hop/event: persist an operation/idempotency key or enforce a suitable unique business key after determining how the existing route model will represent a full precomputed route. This is a planner choice requiring a migration design review. [CITED: docs/UTEExpress_Workflow.md §§39, 84; VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:23] |
| `shipment_events` | shipment, hub, actor, `event_type`, description, timestamp | Append immutable operational evidence in the same service transaction as its state/custody mutation. The legacy schema calls this column `status`; Phase 01 supplies a SQL Server reconciliation script that adds/enforces `event_type`, so Phase 03 must apply/require that prerequisite before writing events. [CITED: docs/UTEExpress_Workflow.md §85; VERIFIED: src/main/resources/db/phase-01-shipment-events-event-type.sql:3-28] |
| `parcel_containers` / `container_items` | container route/status and composite container/shipment membership | Unique container code and composite item identity; only eligible routed `AT_HUB` shipments join an open origin/destination-compatible container; handover participation seals logical membership. [CITED: docs/UTEExpress_Workflow.md §§89-90, 99.2] |
| `transport_trips`, `handovers`, `handover_containers` | driver/trip origins/destination; sender/receiver; expected/accepted counts; container rows | Foreign keys, expected-vs-scanned reconciliation, timestamps, and immutable accepted-container set establish container-level custody. Add a uniqueness rule preventing one active container from appearing in competing active handovers. [CITED: docs/UTEExpress_Workflow.md §§91-92, 99.2; docs/ERD.md §3] |
| `incidents` | shipment/trip/hub, type, report actor, description, status | Persist provenance and references; do not use it to mutate unrelated shipment states or finance. [CITED: docs/UTEExpress_Workflow.md §98] |

### Transaction and race behavior

Use one `@Transactional` service operation per scan/lifecycle command; resolve `AuthenticatedActor` server-side; load the target shipment/container/handover/trip with a SQL Server-compatible pessimistic-write lock or optimistic versioning plus conflict-to-safe-retry translation. The codebase currently has no `@Version` field on `Shipment`, so the plan must explicitly add versioning/locking to every aggregate whose concurrent transition it relies on, rather than assuming a JPA save serializes scans. [VERIFIED: src/main/java/vn/iotstar/entity/Shipment.java:21-118; VERIFIED: src/main/java/vn/iotstar/service/impl/ShipmentServiceImpl.java:40-70; ASSUMED]

| Command | Atomic checks and writes | Retry / conflict result |
|---|---|---|
| inbound scan | Actor hub/position, shipment eligibility/location; set `AT_HUB` only where the canonical transition permits; append arrival event. | Same already-received scan returns the existing result with no duplicate event; competing transition returns a controlled conflict. [CITED: docs/UTEExpress_Workflow.md §13; VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:23] |
| sorter scan | Lock shipment; confirm `AT_HUB` and current hub; resolve exactly one active rule; create/read route decision and sorting event. | Duplicate returns the existing route; missing rule rolls back all mutation; ambiguity is a configuration error for Manager/Admin. [CITED: docs/UTEExpress_Workflow.md §§34, 39, 85.1] |
| add to container / create handover | Lock open container + membership; validate origin/destination/next hop; create container item once; calculate expected containers. | Duplicate scan reads existing item; sealed/handed-over container rejects mutation. [CITED: docs/UTEExpress_Workflow.md §§35-36, 89-91] |
| accept handover | Lock handover and all listed container rows; assert assigned driver, all scans unique, `actual == expected`; write receiver/timestamp and accepted status. | Any mismatch rejects acceptance without partial acceptance; exact replay returns accepted handover without re-event. [VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:27-28] |
| start / arrive trip | Lock trip + accepted handover; assert assigned driver, allowed endpoints, acceptance predecessor; record departure/arrival plus event. | State-predecessor violation is conflict; driver arrival remains pending destination receipt. [VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:34-37] |
| unbag / reconcile | Lock destination receipt, handover container, and container items; mark received items, advance valid shipments to `AT_HUB`, then mark confirmed missing item `MISSING` + on hold + incident. | Repeat scans see prior receipt; valid items commit even when a missing item creates discrepancy. [CITED: docs/UTEExpress_Workflow.md §§37-38] |

**Do not hand-roll:** do not implement client-side locking, manual route selection, in-memory idempotency, a parcel-level driver handover, or a new routing algorithm. SQL Server constraints/locking plus service transactions are the authority. [CITED: docs/ARCHITECTURE_DECISIONS.md ADR-005/ADR-010; docs/UTEExpress_Workflow.md §99.2]

## Architecture Patterns

### System Architecture Diagram

```text
Scoped JSP workspace / JWT field API
            |
            v
Controller + validated DTO (no repository calls)
            |
            v
Operations service transaction
  | actor/role/position/hub/assignment/location checks
  | static-rule decision + lock/idempotency guard
  v
JPA repositories -> SQL Server custody/event records
  |                        |
  |                        +-> event/audit timeline projection
  v
after-commit safe STOMP DTO -> assigned driver / relevant hub workspace

Sorter: AT_HUB -> rule lookup -> route + SORTING_COMPLETED -> container-ready
Outbound: container -> handover -> driver accept -> trip start -> arrive
Destination: inbound unbag -> received items proceed OR missing item + incident
```

### Recommended Project Structure

```text
src/main/java/vn/iotstar/
├── controller/operations/       # JSP route controllers; DTO binding and PRG only
├── controller/api/              # narrow driver/operations field APIs, JWT protected
├── dto/operations/              # command/result, queue, dashboard, audit DTOs
├── entity/                      # canonical hub/custody/trip/incident JPA entities
├── repository/                  # query/lock methods and scoped projections
├── service/                     # OperationsService and focused capability interfaces
├── service/impl/                # transaction + contextual authorization implementation
├── security/                    # extend actor/context helpers, no client identity trust
└── realtime/                    # safe operational event DTO publisher/adapters
src/main/webapp/WEB-INF/views/operations/
├── inbound.jsp
├── sorter.jsp
├── outbound.jsp
├── driver.jsp
└── manager-dashboard.jsp
```

### Pattern 1: Command service with server-derived scope

**What:** Controller supplies a validated command (tracking/container code and bounded note), while service derives user identity from `AuthenticatedActor`, employee profile, hub, assignment and target location. [VERIFIED: src/main/java/vn/iotstar/security/AuthenticatedActor.java:8-17; VERIFIED: src/main/java/vn/iotstar/service/impl/ShipmentServiceImpl.java:40-45]

**Use:** Every state-changing scan, handover, trip, unbag, incident, dashboard/audit query. [CITED: docs/ARCHITECTURE_DECISIONS.md ADR-015]

### Pattern 2: Append operational event alongside a guarded aggregate update

**What:** In a single transaction, validate predecessor and scope, mutate only the allowed shipment/custody records, save an event with actor/hub/reference, then publish a safe notification after commit. [VERIFIED: src/main/java/vn/iotstar/service/impl/ShipmentServiceImpl.java:40-51; VERIFIED: src/main/java/vn/iotstar/realtime/StompShipmentEventPublisher.java:21-35]

**Use:** Inbound, sort, outbound, accepted handover, trip start/arrival, destination receipt, and incident creation. [CITED: docs/UTEExpress_Workflow.md §§13-18, 37-38, 77]

### Pattern 3: Reconciliation, not destructive correction

**What:** Accepted handover membership is immutable. A wrong pre-acceptance composition is cancelled/recreated with audit evidence; destination receipt compares expected container list/count against actual scans, then records each received/missing outcome. [VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:27-30; CITED: docs/UTEExpress_Workflow.md §§37-38]

### Anti-Patterns to Avoid

- **Controller authorization:** UI or URL role gates cannot replace service scope checks. [CITED: docs/README.md §4]
- **State-as-event:** never add `SORTING`, `PACKED`, `READY_FOR_OUTBOUND`, `ARRIVED_AT_HUB`, or incident labels to `ShipmentStatus`. [CITED: docs/ARCHITECTURE_DECISIONS.md ADR-014]
- **Global manager data:** manager dashboards must filter to the manager’s hub/subtree; only Admin is system-wide. [CITED: docs/UTEExpress_Workflow.md §§19-24, 75]
- **Broadcast all scans:** publish only the D-13 action-required messages and minimal DTOs. [VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:42-44]

## JSP, API, STOMP, and Authorization Seams

Browser JSP routes must remain session-backed and CSRF protected; `/api/**` remains stateless JWT with CSRF disabled. Existing route matchers already reserve `/hub/**`, `/driver/**`, `/manager/**`, `/api/hub/**`, and `/api/driver/**` for the relevant role gates; Phase 03 extends these handlers but does not collapse the chains. [VERIFIED: src/main/java/vn/iotstar/config/SecurityConfig.java:43-85]

Use PRG after every browser command. Return safe, role-appropriate blocked/forbidden feedback; let the existing JSP/API exception handlers translate access failures rather than leak SQL, custody, or route-rule details. [CITED: docs/ENGINEERING_STANDARDS.md §§2.10, 3.9, 4, 6, 8]

Current STOMP is same-origin, authenticated, private, and post-commit, but it permits only `/user/queue/shipments`; Phase 03 needs a deliberate subscription authorization extension for assigned driver and hub-scoped recipients. Do not open wildcard destinations or client `SEND` commands—the interceptor currently rejects them. [VERIFIED: src/main/java/vn/iotstar/config/WebSocketConfig.java:20-55]

Recommended destination policy (exact strings are discretionary): resolve recipients server-side, send only safe DTOs after commit, and authorize subscribe by actor role/hub/assignment. D-13 permits only accepted-handover, destination-arrival, inbound-ready-container, new-incident, and blocked-route messages. [VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:30, 42-44]

## Current Reusable Source and Drift

| Source | Reuse | Drift / planner action |
|---|---|---|
| `SecurityConfig` | Two ordered security chains and role route gates | Preserve session+CSRF for JSP and JWT stateless API; add only scoped handlers/endpoints. [VERIFIED: src/main/java/vn/iotstar/config/SecurityConfig.java:43-85] |
| `AuthenticatedActor` | Server-derived user ID/name/role set | It currently lacks employee profile, hub, position, task/route context; add a service-owned resolver/policy instead of trusting request values. [VERIFIED: src/main/java/vn/iotstar/security/AuthenticatedActor.java:8-17] |
| `ShipmentServiceImpl` | Constructor injection, service transaction, ownership check, after-commit publish | It only persists minimal creation and sender-owned detail; do not retrofit all operations into one class without focused interfaces/services. [VERIFIED: src/main/java/vn/iotstar/service/impl/ShipmentServiceImpl.java:24-70] |
| `Shipment` / `ShipmentEvent` | Existing canonical enum, SQL Server mappings, actor/timestamp event record | Shipment lacks operational hub/location fields/mutators/locking; event lacks hub/reference support in JPA despite canonical schema. Add migrations/entities deliberately. [VERIFIED: src/main/java/vn/iotstar/entity/Shipment.java:21-118; VERIFIED: src/main/java/vn/iotstar/entity/ShipmentEvent.java:19-69] |
| `docs/schema.sql` | SQL Server DDL for hubs, profiles, shipments, routes, trips, containers, handovers, incidents | It omits canonical `hub_routing_rules` and still declares `shipment_events.status`; require forward-only SQL Server migration(s), including the Phase-01 `event_type` reconciliation prerequisite. This is source drift, not a canonical conflict. [VERIFIED: docs/schema.sql:44-69, 107-179, 236-309; VERIFIED: src/main/resources/db/phase-01-shipment-events-event-type.sql:3-28] |
| STOMP publisher/config | Authenticated endpoint, private queue, after-commit DTO delivery | Generalize recipient targeting without weakening current same-origin/authenticated/no-client-send safeguards. [VERIFIED: src/main/java/vn/iotstar/config/WebSocketConfig.java:20-55; VERIFIED: src/main/java/vn/iotstar/realtime/StompShipmentEventPublisher.java:11-35] |

## Common Pitfalls

### Pitfall 1: letting a retry create duplicated custody evidence

**What goes wrong:** repeated sorter or scan submissions append routes/events/items twice.  
**Avoid:** protect the aggregate with locking plus a database uniqueness/idempotency guard; return the existing successful result. [VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:23]

### Pitfall 2: closing a trip on driver arrival

**What goes wrong:** custody is marked complete before the destination team physically receives/unbags containers.  
**Avoid:** arrival is only a predecessor; destination inbound reconciliation produces `COMPLETED` or `COMPLETED_WITH_DISCREPANCY`. [VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:34-35; CITED: docs/UTEExpress_Workflow.md §37]

### Pitfall 3: mutating an accepted handover

**What goes wrong:** silent item/container edits destroy the audit trail and make expected counts meaningless.  
**Avoid:** seal container membership on handover; correction is auditable cancellation/recreation. [VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:27-28]

### Pitfall 4: treating a missing parcel as a global stop

**What goes wrong:** all valid parcels stay blocked or a new shipment status is invented.  
**Avoid:** receive valid items and continue them; mark only the missing item `MISSING`, place only that shipment on hold/in transit, and create a `MISSING_PARCEL` incident. [CITED: docs/UTEExpress_Workflow.md §§37-38]

### Pitfall 5: role-only or broadcast authorization

**What goes wrong:** another driver/hub sees or operates a handover/trip, or STOMP reveals internal custody details.  
**Avoid:** check role + active profile + hub + position + assigned resource + shipment location at service and subscription boundaries. [CITED: docs/ARCHITECTURE_DECISIONS.md ADR-015; VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:30, 37, 41-44]

## Code Examples

The following is an implementation skeleton, not a replacement canonical enum or a client-authorized workflow. Names are discretionary. [ASSUMED]

```java
@Transactional
public SortResult sort(SortCommand command) {
    AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
    ScopedEmployee employee = scope.requireHubPosition(actor, "SORTER", command.hubId());
    Shipment shipment = shipments.lockByTrackingNumber(command.trackingNumber())
            .orElseThrow(() -> new ResourceNotFoundException("Shipment"));
    scope.requireShipmentAtHub(shipment, employee.hubId());
    state.requireAtHub(shipment);

    RouteDecision existing = routeDecisions.findByShipmentId(shipment.getId()).orElse(null);
    if (existing != null) return SortResult.existing(existing);

    HubRoutingRule rule = rules.findActiveMatch(employee.hubId(), shipment.destinationRegion(), shipment.destinationProvince())
            .orElseThrow(() -> new BlockedRouteException());
    RouteDecision decision = routes.createNextHop(shipment, rule.nextHubId());
    events.append(shipment, employee.hubId(), actor.userId(), "SORTING_COMPLETED", decision.reference());
    publisher.publishAfterCommit(recipients.forBlockedOrReady(decision), SortReadyEvent.from(decision));
    return SortResult.created(decision);
}
```

The skeleton demonstrates the required ordering: scope → locked aggregate → idempotent existing result → static-rule lookup → atomic route/event → after-commit safe notification. The event value `SORTING_COMPLETED` is canonical; all other names shown are assumed implementation identifiers. [CITED: docs/UTEExpress_Workflow.md §§14, 34, 39, 85.1; ASSUMED]

## State of the Art

| Old / invalid approach | Current required approach | Impact |
|---|---|---|
| Route selected manually by staff or algorithmically optimized | Static `hub_routing_rules` next-hop matrix | Preserves canonical routing authority and auditability. [CITED: docs/ARCHITECTURE_DECISIONS.md ADR-005] |
| Operational labels encoded as shipment statuses | Exactly 13 statuses plus `shipment_events.event_type` | Prevents incompatible state proliferation. [CITED: docs/ARCHITECTURE_DECISIONS.md ADR-003/ADR-014] |
| One generic operations dashboard / role-only access | Dedicated workspaces with contextual scope | Limits cross-hub and cross-driver data/action leakage. [VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:39-44] |
| Driver arrival completes custody | Destination inbound scans/unbags/reconciles | Retains physical receipt evidence and discrepancy isolation. [CITED: docs/UTEExpress_Workflow.md §37] |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|---|---|---|
| A1 | The planner may use JPA pessimistic locking or an added `@Version` mechanism for operations aggregates, provided the chosen SQL Server behavior is tested. | Data Model and SQL Server Plan | An untested locking choice could fail to prevent duplicate scans. |
| A2 | A route-decision idempotency record/key or a compatible unique constraint is needed because the canonical `shipment_routes` field list does not specify a retry key. | Data Model and SQL Server Plan | Duplicate routes/events/container membership could occur. |
| A3 | Exact new STOMP destination strings and DTO names remain discretionary. | JSP, API, STOMP, and Authorization Seams | A poor design could unintentionally broaden subscriptions. |
| A4 | The shown Java interfaces, exception class, and DTO names are illustrative only. | Code Examples | Planner must align names to existing project conventions. |

## Resolved Planning Decisions

1. **Static current-next-hop route materialization and idempotency — RESOLVED.**
   - Plan 03-01 persists exactly one current static next hop per sorter decision; it does not precompute or materialize undefined future hops. The migration adds a deterministic `route_decision_key` derived from shipment ID plus current hub ID and a SQL Server UNIQUE constraint on that key. When a shipment physically arrives at a later hub, its new current hub produces a separate next-hop decision. This is an implementation idempotency mechanism, not a new routing algorithm. [PLANNED: .planning/phases/03-hub-linehaul-operations/03-01-PLAN.md Task 1 and Task 3; CITED: docs/UTEExpress_Workflow.md §§39, 84-85.1]

2. **Manager subtree scope representation — RESOLVED.**
   - Plan 03-05 resolves Manager scope with a SQL Server recursive CTE anchored at the actor-derived profile hub and following `hubs.parent_hub_id` descendants; `hubs.level` remains the canonical hierarchy classification. The service rejects caller-selected scope, tests own/descendant/sibling/outside-subtree and inactive-profile cases, and gives Admin the explicit system-wide projection. [PLANNED: .planning/phases/03-hub-linehaul-operations/03-05-PLAN.md Task 1; CITED: docs/UTEExpress_Workflow.md §§19-25, 81]

## Environment Availability

| Dependency | Required By | Available | Version / evidence | Fallback |
|---|---|---|---|---|
| Maven / project test suite | automated unit/MVC/STOMP tests | ✓ | Existing Maven project and `spring-boot-starter-test` dependency. [VERIFIED: pom.xml:8-11, 143-149] | — |
| SQL Server provisioned DB plus `UTEEXPRESS_SQLSERVER_URL`, `UTEEXPRESS_SQLSERVER_USERNAME`, `UTEEXPRESS_SQLSERVER_PASSWORD`, and `UTEEXPRESS_JWT_SECRET` | real schema/migration/custody integration proof | ✗ in ordinary test environment | Phase 03 validation requires the SQL Server variables and the existing JWT secret for secured application-context tests. [PLANNED: .planning/phases/03-hub-linehaul-operations/03-VALIDATION.md] | No database substitute; provision SQL Server and secret. |
| Authenticated browser/STOMP client and seeded scoped staff data | manual UAT of queues and subscription scope | ✗ documented from Phase 01 | Phase 01 recorded manual UAT blocked without running SQL Server/seeds. [VERIFIED: .planning/phases/01-shared-walking-skeleton/01-03-SUMMARY.md:19-23] | Automated MockMvc/STOMP tests only; not full replacement. |

**Missing dependencies with no fallback:** provisioned SQL Server with canonical migrations and seeded hub/profile/driver/container test data. [CITED: docs/ARCHITECTURE_DECISIONS.md ADR-010]

## Validation Architecture

### Test Framework

| Property | Value |
|---|---|
| Framework | JUnit 5, AssertJ, Spring Boot Test, MockMvc, Spring Security Test. [VERIFIED: pom.xml:143-149; VERIFIED: src/test/java/vn/iotstar/integration/Phase01WalkingSkeletonTest.java:1-39] |
| Config file | `src/test/resources/application.properties`; real SQL Server profile `src/test/resources/application-sqlserver-verification.properties`. [VERIFIED: src/test/java/vn/iotstar/integration/ShipmentSqlServerIntegrationTest.java:23-28] |
| Quick run command | `rtk mvn -Dtest=HubRoutingServiceImplTest,ContainerHandoverServiceImplTest,LinehaulTripServiceImplTest,DestinationReceiptServiceImplTest test` [PLANNED: 03-VALIDATION.md] |
| Full suite command | `rtk mvn test` [PLANNED: 03-VALIDATION.md] |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|---|---|---|---|---|
| OPS-01 | hub/position scope, static rule match/no-match, idempotent sort | service + SQL Server + MVC/CSRF | `rtk mvn -Dtest=HubRoutingServiceImplTest,HubRoutingControllerSecurityTest,Phase03RoutingSqlServerIntegrationTest test` | ❌ Wave 0 |
| OPS-02 | inbound/sort/outbound events retain exactly 13 statuses | service + MVC/CSRF | `rtk mvn -Dtest=HubRoutingServiceImplTest,HandoverControllerSecurityTest,CanonicalDomainContractTest test` | ❌ Wave 0 / existing contract |
| OPS-03 | container/handover sealing and count reconciliation | transactional service + SQL Server + MVC/CSRF | `rtk mvn -Dtest=ContainerHandoverServiceImplTest,HandoverControllerSecurityTest,Phase03CustodySqlServerIntegrationTest test` | ❌ Wave 0 |
| OPS-04 | only assigned driver can accept/start/arrive allowed trip | service + JSP MVC/CSRF + JWT API security | `rtk mvn -Dtest=LinehaulTripServiceImplTest,LinehaulTripRouteSecurityTest,Phase03TripSqlServerIntegrationTest test` | ❌ Wave 0 |
| OPS-05 | missing item isolation and incident provenance/history | service + STOMP + SQL Server + JWT API security | `rtk mvn -Dtest=DestinationReceiptServiceImplTest,OperationalRealtimePublisherTest,OperationalWebSocketSecurityTest,Phase03ReceiptSqlServerIntegrationTest,OperationalIncidentApiSecurityTest test` | ❌ Wave 0 |

### Sampling Rate

- **Per task commit:** relevant focused Maven command above. [ASSUMED]
- **Per wave merge:** `rtk mvn test`. [PLANNED: .planning/phases/03-hub-linehaul-operations/03-VALIDATION.md]
- **Phase gate:** real SQL Server migration/integration command plus manual dual-account/STOMP UAT must pass before claiming custody/security proof. [VERIFIED: src/test/java/vn/iotstar/integration/ShipmentSqlServerIntegrationTest.java:23-28]

### Wave 0 Gaps

- [ ] SQL Server forward migration for `hub_routing_rules`, current schema reconciliation, indexes, checks/unique constraints, and migration idempotency verification.
- [ ] Hub/profile/position repositories and a contextual-scope policy/resolver with descendant-hub tests.
- [ ] Operations service, route, container, handover, trip, incident, and receipt tests listed above.
- [ ] JSP MVC/CSRF tests for each workspace and JWT API tests for driver/mobile actions.
- [ ] STOMP authorization and after-commit recipient-scope tests for the five D-13 messages.
- [ ] Fixture/seed strategy that creates two hubs, scoped staff, one assigned driver, a valid rule, an unmatched-route case, container counts, and missing-item case in SQL Server.

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---|---|---|
| V2 Authentication | yes | Preserve browser session authentication and stateless JWT API chains. [VERIFIED: src/main/java/vn/iotstar/config/SecurityConfig.java:43-85] |
| V3 Session Management | yes | Browser commands retain CSRF protection; API routes remain stateless. [VERIFIED: src/main/java/vn/iotstar/config/SecurityConfig.java:47-48, 67-68] |
| V4 Access Control | yes | Server-derived actor and service-level role + hub + position + assignment + location/resource checks. [CITED: docs/ARCHITECTURE_DECISIONS.md ADR-015] |
| V5 Input Validation | yes | Bean validation and server-side validation of scan codes, bounded notes, and IDs; treat all browser/API input as untrusted. [CITED: docs/ENGINEERING_STANDARDS.md §4] |
| V6 Cryptography | no new crypto | Reuse existing JWT/password mechanisms; never create a phase-specific crypto scheme. [VERIFIED: src/main/java/vn/iotstar/config/SecurityConfig.java:31-33] |

### Known Threat Patterns for this slice

| Pattern | STRIDE | Standard Mitigation |
|---|---|---|
| Driver manipulates another driver’s handover/trip ID | Elevation / Tampering | Lock resource and require assigned driver plus origins/destination and handover predecessor in service. [VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:37] |
| Hub staff submits another hub ID | Elevation / Tampering | Ignore caller hub scope; resolve active profile from actor and verify shipment/container current location. [CITED: docs/README.md §4] |
| Repeat scan races | Tampering / Repudiation | Database-backed idempotency + lock/version and immutable event evidence. [VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:23] |
| STOMP observer receives custody details | Information disclosure | Server-resolved private/hub-scoped destinations, DTO-only payloads, subscription authorization. [VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:30, 42-44] |
| Missing-parcel report hides or corrupts other parcels | Tampering / Availability | Destination evidence only; item-level `MISSING`, incident reference, valid items continue. [CITED: docs/UTEExpress_Workflow.md §§37-38] |

## Scope Fences

- Do not implement last-mile assignment/delivery attempt/outcome, return handling, COD, payment/refund, wallet/ledger, reconciliation, withdrawal, bank/VNPay, GPS, AI routing, or full cross-slice journeys. [VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:9-13, 106-111]
- Do not change the six-role model, add `ShipmentStatus` values, replace JSP, replace SQL Server, or introduce a manual/dynamic route algorithm. [CITED: docs/ARCHITECTURE_DECISIONS.md ADR-002/ADR-005/ADR-007/ADR-010/ADR-014]
- A missing parcel may use the canonical hold/incident workflow, but lost-item closure/financial compensation is Phase 04/05 integration work; Phase 03 stops at accountable incident evidence and operational isolation. [VERIFIED: .planning/phases/03-hub-linehaul-operations/03-CONTEXT.md:29, 106-111]

## Sources

### Primary (HIGH confidence)

- [docs/ARCHITECTURE_DECISIONS.md](../../docs/ARCHITECTURE_DECISIONS.md) — ADR-002, ADR-003, ADR-005, ADR-007, ADR-009 through ADR-015.
- [docs/UTEExpress_Workflow.md](../../docs/UTEExpress_Workflow.md) — §§12-18, 34-41, 74, 77-80, 85.1, 89-92, 98, 99.2, 102, 106-107.
- [docs/ERD.md](../../docs/ERD.md) — hub, route, operations, and incident relationships.
- [Phase 03 Context](03-CONTEXT.md) — locked D-01 through D-15 and scope fences.
- Current Phase-01 security, service, STOMP, entity, schema, and test sources cited inline.

### Secondary (MEDIUM confidence)

- [docs/PROJECT_SPEC_KNOWLEDGE_BASE.md](../../docs/PROJECT_SPEC_KNOWLEDGE_BASE.md) — existing Spring/JPA/JSP/JWT patterns, subordinate to ADR/Workflow.
- [docs/ENGINEERING_STANDARDS.md](../../docs/ENGINEERING_STANDARDS.md) — layering, validation, transaction, JSP, and security conventions.

## Metadata

**Confidence breakdown:**

- Standard stack: HIGH — fixed by ADRs and verified in the existing project configuration.
- Architecture: HIGH — constrained by D-01..D-15, Workflow, and verified Phase-01 seams.
- Pitfalls: HIGH — directly addressed by canonical custody, routing, state/event, and scope decisions.

**Research date:** 2026-10-12  
**Valid until:** 2026-11-11, unless the canonical docs or Phase-01 seams change.
