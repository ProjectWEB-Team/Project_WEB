---
status: testing
phase: 05-cross-slice-integration
source: [05-VERIFICATION.md, docs/PHASE_05_VERIFICATION.md]
started: 2026-10-25
updated: 2026-10-26
---

# Phase 05 runtime acceptance

Source implementation and passing local regression do not complete these tests. A skipped SQL test is not a pass. Keep unobserved Phase05 results pending. The user explicitly assigned TV3 manual review and TV2 Phase06 in parallel on 2026-10-26; Phase05.1 does not block Phase06. Final review belongs to the user.

## Current Test

number: 3
name: Live browser and private after-commit STOMP
expected: |
  Run the bounded actor/action browser checklist with separate authorized and
  unrelated sessions. Verify usable real forms, session/CSRF, safe tracking and
  private minimal STOMP messages only for intended recipients after commit.
  Rollback sends nothing; detail requests enforce their own resource scope.
awaiting: live browser and multi-session STOMP observations

## Tests

### 1. Real SQL Server canonical and legacy schema convergence
expected: Dedicated-database checks, repeated manifest, canonical/legacy mapped inserts, route and custody preservation, conflict rejection, and disposal text boundary all pass on real SQL Server. No arbitrary database reset or credential output.
result: [pass]
evidence: Schema suite 9/9 passed at 2026-10-25 21:26:44 +07; repeated in the complete 21:38:37 clean run, zero skips. Dedicated database uteexpress_phase05_test_20261025_codex_01a17279; see 05-SQL-RESULTS.json.

### 2. Persisted journeys, data scope and concurrent finance
expected: After schema convergence, all five service-journey SQL families execute without skips. Verify physical self-dropoff, two-hop normal delivery, failed delivery and reverse return, missing/found/loss, expiry/refund, configurable return fees, reconciliation and separate payout. Inspect actual persisted handoffs, scope denials, rollback, replay and competing transactions for once-only financial effects.
result: [pass]
evidence: Five journey families 18/18 passed at 2026-10-25 21:28:15 +07; final clean regression 504/504 passed, including all 50 SQL cases, zero skips/failures/errors. See 05-SQL-RESULTS.json and docs/PHASE_05_VERIFICATION.md for the fixture corrections discovered during real execution.

### 3. Live browser and private after-commit STOMP
expected: Run the bounded actor/action checklist in docs/PHASE_05_VERIFICATION.md. Forms work with real sessions and CSRF; populated manager/finance/return screens are usable. Public and recipient tracking show the safe missing-investigation label without private incident text. Missing alerts reach the assigned driver and authorized origin/destination managers, exclude unrelated sessions, and carry only minimal private payloads after commit; rollback emits nothing. Detail endpoints enforce their independent scope checks.
result: [pending]

Partial user-reported evidence (2026-10-26): The user confirmed that shipment creation and payment testing are complete. Record those flows as passed by the user; this confirmation does not establish results for the remaining manager/finance/return screens, unrelated-session scope checks, or private after-commit STOMP delivery and rollback observations. Keep this combined acceptance item pending until those results are recorded.

Follow-up (2026-10-26, final user scope): Phase05.1 is one Spring Tools testing guide for TV3, who independently judges results and consolidates inconsistencies without source repairs. TV2 performs Phase06 in parallel. The user reviews both outputs last; this file does not infer any new browser/STOMP PASS. Preplan regression516/50SQL remains separate evidence in docs/PHASE_05_1_PREPLAN_REGRESSION.md.

## Summary

total: 3
passed: 2
issues: 0
pending: 1
skipped: 0
blocked: 0

The SQL acceptance checks passed after the user provisioned the dedicated database and the existing local properties were privately loaded into Maven's environment. Browser/STOMP is pending, not a failed test result. Electronic COD reconciliation remains manager-recorded manual/demo evidence; real provider verification is outside this task.

## Gaps

Record observed runtime failures here with their command, expected result, actual result and affected journey. Do not treat source-review conclusions or old phase evidence as a substitute for current execution.
