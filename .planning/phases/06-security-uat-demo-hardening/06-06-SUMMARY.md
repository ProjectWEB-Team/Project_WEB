# 06-06 Summary — Demo script (HARD-03)

Planning-only plan; no source change.

- `06-DEMO-SCRIPT.md` (Vietnamese): evidence boundary, preparation (profiles `demo,local[,otp-local]`, demo database rules, opt-in seeds), the real demo-mode keys and waits, the D-09 `app.*` → `uteexpress.*` naming map, role prerequisites, the primary scenario (Workflow §106, 22 steps) and Cases 1–8 (Workflow §107), known limits and a findings template.
- Every URL used exists in `06-MATRIX-ROUTES.md`; every documented key exists in `application-demo.properties` on `feature/phase-06-hardening`; every Result cell is `NOT_RUN`.
- **GAP found while writing step 5.9:** `/driver` has no form for `POST /driver/handovers/scan`, so a driver cannot bring a handover to `ACCEPTED` from the browser and the primary demo stops at Tab 4 unless the POST is sent another way. The driver page also lists no assigned handovers or trips (ids are typed manually). Recorded in Section 7 and in `06-REPORT.md` as F-06-U-01.
- Deviation: 54 `NOT_RUN` cells instead of the planned ≥60; no padding rows were added.
