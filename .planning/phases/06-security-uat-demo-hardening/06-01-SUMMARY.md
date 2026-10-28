# 06-01 Summary — Route permission matrix (HARD-01)

Branch `feature/phase-06-hardening`, commit `c230e98`.

- `PermissionMatrixRouteTest`: 96 web rows (89 controller routes, 5 filter-handled, 2 default-deny probes) × 6 roles + anonymous, a no-CSRF cell for every web POST, and the completeness guard `everyControllerRouteHasAMatrixRow` (fails on a missing or stale row). 1002 tests.
- `PermissionMatrixApiChainTest`: 8 `/api/**` rows (4 controller routes, 4 prefix probes) × 7 principals, asserting the 401/403 JSON bodies and that POST needs no CSRF token. 56 tests.
- Both use a catch-all `NoopController` imported into a `@WebMvcTest` slice with the real `SecurityConfig`. Nested test controllers must be listed in `@Import`, because `TestTypeExcludeFilter` hides classes nested in test classes.
- No observed cell differed from policy; `SecurityConfig.java` is unchanged. Findings F-06-R-01 (MEDIUM, uploads) and F-06-R-02..04 (LOW) are recorded in `06-MATRIX-ROUTES.md`.
