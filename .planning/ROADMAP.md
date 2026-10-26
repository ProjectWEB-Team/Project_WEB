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
- [ ] **Phase 02: User & Shipment Intake** — Person A owns account, intake, pickup/dropoff, and privacy-aware tracking flows.
- [ ] **Phase 03: Hub & Linehaul Operations** — Person B owns hub routing, custody operations, and linehaul flows.
- [ ] **Phase 04: Last-mile & Finance** — Person C owns last-mile, returns, wallet, COD, reconciliation, and withdrawal flows.
- [ ] **Phase 05: Cross-slice Integration** — Join the three vertical slices across normal and exception journeys.
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

**Plans:** 9 plans

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

**Plans:** 5 existing plans; summaries are present, formal `03-VERIFICATION.md` is still absent.

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

**Plans:** 10 plans

Plans:
**Wave 1**

- [ ] 04-01-PLAN.md — Assigned delivery scan, required collection, and delivery tracer.

**Wave 2** *(blocked on Wave 1 completion)*

- [ ] 04-02-PLAN.md — Evidence-backed failure, return-to-hub custody, reschedule, and retry.
- [ ] 04-04-PLAN.md — Source-specific cash reconciliation, COD payout, wallet ledger, and incidents.

**Wave 3** *(blocked on Wave 2 completion)*

- [ ] 04-03-PLAN.md — Fee-gated sender self-pickup and assigned RETURN delivery.
- [ ] 04-05-PLAN.md — Isolated SQL Server proof for reconciliation source and ledger invariants.
- [ ] 04-06-PLAN.md — Owner withdrawal lock, retry identity, and balance safety.

**Wave 4** *(blocked on Wave 3 completion)*

- [ ] 04-07-PLAN.md — Admin-only withdrawal approval and rejection.
- [ ] 04-08-PLAN.md — Reference-safe refund and compensation wallet adjustment seam.
- [ ] 04-09-PLAN.md — Least-privilege shipper, Manager, and Admin JSP workspaces.

**Wave 5** *(blocked on Wave 4 completion)*

- [ ] 04-10-PLAN.md — Private after-commit STOMP and final Phase-04 SQL/UAT evidence.

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

- [x] 05-01-PLAN.md — Intake destination, self-dropoff, safe tracking and scoped operations (66 focused tests passed; SQL/browser pending).
- [x] 05-02-PLAN.md — Linehaul/custody completion, missing recovery and vehicle/scan schema (41 focused tests passed; 4 SQL skips).
- [x] 05-03-PLAN.md — Initial assignments, return routing/custody/grace, collection and sender fee settlement (107 latest focused report cases passed; integration pending).
- [x] 05-04-PLAN.md — Refund settlement, authoritative reconciliation, compensation and private recipients (116 focused cases passed; integration pending).
- [ ] 05-05-PLAN.md — Migration manifest, shared SQL fixtures/journeys/concurrency and Jasper/regression implemented; real SQL acceptance passed; live browser/STOMP acceptance pending in 05-UAT.md.

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

**Plans:** TBD

## Progress

**Execution Order:** Phase01 → Phases02/03/04 → Phase05 source baseline → Phase05.1 (TV3 manual testing/review) and Phase06 (TV2) in parallel → user final review. Phase05.1 is a guide, not an automated acceptance gate for Phase06.

| Phase | Owner | Plans Complete | Status | Completed |
|-------|-------|----------------|--------|-----------|
| 01. Shared Walking Skeleton | Shared | 3/3 | Complete    | 2026-10-07 |
| 02. User & Shipment Intake | Person A | 0/9 summaries | Source merged; formal verification incomplete | - |
| 03. Hub & Linehaul Operations | Person B | 5/5 summaries | Source merged; formal verification incomplete | - |
| 04. Last-mile & Finance | Person C | 0/10 summaries | Source merged; formal verification incomplete | - |
| 05. Cross-slice Integration | Shared | Source repairs implemented; live observations pending | Preplan regression516/0skip, including50SQL; user final review pending | - |
| 05.1 Manual Functional Test Guide | TV3 | 0/1 | Guide prepared; TV3 performs and evaluates | - |
| 06. Security + UAT + Demo Hardening | TV2 | 0/TBD | Assigned parallel work; not executed in this planning turn | - |

---
*Roadmap created: 2026-10-01 from canonical documentation target state.*
