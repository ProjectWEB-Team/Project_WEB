---
phase: 05-cross-slice-integration
verified: 2026-10-25T14:44:16Z
status: human_needed
score: 18/19 must-haves verified
behavior_unverified: 1
overrides_applied: 0
covered_files:
  - ".planning/REQUIREMENTS.md"
  - ".planning/ROADMAP.md"
  - ".planning/phases/05-cross-slice-integration/05-01-PLAN.md"
  - ".planning/phases/05-cross-slice-integration/05-01-SUMMARY.md"
  - ".planning/phases/05-cross-slice-integration/05-02-PLAN.md"
  - ".planning/phases/05-cross-slice-integration/05-02-SUMMARY.md"
  - ".planning/phases/05-cross-slice-integration/05-03-PLAN.md"
  - ".planning/phases/05-cross-slice-integration/05-03-SUMMARY.md"
  - ".planning/phases/05-cross-slice-integration/05-04-PLAN.md"
  - ".planning/phases/05-cross-slice-integration/05-04-SUMMARY.md"
  - ".planning/phases/05-cross-slice-integration/05-05-PLAN.md"
  - ".planning/phases/05-cross-slice-integration/05-05-SUMMARY.md"
  - ".planning/phases/05-cross-slice-integration/05-CONTEXT.md"
  - ".planning/phases/05-cross-slice-integration/05-EARLY-REVIEW.md"
  - ".planning/phases/05-cross-slice-integration/05-REVIEW.md"
  - ".planning/phases/05-cross-slice-integration/05-SQL-RESULTS.json"
  - ".planning/phases/05-cross-slice-integration/05-UAT.md"
  - "docs/ARCHITECTURE_DECISIONS.md"
  - "docs/ENGINEERING_STANDARDS.md"
  - "docs/ERD.md"
  - "docs/PHASE_05_VERIFICATION.md"
  - "docs/UTEExpress_Workflow.md"
  - "docs/schema.sql"
  - "pom.xml"
  - "src/main/java/vn/iotstar/controller/DriverOperationsController.java"
  - "src/main/java/vn/iotstar/controller/FinanceReconciliationController.java"
  - "src/main/java/vn/iotstar/controller/HubOperationsController.java"
  - "src/main/java/vn/iotstar/controller/InboundOperationsController.java"
  - "src/main/java/vn/iotstar/controller/LastMileAssignmentController.java"
  - "src/main/java/vn/iotstar/controller/LastMileDeliveryController.java"
  - "src/main/java/vn/iotstar/controller/ManagerOperationsController.java"
  - "src/main/java/vn/iotstar/controller/MissingParcelController.java"
  - "src/main/java/vn/iotstar/controller/ReturnDisposalController.java"
  - "src/main/java/vn/iotstar/controller/ShipmentFeeController.java"
  - "src/main/java/vn/iotstar/dto/delivery/DeliveryAssignmentCandidate.java"
  - "src/main/java/vn/iotstar/dto/delivery/DeliveryAssignmentRequest.java"
  - "src/main/java/vn/iotstar/dto/delivery/DeliveryCollectionRequest.java"
  - "src/main/java/vn/iotstar/dto/driver/TripActionResponse.java"
  - "src/main/java/vn/iotstar/dto/driver/VehicleOptionResponse.java"
  - "src/main/java/vn/iotstar/dto/finance/CompensationRequest.java"
  - "src/main/java/vn/iotstar/dto/finance/LossCompensationCandidate.java"
  - "src/main/java/vn/iotstar/dto/finance/ReconciliationItemResponse.java"
  - "src/main/java/vn/iotstar/dto/finance/ReconciliationSubmissionRequest.java"
  - "src/main/java/vn/iotstar/dto/finance/ReconciliationSummaryResponse.java"
  - "src/main/java/vn/iotstar/dto/finance/RefundCandidateResponse.java"
  - "src/main/java/vn/iotstar/dto/hub/DestinationReceiptResponse.java"
  - "src/main/java/vn/iotstar/dto/hub/DestinationReconciliationCommand.java"
  - "src/main/java/vn/iotstar/dto/hub/HandoverAcceptanceResponse.java"
  - "src/main/java/vn/iotstar/dto/hub/InboundShipmentCommand.java"
  - "src/main/java/vn/iotstar/dto/hub/MissingParcelCaseResponse.java"
  - "src/main/java/vn/iotstar/dto/hub/MissingParcelResolutionCommand.java"
  - "src/main/java/vn/iotstar/dto/hub/RouteDiagnosticResponse.java"
  - "src/main/java/vn/iotstar/dto/hub/SortingDecisionResponse.java"
  - "src/main/java/vn/iotstar/dto/operations/AuditCustodyReference.java"
  - "src/main/java/vn/iotstar/dto/operations/ManagerOperationsDashboardResponse.java"
  - "src/main/java/vn/iotstar/dto/operations/OperationalWorkItemResponse.java"
  - "src/main/java/vn/iotstar/dto/operations/OperationsAuditTimelineEntry.java"
  - "src/main/java/vn/iotstar/dto/operations/OperationsAuditTimelineResponse.java"
  - "src/main/java/vn/iotstar/dto/realtime/NotificationEventResponse.java"
  - "src/main/java/vn/iotstar/dto/returnflow/ShipmentFeeResponse.java"
  - "src/main/java/vn/iotstar/entity/CodTransaction.java"
  - "src/main/java/vn/iotstar/entity/ContainerItem.java"
  - "src/main/java/vn/iotstar/entity/Handover.java"
  - "src/main/java/vn/iotstar/entity/HandoverContainer.java"
  - "src/main/java/vn/iotstar/entity/Incident.java"
  - "src/main/java/vn/iotstar/entity/ParcelContainer.java"
  - "src/main/java/vn/iotstar/entity/Payment.java"
  - "src/main/java/vn/iotstar/entity/RefundSettlementOutbox.java"
  - "src/main/java/vn/iotstar/entity/Shipment.java"
  - "src/main/java/vn/iotstar/entity/ShipperReconciliation.java"
  - "src/main/java/vn/iotstar/entity/TransportTrip.java"
  - "src/main/java/vn/iotstar/entity/Vehicle.java"
  - "src/main/java/vn/iotstar/realtime/NotificationEventPublisher.java"
  - "src/main/java/vn/iotstar/realtime/OperationalRealtimePublisher.java"
  - "src/main/java/vn/iotstar/realtime/StompNotificationEventPublisher.java"
  - "src/main/java/vn/iotstar/repository/ContainerHandoverRepository.java"
  - "src/main/java/vn/iotstar/repository/FinanceReconciliationRepository.java"
  - "src/main/java/vn/iotstar/repository/HubOperationsRepository.java"
  - "src/main/java/vn/iotstar/repository/HubRepository.java"
  - "src/main/java/vn/iotstar/repository/IncidentRepository.java"
  - "src/main/java/vn/iotstar/repository/LinehaulOperationsRepository.java"
  - "src/main/java/vn/iotstar/repository/ManagerOperationsRepository.java"
  - "src/main/java/vn/iotstar/repository/OperationalIncidentRepository.java"
  - "src/main/java/vn/iotstar/repository/OperationsAuditRepository.java"
  - "src/main/java/vn/iotstar/repository/PaymentRepository.java"
  - "src/main/java/vn/iotstar/repository/RefundSettlementOutboxRepository.java"
  - "src/main/java/vn/iotstar/repository/ShipmentAssignmentRepository.java"
  - "src/main/java/vn/iotstar/repository/ShipmentEventRepository.java"
  - "src/main/java/vn/iotstar/repository/ShipmentRepository.java"
  - "src/main/java/vn/iotstar/repository/ShipperReconciliationItemRepository.java"
  - "src/main/java/vn/iotstar/service/DestinationReceiptService.java"
  - "src/main/java/vn/iotstar/service/FinanceReconciliationService.java"
  - "src/main/java/vn/iotstar/service/LastMileDeliveryService.java"
  - "src/main/java/vn/iotstar/service/LinehaulTripService.java"
  - "src/main/java/vn/iotstar/service/ManagerOperationsService.java"
  - "src/main/java/vn/iotstar/service/ShipmentFinanceService.java"
  - "src/main/java/vn/iotstar/service/impl/ContainerHandoverServiceImpl.java"
  - "src/main/java/vn/iotstar/service/impl/DestinationReceiptServiceImpl.java"
  - "src/main/java/vn/iotstar/service/impl/FinanceAccess.java"
  - "src/main/java/vn/iotstar/service/impl/FinanceReconciliationServiceImpl.java"
  - "src/main/java/vn/iotstar/service/impl/FirstMileServiceImpl.java"
  - "src/main/java/vn/iotstar/service/impl/HubOperationsServiceImpl.java"
  - "src/main/java/vn/iotstar/service/impl/LastMileAccess.java"
  - "src/main/java/vn/iotstar/service/impl/LastMileAssignmentService.java"
  - "src/main/java/vn/iotstar/service/impl/LastMileDeliveryServiceImpl.java"
  - "src/main/java/vn/iotstar/service/impl/LinehaulTripServiceImpl.java"
  - "src/main/java/vn/iotstar/service/impl/ManagerOperationsServiceImpl.java"
  - "src/main/java/vn/iotstar/service/impl/OperationsAuditServiceImpl.java"
  - "src/main/java/vn/iotstar/service/impl/RefundSettlementScheduler.java"
  - "src/main/java/vn/iotstar/service/impl/ReturnGracePeriodScheduler.java"
  - "src/main/java/vn/iotstar/service/impl/ShipmentCancellation.java"
  - "src/main/java/vn/iotstar/service/impl/ShipmentFeeService.java"
  - "src/main/java/vn/iotstar/service/impl/ShipmentFinanceServiceImpl.java"
  - "src/main/java/vn/iotstar/service/impl/ShipmentReturnServiceImpl.java"
  - "src/main/java/vn/iotstar/service/impl/ShipmentServiceImpl.java"
  - "src/main/java/vn/iotstar/service/impl/ShippingFeeCalculator.java"
  - "src/main/java/vn/iotstar/service/impl/TrackingServiceImpl.java"
  - "src/main/java/vn/iotstar/service/impl/WithdrawalServiceImpl.java"
  - "src/main/resources/db/phase-01-shipment-events-event-type.sql"
  - "src/main/resources/db/phase-02-account-otp.sql"
  - "src/main/resources/db/phase-02-handover-wait.sql"
  - "src/main/resources/db/phase-02-refund-outbox.sql"
  - "src/main/resources/db/phase-03-container-handover-schema.sql"
  - "src/main/resources/db/phase-03-destination-receipt-schema.sql"
  - "src/main/resources/db/phase-03-hub-routing-schema.sql"
  - "src/main/resources/db/phase-03-linehaul-trip-schema.sql"
  - "src/main/resources/db/phase-04-delivery-schema.sql"
  - "src/main/resources/db/phase-04-finance-reconciliation-schema.sql"
  - "src/main/resources/db/phase-04-return-schema.sql"
  - "src/main/resources/db/phase-04-withdrawal-schema.sql"
  - "src/main/resources/db/phase-05-custody-alignment.sql"
  - "src/main/resources/db/phase-05-finance-alignment.sql"
  - "src/main/resources/db/phase-05-intake-service-code.sql"
  - "src/main/resources/db/phase-05-integration-fixtures.sql"
  - "src/main/resources/db/phase-05-integration-reset.sql"
  - "src/main/resources/db/phase-05-migration-manifest.txt"
  - "src/main/resources/db/phase-05-route-key-alignment.sql"
  - "src/main/webapp/WEB-INF/views/admin/withdrawals.jsp"
  - "src/main/webapp/WEB-INF/views/driver/trips.jsp"
  - "src/main/webapp/WEB-INF/views/hub/audit-timeline.jsp"
  - "src/main/webapp/WEB-INF/views/hub/delivery-assignments.jsp"
  - "src/main/webapp/WEB-INF/views/hub/inbound-queue.jsp"
  - "src/main/webapp/WEB-INF/views/hub/sorter-queue.jsp"
  - "src/main/webapp/WEB-INF/views/layout/sidebar.jsp"
  - "src/main/webapp/WEB-INF/views/manager/missing-parcels.jsp"
  - "src/main/webapp/WEB-INF/views/manager/operation-work-item.jsp"
  - "src/main/webapp/WEB-INF/views/manager/operations-dashboard.jsp"
  - "src/main/webapp/WEB-INF/views/manager/overdue-returns.jsp"
  - "src/main/webapp/WEB-INF/views/manager/reconciliation.jsp"
  - "src/main/webapp/WEB-INF/views/shipment/detail.jsp"
  - "src/main/webapp/WEB-INF/views/shipper/delivery-tasks.jsp"
  - "src/main/webapp/WEB-INF/views/shipper/return-tasks.jsp"
  - "src/main/webapp/WEB-INF/views/user/return-collection.jsp"
  - "src/main/webapp/WEB-INF/views/user/shipment-fees.jsp"
  - "src/main/webapp/WEB-INF/views/user/wallet.jsp"
  - "src/test/java/vn/iotstar/controller/DestinationReceiptControllerSecurityTest.java"
  - "src/test/java/vn/iotstar/controller/FinanceReconciliationControllerSecurityTest.java"
  - "src/test/java/vn/iotstar/controller/HubRoutingControllerSecurityTest.java"
  - "src/test/java/vn/iotstar/controller/LastMileAssignmentControllerSecurityTest.java"
  - "src/test/java/vn/iotstar/controller/LastMileDeliveryControllerSecurityTest.java"
  - "src/test/java/vn/iotstar/controller/LastMileReturnControllerSecurityTest.java"
  - "src/test/java/vn/iotstar/controller/LinehaulTripRouteSecurityTest.java"
  - "src/test/java/vn/iotstar/controller/ManagerOperationsControllerSecurityTest.java"
  - "src/test/java/vn/iotstar/controller/MissingParcelControllerSecurityTest.java"
  - "src/test/java/vn/iotstar/controller/Phase05OperationsJspRenderingTest.java"
  - "src/test/java/vn/iotstar/controller/ShipmentFeeControllerSecurityTest.java"
  - "src/test/java/vn/iotstar/integration/Phase01WalkingSkeletonTest.java"
  - "src/test/java/vn/iotstar/integration/Phase03RoutingSqlServerIntegrationTest.java"
  - "src/test/java/vn/iotstar/integration/Phase04FinanceSqlServerIntegrationTest.java"
  - "src/test/java/vn/iotstar/integration/Phase05CustodySqlServerIntegrationTest.java"
  - "src/test/java/vn/iotstar/integration/Phase05DeliveryReturnSqlServerIntegrationTest.java"
  - "src/test/java/vn/iotstar/integration/Phase05FinanceSqlServerIntegrationTest.java"
  - "src/test/java/vn/iotstar/integration/Phase05ManagerScopeSqlServerIntegrationTest.java"
  - "src/test/java/vn/iotstar/integration/Phase05NormalJourneySqlServerIntegrationTest.java"
  - "src/test/java/vn/iotstar/integration/Phase05SchemaConvergenceSqlServerIntegrationTest.java"
  - "src/test/java/vn/iotstar/integration/Phase05SqlServerFixtureSupport.java"
  - "src/test/java/vn/iotstar/integration/ShipmentSqlServerIntegrationTest.java"
  - "src/test/java/vn/iotstar/integration/SqlServerMigrationSupport.java"
  - "src/test/java/vn/iotstar/integration/SqlServerMigrationSupportTest.java"
  - "src/test/java/vn/iotstar/realtime/MissingParcelRealtimePublisherTest.java"
  - "src/test/java/vn/iotstar/realtime/OperationalRealtimePublisherTest.java"
  - "src/test/java/vn/iotstar/realtime/StompNotificationEventPublisherTest.java"
  - "src/test/java/vn/iotstar/service/ContainerHandoverServiceImplTest.java"
  - "src/test/java/vn/iotstar/service/DestinationReceiptServiceImplTest.java"
  - "src/test/java/vn/iotstar/service/FinanceReconciliationServiceImplTest.java"
  - "src/test/java/vn/iotstar/service/HubRoutingServiceImplTest.java"
  - "src/test/java/vn/iotstar/service/LastMileCollectionValidationTest.java"
  - "src/test/java/vn/iotstar/service/LastMileDeliveryServiceImplTest.java"
  - "src/test/java/vn/iotstar/service/LastMileReturnServiceImplTest.java"
  - "src/test/java/vn/iotstar/service/LastMileTestFixture.java"
  - "src/test/java/vn/iotstar/service/LinehaulCustodyCompletionTest.java"
  - "src/test/java/vn/iotstar/service/LinehaulTripServiceImplTest.java"
  - "src/test/java/vn/iotstar/service/ManagerOperationsServiceImplTest.java"
  - "src/test/java/vn/iotstar/service/OperationalAuditServiceImplTest.java"
  - "src/test/java/vn/iotstar/service/SelfDropoffReceiptTest.java"
  - "src/test/java/vn/iotstar/service/ShipmentFeeServiceTest.java"
  - "src/test/java/vn/iotstar/service/ShipmentFinanceServiceImplTest.java"
  - "src/test/java/vn/iotstar/service/ShipmentReturnServiceImplTest.java"
  - "src/test/java/vn/iotstar/service/WithdrawalServiceImplTest.java"
  - "src/test/java/vn/iotstar/service/impl/RefundProducerConsumerTest.java"
  - "src/test/java/vn/iotstar/service/impl/ShipmentCancellationTest.java"
  - "src/test/java/vn/iotstar/service/impl/ShipmentServiceImplTest.java"
  - "src/test/java/vn/iotstar/service/impl/TrackingServiceImplTest.java"
