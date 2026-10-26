# Phase 05 integration verification

Date: 2026-10-25. Checkout baseline: `ca2bbf8262980ee346e286e9a8c6ef1c7c3e0e8c` (`main`), with the authorized repairs in the working tree. No commits, branches, worktrees, packages or infrastructure were created by this verification task.

## Evidence boundary

Source repair, mock-service regression, real Jasper HTTP rendering, SQL Server execution and a live browser/STOMP session are separate evidence layers. A skipped SQL test is **BLOCKED_BY_ENVIRONMENT**, not a passing integration gate.

Existing datasource/JWT values were found in the Git-ignored `src/main/resources/application-local.properties`, as documented in PHASE_02_ACCOUNT_SETUP section 4. The prior SQL skips were caused by environment-only test conditions; the local file had not been loaded into Maven's environment. A private child-process loader now imports those values and substitutes only the dedicated database name. Credentials and the original local file were not changed or printed.

The user provisioned `uteexpress_phase05_test_20261025_codex_01a17279` and its application-user permissions. Schema and service journeys were executed there; no verification migration or fixture was applied to `ute_express`. The earlier Windows administration approval-service failure no longer blocks these runs.

| Layer | Result |
|---|---|
| Populated embedded Tomcat/Jasper, real login/session/CSRF and production controllers | 12 tests passed in `Phase05OperationsJspRenderingTest` |
| Canonical extraction, actual database identity rejection, manifest resource/order checks | 3 tests passed in `SqlServerMigrationSupportTest` |
| Full fresh regression | 84 suites; 504 executed/passed, 0 skipped, 0 failures/errors |
| Real SQL schema/journeys/transactions/concurrency | 50 SQL cases across 15 suites passed; includes 27 Phase05 cases across six families |
| Live browser layout and multi-user STOMP delivery | NOT_RUN; SQL is ready, live multi-session acceptance remains pending |
| Real payment-provider verification | Outside this implementation; VNPay/TRANSFER settlement is explicit manager-recorded manual/demo evidence |

Runtime: Maven 3.9.11, Oracle JDK 22, Windows 11 amd64. The POM compiles with **Java release 21**; runtime 22 is not a change to the target. Jasper/Tomcat dependency is 10.1.30 and JSTL uses `jakarta.tags.*`.

## Reproducible SQL order and safe database boundary

Provision an empty, disposable SQL Server database named `uteexpress_phase05_test_<suffix>` (letters/digits/underscores in the suffix). Configure these environment variables in the process launching Maven:

- `UTEEXPRESS_SQLSERVER_URL`: JDBC SQL Server URL whose `databaseName` is that dedicated database.
- `UTEEXPRESS_SQLSERVER_USERNAME` and `UTEEXPRESS_SQLSERVER_PASSWORD`: credentials for that database.
- `UTEEXPRESS_JWT_SECRET`: application test JWT key, supplied through the environment.

The agent imports configured User/Machine values or the existing local properties into a child process when necessary, without logging them. The local helper `.local/phase05-sql.ps1` changes only the child process target database and leaves the original file intact. Provisioning has completed. Run its Schema, Journeys and All actions in that order; `.local/PHASE05_SQL_README.md` describes the commands. Do not put passwords in command arguments or committed files.

**On an empty database, always run the Phase05 schema suite FIRST.** Older Phase03 SQL metadata tests assume the base tables already exist. A full suite first on an empty database is not an initialization mechanism.

```powershell
rtk mvn "-Dspring.profiles.active=sqlserver-verification" "-Dtest=Phase05SchemaConvergenceSqlServerIntegrationTest" test
rtk mvn "-Dspring.profiles.active=sqlserver-verification" "-Dtest=Phase05NormalJourneySqlServerIntegrationTest,Phase05CustodySqlServerIntegrationTest,Phase05DeliveryReturnSqlServerIntegrationTest,Phase05FinanceSqlServerIntegrationTest,Phase05ManagerScopeSqlServerIntegrationTest" test
rtk mvn clean test
```

`SqlServerMigrationSupport` checks `SELECT DB_NAME()` against the exact dedicated naming rule before bootstrap. It extracts `docs/schema.sql` beginning with `CREATE TABLE dbo.users`, preserving every table definition, foreign key, unique constraint and pricing trigger. It never executes that document's `CREATE DATABASE ute_express` or `USE ute_express`. Bootstrap runs transactionally only if the database has **zero user tables**. Existing/partially initialized databases are not dropped or silently rebuilt; missing base tables cause migration/test failure.

