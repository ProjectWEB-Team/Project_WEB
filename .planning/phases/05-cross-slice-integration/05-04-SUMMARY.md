---
phase: 05-cross-slice-integration
plan: "04"
subsystem: finance-settlement-reconciliation-notifications
status: implemented_pending_integration
tags: [refund-outbox, reconciliation, compensation, finance-scope, private-events]
requires: [05-01, 05-02, 05-03]
provides: [refund-producer-consumer, authoritative-reconciliation, confirmed-loss-settlement, owner-finance-notifications]
affects: [05-05]
tech-stack:
  added: []
  patterns: [shipment-first-lock-order, server-derived-finance-scope, bounded-purpose-dto, after-commit-owner-notification]
key-files:
  created:
    - src/main/java/vn/iotstar/service/impl/FinanceAccess.java
    - src/main/java/vn/iotstar/service/impl/RefundSettlementScheduler.java
    - src/main/java/vn/iotstar/dto/finance/CompensationRequest.java
    - src/main/java/vn/iotstar/dto/finance/RefundCandidateResponse.java
    - src/main/java/vn/iotstar/dto/finance/LossCompensationCandidate.java
    - src/main/java/vn/iotstar/dto/finance/ReconciliationSummaryResponse.java
    - src/main/java/vn/iotstar/dto/finance/ReconciliationItemResponse.java
    - src/test/java/vn/iotstar/service/impl/RefundProducerConsumerTest.java
    - src/test/java/vn/iotstar/realtime/StompNotificationEventPublisherTest.java
  modified:
    - src/main/java/vn/iotstar/service/impl/ShipmentFinanceServiceImpl.java
    - src/main/java/vn/iotstar/service/impl/FinanceReconciliationServiceImpl.java
    - src/main/java/vn/iotstar/service/impl/WithdrawalServiceImpl.java
    - src/main/java/vn/iotstar/service/impl/ShipmentCancellation.java
    - src/main/java/vn/iotstar/service/impl/FirstMileServiceImpl.java
    - src/main/java/vn/iotstar/service/impl/LastMileDeliveryServiceImpl.java
    - src/main/java/vn/iotstar/entity/RefundSettlementOutbox.java
    - src/main/java/vn/iotstar/entity/Incident.java
    - src/main/java/vn/iotstar/entity/ShipperReconciliation.java
    - src/main/java/vn/iotstar/entity/CodTransaction.java
    - src/main/java/vn/iotstar/controller/FinanceReconciliationController.java
    - src/main/java/vn/iotstar/realtime/OperationalRealtimePublisher.java
    - src/main/java/vn/iotstar/realtime/StompNotificationEventPublisher.java
    - src/main/java/vn/iotstar/dto/realtime/NotificationEventResponse.java
    - src/main/java/vn/iotstar/repository/OperationsAuditRepository.java
    - src/main/java/vn/iotstar/service/impl/OperationsAuditServiceImpl.java
    - src/main/resources/db/phase-05-finance-alignment.sql
    - src/main/webapp/WEB-INF/views/manager/reconciliation.jsp
    - src/main/webapp/WEB-INF/views/shipper/delivery-tasks.jsp
key-decisions:
  - "Manual and scheduled refunds share one source-bound transaction and shipment-first lock order."
  - "Compensate declared value by INCIDENT and refund actually paid forward fee components by PAYMENT; zero-payable confirmed loss still closes without a fabricated ledger credit."
  - "CASH reconciliation and electronic COD settlement have separate per-method headers and totals; electronic settlement requires manager-recorded manual evidence, not a fabricated provider callback."
  - "Preserve legacy collection paymentMethod fallback while allowing independent COD and receiver-fee methods."
  - "Financial notifications disclose only event type, reference and safe message to the actual owner."
metrics:
  completed: "2026-10-25"
actuals:
  tokens: null
  tasks: 3
  commits: 0
plan_head_before: ca2bbf8262980ee346e286e9a8c6ef1c7c3e0e8c
---

# Phase 05 Plan 04: Finance integration repair Summary

Cancellation obligations now settle through one atomic refund path; reconciliation binds every amount to its collected source and finance scope; confirmed missing parcels have a source-bound compensation action and owner notifications.

## Finding resolution

