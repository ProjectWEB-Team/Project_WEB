---
milestone: v1
audited: 2026-10-25
scope: "Remote Phase 02, Phase 03, and Phase 04 branches before integration"
status: gaps_found
branches:
  phase_02: "origin/feature/phase-02-intake @ 6e2c3f6"
  phase_03: "origin/feature/hub-linehaul @ 03dd999"
  phase_04: "origin/feature/phase-04-last-mile-finance @ 8f7bf46"
scores:
  requirements: "0/16 formally integration-verified"
  phases: "0/3 formally verified"
  integration: "0/3 merge-ready paths"
  flows: "0/2 end-to-end paths"
gaps:
  integration:
    - id: "MERGE-01"
      severity: "critical"
      issue: "The three independently-developed branches have unresolved source-level merge conflicts in shared domain/configuration classes."
    - id: "MERGE-02"
      severity: "critical"
      issue: "Phase 03 Incident mapping conflicts with the canonical incidents schema; Phase 04 mapping is materially closer to ERD/Workflow."
    - id: "MERGE-03"
      severity: "high"
      issue: "Shipment, ShipmentAssignment, Payment, Hub, EmployeeProfile, ShipmentEvent and STOMP subscription contracts must be composed, not selected wholesale from one branch."
  verification:
    - id: "VERIFY-01"
      severity: "high"
      issue: "Phase 02 and Phase 04 have no GSD SUMMARY/VERIFICATION artifacts in their remote branches; Phase 03 has summaries but no 03-VERIFICATION.md."
---

> Status clarification (2026-10-26): This is historical verification evidence, not the current phase implementation status. The owner confirms Phase01–05 implementation complete. Remaining unobserved manual/realtime checks are carried into TV3 Phase05.1 and TV2 Phase06; original scores/results below are preserved and are not changed to PASS.

# v1 — Audit Phase 02–04 trước Integration Merge

## Verdict

`GAPS_FOUND` — source của cả ba phase **đã tồn tại trên GitHub**, nhưng chưa thể verify như một hệ thống vì chúng chưa được merge vào cùng integration tree. Không được merge tự động bằng chọn `ours`/`theirs`; có conflict vật lý và semantic contract cần giải theo canonical documentation.

## Branch evidence

| Phase | Remote branch | Commit | Source scope confirmed |
|---|---|---|---|
| 02 | `origin/feature/phase-02-intake` | `6e2c3f6` | registration/OTP, intake, pricing/payment, pickup/dropoff, receiver privacy/tracking, tests |
| 03 | `origin/feature/hub-linehaul` | `03dd999` | hub routing, custody/container, linehaul, destination receipt, operational events/dashboard, tests |
| 04 | `origin/feature/phase-04-last-mile-finance` | `8f7bf46` | last-mile, return, wallet, COD, reconciliation, withdrawal, notification, tests |

## Automated evidence

`feature/hub-linehaul` passed `mvn test` with 120 tests passing and 7 environment-gated SQL Server suites skipped. This does **not** prove Phase 02/04 or the merged product. A merged test run cannot start until the conflicts below are resolved.

## Pairwise merge conflicts

| Merge | Conflict files |
|---|---|
| Phase 03 + Phase 02 | `ARCHITECTURE_DECISIONS.md`, `EmployeeProfile.java`, `Hub.java`, `Shipment.java`, `ShipmentSqlServerIntegrationTest.java` |
| Phase 03 + Phase 04 | `WebSocketConfig.java`, `Incident.java`, `Shipment.java`, `ShipmentEvent.java` |
| Phase 02 + Phase 04 | `SchedulingConfig.java`, `Payment.java`, `Shipment.java`, `ShipmentAssignment.java`, `PaymentRepository.java`, `ShipmentAssignmentRepository.java`, `ShipmentRepository.java`, `application.properties`, `login.jsp`, `sidebar.jsp`, `create.jsp`, `detail.jsp`, `ute-express.css` |

## Canonical resolution rules

### CRITICAL — `Incident.java`

Phase 03 maps a separate operational-only schema (`transport_trip_id`, `actor_user_id`, `incident_type`, `note`). This conflicts with the canonical ERD/Workflow `incidents` schema: `shipment_id`, `trip_id`, `hub_id`, `type`, `description`, `reported_by`, `status`, `created_at`, `resolved_at`.

**Resolution:** retain the canonical shared incident schema used by Phase 04; migrate Phase 03 trip-delay/missing-parcel service logic to it. Keep both operational types and financial `MONEY_SHORTAGE`/`MONEY_OVERAGE`; do not create a second incidents table.

### HIGH — `Shipment.java`

All three branches add valid fields to the same aggregate. The merged entity must retain Phase 02 intake/recipient/pickup fields, Phase 03 hub association/routing fields, Phase 04 destination/direction/return/closure fields, and the fixed 13-status model. Phase 02/04 use hub ID columns while Phase 03 maps `current_hub_id` as a `Hub` association; services must be adapted to one authoritative JPA representation without duplicated column mappings.

### HIGH — shared contracts

- `Payment.java`: take the canonical financial lifecycle (including refund fields) while preserving Phase 02 shipping-fee creation/idempotent confirmation behavior.
- `ShipmentAssignment.java`: use the generic assignment representation required by pickup and last-mile/return; preserve valid pickup completion/cancellation behavior and Phase 04 acceptance timestamps.
- `Hub.java` and `EmployeeProfile.java`: use Phase 03's full hub hierarchy and profile mapping; adapt Phase 02 read-only scope logic rather than discarding hierarchy/position data.
- `ShipmentEvent.java`: keep canonical `event_type`; combine Phase 03 hub-bound operation helpers with Phase 04 generic event creation.
- `WebSocketConfig.java`: allow both scoped operational and notification destinations for an authenticated session. Do not loosen `/ws`, `SEND`, or subscription authorization.
- `application.properties`: retain environment-only datasource/JWT secrets and combine only non-secret feature settings.

## Workflow assessment

Each branch appears to target its assigned vertical slice. No seventh role or extra ShipmentStatus was found in this branch-level audit. The actual normal and exception journeys cannot be approved until the integration tree compiles and passes tests:

`intake → pickup/dropoff → origin hub → linehaul → destination hub → last-mile/return → payment/COD/wallet/reconciliation → tracking/realtime`

## Required next action

1. Create an integration branch from `feature/hub-linehaul`.
2. Merge Phase 02, then Phase 04, resolving every conflict by the canonical rules above; do not use blanket `ours` or `theirs`.
3. Run compile, targeted cross-slice tests, full `mvn test`, real SQL Server integration suite, and `git diff --check` on the resulting integration tree.
4. Create missing `02-VERIFICATION.md`, `03-VERIFICATION.md`, and `04-VERIFICATION.md`, then rerun this GSD audit before Phase 05.
