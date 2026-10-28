# Phase 06 — Demo-flag safety matrix (HARD-03, plan 06-04)

Source branch: `feature/phase-06-hardening`. Decisions: D-08 (flags only under the demo profile), D-09 (naming).

## Flag matrix

| Flag (bound key) | Code default | `application.properties` | `application-demo.properties` | Other `application*.properties` | Hook | Off | On | Evidence |
|---|---|---|---|---|---|---|---|---|
| `uteexpress.demo.fast-track-route-bypass` | false | not set (false) | true | not set (scanned) | `HubOperationsServiceImpl.createDecision` → `fastTrackTarget` | No single static rule → `RouteConfigurationException`, durable blocked-route exception, `ROUTE_LOOKUP_BLOCKED` event | No single static rule → next hop is the persisted destination hub (origin hub for RETURN), saved as the normal `ShipmentRoute` + `SORTING_COMPLETED`. A single valid rule always wins; missing, inactive or same-hub target still blocks | `HubRoutingServiceImplTest` (7 bypass tests), `DemoProfilePropertiesTest` |
| `uteexpress.demo.auto-linehaul-transit-simulation` | false | not set (false) | true | not set (scanned) | `LinehaulTripServiceImpl.startTrip` → `arriveTrip` | Trip stays `IN_TRANSIT` until the driver presses Arrive | Same transaction calls `arriveTrip`: trip `ARRIVED`, `LINEHAUL_ARRIVED` events, `DESTINATION_ARRIVAL` + `INBOUND_READY_CONTAINER`. Handover stays `IN_TRANSIT`; destination item receipt is still required | `LinehaulTripServiceImplTest` (5 simulation tests), `DemoProfilePropertiesTest` |

Both hooks run under the already-authorized actor (sorter position/hub check, assigned active driver check run first and are tested with the flag on).

## Invariants verified unchanged

- 13 shipment statuses: `DemoProfilePropertiesTest#flagsDoNotAddShipmentStatuses`, `CanonicalDomainContractTest`.
- No new event type: bypass emits `SORTING_COMPLETED`; simulation emits `LINEHAUL_TRIP_STARTED` then `LINEHAUL_ARRIVED` (asserted in the tests above).
- No wallet movement and no authorization change: neither hook touches the ledger; unauthorized sorter/driver denied with flag on (`bypassKeepsTheSorterAuthorizationRules`, `simulationNeverLetsAnUnassignedDriverStartTheTrip`).
- Only the demo profile sets demo keys: `DemoProfilePropertiesTest#noOtherClasspathPropertiesFileSetsADemoKey`.

## Workflow → code naming map (D-09)

Spring silently ignores unknown keys, so only the `uteexpress.*` keys take effect. No second binding was added.

| Workflow (`docs/UTEExpress_Workflow.md`) | Key that the code reads |
|---|---|
| `app.demo.fast-track-route-bypass` | `uteexpress.demo.fast-track-route-bypass` |
| `app.demo.auto-linehaul-transit-simulation` | `uteexpress.demo.auto-linehaul-transit-simulation` |
| `app.demo.delivery-attempt-day-lock` | `uteexpress.delivery.attempt-day-lock` |
| `app.logistics.timeout.dropoff-expiration-minutes` | `uteexpress.timeout.dropoff-minutes` (plus `dropoff-hours`) |
| `app.logistics.timeout.pickup-expiration-minutes` | `uteexpress.timeout.pickup-minutes` (plus `pickup-hours`) |
| `app.logistics.return.grace-period-minutes` | `uteexpress.timeout.disposed-grace-minutes` (plus `disposed-grace-days`) |
| `app.logistics.delivery.max-attempts` | `uteexpress.delivery.max-attempts` |
| `app.scheduler.timeout-check-interval-ms` | `uteexpress.scheduler.timeout-check-interval-ms` |

## Findings

| ID | Severity | Surface | Description | Disposition | Evidence |
|---|---|---|---|---|---|
| F-06-D-01 | LOW | Audit trail | Bypass and simulated-arrival decisions use the normal event types, so the timeline cannot tell them apart from manual ones. ADR-014 forbids new statuses and no event type was added. | ACCEPTED (owner review) | `HubRoutingServiceImplTest`, `LinehaulTripServiceImplTest` |
| F-06-D-02 | LOW | Configuration | Any property source (for example an environment variable `UTEEXPRESS_DEMO_FAST_TRACK_ROUTE_BYPASS=true`) can enable a flag outside the demo profile. Configuration is operator-controlled. | ACCEPTED (owner review) | `DemoProfilePropertiesTest` covers files only |
| F-06-D-03 | LOW | Documentation | The Workflow documents `app.*` keys that Spring ignores; an operator copying them would believe a mode is active when it is not. | MITIGATED by the naming map above and in `06-DEMO-SCRIPT.md` | this document |