covered_digest: "v1:sha256:52f9b50eb51ce84a40ef3471a980d7a851ae70d70a5fa5130b416b2916b94537"
decision_coverage:
  honored: 16
  total: 16
  not_honored: []
must_haves:
  truths:
    - "A normal shipment completes from creation through hub/linehaul/last-mile delivery and its financial outcome, with coherent tracking and required realtime updates."
    - "The defined expiry, delivery-failure/return, missing-parcel, COD-discrepancy, and compensation journeys preserve state, event, custody, and wallet consistency end-to-end."
    - "Manager/admin operational and reporting views aggregate cross-slice information while retaining contextual data scope."
    - "A real intake creates authoritative destination geography and a unique active delivery hub; routing stops at that destination."
    - "An authorized inbound operator receives a fresh self-dropoff with physical measurement and payment evidence in one transaction."
    - "Persisted journey events appear through safe tracking labels and managers see only current scoped work items."
    - "Trip departure transitions every eligible manifested shipment with recorded custody and an assigned vehicle."
    - "Destination receipt completes containers, handover and trip with the canonical full/discrepancy outcome."
    - "Missing parcels remain on hold until authorized incident-bound found recovery or confirmed loss; historical evidence is preserved."
    - "A scoped coordinator can create initial DELIVERY and RETURN_DELIVERY assignments from real eligible hub inventory."
    - "All collection branches validate amount/method/collector and return completion requires actual outbound custody and settled liabilities."
    - "Return routing preserves direction; grace starts once at actual origin arrival and disposal requires expiry and scoped physical custody."
    - "Cancellation/expiry refund outbox and wallet/payment settlement commit together and retries cannot credit twice."
    - "Reconciliation expected values come from the exact collected source for the chosen shipper/date/method and manager scope, never submitted totals."
    - "Compensation requires a matching confirmed-loss incident and actual paid fee evidence; finance messages reach the business owner after commit."
    - "Canonical-base and legacy-upgrade SQL Server schemas converge under one ordered repeatable manifest and real application inserts."
    - "Shared fixtures create policy/actors only; normal and exception journeys advance through production service commands with persisted handoff assertions."
    - "SQL, regression and browser/STOMP evidence are separately reported; blocked infrastructure never becomes a passing Phase05 gate."
    - "Missing-parcel investigation alerts reach the authorized origin and destination supervisors and the assigned linehaul driver through private after-commit messages."
