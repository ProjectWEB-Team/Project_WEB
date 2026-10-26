# Phase 06 — Security + UAT + Demo Hardening: report for the owner

Regression commit: 5496064

Source branch `feature/phase-06-hardening` (from `main` at `6a238b1`); planning documents on `project-plans`. Severity rubric: ASVS L1, block on high (see 06-01 context).

## 1. Verdict per requirement

| Requirement | Verdict |
|---|---|
| HARD-01 contextual authorization | Automated evidence complete for the route matrix (every controller route × 6 roles × anonymous × CSRF, plus the `/api/**` chain), the service matrices (ownership/finance/last-mile and hub/custody/linehaul/operations) and the STOMP matrix. No CRITICAL or HIGH finding. MEDIUM and LOW findings are recorded for owner decision (section 5). |
| HARD-02 UAT evidence | Automated evidence complete for the D-06 categories, including multi-session STOMP delivery in Maven (technical part of 05-UAT item 3). The real-SQL suites were **not** executed in this run (BLOCKED_BY_ENVIRONMENT, section 2). Browser and multi-session manual checks are NOT_RUN (`06-UAT-RESULTS.md` section 4). |
| HARD-03 demo mode | Both Workflow §101 flags are implemented, enabled only by the demo profile, and proven inert outside it. The primary and exception demo scripts are written; every result is NOT_RUN. The primary demo has a UI gap at the driver container scan (F-06-U-01). |

## 2. Evidence

