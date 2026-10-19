# UTEExpress Architecture Decisions

> **Authority:** This document has the highest authority among UTEExpress project documents. It resolves documented conflicts without changing business requirements.

## ADR-001 — Separate wallet entity

**Status:** ACCEPTED

**Decision:** Store each wallet in `wallets` with `user_id UNIQUE`, `balance`, `locked_balance`, `status`, and timestamps. Do not store `users.e_wallet` or `users.locked_balance`.

**Reason:** A wallet is a financial aggregate with its own lifecycle and transaction ledger.

**Consequences:** Refunds, COD payouts, compensation, fees, and withdrawal locks operate on `wallets.balance` and `wallets.locked_balance`.

## ADR-002 — Canonical roles and MVP role storage

**Status:** ACCEPTED

**Decision:** The canonical roles are `ROLE_USER`, `ROLE_SHIPPER`, `ROLE_HUB_STAFF`, `ROLE_LINEHAUL_DRIVER`, `ROLE_MANAGER`, and `ROLE_ADMIN`. MVP stores the role in `users.role`; do not create role/permission tables without a new Project Decision. `GUEST` is not stored as a role.

**Reason:** The Workflow defines six roles and uses position, hub scope, and assignment for further authorization.

**Consequences:** Generic example roles never become UTEExpress domain roles.

## ADR-003 — Shipment state and operational event are distinct

**Status:** ACCEPTED

**Decision:** `shipment_events` uses `event_type`, while `shipments.current_status` holds ShipmentStatus.

**Reason:** A shipment state is not an operational event; for example, `AT_HUB` and `ARRIVED_AT_HUB` answer different questions.

**Consequences:** Events such as sorting and bagging are not encoded as ShipmentStatus.

## ADR-004 — Workflow database sections are field-level authority

**Status:** ACCEPTED

**Decision:** The Database sections of `UTEExpress_Workflow.md` are the field-level source of truth. `ERD.md` is the relational and logical overview.

**Reason:** Mermaid diagrams intentionally show reduced fields to preserve readability.

**Consequences:** Coding work must not treat omitted Mermaid fields as absent columns.

## ADR-005 — Route planner uses hub routing rules

**Status:** ACCEPTED

**Decision:** Route Planner includes `hub_routing_rules`, mapping current hub and destination region/province to next hub.

**Reason:** This is the static routing matrix specified by the Workflow.

**Consequences:** The table configures a next hop; it does not introduce a new routing algorithm.

## ADR-006 — No shipper debt table in MVP

**Status:** ACCEPTED

**Decision:** Do not create a separate `shipper_debts` table in MVP. Financial discrepancies are represented by `shipper_reconciliation_items` plus `incidents` of type `MONEY_SHORTAGE` or `MONEY_OVERAGE`.

**Reason:** The existing reconciliation flow already identifies the responsible shipper and affected item.

**Consequences:** “Shipper debt” is workflow wording for financial responsibility, not a table name.

## ADR-007 — Primary frontend

**Status:** ACCEPTED

**Decision:** The primary frontend is JSP/JSTL with Bootstrap.

**Reason:** It is the frontend explicitly specified by the project Workflow.

**Consequences:** Do not silently replace it with a different rendering stack.

## ADR-008 — Service naming

**Status:** ACCEPTED

**Decision:** Use names such as `UserService` and `UserServiceImpl`; do not use `IUserService` by default.

**Reason:** This follows the engineering standard's Spring Boot convention.

**Consequences:** A different naming convention requires an explicit project decision.

## ADR-009 — Constructor injection

**Status:** ACCEPTED

**Decision:** Constructor injection is the default dependency-injection pattern.

**Reason:** It makes required dependencies explicit and supports testing.

**Consequences:** Field injection is not the default implementation pattern.

## ADR-010 — Implementation database

**Status:** ACCEPTED

**Decision:** SQL Server is the canonical implementation database for the current project.

**Reason:** This resolves the Workflow's multi-database technology notation for implementation planning.

**Consequences:** Database-specific implementation choices target SQL Server unless a new Project Decision changes this.

## ADR-011 — Core stack

**Status:** ACCEPTED

**Decision:** The core stack is Spring Boot, JPA, JSP/JSTL, JWT, and WebSocket.

**Reason:** These technologies are explicitly required by the project Workflow.