Forward scripts are ordered by [phase-05-migration-manifest.txt](../src/main/resources/db/phase-05-migration-manifest.txt). Demo pricing seed is deliberately excluded. Phase05 fixtures own their price rows. The schema suite applies the manifest twice and uses real service inserts, including a selected persisted vehicle and an initially null handover scan timestamp.

The manifest now contains 16 scripts. `V15__phase_05_route_key_alignment.sql` runs after the routing/return schemas and before custody alignment/application startup. In one transaction it locks shipment and route tables, validates all route identities, then appends `:FORWARD` only to exact legacy keys for still-FORWARD shipments in PENDING/LOADED route status. Valid suffixed FORWARD/RETURN keys, route IDs, timestamps, statuses and next hops are retained. Malformed identities, legacy keys on RETURN shipments or unknown statuses, and coexisting legacy/canonical decisions fail before any route update. Those cases require evidence-based reconciliation; the script never deletes or guesses historical decisions. Schema tests cover legacy PENDING loading, LOADED re-sort/no second membership, twice-applied normalization, conflict atomicity and ambiguous RETURN history. All nine schema-convergence cases now execute and pass on the dedicated SQL database.

Loading also checks for another IN_CONTAINER membership in OPEN/SEALED/LOADED containers while holding the existing shipment write lock. This prevents duplicate active custody even if a stale pending decision exists; completed prior-hop receipts do not block onward loading. The unit regression verifies rejection before route lookup or any mutation; existing accepted driver-scan replay remains covered.

DB-04's guarded legacy incident copy now compiles dynamically only after confirming all legacy columns exist; canonical columns are added in a preceding batch. This is a preventive source compatibility repair. **The suspected old SQL compilation failure was not reproduced against a live SQL Server here.** The canonical manifest/repeat test is its runtime gate.

Legacy custody convergence tests contain service-created trips/manifests followed by explicit historical-snapshot transforms, solely in the schema suite. They cover STARTED, partially RECEIVED ARRIVED, MISSING discrepancy/hold and conflicting vehicle mappings. The migration normalizes only custody supported by scans, matching routes and incident evidence, preserves received/found facts, restores missing hold without fabricating receipt and fails closed on ambiguity. Conflict repair in the test restores the original known service-selected vehicle; it never invents historical IDs.

### Generated fixture ownership and reset

The new SQL families also run the identity check and bootstrap/manifest in an application-context initializer **before context refresh**, so production scheduled beans cannot execute against an unguarded database. The per-test setup repeats migration and then seeds only its own namespace. Error messages identify the generated namespace for bounded recovery.

[phase-05-integration-fixtures.sql](../src/main/resources/db/phase-05-integration-fixtures.sql) and [phase-05-integration-reset.sql](../src/main/resources/db/phase-05-integration-reset.sql) require a connection-local `phase05_fixture` session context value of `p05` plus 12 generated hexadecimal characters, and a dedicated database name. The shared Java builder generates that namespace, binds it as a JDBC parameter and executes the scripts on the same connection.

The idempotent seed creates nine actors, five hubs (including an unrelated scope), four routing rules, one pricing policy and a vehicle. Actor password fields contain randomized disabled hashes; they are not browser credentials. A second vehicle is created only by the allocation-conflict test. Shipment state, assignments, payments, outboxes, custody and reconciliation are produced by real services. No journey is advanced through fixture setters or SQL updates. Only the explicitly named legacy-schema tests synthesize historical input states.

Reset selects exact namespace-owned users, their shipments/containers/trips and dependent records, then deletes in foreign-key order. It does not use `TRUNCATE`, global business deletes, disabled constraints or fabricated credentials. Teardown runs after failures as well. If a failed connection prevents teardown, recover using the recorded namespace on the same dedicated database; never run a broad cleanup.

## Executable SQL coverage

