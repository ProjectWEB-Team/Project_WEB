---
phase: 05-cross-slice-integration
plan: "03"
subsystem: last-mile-return-custody-fees
status: implemented_pending_integration
tags: [last-mile, reverse-routing, custody, collection, surcharge, return-grace]
requires: [05-01, 05-02]
provides: [scoped-last-mile-dispatch, physical-retry-receipt, return-origin-grace, sender-fee-settlement, collector-attribution]
affects: [05-04, 05-05]
tech-stack:
  added: []
  patterns: [shipment-lock-boundary, source-bound-payment-replay, server-local-clock, actual-physical-custody]
key-files:
  created:
    - src/main/java/vn/iotstar/service/impl/LastMileAccess.java
    - src/main/java/vn/iotstar/service/impl/LastMileAssignmentService.java
    - src/main/java/vn/iotstar/service/impl/ShipmentFeeService.java
    - src/main/java/vn/iotstar/controller/LastMileAssignmentController.java
    - src/main/java/vn/iotstar/controller/ShipmentFeeController.java
    - src/main/java/vn/iotstar/controller/ReturnDisposalController.java
    - src/main/java/vn/iotstar/dto/delivery/DeliveryAssignmentCandidate.java
    - src/main/java/vn/iotstar/dto/delivery/DeliveryAssignmentRequest.java
    - src/main/java/vn/iotstar/dto/returnflow/ShipmentFeeResponse.java
    - src/main/resources/db/phase-05-finance-alignment.sql
    - src/main/webapp/WEB-INF/views/hub/delivery-assignments.jsp
    - src/main/webapp/WEB-INF/views/user/shipment-fees.jsp
    - src/main/webapp/WEB-INF/views/manager/overdue-returns.jsp
    - src/test/java/vn/iotstar/service/LastMileTestFixture.java
    - src/test/java/vn/iotstar/service/LastMileCollectionValidationTest.java
    - src/test/java/vn/iotstar/service/ShipmentFeeServiceTest.java
    - src/test/java/vn/iotstar/controller/LastMileAssignmentControllerSecurityTest.java
    - src/test/java/vn/iotstar/controller/ShipmentFeeControllerSecurityTest.java
  modified:
    - src/main/java/vn/iotstar/service/impl/LastMileDeliveryServiceImpl.java
    - src/main/java/vn/iotstar/service/impl/HubOperationsServiceImpl.java
    - src/main/java/vn/iotstar/service/impl/ShipmentReturnServiceImpl.java
    - src/main/java/vn/iotstar/service/impl/ReturnGracePeriodScheduler.java
    - src/main/java/vn/iotstar/service/impl/ContainerHandoverServiceImpl.java
    - src/main/java/vn/iotstar/service/impl/DestinationReceiptServiceImpl.java
    - src/main/java/vn/iotstar/service/impl/OperationsAuditServiceImpl.java
    - src/main/java/vn/iotstar/entity/Shipment.java
    - src/main/java/vn/iotstar/entity/Payment.java
    - src/main/java/vn/iotstar/entity/CodTransaction.java
key-decisions:
  - "Preserve user-authorized 25000 VND default return fee with configurable override and existing priced values."
  - "An unsuccessful delivery leaves custody with shipper until physical inbound scan; retry waits until next day or the later promised date."
  - "Sender online settlement cannot assert physical cash collection or parcel release."
  - "Grace starts from first actual origin receipt; scheduler only reports and scoped manager/admin explicitly disposes."
metrics:
  completed: "2026-10-25"
actuals:
  tokens: null
  tasks: 3
  commits: 0
plan_head_before: ca2bbf8262980ee346e286e9a8c6ef1c7c3e0e8c
commits: 0
---

# Phase 05 Plan 03: Physical last-mile custody and authoritative return liabilities

Coordinator dispatch, shipper outbound scans, physical retry receipt, return-origin routing/grace, and sender fee settlement now share authoritative custody and payment checks. Implementation and focused tests are complete; SQL/Jasper and full cross-slice verification belong to 05-05 and are not claimed here.

## Finding resolution

