---
phase: "03"
slug: "hub-linehaul-operations"
status: draft
nyquist_compliant: false
wave_0_complete: true
created: "2026-10-13"
---

# Phase 03 — Validation Strategy

> Per-phase validation contract for Hub & Linehaul feedback sampling during execution.

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5, Spring Boot Test, MockMvc, Spring Security Test, real SQL Server verification profile |
| **Config file** | `src/test/resources/application.properties`; `src/test/resources/application-sqlserver-verification.properties` |
| **Quick run command** | `rtk mvn -Dtest=HubRoutingServiceImplTest,ContainerHandoverServiceImplTest,LinehaulTripServiceImplTest,DestinationReceiptServiceImplTest test` |
| **Full suite command** | `rtk mvn test` |
| **Estimated runtime** | ~90 seconds without the condition-gated real SQL Server test |

## Sampling Rate

- **After every task commit:** Run the quick command above, or the task's narrower `rtk mvn -Dtest=... test` command.
- **After every plan wave:** Run `rtk mvn test`.
- **Before `/gsd-verify-work`:** Full suite must be green; run real SQL Server verification separately when environment variables are available.
- **Max feedback latency:** 90 seconds for targeted tests.

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 03-01-01 | 01 | 1 | OPS-01/OPS-02 | T-03-01 | Same-hub inbound/sort derives only static next hop; duplicate scan is idempotent | service + SQL Server | `rtk mvn -Dtest=HubRoutingServiceImplTest,Phase03RoutingSqlServerIntegrationTest test` | ❌ W0 | ⬜ pending |
| 03-01-02 | 01 | 1 | OPS-01 | T-03-02 | Missing rule writes no route/container assignment and only scoped supervisor feedback | service + MVC/CSRF | `rtk mvn -Dtest=HubRoutingServiceImplTest,HubRoutingControllerSecurityTest test` | ❌ W0 | ⬜ pending |
| 03-01-03 | 01 | 1 | OPS-01/OPS-02 | T-03-03 | SQL Server constraints prevent duplicate route/event and cross-hub action | integration | `rtk mvn -Dtest=Phase03RoutingSqlServerIntegrationTest test` | ❌ W0 | ⬜ pending |
| 03-02-01 | 02 | 2 | OPS-02/OPS-03 | T-03-04 | Outbound scans every container and driver accepts only assigned, reconciled handover | service + MVC/CSRF | `rtk mvn -Dtest=ContainerHandoverServiceImplTest,HandoverControllerSecurityTest test` | ❌ W0 | ⬜ pending |
| 03-02-02 | 02 | 2 | OPS-03 | T-03-05 | Sealed container/handover corrections are auditable and race-safe | service | `rtk mvn -Dtest=ContainerHandoverServiceImplTest test` | ❌ W0 | ⬜ pending |
| 03-02-03 | 02 | 2 | OPS-03 | T-03-06 | Expected/accepted container constraints persist on SQL Server | integration | `rtk mvn -Dtest=Phase03CustodySqlServerIntegrationTest test` | ❌ W0 | ⬜ pending |
| 03-03-01 | 03 | 3 | OPS-04 | T-03-07 | Driver JWT/JSP access is assignment- and route-scoped through accept/start/arrive | service + MVC/JWT | `rtk mvn -Dtest=LinehaulTripServiceImplTest,LinehaulTripRouteSecurityTest test` | ❌ W0 | ⬜ pending |
| 03-03-02 | 03 | 3 | OPS-05 | T-03-08 | Driver can report only bounded TRIP_DELAY; missing requires receipt evidence | service + API/JWT | `rtk mvn -Dtest=LinehaulTripServiceImplTest,OperationalIncidentApiSecurityTest test` | ❌ W0 | ⬜ pending |
| 03-03-03 | 03 | 3 | OPS-04/OPS-05 | T-03-09 | SQL Server rejects invalid trip transition and incident linkage | integration | `rtk mvn -Dtest=Phase03TripSqlServerIntegrationTest test` | ❌ W0 | ⬜ pending |
| 03-04-01 | 04 | 4 | OPS-03/OPS-04 | T-03-10 | Destination inbound session route/CSRF and reconciliation complete custody | service + MVC/CSRF | `rtk mvn -Dtest=DestinationReceiptServiceImplTest,DestinationReceiptControllerSecurityTest test` | ❌ W0 | ⬜ pending |
| 03-04-02 | 04 | 4 | OPS-05 | T-03-11 | Missing evidence is isolated; valid cargo continues and no new ShipmentStatus appears | service + SQL Server | `rtk mvn -Dtest=DestinationReceiptServiceImplTest,Phase03ReceiptSqlServerIntegrationTest test` | ❌ W0 | ⬜ pending |
| 03-04-03 | 04 | 4 | OPS-03/OPS-04/OPS-05 | T-03-12 | Each allowed producer sends only post-commit, scoped STOMP action event | STOMP + security | `rtk mvn -Dtest=OperationalRealtimePublisherTest,OperationalWebSocketSecurityTest test` | ❌ W0 | ⬜ pending |
| 03-05-01 | 05 | 5 | OPS-01/OPS-02/OPS-03/OPS-04/OPS-05 | T-03-13 | Manager dashboard has descendant-hub scope and no cross-hub leakage | service + MVC/CSRF | `rtk mvn -Dtest=ManagerOperationsServiceImplTest,ManagerOperationsControllerSecurityTest test` | ❌ W0 | ⬜ pending |
| 03-05-02 | 05 | 5 | OPS-03/OPS-04/OPS-05 | T-03-14 | Audit timeline whitelists scoped event/custody references only | service + MVC/CSRF | `rtk mvn -Dtest=OperationalAuditServiceImplTest,OperationalAuditControllerSecurityTest test` | ❌ W0 | ⬜ pending |
| 03-05-03 | 05 | 5 | OPS-01..OPS-05 | T-03-15 | Full boundary regression keeps finance/delivery/return work out of Phase 03 | full suite + manual UAT | `rtk mvn test` | ✅ existing suite / ❌ W0 additions | ⬜ pending |