| Family | Behavior executed on the dedicated SQL database |
|---|---|
| [Normal journey](../src/test/java/vn/iotstar/integration/Phase05NormalJourneySqlServerIntegrationTest.java) | Create → physical self-dropoff → origin sorting/custody → two vehicle hops with item receipt → coordinator assignment → real outbound scan → receiver/COD collection → delivered → reconciliation → separate once-only payout. Persisted status/location/custody/route/money/event assertions. |
| [Custody](../src/test/java/vn/iotstar/integration/Phase05CustodySqlServerIntegrationTest.java) | Empty receipt means all missing; hold and no physical hub; wrong reference rejected; exact FOUND releases hold while retaining MISSING evidence and discrepancy history; next hop remains possible. Exact confirmed-loss investigation gates compensation and replay. |
| [Delivery/return](../src/test/java/vn/iotstar/integration/Phase05DeliveryReturnSqlServerIntegrationTest.java) | Failed delivery retains shipper custody until physical inbound; same-day retry denied, Clock permits next eligible day; real reverse two-hop route; immutable origin arrival; unpaid forward + 25,000 return settlement; configured 32,000 override; staff self-pickup and scanned home return; scoped overdue disposal by Clock and manager action. |
| [Finance](../src/test/java/vn/iotstar/integration/Phase05FinanceSqlServerIntegrationTest.java) | Real pickup/dropoff timeout produces cancellation/outbox; prepaid shipping plus assessed-and-paid surcharge both refund once; concurrent refunds, duplicate reconciliation sources and concurrent payout use independent service transactions; wrong source rolls back; shortage/overage hold COD; VNPAY/TRANSFER need method-specific manager evidence before separate payout. |
| [Manager scope](../src/test/java/vn/iotstar/integration/Phase05ManagerScopeSqlServerIntegrationTest.java) | Real incident and finance facts are visible in the manager subtree, excluded from unrelated work lists, and denied to outsiders/senders on detail/payout. |
| [Schema convergence](../src/test/java/vn/iotstar/integration/Phase05SchemaConvergenceSqlServerIntegrationTest.java) | Canonical bootstrap plus repeated manifest/fixture; mapped vehicle/pending scan inserts; STARTED and partial ARRIVED legacy facts; missing incident evidence/hold; conflicting vehicle fail-closed and legitimate remediation. |

The retained Phase04 finance SQL test now uses the same real service fixture and verifies a combined COD/receiver-fee shift. Its old fixture had no manager/shipper profile or actual delivered collection facts and could not satisfy the repaired finance contract. Old Phase03 tests remain **metadata tests**, not full journeys.

## Jasper and HTTP verification

```powershell
rtk mvn "-Dtest=Phase05OperationsJspRenderingTest,SqlServerMigrationSupportTest" test
```

The Jasper suite starts real embedded Tomcat, authenticates through `/login`, stores session cookies, extracts actual CSRF tokens and renders populated production DTOs through production controllers. It covers manager dashboard/work item/missing investigation, driver vehicle selection/trip actions, sorter flash result, nested audit entries, assignment, sender fees, hub self-pickup, shipper collection/return, overdue inventory, wallet ledger/withdrawal history, administrator reviews and all three finance methods/detail. Form round trips verify typed production requests, independent collection methods and authoritative source references; missing CSRF returns 403 before service invocation.

Initial execution reproduced an audit HTTP 500: `fmt:formatDate` cannot coerce `LocalDateTime` to `java.util.Date`. Audit, wallet and withdrawal pages now escape/display their java.time values directly. Operational record DTOs expose JavaBean getters for the installed EL resolver. Nonempty rows verify these paths; an empty-page/view-name assertion would not.

Services are mocked in this rendering suite. It proves JSP compilation, EL binding, form encoding and session/CSRF routing, **not persistence, real role-profile authorization, visual layout or browser JavaScript execution**.

## Bounded live browser and STOMP acceptance — NOT_RUN

After SQL suites pass, start the application against the dedicated migrated database and use separately provisioned test accounts for sender, origin/destination hub staff, driver, delivery shipper, subtree manager and unrelated manager. Execute these steps through the browser; the agent handles setup/commands and records observations. Do not reuse disabled fixture accounts as interactive logins.