re_verification:
  previous_status: human_needed
  previous_score: 12/19
  previous_verified: 2026-10-25T14:07:07Z
  gaps_closed: []
  gaps_remaining: []
  regressions: []
  behavior_evidence_closed: [2, 3, 5, 13, 16, 17]
resolved_gaps:
  - id: V-01
    prior_classification: BLOCKER
    disposition: "Resolved: exact safe MISSING_PARCEL label is retained in public, masked and unlocked recipient timelines; raw and unknown internal content remains excluded."
  - id: V-02
    prior_classification: BLOCKER
    disposition: "Resolved: persisted missing incident and bound trip determine origin/destination supervisor and assigned active driver recipients; deduplicated private after-commit delivery with invalid-reference and rollback guards."
advisory: []
behavior_unverified_items:
  - truth: "A normal shipment completes from creation through hub/linehaul/last-mile delivery and its financial outcome, with coherent tracking and required realtime updates."
    test: "Complete the bounded browser and private STOMP actor matrix in 05-UAT.md and docs/PHASE_05_VERIFICATION.md."
    expected: "Usable real forms; safe missing tracking; intended owner, origin/destination supervisor and assigned driver recipients only; after-commit delivery and rollback silence; independently authorized detail reads."
    why_human: "SQL journeys now pass, but Jasper and Spring listener tests do not exercise live browser JavaScript, visual flow or real broker sessions."
