---
phase: 05-cross-slice-integration
plan: "05"
subsystem: integration-verification-migrations-jsp
status: implemented_pending_browser_acceptance
tags: [sqlserver, custody-migration, jasper, csrf, service-journeys, concurrency]
requires: [05-01, 05-02, 05-03, 05-04]
provides: [safe-migration-manifest, shared-sql-fixtures, six-service-journey-families, populated-jsp-proof, fresh-regression-evidence]
affects: [phase05-final-verification, phase06-gate]
tech-stack:
  added: []
  patterns: [guard-before-context-refresh, exact-fixture-ownership, production-service-journeys, real-jasper-http-tests]
key-files:
  created:
    - src/test/java/vn/iotstar/realtime/MissingParcelRealtimePublisherTest.java
    - src/main/resources/db/phase-05-route-key-alignment.sql
    - src/main/resources/db/phase-05-migration-manifest.txt
    - src/main/resources/db/phase-05-integration-fixtures.sql
    - src/main/resources/db/phase-05-integration-reset.sql
    - src/test/java/vn/iotstar/integration/Phase05SqlServerFixtureSupport.java
    - src/test/java/vn/iotstar/integration/Phase05SchemaConvergenceSqlServerIntegrationTest.java
    - src/test/java/vn/iotstar/integration/Phase05NormalJourneySqlServerIntegrationTest.java
    - src/test/java/vn/iotstar/integration/Phase05CustodySqlServerIntegrationTest.java
    - src/test/java/vn/iotstar/integration/Phase05DeliveryReturnSqlServerIntegrationTest.java
    - src/test/java/vn/iotstar/integration/Phase05FinanceSqlServerIntegrationTest.java
    - src/test/java/vn/iotstar/integration/Phase05ManagerScopeSqlServerIntegrationTest.java
    - src/test/java/vn/iotstar/integration/SqlServerMigrationSupportTest.java
    - src/test/java/vn/iotstar/controller/Phase05OperationsJspRenderingTest.java
    - docs/PHASE_05_VERIFICATION.md
  modified:
    - src/main/java/vn/iotstar/realtime/OperationalRealtimePublisher.java
    - src/main/java/vn/iotstar/service/impl/TrackingServiceImpl.java
    - src/test/java/vn/iotstar/service/impl/TrackingServiceImplTest.java
    - src/main/java/vn/iotstar/repository/ContainerHandoverRepository.java
    - src/main/java/vn/iotstar/service/impl/ContainerHandoverServiceImpl.java
    - src/main/java/vn/iotstar/service/impl/ShipmentReturnServiceImpl.java
    - src/test/java/vn/iotstar/service/ContainerHandoverServiceImplTest.java
    - src/test/java/vn/iotstar/service/ShipmentReturnServiceImplTest.java
    - src/test/java/vn/iotstar/integration/SqlServerMigrationSupport.java
    - src/test/java/vn/iotstar/integration/Phase01WalkingSkeletonTest.java
    - src/test/java/vn/iotstar/integration/Phase04FinanceSqlServerIntegrationTest.java
    - src/main/resources/db/phase-03-linehaul-trip-schema.sql
    - src/main/resources/db/phase-05-custody-alignment.sql
    - src/main/java/vn/iotstar/dto/operations
    - src/main/java/vn/iotstar/dto/driver
    - src/main/java/vn/iotstar/dto/hub
    - src/main/webapp/WEB-INF/views/hub/audit-timeline.jsp
    - src/main/webapp/WEB-INF/views/user/wallet.jsp
    - src/main/webapp/WEB-INF/views/admin/withdrawals.jsp
key-decisions:
  - "Guard actual disposable database identity and initialize schema before Spring context refresh, so schedulers never see an unguarded target."
  - "Keep canonical constraints; never execute schema.sql CREATE DATABASE/USE and never rebuild a nonempty database silently."
  - "Generate actor/topology/policy/fleet setup only; production services create all normal financial and custody transitions. Historical transforms are restricted to named migration tests."
  - "Restore legacy departed custody only with bounded matching evidence; conflicting vehicle allocations and ambiguous parcel custody fail closed."
  - "Treat disabled SQL tests and unrun live browser/STOMP acceptance as open mandatory gates."
