# Phase 03: Critical-Fix Summary

**Scope:** CR-01 through CR-04 from `03-REVIEW.md` only.

## Resolved findings

### CR-01 — Inbound destination proof

- Initial `WAITING_FOR_HANDOVER` or `PICKED_UP` intake now requires the shipment's server-side origin/intake hub to equal the authenticated inbound operator's active hub.
- The generic shipment-only inbound route now rejects every `IN_TRANSIT` shipment. Container-custody arrivals must pass through `DestinationReceiptService`, which owns handover, container, item, and discrepancy reconciliation.
- A missing item or `DISCREPANCY` handover cannot be converted to `AT_HUB` through `/hub/inbound`.
- An unrelated hub is denied before shipment location/status or event history can change.

### CR-02 — Current-hop outbound route selection

- Outbound loading selects a `PENDING` `ShipmentRoute` by both shipment and the shipment's server-derived current hub.
- A route is marked `LOADED` only after the matching shipment is persisted in the outbound container.
- Earlier-hop route decisions cannot be selected after a shipment reaches and is sorted at a later hub.

### CR-03 — Active driver-only audit access

- Driver audit authorization now requires an `ACCEPTED` active handover assigned to that driver.
- Historical drivers receive the established non-enumerating not-found outcome once later custody has replaced/completed their leg.
- Manager, Admin, and scoped hub-staff behavior remains unchanged.

### CR-04 — Durable blocked-route evidence

- Added ADR-016: static lookup failure creates or reuses a deduplicated `routing_exceptions` record, not a new shipment status and not a manual route.
- A newly opened exception writes one minimal `ROUTE_LOOKUP_BLOCKED` `shipment_events.event_type` record and emits one after-commit Manager/Admin-scoped alert.
- A retry after static-rule correction resolves the open exception; no route/container/handover is created on the failure path.
- The manager KPI now counts open durable routing exceptions, not transient errors or historical event totals.
- Added an idempotent SQL Server forward migration for the exception table, constraints, foreign keys, and filtered open-exception index.

## Added/updated automated coverage

- Negative inbound tests for unrelated `IN_TRANSIT` and initial-intake hub attempts.
- A discrepancy regression proving generic inbound cannot bypass destination item-level receipt: the shipment remains `IN_TRANSIT` and no arrival event is created.
- Two-hop outbound regression proving the route lookup uses hub B after the shipment arrives from hub A.
- Historical-driver audit denial regression, while retaining active-driver coverage.
- Blocked-route deduplication, safe MVC PRG behavior, resolution-on-retry, and durable KPI coverage.
- Real SQL Server migration/service verification for the routing exception, deduplication, event evidence, manager count, and resolution.

## Verification

| Command | Result |
| --- | --- |
| Targeted CR-01 hub/receipt/security suite | 20 tests, 0 failures, 0 errors, 0 skipped |
| `rtk proxy mvn test` | 120 tests, 0 failures, 0 errors, 7 condition-skipped SQL Server tests |
| Real SQL Server verification group | 7 tests, 0 failures, 0 errors, 0 skipped |
| `rtk git diff --check` | PASS |

`REAL_SQLSERVER_VERIFICATION = PASS` for the automated migration/persistence suite. This does not replace browser/STOMP manual UAT.

## Remaining human gates

- Multi-account manual UAT remains pending, including unrelated-hub inbound, two-hop custody, historical-driver audit denial, and Manager/Admin blocked-route notification visibility.
- Person A security/privacy review and Person C custody/finance-boundary review have not been performed or claimed by this fix.
- No Phase 02 or Phase 04 workflow was added.
