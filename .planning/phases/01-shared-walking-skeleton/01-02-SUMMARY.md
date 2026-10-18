---
phase: 01-shared-walking-skeleton
plan: 02
subsystem: shipment-persistence
tags: [spring-jpa, sql-server, jsp, shipment, data-scope, transaction]
requires:
  - phase: 01-01
    provides: Canonical authentication, session/JWT chains, and AuthenticatedActor
provides:
  - Authenticated minimal shipment create/detail vertical tracer
  - Exactly thirteen persisted ShipmentStatus values and separate shipment_events.event_type semantics
  - Transactional shipment-plus-creation-event persistence with owner-only detail access
  - SQL Server integration verification profile and contract tests
affects: [01-03, phase-02, phase-03, phase-04]
tech-stack:
  added: []
  patterns: [dto-at-web-boundary, service-transaction-boundary, prg-jsp-flow, owner-data-scope]
key-files:
  created:
    - src/main/java/vn/iotstar/entity/Shipment.java
    - src/main/java/vn/iotstar/entity/ShipmentEvent.java
    - src/main/java/vn/iotstar/service/impl/ShipmentServiceImpl.java
    - src/test/java/vn/iotstar/integration/ShipmentSqlServerIntegrationTest.java
  modified:
    - src/main/java/vn/iotstar/controller/ShipmentController.java
    - src/main/webapp/WEB-INF/views/shipment/create.jsp
    - src/main/webapp/WEB-INF/views/shipment/detail.jsp
    - src/main/resources/db/phase-01-shipment-events-event-type.sql
key-decisions:
  - "Use CREATED as the Phase-01 initial status and SHIPMENT_CREATED as a distinct event_type."
  - "Persist the shipment and creation event atomically in the service transaction."
  - "Treat SELF_DROPOFF, SENDER, NO_INSPECTION, and UNPAID solely as Phase-01 tracer defaults."
patterns-established:
  - "Shipment controllers send validated DTOs to a service and never access a repository."
  - "A service-level ownership check denies another user before sensitive shipment data or events are returned."
requirements-completed: [SKEL-01, SKEL-03]
status: complete
---

# Plan 01-02 Summary

**An authenticated JSP → controller → DTO → service → JPA → SQL Server shipment tracer with owner-scoped detail readback.**

## Accomplishments

- Added a minimal shipment/create-detail path that persists `CREATED` plus one `SHIPMENT_CREATED` event through the service transaction boundary.
- Enforced the exact 13-value `ShipmentStatus` contract and mapped the separate event field to `shipment_events.event_type`.
- Kept the web boundary DTO-based and bound shipment ownership exclusively to `AuthenticatedActor`; cross-user detail requests are denied.
- Added SQL Server verification that inserts/reads the user, shipment, and shipment event while checking tracking uniqueness, foreign-key behavior, and cleanup.
- Added filter-enabled JSP/session/CSRF route coverage and JSP contract tests for the actual create action and detail-response accessors.

## Verification

- Targeted service, MVC, route-security, and domain-contract tests passed.
- The dedicated real SQL Server command was executed successfully with one test run, zero failures/errors/skips; its outcome is recorded in `01-UAT.md`.
- Current full-suite regression: `mvn test` completed successfully on 2026-10-07; 57 tests passed and the real SQL Server test was condition-skipped only because this ordinary Maven process did not receive its environment variables.
- Person C review approved the tracer after fixes for serializable JPA entities and filter-enabled JSP/session/CSRF coverage.

## Deviations from Plan

The browser form action and record-compatible JSP getters were corrected within the tracer boundary so the live create/detail flow works through the real JSP view resolver. These changes did not add pickup, sorting, linehaul, delivery, returns, COD, wallet, reconciliation, or withdrawal behavior.

## Next Phase Readiness

Plan 01-03 can publish the safe creation event after commit and translate errors at the presentation boundary. The SQL Server/JPA contract remains canonical.

---
*Retrospective execution record created from approved reviews, current code, real-environment UAT evidence, and current tests on 2026-10-07. Manual Git checkpoints are intentionally not reproduced here.*