| Finding | Production repair | Evidence |
|---|---|---|
| G-06 | Scoped active coordinator creates DELIVERY/RETURN_DELIVERY assignments from eligible physical inventory; active same-hub shipper, no hold, correct final/origin hub, no active task/container custody. | Real assignment-to-scan fixture, duplicate/wrong-position/held/wrong-hub denial; MVC record binding, role and CSRF tests. |
| G-07 | Return target uses actual origin geography; direction is included in route decision key; return loading excludes origin; receipt preserves RETURN_TO_SENDER and immutable first-origin timestamp; no creation-time fallback; manager-only explicit disposal after grace and custody checks. | Reverse routing/loading tests, physical destination-origin receipt replay, held/null-location/cross-hub/active-task/container disposal denial, deterministic Clock scheduler test. |
| G-08 | Every submitted COD/receiver fee amount is checked; existing records validate source amount/status/method/collector before either mutation. WAITING COD can record the first actual method and rebind to a new legitimate retry assignee. Collected evidence is immutable. | New/pre-created collection, mismatch-without-partial-mutation, method replay, collector and confirmation tests. |
| G-11 | Separate unpaid forward and return obligations are settled by sender wallet or explicit demo VNPay, or actual scoped cashier/return shipper CASH. Physical self-pickup and scanned home return require all liabilities settled. | Wallet debits once; original receiver obligation preserved; unscanned/unpaid return denial; staff self-pickup/return shipper completion and replay. |
| G-13 | User-authorized configurable 25000 VND default; existing priced fee preserved; negative policies rejected. | Default/override/preserved-price tests. |
| G-10 producer | Payment collector user ID, paid timestamp and immutable source reference; COD collector maintained through actual scan and collection. | Collector attribution assertions. Finance consumption remains 05-04. |
| Workflow 30 addendum | PAY_ONLINE creates authoritative pending weight surcharge without taking custody or cash; sender wallet/demo VNPay pays; physical receipt consumes a matching legitimate PAID surcharge without collecting it again. | Composed sender wallet payment to physical receipt; no second debit/payment write; original base fee preserved; forged/mismatched or stale assessment rejected. |

## Cross-plan interfaces and financial handoff

- `LastMileDeliveryService` adds `scanReturnDelivery(Long)` and `receiveFailedDelivery(Long,String)`. Its implementation constructor adds `LastMileAccess` and `ShipmentFeeService`.
- `LastMileAccess` requires employee profile repository, last-mile attempt repository and Clock. It enforces retry eligibility at candidates, assignment and outbound scan, using existing `ServerTime` local operational DATETIME2 convention.
- `ShipmentReturnServiceImpl` adds assignment repository, manager repository, access guard and Clock; `ReturnGracePeriodScheduler` also takes Clock. Scheduler never disposes or fabricates authentication.
- `Shipment.returnArrivedAtOriginAt` is nullable immutable evidence from actual `receiveAt(origin)`. Legacy rows without receipt evidence remain without grace.
- Route keys are `shipment:<id>:hub:<id>:<FORWARD|RETURN>`. Repository pending-route and manager backlog queries use the same direction-qualified identity. Historical forward keys cannot authorize return loading.
- `ShipmentRepository.hasActiveContainerCustody` checks IN_CONTAINER in OPEN/SEALED/LOADED containers. Assignment, return outbound/self-pickup and disposal reject this custody.
- New Payment types: `FORWARD_FEE_ON_RETURN`, `RETURN_FEE`, `WEIGHT_SURCHARGE`. Wallet transaction types are respectively `FORWARD_FEE_ON_RETURN_PAYMENT`, `RETURN_FEE_PAYMENT`, `WEIGHT_SURCHARGE_PAYMENT`, referenced to shipment ID. Unpaid original receiver SHIPPING_FEE remains as original evidence.
- `Payment.collect(method,collectorId,reference)` records CASH/VNPAY/E_WALLET evidence; sender ID is recorded for noncash settlement too. **05-04 must filter actual CASH shipper sources, not simply every attributed Payment.** Hub cash is not a shipper shift receipt; unattributed legacy cash remains excluded.
- Additive migration adds `payments.collected_by_user_id` (users FK) and `shipments.return_arrived_at_origin_at`. 05-04 must extend financial migration as required and implement paid surcharge cancellation refund/outbox handling. No production SQL was executed.

## Reachable workflow

