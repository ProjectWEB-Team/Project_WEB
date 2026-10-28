---
phase: 05-cross-slice-integration
plan: "01"
subsystem: intake-hub-tracking-management
status: implemented_pending_integration
tags: [intake, routing, self-dropoff, tracking, scope]
requires: [phase-01, phase-02, phase-03, phase-04]
provides: [authoritative-destination, physical-self-dropoff, safe-journey-history, scoped-operational-work-items]
affects: [05-02, 05-03, 05-04, 05-05]
tech-stack:
  added: []
  patterns: [service-transactions, pessimistic-shipment-lock, safe-dto]
key-files:
  created:
    - src/main/java/vn/iotstar/service/impl/ShippingFeeCalculator.java
    - src/main/java/vn/iotstar/dto/operations/OperationalWorkItemResponse.java
    - src/main/webapp/WEB-INF/views/manager/operation-work-item.jsp
    - src/main/resources/db/phase-05-intake-service-code.sql
    - src/test/java/vn/iotstar/service/SelfDropoffReceiptTest.java
  modified:
    - src/main/java/vn/iotstar/service/impl/ShipmentServiceImpl.java
    - src/main/java/vn/iotstar/service/impl/HubOperationsServiceImpl.java
    - src/main/java/vn/iotstar/service/impl/TrackingServiceImpl.java
    - src/main/java/vn/iotstar/repository/ManagerOperationsRepository.java
    - src/main/java/vn/iotstar/service/impl/ManagerOperationsServiceImpl.java
    - src/main/java/vn/iotstar/entity/Shipment.java
    - src/main/java/vn/iotstar/entity/Payment.java
    - src/test/java/vn/iotstar/integration/ShipmentSqlServerIntegrationTest.java
metrics:
  duration: 18min
  completed: "2026-10-25"
actuals:
  tokens: 19195
  tasks: 3
  commits: 0
plan_head_before: ca2bbf8
commits: 0
---

# Phase 05 Plan 01: Intake, physical receipt and safe operations

Intake resolves a unique active delivery hub, self-dropoff now records physical/cash/surcharge evidence, and tracking plus management views expose scoped persisted journey data.

## Finding resolution

| Finding | Production change | Evidence / remaining boundary |
|---|---|---|
| G-01 | Create resolves active level-1 DELIVERY_HUB by exact receiver province/district; zero/multiple coverage or region mismatch rejects before save. Stores destination hub/grouping and selected service. Sort at destination terminates linehaul routing. | Intake assertions, missing/ambiguous negative tests, terminal sorting replay, and a real intake-service output composed into self-dropoff and terminal sorting without setting routing fields in the fixture. |
| G-02 | Self-dropoff requires active inbound/general staff at delivery hub, FORWARD direction, tracking scan, positive measured values, eligible shipping payment and exact cash. Sets origin/current hub only on valid first receipt. | Eight receipt regressions plus MVC binding/CSRF and existing pickup regressions. Replay checks the locked shipment and persisted origin-arrival event. Generic inbound cannot receive container/transit or unpicked HOME_PICKUP parcels. |
| G-14 | Persisted hub, transit, delivery, return and terminal events get safe public labels. Unknown/internal events remain hidden. | Public and masked-recipient journey assertions preserve event order and exclude raw descriptions, addresses, cash and incident details. |
| G-16 operational | Backlog correlates routes to current physical hub, excludes final delivery hub; bounded work items for blocked routes, missing parcels and pending handovers; detail lookup repeats subtree filtering. Correct sidebar routes. | Service and MVC allow/deny tests pass. Return-direction-aware route identity is a shared handoff to 05-03; real SQL query/multi-hop proof remains 05-05. |

## Business handling

- Workflow section 30 takes precedence over a generic reprice interpretation: sender weight surcharge creates a separate PAID WEIGHT_SURCHARGE payment, never rewrites a paid base fee. Receiver surcharge updates the unpaid SHIPPING_FEE and shipment total. Smaller weight does not reduce the original fee.
- One pure ShippingFeeCalculator is reused by quote and physical intake. Dimensions are recorded but no invented volumetric formula is introduced.
- Sender surcharge acceptance requires explicit ACCEPT and exact cash; explicit REJECT invokes shared WEIGHT_DISCREPANCY cancellation and its existing refund outbox producer. Missing decision rejects without mutation.
- Receipt remains atomic under the shipment row lock. A stale/replayed self-dropoff at another hub, on return direction, or in transit cannot move custody through generic inbound.
- Service code was previously discarded. New nullable shipments.service_code stores it for new intake; forward migration deliberately does not guess the purchased service for legacy rows. Legacy overweight receipts fail closed until legitimate pricing context is restored.

