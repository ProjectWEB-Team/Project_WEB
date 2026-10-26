# 06-02 Summary — Service matrix: ownership, finance, last-mile (HARD-01)

Branch `feature/phase-06-hardening`, commits `ae2e948`, `e6cddd1` (test-only).

- `PermissionMatrixShipmentOwnershipSliceTest` (6): filter chain + `ShipmentController` + real `ShipmentServiceImpl`; stranger gets 403 `error/403` on detail, cancel and confirm-payment with no write; list queries only the caller's id.
- `PermissionMatrixOwnershipServiceTest` (8) and `PermissionMatrixFinanceScopeServiceTest` (10): every NEW cell in `06-MATRIX-SERVICE-OWNERSHIP.md` (recipient inbox, withdrawal review, cash return-fee collection, reconciliation reads, compensation and candidate lists, unassigned shipper on delivery and return actions, task lists by non-shippers), each with a positive pair.
- 47 public methods inventoried; two GAP rows (wallet reads) recorded as F-06-O-02.
- Hypothesis (b) confirmed with a throwaway probe: a plain `AccessDeniedException` from a service renders HTTP 500 instead of 403 (F-06-O-01, MEDIUM; one-line fix in `WebExceptionHandler`, outside this plan's file scope).
- No CRITICAL/HIGH finding; no production file changed.
