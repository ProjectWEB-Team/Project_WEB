---
phase: 05-cross-slice-integration
reviewed: 2026-10-25T13:31:29Z
initial_reviewed: 2026-10-25T13:20:18Z
review_kind: targeted_re_review
initial_files_reviewed: 166
depth: standard
files_reviewed: 167
files_reviewed_list:
  - docs/PHASE_05_VERIFICATION.md
  - src/main/java/vn/iotstar/controller/DriverOperationsController.java
  - src/main/java/vn/iotstar/controller/FinanceReconciliationController.java
  - src/main/java/vn/iotstar/controller/HubOperationsController.java
  - src/main/java/vn/iotstar/controller/InboundOperationsController.java
  - src/main/java/vn/iotstar/controller/LastMileAssignmentController.java
  - src/main/java/vn/iotstar/controller/LastMileDeliveryController.java
  - src/main/java/vn/iotstar/controller/ManagerOperationsController.java
  - src/main/java/vn/iotstar/controller/MissingParcelController.java
  - src/main/java/vn/iotstar/controller/ReturnDisposalController.java
  - src/main/java/vn/iotstar/controller/ShipmentFeeController.java
  - src/main/java/vn/iotstar/dto/delivery/DeliveryAssignmentCandidate.java
  - src/main/java/vn/iotstar/dto/delivery/DeliveryAssignmentRequest.java
  - src/main/java/vn/iotstar/dto/delivery/DeliveryCollectionRequest.java
  - src/main/java/vn/iotstar/dto/driver/TripActionResponse.java
  - src/main/java/vn/iotstar/dto/driver/VehicleOptionResponse.java
  - src/main/java/vn/iotstar/dto/finance/CompensationRequest.java
  - src/main/java/vn/iotstar/dto/finance/LossCompensationCandidate.java
  - src/main/java/vn/iotstar/dto/finance/ReconciliationItemResponse.java
  - src/main/java/vn/iotstar/dto/finance/ReconciliationSubmissionRequest.java
  - src/main/java/vn/iotstar/dto/finance/ReconciliationSummaryResponse.java
  - src/main/java/vn/iotstar/dto/finance/RefundCandidateResponse.java
  - src/main/java/vn/iotstar/dto/hub/DestinationReceiptResponse.java
  - src/main/java/vn/iotstar/dto/hub/DestinationReconciliationCommand.java
  - src/main/java/vn/iotstar/dto/hub/HandoverAcceptanceResponse.java
  - src/main/java/vn/iotstar/dto/hub/InboundShipmentCommand.java
  - src/main/java/vn/iotstar/dto/hub/MissingParcelCaseResponse.java
  - src/main/java/vn/iotstar/dto/hub/MissingParcelResolutionCommand.java
  - src/main/java/vn/iotstar/dto/hub/RouteDiagnosticResponse.java
  - src/main/java/vn/iotstar/dto/hub/SortingDecisionResponse.java
  - src/main/java/vn/iotstar/dto/operations/AuditCustodyReference.java
  - src/main/java/vn/iotstar/dto/operations/ManagerOperationsDashboardResponse.java
  - src/main/java/vn/iotstar/dto/operations/OperationalWorkItemResponse.java
  - src/main/java/vn/iotstar/dto/operations/OperationsAuditTimelineEntry.java
  - src/main/java/vn/iotstar/dto/operations/OperationsAuditTimelineResponse.java
  - src/main/java/vn/iotstar/dto/realtime/NotificationEventResponse.java
  - src/main/java/vn/iotstar/dto/returnflow/ShipmentFeeResponse.java
  - src/main/java/vn/iotstar/entity/CodTransaction.java
  - src/main/java/vn/iotstar/entity/ContainerItem.java
  - src/main/java/vn/iotstar/entity/Handover.java
  - src/main/java/vn/iotstar/entity/HandoverContainer.java
  - src/main/java/vn/iotstar/entity/Incident.java
  - src/main/java/vn/iotstar/entity/ParcelContainer.java
  - src/main/java/vn/iotstar/entity/Payment.java
  - src/main/java/vn/iotstar/entity/RefundSettlementOutbox.java
  - src/main/java/vn/iotstar/entity/Shipment.java
  - src/main/java/vn/iotstar/entity/ShipperReconciliation.java
  - src/main/java/vn/iotstar/entity/TransportTrip.java
  - src/main/java/vn/iotstar/entity/Vehicle.java
  - src/main/java/vn/iotstar/realtime/NotificationEventPublisher.java
  - src/main/java/vn/iotstar/realtime/OperationalRealtimePublisher.java
  - src/main/java/vn/iotstar/realtime/StompNotificationEventPublisher.java
  - src/main/java/vn/iotstar/repository/ContainerHandoverRepository.java
  - src/main/java/vn/iotstar/repository/FinanceReconciliationRepository.java
  - src/main/java/vn/iotstar/repository/HubOperationsRepository.java
  - src/main/java/vn/iotstar/repository/HubRepository.java
  - src/main/java/vn/iotstar/repository/IncidentRepository.java
  - src/main/java/vn/iotstar/repository/LinehaulOperationsRepository.java
  - src/main/java/vn/iotstar/repository/ManagerOperationsRepository.java
  - src/main/java/vn/iotstar/repository/OperationalIncidentRepository.java
  - src/main/java/vn/iotstar/repository/OperationsAuditRepository.java
  - src/main/java/vn/iotstar/repository/PaymentRepository.java
  - src/main/java/vn/iotstar/repository/RefundSettlementOutboxRepository.java
  - src/main/java/vn/iotstar/repository/ShipmentAssignmentRepository.java
  - src/main/java/vn/iotstar/repository/ShipmentEventRepository.java
  - src/main/java/vn/iotstar/repository/ShipmentRepository.java
  - src/main/java/vn/iotstar/repository/ShipperReconciliationItemRepository.java
  - src/main/java/vn/iotstar/service/DestinationReceiptService.java
  - src/main/java/vn/iotstar/service/FinanceReconciliationService.java
  - src/main/java/vn/iotstar/service/impl/ContainerHandoverServiceImpl.java
  - src/main/java/vn/iotstar/service/impl/DestinationReceiptServiceImpl.java
  - src/main/java/vn/iotstar/service/impl/FinanceAccess.java
  - src/main/java/vn/iotstar/service/impl/FinanceReconciliationServiceImpl.java
  - src/main/java/vn/iotstar/service/impl/FirstMileServiceImpl.java
  - src/main/java/vn/iotstar/service/impl/HubOperationsServiceImpl.java
  - src/main/java/vn/iotstar/service/impl/LastMileAccess.java
  - src/main/java/vn/iotstar/service/impl/LastMileAssignmentService.java
  - src/main/java/vn/iotstar/service/impl/LastMileDeliveryServiceImpl.java
  - src/main/java/vn/iotstar/service/impl/LinehaulTripServiceImpl.java
  - src/main/java/vn/iotstar/service/impl/ManagerOperationsServiceImpl.java
  - src/main/java/vn/iotstar/service/impl/OperationsAuditServiceImpl.java
  - src/main/java/vn/iotstar/service/impl/RefundSettlementScheduler.java
  - src/main/java/vn/iotstar/service/impl/ReturnGracePeriodScheduler.java
  - src/main/java/vn/iotstar/service/impl/ShipmentCancellation.java
  - src/main/java/vn/iotstar/service/impl/ShipmentFeeService.java
  - src/main/java/vn/iotstar/service/impl/ShipmentFinanceServiceImpl.java
  - src/main/java/vn/iotstar/service/impl/ShipmentReturnServiceImpl.java
  - src/main/java/vn/iotstar/service/impl/ShipmentServiceImpl.java
  - src/main/java/vn/iotstar/service/impl/ShippingFeeCalculator.java
  - src/main/java/vn/iotstar/service/impl/TrackingServiceImpl.java
  - src/main/java/vn/iotstar/service/impl/WithdrawalServiceImpl.java
  - src/main/java/vn/iotstar/service/LastMileDeliveryService.java
  - src/main/java/vn/iotstar/service/LinehaulTripService.java
  - src/main/java/vn/iotstar/service/ManagerOperationsService.java
  - src/main/java/vn/iotstar/service/ShipmentFinanceService.java
  - src/main/resources/db/phase-03-linehaul-trip-schema.sql
  - src/main/resources/db/phase-05-custody-alignment.sql
  - src/main/resources/db/phase-05-finance-alignment.sql
  - src/main/resources/db/phase-05-intake-service-code.sql
  - src/main/resources/db/phase-05-route-key-alignment.sql
  - src/main/resources/db/phase-05-integration-fixtures.sql
  - src/main/resources/db/phase-05-integration-reset.sql
  - src/main/resources/db/phase-05-migration-manifest.txt
  - src/main/webapp/WEB-INF/views/admin/withdrawals.jsp
  - src/main/webapp/WEB-INF/views/driver/trips.jsp
  - src/main/webapp/WEB-INF/views/hub/audit-timeline.jsp
  - src/main/webapp/WEB-INF/views/hub/delivery-assignments.jsp
  - src/main/webapp/WEB-INF/views/hub/inbound-queue.jsp
  - src/main/webapp/WEB-INF/views/hub/sorter-queue.jsp
  - src/main/webapp/WEB-INF/views/layout/sidebar.jsp
  - src/main/webapp/WEB-INF/views/manager/missing-parcels.jsp
  - src/main/webapp/WEB-INF/views/manager/operations-dashboard.jsp
  - src/main/webapp/WEB-INF/views/manager/operation-work-item.jsp
  - src/main/webapp/WEB-INF/views/manager/overdue-returns.jsp
  - src/main/webapp/WEB-INF/views/manager/reconciliation.jsp
  - src/main/webapp/WEB-INF/views/shipment/detail.jsp
  - src/main/webapp/WEB-INF/views/shipper/delivery-tasks.jsp
  - src/main/webapp/WEB-INF/views/shipper/return-tasks.jsp
  - src/main/webapp/WEB-INF/views/user/return-collection.jsp
  - src/main/webapp/WEB-INF/views/user/shipment-fees.jsp
  - src/main/webapp/WEB-INF/views/user/wallet.jsp
  - src/test/java/vn/iotstar/controller/DestinationReceiptControllerSecurityTest.java
  - src/test/java/vn/iotstar/controller/FinanceReconciliationControllerSecurityTest.java
  - src/test/java/vn/iotstar/controller/HubRoutingControllerSecurityTest.java
  - src/test/java/vn/iotstar/controller/LastMileAssignmentControllerSecurityTest.java
  - src/test/java/vn/iotstar/controller/LastMileDeliveryControllerSecurityTest.java
  - src/test/java/vn/iotstar/controller/LastMileReturnControllerSecurityTest.java
  - src/test/java/vn/iotstar/controller/LinehaulTripRouteSecurityTest.java
  - src/test/java/vn/iotstar/controller/ManagerOperationsControllerSecurityTest.java
  - src/test/java/vn/iotstar/controller/MissingParcelControllerSecurityTest.java
  - src/test/java/vn/iotstar/controller/Phase05OperationsJspRenderingTest.java
  - src/test/java/vn/iotstar/controller/ShipmentFeeControllerSecurityTest.java
  - src/test/java/vn/iotstar/integration/Phase01WalkingSkeletonTest.java
  - src/test/java/vn/iotstar/integration/Phase04FinanceSqlServerIntegrationTest.java
  - src/test/java/vn/iotstar/integration/Phase05CustodySqlServerIntegrationTest.java
  - src/test/java/vn/iotstar/integration/Phase05DeliveryReturnSqlServerIntegrationTest.java
  - src/test/java/vn/iotstar/integration/Phase05FinanceSqlServerIntegrationTest.java
  - src/test/java/vn/iotstar/integration/Phase05ManagerScopeSqlServerIntegrationTest.java
  - src/test/java/vn/iotstar/integration/Phase05NormalJourneySqlServerIntegrationTest.java
  - src/test/java/vn/iotstar/integration/Phase05SchemaConvergenceSqlServerIntegrationTest.java
  - src/test/java/vn/iotstar/integration/Phase05SqlServerFixtureSupport.java
  - src/test/java/vn/iotstar/integration/ShipmentSqlServerIntegrationTest.java
  - src/test/java/vn/iotstar/integration/SqlServerMigrationSupport.java
  - src/test/java/vn/iotstar/integration/SqlServerMigrationSupportTest.java
  - src/test/java/vn/iotstar/realtime/OperationalRealtimePublisherTest.java
  - src/test/java/vn/iotstar/realtime/StompNotificationEventPublisherTest.java
  - src/test/java/vn/iotstar/service/ContainerHandoverServiceImplTest.java
  - src/test/java/vn/iotstar/service/DestinationReceiptServiceImplTest.java
  - src/test/java/vn/iotstar/service/FinanceReconciliationServiceImplTest.java
  - src/test/java/vn/iotstar/service/HubRoutingServiceImplTest.java
  - src/test/java/vn/iotstar/service/impl/RefundProducerConsumerTest.java
  - src/test/java/vn/iotstar/service/impl/ShipmentCancellationTest.java
  - src/test/java/vn/iotstar/service/impl/ShipmentServiceImplTest.java
  - src/test/java/vn/iotstar/service/impl/TrackingServiceImplTest.java
  - src/test/java/vn/iotstar/service/LastMileCollectionValidationTest.java
  - src/test/java/vn/iotstar/service/LastMileDeliveryServiceImplTest.java
  - src/test/java/vn/iotstar/service/LastMileReturnServiceImplTest.java
  - src/test/java/vn/iotstar/service/LastMileTestFixture.java
  - src/test/java/vn/iotstar/service/LinehaulCustodyCompletionTest.java
  - src/test/java/vn/iotstar/service/LinehaulTripServiceImplTest.java
  - src/test/java/vn/iotstar/service/ManagerOperationsServiceImplTest.java
  - src/test/java/vn/iotstar/service/OperationalAuditServiceImplTest.java
  - src/test/java/vn/iotstar/service/SelfDropoffReceiptTest.java
  - src/test/java/vn/iotstar/service/ShipmentFeeServiceTest.java
  - src/test/java/vn/iotstar/service/ShipmentFinanceServiceImplTest.java
  - src/test/java/vn/iotstar/service/ShipmentReturnServiceImplTest.java
  - src/test/java/vn/iotstar/service/WithdrawalServiceImplTest.java
