---
phase: 05
status: browser_acceptance_pending
nyquist_compliant: false
wave_0_complete: false
updated: 2026-10-25
---

# Phase 05 gap-repair validation

This document tracks implementation evidence separately from the research snapshot. No new phase may be inferred from a green unit suite. SQL Server skips and unperformed browser/STOMP checks are not passes.

## Baseline

Before repair, `rtk mvn test` reported 337 tests: 317 passed, 20 SQL Server tests skipped, no failures/errors. The current process has no configured SQL Server/JWT verification environment. JDK runtime 22, compiler target 21.

## Finding-to-verification map

| Plan | Findings | Required evidence | Current result |
|---|---|---|---|
| 05-01 | G-01, G-02, G-14, operational G-16 | Authoritative destination, fresh self-dropoff with measurement/payment, invalid/replayed intake, safe history, second-hub backlog and denied out-of-scope detail | 66 focused tests passed; SQL/browser pending; see 05-01-SUMMARY.md |
| 05-02 | G-03, G-04, G-05, DB-01, DB-02 | Manifest departure transitions, terminal receipt/container/trip, incident-bound missing/found/lost outcomes, vehicle binding, pending scan semantics | 41 focused tests passed; 4 SQL skips; see 05-02-SUMMARY.md |
| 05-03 | G-06, G-07, G-08, G-11, G-13; G-02 review correction | Initial delivery/return assignment, return routing/custody, immutable origin grace, fee settlement/release, collection amount/method validation, negative and replay checks; prepaid surcharge consumption | 107 latest focused report cases passed (last run 31/31); integration pending; see 05-03-SUMMARY.md |
| 05-04 | G-09, G-10, G-12, G-15, financial G-16 | Settled refund outbox, authoritative reconciliation sources/scope, legitimate-loss compensation, correct private recipients, negative and replay checks | 116 focused cases passed; SQL/JSP pending; see 05-04-SUMMARY.md |
| 05-05 | DB-03, DB-04, DB-05 and INTEG-01..03 | Ordered repeatable canonical/legacy migration, shared isolated fixtures, normal/custody/return/finance service journeys, concurrent financial-source uniqueness, full regression | Implemented; full suite 454 passed / 45 SQL-skipped; real SQL and live browser/STOMP pending |

## Gates

1. Run focused regression for each changed business boundary, including denial and repeated commands.
2. Compile the existing tests as well as new tests after signature changes.
3. Run the full Maven suite after all source repairs; record actual Surefire totals and skip counts.
4. Run real SQL Server migration and service journey suites only against a dedicated configured test database. Never apply `docs/schema.sql` with its database switching statements to an arbitrary database.
5. Verify browser forms/session/CSRF and private after-commit STOMP delivery; retain `NOT_RUN` until observed.
6. Review final finding resolutions against actual source and tests. Remaining code gaps prevent a source-complete verdict; environment blockers prevent Phase 05 completion.

## Current environment gate

- SQL Server migration/JPA/concurrency: `BLOCKED_BY_ENVIRONMENT` until an explicitly configured test boundary is available and tests actually execute.
- Browser/STOMP UAT: `NOT_RUN`.
- Phase 06: not started; no automatic advancement.

## Final evidence

- Fresh `rtk mvn clean test`, 2026-10-25 21:02:07 +07: **84 suites / 499 reported / 454 passed / 45 SQL-skipped / 0 failures / 0 errors**. Parent independently parsed Surefire XML.
- Twelve real embedded Jasper rendering/form/session/CSRF tests, three migration-support contract checks, and seven missing-parcel realtime recipient/transaction-listener tests passed.
- `rtk git diff --check` passed. The canonical 16-script manifest and six Phase05 SQL families are authored; the 24 disabled methods in those families did not execute. The full suite contains 45 SQL skips overall.
- Code review CR-01/CR-02 repaired and re-reviewed. Goal findings G-14/G-15 repaired with safe investigation-label and private origin/destination/driver audience regressions; independent targeted re-verification confirms both resolved. Verdict: `human_needed`, 12/19 truths verified, seven runtime truths unverified.
- The verifier's later two named spot checks passed but overwrote two default XML files. The 499/454/45 totals refer specifically to the 21:02:07 clean run; current mixed XML must not be presented as a fresh complete regression.
- No SQL Server migration/JPA/concurrency pass or live browser/STOMP pass is claimed. `05-UAT.md` persists the three acceptance groups; `05-VERIFICATION.md` is the goal-level verdict, and `docs/PHASE_05_VERIFICATION.md` provides reproducible commands.
- Phase 05 remains pending acceptance. Phase 06 has not started. Changes are uncommitted.

## SQL-enabled acceptance (supersedes earlier SQL-pending entries)

User-provisioned dedicated database: `uteexpress_phase05_test_20261025_codex_01a17279`. Existing Git-ignored local properties were loaded privately into the Maven child process; original configuration and credentials were unchanged.

- Schema 9/9 and five journey families 18/18 passed without skips.
- Final clean regression at 2026-10-25 21:38:37 +07: **84 suites / 504 passed / 0 skipped / 0 failures / 0 errors**. This includes **50 SQL cases across 15 suites**, with 27 Phase05 cases. Fresh Surefire XML was independently parsed; exact sanitized counts are in `05-SQL-RESULTS.json`.
- Three test-fixture corrections were required by actual SQL: lowercase generated emails; a bounded 16-character hub code distinct from the unique district; direction-aware :FORWARD key in the older duplicate-route assertion. Affected suites passed 8/8, then the complete clean suite passed. Canonical rules and schema bounds were preserved.
- SQL setup, migrations, normal/exception journeys, data scope and concurrent finance acceptance groups are now PASS. The prior SQL-skipped/blocked entries are historical evidence.
- Embedded Jasper 12/12 and missing-event transaction-listener 7/7 passed in the same full regression. Live browser/multi-session STOMP remains NOT_RUN; `05-UAT.md` is 2 passed / 1 pending.
- Phase05 remains open only for the pending acceptance verdict; Phase06 has not started. No commits were created.

Independent evidence refresh: `05-VERIFICATION.md` now reports **human_needed / 18 of19 truths verified**, with no source blocker and only live browser/multi-session STOMP acceptance pending. Verifier reran no tests and left the fresh full-run XML unchanged.
