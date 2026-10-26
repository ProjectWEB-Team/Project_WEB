---
phase: 03-hub-linehaul-operations
reviewed: 2026-10-19T00:00:00Z
depth: deep
files_reviewed: 41
files_reviewed_list:
  - src/main/java/vn/iotstar/config/SecurityConfig.java
  - src/main/java/vn/iotstar/config/WebSocketConfig.java
  - src/main/java/vn/iotstar/controller/DriverHandoverController.java
  - src/main/java/vn/iotstar/controller/DriverOperationsController.java
  - src/main/java/vn/iotstar/controller/HubOperationsController.java
  - src/main/java/vn/iotstar/controller/InboundOperationsController.java
  - src/main/java/vn/iotstar/controller/ManagerOperationsController.java
  - src/main/java/vn/iotstar/controller/OperationsAuditController.java
  - src/main/java/vn/iotstar/controller/OutboundOperationsController.java
  - src/main/java/vn/iotstar/dto/hub/DestinationReceiptCommand.java
  - src/main/java/vn/iotstar/dto/hub/DestinationReconciliationCommand.java
  - src/main/java/vn/iotstar/dto/hub/InboundShipmentCommand.java
  - src/main/java/vn/iotstar/entity/ContainerItem.java
  - src/main/java/vn/iotstar/entity/Handover.java
  - src/main/java/vn/iotstar/entity/Incident.java
  - src/main/java/vn/iotstar/entity/ParcelContainer.java
  - src/main/java/vn/iotstar/entity/Shipment.java
  - src/main/java/vn/iotstar/entity/ShipmentEvent.java
  - src/main/java/vn/iotstar/entity/ShipmentRoute.java
  - src/main/java/vn/iotstar/entity/ShipmentStatus.java
  - src/main/java/vn/iotstar/entity/TransportTrip.java
  - src/main/java/vn/iotstar/realtime/OperationalRealtimePublisher.java
  - src/main/java/vn/iotstar/repository/ContainerHandoverRepository.java
  - src/main/java/vn/iotstar/repository/HubOperationsRepository.java
  - src/main/java/vn/iotstar/repository/ManagerOperationsRepository.java
  - src/main/java/vn/iotstar/repository/OperationsAuditRepository.java
  - src/main/java/vn/iotstar/service/impl/ContainerHandoverServiceImpl.java
  - src/main/java/vn/iotstar/service/impl/DestinationReceiptServiceImpl.java
  - src/main/java/vn/iotstar/service/impl/HubOperationsServiceImpl.java
  - src/main/java/vn/iotstar/service/impl/LinehaulTripServiceImpl.java
  - src/main/java/vn/iotstar/service/impl/OperationsAuditServiceImpl.java
  - src/main/resources/db/phase-01-shipment-events-event-type.sql
  - src/main/resources/db/phase-03-container-handover-schema.sql
  - src/main/resources/db/phase-03-destination-receipt-schema.sql
  - src/main/resources/db/phase-03-hub-routing-schema.sql
  - src/main/resources/db/phase-03-linehaul-trip-schema.sql
  - src/test/java/vn/iotstar/config/WebSocketConfigTest.java
  - src/test/java/vn/iotstar/realtime/OperationalRealtimePublisherTest.java
  - src/test/java/vn/iotstar/service/HubRoutingServiceImplTest.java
  - src/test/java/vn/iotstar/service/OperationalAuditServiceImplTest.java
  - src/test/java/vn/iotstar/controller/OperationalAuditControllerSecurityTest.java
findings:
  critical: 4
  warning: 4
  info: 0
  total: 8
status: issues_found
---

# Phase 03: Deep Code Review Report

**Reviewed:** 2026-10-19
**Depth:** deep (static source and cross-module tracing)
**Status:** issues_found

## Summary

The review traced the Phase-03 hub receipt, static routing, container/handover, linehaul, destination receipt, private STOMP, manager dashboard, audit timeline, and SQL Server migration paths against the canonical authority chain and Plans 03-01 through 03-05. The six canonical roles, fixed thirteen-value `ShipmentStatus`, and `shipment_events.event_type` separation remain intact. Controllers delegate to services and no Controller-to-Repository dependency was found in the reviewed Phase-03 paths.

