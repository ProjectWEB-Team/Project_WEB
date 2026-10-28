# 06-04 Summary — Demo flags (HARD-03)

Branch `feature/phase-06-hardening`, commits `f59bfec`, `174ba65`, `86ea369`.

- `LogisticsProperties.Demo` with `fastTrackRouteBypass` and `autoLinehaulTransitSimulation` (default false); both set true only in `application-demo.properties`.
- `HubOperationsServiceImpl`: side-effect-free `singleStaticRule` lookup; when it finds no single rule and the bypass flag is on, `fastTrackTarget` uses the persisted destination (origin for RETURN) if it is an active, different hub; otherwise the unchanged `blocked(...)` path.
- `LinehaulTripServiceImpl.startTrip`: with the simulation flag on, ends with `return arriveTrip(tripId)` in the same transaction.
- Tests: 7 new in `HubRoutingServiceImplTest`, 5 new in `LinehaulTripServiceImplTest`, new `DemoProfilePropertiesTest` (5); constructor call sites updated in `ShipmentServiceImplTest` and `LinehaulCustodyCompletionTest`. The 11-class regression set passed: 95 tests, 0 failures.
- Deviation: the plan asked for `@Spy LogisticsProperties`. Mockito cannot instrument concrete classes on the local default JDK 25, so tests construct the services with a real `LogisticsProperties`. All Maven runs used JDK 22, which matches the documented baseline.
- Findings F-06-D-01..03 (all LOW) in `06-MATRIX-DEMO.md`.
