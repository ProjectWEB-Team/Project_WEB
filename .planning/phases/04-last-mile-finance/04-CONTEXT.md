# Phase 04: Last-mile & Finance - Context

**Gathered:** 2026-10-13
**Status:** Ready for planning

<domain>
## Phase Boundary

Person C — Huỳnh Tấn Anh Phát — owns the end-to-end last-mile and finance vertical slice: delivery-task custody and outcome recording, canonical retry/return handling, COD collection and payout, wallet/ledger mutations, shipper reconciliation, financial incidents, and withdrawal review. Every feature follows JSP/JSTL + Bootstrap → Controller → DTO → Service → Repository/JPA → SQL Server, with service-level contextual authorization and relevant private STOMP notification.

This phase extends the Phase-01 foundation but does not redesign the six roles, exactly-13 `ShipmentStatus` values, `shipment_events.event_type` model, SQL Server, or the dual session/JWT security architecture. It must not implement Phase-05 cross-slice orchestration, a real bank/VNPay payout provider, a `shipper_debts` table, broad operational broadcasts, or any new stored role.
</domain>

<decisions>
## Implementation Decisions

### Last-mile delivery, failure, and return

- **D-01:** Delivery failure uses the Workflow's two reason groups. Every failed attempt creates a `delivery_attempt` with reason and evidence; `NO_ANSWER`, `PAYMENT_NOT_AVAILABLE`, `RESCHEDULED`, and `OTHER` follow the 3-attempts/3-days rule, while `RECEIVER_REFUSED` and `WRONG_ADDRESS` fast-fail into `RETURN_TO_SENDER`. Do not add a ShipmentStatus for sorting, attempts, or failure detail; retain the fixed 13 states and append operational events. — **Reversibility:** one-way — changing persisted reason/state semantics requires migration and breaks canonical Workflow tracking contracts.
- **D-02:** An assigned last-mile shipper must scan each shipment assigned to that shipper at the applicable delivery hub before it can enter `OUT_FOR_DELIVERY`. Role alone is insufficient: Service authorization combines role, hub, assignment, resource, and current state.
- **D-03:** If COD and/or receiver-paid shipping fee is due, the Service must record valid collection before `CONFIRM DELIVERY` changes the shipment to `DELIVERED`; the collection becomes an input to the shipper's reconciliation. — **Reversibility:** costly — reversing ordering permits an already-delivered shipment without a traceable financial obligation.
- **D-04:** Support both canonical return completion modes: sender collection at the origin hub after return-fee handling, and coordinator-assigned `direction=RETURN` home delivery. Both retain scope checks, events/audit, fee conditions, and only reach `RETURNED_TO_SENDER` when valid. — **Reversibility:** costly — omitting either Workflow path breaks the documented return contract.
- **D-05:** The Phase-04 schema includes a nullable `evidence_url`/attachment reference on `delivery_attempts` to meet the Workflow requirement for a failed-delivery verification photo. Persist a reference/metadata, not raw image binary in SQL Server; use a configurable implementation detail for storage. This fills a field-level canonical-source gap without changing business rules.

### COD, fees, refund, and compensation

- **D-06:** COD follows the canonical lifecycle `WAITING_COLLECTION → COLLECTED → RECONCILED → READY_FOR_PAYOUT → PAID_TO_SENDER`; use `HOLD_DISPUTE` for a discrepant item and `CANCELLED` where a return starts before collection. COD state is financial data, not a new ShipmentStatus. — **Reversibility:** one-way — this is a persisted lifecycle and payout contract.
- **D-07:** Enforce canonical liability: paid outbound shipping fee is not refunded after a shipment is in transit; sender pays return fee; when a receiver was to pay but refuses, sender owes the outstanding shipping fee plus return fee. Keep the shipment in `RETURN_TO_SENDER` and block handover until payment/approved exception conditions are met.
- **D-08:** Refund and compensation credit `wallets.balance` and append an atomic `wallet_transactions` ledger entry with `REFUND` or `COMPENSATION` and its canonical reference. Do not represent financial balance only on `shipments` or only as an event. — **Reversibility:** one-way — ledger/balance consistency is a canonical financial integrity contract.
- **D-09:** Every financial action is idempotent at the Service transaction boundary: validate permitted current state/reference, update balance or financial state, and append ledger/record atomically with appropriate concurrency protection. JSP button disabling is UX only, not financial protection.

