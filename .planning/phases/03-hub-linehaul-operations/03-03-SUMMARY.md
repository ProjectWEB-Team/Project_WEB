---
phase: 03-hub-linehaul-operations
plan: 03
subsystem: assigned-driver-linehaul
status: complete
requires: [03-02]
provides: [assigned-driver-trip-lifecycle, trip-delay-evidence]
affects: [03-04-destination-receipt]
tech-stack: [Spring MVC, Spring Security, JPA, SQL Server]
key-files:
  created:
    - src/main/java/vn/iotstar/service/LinehaulTripService.java
    - src/main/java/vn/iotstar/service/impl/LinehaulTripServiceImpl.java
    - src/main/java/vn/iotstar/controller/DriverOperationsController.java
    - src/main/resources/db/phase-03-linehaul-trip-schema.sql
  modified:
    - src/main/java/vn/iotstar/entity/TransportTrip.java
    - src/main/java/vn/iotstar/entity/Handover.java
    - src/main/java/vn/iotstar/entity/ShipmentEvent.java
decisions:
  - Arrival leaves the handover accepted; destination inbound receipt remains the only custody completion action.
  - Driver-created incidents are limited to bounded TRIP_DELAY evidence on an assigned started trip.
---

# Phase 03 Plan 03: Assigned Driver Trip Controls Summary

Implemented the assigned-driver lifecycle from an accepted handover through trip start and destination arrival, with service-owned assignment/profile checks and no custody completion shortcut.

## Delivered

- Added locked linehaul repository and transactional service that derives the actor from `AuthenticatedActor`, requires an active `ROLE_LINEHAUL_DRIVER` profile, and limits every action to the assigned resource.
- Added accepted → started → arrived trip transitions with server-derived origin/destination checks, idempotent repeated actions, actor/timestamp persistence, shipment `event_type` evidence, and scoped action notifications.
- Added bounded `TRIP_DELAY` incident evidence only. There is no driver route for `MISSING_PARCEL`, finance, delivery, or custody receipt.
- Added CSRF-protected JSP driver commands and a stateless `/api/driver/**` delay endpoint; the controller has no repository dependency.
- Added a forward-only SQL Server migration for trip provenance and incident foreign keys/constraint, plus real-environment-gated integration test coverage.

## Verification

- `rtk mvn "-Dtest=LinehaulTripServiceImplTest,LinehaulTripRouteSecurityTest,CanonicalDomainContractTest" test` — 8 tests passed.
- `rtk mvn "-Dtest=LinehaulTripServiceImplTest,OperationalIncidentApiSecurityTest" test` — 4 tests passed.
- `rtk mvn test` — BUILD SUCCESS. Environment-gated SQL Server tests remain skipped when SQL Server variables are absent.
- `rtk git diff --check` — passed.

## Blockers

- `REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT` unless `UTEEXPRESS_SQLSERVER_URL`, `UTEEXPRESS_SQLSERVER_USERNAME`, `UTEEXPRESS_SQLSERVER_PASSWORD`, and `UTEEXPRESS_JWT_SECRET` are provided. No alternate database was used.
- Git commit was not created because the sandbox approval reviewer rejected the Git write before executing it. Application changes remain unstaged.

## Deviations from Plan

None in business scope. The driver UI uses identifier inputs because driver queue/projection work remains in a later operational workspace plan; server-side assignment scope still governs every action.

## Self-Check: PASSED

All listed source, test, migration, and JSP files exist. No Phase 02/04 behavior was introduced.
