# Roadmap: UTEExpress

## Overview

This roadmap realizes the canonical UTEExpress target state without redesigning its domain, workflow, roles, state machine, wallet model, routing model, or database architecture. Phase 01 establishes a shared end-to-end walking skeleton; Phases 02, 03, and 04 then remain independent vertical slices for the three-person team. Phase 05 connects the implemented workflows. Phase 05.1 supplies one Spring Tools manual testing guide for Member 3; Member 2 works on Phase 06 in parallel. The user reviews both outputs last.

Canonical authority remains: `ARCHITECTURE_DECISIONS.md` > `UTEExpress_Workflow.md` > `ERD.md` > `PROJECT_SPEC_KNOWLEDGE_BASE.md` > `ENGINEERING_STANDARDS.md` > supplementary engineering knowledge. `.planning/codebase/` is current-state evidence only; it cannot override this roadmap.

## Phase Dependencies

```text
Phase 01 — Shared Walking Skeleton
    ├── Phase 02 — User & Shipment Intake (Person A)
    ├── Phase 03 — Hub & Linehaul Operations (Person B)
    └── Phase 04 — Last-mile & Finance (Person C)
                         |
                 Phase 05 source baseline
                    /                \
Phase 05.1 — TV3 manual review     Phase 06 — TV2 hardening/UAT/demo
                    \                /
                      User final review
```

Phases 02–04 depend only on Phase 01 and are deliberately designed to proceed in parallel. They are vertical slices—JSP/UI → Controller → DTO → Service → Repository/JPA → SQL Server → Security/Data Scope → WebSocket where needed → Tests—not frontend/backend/database allocations.

## Phases

- [x] **Phase 01: Shared Walking Skeleton** — Create the canonical cross-layer foundation and one auditable persisted shipment path. (completed 2026-10-07)
- [x] **Phase 02: User & Shipment Intake** — Person A owns account, intake, pickup/dropoff, and privacy-aware tracking flows.
- [x] **Phase 03: Hub & Linehaul Operations** — Person B owns hub routing, custody operations, and linehaul flows.
- [x] **Phase 04: Last-mile & Finance** — Person C owns last-mile, returns, wallet, COD, reconciliation, and withdrawal flows.
- [x] **Phase 05: Cross-slice Integration** — Join the three vertical slices across normal and exception journeys.
- [ ] **Phase 05.1: Manual Functional Test Guide (INSERTED)** — One Spring Tools guide; TV3 tests, judges results and lists inconsistencies without changing source.
- [ ] **Phase 06: Security + UAT + Demo Hardening** — Prove data scope, workflow invariants, test coverage, and canonical demos.

## Phase Details

### Phase 01: Shared Walking Skeleton

**Goal:** As a UTEExpress team member, I want to create and view a minimal persisted shipment through a shared secure walking skeleton, so that the team can verify the canonical architecture end-to-end before vertical slices begin.
**Mode:** mvp
**Depends on:** Nothing (first phase)
**Owner:** Shared — all three members agree interface/schema seams; no horizontal ownership split.
**Requirements:** SKEL-01, SKEL-02, SKEL-03, SKEL-04, SKEL-05
**Success Criteria** (what must be TRUE):

  1. A browser can traverse a minimal shipment path through JSP/UI, controller, DTO, service, repository/JPA, and SQL Server with automated tests.
  2. The six canonical roles are usable at the authentication boundary, and later phases have extension points for contextual data scope rather than a new role model.
  3. Persisted shipments accept only the fixed 13-status model, while operational actions are recorded using `shipment_events.event_type`.
  4. The common validation, error, audit, realtime-capability, and layered-architecture seams are available for every vertical slice.

**Plans:** 3/3 plans complete

Plans:

- [x] 01-01-PLAN.md — Person B: canonical authentication, JWT, role, and data-scope foundation.
- [x] 01-02-PLAN.md — Person A: authenticated persisted shipment create/detail tracer with SQL Server evidence.
- [x] 01-03-PLAN.md — Person C: STOMP/error completion, end-to-end verification, and cross-review sign-off.

### Phase 02: User & Shipment Intake

