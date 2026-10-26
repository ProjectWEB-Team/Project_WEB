# Pre-Phase 05.1 regression — 2026-10-26

## Fresh execution

The existing ignored local helper privately loaded datasource/JWT settings and targeted the previously provisioned dedicated SQL Server verification database. Application database `ute_express` was not reset or used as the integration fixture target.

Command from the project root:

```powershell
rtk proxy powershell -NoProfile -ExecutionPolicy Bypass -File .local/phase05-sql.ps1 -Action All
```

The helper ran `rtk mvn clean test` with the SQL verification environment configured. It is a local helper, not a portable committed command; Phase 05.1 plan 01 must provide the versioned setup/runner and Spring Tools instructions.

| Check | Observed result |
|---|---|
| Maven completion | BUILD SUCCESS, 2026-10-26 10:03:37 +07, duration 2m18s |
| Fresh Surefire XML | 85 suites; 516 tests; 0 failures; 0 errors; 0 skipped |
| Real SQL integration suites (`*SqlServerIntegrationTest`) | 15 suites; 50 tests; 0 failures/errors/skips |
| SQL migration-support unit tests | 3 tests; excluded from the 50 real SQL cases |
| `rtk proxy node --check src/main/webapp/assets/js/shipment-address.js` | Exit 0 |
| `rtk proxy node --check src/main/webapp/assets/js/shipment-wizard.js` | Exit 0 |

Source under test: working tree based on `e954142` plus the reviewed registration phone-feedback, nationwide address catalog/selectors, server-derived address/pricing and shipment fee/detail UI changes. Commit publication was not completed at execution time. This remains a pre-planning regression record; the final user instruction makes Phase05.1 one Spring Tools manual test guide assessed by TV3, while TV2 performs Phase06 in parallel and the user reviews both outputs last.

## Evidence limits

- The user separately confirmed successful manual shipment creation/payment on 2026-10-26.
- No fresh multi-role live browser or multi-session STOMP acceptance was executed in this pre-planning run.
- Existing Phase 05 acceptance stays pending until the remaining observations are recorded.
- Phase 05.1 is a manual guide, not marked executed or assessed by this regression. TV3 owns its evaluation; no implementation/closeout plan is authorized in that phase.
- Raw Surefire XML may contain environment properties. Keep raw reports and credentials local; publish sanitized counts and observations only.