## Wave 0 Requirements

- Existing infrastructure covers JUnit, MockMvc, Spring Security Test, STOMP, and SQL Server verification profile.
- Each test class marked `❌ W0` must be created in its plan before that plan's first behavior task is considered complete.
- Real SQL Server tests require `UTEEXPRESS_SQLSERVER_URL`, `UTEEXPRESS_SQLSERVER_USERNAME`, `UTEEXPRESS_SQLSERVER_PASSWORD`, and `UTEEXPRESS_JWT_SECRET`; absence is `BLOCKED_BY_ENVIRONMENT`, never PASS.

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Scan-driven route/container/handover workstation | OPS-01–OPS-03 | Barcode/operator flow is visual and role-scoped | Seed a scoped hub staff user; scan valid/duplicate/unmatched shipments and capture queue, blocked feedback, and custody evidence. |
| Driver and destination receipt flow | OPS-04/OPS-05 | Requires separate users and physical-flow comprehension | Use assigned driver and destination inbound user: accept, start, arrive, scan/unbag, then capture normal and missing-item outcomes. |
| Scoped notifications/dashboard | OPS-01–OPS-05 | STOMP subscriptions and JSP visibility require live session proof | Open scoped staff sessions and verify only applicable action events/dashboard rows appear. |

## Validation Sign-Off

- [x] Every planned task has an automated verification command or an explicit Wave 0 test requirement.
- [x] Sampling continuity has no three consecutive tasks without automated verification.
- [x] Wave 0 identifies all missing Phase-03 test references.
- [x] No watch-mode commands are used.
- [ ] `nyquist_compliant: true` set after plans/revision and Wave 0 artifacts are verified.

**Approval:** pending plan revision
