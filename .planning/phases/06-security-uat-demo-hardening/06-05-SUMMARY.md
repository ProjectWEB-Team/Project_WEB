# 06-05 Summary — Service matrix: operations (HARD-01)

Branch `feature/phase-06-hardening`, commit `5496064` (test-only).

- `PermissionMatrixHubServiceTest` (27): role × profile × position × hold/status cells for receive and sort, the admin/customer route diagnostic, custody position and hub checks (create/handover/correct), named-driver scan acceptance, the destination-receipt position check and the missing-parcel queue scope.
- `PermissionMatrixTripAndScopeServiceTest` (14): driver-only vehicle list and arrival, manager/admin dashboard scope, coordinator-only pickup queue and service failure, shipper-only pickup tasks, coordinator-only delivery-assignment screens, manager/admin disposal candidates.
- 36 public methods across nine services inventoried in `06-MATRIX-SERVICE-OPERATIONS.md`; the eleven-class regression passes (122 tests).
- Hypothesis (a) confirmed: unclaimed home pickups and their sender PII are visible to coordinators of every hub (F-06-H-01, MEDIUM, owner decision). (b) and (c) are LOW; (d) and (e) are discarded with evidence.
- No CRITICAL/HIGH finding; no production change. Services are built with a real `LogisticsProperties`, not `@Spy` (see 06-04 deviation).