### Wallet, reconciliation, and withdrawal

- **D-10:** Reconciliation discrepancy is resolved item by item. `MATCHED` items proceed to payout; each `DISCREPANCY` item is held in `HOLD_DISPUTE` and produces a `MONEY_SHORTAGE` or `MONEY_OVERAGE` financial incident. No `shipper_debts` table is permitted. — **Reversibility:** one-way — ADR-006 explicitly fixes the discrepancy model.
- **D-11:** A withdrawal request validates available balance and atomically moves `amount` from `wallets.balance` to `wallets.locked_balance`, creates `PENDING`, and appends `WITHDRAWAL_LOCK`. Insufficient available balance fails without a request. — **Reversibility:** one-way — removing the lock violates the anti-double-spending contract.
- **D-12:** Only `ROLE_ADMIN` reviews withdrawals. Approval decreases `locked_balance`, marks the request `COMPLETED`, and appends `WITHDRAWAL_DEDUCT`; rejection requires `rejection_reason`, restores `balance`, marks `REJECTED`, and appends `WITHDRAWAL_REFUND`. Demo approval does not call a real bank.
- **D-13:** Create a wallet lazily when a user first becomes eligible for a canonical financial action (for example COD payout, refund, compensation, or wallet payment). Enforce exactly one wallet per user; do not create wallets for every role indiscriminately.

### Workspaces, privacy, and realtime

- **D-14:** Provide dedicated JSP workspaces by operational responsibility. A shipper sees only assigned delivery/return tasks at the applicable hub; Manager sees only scoped hub/shipper work; Admin has only canonical financial review scope. Service-level policies enforce authorization; JSP is presentation only.
- **D-15:** STOMP delivers only private, action-required events to scoped recipients: assignment/task updates, wallet-owner payout/withdrawal status, and manager/admin review work. Do not broadcast shipment address, COD, or financial details to all staff. — **Reversibility:** costly — broad destinations can create unrecoverable privacy leakage.
- **D-16:** Apply least-privilege personal-data display. An assigned shipper receives only contact/address data needed for the active task; access ends when assignment/scope ends. Other lists, manager/admin screens, and realtime payloads expose only data required for their authorized operation.

### the agent's Discretion

- Choose concrete JSP layouts, DTO/service/repository names, attachment storage adapter, event destination names, validation messages, and safe notification payload shapes, provided they preserve the locked finance, privacy, role, and data-scope decisions.
- Choose SQL-Server-compatible locking/JPA implementation and focused automated/manual verification methods. The implementation must prove correctness under duplicate/concurrent financial action attempts without replacing SQL Server or bypassing the Service boundary.
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Authority and fixed domain rules

- `docs/README.md` — canonical authority hierarchy and target-state planning policy.
- `docs/ARCHITECTURE_DECISIONS.md` ADR-001, ADR-002, ADR-003, ADR-004, ADR-006, ADR-007, ADR-009 through ADR-015 — separate wallet, roles, state/event distinction, Workflow field authority, discrepancy model, JSP/SQL Server/stack, construction, and contextual authorization.
- `docs/UTEExpress_Workflow.md` Sections 44–48 — last-mile task, delivery, failure grouping, retry, and return workflow.
- `docs/UTEExpress_Workflow.md` Sections 50–70 and 73 — fee payer, refund, return fee, reconciliation, payout, withdrawal, wallet design, and COD lifecycle.
- `docs/UTEExpress_Workflow.md` Sections 88, 95–97.1 — delivery attempt, COD, reconciliation, withdrawal, and wallet-ledger data definitions.
- `docs/UTEExpress_Workflow.md` Sections 77–80 — security and realtime route/event expectations.
- `docs/ERD.md` Sections 1–4 — `shipments`, assignments, delivery attempts, wallets, COD, reconciliation, withdrawal, and wallet transaction relationships.