human_verification:
  - test: "Complete the bounded browser and private STOMP actor matrix in 05-UAT.md and docs/PHASE_05_VERIFICATION.md."
    expected: "Usable real forms; safe missing tracking; intended owner, origin/destination supervisor and assigned driver recipients only; after-commit delivery and rollback silence; independently authorized detail reads."
    why_human: "SQL journeys now pass, but Jasper and Spring listener tests do not exercise live browser JavaScript, visual flow or real broker sessions."
---
# Phase 05: Cross-slice Integration Verification Report

**Goal:** As a UTEExpress user or operator with appropriate authorization, I want to integrate the three vertical slices across canonical normal and exception journeys with scoped operational and management views, so that shipments and financial outcomes remain consistent end-to-end.

**Verified:** 2026-10-25T14:44:16Z  
**Status:** human_needed  
**Score:** 18/19 truths verified; 1 present/wired but behavior-unverified  
**Re-verification:** Yes — bounded SQL evidence refresh following the 12/19 report.

SQL schema convergence, persisted journeys, scope and concurrency acceptance now pass. Six formerly unverified runtime truths are closed. No source blocker remains. Live browser and multi-session STOMP acceptance is still pending; Phase05 is not complete and Phase06 must not begin.

## User Flow Coverage

The scope-preserving MVP goal grammar was validated in the initial pass. This refresh retains that contract and the prior source/wiring review.

