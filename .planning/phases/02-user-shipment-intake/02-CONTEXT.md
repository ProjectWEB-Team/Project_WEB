# Phase 02: User & Shipment Intake - Context

**Gathered:** 2026-10-12
**Status:** Ready for planning

<domain>
## Phase Boundary

Person A (Hoàng Phúc Khang) owns the complete sender/recipient and first-mile intake slice: registration/account access, full shipment creation, quote and payment initiation, `HOME_PICKUP`/`SELF_DROPOFF`, ownership-aware tracking, recipient auto-linking, and data masking. The slice extends the completed Phase-01 walking skeleton without redesigning the six-role model, the 13-state shipment model, `shipment_events.event_type`, or the SQL Server/JSP/JWT/WebSocket architecture.

This phase does not implement hub hierarchy/routing/custody/linehaul (Phase 03), delivery/returns/COD settlement/wallet/reconciliation/withdrawal (Phase 04), or live VNPay/bank integration (V2).
</domain>

<decisions>
## Implementation Decisions

### Canonical account and privacy rules

- **D-01:** A sender must have an account. Self-registration creates only `ROLE_USER`, follows phone OTP verification before becoming active, and uses the existing session/JWT authentication foundations. `ROLE_SHIPPER` and operational roles are not self-selectable. — **Reversibility:** one-way — changing persisted role assignment later needs a data migration and conflicts with ADR-002.
- **D-02:** A receiver account is optional. Shipment creation normalizes the receiver phone, auto-links `receiver_user_id` when a matching user exists, and always persists receiver name/phone/address snapshots so later profile edits do not rewrite shipment history. — **Reversibility:** costly — undoing link/snapshot semantics breaks privacy and historical-readback contracts.
- **D-03:** A linked recipient starts with masked sender/address/item information. Full detail unlock requires the full tracking number or receiver OTP; public tracking never exposes contacts, full addresses, employee/driver data, incidents, finance, or audit data. — **Reversibility:** costly — privacy leakage cannot be safely recalled once exposed.

### Intake wizard and payment path

- **D-04:** Shipment creation uses a server-validated JSP/Bootstrap multi-step wizard: sender/receiver information → parcel/service information → quote and fee-payer/payment selection → first-mile method. The final request still crosses Controller → DTO → Service → Repository/JPA → SQL Server. — **Reversibility:** costly — changing a public form flow affects validation, PRG, tests, and user-facing drafts.
- **D-05:** Phase 02 implements internal demo payment initiation: persist the canonical payment record and explicit payment-state transition, then allow a controlled demo confirmation. Do not integrate live VNPay or a bank provider, and do not use wallet settlement, which belongs to Phase 04. — **Reversibility:** costly — payment state/history must remain compatible with later real-provider and finance flows.
- **D-06:** The sender chooses `HOME_PICKUP` or `SELF_DROPOFF` at the final wizard step after payment handling, then receives the corresponding detail/instruction view. Timeout and cancellation follow the Workflow and record canonical reasons rather than creating new statuses. — **Reversibility:** costly — first-mile method changes affect task, deadline, event, and cancellation semantics.

### First-mile task boundary and public tracking

- **D-07:** Phase 02 includes a minimal coordinator action that assigns a pickup task to a `ROLE_SHIPPER`; a shipper can read and complete only its assigned pickup work. This is sufficient for the intake vertical slice and must not absorb Phase-03 hub hierarchy, routing, or advanced custody operations. — **Reversibility:** costly — assignment data scope is a shared contract with later operational slices.
- **D-08:** Public tracking is a separate guest search page/API: a visitor enters a tracking number and sees only canonical public tracking fields. Do not create a directly shareable tracking URL as the primary public boundary. — **Reversibility:** reversible.

### the agent's Discretion

- Choose exact JSP layout, DTO names, server-side validation grouping, demo-payment confirmation mechanics, and WebSocket event payloads, provided each preserves the locked Workflow, data-scope, and Phase-02 boundaries.
- Use the completed Phase-01 exception and STOMP seams rather than creating a competing architecture.
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Authority and intake workflow

