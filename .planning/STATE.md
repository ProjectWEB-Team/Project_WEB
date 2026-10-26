---
gsd_state_version: "1.0"
current_phase: "05.1"
current_phase_name: manual-functional-test-guide
status: Guide ready for TV3 manual testing and independent assessment; TV2 Phase06 parallel; user final review pending.
last_updated: "2026-10-26T03:52:50.913Z"
last_activity: 2026-10-26
last_activity_desc: replaced seven implementation plans with one Spring Tools test guide per final user instructions. Earlier preplan regression516/50SQL/0skip is separate. Git publication blocked403; no commit/push performed.
progress:
  total_phases: 7
  completed_phases: 1
  total_plans: 33
  completed_plans: 13
  percent: 14
  summary_files: 15
needs_replan: false
stopped_at: SQL acceptance passed; live browser and multi-session STOMP acceptance pending
---

# Project State

## Project Reference

See `.planning/PROJECT.md` and the canonical documents under `docs/`.

**Core value:** Vận đơn đi đúng workflow logistics có thể theo dõi/kiểm soát end-to-end với authorization theo ngữ cảnh và dữ liệu tài chính nhất quán.
**Current focus:** One Spring Tools manual testing guide for TV3. TV3 judges results; no Phase05.1 source changes. TV2 performs Phase06 in parallel; user final review.

## Current Position

Phase: 05.1 (manual-functional-test-guide)
Plan: 1 manual testing guide for TV3; no execution or assessment performed by Codex. TV2 Phase06 may proceed in parallel.
Status: Guide ready for TV3 manual testing and independent assessment; TV2 Phase06 parallel; user final review pending.
Last activity: 2026-10-26 — replaced seven implementation plans with one Spring Tools test guide per final user instructions. Earlier preplan regression516/50SQL/0skip is separate. Git publication blocked403; no commit/push performed.

Counters distinguish formal acceptance from inventory: 33 plan files and 15 summary files exist; only Phase 01 and its three plans are formally verified. Phase 02/04 source is merged but formal completion artifacts remain incomplete; Phase 03 has summaries without formal phase verification. A summary or merged source is not proof of end-to-end completion.

## Accumulated Context

### Decisions

- Authority: ADR > Workflow > ERD > Knowledge Base > Engineering Standards; brownfield maps describe current evidence only.
- Preserve Spring Boot/JPA/SQL Server/JSP, six roles, exactly 13 ShipmentStatus values, operational events, separate wallet balance/locked_balance, static routing and service-level contextual authorization.
- Return fee defaults to 25,000 VND and is configurable, as explicitly selected by the user. Any unpaid forward fee remains a separate liability.
- Electronic COD reconciliation uses manager-recorded manual/demo evidence; no real bank/VNPay-provider verification is claimed.
- The user has SQL Server and approved using the existing Git-ignored application-local.properties. All four datasource/JWT values are present there and can be privately imported into a child process. Never print credentials.
- Execute repairs in the shared checkout; no branch/worktree/commit/push was created. Phase 06 is not started.

### Implemented Repairs

- Intake destination/routing, physical self-dropoff measurement/payment, safe tracking and scoped work lists.
- Vehicle and linehaul custody transitions, terminal receipt, missing/found/confirmed-loss handling.
- Initial delivery/return assignment, reverse custody, immutable origin grace, controlled disposal and fee/collection settlement.
- Atomic refund outbox settlement, authoritative reconciliation, separate payout, loss compensation and private notifications.
- Ordered migrations, canonical/legacy fixtures and SQL journey/concurrency suites, populated Jasper form/session/CSRF checks.
- Code-review fixes: legacy route-key convergence with conflicting active-container denial; full 255-character disposal reason persistence.
- Goal-verification fixes: canonical safe missing-investigation tracking label; private after-commit alerts for the assigned driver and authorized origin/destination supervisors.

### Pending Acceptance

1. Complete only the live browser/private multi-session STOMP group in `05-UAT.md` and `docs/PHASE_05_VERIFICATION.md`.

### Current Environment and Evidence

- Existing Git-ignored local datasource/JWT properties were located and privately loaded into each Maven child process. Original credentials and the local configuration file were unchanged.
- The user provisioned `uteexpress_phase05_test_20261025_codex_01a17279` and application-user permissions. SQL migrations and fixtures ran only on that dedicated target.
- Schema suite9/9, five service-journey families18/18, and the final full clean regression all passed.
- Three execution-discovered fixture/oracle corrections: lowercase generated emails; separate bounded hub code; current :FORWARD route identity for the older uniqueness assertion. No production schema bound or guard was weakened.
- Real SQL Server/JPA/migration/transaction/concurrency gates are PASS. Previous missing-environment and provisioning blockers are resolved.
- Live browser layout and multi-session STOMP remain NOT_RUN. Embedded Jasper and transaction-listener tests cover narrower boundaries.
- Earlier phase formal completion remains separate; do not mark earlier phases complete from merged-source inventory.

### Roadmap Evolution

- Phase 05.1 inserted after Phase 05: User-requested full functional acceptance before Phase 06 (URGENT)
- Phase 05.1 revised: Final user scope supersedes earlier acceptance plan: one Spring Tools guide for TV3 self-assessment, no source repairs; TV2 Phase06 parallel; user final review

## Deferred Items

Real bank/VNPay integration, AI routing, realtime GPS and fraud detection remain outside this task. Phase 06 security/demo hardening has not started.

## Latest Verification (2026-10-25)

- `.local/phase05-sql.ps1 -Action All` privately imported local config and invoked `rtk mvn clean test`: BUILD SUCCESS at **21:38:37 +07**, duration2m12s; **84 suites / 504 passed / 0 skipped / 0 failures / 0 errors**. Parent independently parsed all fresh Surefire XML.
- **50 real SQL cases across15 suites**, including27 cases in the six Phase05 SQL families. The prior45 disabled cases expand to50 when parameterized SQL invocations run.
- Includes12 embedded Jasper rendering/session/form/CSRF tests and7 missing-event transaction-listener tests.
- Exact sanitized counts: `.planning/phases/05-cross-slice-integration/05-SQL-RESULTS.json`.
- `rtk git diff --check` passed. Maven3.9.11/JDK runtime22/compiler target21.
- The prior454-pass/45-skip run and mixed spot-check XML are historical; the current clean run regenerated every report with SQL enabled.
- Source review and goal re-verification resolved all confirmed source findings. Independent SQL-evidence refresh verified18/19 truths; live browser/multi-session STOMP is the one remaining gate. No tests were rerun by that verifier, so the current XML remains the complete clean-run evidence.

## Session Continuity

Last session: 2026-10-26
Resume report: `.planning/phases/05-cross-slice-integration/05-VERIFICATION.md`
Execution guide: `docs/PHASE_05_VERIFICATION.md`
Acceptance checklist: `.planning/phases/05-cross-slice-integration/05-UAT.md`
Next: TV3 follows the single Phase05.1 Spring Tools guide and assesses findings; TV2 runs Phase06 in parallel. Submit both outputs to user for final review; do not auto-execute repair plans.