| Step | Expected | Current evidence | Status |
|---|---|---|---|
| Create and physically receive | Authoritative destination, measurements, payment and origin custody | Normal journey SQL plus existing intake/receipt regressions | VERIFIED |
| Route, load, transport and receive | Vehicle-bound manifest and accountable two-hop custody | Normal/custody SQL families with persisted handoff assertions | VERIFIED |
| Assign, collect, deliver, reconcile and pay out | Scoped assignment, exact source collection, separate payout | Normal/delivery-return/finance SQL families | VERIFIED |
| Resolve exceptions and report scoped work | Lawful return/refund/loss outcomes, correct ledger and contextual queries | Custody, delivery-return, finance and manager-scope SQL families | VERIFIED |
| Use real browser forms and private updates | Intended recipients, after-commit delivery, rollback silence and authorized detail reads | Source/local listener evidence retained; live sessions not observed | UNCERTAIN (WARNING) |

## Fresh SQL and Regression Evidence

The clean regression completed **2026-10-25 21:38:37 +07:00**, duration **2m12s**. This verifier independently inspected current Surefire XML and the structured `05-SQL-RESULTS.json` evidence. No tests were executed during this refresh.

| Evidence | Cases/suites | Result |
|---|---|---|
| Phase05 schema convergence | 9 cases | 9 passed |
| Phase05 normal journey | 1 case | 1 passed |
| Phase05 custody | 2 cases | 2 passed |
| Phase05 delivery/return | 4 cases | 4 passed |
| Phase05 finance | 9 cases | 9 passed |
| Phase05 manager scope | 2 cases | 2 passed |
| All Phase05 SQL families | 27 cases / 6 suites | 27 passed, zero skipped/failures/errors |
| All SQL suites, including previous phases | 50 cases / 15 suites | 50 passed, zero skipped/failures/errors |
| Fresh full regression | 504 cases / 84 suites | 504 passed, zero skipped/failures/errors |

