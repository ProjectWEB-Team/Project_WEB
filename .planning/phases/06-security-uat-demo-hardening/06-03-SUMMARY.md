# 06-03 Summary — STOMP matrix (HARD-01, HARD-02)

Branch `feature/phase-06-hardening`, commit `390bc32` (test-only).

- New `StompMultiSessionDeliveryTest` (12): owner vs stranger shipment delivery after commit only, rollback silence, minimal payloads for all three publisher kinds, two notification recipients, HANDOVER_ACCEPTED / DESTINATION_ARRIVAL / INBOUND_READY_CONTAINER recipient exclusivity by hub and position, the real Spring transactional listener (no send before commit or on rollback), the publisher-destination ↔ interceptor invariant and principal-name addressing.
- `WebSocketConfigTest` extended from 3 to 22 tests: full command × destination × principal matrix and wiring checks (single interceptor, `/ws` only with no origin override, `/queue`/`/app`/`/user` prefixes). The original three tests are unchanged.
- Closes the technical part of 05-UAT item 3; browser-observed parts stay NOT_RUN (D-05).
- Findings F-06-S-01..02, both LOW, are recorded in `06-MATRIX-STOMP.md`. No production change.