**Consequences:** Engineering guidance must support this stack rather than prohibit it solely because an individual lecture source omits it.

## ADR-012 — Lecture Ground Truth is non-exhaustive

**Status:** ACCEPTED

**Decision:** Lecture Ground Truth is authoritative but not exhaustive. Supplementary engineering knowledge may implement requirements that lecture materials do not define.

**Reason:** The project has explicit features beyond individual lecture examples.

**Consequences:** Supplementary knowledge must not override a higher-authority project decision.

## ADR-013 — `NOT_DEFINED_IN_SOURCE` is provenance

**Status:** ACCEPTED

**Decision:** `NOT_DEFINED_IN_SOURCE` means only that lecture materials do not define the item; it is not an implementation prohibition.

**Reason:** Provenance and permission are separate concerns.

**Consequences:** A Workflow-required WebSocket feature may be implemented with supplementary engineering knowledge.

## ADR-014 — ShipmentStatus set is fixed

**Status:** ACCEPTED

**Decision:** The 13 ShipmentStatus values in the Workflow are canonical. Do not add ShipmentStatus values for `SORTING`, `SORTED`, `PACKED`, `READY_FOR_OUTBOUND`, or `LAST_MILE_SORTING`.

**Reason:** These are operational activities and are captured as events.

**Consequences:** Implementations keep the shipment at its valid current state and append an appropriate `shipment_events.event_type`.

## ADR-015 — Contextual authorization

**Status:** ACCEPTED

**Decision:** When the relevant Workflow requires it, authorization evaluates role, hub scope, position, task assignment, and current shipment location/resource ownership.

**Reason:** Role alone cannot secure operational logistics actions.

**Consequences:** Service-layer authorization must use the applicable contextual checks.

## ADR-016 — Durable static-routing exceptions

**Status:** ACCEPTED

**Decision:** When static routing lookup has no single active rule, the system keeps the shipment's status and location unchanged and creates or reuses one deduplicated `routing_exceptions` record with `exception_type = ROUTE_LOOKUP_BLOCKED` and `status = OPEN`. A minimal `shipment_events.event_type = ROUTE_LOOKUP_BLOCKED` is appended only when the exception is newly opened. The same successful transaction emits an after-commit, Manager/Admin-scoped operational alert. No route, container assignment, handover, or manual next-hop selection is created by this exception path. When a later sorter retry succeeds after a routing-rule correction, the open exception is marked `RESOLVED`.

**Reason:** Operational routing failures require durable, queryable accountability and actionable supervision without becoming a fourteenth shipment state or bypassing the static routing matrix.

**Consequences:** Manager blocked-route KPIs count open `routing_exceptions`, not transient errors or all historical event rows. Retry remains static-rule-driven and idempotent; it does not authorize a user-selected next hop.

## OPEN-001 — Historical Issue: Missing Project Spec Knowledge Base source

**Status:** RESOLVED

**Issue:** `docs/PROJECT_SPEC_KNOWLEDGE_BASE.md` previously did not exist in the workspace, despite being required by the authority chain.

**Resolution:** `docs/PROJECT_SPEC_KNOWLEDGE_BASE.md` has been restored and is the canonical Core Lecture Ground Truth.

**Resolved by:** Documentation baseline update.

**Consequence:** Team members must read this file according to the current authority chain before planning or implementation.

## OPEN-002 — Legacy duplicate standards document

**Status:** RESOLVED (historical)

**Issue:** `docs/rule.md` was a legacy duplicate headed `LECTURE_STANDARDS.md` and contained older authority plus generic-domain wording.

**Audit:** The legacy file was compared with `docs/ENGINEERING_STANDARDS.md`, `docs/PROJECT_SPEC_KNOWLEDGE_BASE.md`, and this ADR register. Its technical rules were already represented by the current Engineering Standards and/or preserved as Core Lecture Ground Truth source examples. Its additional material was illustrative detail, repeated guidance, or superseded generic-domain examples such as `ROLE_VENDOR`; it did not define a unique, still-applicable canonical UTEExpress rule.

**Resolution:** Deleted `docs/rule.md`. No canonical rule was lost. `docs/ENGINEERING_STANDARDS.md` is the current engineering-rules source, and `docs/PROJECT_SPEC_KNOWLEDGE_BASE.md` is the current Core Lecture Ground Truth/source-example record.

**Resolved by:** Documentation baseline audit and consolidation.