findings:
  critical: 0
  warning: 0
  info: 0
  total: 0
status: clean
resolved_findings:
  - CR-01
  - CR-02
targeted_re_review_files:
  - src/main/resources/db/phase-05-route-key-alignment.sql
  - src/main/resources/db/phase-05-migration-manifest.txt
  - src/main/java/vn/iotstar/repository/ContainerHandoverRepository.java
  - src/main/java/vn/iotstar/service/impl/ContainerHandoverServiceImpl.java
  - src/main/java/vn/iotstar/service/impl/ShipmentReturnServiceImpl.java
  - src/test/java/vn/iotstar/service/ContainerHandoverServiceImplTest.java
  - src/test/java/vn/iotstar/service/ShipmentReturnServiceImplTest.java
  - src/test/java/vn/iotstar/integration/Phase05SchemaConvergenceSqlServerIntegrationTest.java
  - src/test/java/vn/iotstar/integration/Phase05DeliveryReturnSqlServerIntegrationTest.java
  - src/test/java/vn/iotstar/integration/SqlServerMigrationSupportTest.java
---

# Phase 05: Code Review Report

**Initially reviewed:** 2026-10-25T13:20:18Z
**Targeted re-review:** 2026-10-25T13:31:29Z
**Depth:** standard
**Files Reviewed:** 166 initially; 167 cumulative, including the new route-key migration
**Status:** clean (both original findings resolved in the targeted source re-review)