metrics:
  completed_source: "2026-10-25"
  regression_finished: "2026-10-25T21:38:37+07:00"
  regression_duration: "2m12s"
actuals:
  tokens: null
  tasks: 3
  commits: 0
plan_head_before: ca2bbf8262980ee346e286e9a8c6ef1c7c3e0e8c
---

# Phase 05 Plan 05: Safe SQL journeys and real JSP verification Summary

Added a guarded canonical migration/fixture boundary, six real SQL service-journey families and populated Jasper form proof; repaired legacy custody convergence and reachable JSP runtime failures. Source work and available regression are complete. Mandatory live SQL and browser/STOMP verification remain blocked, so this is not phase completion.

## Task outcomes

| Task | Implemented result | Verification status |
|---|---|---|
| 1 — Manifest, migration and fixture contract | Ordered 16-script manifest; original canonical table definitions retained; exact DB_NAME guard before context refresh; generated idempotent seed and FK-safe reset; metadata plus actual mapped custody inserts; legacy STARTED, partial RECEIVED/ARRIVED, MISSING incident/hold, conflicting vehicle and recovery cases; legacy PENDING/LOADED route keys, conflict atomicity and ambiguous history | 3 safety/unit tests PASS; 9 real SQL methods BLOCKED_BY_ENVIRONMENT |
| 2 — Normal/custody/return/finance/scope journeys | Shared actors/geography/static routes/fleet; two-hop normal delivery/payout; missing-found/confirmed-loss; retry/physical return/reverse route; both return releases/default and configured fees/overdue disposal; real timeout producers; paid-surcharge plus fee refunds; electronic evidence settlement; independent concurrent source/refund/payout transactions; scope denials | Compiles; 15 SQL methods BLOCKED_BY_ENVIRONMENT across other five families (parameterized cases expand only when enabled) |
| 3 — Regression, Jasper and acceptance record | Real session/CSRF/production-controller forms with nonempty DTOs; complete fresh regression; findings G-01..16 and DB-01..05 linked; runnable SQL order and bounded live UAT documented | 12 Jasper + full regression PASS; live browser/STOMP NOT_RUN |

See [PHASE_05_VERIFICATION.md](../../../docs/PHASE_05_VERIFICATION.md) for finding-by-finding production and test evidence, exact SQL setup order, fixture ownership contract and remaining browser observations. Paths in that document are repository-relative.

## Fresh verification evidence

- Final command: `rtk mvn clean test`.
- Finished: **2026-10-25 21:02:07 +07:00**; elapsed **1m45s**, after CR-01/CR-02 and G-14/G-15 corrections.
- Fresh Surefire XML: **84 suites / 499 reported / 454 executed and passed / 45 skipped / 0 failures / 0 errors**.
- All 45 skips are real SQL environment gates; none count as integration passes. Phase05's new six families report 24 disabled methods; class-level gating does not expand parameterized invocations.
- `Phase05OperationsJspRenderingTest`: **12 passed**, real embedded Tomcat/Jasper with actual HTTP login/session/CSRF and mock services.
- `SqlServerMigrationSupportTest`: **3 passed**, canonical extraction and target-safety/resource-order checks.
- `MissingParcelRealtimePublisherTest`: **7 passed**, sibling-hub recipient resolution, private two-field payload, binding/user exclusions and actual Spring transaction-event commit/rollback dispatch with mock persistence/messaging boundaries.
- `Phase01WalkingSkeletonTest` compatibility correction passed its focused run and final clean run.
- `git diff --check`: PASS.
- Runtime: Maven **3.9.11**, Oracle **JDK 22**, Windows 11 amd64; compiler target **release 21**.
- SQL environment check: URL, username, password and JWT secret all absent in Process/User/Machine scopes; no secret values printed.

The final normalized-phone fixture correction was source-reviewed only; disabled SQL suites did not execute it. The final Java regression is not evidence of SQL statement validity or live transaction outcomes.

## Runtime corrections discovered

