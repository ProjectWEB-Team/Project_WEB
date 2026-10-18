# Phase 01: Shared Walking Skeleton - Context

**Gathered:** 2026-10-01
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 01 creates a small, runnable end-to-end learning path for all three team members: JSP/Bootstrap → Controller → DTO → Service → ServiceImpl → Repository/JPA → SQL Server → shipment detail/tracking response or event. It establishes the minimum shared authentication, JWT, six-role, contextual-authorization, exception-handling, WebSocket/STOMP, and test foundations needed by later slices.

This is explicitly **not** an early implementation of pickup, hub sorting, linehaul, delivery, return, COD, reconciliation, or withdrawal. Those workflows remain Phase 02, 03, and 04 work.
</domain>

<decisions>
## Implementation Decisions

### Scope and canonical model

- **D-01:** The only business tracer is an authenticated user's minimal shipment creation followed by persisted detail/tracking readback and a demonstrable WebSocket event skeleton. It must cross every listed architectural layer.
- **D-02:** `ARCHITECTURE_DECISIONS.md` > Workflow > ERD > Knowledge Base > Engineering Standards > supplementary knowledge is binding. Brownfield source and codebase map are current-state evidence only; any mismatch is implementation work, never a replacement design.
- **D-03:** Implement the fixed six-role model only: `ROLE_USER`, `ROLE_SHIPPER`, `ROLE_HUB_STAFF`, `ROLE_LINEHAUL_DRIVER`, `ROLE_MANAGER`, and `ROLE_ADMIN`; no stored `GUEST`, no seventh role, and no role/permission tables. â€” **Reversibility:** one-way â€” changing role storage or database roles later requires a data migration and invalidates canonical ADR-002.
- **D-04:** The tracer persists a minimal canonical shipment and records operational creation/tracking activity through `shipment_events.event_type`. The project must define and enforce the entire canonical set of exactly 13 `ShipmentStatus` values now; it must not create convenience states for future operations. â€” **Reversibility:** one-way â€” changing a persisted enum/state contract requires data migration and breaks canonical ADR-003/014.
- **D-05:** Finance and routing workflows are out of this phase. Do not implement wallet operations, COD, reconciliation, withdrawal, hub hierarchy, `hub_routing_rules`, pickup, sorting, linehaul, delivery, or return. Later work must still use a separate `wallets` table, never wallet balances on `users`.

### Architecture and security baseline

- **D-06:** The tracer follows JSP/Bootstrap → Controller → request/response DTO → Service interface → ServiceImpl → Repository/JPA → SQL Server. Controllers contain HTTP/validation/view concerns only and never call repositories; business transactions and authorization checks live at the service boundary; use constructor injection.
- **D-07:** Preserve the existing dual security-chain direction: session/CSRF-protected JSP web routes and stateless JWT REST routes. Add a minimal canonical authentication/JWT path and align existing `hasRole("DRIVER")` drift to `ROLE_LINEHAUL_DRIVER`; do not treat `DRIVER` as a new role.
- **D-08:** Establish a contextual authorization seam. For the Phase-01 tracer, an authenticated user may create/read only their own shipment; later hub/position/assignment/location checks extend this service/policy boundary instead of scattering checks in controllers/JSP. â€” **Reversibility:** costly â€” bypassing this boundary would require later cross-layer refactoring and risks data-scope defects.
- **D-09:** Establish consistent exception translation for JSP and REST/API paths, including DTO validation failures and access denial, without exposing internal errors or copying business handling into controllers.

### Realtime, data verification, and team learning

- **D-10:** Add a secured WebSocket/STOMP skeleton and prove the shipment-creation event can be emitted or demonstrably wired. Full operational notification behavior belongs to later slices.
- **D-11:** SQL Server is the database target. The plan must include a reproducible database verification path for mappings, constraints, and persisted tracer readback; it must not silently substitute another project database. â€” **Reversibility:** costly â€” changing the database/migration path after persisted data exists requires migration and violates ADR-010.
- **D-12:** Primary ownership is A: domain/DTO/entity/repository baseline; B: security/JWT/RBAC/data-scope baseline; C: JSP/controller/WebSocket/exception-handling baseline. All tasks retain full vertical-path awareness, and mandatory cross-review is A reviews B, B reviews C, C reviews A.

### the agent's Discretion