| Step | Actor/action | Expected observation to record |
|---|---|---|
| 1 | Sender logs in, creates receiver-paid parcel; origin staff measures/receives | Session/CSRF works; destination derived from geography; receipt alone establishes origin inventory. |
| 2 | Sorter sorts, outbound creates container/handover; assigned driver scans and chooses persisted available vehicle | Container scan and vehicle selection are required; departure clears physical hub; no fabricated vehicle. |
| 3 | Destination staff receipts items; coordinator assigns; shipper scans and records actual COD/receiver amounts | Delivery requires actual custody; independent methods persist; delivered cannot precede obligations. |
| 4 | Manager loads CASH then VNPAY/TRANSFER source lists, records evidence and reconciles, then pays COD separately | Each method has separate totals/header; electronic evidence is explicitly manual/demo, no provider claim; wallet changes only on payout. |
| 5 | Destination reconciles all missing; origin and destination managers (including sibling branches), assigned driver and unrelated sessions are connected; scoped manager investigates FOUND or CONFIRMED_LOST | Public/recipient timeline shows “Đơn hàng đang được kiểm tra hành trình” without incident details. Assigned driver and authorized supervisors at both ends receive one private minimal NEW_INCIDENT alert after commit; inactive users, unassigned drivers and unrelated managers receive none. Detail access still enforces its original scope. Original missing evidence remains; recovery is physical; compensation requires exact confirmed incident. |
| 6 | Failed delivery returned physically, reverse route reaches origin; sender pays fees; staff self-pickup or shipper return scan | No retry before date/custody; grace starts at real origin arrival; default return fee 25,000 unless configured; no auto-disposal. |
| 7 | Owner and unrelated signed-in browser sessions subscribe to their private notification queues; manager performs one successful financial action | Intended owner gets only safe event type/reference/message after commit. Other browser receives nothing. Fetching detail still checks scope. |
| 8 | Trigger a rejected action/rolled-back service mutation; inspect private queues and persisted state | No financial event on rollback; no partial source/ledger change; unrelated manager detail remains 403/denied. |

Record actor IDs/roles, timestamp, URL/action, server-derived resource reference, observed state and intended/non-intended recipient result. Screenshots are supplementary. Existing `OperationalRealtimePublisherTest`, `StompNotificationEventPublisherTest` and `WebSocketEndpointSecurityTest` provide automated recipient/after-commit/rollback/payload/handshake evidence, but do not replace this two-session acceptance run.

## Findings and evidence links

Production repairs have source/unit/MVC regression evidence and all six Phase05 SQL families have executed successfully. The persisted SQL assertions below passed in the final full run; live browser/STOMP proof remains pending. See [SQL results](PHASE_05_SQL_RESULTS.md) for exact suite counts.

| Finding | Repair | Regression / integration boundary |
|---|---|---|
| G-01 | Unique active terminal destination from receiver geography; retained service code | `ShipmentServiceImplTest`, Normal SQL family |
| G-02 | Measured self-dropoff receipt; explicit overweight assessment/settlement | `SelfDropoffReceiptTest`, `ShipmentFeeServiceTest`, Finance SQL paid-surcharge cancellation |
| G-03 | Selected persisted vehicle; immutable manifest; departure status/location | `LinehaulTripServiceImplTest`, Schema + Normal SQL families |
| G-04 | Item receipt closes container/handover/trip and releases vehicle | `LinehaulCustodyCompletionTest`, Normal/Custody SQL families |
| G-05 | Exact missing evidence, found recovery or confirmed-loss investigation | `DestinationReceiptServiceImplTest`, Custody + Schema SQL families, missing-case Jasper form |
| G-06 | Coordinator-created delivery/return assignment with physical inventory checks | `LastMileCollectionValidationTest`, Delivery/return SQL, assignment Jasper |
| G-07 | Failed-return receipt, reverse routing and immutable origin grace | `LastMileReturnServiceImplTest`, `ShipmentReturnServiceImplTest`, Delivery/return SQL |
| G-08 | Paid liabilities and real custody before pickup/home return/disposal | `ShipmentFeeServiceTest`, Delivery/return SQL, return/overdue Jasper |
| G-09 | Source-bound refund producer/consumer and once-only ledger/outbox | `RefundProducerConsumerTest`, Finance SQL timeout/cancellation/concurrency |
| G-10 | Collector/date/method/source/shipment-bound reconciliation and separate payout | `FinanceReconciliationServiceImplTest`, Finance SQL + populated finance Jasper |
| G-11 | Exact collected amounts, compatible independent methods, valid source replay | `LastMileCollectionValidationTest`, Normal/Finance SQL, collection Jasper POST |
| G-12 | Confirmed incident-bound declared-value compensation plus actually paid fees | `ShipmentFinanceServiceImplTest`, Custody SQL; zero-payable closure remains focused service evidence |
| G-13 | User-authorized default 25,000 configurable; separate forward/return/surcharge obligations | `ShipmentFeeServiceTest`, Delivery/return SQL default + override + release |
| G-14 | Timestamp/id deterministic tracking, null-actor SYSTEM history and exact safe missing-parcel investigation label for public/recipient timelines | `TrackingServiceImplTest` requires the label and suppresses raw/unknown events in masked and unlocked views; Custody SQL adds persisted missing-label assertion (passed); `OperationalAuditServiceImplTest`, populated audit Jasper |
| G-15 | Private owner/ancestor-manager recipients; persisted missing incident resolves its trip for assigned driver and authorized origin/destination supervisors; deduplicated minimal after-commit payload | `MissingParcelRealtimePublisherTest` covers sibling branches, inactive/wrong-role/unrelated exclusions, invalid incident/trip binding, unchanged other-incident scope, two-field JSON, actual Spring transaction listener commit/rollback/no-transaction dispatch; existing realtime regressions pass; live multi-session UAT pending |
| G-16 | Current-hub/backlog and scoped work/detail/finance projections | `ManagerOperationsServiceImplTest`, manager/finance controller security tests, ManagerScope SQL, populated management Jasper |
| DB-01 | Required legitimate trip vehicle and allocation guard | Custody migration; Schema SQL canonical/legacy/conflict cases passed |
| DB-02 | Pending membership has null scan; actual driver scan stamps it | Custody migration; shared SQL `depart()` asserts null before actual scan |
| DB-03 | Ordered canonical event-type migration before operational scripts | Manifest safety test; Schema SQL metadata passed |
| DB-04 | Dynamically compiled guarded legacy incident copy | Original suspected old failure was not reproduced; repeated canonical manifest now passed on SQL Server |
| DB-05 | Unique source pair/reference retained; real competing transactions authored | Finance service regression; Finance SQL concurrency passed |
| CR-01 | Transactional legacy FORWARD key normalization and shipment-locked guard against another active container | Focused unit RED/GREEN; Schema SQL legacy PENDING/LOADED/repeat/conflict/ambiguity cases passed |
| CR-02 | Persist the full validated disposal reason directly; RETURN_DISPOSED supplies the action type | Focused 255-character unit RED/GREEN; SQL exact description and LEN=255 boundary passed |