1. **[Rule 1 — JSP runtime]** Operational record DTO properties were incompatible with the installed JavaBean EL resolver. Added getters consistently for operations, driver and hub response records; populated manager, driver, sorter and audit pages now render through Jasper.
2. **[Rule 1 — JSP runtime]** Retained wallet/withdrawal views used unmapped JSTL URLs. Updated reachable old URLs to `jakarta.tags.core`, `jakarta.tags.fmt` and `jakarta.tags.functions`.
3. **[Rule 1 — JSP runtime]** Actual audit rendering reproduced HTTP 500 because `fmt:formatDate` cannot coerce `LocalDateTime` into `java.util.Date`. Audit/wallet/withdrawal date cells now escape/display java.time values; populated pages passed.
4. **[Rule 3 — Test compatibility]** Full regression exposed the Phase01 walking skeleton's missing new `HubRepository` dependency. Added the legitimate active terminal destination fixture and asserted its resolved ID. No production behavior was weakened.
5. **[Rule 3 — Test compatibility]** Replaced Phase04 finance's obsolete unprofiled/non-delivered fixture with the shared production-service journey, retaining its combined COD/receiver-fee and separate-payout assertions. Runtime remains SQL-blocked.
6. **[Rule 2 — Migration correctness]** Extended custody migration beyond alias renames to evidence-bounded physical state, missing holds, terminal container evidence and mapped vehicle reservation. Conflict/ambiguity has explicit error messages and no invented historical mapping.
7. **[Rule 2 — Preventive migration compatibility]** DB-04 legacy incident copy is dynamically compiled after its guarded shape is known and canonical columns are in a prior batch. The old SQL compilation hypothesis was **not live-reproduced**; repeated canonical/legacy SQL execution remains the verification gate.

## Scope and invariants

Goal-verification corrections G-14/G-15 are implemented against Workflow section 38. Tracking now allowlists MISSING_PARCEL with the exact safe label “Đơn hàng đang được kiểm tra hành trình”; public, masked recipient and unlocked recipient timelines all retain it while excluding raw descriptions and unknown internal events. NEW_INCIDENT resolves the persisted incident and, only for MISSING_PARCEL, its bound trip: consistent destination is required before adding authorized origin/destination supervisors and the active assigned driver with the linehaul-driver role. Other incident types retain their prior destination scope. Username deduplication prevents repeated sends across shared ancestors/admin instances. The payload remains type/reference only on the private operations queue; detail authorization is unchanged. Seven new realtime tests cover sibling scope, invalid bindings, inactive/wrong-role driver exclusion, other incident scope and actual Spring listener dispatch after commit with rollback/no-transaction suppression. The corrected RED run had 10 tests/5 intentional failures/0 errors; focused GREEN completed at 20:59:33 +07 in 34.289s. Custody SQL and live browser acceptance now include the safe tracking label and notification expectations but remain unrun.

Final review repairs CR-01/CR-02 are implemented in the same working tree. CR-01 adds an ordered transactional legacy route-key migration: exact still-FORWARD PENDING/LOADED keys gain `:FORWARD`; valid suffixed history is untouched; malformed identity, ambiguous legacy RETURN/unknown status and conflicting old/new decisions fail before any mutation. The shipment-locked loading boundary also rejects another active container. Four SQL methods cover legacy PENDING/LOADED, repeated migration, atomic conflict rejection and ambiguous direction. CR-02 persists the complete validated disposal reason directly, avoiding a prefix overflow beyond NVARCHAR(255), with exact 255-character unit and SQL assertions. Both new unit regressions failed intentionally before repair (20 tests, 2 failures, no errors; 20:27:18 +07) and the focused run passed after repair (20:29:32 +07). SQL cases remain blocked; neither review closure nor runtime migration success is inferred from this source repair.

