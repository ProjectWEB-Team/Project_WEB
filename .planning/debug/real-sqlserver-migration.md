---
status: verifying
trigger: Real SQL Server verification must apply Phase 01 and Phase 03 forward migrations to the local ute_express database and complete without errors.
created: 2026-10-19
updated: 2026-10-19
---

## Symptoms

- `src/main/resources/application-local.properties` contains valid local SQL Server/JWT values; JDBC connects successfully to `localhost\\SQLEXPRESS`, database `ute_express`.
- The first real test exposed a Spring Data derived-query defect; it was corrected from `findRouteByDecisionKey` to `findByRouteDecisionKey`.
- The live database is partial: `shipments` exists but `shipment_events` is absent or unavailable to the migration account.
- `phase-01-shipment-events-event-type.sql` was extended to create a missing canonical event table.
- The existing Spring `ScriptUtils` runner did not create the expected table before later batches. A test-only `SqlServerMigrationSupport` executes standalone `GO` batches over the working JDBC connection, but the later batch still observes no `shipment_events` table.
- `sqlcmd` cannot establish the local TLS connection with its installed ODBC Driver 17, while JDBC/Hikari connects successfully. Do not rely on sqlcmd.

## Current Focus

bug_class: Mandelbug / environment-dependent configuration and SQL Server permissions; deterministic once the local configuration and database state are fixed.
pattern_candidates: Environment/config permission denied; partial database provisioning.
sbfl: skipped — the failing path is environment-dependent and no per-test coverage spectrum is available.
candidate_causes:
  - code: the forward-only migration may still contain an incompatible SQL Server batch or non-idempotent DDL.
  - environment: the configured login may still lack required DDL rights despite the reported grant.
  - data: the partial local database may conflict with a canonical migration precondition.
and_gate: yes — the migration requires both a compatible idempotent script and a login/database state that permits each required DDL operation.
hypothesis: Legacy handover nullability is reconciled and pending handovers persist; the remaining real custody failure is that pre-existing handovers lacks the Phase-03 count/reconciliation check constraints because the migration adds them only during CREATE TABLE.
test: Add idempotent guarded check-constraint creation for existing handovers, then rerun the custody test and complete real SQL Server set.
expecting: SQL Server rejects an accepted handover whose accepted count differs from expected, and every real SQL Server test completes with zero errors or skips.
next_action: Add the two missing handover constraint guards and rerun the real SQL Server test set.
reasoning_checkpoint:
  hypothesis: "Existing partial handovers and incidents tables cause FK creation to reference columns that are absent; separate SQL Server integration tests also run later migration files without prerequisite schema files."
  confirming_evidence:
    - "Phase03CustodySqlServerIntegrationTest failed at phase-03-container-handover-schema.sql batch 6: FK_handovers_replaces references absent handovers.replaces_handover_id."
    - "Phase03TripSqlServerIntegrationTest failed at phase-03-linehaul-trip-schema.sql batch 4: FK_incidents_trip references absent incidents.transport_trip_id."
    - "Phase03ReceiptSqlServerIntegrationTest failed because it applies only the receipt migration, which references incidents.incident_type supplied by the prerequisite linehaul migration."
  falsification_test: "After adding only guarded missing columns and explicit predecessor scripts, the five real SQL Server tests must all execute with zero errors; a different SQL exception falsifies the exact remaining-schema hypothesis."
  fix_rationale: "Idempotent COL_LENGTH guards make the forward migration compatible with partially provisioned tables, while explicit predecessor application makes each integration test independent of Maven class order."
  blind_spots: "A live legacy incidents table containing rows that cannot be attributed to a trip/hub/actor would require explicit data remediation before non-null enforcement; the current errors occur before such enforcement."
  candidate_causes:
    - "code: IF OBJECT_ID-only table creation does not repair partially existing tables before later FK creation."
    - "test configuration: trip and receipt tests omit predecessor migration files and therefore depend on persistent suite order."
    - "environment/data: local SQL Server retains partial tables from earlier forward migration attempts."
  and_gate: "yes — the observed failures require both retained partial schema state and migrations/tests that assume a full prior schema."
fixture_reasoning_checkpoint:
  hypothesis: "The custody test uses a generated hub code longer than the local legacy hubs.code column permits."
  confirming_evidence:
    - "SQL Server reports truncation specifically for dbo.hubs.code while inserting P3-ORIGIN-4359250598."
    - "The migration FK failures are no longer present; the other four real SQL Server tests pass."
  falsification_test: "A shorter unique hub fixture code must let the custody test proceed beyond hub insertion; another truncation would identify a different fixture field."
  fix_rationale: "The test needs uniqueness, not a long hub code; shortening only the fixture preserves canonical production schema and migration behavior."
  blind_spots: "The local hub code limit may be narrower than the target migration definition, but this test does not need to assert schema width."
  candidate_causes:
    - "test fixture: generated code has an unnecessarily long fixed prefix."
    - "data: existing local hub table retains a narrower legacy code column."
  and_gate: "yes — the error occurs because the fixture length exceeds the retained local schema length."
