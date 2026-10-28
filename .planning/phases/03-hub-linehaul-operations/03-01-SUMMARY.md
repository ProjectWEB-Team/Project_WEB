---
phase: 03-hub-linehaul-operations
plan: 01
subsystem: hub-routing
status: complete
requires:
  - Phase 01 security and shipment foundation
provides:
  - Scoped inbound receipt and static next-hop sorting
  - SQL Server hub/profile/routing migration and verification test
affects:
  - Phase 03 custody, linehaul, destination receipt, and manager dashboards
key-files:
  created:
    - src/main/java/vn/iotstar/service/HubOperationsService.java
    - src/main/java/vn/iotstar/service/impl/HubOperationsServiceImpl.java
    - src/main/java/vn/iotstar/controller/HubOperationsController.java
    - src/main/resources/db/phase-03-hub-routing-schema.sql
  modified:
    - src/main/java/vn/iotstar/entity/Shipment.java
    - src/main/java/vn/iotstar/entity/ShipmentEvent.java
    - src/main/java/vn/iotstar/exception/WebExceptionHandler.java
decisions:
  - Static routing derives one next hub only from active hub_routing_rules.
  - Repeated sorting reuses route_decision_key and never duplicates an event.
  - Missing routing rules roll back operational writes and expose only safe staff feedback.
---

# Phase 03 Plan 01: Hub Routing Tracer Summary

Delivered the scoped inbound-to-static-routing vertical slice: JSP form, CSRF-protected MVC controller, DTOs, transactional service, JPA repository locking, SQL Server migration, and security/service tests.

## Completed Work

- Added the canonical Hub, employee profile, static route rule, and current-hop route persistence model.
- Added inbound receipt with `ARRIVED_AT_HUB`; eligible shipments transition to `AT_HUB` at the server-derived staff hub.
- Added sorter routing with `SORTING_COMPLETED`, a deterministic `route_decision_key`, pessimistic shipment locking, and reuse on repeated scans.
- Added safe blocked-route behavior. It writes no route/event and emits an internal `BLOCKED_ROUTE` application event without exposing configuration details to Hub Staff.
- Added Manager/Admin-only route diagnostics with server-derived hub/subtree scope.
- Added `/hub/sorter` JSP workspace and CSRF-protected POST actions; controllers use DTOs and delegate all rules to the service.
- Added forward-only SQL Server migration with current hub/event hub foreign keys, static routing unique/index constraints, and Phase-01 `event_type` reconciliation.

## Verification

- `rtk mvn -Dtest=HubRoutingServiceImplTest,HubRoutingControllerSecurityTest,CanonicalDomainContractTest test` — PASS: 12 tests, 0 failures, 0 errors, 0 skipped.
- `rtk mvn test` — PASS.
- `rtk git diff --check` — PASS.
- `rtk mvn -Dtest=Phase03RoutingSqlServerIntegrationTest test` — the class is condition-skipped: 2 tests skipped because `UTEEXPRESS_SQLSERVER_URL`, `UTEEXPRESS_SQLSERVER_USERNAME`, `UTEEXPRESS_SQLSERVER_PASSWORD`, and `UTEEXPRESS_JWT_SECRET` are absent from this terminal.

## SQL Server Evidence

`REAL_SQLSERVER_VERIFICATION: BLOCKED_BY_ENVIRONMENT`.

The environment-gated integration test verifies a real SQL Server migration, unique route-decision key, required route/event/current-hub foreign keys, no future-hop materialization, unmatched-route rollback, and concurrent sorter convergence once all four required environment variables are available.

## Deviations from Plan

### Auto-fixed Issues

1. **[Rule 2 - Verification completeness] Added missing foreign-key checks and concurrent route proof.**
   - The first migration covered route/profile/rule keys but omitted foreign keys for `shipments.current_hub_id` and `shipment_events.hub_id`.
   - The migration and gated SQL Server test now validate both constraints and concurrent sorter convergence.

## Scope Confirmation

No ShipmentStatus values were added or changed; the canonical six roles remain intact. No pickup, custody/handover, linehaul, delivery, return, COD, wallet, reconciliation, or withdrawal behavior was implemented.

## Self-Check: PASSED

All Plan 03-01 implementation and test artifacts exist. The only unverified gate is real SQL Server execution, which remains explicitly blocked by absent environment configuration.