### Engineering and source provenance

- `docs/ENGINEERING_STANDARDS.md` Sections 1, 2.10, 4, 6, 7.2, and 8 — layered DTO boundaries, validation, exception handling, JSP/JSTL, transactions, security, and data scope.
- `docs/PROJECT_SPEC_KNOWLEDGE_BASE.md` — Core Lecture Ground Truth/source examples only; it is authoritative but cannot override ADR/Workflow decisions or make sample naming a project rule.

### Team, planning, and inherited slice contracts

- `docs/TEAM_OWNERSHIP.md` — Person C vertical-slice responsibility and required cross-review model.
- `docs/TEAM_EXECUTION_PLAN.md` — named ownership, handoff, review, and Git evidence expectations.
- `.planning/PROJECT.md` — canonical target state versus brownfield current state.
- `.planning/REQUIREMENTS.md` LAST-01, LAST-02, FIN-01, FIN-02, FIN-03, FIN-04 — Phase-04 requirements.
- `.planning/ROADMAP.md` Phase 04 — phase goal, dependency, owner, scope, and success criteria.
- `.planning/phases/01-shared-walking-skeleton/01-CONTEXT.md` — existing security, service, SQL Server, exception, and STOMP foundations.
- `.planning/phases/02-user-shipment-intake/02-CONTEXT.md` — fee/payer/payment-initiation, assignment, ownership, and privacy contracts that Phase 04 must consume rather than redesign.
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets

- `src/main/java/vn/iotstar/config/SecurityConfig.java` — session/CSRF-protected JSP routes plus stateless JWT API routes; keep their separation.
- `src/main/java/vn/iotstar/security/AuthenticatedActor.java` — server-derived actor seam for every service-level assignment/hub/ownership policy.
- `src/main/java/vn/iotstar/service/impl/ShipmentServiceImpl.java` — established transaction, event-persistence, and owner-detail baseline to extend rather than bypass.
- `src/main/java/vn/iotstar/config/WebSocketConfig.java` and shipment event publisher — secured/private STOMP foundation for action-required events.
- `src/main/webapp/WEB-INF/views/` — Bootstrap, CSRF, JSP/JSTL, and PRG conventions for role workspaces.
- Phase-01 service, MVC, security, SQL Server integration, and STOMP tests — regression baseline for Phase-04 work.

### Established Patterns

- Controllers are DTO/presentation boundaries; Services own transactions, financial invariants, and authorization; controllers never call repositories.
- Browser routes use session + CSRF; REST `/api/**` routes use JWT and remain stateless.
- SQL Server is the only project database. Real SQL Server verification must be explicitly blocked if environment variables are unavailable, never replaced by H2/MySQL/PostgreSQL.

### Integration Points

- Extend the existing User, Shipment, ShipmentEvent, assignment, DTO, service, repository, exception, JSP, security, and WebSocket seams.
- Add canonical wallet/ledger/COD/reconciliation/withdrawal and last-mile records without duplicating Phase-02 intake or Phase-03 hub/linehaul responsibilities.
</code_context>

<specifics>
## Specific Ideas

- Evidence is stored as a delivery-attempt attachment reference, not an image blob in SQL Server.
- Financial correctness is more important than a quick UI shortcut: a valid transition, balance movement, and ledger entry must be one atomic Service operation.
- Workspace visibility and every STOMP recipient must follow the same contextual authorization rules as the HTTP action.
</specifics>

<deferred>
## Deferred Ideas

- Live bank payout, Open Banking, and live VNPay/refund integration — V2 only.
- End-to-end orchestration across the already-planned intake and operations implementations — Phase 05.
- System-wide security/UAT/demo completion work — Phase 06.
</deferred>

---

*Phase: 04-last-mile-finance*
*Context gathered: 2026-10-13*