## Final fresh regression evidence

`rtk proxy powershell -NoProfile -ExecutionPolicy Bypass -File .local/phase05-sql.ps1 -Action All` privately loaded the existing local properties, selected the dedicated test database, and invoked `rtk mvn clean test`. The fresh complete run finished at **2026-10-25 21:38:37 +07:00**, taking **2 minutes 12 seconds**: **84 suites / 504 passed / 0 skipped / 0 failures / 0 errors**. The parent independently parsed all fresh Surefire XML. A sanitized suite snapshot is saved in [SQL results](PHASE_05_SQL_RESULTS.md). The 15 SQL suites account for **50 cases**; six Phase05 families account for **27** (schema 9; normal 1; custody 2; delivery-return 4; finance 9; manager scope 2). Parameterized tests expand only when enabled, explaining why the earlier 45 skipped cases become 50 executed SQL cases.

Execution order and observed results:

1. Initial schema run: 9 executed, 1 error. Fixture emails used uppercase actor suffixes, correctly rejected by the canonical identity-normalization migration. Fixture email generation now lowercases the whole address.
2. Repeated schema run: **9 passed / 0 skipped**, finished 21:26:44 +07.
3. Five service-journey families: **18 passed / 0 skipped**, finished 21:28:15 +07.
4. Initial all-suite SQL run: no skipped tests, but two older fixtures were stale. Shipment test used a 40-character identity as a hub code despite the canonical NVARCHAR(20) bound (six setup errors); the Phase03 duplicate-route assertion used an unsuffixed historical key rather than the current :FORWARD identity (one failure).
5. Corrected only test data/oracles: a separate bounded hub code preserves the unique district and exact cleanup scope; the duplicate-key assertion uses the production direction-aware identity. The two affected suites then passed **8/8**, finished 21:34:37 +07.
6. Final clean all-suite run: **504/504 passed**, no skips. `rtk git diff --check` passed.

No production rule, schema width or migration guard was weakened to obtain this result. The previous 21:02:07 run (454 passed / 45 skipped) remains historical evidence of the environment-gated run and is superseded by the complete SQL-enabled result. Earlier verifier spot checks had overwritten partial XML; the final clean run above recreated all reports.

**Remaining acceptance:** live browser layout/session workflows and private multi-user STOMP delivery have not been observed. Embedded Jasper and transaction-listener tests passed but have narrower boundaries. Phase05 remains open for that acceptance; Phase06 has not started.