- `docs/ARCHITECTURE_DECISIONS.md` ADR-002, ADR-003, ADR-004, ADR-007, ADR-009 through ADR-015 — roles, state/event separation, field-level Workflow authority, JSP stack, constructor injection, SQL Server, stack, and contextual authorization.
- `docs/UTEExpress_Workflow.md` Sections 5–8 — sender account, receiver account, auto-link, snapshots, and data masking.
- `docs/UTEExpress_Workflow.md` Sections 78–79 — public tracking fields and JSP/JWT route security matrix.
- `docs/UTEExpress_Workflow.md` database sections for `shipments`, `shipment_events`, `shipment_assignments`, `pickup_attempts`, and `payments` — field-level authority under ADR-004.
- `docs/UTEExpress_Workflow.md` first-mile, payment, timeout, cancellation, and demo-mode sections — canonical `HOME_PICKUP`/`SELF_DROPOFF`, payment, and deadline behavior.
- `docs/ERD.md` Sections 1–2 — users, shipments, events, assignments, and pickup-attempt relationships.

### Engineering and delivery constraints

- `docs/ENGINEERING_STANDARDS.md` Sections 1, 2.10, 4, 6, 7.2, and 8 — DTO/layering, exception handling, validation, JSP/JSTL, transactions, security, and data scope.
- `docs/PROJECT_SPEC_KNOWLEDGE_BASE.md` — Core Lecture Ground Truth/source examples only; it cannot override ADR or Workflow decisions.
- `docs/TEAM_OWNERSHIP.md` — Person A ownership and required vertical-slice/cross-review model.
- `docs/TEAM_EXECUTION_PLAN.md` — named ownership, handoff, review, and Git evidence plan.
- `.planning/REQUIREMENTS.md` INTAKE-01 through INTAKE-05 — Phase-02 requirements.
- `.planning/ROADMAP.md` Phase 02 — goal, success criteria, dependency, and owner.
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets

- `src/main/java/vn/iotstar/config/SecurityConfig.java` — Phase-01 dual JSP/session and `/api/**` JWT security chains.
- `src/main/java/vn/iotstar/security/AuthenticatedActor.java` — server-derived identity for service-level ownership/data-scope checks.
- `src/main/java/vn/iotstar/service/impl/ShipmentServiceImpl.java` — transactional shipment/event persistence and owner-only detail baseline.
- `src/main/java/vn/iotstar/config/WebSocketConfig.java` plus shipment event publisher — authenticated private-event seam.
- `src/main/webapp/WEB-INF/views/shipment/create.jsp` and `detail.jsp` — JSP/Bootstrap/CSRF/PRG conventions to extend rather than replace.
- Phase-01 security, service, MVC, SQL Server integration, and STOMP tests — regression baseline for all changes.

### Established Patterns

- Controllers remain DTO/presentation boundaries; services own transactions and authorization; repositories are never called directly from controllers.
- JSP browser routes use session + CSRF; REST API routes use JWT and remain stateless.
- SQL Server is the only project database; real-environment tests must not be substituted with H2/MySQL/PostgreSQL.

### Integration Points

- Extend the existing User, Shipment, ShipmentEvent, DTO, service, repository, JSP, security, exception, and STOMP seams from Phase 01.
- Preserve later Phase-03/04 schema boundaries when adding first-mile/payment records and events.
</code_context>

<specifics>
## Specific Ideas

- Khang owns the user-facing JSP/UI experience but implements its full vertical slice, including server-side DTO validation, security/data scope, persistence, events, and tests.
- The create-shipment flow should feel like a guided wizard rather than a single long form.
</specifics>

<deferred>
## Deferred Ideas

- Live VNPay/bank-provider integration — V2 only.
- Hub hierarchy, static routing rules, bag/container/custody/handover/trip/incident flows — Phase 03, owned by Vạn.
- Last-mile delivery, returns, COD settlement, standalone wallet/ledger, reconciliation, and withdrawal — Phase 04, owned by Phát.
</deferred>

---

*Phase: 02-user-shipment-intake*
*Context gathered: 2026-10-12*