| Finding | Implemented behavior | Evidence boundary |
|---|---|---|
| G-09 | Cancellation and timeout create distinct obligations for paid sender SHIPPING_FEE and WEIGHT_SURCHARGE. Worker/manual recovery lock shipment, payment, obligation then wallet; validate owner, source, cancellation reason, amount and custody; stamp SETTLED only with wallet/payment changes. REFUNDED replay requires matching existing ledger owner/amount/balance delta. | Real producer/consumer service tests with repository doubles, including timeout and retry. SQL atomic rollback/concurrency remains 05-05. |
| G-10 / finance G-16 | Active manager subtree/admin scope covers shipper list, source reads, reconciliation, detail and payout. Exact collector/date/method/source-shipment/amount/state checks precede all persistence. Duplicate rows/sources and invalid actual amounts rejected. Item shortage and overage cannot net away; COD payout is separate. | Focused service and MVC tests. Existing source-pair and wallet-reference SQL uniqueness preserved. |
| G-12 | Compensation needs exact matching MISSING_PARCEL incident, CONFIRMED_LOST investigation, investigator/evidence and matching missing custody. Declared value and actual prepaid sender forward-fee components use separate unique references. Wrong incident replay fails. Zero payable amount closes without a zero credit. Scoped candidate list and validated CSRF POST are reachable. | Service tests include actual/unpaid/refunded fees, wrong/open case, replay, zero payable and 255-character decision reason. Database query remains to be exercised against SQL. |
| G-15 | Refund/compensation/COD/withdrawal events resolve the business owner, publish after commit and carry only safe fields. Operational supervisor lookup includes active ancestor managers and excludes unrelated hubs using the resolved ancestor IDs. | Publisher callback tests distinguish commit/rollback, serialized payload test, recipient service tests and ancestor query-binding regression. |

## Additional integration corrections

- First-mile sender cash stamps the actual pickup shipper as collector; hub employee collections remain outside shipper reconciliation.
- COD supports CASH, VNPAY and TRANSFER through delivery collection and confirmation. Optional independent `codPaymentMethod` / `shippingFeePaymentMethod` preserve old `paymentMethod` clients. Receiver fees remain CASH/VNPAY; unsupported fee methods reject before either source mutates.
- Delivery JSP now submits explicit received amounts and independent methods; the old form supplied only shipment ID and could not satisfy authoritative amount validation.
- Electronic COD is shown in a separate per-method reconciliation workspace. A scoped manager/admin records settlement evidence and actual verified amount; the shipper's COLLECTED state alone cannot authorize payout. This is manual/demo verification, **not live VNPAY/bank verification**. No provider API or callback was invented.
- Finance history/item endpoints return purpose DTOs; JSP uses explicit record accessors compatible with Jasper 10.1 and canonical `jakarta.tags.*` URIs. Local finance access-denied handling returns 403 instead of generic advice's 500.
- Automated refund/timeout audit events use the canonical null actor. Audit retrieval now left joins the actor and renders `SYSTEM` instead of dropping or dereferencing those events.
- Compensation decision text is persisted without a prefix so the allowed 255-character reason fits the existing NVARCHAR(255) event field.

## Verification

Final focused commands:

```text
rtk mvn "-Dtest=ShipmentFinanceServiceImplTest,ShipmentCancellationTest,IntakeTimeoutServiceImplTest,RefundProducerConsumerTest,FinanceReconciliationServiceImplTest,FinanceReconciliationControllerSecurityTest,WithdrawalServiceImplTest,OperationalRealtimePublisherTest,StompNotificationEventPublisherTest,WebSocketEndpointSecurityTest,WalletAndWithdrawalControllerSecurityTest,FirstMileServiceImplTest,OperationalAuditServiceImplTest,LastMileCollectionValidationTest,LastMileDeliveryServiceImplTest" test
rtk mvn "-Dtest=LastMileDeliveryControllerSecurityTest" test
rtk git diff --check
```

First command: **109 tests, 0 failures, 0 errors, 0 skipped**. Second: **7 tests, 0 failures, 0 errors, 0 skipped**. Total distinct selected tests: **116**. Maven compiled all ordinary test sources. `git diff --check` passed.

