# 06-07 Summary — Regression, UAT evidence, report

- Full `mvn -B clean test` on `feature/phase-06-hardening` at `5496064` (JDK 22): 1682 tests, 0 failures, 0 errors, 45 skipped (the 15 SQL Server suites; REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT until the owner runs the private loader against the dedicated database).
- `06-UAT-RESULTS.md`: D-06 coverage map, 05-UAT item 3 split into automated evidence vs Manual only (item 3 remains pending; `05-UAT.md` unchanged), 13-row manual checklist all NOT_RUN.
- `06-REPORT.md`: verdicts, evidence, 20 consolidated findings (5 MEDIUM incl. the new F-06-U-01 driver-scan UI gap, 15 LOW, none HIGH/CRITICAL), gate (nothing escalated, no escalated regression test), owner recommendations, D-01..D-12 traceability, hand-off.
- Deviation: the plan's secret-pattern grep (`[A-Za-z0-9+/]{32,}`) matches long test class/method names; every match was inspected and none is a secret.