Schema convergence first passed at 21:26:44; the five journey families passed 18/18 at 21:28:15. All results above are included in the subsequent clean regression. The earlier 499-reported/454-passed/45-skipped run and mixed 489-report directory are historical and superseded. Current XML is a complete clean run, not a mixture of named reruns. The formerly gated 45 SQL methods expand to 50 executed cases through parameterization.

The parent ran the documented dedicated-database workflow against `uteexpress_phase05_test_20261025_codex_01a17279`. Existing ignored local datasource/JWT configuration was privately imported into the child process. This verifier did not read or print secrets, run database operations, or alter the original database.

### Bounded Fixture/Oracle Review

Only three small test-data/oracle corrections were reviewed for this refresh:

- `phase-05-integration-fixtures.sql` lowercases fixture email identities to match canonical normalization.
- `ShipmentSqlServerIntegrationTest.java` separates the bounded delivery-hub code from the longer unique district value, with matching cleanup keys.
- `Phase03RoutingSqlServerIntegrationTest.java` uses the current `shipment:<id>:hub:<id>:FORWARD` duplicate-route identity.

These align fixtures with existing constraints; no production schema or guard was weakened. The affected suites passed 8/8 before the complete clean run.

## Observable Truths

Full contract wording is preserved in frontmatter. Prior source/local behavioral evidence is retained; current SQL evidence upgrades truths 2, 3, 5, 13, 16 and 17.