- Choose the smallest Workflow/ERD-compliant field subset and canonical initial shipment status for the tracer; document the source reference and do not invent business workflow behavior.
- Choose the SQL-Server-compatible automated-test mechanism and concrete STOMP destinations/configuration, provided it preserves the locked stack, security, and verification requirements.
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Authority and fixed domain decisions
- `docs/README.md` §§1, 3–7 — authority order, fixed roles/state/event principles, authorization, supplementary-knowledge policy, and GSD planning rules.
- `docs/ARCHITECTURE_DECISIONS.md` ADR-001 through ADR-015 — wallet separation, role storage, state/event distinction, service naming, constructor injection, SQL Server, core stack, source provenance, fixed states, and contextual authorization.
- `docs/UTEExpress_Workflow.md` §§2–3, 74, 77–80 — six roles, authorization principle, exact ShipmentStatus/event distinction, WebSocket events, public tracking, and web/API security matrix.
- `docs/ERD.md` §§1–2 — `users`, `shipments`, `shipment_events`, and their canonical relationships; consult §§3–4 only to preserve later-slice boundaries.

### Engineering implementation rules
- `docs/ENGINEERING_STANDARDS.md` §§1.1–1.8 — layered request flow, controller/service/repository boundaries, DTOs, constructor injection, and Spring Boot flow.
- `docs/ENGINEERING_STANDARDS.md` §§2.10, 4, 6, 7.2, 8 — exception handling, validation, JSP/JSTL, JPA transactions, authentication/authorization, and data scoping.
- `docs/PROJECT_SPEC_KNOWLEDGE_BASE.md` Chapters 3–6 and 8–9 — JPA, DTO, JWT, REST/JSP/AJAX source examples, `NOT_DEFINED_IN_SOURCE` provenance, and source-conflict handling. Examples remain SOURCE EXAMPLE where ADRs differ.

### Team and planning boundaries
- `docs/TEAM_OWNERSHIP.md` Phase 01 and cross-slice review sections — shared ownership and mandatory A→B→C→A cross review.
- `.planning/PROJECT.md` — canonical target versus brownfield current state and known `ROLE_DRIVER` implementation drift.
- `.planning/REQUIREMENTS.md` SKEL-01 through SKEL-05 — Phase 01 requirements.
- `.planning/ROADMAP.md` Phase 01 — fixed goal, success criteria, and exclusion of Phase 02–04 workflows.
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `src/main/java/vn/iotstar/UteExpressApplication.java` — Spring Boot/WAR entry point.
- `src/main/java/vn/iotstar/controller/HomeController.java` and `src/main/webapp/WEB-INF/views/` — existing JSP entry page and layout conventions.
- `src/main/java/vn/iotstar/config/SecurityConfig.java` — existing web/API security-chain starting point and explicit role-name drift to repair.
- `pom.xml` — already declares Spring MVC, JPA, Security, Validation, WebSocket, SQL Server JDBC, JSP/JSTL, and test foundations.
- `src/test/java/vn/iotstar/controller/HomeControllerTest.java` — existing MockMvc testing pattern.

### Established Patterns
- Existing web traffic is session-capable with CSRF enabled; `/api/**` is stateless and CSRF-disabled.
- Current project has only HomeController/JSP/bootstrap/security chains; entities, DTOs, services, repositories, persistence, JWT implementation, domain authorization, and WebSocket implementation are absent.

### Integration Points
- Extend the current JSP layout and controller convention for the tracer create/detail pages.
- Evolve rather than bypass `SecurityConfig`, preserving web/API chain separation while replacing `DRIVER` with the canonical linehaul role.
- Add the first domain path beneath the existing `vn.iotstar` package structure; do not introduce a competing architecture.
</code_context>

<specifics>
## Specific Ideas

All three team members must be able to explain the complete request path:

```text
Browser → JSP/AJAX → Controller → DTO → Service → Repository → SQL Server → response/event
```

The persisted tracer is intentionally a teaching/architecture proof, not a compressed version of Phase 02–04 functionality.
</specifics>

<deferred>
## Deferred Ideas

- Phase 02: account/intake, fee/payment initiation, HOME_PICKUP, SELF_DROPOFF, and recipient tracking/privacy flows.
- Phase 03: hub hierarchy, routing rules, inbound/sorting/outbound, containers, handover, trips, and incidents.
- Phase 04: last-mile delivery, returns, wallets/ledger operations, COD, reconciliation, and withdrawal.
- Phase 05: normal/exception cross-slice integrations and manager reporting.
- Phase 06: full authorization/UAT/demo hardening beyond the Phase-01 baseline.
</deferred>

---
*Phase: 01-shared-walking-skeleton*
*Context gathered: 2026-10-01*
