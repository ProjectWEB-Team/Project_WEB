---
phase: 05-cross-slice-integration
plan: "02"
subsystem: linehaul-custody-investigation
status: implemented_pending_integration
tags: [vehicle, manifest, custody, missing-parcel, investigation]
requires: [05-01]
provides: [vehicle-bound-trips, manifest-departure, terminal-destination-custody, incident-bound-recovery]
affects: [05-03, 05-04, 05-05]
tech-stack:
  added: []
  patterns: [pessimistic-locks, immutable-discrepancy-evidence, scoped-manager-decisions]
key-files:
  created:
    - src/main/java/vn/iotstar/entity/Vehicle.java
    - src/main/java/vn/iotstar/controller/MissingParcelController.java
    - src/main/java/vn/iotstar/dto/driver/VehicleOptionResponse.java
    - src/main/java/vn/iotstar/dto/hub/MissingParcelCaseResponse.java
    - src/main/java/vn/iotstar/dto/hub/MissingParcelResolutionCommand.java
    - src/main/resources/db/phase-05-custody-alignment.sql
    - src/main/webapp/WEB-INF/views/manager/missing-parcels.jsp
    - src/test/java/vn/iotstar/service/LinehaulCustodyCompletionTest.java
    - src/test/java/vn/iotstar/controller/MissingParcelControllerSecurityTest.java
  modified:
    - src/main/java/vn/iotstar/service/impl/LinehaulTripServiceImpl.java
    - src/main/java/vn/iotstar/service/impl/DestinationReceiptServiceImpl.java
    - src/main/java/vn/iotstar/entity/TransportTrip.java
    - src/main/java/vn/iotstar/entity/Handover.java
    - src/main/java/vn/iotstar/entity/ParcelContainer.java
    - src/main/java/vn/iotstar/entity/HandoverContainer.java
    - src/main/java/vn/iotstar/entity/ContainerItem.java
    - src/main/java/vn/iotstar/entity/Incident.java
    - src/main/java/vn/iotstar/repository/ShipmentEventRepository.java
    - src/main/webapp/WEB-INF/views/hub/inbound-queue.jsp
metrics:
  duration: 22min
  completed: "2026-10-25"
actuals:
  tokens: null
  tasks: 3
  commits: 0
plan_head_before: ca2bbf8
commits: 0
---

# Phase 05 Plan 02: Accountable linehaul and destination custody

A selected available vehicle is reserved for an assigned driver; departure moves the entire validated manifest into physical transit, and destination reconciliation closes container, handover and trip with preserved discrepancy evidence.

## Finding resolution

| Finding | Production repair | Verification |
|---|---|---|
| G-03 | Vehicle-selected trip acceptance; locked vehicle reservation; whole-manifest validation before mutation; forward parcels become IN_TRANSIT and leave current hub; RETURN parcels retain RETURN_TO_SENDER; one departure event per parcel. | Composed service test from acceptance through departure/arrival/receipt; held member prevents every departure mutation, explicit vehicle/availability and replay guards. |
| G-04 | Trip SCHEDULED → IN_TRANSIT → ARRIVED → COMPLETED/COMPLETED_WITH_DISCREPANCY; handover IN_TRANSIT at departure; each loaded container becomes UNPACKED_FULL/UNPACKED_WITH_DISCREPANCY; membership DELIVERED/DISCREPANCY; vehicle releases only on terminal trip receipt. | Partial receipt stays open, full/discrepant terminal assertions, receipt replay after terminal state, preserved return direction. Driver arrival never claims physical receipt. |
| G-05 | Missing marks only affected shipment held, leaves in-transit state; investigation list/detail/action is Manager/Admin scoped to incident custody hub; FOUND records actual destination receipt while retaining original MISSING item and terminal discrepancy; CONFIRMED_LOST remains held for finance. | Found/lost outcomes, immutable outcome replay, wrong incident/shipment/subtree denial, generic receipt bypass denial, unrelated open incident preserves hold, unknown scanned IDs rejected before mutation. |
| DB-01 | Canonical Vehicle entity and required TransportTrip.vehicle; mapped departure_time/arrival_time; migration adds legitimate FK/non-null constraint and refuses historical unmapped trips. | Source/compilation/domain regression pass. Real SQL migration/insert/concurrency remains pending in 05-05. |
| DB-02 | EXPECTED membership has no fabricated scan; scan creates LOADED with timestamp; migration makes scanned_at nullable and normalizes legacy SCANNED. | Service/domain tests; live canonical-base convergence pending. |

## Interface and next-plan handoff