## Summary

The original standard-depth review covered the explicit 166-file current-worktree scope and reported CR-01 and CR-02. This follow-up inspected the ten directly related repair/regression files listed above and their route, custody, event-schema, and form consumers. Both findings are resolved at source level, with no additional defect identified in that targeted scope. This is not a fresh review of all 167 files after the repairs. The added migration is now included in the cumulative review list and 05-REVIEW-SCOPE.txt.

No source files were modified, no tests were run by this reviewer, and no commit was created. Regression assertions were inspected as source, not treated as executed evidence. The orchestrator's full Maven run was still in progress when this disposition was written. Real SQL Server tests remain unexecuted because all four required environment settings are absent, as reported by the orchestrator; live SQL migration and browser UAT are not claimed. The clean verdict records closure of the two source findings and does not remove those environment gates.

## Narrative Findings (AI reviewer)

## Targeted Re-review Dispositions

**CR-01 — RESOLVED (original classification: BLOCKER).** `src/main/resources/db/phase-05-route-key-alignment.sql:17-49` validates route identity, rejects ambiguous legacy direction/status and conflicting canonical keys before mutation, and normalizes only eligible unsuffixed decisions. Its transaction and table locks protect preflight/update consistency; canonical keys are unchanged on rerun, and route IDs, status, timestamps, and next hops are preserved. The 16-script manifest orders it after the return schema and before custody alignment. The existing sorter, pending-route query, loading guard, and backlog query consistently use the normalized direction-suffixed identity. `ContainerHandoverServiceImpl.java:76-84` obtains the shipment write lock before `hasOtherActiveContainerCustody`, which rejects another IN_CONTAINER membership in OPEN, SEALED, or LOADED containers before route consumption or insertion. Historical RECEIVED/MISSING entries are not mistaken for active membership. Regression source covers repeated normalization of legacy PENDING and LOADED rows, re-sort reuse, second-container rejection, conflicting keys with no partial rewrite, and ambiguous return history; the focused service test verifies rejection occurs before mutation.