| # | Truth / source | Status | Evidence |
|---|---|---|---|
| 1 | Roadmap: normal journey through delivery/finance/tracking/realtime | UNCERTAIN — PRESENT_BEHAVIOR_UNVERIFIED (WARNING) | Normal SQL journey passes; live browser and multi-session STOMP portion remains pending. |
| 2 | Roadmap: exception state/event/custody/wallet consistency end-to-end | VERIFIED | Custody 2/2, delivery-return 4/4 and finance 9/9 SQL cases pass. |
| 3 | Roadmap: context-scoped manager/admin aggregation | VERIFIED | ManagerScope SQL 2/2 passes, supplementing prior scope/Jasper evidence. |
| 4 | 05-01: authoritative unique destination and terminal routing | VERIFIED | Retained intake/terminal-routing regressions plus normal SQL journey. |
| 5 | 05-01: measured/payment-backed self-dropoff in one transaction | VERIFIED | Real normal journey/receipt persistence and financial SQL acceptance pass. |
| 6 | 05-01: safe persisted journey labels and scoped current work items | VERIFIED | V-01 repair, tracking regression and current custody/manager SQL evidence. |
| 7 | 05-02: eligible vehicle-bound whole-manifest departure | VERIFIED | Retained departure/vehicle/replay checks plus normal/custody SQL. |
| 8 | 05-02: full/discrepant container/handover/trip completion | VERIFIED | Retained terminal receipt/replay checks plus normal/custody SQL. |
| 9 | 05-02: incident-bound missing hold/found/loss with preserved history | VERIFIED | Exact-reference/found/loss checks and custody SQL pass. |
| 10 | 05-03: scoped initial delivery/return assignments from inventory | VERIFIED | Retained assignment guards and service-driven SQL journeys. |
| 11 | 05-03: exact collection and custody/fee-gated return completion | VERIFIED | Retained collection/replay guards plus delivery-return/finance SQL. |
| 12 | 05-03: reverse routing, immutable actual-origin grace, scoped disposal | VERIFIED | Retained reverse/grace/custody guards plus delivery-return SQL. |
| 13 | 05-04: refund outbox/payment/wallet commit together without duplicate credit | VERIFIED | Finance SQL 9/9 passes, including real transaction/replay/concurrency coverage. |
| 14 | 05-04: reconciliation derives exact scoped collected sources | VERIFIED | Retained 26 local source/scope/discrepancy tests plus finance SQL. |
| 15 | 05-04: source-bound compensation and owner after-commit finance messages | VERIFIED | Retained source/owner/callback tests plus finance SQL; live-session acceptance remains in truth 1. |
| 16 | 05-05: canonical/legacy repeatable SQL convergence and mapped inserts | VERIFIED | SchemaConvergence SQL 9/9 passes. |
| 17 | 05-05: service-driven shared-fixture journeys with persisted assertions | VERIFIED | Five production-service journey SQL families pass 18/18. |
| 18 | 05-05: separately and honestly report SQL/regression/browser evidence | VERIFIED | Fresh clean XML and SQL snapshot distinguished from historical runs and pending live UAT. |
| 19 | Workflow §38/G-15: private origin/destination/assigned-driver missing alerts | VERIFIED | Prior V-02 audience/listener checks retained and included in clean regression; live-session proof remains in truth 1. |

No overrides, typed prohibitions or backstop checks were added. One behavior-unverified item is retained in frontmatter and excluded from the score.

## Resolved Source History and Advisory

| Finding | Resolution and retained evidence | Disposition |
|---|---|---|
| V-01 / G-14 | `TrackingServiceImpl` retains the exact safe MISSING_PARCEL label “Đơn hàng đang được kiểm tra hành trình” in public, masked and unlocked recipient timelines; raw/unknown content remains excluded. Corrected tracking test passed previously and in the clean regression; custody SQL now also passes. | RESOLVED |
| V-02 / G-15 | Persisted incident and bound trip select validated origin/destination supervisors and assigned active linehaul driver; deduplicated private after-commit delivery preserves invalid-reference and rollback guards. Focused audience and actual Spring listener tests pass. | RESOLVED |

No new-scope advisory or blocker was introduced. This is a bounded evidence refresh, not a repeated broad source review.

## Required Artifacts, Key Links and Data Flow

The initial manual review established substantive service/controller/repository/JSP/test artifacts for all five plans. PLAN artifact/key-link lists are scalar strings; the helper parsed zero entries, so manual wiring evidence remains authoritative. Existing source links are retained, now supplemented by actual SQL behavior.

| Artifact/link | Data/control flow | Current result |
|---|---|---|
| Intake/receipt services → repositories | Destination, measurement, payment, custody and events persist together | WIRED/FLOWING; normal SQL passes |
| Custody/linehaul/receipt services → persisted journey and tracking | Manifest, trip, incident and allowlisted event projections | WIRED/FLOWING; normal/custody SQL passes |
| Assignment/delivery/return services → custody and finance | Scoped inventory, real collection and lawful return completion | WIRED/FLOWING; delivery-return SQL passes |
| Cancellation/outbox/finance services → source obligations and ledger | Atomic settlement, replay protection and competing transactions | WIRED/FLOWING; finance SQL passes |
| Manager repositories → scoped projection → views | Actual subtree queries and authorized detail reads | WIRED/FLOWING; manager SQL plus prior JSP evidence |
| Migration manifest → canonical/legacy schemas → mapped inserts | Ordered repeatable SQL migration with conflict checks | VERIFIED; schema SQL passes |
| Persisted missing incident → publisher → private minimal DTO | Validated supervisor/driver audience and after-commit callback | WIRED; local behavior passes; actual browser/broker acceptance pending |

