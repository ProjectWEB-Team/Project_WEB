---
phase: 03-hub-linehaul-operations
plan: 05
subsystem: operations-supervision-and-audit
tags: [manager-dashboard, custody-audit, sql-server, jsp, authorization]
requires: [03-01, 03-02, 03-03, 03-04]
provides: [scoped-manager-kpis, scoped-custody-timeline]
affects: [phase-05-integration]
tech-stack: [Spring Boot, JPA, SQL Server, JSP/JSTL, Bootstrap, Spring Security]
key-files:
  created:
    - src/main/java/vn/iotstar/service/ManagerOperationsService.java
    - src/main/java/vn/iotstar/service/OperationsAuditService.java
    - src/main/java/vn/iotstar/controller/ManagerOperationsController.java
    - src/main/java/vn/iotstar/controller/OperationsAuditController.java
    - src/main/webapp/WEB-INF/views/manager/operations-dashboard.jsp
    - src/main/webapp/WEB-INF/views/hub/audit-timeline.jsp
  modified: []
decisions:
  - Manager scope is derived only from the authenticated active profile; Admin scope is explicitly system-wide.
  - Audit responses expose event type, time, hub, actor role, and custody identifiers only.
  - Out-of-scope shipment audit reads return safe not-found behavior before events are loaded.
status: complete
---

# Phase 03 Plan 05: Operations supervision and audit summary

Implemented the final Phase-03 read-only operational surfaces without changing the fixed shipment status set, adding roles, or expanding into Phase 02/04 business flows.

## Delivered

- `/manager/operations` renders a manager/admin dashboard backed by service-authorized scoped KPI projections: inbound, outbound containers, sorting backlog, active handovers/trips, blocked-route evidence, and actionable incidents.
- `ManagerOperationsRepository` resolves manager scope through a SQL Server recursive CTE anchored at the server-derived active employee profile. It uses a path/cycle guard and leaves no caller-selected scope input.
- Audit browser routes are separated by the existing protected workspace prefixes:
  - `/hub/audit/shipments/{shipmentId}`
  - `/manager/audit/shipments/{shipmentId}`
  - `/driver/audit/shipments/{shipmentId}`
- `OperationsAuditService` applies role, active profile, hub/subtree, current shipment location, and driver-custody-assignment checks before loading an immutable, minimal event timeline.
- Timeline DTOs omit customer data, event descriptions, incident notes/evidence, and container contents.

## Verification

- Focused manager/audit plus routing regression suite: **20 tests, 0 failures, 0 errors, 0 skipped**.
- Full `mvn test`: **112 tests, 0 failures, 0 errors, 7 condition-skipped SQL Server environment tests**.
- Live SQL Server manager-scope test with `sqlserver-verification`: **1 test, 0 failures, 0 errors, 0 skipped**. It proved the recursive CTE includes the root and descendant hubs while excluding a sibling, and exercised the audit repository's scope, shipment, event, and custody-reference queries.
- `git diff --check`: **PASS**.

## Scope and security review

- Exactly six persisted roles and thirteen `ShipmentStatus` values remain unchanged.
- Browser access continues to use the existing JSP/session chain and CSRF policy. No new REST/API route or WebSocket behavior was added.
- Controllers call service interfaces only; repositories remain behind service transaction boundaries.
- No delivery, return, COD, wallet, reconciliation, withdrawal, pickup, or account-management feature was added.

## Deviations from Plan

### Auto-fixed issues

**1. [Rule 1 - Test fixture] SQL Server legacy `hubs.code` width rejected the generated integration-test code.**

- **Found during:** live SQL Server verification.
- **Fix:** generated a compact base-36 suffix so the test hub code remains below the legacy column width; the production schema was not widened.
- **Files modified:** `src/test/java/vn/iotstar/integration/Phase03ManagerScopeSqlServerIntegrationTest.java`.

## Remaining human evidence

- `BLOCKED-ROUTE KPI ASSUMPTION:` the committed D-02 implementation deliberately persists no failed route or event, and its transaction rolls back on a no-rule result. The dashboard retains the scoped blocked-route card/query contract, but it has no durable source record to count until a canonical decision authorizes durable blocked-route evidence. This was not changed silently.
- `MANUAL_UAT: BLOCKED` — no multi-account manager/sibling-hub-manager/driver browser walkthrough was performed in this execution.
- Person A security/privacy review and Person C custody/finance-boundary review remain human cross-review steps; no finding is claimed on their behalf.
- Dashboard aggregate and timeline projections have unit/security coverage; their full multi-account live UI walkthrough remains part of the manual UAT gate.

## Self-Check: PASSED

- All service, repository, controller, DTO, JSP, and test artifacts listed above exist.
- No Git staging, commit, branch, or worktree operation was performed.