**CR-02 — RESOLVED (original classification: BLOCKER).** `src/main/java/vn/iotstar/service/impl/ShipmentReturnServiceImpl.java:59-61` retains the 255-character validation and persists the reason unchanged as the RETURN_DISPOSED description. The controller does not add a prefix, the form still allows 255 characters, and ShipmentEvent/SQL retain the matching NVARCHAR(255) limit. The focused regression asserts the full 255-character reason and event type; the SQL regression asserts the stored text and length, closure state, and unchanged origin-arrival timestamp.

## Original Critical Findings (Resolved History)

The following preserves the initial findings and source locations as recorded at 13:20:18Z. Their issue descriptions describe the pre-repair implementation, not the current source. They are excluded from the current open-finding counts above.

### CR-01: Direction-suffixed route keys invalidate existing forward decisions

**Classification:** BLOCKER
**Disposition:** RESOLVED by targeted re-review at 2026-10-25T13:31:29Z; historical finding only.
**File:** `C:/Users/Admin/Desktop/Project WEB/src/main/java/vn/iotstar/entity/Shipment.java:137`
**Related locations:** `C:/Users/Admin/Desktop/Project WEB/src/main/java/vn/iotstar/repository/ContainerHandoverRepository.java:33-37`; `C:/Users/Admin/Desktop/Project WEB/src/main/java/vn/iotstar/service/impl/HubOperationsServiceImpl.java:106-109`; `C:/Users/Admin/Desktop/Project WEB/src/main/java/vn/iotstar/service/impl/ContainerHandoverServiceImpl.java:78-91`.