**Goal:** Deliver Person A's end-to-end sender/recipient and first-mile workflows without waiting for Person B or Person C's domain implementation.
**Mode:** mvp
**Depends on:** Phase 01
**Owner:** Person A — vertical slice ownership.
**Requirements:** INTAKE-01, INTAKE-02, INTAKE-03, INTAKE-04, INTAKE-05
**Success Criteria** (what must be TRUE):

  1. A sender or recipient can register/authenticate and access only the account and shipment data permitted by canonical ownership/privacy rules.
  2. A sender can create a fully valid shipment, see the defined fee/payment path, and choose HOME_PICKUP or SELF_DROPOFF.
  3. Authorized users/shipper can complete, cancel, or time out intake according to the defined pickup/dropoff workflow without invalid state transitions.
  4. A sender can track their shipment and an eligible recipient is auto-linked with canonical data masking; required updates can be received through the shared realtime seam.

**Plans:** 9/9 implementation plans complete; original per-plan summaries were not backfilled.

### Phase 03: Hub & Linehaul Operations

**Goal:** Deliver Person B's end-to-end controlled hub, route-planning, custody, and inter-hub transport workflows independently after the shared foundation.
**Mode:** mvp
**Depends on:** Phase 01
**Owner:** Person B — vertical slice ownership.
**Requirements:** OPS-01, OPS-02, OPS-03, OPS-04, OPS-05
**Success Criteria** (what must be TRUE):

  1. Authorized staff work only in their permitted hub scope and use `hub_routing_rules` to determine the canonical next hub.
  2. Inbound, sorting, and outbound actions add operational events and preserve the 13-status shipment state machine.
  3. Container, handover, trip, receipt, and unbagging actions retain accountable custody records across authorized actors.
  4. An assigned linehaul driver can run an allowed trip and authorized operations staff can record and investigate a missing/discrepant parcel without corrupting history.

**Plans:** 5/5 implementation plans complete; summaries are present. Missing historical phase verification documentation is tracked separately from delivered implementation.

### Phase 04: Last-mile & Finance

**Goal:** Deliver Person C's end-to-end last-mile, return, wallet, COD, reconciliation, and withdrawal workflows independently after the shared foundation.
**Mode:** mvp
**Depends on:** Phase 01
**Owner:** Person C — vertical slice ownership.
**Requirements:** LAST-01, LAST-02, FIN-01, FIN-02, FIN-03, FIN-04
**Success Criteria** (what must be TRUE):

  1. An assigned last-mile shipper can record only authorized delivery attempts and outcomes, including canonical retries and returns.
  2. Shipping-fee, COD, refund, return-fee, and compensation outcomes create traceable payment/COD records without bypassing canonical finance states.
  3. Wallet operations use the standalone `wallets` table and preserve `balance`/`locked_balance`, ledger traceability, and withdrawal anti-double-spending behavior.
  4. Reconciliation discrepancies use reconciliation items and financial incidents, not a `shipper_debts` table, and authorized staff can resolve the outcome.

**Plans:** 10/10 implementation plans complete; original per-plan summaries were not backfilled.

Plans:
**Wave 1**

- [x] 04-01-PLAN.md — Assigned delivery scan, required collection, and delivery tracer.

**Wave 2** *(original execution order; implementation complete)*

- [x] 04-02-PLAN.md — Evidence-backed failure, return-to-hub custody, reschedule, and retry.
- [x] 04-04-PLAN.md — Source-specific cash reconciliation, COD payout, wallet ledger, and incidents.

**Wave 3** *(original execution order; implementation complete)*

- [x] 04-03-PLAN.md — Fee-gated sender self-pickup and assigned RETURN delivery.
- [x] 04-05-PLAN.md — Isolated SQL Server proof for reconciliation source and ledger invariants.
- [x] 04-06-PLAN.md — Owner withdrawal lock, retry identity, and balance safety.

**Wave 4** *(original execution order; implementation complete)*

- [x] 04-07-PLAN.md — Admin-only withdrawal approval and rejection.
- [x] 04-08-PLAN.md — Reference-safe refund and compensation wallet adjustment seam.
- [x] 04-09-PLAN.md — Least-privilege shipper, Manager, and Admin JSP workspaces.

**Wave 5** *(original execution order; implementation complete)*

- [x] 04-10-PLAN.md — Private after-commit STOMP and final Phase-04 SQL/UAT evidence.

### Phase 05: Cross-slice Integration

