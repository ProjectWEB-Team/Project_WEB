---
phase: 03-hub-linehaul-operations
plan: "02"
subsystem: container-custody
status: blocked_pending_commit
requires:
  - Scoped hub routing from 03-01
provides:
  - Container-level shipment membership and outbound evidence
  - Assigned-driver, reconciled handover acceptance
  - Auditable cancellation and recreation custody correction
affects:
  - 03-03 assigned trip lifecycle
  - 03-04 destination receipt
key-files:
  created:
    - src/main/java/vn/iotstar/service/impl/ContainerHandoverServiceImpl.java
    - src/main/resources/db/phase-03-container-handover-schema.sql
    - src/test/java/vn/iotstar/service/ContainerHandoverServiceImplTest.java
  modified:
    - src/main/java/vn/iotstar/entity/ShipmentEvent.java
decisions:
  - Container handover accountability is at container level; driver scans never accept individual parcels.
  - Accepted or pending custody seals membership; correction creates a linked replacement instead of changing history.
  - Browser hub commands remain session/CSRF protected and the driver scan route uses the existing LINEHAUL_DRIVER guard.
metrics:
  tasks_completed: 3
  files_changed: 23
  test_results: "81 tests, 0 failures, 0 errors, 4 condition-skipped SQL Server tests"
actuals:
  tasks: 3
  commits: 0
---

# Phase 03 Plan 02: Container Custody and Handover Summary

Implemented the container-level outbound custody slice: an authorized outbound operator can form route-compatible containers, attach each eligible shipment once, and prepare a reconciled handover for one assigned driver.

## Completed Work

- Added `ParcelContainer`, composite-key `ContainerItem`, `Handover`, `HandoverContainer`, and the persistence placeholder for later `TransportTrip` lifecycle work.
- Added server-derived hub/position checks, shipment route compatibility, per-container active-custody checks, pessimistic resource lookup, and transaction-boundary service operations.
- Added `BAG_CREATED` and `OUTBOUND_SCANNED` operational evidence without adding any shipment status.
- Added driver-only CSRF-protected container scan acceptance. Expected counts are calculated by the service, and duplicate scans are idempotent.
- Added cancellation/recreation correction linkage that preserves the original handover while sealing active membership.
- Added SQL Server migration with foreign keys, unique constraints, count constraints, competing-handover and sealed-membership triggers.
- Added focused service, MVC/security, and real-SQL-Server-gated integration tests.

## Verification

- `rtk mvn -Dtest=ContainerHandoverServiceImplTest,HandoverControllerSecurityTest,CanonicalDomainContractTest test` — PASS: 12 tests, 0 failures, 0 errors, 0 skipped.
- `rtk mvn -Dtest=Phase03CustodySqlServerIntegrationTest test` — condition-skipped because required SQL Server/JWT environment variables are absent. `REAL_SQLSERVER_VERIFICATION: BLOCKED_BY_ENVIRONMENT`.
- `rtk mvn test` — PASS: 81 tests, 0 failures, 0 errors, 4 condition-skipped real SQL Server tests.
- `rtk git diff --check` — PASS.

## Deviations from Plan

### Auto-fixed Issues

1. **[Rule 2 - Authorization correctness] Moved driver acceptance to the existing driver-protected route family.**
   - A hub-scoped scan URL would have been rejected by the canonical `ROLE_LINEHAUL_DRIVER` web guard.
   - Added the small `DriverHandoverController` adapter at `/driver/handovers/scan`; it delegates all custody decisions to the same service.

## Scope Confirmation

No roles or `ShipmentStatus` values were added. No pickup, delivery, return, COD, wallet, reconciliation, withdrawal, or other Phase 02/04 behavior was introduced.

## Blocker

The implementation is not committed because the mandatory sandbox approval reviewer rejected the Git write request due to its invalid configured model (`gpt-5.6-luna`). The files remain intact and unstaged for the human checkpoint.

## Self-Check: PASSED

All Plan 03-02 implementation and test artifacts exist. Real SQL Server execution remains explicitly blocked by absent environment configuration.