## Verification

Final focused command:

`rtk mvn "-Dtest=SelfDropoffReceiptTest,HubRoutingServiceImplTest,HubRoutingControllerSecurityTest,FirstMileServiceImplTest,ShipmentServiceImplTest,TrackingServiceImplTest,ManagerOperationsServiceImplTest,ManagerOperationsControllerSecurityTest" test`

**BUILD SUCCESS: 66 executed/pass, 0 failures, 0 errors, 0 skipped**, 37.477 seconds. Maven compiles all test sources in this lifecycle.

| Suite | Passed |
|---|---:|
| SelfDropoffReceiptTest | 8 |
| HubRoutingServiceImplTest | 11 |
| HubRoutingControllerSecurityTest | 7 |
| FirstMileServiceImplTest | 7 |
| ShipmentServiceImplTest | 18 |
| TrackingServiceImplTest | 5 |
| ManagerOperationsServiceImplTest | 5 |
| ManagerOperationsControllerSecurityTest | 5 |

`rtk git diff --check`: passed. No new TODO/FIXME/placeholder stubs found in touched source files.

RED evidence: destination assertion failed before production wiring; later-journey timeline assertion failed before label mapping. The physical receipt MVC test also exposed a real binding failure from overloaded record constructors; replacing the convenience constructor with static pickupReceipt restored canonical record construction and the end-to-end MVC test passes.

The legacy ShipmentSqlServerIntegrationTest fixture now creates a unique active delivery hub and applies the service_code migration. That test was **compiled, not run** in this focused suite. No live SQL Server, migration execution, transaction/concurrency proof or browser/STOMP UAT was performed. Those remain BLOCKED_BY_ENVIRONMENT / pending integration verification, not PASS.

## Interfaces and next-plan handoff

- ShipmentServiceImpl constructor adds HubRepository.
- HubOperationsServiceImpl constructor adds PaymentRepository, PricingPolicyRepository, ShipmentCancellation and ShipmentEventPublisher.
- InboundShipmentCommand is the single canonical eight-field record. Existing pickup-only callers use static pickupReceipt(id); do not add an overloaded constructor because MVC binding failed with it.
- Shipment adds serviceCode and measured-dimension accessors/recordMeasurements. Payment adds reviseUnpaidAmount, which refuses settled payments.
- ManagerOperationsService adds getWorkItems/getWorkItem; repository projections filter authorized hubs before returning any DTO. New management detail URL is /manager/operations/work/{kind}/{id}, with scoped shipment-audit drilldown.
- **05-02:** new management projections must follow any corrected handover/trip terminal-state names.
- **05-03:** align return-direction route decision keys in routing, loading and backlog. Forward keys currently remain shipment:{id}:hub:{id}; do not reuse a forward route for return.
- **05-03/04:** annotate hub-staff base/surcharge CASH collection with forthcoming collector attribution; these receipts must not count as shipper collections.
- **05-05:** include phase-05-intake-service-code.sql in the ordered migration manifest before entity use. Prove SQL work-list/backlog queries and full cross-slice journeys on the dedicated SQL Server boundary.

## Deviations and commit state

Source work was completed in the existing main checkout at the parent's direction. The default-branch commit guard and no-new-branch direction were preserved; **all 28 changed/new source/test/migration/view files are uncommitted**, with no staging, push or Git ref changes. No task commits are claimed. Parent owns STATE/ROADMAP and phase progression; this executor did not edit them or mark INTEG requirements complete. Planning artifacts remain uncommitted because commit_docs=false.

Added service_code migration, shared pricing utility and focused receipt suite as necessary production correctness work. Documentation status is implemented_pending_integration because return-direction/source-attribution seams and live integration proof belong to subsequent repair groups.

## Self-Check: PASSED (source and focused regression only)

All five new files and changed production interfaces exist. Final focused suite is green; diff whitespace check is green. There are zero commits and no current live SQL/browser proof. This summary does not certify Phase 05 completion.