- `/hub/delivery-assignments` is the coordinator workspace. Physical failed-delivery receipt form is on `/hub/inbound`, where INBOUND staff can reach it.
- `/shipper/return-tasks/scan` establishes IN_PROGRESS plus OUT_FOR_DELIVERY with direction RETURN and null current hub. Cash receipt and final confirmation are separate commands.
- `/hub/return-collection` is the staff physical self-pickup page. Sender `/user/return-collection` physical release POST was removed; sender settlement is `/user/shipment-fees/{id}`, linked inside shipment detail.
- Fee page exposes only relevant outstanding payment actions and clearly labels VNPay as simulated. Both fees and new assignment records support JSP property getters/explicit accessors. New overdue JSP c:out syntax was corrected after independent review; actual Jasper execution remains 05-05.
- `/manager/overdue-returns` and `/admin/overdue-returns` are scoped review/decision pages. No automatic disposal exists.
- Active linehaul driver audit access now works while currentHub is null using current custody references; historical/unrelated drivers remain denied before event projection.

## Verification

- Production and all test sources compile via the Maven test lifecycle.
- Initial broad focused suite: 95 tests, 0 failures/errors/skips.
- Expanded routing/origin/audit suite initially ran 105 tests with one test-only expected event-order mismatch; changed to unordered same-transaction event assertion.
- Correction run: `rtk mvn "-Dtest=DestinationReceiptServiceImplTest,ContainerHandoverServiceImplTest,LastMileReturnServiceImplTest" test` — **21 passed, 0 failures/errors/skips**.
- Independent retry-date review correction run: `rtk mvn "-Dtest=LastMileDeliveryServiceImplTest,LastMileCollectionValidationTest,LastMileReturnServiceImplTest,ShipmentFeeServiceTest,ShipmentReturnServiceImplTest,SelfDropoffReceiptTest,LastMileAssignmentControllerSecurityTest,LastMileDeliveryControllerSecurityTest,LastMileReturnControllerSecurityTest,ShipmentFeeControllerSecurityTest" test` — **80 passed, 0 failures/errors/skips**.
- Final server-local Clock follow-up: `rtk mvn "-Dtest=LastMileReturnServiceImplTest,LastMileCollectionValidationTest,ShipmentReturnServiceImplTest" test` — **31 passed, 0 failures/errors/skips** (26.381 seconds).
- Latest per-class Surefire XML across the 14 relevant classes totals **107 tests, 0 failures, 0 errors, 0 skipped**. This is aggregated latest class evidence from the focused runs, not a claim that 107 ran together in the final invocation.
- `rtk git diff --check` passed. No full regression suite was run by this executor.
- Live SQL migration/query/concurrency/rollback proof and populated Jasper/browser journeys: **NOT RUN here**, remaining 05-05 integration evidence. Unit/MVC mock results are not SQL proof, and SQL skips are not passes.

## Deviations and review corrections

1. Physical failed-delivery receipt was added because the previous retry path fabricated AT_HUB while the shipper still held the parcel. Retry assignment now follows actual receipt.
2. Independent review found that the retry producer could ignore the requested delivery date. Both assignment and scan now wait until next day or later RESCHEDULED date; a composed test rejects premature dispatch even with a malformed existing assignment, then allows the promised date.
3. Independent review found malformed overdue JSP markup; corrected before handoff.
4. Parent-requested transit audit correction preserves assigned driver access after real departure clears current hub.
5. TDD RED/GREEN gates were not separately captured for all added tests; this summary reports executed behavioral regression evidence rather than claiming an unobserved canonical TDD sequence.
6. Parent explicitly required no commits/branch/worktree/state advancement. All source changes remain uncommitted with previous 05-01/02 edits preserved. Measured commit count since the captured HEAD is 0. `actuals.tokens` is null because shared uncommitted overlapping files prevent an honest isolated realized diff measurement; no harness tokens substituted.

## Known stubs and remaining boundaries

No TODO/FIXME or placeholder implementation was found in the newly added/rewritten service/controller surfaces. Demo VNPay is an explicitly labeled existing project simulation boundary, not a real gateway claim. Finance refund/source consumption and live integration evidence remain assigned to 05-04/05-05. This summary does not advance Phase 05 or mark the whole phase verified.

## Self-Check: PASSED

New production services/controllers/DTOs/JSPs, additive migration, regression tests and this summary exist on disk. Latest focused run is green; per-class XML was read to verify the aggregate counts. `rtk git diff --check` passed. `git rev-list --count ca2bbf8262980ee346e286e9a8c6ef1c7c3e0e8c..HEAD` returned 0 as expected under the explicit no-commit instruction. STATE/ROADMAP were not changed by this executor.