- `LinehaulTripService.acceptHandover(handoverId, vehicleId)` is the production acceptance command. `availableVehicles()` returns bounded safe options. The legacy one-argument command can replay an already-bound trip but cannot choose a vehicle or create an unbound trip.
- `TransportTrip.accepted(handover, driver, vehicle)` requires a vehicle. `completeReceipt` releases its reservation. The service validates active driver assignment and locks vehicle/manifest records.
- `DestinationReceiptServiceImpl` constructor adds `ManagerOperationsRepository`. New `missingCases`, `missingCase`, and `resolveMissing` methods enforce investigation authority on every call.
- `Incident` adds `resolutionOutcome`, `resolutionEvidence`, `investigatedBy`, `investigatedAt`. **Confirmed loss contract for 05-04:** type MISSING_PARCEL, status INVESTIGATING, resolutionOutcome CONFIRMED_LOST, attributable bounded investigation evidence; shipment stays held and in its transit/return state. Finance must validate incident/shipment/trip/hub and manager incident-hub subtree, then atomically credit compensation, close shipment LOST, and resolve incident. Do not require missing shipment.currentHub to equal incident hub: physical departure clears currentHubId.
- FOUND uses the incident's recorded destination hub as the physical receipt boundary. ContainerItem keeps MISSING plus foundAt/foundBy; incident becomes RESOLVED. Hold clears only when no other OPEN/INVESTIGATING incident exists for that shipment. Historical terminal discrepancy is never rewritten.
- Return receipt preserves RETURN direction and RETURN_TO_SENDER. 05-03 still owns origin-arrival grace recording, reverse route identities and return assignment/release.
- `phase-05-custody-alignment.sql` follows Phase03 receipt migration. Historical vehicle mappings are deliberately not fabricated. Existing ACCEPTED/STARTED trip aliases and DISCREPANCY handover aliases are normalized; canonical timing columns are populated only from existing evidence.
- Manager active handover/trip queries and driver audit custody predicates include the new lifecycle. Sealed-membership migration protects the `(container_id, shipment_id)` identity, including terminal custody, while permitting receipt/recovery evidence updates.

## Reachable actions and cross-plan corrections

- Driver JSP offers server-listed available vehicle selection; its CSRF POST binds the actual vehicle ID.
- Inbound JSP now offers receipt reconciliation, plus an explicit all-contents-missing confirmation. Empty receipt is a valid reconciliation outcome; the previous `@NotEmpty` restriction incorrectly prevented it.
- `/manager/incidents/missing` lists scoped cases; `/{incidentId}` is an independently scoped detail route and `/resolve` is validated CSRF POST. The manager work-item MISSING link goes here, so destination managers can act even while a missing shipment has no current physical hub. Global shipment audit scope was not broadened.
- Parent-requested review correction: shipment event history orders by `createdAt, id`, not timestamp alone. A real SQL test forces same-second timestamps and verifies persisted ID ordering. This test is authored/compiled, **not claimed SQL-verified**.

## Verification

RED: the pre-fix departure test failed because the manifested parcel stayed AT_HUB after START; this was a behavioral failure, not a compile failure.

Final command:

`rtk mvn "-Dtest=LinehaulCustodyCompletionTest,LinehaulTripServiceImplTest,DestinationReceiptServiceImplTest,LinehaulTripRouteSecurityTest,DestinationReceiptControllerSecurityTest,MissingParcelControllerSecurityTest,ContainerHandoverServiceImplTest,ManagerOperationsServiceImplTest,CanonicalDomainContractTest,OperationalIncidentApiSecurityTest,ShipmentSqlServerIntegrationTest" test`

**BUILD SUCCESS, 38.906 seconds. Surefire: 45 reported, 41 executed/pass, 0 failures/errors, 4 SQL tests skipped.** All test sources compiled in Maven's ordinary lifecycle.

| Suite | Passed | Skipped |
|---|---:|---:|
| LinehaulCustodyCompletionTest | 10 | 0 |
| LinehaulTripServiceImplTest | 2 | 0 |
| DestinationReceiptServiceImplTest | 2 | 0 |
| LinehaulTripRouteSecurityTest | 5 | 0 |
| DestinationReceiptControllerSecurityTest | 5 | 0 |
| MissingParcelControllerSecurityTest | 3 | 0 |
| ContainerHandoverServiceImplTest | 5 | 0 |
| ManagerOperationsServiceImplTest | 5 | 0 |
| CanonicalDomainContractTest | 2 | 0 |
| OperationalIncidentApiSecurityTest | 2 | 0 |
| ShipmentSqlServerIntegrationTest | 0 | 4 |

`rtk git diff --check` passed. Relevant changed production files contain no TODO/FIXME/placeholder stubs. No browser/STOMP UAT or real SQL migration was run by this executor; these gates remain in 05-05. No skipped SQL test is counted as a pass.

## Deviations and Git status

1. Added the dedicated Manager missing-parcel controller/view because `/hub/**` correctly excludes Managers; broadening hub authorization would be incorrect.
2. Added the explicit empty-reconciliation UI and validation correction necessary for complete missing-bag handling.
3. Corrected sealed-membership trigger comparison to include parcel identity, not only container identity, and updated active custody predicates for IN_TRANSIT.
4. Applied the parent's timeline tie-break and incident-detail navigation review corrections.
5. Changes remain **uncommitted** on main as directed by the orchestrator/default-branch guard. No branch/config mutation, staging, commit or push. Commit count is zero, not a fabricated completion commit. `actuals.tokens` is unavailable because shared uncommitted paths already contained prior-plan changes and no isolated pre-plan diff snapshot was captured.

The parent owns STATE/ROADMAP/requirement completion; this executor did not advance any phase. No Phase06 work was started.

## Self-Check: PASSED (source/test artifacts)

All listed new files exist; final targeted reports are green with explicit SQL skips; whitespace check passes. Live SQL/browser completion and source commits remain explicitly unclaimed.