However, four defects block Phase-03 from being considered secure/correct: an unrelated inbound operator can move an in-transit shipment to their hub; repeated multi-hop routing has no current-route selection; a driver assigned to any historical handover can read the full subsequent audit history; and the required blocked-route dashboard/realtime evidence can never exist after the transaction is rolled back. The report is static architecture evidence only. It does not replace the required Person A / Person C human cross-reviews or multi-account manual UAT.

## Narrative Findings (AI reviewer)

## Critical Issues

### CR-01: Inbound receipt lacks a shipment-to-actor hub proof

**File:** `src/main/java/vn/iotstar/service/impl/HubOperationsServiceImpl.java:52-61`

**Issue:** `receiveAtHub` verifies that the actor has an active `INBOUND` profile, but it does not establish that the shipment is currently assigned to that hub or that the inbound action is backed by an expected next-hop/accepted custody record. Any hub staff member who knows an eligible shipment ID in `WAITING_FOR_HANDOVER`, `PICKED_UP`, or `IN_TRANSIT` can set `currentHub` to their own hub and append `ARRIVED_AT_HUB`. This is a contextual-authorization bypass and corrupts custody/location state.

**Canonical rule affected:** ADR-015 and Workflow section 3 require role + assigned hub + position + assignment + current shipment location. Plan 03-01 requires scoped inbound receipt, rather than role-only access.

**Fix:** At the service boundary, require an expected inbound relationship before mutating the shipment: for a linehaul arrival, validate the accepted handover/trip destination and container membership; for initial intake, validate the canonical origin/intake assignment once provided by Phase 02. Reject an unrelated hub before calling `Shipment.receiveAt`. Add a negative test using an `IN_TRANSIT` shipment whose expected destination is another hub.

### CR-02: Outbound loading can select an old route from an earlier hub

**File:** `src/main/java/vn/iotstar/repository/ContainerHandoverRepository.java:33-35`

**Issue:** `findCurrentRouteForShipment` is neither scoped to `current_hub_id` nor ordered, and it has no status predicate. `ShipmentRoute` records a row per `shipment + current hub` and never transitions the prior row out of `PENDING`. After a shipment completes its first hop and is sorted at the next hub, this query can return the original next-hop route. `loadShipment` then either rejects a valid container because its destination differs or loads toward the wrong earlier route, depending on the database result order.

**Canonical rule affected:** ADR-005 and Workflow sections 34/39 define the static matrix as the current hub's next-hop decision; Plan 03-01 D-03 requires deterministic idempotent current-hop routing.

**Fix:** Query by both `shipment.id` and the server-derived current hub, with a deterministic active/current status. Explicitly retire/consume the prior route during the handover/receipt transition, or model an unambiguous current-route invariant. Add a two-hop integration test proving a shipment sorted at hub B cannot load using its old hub-A route.

### CR-03: Historical driver custody grants access to all future audit events

**File:** `src/main/java/vn/iotstar/service/impl/OperationsAuditServiceImpl.java:39-46, 50-55` and `src/main/java/vn/iotstar/repository/OperationsAuditRepository.java:46-50`

**Issue:** Driver authorization succeeds if any custody row for the shipment has `assignedDriverUserId == actor.userId()`. The query includes all handovers for every container ever associated with the shipment, including completed historical trips. Once authorized, the driver receives the entire event timeline, including later hub operations and future linehaul legs unrelated to that driver. This is a cross-driver operational-data disclosure.

**Canonical rule affected:** ADR-015; Plan 03-05 D-15 requires assignment/resource ownership plus current shipment location before disclosure, and explicitly prohibits unrelated custody detail.

**Fix:** Authorize drivers only against the current active handover/trip (or filter the returned timeline to the specific custody interval they own). Do not use historical handover references as a permanent grant. Add a negative test where driver A completed the first leg, driver B owns the second leg, and driver A cannot read B's events.

### CR-04: Blocked-route evidence and notification are impossible after rollback

**File:** `src/main/java/vn/iotstar/service/impl/HubOperationsServiceImpl.java:125-128`, `src/main/java/vn/iotstar/realtime/OperationalRealtimePublisher.java:31-35`, and `src/main/java/vn/iotstar/repository/ManagerOperationsRepository.java:71-74`

**Issue:** The no-rule path publishes an application event and then throws `RouteConfigurationException`. Because the publisher is an `AFTER_COMMIT` listener, no `BLOCKED_ROUTE` message is delivered when this transaction rolls back. The dashboard counts persisted `ShipmentEvent` rows with `ROUTE_LOOKUP_BLOCKED`, but this path deliberately persists no event. Consequently the dashboard's blocked-route KPI is permanently zero and the allowed blocked-route notification cannot occur.