created_by_reasoning_checkpoint:
  hypothesis: "ParcelContainer lacks the canonical created_by mapping and migration column, so inserts omit a required existing SQL Server column."
  confirming_evidence:
    - "The real custody test fails because dbo.parcel_containers.created_by is NOT NULL and the insert omits it."
    - "UTEExpress_Workflow.md and docs/schema.sql both define parcel_containers.created_by as a required user reference."
    - "ParcelContainer.java and phase-03-container-handover-schema.sql currently omit created_by."
  falsification_test: "After server-derived creator persistence is implemented, the custody test must create a container successfully; a NOT NULL created_by error would falsify the mapping/service fix."
  fix_rationale: "Adding the canonical actor relationship and deriving it from AuthenticatedActor at the service boundary restores the documented schema without trusting client input."
  blind_spots: "Legacy parcel-container rows with NULL created_by require an explicit data remediation before a nullable legacy column can be tightened; the live database already has a required created_by column."
  candidate_causes:
    - "code: entity and factory method do not represent the canonical required creator relation."
    - "migration: Phase-03 CREATE TABLE path omitted the canonical created_by column and FK."
    - "data: the live baseline table correctly retains the older canonical required column, exposing the drift."
  and_gate: "yes — the failure requires both current code omitting the field and the real SQL Server schema enforcing it."

## Constraints

- Use SQL Server only; no substitute database.
- Preserve canonical six roles, thirteen ShipmentStatus values, and shipment_events.event_type.
- Never log or expose local datasource/JWT secrets.
- Do not start Phase 03 plan 05, Phase 02, or Phase 04.
- Do not reset/revert existing uncommitted Phase 03 plan 04 work.

## Evidence

- timestamp: 2026-10-19 — The live Spring Boot SQL Server profile connected successfully through Hikari; no TLS or datasource authentication issue was observed.
- timestamp: 2026-10-19 — JDBC diagnostics confirmed dbo.shipment_events exists, event_type is NVARCHAR(50), and the column is already NOT NULL before the former unconditional ALTER COLUMN batch.
- timestamp: 2026-10-19 — Guarding that redundant ALTER lets the migration reach db/phase-03-hub-routing-schema.sql batch 7, where SQL Server returns CREATE TABLE permission denied in database ute_express while attempting to provision the missing canonical hub schema.
- timestamp: 2026-10-19 — ShipmentSqlServerIntegrationTest now invokes the same current forward migrations before persistence verification, therefore it fails truthfully at the DDL privilege gate rather than later with an unrelated missing-column error.
- timestamp: 2026-10-19 — User reports that the requested SQL Server DDL permissions were granted; this is unverified until the targeted real test is rerun.
- timestamp: 2026-10-19 — Current Codex process has none of the four UTEEXPRESS_* variables, while an ignored local application-local.properties exists and contains exactly the datasource URL/username/password and JWT secret properties needed to populate a child Maven process without revealing values.
- timestamp: 2026-10-19 — SBFL skipped: this is environment-dependent and no per-test coverage spectrum is available.
- timestamp: 2026-10-19 — Targeted ShipmentSqlServerIntegrationTest, run with variables populated only in the child process from ignored application-local.properties, completed against real SQL Server: Tests run 1, Failures 0, Errors 0, Skipped 0, BUILD SUCCESS.
- timestamp: 2026-10-19 — Full-suite report parsing found 97 tests with 3 errors and 0 skips; the passing shipment/routing real tests executed, while custody, trip, and receipt real SQL Server tests failed deterministically at migration batches.
- timestamp: 2026-10-19 — Exact failures: custody FK_handovers_replaces references missing handovers.replaces_handover_id; trip FK_incidents_trip references missing incidents.transport_trip_id; receipt references missing incidents.incident_type because it does not apply prerequisite migrations.
- timestamp: 2026-10-19 — After the migration guards and explicit predecessor chain were applied, shipment/routing/trip/receipt real SQL Server tests passed with no skip. Custody reached fixture seeding and then failed only because P3-ORIGIN-<nanoTime> exceeds the existing local dbo.hubs.code length.
- timestamp: 2026-10-19 — The custody fixture's hub code is not production data or a canonical maximum-length assertion; shortening its unique prefix is the minimal compatibility correction.
- timestamp: 2026-10-19 — The shortened fixture passed hub insertion; custody then failed at container creation because the retained local dbo.parcel_containers table lacks the entity-required version column.
- timestamp: 2026-10-19 — After version backfill, SQL Server rejected container insert because canonical dbo.parcel_containers.created_by is NOT NULL; the canonical Workflow and docs/schema.sql require that user reference, while current Phase-03 entity/migration omitted it.
- timestamp: 2026-10-19 — Focused container/receipt unit tests pass after restoring server-derived created_by. The real custody test now persists its container and next fails only because the retained dbo.handovers table lacks the entity-required version column.
- timestamp: 2026-10-19 — After the version guard, real custody persistence reaches handover creation and fails only because the legacy dbo.handovers.transport_trip_id is NOT NULL. The approved Phase-03 workflow creates a pending handover before driver acceptance creates and links a trip, so that legacy nullability must be reconciled forward.
- timestamp: 2026-10-19 — After transport_trip_id reconciliation, the custody test reaches its specified count-mismatch assertion; SQL Server accepts the invalid update because the existing handovers table lacks CK_handovers_counts and CK_handovers_accepted_reconciled.

## Resolution

- root_cause: The live database is only partially provisioned and the configured SQL Server login lacks CREATE TABLE permission for the missing Phase-03 hub schema; additionally, redundant unconditional ALTER COLUMN batches attempted an unnecessary DDL operation on an already non-null event_type column.
- fix: Made event_type nullability enforcement conditional in the Phase-01 and Phase-03 migrations, added migration batch context to the test-only runner, and made the real shipment verification apply the required forward migrations.
- verification: JDBC reached SQL Server; targeted verification currently FAILS at the explicit CREATE TABLE permission gate in Phase-03 hub migration batch 7. git diff --check passes.
- remaining_blocker: Provision the canonical schema with a DBA account or grant the configured application login DDL privileges for the forward migrations, then rerun targeted real-SQL tests. No secrets were logged.