**Goal:** As a UTEExpress user or operator with appropriate authorization, I want to integrate the three vertical slices across canonical normal and exception journeys with scoped operational and management views, so that shipments and financial outcomes remain consistent end-to-end.
**Mode:** mvp
**Depends on:** Phase 02, Phase 03, Phase 04
**Owner:** Shared cross-slice review — all three members retain their slice responsibility.
**Requirements:** INTEG-01, INTEG-02, INTEG-03
**Success Criteria** (what must be TRUE):

  1. A normal shipment completes from creation through hub/linehaul/last-mile delivery and its financial outcome, with coherent tracking and required realtime updates.
  2. The defined expiry, delivery-failure/return, missing-parcel, COD-discrepancy, and compensation journeys preserve state, event, custody, and wallet consistency end-to-end.
  3. Manager/admin operational and reporting views aggregate cross-slice information while retaining contextual data scope.

**Research baseline (2026-10-25):** `GAPS_FOUND` on merged `main@ca2bbf8`; `05-RESEARCH.md` and `05-ALIGNMENT-REVIEW.md` preserve that historical diagnosis. All five repair groups are now implemented in the working tree, including review corrections. Current evidence is recorded in `05-VERIFICATION.md` and `docs/PHASE_05_VERIFICATION.md`: 504 tests passed including 50 real SQL cases, zero skips/failures/errors. SQL acceptance passed; live browser/STOMP acceptance remains pending.

**Follow-up (2026-10-26):** Fresh pre-planning regression passed 516 tests including 50 real SQL cases, zero skips/failures/errors (`docs/PHASE_05_1_PREPLAN_REGRESSION.md`); the user confirmed creation/payment. Under the final team instruction, TV3 uses one Phase05.1 manual guide and judges findings while TV2 works on Phase06 in parallel. Original live observations remain pending; final review belongs to the user.

**Plans:** 5 sequential repair plans replace the earlier evidence-only drafts following the user's request to implement the research findings before starting another phase. Production fixes precede cross-slice verification.

Implementation summaries (checked items mean source repairs are implemented; they do not certify SQL/browser acceptance):

- [x] 05-01-PLAN.md — Intake destination, self-dropoff, safe tracking and scoped operations (historical focused result: 66 passed; later full SQL regression passed).
- [x] 05-02-PLAN.md — Linehaul/custody completion, missing recovery and vehicle/scan schema (historical focused result: 41 passed/4 SQL skips; later full regression: zero skips).
- [x] 05-03-PLAN.md — Initial assignments, return routing/custody/grace, collection and sender fee settlement (historical focused result: 107 passed; later SQL integration passed).
- [x] 05-04-PLAN.md — Refund settlement, authoritative reconciliation, compensation and private recipients (historical focused result: 116 passed; later SQL integration passed).
- [x] 05-05-PLAN.md — Migration manifest, shared SQL fixtures/journeys/concurrency and Jasper/regression implemented; real SQL acceptance passed. Unobserved browser/STOMP checks in 05-UAT.md are carried into TV3 Phase05.1 review and TV2 Phase06 hardening, not unfinished Phase05 implementation.

### Phase 05.1: Manual Functional Test Guide (INSERTED)

**Goal:** Provide exactly one practical Spring Tools manual testing guide for all implemented functions; Member 3 performs the tests, judges the outcomes and consolidates inconsistencies.
**Owner:** Person C — Member 3 (TV3).
**Requirements:** GUIDE-01
**Depends on:** Existing Phase05 implementation and available local environment/data. No dependency on Phase06 completion.
**Mode:** Manual guide; no source, test, script, build configuration or database schema changes. Findings are recorded, not repaired in this phase.
**Deliverable:** One guide, 05.1-01-PLAN.md, with STS startup, roles/prerequisites, URLs/actions/expected behavior and a blank result/findings template. Missing prerequisites are recorded by TV3 instead of bypassed.
**Evaluation:** TV3 decides pass/fail/not-tested and explains observations. Codex does not execute or grade this guide on TV3's behalf.
**Parallel work:** TV2 may run Phase06 at the same time. At completion of both workstreams, the user performs final review.
**Plans:** 1 manual testing guide; not executed.

- [ ] 05.1-01-PLAN.md — Run with Spring Tools; inspect current identity/intake/operations/delivery/finance/tracking/realtime behavior and record TV3's own findings.

### Phase 06: Security + UAT + Demo Hardening