| Test class | Passed |
|---|---:|
| ShipmentFinanceServiceImplTest | 13 |
| FinanceReconciliationServiceImplTest | 26 |
| ShipmentCancellationTest | 4 |
| IntakeTimeoutServiceImplTest | 3 |
| RefundProducerConsumerTest | 2 |
| FinanceReconciliationControllerSecurityTest | 9 |
| WithdrawalServiceImplTest | 9 |
| OperationalRealtimePublisherTest | 2 |
| StompNotificationEventPublisherTest | 4 |
| WebSocketEndpointSecurityTest | 2 |
| WalletAndWithdrawalControllerSecurityTest | 8 |
| FirstMileServiceImplTest | 7 |
| OperationalAuditServiceImplTest | 6 |
| LastMileCollectionValidationTest | 8 |
| LastMileDeliveryServiceImplTest | 6 |
| LastMileDeliveryControllerSecurityTest | 7 |

Earlier focused run exposed the finance AccessDenied→500 error (82 tests, 1 failure), then passed after the local 403 correction. Final evidence above supersedes that run; no failure or skipped SQL test is counted as passing.

## Interface and migration handoff to 05-05

- `ShipmentFinanceServiceImpl` now injects outbox repository, wallet transaction repository, FinanceAccess and Clock in addition to prior dependencies. `settlePendingRefund(paymentId)` is the scheduler's internal consumer; no HTTP route exposes it.
- `ShipmentFinanceService` candidate lists are bounded DTOs; new confirmed-loss list is available. Manual refund/compensation still recheck scope/state at mutation time.
- `FinanceReconciliationServiceImpl` now injects FinanceAccess and Clock. History/items return DTOs; `getReadyPayouts` added. Existing two-argument expected-items method remains CASH; overload accepts collection method.
- Reconciliation request adds `collectionMethod` (default CASH) and `settlementEvidence` (required for electronic COD). Header snapshots `hub_id`, `collection_method`, `settlement_evidence` so payouts do not derive authority from a later shipper transfer.
- Finance SQL script is additive and retains plan03 collector and return-origin fields. New columns: `refund_settlement_outbox.settled_at`, `incidents.compensated_at`, `shipper_reconciliations.hub_id` with hub FK, `collection_method`, `settlement_evidence`. No legacy hub/method/evidence is guessed; legacy unproven records fail closed and need accountable remediation before payout.
- Existing phase04 `UNIQUE(source_type,source_id)` and wallet reference constraints are preserved. Verify them using the real migration/test database, not JPA fixture assumptions.
- New Incident repository missing-custody query joins ContainerItem and HandoverContainer with exact shipment/trip/destination hub; this must be checked against real Hibernate/SQL metadata in 05-05.
- Notification publisher adds recipient username API; compatibility actor API delegates. Notification DTO retains old constructor for producers but discards sensitive arguments.
- Scheduler runs at `uteexpress.finance.refund-scan-interval-ms` (60000 default); global scheduling switch remains `uteexpress.timeout.scheduler-enabled`.

## Remaining verification / limitations

- **BLOCKED_BY_ENVIRONMENT:** no dedicated SQL test connection was available to this executor. No live SQL integration tests were run or represented as passing. Real rollback, concurrent settlement/reconciliation/payout, additive migrations and repository query execution belong to 05-05 once configured.
- Full project regression, real JSP rendering and browser action journeys are delegated to 05-05. MockMvc verifies route/authentication/CSRF/form binding; it does not render JSPs. No browser proof claimed here.
- No source stubs were introduced for this plan. Electronic settlement uses explicitly recorded human verification evidence; external provider verification remains outside this integration repair.

## Execution bookkeeping

Per parent instruction the shared checkout remains uncommitted; no branches, worktrees, Git config or pushes were created. STATE/ROADMAP are parent-owned and unchanged by this executor. Phase 06 was not started. `actuals.tokens` is unknown because shared uncommitted earlier-plan diffs cannot honestly be attributed to this plan. Zero commits is an explicit uncommitted handoff, not a completed atomic-commit claim.

## Self-Check: PASSED

Production artifacts, new tests and this summary exist. All 16 selected Surefire XML reports were inspected for current counts; final executed tests all passed without skips. No commit existence assertion applies because no commits were requested or made. Status remains `implemented_pending_integration` until 05-05 verifies full-system evidence.
