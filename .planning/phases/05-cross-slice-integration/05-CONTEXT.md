# Phase 05: Cross-slice Integration - Context

**Gathered:** 2026-10-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Integrate and verify the already-built intake, hub/linehaul, last-mile, and finance slices against the canonical workflow. Phase 05 proves the normal journey, required exception journeys, scoped realtime/tracking behavior, and Manager/Admin cross-slice views. It does not redesign slice business rules, add a new role/status/database, or perform Phase 06's exhaustive security/demo hardening.

</domain>

<decisions>
## Implementation Decisions

### Integration verification and fixtures

- **D-01:** Use real SQL Server automated journey suites for core end-to-end invariants. Manual UAT is limited to browser/JSP and authenticated STOMP proof. A skipped SQL Server test is `BLOCKED_BY_ENVIRONMENT`, never a pass. — **Reversibility:** costly — test topology, fixture ownership, and the evidence expected for the phase depend on a real SQL Server boundary.
- **D-02:** Provide idempotent local SQL Server seed/reset scripts for users, hubs, routing rules, prices, assignments, and financial fixtures. Scripts must not commit passwords, JWT secrets, or other credentials. — **Reversibility:** costly — deterministic fixtures are shared by all normal/exception journeys and manual UAT.
- **D-03:** Organize exception verification as separate custody, delivery/return, and finance test families that reuse one fixture builder. — **Reversibility:** reversible.

### Normal journey, tracking, and realtime

- **D-04:** For every normal handoff (intake → origin hub → linehaul → destination hub → last mile → financial outcome), assert the canonical shipment status, `shipment_events.event_type`, custody/current hub, and relevant financial record. — **Reversibility:** costly — the end-to-end proof must expose integration regressions at the handoff that caused them.
- **D-05:** Build tracking timelines from persisted canonical event/audit records. The current shipment status is a snapshot and must not be used to reconstruct history.
- **D-06:** Tracking and STOMP use purpose- and actor-scoped minimum DTOs. They must not disclose full addresses, COD, balances, or task data outside authorized scope. — **Reversibility:** costly — broad payloads create privacy leakage and widen client contracts.
- **D-07:** Verify service-level transaction/idempotency behavior at cross-slice handoffs: a duplicate command must not create a second event, custody record, or financial movement. UI double-click prevention is not sufficient. — **Reversibility:** one-way — permitting duplicate persisted financial/custody facts corrupts the audit trail.
- **D-08:** In Phase 05, prove only private action-required STOMP messages plus Manager/Admin scoped views. The exhaustive authorization/WebSocket matrix belongs to Phase 06.

### Exception consistency

- **D-09:** Every expiry, delivery-failure/return, missing-parcel, COD-discrepancy, and compensation path must be transactionally atomic. If a state, custody, or financial precondition is invalid, persist no partial outcome and return a safe error. — **Reversibility:** one-way — atomicity protects persisted workflow and financial facts.
- **D-10:** Missing-parcel and COD-discrepancy proof uses the canonical incident or reconciliation item with type, actor, timestamp, and reference. Do not add ShipmentStatus values or a `shipper_debts` table. — **Reversibility:** one-way — the ADR fixes the data model and status/event distinction.
- **D-11:** Follow exception recovery through its canonical terminal outcome. Return routing/custody and wallet ledger/balance can change only through valid service actions, never direct demo adjustment. — **Reversibility:** costly — terminal recovery is the evidence that slice rules compose correctly.
- **D-12:** Verify expiry by seeding an already-expired fixture or using a controllable clock/scheduler seam in tests while executing the real business rule. Do not shorten production timers.

### Cross-slice management proof

- **D-13:** Manager and Admin aggregation is enforced at the Service boundary. A Manager sees only permitted hub/shipper operational scope; Admin sees only the canonical financial/administrative scope needed for the action. JSP visibility is never the authorization control. — **Reversibility:** one-way — broad data access leaks protected operational/financial data.
- **D-14:** Management proof uses KPI summaries and actionable work-item lists. Drill-down uses minimum DTOs and repeats service-level scope enforcement; do not expose entities or all financial/shipment details in one view.
- **D-15:** Private STOMP management notifications carry only an identifier, action type, and safe summary. Details are fetched through a subsequently scoped Service/API/view request.
- **D-16:** Test at least a cross-hub Manager denial and a non-Admin financial/administrative denial before any sensitive DTO or event detail is returned.

### the agent's Discretion

- Choose concrete fixture-builder, SQL Server script, DTO, test-class, dashboard layout, and STOMP destination names, provided they preserve the canonical workflow, contextual authorization, SQL Server-only constraint, and these decisions.
- Choose the exact normal and exception test decomposition, provided all `INTEG-01` through `INTEG-03` criteria are evidenced without pulling Phase 06 hardening into this phase.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Authority and workflow

- `docs/README.md` — canonical authority hierarchy; canonical documentation is target state and code drift cannot override it.
- `docs/ARCHITECTURE_DECISIONS.md` — ADRs covering six roles, 13 statuses, event/state separation, wallet/reconciliation, JSP/SQL Server, contextual authorization, and routing.
- `docs/UTEExpress_Workflow.md` — canonical intake, hub/linehaul, last-mile, return, COD, reconciliation, compensation, realtime, and reporting workflows.
- `docs/ERD.md` — canonical relationships/constraints for shipment, event, assignment, custody, incident, wallet, COD, reconciliation, and withdrawal data.