**Canonical rule affected:** Plan 03-05 requires a scoped blocked-route projection; Plan 03-04 D-13 lists `BLOCKED_ROUTE` as an action-required notification while maintaining post-commit delivery. The existing plan/summaries identify this as unresolved, so it cannot be left as a working implementation claim.

**Fix:** Resolve the documented contract conflict before release. If durable diagnostics are authorized, persist a minimal, non-routing diagnostic/audit record in a transaction that commits without creating a route/container assignment, then notify after commit. If durable diagnostics are not authorized, remove the KPI/notification claim and route manager diagnosis through the already scoped read path. Do not silently choose either behavior without the corresponding canonical decision.

## Warnings

### WR-01: Audit timeline assigns one arbitrary custody reference to every event

**File:** `src/main/java/vn/iotstar/service/impl/OperationsAuditServiceImpl.java:44-46, 69-72` and `src/main/java/vn/iotstar/repository/OperationsAuditRepository.java:46-50`

**Issue:** The repository returns every custody reference for a shipment, but the service chooses `references.getFirst()` and attaches it to every event. A shipment can be re-containerized on later hops, so the displayed container/handover/trip can be unrelated to a particular event and query order is not deterministic.

**Canonical rule affected:** Plan 03-05 D-15's custody timeline must provide accurate, immutable operational evidence.

**Fix:** Persist/derive a per-event custody correlation, or omit a reference where it cannot be accurately associated. Add a multi-hop test asserting each event maps only to its own custody leg.

### WR-02: Realtime tests do not exercise the permission or delivery contract

**File:** `src/test/java/vn/iotstar/realtime/OperationalRealtimePublisherTest.java:13-19`

**Issue:** The sole publisher test verifies that an unallowlisted type does not send. It does not test any of the five permitted types, server-resolved recipient sets, safe payloads, `AFTER_COMMIT` behavior, rollback suppression, or unrelated-user exclusion. The only STOMP interceptor tests also accept any authenticated client at the generic private operation queue, so they cannot substantiate the Plan 03-04 negative-scope evidence.

**Canonical rule affected:** Plan 03-04 D-07/D-13 and its required tests for five allowed message types, recipient authorization, rollback suppression, and absence for non-listed operations.

**Fix:** Add integration-level tests with real transactions and a mocked messaging template to verify each allowed producer path after commit, no send after rollback, only expected usernames, and no recipient for unrelated hub/driver accounts.

### WR-03: Legacy SQL Server migration does not enforce required incident provenance

**File:** `src/main/resources/db/phase-03-linehaul-trip-schema.sql:31-42`

**Issue:** When upgrading an existing `incidents` table, all newly added provenance columns are nullable and remain nullable; the script adds foreign keys but neither backfills data nor alters `transport_trip_id`, `hub_id`, `actor_user_id`, `incident_type`, `note`, and `created_at` to `NOT NULL`. This differs from the entity's required associations and the fresh-table schema, allowing incomplete legacy incident rows indefinitely.

**Canonical rule affected:** Workflow incident accountability and Plan 03-03/03-04 provenance requirements; SQL Server is the canonical persistence authority under ADR-010.

**Fix:** Add a documented forward reconciliation: either populate/resolve legacy rows then enforce `NOT NULL`, or explicitly quarantine irreconcilable legacy rows and fail migration with a clear actionable error. Verify required column nullability in the real SQL Server migration test.

### WR-04: Handover creation accepts a role-only driver assignment that cannot execute

**File:** `src/main/java/vn/iotstar/service/impl/ContainerHandoverServiceImpl.java:93-101` and `src/main/java/vn/iotstar/service/impl/LinehaulTripServiceImpl.java:127-133`

**Issue:** The outbound service checks only `UserRole.ROLE_LINEHAUL_DRIVER` when assigning a handover. Later, the same driver is required to have an active employee profile with position `LINEHAUL_DRIVER` before accepting or starting the trip. An inactive/unprofiled driver can therefore be assigned and permanently block the handover.

**Canonical rule affected:** Workflow section 3 contextual authorization and Plan 03-03 D-11 assigned-driver resource controls.

**Fix:** Validate the selected driver has an active `LINEHAUL_DRIVER` employee profile (and any required hub compatibility) before creating the handover. Add a test for inactive/missing driver profile rejection.