**Goal:** Verify the canonical project behavior, close cross-cutting security and test gaps, and prepare the approved demo-mode scenarios.
**Mode:** mvp
**Depends on:** Existing Phase05 source implementation. May proceed in parallel with Phase05.1; no wait for TV3 assessment.
**Owner:** Person B — Member 2 (TV2). The user reviews the final result together with TV3 findings.
**Requirements:** HARD-01, HARD-02, HARD-03
**Success Criteria** (what must be TRUE):

  1. Protected UI/API/workflow actions enforce their required role, hub scope, position, assignment, location, and resource-ownership checks.
  2. Automated and manual UAT evidence covers vertical paths, state/event invariants, financial invariants, error paths, data scope, and applicable WebSocket behavior.
  3. The canonical primary and exception demo scenarios run under Workflow-permitted demo mode without changing real domain rules.

**Plans:** 7 plans (planned 2026-10-26; source work on branch `feature/phase-06-hardening` from `main`, planning artifacts stay on `project-plans`)

Plans:
**Wave 1**

- [ ] 06-01-PLAN.md — Route-level permission matrix: role x CSRF x anonymous over every controller route, API chain (D-03), completeness guard, findings (HARD-01).
- [ ] 06-02-PLAN.md — Service-layer matrix, ownership/finance/last-mile slice: shipment/tracking/fee/wallet/withdrawal/reconciliation/refund/delivery cells and findings (HARD-01).
- [ ] 06-03-PLAN.md — Maven-only multi-session STOMP delivery, rollback, payload and interceptor matrix; closes the technical part of 05-UAT item 3 (HARD-01, HARD-02).
- [ ] 06-04-PLAN.md — Demo flags `uteexpress.demo.fast-track-route-bypass` and `uteexpress.demo.auto-linehaul-transit-simulation`, inert outside the demo profile, with naming map (HARD-03).

**Wave 2** *(after 06-01 / 06-04)*

- [ ] 06-05-PLAN.md — Service-layer matrix, hub/custody/linehaul/manager/audit/first-mile slice, hub scope, position, assignment, location (HARD-01).
- [ ] 06-06-PLAN.md — Demo scenario script and checklist for Workflow sections 106 and 107 (HARD-03).

**Wave 3** *(after all)*

- [ ] 06-07-PLAN.md — Full regression, UAT coverage map and manual checklist (results NOT_RUN), findings gate, owner report and branch hand-off (HARD-01, HARD-02, HARD-03).

## Progress

**Execution Order:** Phase01 → Phases02/03/04 → Phase05 source baseline → Phase05.1 (TV3 manual testing/review) and Phase06 (TV2) in parallel → user final review. Phase05.1 is a guide, not an automated acceptance gate for Phase06.

| Phase | Owner | Implementation / deliverable | Status | Completion record |
|-------|-------|------------------------------|--------|-------------------|
| 01. Shared Walking Skeleton | Shared | 3/3 plans | Complete | 2026-10-07 |
| 02. User & Shipment Intake | Person A | 9/9 implementation plans | Complete | Owner-confirmed 2026-10-26; source merged earlier |
| 03. Hub & Linehaul Operations | Person B | 5/5 implementation plans | Complete | Owner-confirmed 2026-10-26; source merged earlier |
| 04. Last-mile & Finance | Person C | 10/10 implementation plans | Complete | Owner-confirmed 2026-10-26; source merged earlier |
| 05. Cross-slice Integration | Shared | 5/5 implementation plans | Complete | Owner-confirmed 2026-10-26; regression 516/50 SQL/0 skips |
| 05.1 Manual Functional Test Guide | TV3 | 1/1 guide delivered; manual review not executed by Codex | TV3 testing/assessment pending | User final review pending |
| 06. Security + UAT + Demo Hardening | TV2 | 0/7 plans (planned) | Planned; execution pending, may run in parallel with TV3 | User final review pending |

Phase01–05 are complete as implementation phases. Missing original summaries and unobserved manual/realtime checks do not reset their implementation status to Pending. Historical audits retain the results actually observed; TV3 and TV2 own the follow-up evidence. The original completion dates of Phase02–05 are not reconstructed; 2026-10-26 is the date of the owner's confirmation and this status correction.

Planning lives on `project-plans`. Synchronize source from `main` into `project-plans`; do not merge the planning branch back into `main`.

---
*Roadmap created: 2026-10-01. Status and branch correction: 2026-10-26.*