| Item | Value |
|---|---|
| Command | `mvn -B clean test` on JDK 22 (Byte Buddy in this project does not support the machine's default JDK 25) |
| Date | 2026-10-26 23:02 +07:00 |
| Totals | Tests run 1682, Failures 0, Errors 0, Skipped 45; BUILD SUCCESS; 94 test classes reported |
| Skipped | 45 cases in the 15 SQL Server suites (`Phase03*`, `Phase04*`, `Phase05*`, `RegistrationSqlServerIntegrationTest`, `ShipmentSqlServerIntegrationTest`), gated by `Phase05SqlServerFixtureSupport#configured` |
| SQL status | REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT. The owner's private loader (`.local/phase05-sql.ps1 -Action All`) against the dedicated `uteexpress_phase05_test_*` database must be run by a person; skipped cases are not passes. |
| Baseline | 516 tests (2026-10-26 10:03, SQL environment loaded, 0 skipped) |

New or extended test classes (all on the source branch):

| Class | Tests | Plan |
|---|---|---|
| PermissionMatrixRouteTest | 1002 | 06-01 |
| PermissionMatrixApiChainTest | 56 | 06-01 |
| PermissionMatrixShipmentOwnershipSliceTest | 6 | 06-02 |
| PermissionMatrixOwnershipServiceTest | 8 | 06-02 |
| PermissionMatrixFinanceScopeServiceTest | 10 | 06-02 |
| StompMultiSessionDeliveryTest | 12 | 06-03 |
| WebSocketConfigTest | 22 (was 3) | 06-03 |
| HubRoutingServiceImplTest | 20 (was 13) | 06-04 |
| LinehaulTripServiceImplTest | 7 (was 2) | 06-04 |
| DemoProfilePropertiesTest | 5 | 06-04 |
| PermissionMatrixHubServiceTest | 27 | 06-05 |
| PermissionMatrixTripAndScopeServiceTest | 14 | 06-05 |

Production changes (plan 06-04 only): `LogisticsProperties.java`, `application-demo.properties`, `HubOperationsServiceImpl.java`, `LinehaulTripServiceImpl.java`.

## 3. Consolidated findings

| ID | Severity | Surface | Description | Disposition | Evidence |
|---|---|---|---|---|---|
| F-06-R-01 | MEDIUM | `/uploads/**` | Delivery evidence files of any extension are served same-origin to any authenticated principal. | RECORDED | 06-MATRIX-ROUTES.md |
| F-06-R-02 | LOW | `formLogin` | Every role lands on `/user/shipments/create` after login. | RECORDED | 06-MATRIX-ROUTES.md |
| F-06-R-03 | LOW | `/api/calculator/**` | Public rule with no handler. | RECORDED | 06-MATRIX-ROUTES.md |
| F-06-R-04 | LOW | `JwtAuthenticationFilter` | Does not check `isAccountNonLocked()`. | RECORDED | 06-MATRIX-ROUTES.md |
| F-06-O-01 | MEDIUM | `WebExceptionHandler` | A plain Spring `AccessDeniedException` from a service renders HTTP 500 instead of 403 (still denied, nothing exposed). | RECORDED | 06-MATRIX-SERVICE-OWNERSHIP.md (probe) |
| F-06-O-02 | MEDIUM | `WalletLedgerServiceImpl.getWalletResponse`, `getTransactions` | No caller-identity check inside the service; the only caller passes the actor id. | RECORDED | 06-MATRIX-SERVICE-OWNERSHIP.md |
| F-06-O-03 | LOW | `ShipmentServiceImpl.create` | Any role can create a shipment, but only `ROLE_USER` can pay its fees online. | RECORDED | 06-MATRIX-SERVICE-OWNERSHIP.md |
| F-06-O-04 | LOW | Flash messages | Controllers flash `exception.getMessage()`; database text can reach users. | RECORDED | 06-MATRIX-SERVICE-OWNERSHIP.md |
| F-06-O-05 | LOW | `settlePendingRefund` | Public method without an actor check (scheduler-only caller, re-validates). | RECORDED | 06-MATRIX-SERVICE-OWNERSHIP.md |
| F-06-O-06 | LOW | `ShipmentServiceImpl.getDetail` | 404 vs 403 reveals whether a shipment id exists. | RECORDED | 06-MATRIX-SERVICE-OWNERSHIP.md |
| F-06-O-07 | LOW | `getDeliveryTask` | Returns the shipper's completed historical assignment. | RECORDED | 06-MATRIX-SERVICE-OWNERSHIP.md |
| F-06-S-01 | LOW | STOMP interceptor | DISCONNECT/UNSUBSCRIBE pass without a principal check (no data). | RECORDED | 06-MATRIX-STOMP.md |
| F-06-S-02 | LOW | `OperationalRealtimePublisher` | Handover/trip driver recipient not re-checked for the driver role. | RECORDED | 06-MATRIX-STOMP.md |
| F-06-H-01 | MEDIUM | `FirstMileServiceImpl.pendingPickups` | Unclaimed home pickups (sender name, phone, address) are visible to and claimable by coordinators of every hub because no origin hub is set at creation. | RECORDED (owner decision) | 06-MATRIX-SERVICE-OPERATIONS.md |
| F-06-H-02 | LOW | `createHandover` | Receiver driver checked by role only (blocked later at acceptance). | RECORDED | 06-MATRIX-SERVICE-OPERATIONS.md |
| F-06-H-03 | LOW | Audit timeline | Any hub-staff position at the current hub reads the timeline. | RECORDED | 06-MATRIX-SERVICE-OPERATIONS.md |
| F-06-D-01 | LOW | Audit trail | Demo-flag decisions are not distinguishable from manual ones. | ACCEPTED (owner review) | 06-MATRIX-DEMO.md |
| F-06-D-02 | LOW | Configuration | Any property source can enable a demo flag. | ACCEPTED (owner review) | 06-MATRIX-DEMO.md |
| F-06-D-03 | LOW | Documentation | Workflow `app.*` keys are silently ignored. | MITIGATED | 06-MATRIX-DEMO.md |
| F-06-U-01 | MEDIUM | `driver/trips.jsp` | No form for `POST /driver/handovers/scan`, so a driver cannot reach handover `ACCEPTED` from the browser; the page also lists no assigned handovers or trips. Blocks the primary demo at Tab 4. Functional, not a security exposure. | RECORDED | 06-DEMO-SCRIPT.md step 5.9 |

## 4. Gate

No finding has Severity CRITICAL or HIGH, so nothing was escalated and `PermissionMatrixEscalatedRegressionTest` was not created. `SecurityConfig.java`, `WebSocketConfig.java`, the publishers and the services in 06-02/06-05 scope are unchanged.

## 5. Owner decisions (recommendations, not applied — D-11)

| Finding | Recommended action |
|---|---|
| F-06-U-01 | Add a "scan container" form (handoverId, containerId) to `driver/trips.jsp` posting to `/driver/handovers/scan`; ideally list the driver's pending handovers and trips. Needed before the live demo. |
| F-06-H-01 | Decide whether the pickup queue is national or per area. If per area: set `originHubId` at creation from the sender's province (same lookup as the destination) and drop the `originHubId is null` branch in `findPendingPickups`. |
| F-06-O-01 | Add `@ExceptionHandler(AccessDeniedException.class)` returning 403 `error/403` to `WebExceptionHandler` (one method). |
| F-06-R-01 | Allow only image extensions/content types for delivery evidence; optionally serve uploads through a scoped controller. |
| F-06-O-02 | Check `actor.userId()` (or admin) inside the two wallet read methods. |
| F-06-O-04 | Flash generic messages for unexpected exceptions; keep specific text only for validation messages. |
| LOW items (F-06-R-02..04, F-06-O-03/05/06/07, F-06-S-01/02, F-06-H-02/03, F-06-D-01/02) | Review as policy or hardening backlog; none exposes data today. |

Outside this phase: the blank withdrawal idempotency key bug is fixed on its own branch `fix/withdrawal-blank-key` (not merged here).

## 6. Decision traceability

| Decision | Honored by |
|---|---|
| D-01 full permission matrix | 06-01, 06-02, 06-03, 06-05; `06-MATRIX-*.md` |
| D-02 no method-security migration | No `@PreAuthorize` added; observation in 06-MATRIX-ROUTES.md |
| D-03 CSRF on web, off on `/api/**` | PermissionMatrixRouteTest no-CSRF cells, PermissionMatrixApiChainTest |
| D-04 multi-session STOMP in Maven | StompMultiSessionDeliveryTest |
| D-05 manual checks stay NOT_RUN | 06-UAT-RESULTS.md section 4, 06-DEMO-SCRIPT.md |
| D-06 extend, do not duplicate; never reset the app DB | Matrices cite existing tests; no DB touched |
| D-07 no browser E2E framework | No new dependency; `pom.xml` unchanged |
| D-08 demo flags only under the demo profile | 06-04, DemoProfilePropertiesTest |
| D-09 property naming | 06-MATRIX-DEMO.md naming map; `uteexpress.*` kept |
| D-10 demo scenarios | 06-DEMO-SCRIPT.md sections 5–6 |
| D-11 fix HIGH/CRITICAL, record the rest | Section 4 (none HIGH), section 5 |
| D-12 TV3 findings are inputs only | Section 8 |

## 7. Property naming (D-09)

The working `uteexpress.*` binding is kept; the Workflow's `app.demo.*` / `app.logistics.*` names are documented as a mapping in `06-MATRIX-DEMO.md` and `06-DEMO-SCRIPT.md` section 3. No second binding was added.

## 8. Inputs from TV3 (D-12)

Phase 05.1 results were not awaited and none were supplied to this phase.

## 9. Branch hand-off

`git log --oneline main..feature/phase-06-hardening`:

```
5496064 test(security): add operations service permission matrix cells
390bc32 test(realtime): add multi-session STOMP delivery and interceptor matrices
ae2e948 test(security): add ownership and finance-scope service matrix cells
e6cddd1 test(security): add shipment ownership slice through the real service
c230e98 test(security): add route and API permission matrices with completeness guard
86ea369 test(demo): prove demo flags bind only from the demo profile
174ba65 feat(demo): simulate linehaul arrival on trip start behind the demo profile
f59bfec feat(demo): add fast-track route bypass behind the demo profile
```

Suggested pull request: **Phase 06: permission matrices, STOMP multi-session tests and demo-mode flags**
- Two demo flags (`uteexpress.demo.*`) behind the demo profile, with tests proving they are inert elsewhere.
- Route, API, service and STOMP permission matrices (about 1160 new test cells) with a route completeness guard.
- No security finding above MEDIUM; recommendations listed in the Phase 06 report.

Planning documents stay on `project-plans`; no merge of `project-plans` into `main` is requested. The branch was not pushed or merged by this phase.

Open follow-ups: SQL-enabled regression by the owner; manual browser and multi-session checks (06-UAT-RESULTS.md section 4); 05-UAT item 3 stays pending; TV3 review; owner decisions in section 5 (F-06-U-01 first, before the live demo).