## Behavioral Spot-Checks and Probe Execution

This refresh inspected existing completed execution evidence only. No named test, full suite, server, SQL operation or probe was started by this verifier. Prior named tracking and Spring listener spot-check passes remain valid and are also covered by the fresh clean run. The phase's SQL acceptance entry points now have actual unskipped execution evidence; live browser/STOMP remains NOT_RUN.

## Requirements and Research Coverage

| Requirement | Source plans | Current disposition |
|---|---|---|
| INTEG-01 | 01–05 | Normal persisted journey satisfied; NEEDS HUMAN for live browser/realtime acceptance. |
| INTEG-02 | 02–05 | SATISFIED programmatically: exception state/custody/wallet, rollback and concurrent outcomes pass SQL acceptance. |
| INTEG-03 | 01–05 | SATISFIED programmatically: actual scoped aggregation/detail SQL and prior rendering tests pass; browser acceptance remains part of the phase checkpoint. |

No orphaned Phase05 requirement was found in the initial pass. Decision coverage remains 16/16 honored; no decision changed.

| Finding group | Current disposition |
|---|---|
| G-01/G-02 | Intake/physical receipt/surcharge implementation retained; actual SQL journey passes. |
| G-03/G-04/G-05 | Vehicle/manifest/custody/incident implementation retained; SQL persistence and migration pass. |
| G-06/G-07/G-08 | Assignment/reverse-route/grace/return implementation retained; SQL journey passes. |
| G-09/G-10/G-11/G-12/G-13 | Financial source/collection/refund/compensation implementation retained; SQL atomicity and concurrency pass. |
| G-14 | V-01 resolved; safe investigation label now also covered by passing custody SQL. |
| G-15 | V-02 resolved; intended owner/supervisor/driver wiring and local tests pass; live delivery pending. |
| G-16 | Current-hub/direction and scoped work/detail queries pass actual SQL. |
| DB-01/DB-02 | Vehicle/scan mappings and real inserts pass schema convergence. |
| DB-03/DB-04 | Ordered canonical/legacy manifest passes repeated actual SQL execution. DB-04's historical failure hypothesis was not reproduced. |
| DB-05 | Unique source/reference and competing-transaction SQL cases pass. |
| CR-01/CR-02 | Prior route-key/container/disposal fixes retain closure; SQL migration/boundary acceptance passes. |

## Test Quality and Anti-Patterns

Prior debt/stub/source scan evidence is retained. The three reviewed fixture/oracle changes match established identity/length/route-key contracts, with no production guard relaxation. SQL tests execute rather than silently skip. No new blocker or unresolved debt marker was identified in the bounded refresh.

## Human Verification Required

**One acceptance group remains: live browser and private multi-session STOMP.**

**Test:** Complete test 3 in `05-UAT.md` using the bounded actor matrix in `docs/PHASE_05_VERIFICATION.md`: real forms/session/CSRF and management views; public/recipient investigation label; intended owner/driver/sibling origin and destination managers versus unrelated sessions; independently authorized detail reads.

**Expected:** Usable forms and coherent displayed state; safe labels; minimal private notifications only to intended recipients after commit; silence on rollback; unrelated detail access denied.

**Why human:** Passing SQL, Jasper and Spring listener tests do not observe browser JavaScript, visual usability or real broker sessions. SQL acceptance is complete; it must not be rerouted into this pending live check.

Current UAT is **2 pass / 1 pending / 0 issues / 0 blocked**. Electronic COD remains manager-recorded manual/demo evidence, not provider verification. The authorized return fee remains the configurable **25000 VND default**.

## Completion Boundary

No source gap remains. Overall **human_needed**, **18/19**, because the live browser/realtime part of truth 1 remains unobserved. Nothing is deferred to Phase06 to bypass this gate.

Only this report was changed during the refresh. No tests, source, STATE, ROADMAP, database operations, Git mutations or commits were performed. The deterministic fingerprint covers **203 root-relative files**, including the SQL snapshot and corrected routing-test input; hashing was not another broad review.

---

_Verified: 2026-10-25T14:44:16Z_  
_Verifier: the agent (gsd-verifier)_