- Six roles and 13 ShipmentStatus values remain unchanged. Disposal is `CLOSED_EXCEPTION` with `closure_reason=DISPOSED`; missing/found and other operational detail remain event/evidence facts.
- User's chosen fixed return fee remains **25,000 VND default, configurable**; the SQL return suite includes a 32,000 override without changing production configuration.
- Electronic settlement is manager-recorded manual/demo evidence, not provider verification.
- Normal/exception SQL journeys do not manufacture intermediate state through direct entity setters/SQL. Only schema tests construct explicitly labeled historical input snapshots after real service creation.
- The SQL source/reset scripts neither contain usable fixed passwords nor remove non-owned business data. Environment guard runs before application startup; invalid configured connection/target fails.
- No new endpoints or dependencies were added by this plan. Response JavaBean accessors supplement existing record accessors; existing call signatures remain.
- No Git mutation/commit was performed, as instructed for this shared checkout. All source changes remain reviewable in place. STATE.md, ROADMAP.md and 05-VALIDATION.md remain owned by the parent executor.

## Known stubs and deferred gates

No placeholder implementation, disabled test annotation, TODO/FIXME or fake service result was introduced into production. Test service mocks are isolated to the explicitly bounded Jasper suite.

| Open gate | Reason | Next concrete action |
|---|---|---|
| Real SQL convergence + six journey families | Four required environment variables absent | Run schema suite first against dedicated `uteexpress_phase05_test_<suffix>`, then families, then full regression as documented |
| Persisted concurrent money/source guarantees | Authored separate service transactions have not run on SQL Server | Execute Finance SQL family unskipped and inspect unique source/ledger/balance assertions |
| Live browser and private STOMP multi-session evidence | No running configured application/database | Execute documented actor/action/observation matrix; record intended and unrelated recipients and rollback |
| Legacy DB-04 runtime behavior | Preventive source change has no live reproduction | Run canonical manifest twice and legacy convergence cases without skips |

No phase advancement is warranted while these gates remain open. The same checkout must be used for final review; committed HEAD alone does not include the authorized repairs.

## Threat flags

| Flag | File | Description |
|---|---|---|
| threat_flag: test-database-mutation | `Phase05SqlServerFixtureSupport`, seed/reset SQL | Guarded identity before context refresh; exact generated ownership; canonical constraints preserved; credentials stay external |
| threat_flag: legacy-custody-reconciliation | `phase-05-custody-alignment.sql` | Changes persisted historical custody/vehicle facts only with bounded scan/route/incident evidence; ambiguity fails closed; actual SQL execution pending |
| threat_flag: legacy-route-identity | `phase-05-route-key-alignment.sql` | Transactionally normalizes only provable legacy FORWARD identities, preserves decision facts and rejects ambiguous/conflicting history before update; actual SQL execution pending |
| threat_flag: incident-recipient-scope | `OperationalRealtimePublisher` | Missing-parcel recipients expand only from persisted consistent incident/trip bindings to assigned active driver and scoped supervisors at both ends; no detail authorization expansion or raw evidence payload; live multi-session verification pending |

## Self-Check: PASSED

All listed test/support/manifest/seed/reset/document artifacts exist. Fresh Surefire XML and successful Maven exit substantiate the recorded non-SQL counts. No commits were expected or claimed. Parent-owned state files were not edited. Mandatory SQL/browser gates are explicitly open.

## SQL-enabled follow-up (current evidence)

The user provisioned the dedicated Phase05 test database. Existing local datasource/JWT properties were imported privately into each Maven child process; original local configuration and credentials were unchanged.

- Schema convergence: **9 passed**, no skips (21:26:44 +07).
- Five service-journey families: **18 passed**, no skips (21:28:15 +07).
- Final fresh `rtk mvn clean test`: **84 suites / 504 passed / 0 skipped / 0 failures / 0 errors**, 21:38:37 +07, duration 2m12s. **50 real SQL cases across 15 suites**, including 27 Phase05 cases.
- Source corrections discovered by execution: lowercase generated fixture emails to satisfy the canonical migration; give the old shipment fixture a separate hub code within NVARCHAR(20), preserving its unique district and bounded cleanup; use the current :FORWARD identity for the old Phase03 duplicate-route assertion. No production schema/guard was weakened. Focused affected suites passed 8/8 before the final clean regression.
- Sanitized exact results: `05-SQL-RESULTS.json`. Prior sections describing SQL as unexecuted are historical pre-configuration evidence, superseded by this follow-up.
- `git diff --check` passed. Changes remain uncommitted.
- Live browser/multi-session STOMP acceptance remains NOT_RUN, so Phase05 stays open and Phase06 has not started.