---

_Reviewed: 2026-10-19T00:00:00Z_
_Reviewer: gsd-code-reviewer (static architecture evidence only)_
_Depth: deep_

## Critical-Fix Re-review Addendum — 2026-10-19

**Method:** static source, test, migration, and ADR-016 inspection only. No test execution, manual UAT, or human Person A/Person C review is represented here.

| Original finding | Result | Evidence |
|---|---|---|
| CR-01 | **FAIL** | Cross-hub receipt is now constrained, but the generic inbound route can still bypass destination item-level receipt for a missing/discrepant shipment. |
| CR-02 | **PASS** | The lookup is now hub-scoped and pending-only; successful outbound loading consumes that route. |
| CR-03 | **PASS** | Driver audit access is now limited to currently `ACCEPTED` custody assigned to that driver. |
| CR-04 | **PASS** | ADR-016 supplies the required durable exception contract; the code/migration persist and count it in a committed transaction. |

### CR-01 re-review — FAIL

**Files:** `src/main/java/vn/iotstar/service/impl/HubOperationsServiceImpl.java:53-62, 149-156`; `src/main/java/vn/iotstar/repository/HubOperationsRepository.java:43-48`.

The unrelated-hub part is fixed: initial intake now requires `shipment.currentHub == profile.hub`, while an in-transit item requires a custody relationship whose destination is the actor's hub. The new negative tests cover a different hub and an initial-intake hub mismatch.

However, `hasArrivedExpectedInboundCustody` accepts handovers in `DISCREPANCY` and does not require the specific `ContainerItem` to be unreceived/non-missing. A shipment marked missing by destination reconciliation remains `IN_TRANSIT`; a destination staff member can then call the generic `POST /hub/inbound` route with only its shipment ID, satisfy this query through the already-arrived discrepant handover, and set it to `AT_HUB` without the container/item scan or receipt evidence required by the destination receipt boundary. This can clear the exceptional shipment path and creates duplicate or contradictory `ARRIVED_AT_HUB` history.

**Required correction:** do not allow the generic shipment-only inbound endpoint to process `IN_TRANSIT` custody arrivals. Require the destination receipt/reconciliation service with handover + container + item evidence, or extend the authorization query and command so it proves an unreceived, non-missing item in the currently accepted custody. Add a negative test for an `IN_TRANSIT` missing item/handover `DISCREPANCY`.

### CR-02 re-review — PASS

**Files:** `src/main/java/vn/iotstar/repository/ContainerHandoverRepository.java:33-37`; `src/main/java/vn/iotstar/service/impl/ContainerHandoverServiceImpl.java:83-88`; `src/main/java/vn/iotstar/entity/ShipmentRoute.java:52-58`.

The route query now requires the shipment's server-derived current hub and `PENDING` status, orders deterministically, and `loadShipment` calls `markOutboundLoaded()` after matching the destination. The added two-hop unit test verifies hub B requests hub B's next hop rather than the historical hub-A route.

### CR-03 re-review — PASS

**Files:** `src/main/java/vn/iotstar/repository/OperationsAuditRepository.java:53-60`; `src/main/java/vn/iotstar/service/impl/OperationsAuditServiceImpl.java:50-62`; `src/test/java/vn/iotstar/service/OperationalAuditServiceImplTest.java:85-96`.

Driver access now queries only custody assigned to that driver with handover status `ACCEPTED`; it no longer consults all historical custody references. The added second-driver test verifies a historical driver receives safe not-found before events are read.

### CR-04 re-review — PASS

**Files:** `docs/ARCHITECTURE_DECISIONS.md:155-163`; `src/main/java/vn/iotstar/service/impl/HubOperationsServiceImpl.java:64-65, 104-143`; `src/main/java/vn/iotstar/repository/ManagerOperationsRepository.java:71-75`; `src/main/resources/db/phase-03-hub-routing-schema.sql:132-160`.

ADR-016 resolves the earlier documentation conflict. On a failed static lookup, a unique open `routing_exceptions` record and one `ROUTE_LOOKUP_BLOCKED` event are persisted; `noRollbackFor = RouteConfigurationException.class` allows the transaction to commit, so the existing after-commit publisher can deliver its server-scoped alert. The manager KPI now counts only open durable exceptions and the SQL Server test verifies create, deduplication, resolution, and KPI transitions.