**Issue:** The baseline sorter at main@ca2bbf8 persisted keys such as `shipment:101:hub:11`. The new producer, pending-route query, and loading guard require `shipment:101:hub:11:FORWARD`. None of the checked-in migrations rewrites existing route keys or supplies a compatibility path. An existing PENDING forward route therefore cannot be loaded after upgrade. Sorting again creates another decision instead of reusing the persisted one. If the legacy decision is already LOADED while the parcel remains AT_HUB in an open or sealed container awaiting departure, re-sorting creates a fresh PENDING decision; loading into another open container checks only membership in that second container. That permits conflicting container manifests for the same parcel. The composite container-item key does not prohibit membership in another container.

**Fix:** Preserve the legacy key for forward decisions while introducing a separate RETURN key, or add an ordered, idempotent migration that safely maps provable legacy forward decisions to their new keys and rejects conflicting/ambiguous rows before deployment. Keep producer, repository query, loading guard, and backlog query consistent. Also reject loading when the shipment already has another active container membership, under the existing shipment lock. Add regression coverage for both legacy PENDING and legacy LOADED decisions through migration and re-sort; assert that existing decisions remain usable and a second active membership cannot be created.

### CR-02: Accepted disposal reasons overflow the event description column

**Classification:** BLOCKER
**Disposition:** RESOLVED by targeted re-review at 2026-10-25T13:31:29Z; historical finding only.
**File:** `C:/Users/Admin/Desktop/Project WEB/src/main/java/vn/iotstar/service/impl/ShipmentReturnServiceImpl.java:59-61`
**Related locations:** `C:/Users/Admin/Desktop/Project WEB/src/main/webapp/WEB-INF/views/manager/overdue-returns.jsp:6`; `C:/Users/Admin/Desktop/Project WEB/src/main/java/vn/iotstar/entity/ShipmentEvent.java:42-43`; `C:/Users/Admin/Desktop/Project WEB/docs/schema.sql:174`.

**Issue:** The form and service accept a disposal reason up to 255 characters, but the saved event prepends the 37-character string `Overdue inventory disposal approved: `. The event description is NVARCHAR(255), and its factory performs no truncation. Any otherwise-valid reason longer than 218 characters exceeds the column limit; a 255-character reason produces 292 characters. SQL Server rejects the audit insert, rolling back the disposal and exposing a persistence failure for accepted input. The existing mock-repository unit tests do not exercise this schema boundary.

**Fix:** Preserve the validated reason directly in the description, since RETURN_DISPOSED already identifies the action, or store the full reason separately and generate a bounded audit summary. If the prefix is required, align the form and service limit to the remaining 218 characters. Add a regression assertion that the longest accepted reason is persisted intact within the schema limit, and cover that boundary in the SQL suite when available.

---

_Initially reviewed: 2026-10-25T13:20:18Z_
_Targeted re-review completed: 2026-10-25T13:31:29Z_
_Reviewer: the agent (gsd-code-reviewer)_
_Depth: standard_
