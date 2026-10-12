# UTEExpress Team Execution Plan

## Purpose

This plan records the named three-person ownership model and the work boundaries used for Phase 02–04. It is evidence of collaboration planning; it does not change canonical domain decisions or authorize implementation outside an approved phase plan.

## Team allocation

| Member | Role | Phase | Owns |
|---|---|---|---|
| Hoàng Phúc Khang | Person A | Phase 02 — User & Shipment Intake | Account access, full shipment intake, fee/payment initiation, `HOME_PICKUP`, `SELF_DROPOFF`, user/public tracking, and privacy-aware recipient access. |
| Ngô Bá Vạn | Person B / leader | Phase 03 — Hub & Linehaul Operations | Hub hierarchy, `hub_routing_rules`, inbound/sorting/outbound events, custody/container/handover, trips, and operational incidents. Also keeps architecture and integration contracts aligned with canonical docs. |
| Huỳnh Tấn Anh Phát | Person C | Phase 04 — Last-mile & Finance | Delivery assignment/attempts, delivery failure/return, COD, reconciliation, standalone wallets, ledger, and withdrawal. |

Each owner works through the complete vertical path:

```text
JSP/JSTL + Bootstrap
→ Controller
→ DTO
→ Service / ServiceImpl
→ Repository / JPA
→ SQL Server
→ Security and Data Scope
→ WebSocket when the Workflow requires it
→ Automated tests and manual UAT
```

No member is assigned only frontend, backend, or database work.

## Phase order and parallelism

```text
Phase 01 — Shared Walking Skeleton (complete)
       ├── Phase 02 — Khang / Person A
       ├── Phase 03 — Vạn / Person B
       └── Phase 04 — Phát / Person C
                         │
                         └── Phase 05 — shared cross-slice integration
                                      │
                                      └── Phase 06 — shared hardening and demo
```

Phases 02–04 may be developed in parallel after Phase 01. Each person develops and tests their slice independently against the Phase-01 contracts; Phase 05 is the only phase that joins normal and exception flows end-to-end.

## Work boundaries

### Person A — Khang: Phase 02

- Implement account access only as allowed by the canonical six-role model; do not invent roles.
- Extend the minimal shipment tracer into full intake: sender/recipient, parcel, address, service, declared value, COD, fee, payer, and permitted payment initiation.
- Implement the Workflow-defined `HOME_PICKUP` and `SELF_DROPOFF` paths, including assignment, timeout, cancellation, and race-condition protection.
- Implement tracking/privacy behavior: sender ownership, eligible recipient linking, and required masking.
- Do not implement hub operations, delivery/returns, wallet, reconciliation, or withdrawal.

### Person B — Vạn: Phase 03

- Implement hub hierarchy and route lookup using `hub_routing_rules`; do not replace it with an invented routing model.
- Implement inbound, sorting, outbound, container, custody, handover, trip, destination-hub, and incident workflows as events/authorized transitions.
- Enforce canonical hub/position/assignment/location data scope at service boundaries.
- Maintain the canonical 13-status and `shipment_events.event_type` distinction; sorting/bagging/outbound do not create convenience statuses.
- Act as architecture coordinator: review shared contracts, resolve only through existing ADRs, and raise an unresolved canonical conflict before choosing a design.

### Person C — Phát: Phase 04

- Implement assigned last-mile work, delivery attempts, valid delivery/failure outcomes, and return workflow.
- Implement COD, payments, reconciliation, and financial incidents according to the Workflow.
- Implement standalone `wallets`, wallet transactions, `balance`, `locked_balance`, and withdrawal anti-double-spending rules.
- Do not store wallet values on `users` and do not introduce `shipper_debts`.

## Shared contracts and review cadence

| Contract | Owner | Reviewers | Rule |
|---|---|---|---|
| Canonical authority, cross-slice schema changes, API/DTO contracts | Vạn | Khang, Phát | ADR > Workflow > ERD > Knowledge Base > Engineering Standards. An unresolved conflict blocks the decision. |
| Intake-to-operations handoff | Khang | Vạn | A shipment enters operations only through canonical state/event history and must retain ownership/privacy evidence. |
| Operations-to-last-mile handoff | Vạn | Phát | Delivery work must be assignment-scoped, auditable, and based on canonical location/custody state. |
| Delivery/finance outcome handoff | Phát | Vạn, Khang | Financial records retain canonical payment/COD/wallet invariants and do not alter shipment history improperly. |

Mandatory cross-review remains:

```text
Khang (A) reviews Vạn (B)
Vạn (B) reviews Phát (C)
Phát (C) reviews Khang (A)
```

## GitHub evidence plan

Each member should keep their own vertical-slice work traceable in Git:

1. Work on a named feature branch for the owned phase.
2. Make small, descriptive commits that pair a vertical feature with its tests.
3. Open a pull request or retain a branch comparison for each completed phase plan.
4. Request the assigned cross-review before merging/integrating the slice.
5. Keep documentation-only coordination commits separate from application-code commits.

Suggested commit prefixes:

```text
docs(team): assign vertical-slice ownership
feat(intake): add canonical shipment intake step
feat(operations): add hub custody event flow
feat(finance): add wallet ledger transition
test(intake): cover recipient data scope
```

## Definition of done for each owner

A member's work is ready for cross-slice integration only when:

- the approved phase plan is complete without Phase-05 scope leaking in;
- controller, DTO, service, repository, SQL Server, security/data scope, and applicable WebSocket paths are covered;
- canonical roles, 13 statuses, `event_type`, wallet separation, and routing rules remain intact as applicable;
- automated tests and manual UAT evidence are available;
- the assigned cross-review is recorded; and
- the work is traceable through focused Git commits and a reviewable branch or pull request.

---

**Named team decision date:** 2026-10-12  
**Canonical references:** `docs/TEAM_OWNERSHIP.md`, `docs/ARCHITECTURE_DECISIONS.md`, `docs/UTEExpress_Workflow.md`, `docs/ERD.md`, and `docs/ENGINEERING_STANDARDS.md`.
