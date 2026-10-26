# Walking Skeleton — UTEExpress

**Phase:** 1  
**Generated:** 2026-10-01

## Capability Proven End-to-End

An authenticated canonical-role user creates one minimal shipment in the JSP UI, receives a persisted detail/tracking readback from SQL Server, and the creation is demonstrably published through the secured STOMP seam.

## Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Framework | Spring Boot 3.3.4 WAR with Spring MVC | Existing brownfield bootstrap; canonical monolith direction. |
| Presentation | JSP/JSTL with Bootstrap 5 | ADR-007 fixes the server-rendered primary frontend. |
| Data layer | Spring Data JPA against SQL Server 2019+/2022+ | ADR-010 and the canonical schema fix the persistence target. |
| Domain boundaries | Controller → request/response DTO → Service interface → ServiceImpl → Repository/JPA | D-06, ADR-008, and engineering standards keep HTTP, business, and persistence concerns separate. |
| Auth | Session/CSRF-protected JSP chain plus stateless JWT REST chain | D-07 preserves and completes the existing dual-chain direction. |
| Authorization | Service-boundary actor/ownership policy seam | D-08 allows own-shipment enforcement now and hub/position/assignment/location extension later. |
| Realtime | Secured Spring WebSocket/STOMP broker with a shipment-topic publication seam | D-10 supplies a reusable capability without implementing future notification workflows. |
| Deployment target | Local Spring Boot run using a provisioned SQL Server database | The skeleton proves the canonical database locally; no deployment platform is specified. |
| Directory layout | `vn.iotstar` packages for config, controller, dto, entity, repository, security, service, and service.impl | Extends existing source without creating a competing architecture. |

## Stack Touched in Phase 1

- [ ] Project scaffold: existing Maven/Spring Boot WAR, JSP runtime, and JUnit/MockMvc runner retained.
- [ ] Routing: authenticated JSP create/detail routes and REST authentication routes.
- [ ] Database: SQL Server receives one `users` read, one `shipments`/`shipment_events` write, and a detail readback.
- [ ] UI: a Bootstrap/JSP form posts a request DTO and redirects to a tracking/detail response.
- [ ] Local full-stack run: `mvn spring-boot:run` with the documented SQL Server environment values, then authenticated create/readback UAT.

## Out of Scope (Deferred to Later Slices)

- Account registration and complete intake, pricing, payment initiation, `HOME_PICKUP`, `SELF_DROPOFF`, recipient privacy, and public tracking (Phase 02).
- Hub hierarchy, route rules, inbound/sorting/outbound, containers, handovers, trips, and incidents (Phase 03).
- Delivery, returns, COD, reconciliation, wallet records/ledger behavior, and withdrawals (Phase 04).
- End-to-end normal and exception workflows across all slices (Phase 05) and hardening beyond this baseline (Phase 06).

## Subsequent Slice Plan

- Phase 02: User & Shipment Intake adds the canonical first-mile and privacy-aware tracking workflows over the shipment/security seams.
- Phase 03: Hub & Linehaul Operations adds scoped custody and route operations using the event seam without extending `ShipmentStatus`.
- Phase 04: Last-mile & Finance adds delivery and separate-wallet workflows without adding balances to `users`.
- Phase 05: Cross-slice Integration joins the three workflows into normal and exception journeys.
- Phase 06: Security + UAT + Demo Hardening proves full contextual authorization and canonical demo evidence.