### Engineering constraints and source provenance

- `docs/ENGINEERING_STANDARDS.md` — controller/DTO/service/repository boundaries, transactions, exception handling, JSP/JSTL, testing, and data-scope rules.
- `docs/PROJECT_SPEC_KNOWLEDGE_BASE.md` — Core Lecture Ground Truth; source examples do not override higher-authority project decisions.

### Team and inherited phase contracts

- `docs/TEAM_OWNERSHIP.md` — vertical-slice ownership and mandatory cross-review responsibilities.
- `docs/TEAM_EXECUTION_PLAN.md` — team handoff, review, and Git-evidence expectations.
- `.planning/PROJECT.md` — canonical target state versus brownfield/current implementation distinction.
- `.planning/REQUIREMENTS.md` — `INTEG-01`, `INTEG-02`, and `INTEG-03` requirements.
- `.planning/ROADMAP.md` — Phase 05 goal, dependency, scope, success criteria, and Phase 06 boundary.
- `.planning/phases/01-shared-walking-skeleton/01-CONTEXT.md` — security, SQL Server, exceptions, and STOMP foundations.
- `.planning/phases/02-user-shipment-intake/02-CONTEXT.md` — intake, fee, payment, pickup/dropoff, ownership, recipient privacy contracts.
- `.planning/phases/03-hub-linehaul-operations/03-CONTEXT.md` — hub routing, event/custody, container/handover/trip, and operational incident contracts.
- `.planning/phases/04-last-mile-finance/04-CONTEXT.md` — delivery/return, COD, wallet ledger, reconciliation, withdrawal, finance/privacy contracts.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets

- `src/main/java/vn/iotstar/security/AuthenticatedActor.java` — server-derived actor seam for ownership, assignment, hub, and role data-scope checks.
- `src/main/java/vn/iotstar/config/SecurityConfig.java` and `src/main/java/vn/iotstar/config/WebSocketConfig.java` — session/JSP + JWT/API separation and secured STOMP infrastructure.
- `src/main/java/vn/iotstar/realtime/` — shipment, operational, and notification publishers for private action-required messages.
- `src/main/java/vn/iotstar/service/impl/ShipmentServiceImpl.java`, `HubOperationsServiceImpl.java`, `LinehaulTripServiceImpl.java`, `LastMileDeliveryServiceImpl.java`, `ShipmentFinanceServiceImpl.java`, `FinanceReconciliationServiceImpl.java`, `WalletLedgerServiceImpl.java`, and `WithdrawalServiceImpl.java` — established service boundaries to compose rather than bypass.
- `src/main/java/vn/iotstar/config/SchedulingConfig.java`, `IntakeTimeoutServiceImpl.java`, and `ReturnGracePeriodScheduler.java` — expiry/scheduling seams for deterministic verification.

### Established Patterns

- Controllers are DTO/presentation boundaries; services own transactions, authorization, state transitions, custody, and finance invariants; controllers never access repositories directly.
- Browser routes use session plus CSRF; `/api/**` JWT routes remain stateless; WebSocket authentication uses the established session security path.
- SQL Server is the only database. Real SQL Server verification is reported as blocked when environment variables are absent, never substituted with H2/MySQL/PostgreSQL.
- Shipment state is exactly the canonical 13-value model; operational history uses `shipment_events.event_type`.

### Integration Points

- Compose existing intake, operational, last-mile, finance, tracking, incident/reconciliation, realtime, and management-service paths through normal/exception journey tests.
- Extend existing tests and SQL Server reconciliation/integration scripts with deterministic fixtures and cleanup, not alternate schema or parallel models.
- Connect Manager/Admin JSP/workspace/reporting views to scoped service DTOs and private safe-summary notifications.

</code_context>

<specifics>
## Specific Ideas

### Execution clarification (2026-10-25)

- The user explicitly selected a fixed default return fee of **25,000 VND**, configurable through the existing logistics configuration. This is the authorized application policy for the repair; the Workflow's illustrative numbers alone were not used to invent a pricing formula. Preserve validation of configured values and settle any unpaid forward fee separately.
- The user confirmed a dedicated SQL Server test instance exists and will configure verification environment variables. Availability is not a passing test result; recheck the execution process and run the real suites before claiming database verification.

- The definitive normal-journey evidence is a sequence of persisted state/event/custody/financial assertions at every handoff, not merely an initial/final UI screen.
- Manual UAT is intentionally limited to the browser/session/STOMP proof that automated SQL Server tests cannot demonstrate as directly.
- Phase 05 should produce evidence suitable for merging the three vertical slices while leaving system-wide security-matrix and demo hardening work to Phase 06.

</specifics>

<deferred>
## Deferred Ideas

- Comprehensive authorization/WebSocket matrix and demo hardening — Phase 06.
- Live banking payout/refund provider integration, AI routing, realtime GPS, and new infrastructure — V2/out of scope.

</deferred>

---

*Phase: 05-cross-slice-integration*
*Context gathered: 2026-10-25*
