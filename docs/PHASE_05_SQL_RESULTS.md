# Phase 05 SQL verification results

Completed: 2026-10-25T21:38:37+07:00. Dedicated database: `uteexpress_phase05_test_20261025_codex_01a17279`.

Full fresh `rtk mvn clean test`: **84 suites, 504 passed, 0 failures, 0 errors, 0 skipped**.
The 15 SQL Server suites executed **50 cases**, including 27 Phase05 cases.
The earlier 45 disabled SQL cases expand to 50 when parameterized invocations execute.

| SQL suite | Executed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| Phase03CustodySqlServerIntegrationTest | 1 | 0 | 0 | 0 |
| Phase03ManagerScopeSqlServerIntegrationTest | 1 | 0 | 0 | 0 |
| Phase03ReceiptSqlServerIntegrationTest | 1 | 0 | 0 | 0 |
| Phase03RoutingSqlServerIntegrationTest | 2 | 0 | 0 | 0 |
| Phase03TripSqlServerIntegrationTest | 1 | 0 | 0 | 0 |
| Phase04FinanceSqlServerIntegrationTest | 1 | 0 | 0 | 0 |
| Phase04WalletSqlServerIntegrationTest | 4 | 0 | 0 | 0 |
| Phase05CustodySqlServerIntegrationTest | 2 | 0 | 0 | 0 |
| Phase05DeliveryReturnSqlServerIntegrationTest | 4 | 0 | 0 | 0 |
| Phase05FinanceSqlServerIntegrationTest | 9 | 0 | 0 | 0 |
| Phase05ManagerScopeSqlServerIntegrationTest | 2 | 0 | 0 | 0 |
| Phase05NormalJourneySqlServerIntegrationTest | 1 | 0 | 0 | 0 |
| Phase05SchemaConvergenceSqlServerIntegrationTest | 9 | 0 | 0 | 0 |
| RegistrationSqlServerIntegrationTest | 6 | 0 | 0 | 0 |
| ShipmentSqlServerIntegrationTest | 6 | 0 | 0 | 0 |

These counts were independently parsed from the fresh Surefire XML after the complete clean run.
Test fixtures use a dedicated database and bounded cleanup. Existing local datasource/JWT properties
were loaded privately into the Maven child process; credentials and the original local configuration
were not changed or printed. No verification migration or fixture was applied to `ute_express`.

Real execution found and corrected three test data/oracle issues: lowercase generated emails,
a separate hub code within NVARCHAR(20), and a direction-aware `:FORWARD` duplicate-key assertion.
No production schema bound or migration guard was weakened. The two affected suites passed 8/8
before the final clean regression. `git diff --check` also passed.

Live browser and private multi-session STOMP acceptance remains pending. Phase05 is open for that
acceptance; Phase06 has not started. See [the execution guide](PHASE_05_VERIFICATION.md).
