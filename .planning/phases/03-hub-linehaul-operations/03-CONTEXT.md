# Phase 03: Hub & Linehaul Operations - Context

**Gathered:** 2026-10-12
**Status:** Ready for planning

<domain>
## Phase Boundary

Person B (Ngô Bá Vạn) owns the complete Hub/SOC and linehaul vertical slice: hub-scope route lookup, inbound/sorting/outbound events, parcel containers, handovers, assigned driver trips, arrival reconciliation, and operational incidents. The slice extends the completed Phase-01 foundation and consumes a canonical first-mile shipment boundary without redesigning Phase-02 intake, Phase-04 delivery/finance, or the canonical domain.

This phase must follow JSP/UI → Controller → DTO → Service → Repository/JPA → SQL Server → contextual authorization/data scope → WebSocket where required → tests. It must preserve exactly six roles, exactly 13 `ShipmentStatus` values, and `shipment_events.event_type` for operational history.

This phase does not implement last-mile delivery, return, COD settlement, wallets, reconciliation, withdrawal, bank/VNPay integration, a new route algorithm, or a stored role beyond the six canonical roles.
</domain>

<decisions>
## Implementation Decisions

### Sorting and static route matrix

- **D-01:** Sorting derives `next_hub` only from canonical `hub_routing_rules` using current hub plus destination region/province. Hub Staff never choose a next hub manually. — **Reversibility:** costly — replacing the static canonical matrix changes the route-planning contract and later integration behavior.
- **D-02:** If no matching routing rule exists, block the scan and create neither route nor container assignment. Only the current Hub/SOC Manager and Admin see/configure the actionable error; Hub Staff receive safe blocked feedback only. — **Reversibility:** costly — allowing a manual fallback would bypass the canonical routing authority and corrupt custody routing.
- **D-03:** A successful sorter scan shows only next Hub/SOC, destination grouping, and container readiness. A repeated scan is idempotent: it creates no duplicate route, event, or container item and returns the existing route. — **Reversibility:** costly — duplicate operational records break event/audit and container-custody evidence.

### Containers, handovers, and discrepancy evidence

- **D-04:** Before accepting a handover, the driver scans every bag/container, reconciles `expected_containers` against actual scans, and the system records both actors and timestamps. Handover accountability is container-level, not individual-parcel-level. — **Reversibility:** costly — weakening the scan/reconciliation record defeats the canonical custody evidence.
- **D-05:** Once a container enters a handover it is logically sealed. Corrections require auditable handover cancellation/recreation, never direct silent addition/removal. — **Reversibility:** costly — post-handover mutation would make custody and event history unreliable.
- **D-06:** At destination unbagging, valid containers/parcels continue through the workflow. A confirmed missing item becomes `container_items.status = MISSING` with a `MISSING_PARCEL` incident; only that shipment follows its canonical exception path. Do not block every container or create a new ShipmentStatus. — **Reversibility:** costly — missing-parcel evidence, incident linkage, and the fixed state/event model are cross-slice contracts.
- **D-07:** Handover realtime is role- and hub-scoped: relevant Outbound Staff see acceptance, the assigned Driver sees assigned handover/trip updates, and destination Inbound Staff see arrival/readiness. Do not broadcast internal custody details broadly. — **Reversibility:** costly — broadening delivery would leak operational data across hub/assignment boundaries.

### Assigned linehaul lifecycle

- **D-08:** Driver lifecycle is explicit: Accept Handover → Start Trip → Arrive. Demo mode may shorten elapsed transit only where Workflow permits; it still records the same canonical events and audit trail. — **Reversibility:** costly — automatic completion would erase accountable custody transitions.
- **D-09:** Driver arrival does not finish physical receipt. Destination Inbound Staff must scan/unbag/reconcile containers before the handover/trip reaches its canonical completed or discrepancy outcome. — **Reversibility:** costly — unilateral driver completion conflicts with the canonical destination-receipt workflow.
- **D-10:** An assigned driver may report `TRIP_DELAY` with bounded evidence/note. `MISSING_PARCEL` is created only from destination inbound/unbag reconciliation evidence, not a driver assertion. — **Reversibility:** costly — incident provenance controls operational accountability.
- **D-11:** A linehaul driver sees and acts only on their assigned handovers/trips and may start only after acceptance, with allowed origin/destination hub scope. Role alone never grants access to another driver's work. — **Reversibility:** costly — this extends the canonical service-level contextual authorization seam.

### Position workspaces, realtime, and audit visibility

- **D-12:** Inbound, Sorter, Outbound, and Driver each use a dedicated workspace/queue. Managers use a separate supervisory dashboard. Views and actions enforce role, active position, hub scope, assignment, shipment location, and resource ownership at the service boundary. — **Reversibility:** costly — a shared unrestricted operations surface would require later security/data-scope redesign.
- **D-13:** STOMP/WebSocket sends only action-required events: accepted handover, destination arrival, inbound-ready container, new incident, and blocked route. It does not publish every scan/sort/container change. — **Reversibility:** reversible — event subscription/payload presentation can evolve while keeping authorization intact.
- **D-14:** The Manager dashboard prioritizes scoped hub/SOC KPIs: inbound/outbound, sorting backlog, active handovers/trips, blocked routes, and actionable incidents. It is not a global unrestricted dashboard. — **Reversibility:** reversible — dashboard layout can change without changing data scope.
- **D-15:** Authorized shipment detail exposes a scoped audit timeline with event type, time, hub, actor role, and container/handover/trip reference. It does not expose data outside the viewer's scope. — **Reversibility:** costly — audit disclosure rules are an operational privacy/security contract.

### the agent's Discretion

- Choose JSP layout, DTO/entity/service/repository names, route prefixes, SQL Server-compatible locking strategy, and exact STOMP destinations/payloads, provided they preserve the locked Workflow, data scope, and existing Phase-01 security/realtime seams.
- Use canonical event and incident names/fields from Workflow/ERD; use supplementary engineering knowledge only to fill technical gaps and never to override canonical business decisions.
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing. Canonical documentation is the target state; brownfield source and `.planning/codebase/` describe current state only and may not override it.**

### Authority and operating workflow
- `docs/README.md` — canonical authority order and planning rule.
- `docs/ARCHITECTURE_DECISIONS.md` ADR-002, ADR-003, ADR-005, ADR-007, ADR-009 through ADR-015 — six roles, state/event separation, static `hub_routing_rules`, JSP stack, SQL Server, fixed statuses, and contextual authorization.
- `docs/UTEExpress_Workflow.md` §§12–18, 34–41, 77–80 — hub positions, inbound/sorting/outbound, containers, handovers, driver workflow, destinations, WebSocket, and route/security behavior.
- `docs/UTEExpress_Workflow.md` §§35–39 and database sections 85.1, 89–92, 98 — static route matrix, container-level handover/reconciliation, `transport_trips`, and operational incidents.
- `docs/UTEExpress_Workflow.md` §§77–80 and §§1978–2025 — scoped realtime/security and exact 13-status/event-type separation.
- `docs/UTEExpress_Workflow.md` §§3020–3029 — immutable shipment snapshots and container-level, not parcel-level, linehaul handover accounting.
- `docs/ERD.md` §§1–3 — hubs, `hub_routing_rules`, employee profiles, `parcel_containers`, `container_items`, `handovers`, `handover_containers`, `transport_trips`, and `incidents` relationships.

### Engineering and delivery constraints
- `docs/ENGINEERING_STANDARDS.md` §§1, 2.10, 4, 6, 7.2, and 8 — DTO/layering, exception handling, validation, JSP/JSTL, transactions, security, and service-level data scope.
- `docs/PROJECT_SPEC_KNOWLEDGE_BASE.md` — Core Lecture Ground Truth/source examples; it never overrides ADR or Workflow decisions.
- `docs/TEAM_OWNERSHIP.md` — Person B Phase-03 ownership and required cross-review.
- `docs/TEAM_EXECUTION_PLAN.md` — vertical-slice, handoff, review, and Git-evidence expectations.
- `.planning/REQUIREMENTS.md` OPS-01 through OPS-05 — Phase-03 requirements.
- `.planning/ROADMAP.md` Phase 03 — goal, dependency, success criteria, and owner.

### Prior-phase contracts
- `.planning/phases/01-shared-walking-skeleton/01-CONTEXT.md` — dual security, `AuthenticatedActor`, service transaction/data-scope seam, exact role/status/event constraints, SQL Server, and secured STOMP foundation.
- `.planning/phases/02-user-shipment-intake/02-CONTEXT.md` — Phase-02 boundaries; hub acceptance, routing, container/custody, handover, trip, and incidents remain Phase 03.
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `src/main/java/vn/iotstar/config/SecurityConfig.java` — Phase-01 dual JSP/session and JWT API security chains to extend, not replace.
- `src/main/java/vn/iotstar/security/AuthenticatedActor.java` — server-derived identity seam for role/hub/position/assignment/location checks.
- `src/main/java/vn/iotstar/service/impl/ShipmentServiceImpl.java` — service transaction and ownership baseline to evolve into operational service boundaries.
- `src/main/java/vn/iotstar/config/WebSocketConfig.java` and existing shipment-event publisher — authenticated private STOMP seam.
- `src/main/webapp/WEB-INF/views/` and Phase-01 JSP routes/tests — JSP/Bootstrap, CSRF, PRG, MVC, exception, SQL Server, and STOMP conventions.

### Established Patterns
- Controllers are DTO/presentation boundaries; services own transactions and authorization; repositories are never called directly from controllers.
- JSP browser routes use session + CSRF; REST API routes use stateless JWT.
- SQL Server is the only database. Real environment verification cannot be substituted by H2, MySQL, or PostgreSQL.

### Integration Points
- Extend shipment/event, user/employee profile, service, repository, JSP, security, exception, and STOMP seams from Phase 01.
- Add canonical Phase-03 entities/relationships without replacing Phase-02 first-mile records or prematurely implementing Phase-04 finance/delivery.
</code_context>

<specifics>
## Specific Ideas

- Operations should be scan/work-queue oriented, not a generic unrestricted dashboard.
- Demonstrations retain real accountable transitions; allowed demo simulation changes elapsed time only, not the event/custody sequence.
</specifics>

<deferred>
## Deferred Ideas

- Phase 04 owns last-mile delivery, returns, COD settlement, wallet/ledger operations, reconciliation, and withdrawal.
- Phase 05 owns end-to-end cross-slice normal/exception integration beyond Phase-03 boundaries.
- Live payment/bank providers, GPS tracking, AI routing, and other V2 infrastructure remain out of scope.
</deferred>

---

*Phase: 03-hub-linehaul-operations*
*Context gathered: 2026-10-12*
