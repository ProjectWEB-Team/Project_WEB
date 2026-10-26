# Requirements: UTEExpress

**Defined:** 2026-10-01
**Core Value:** Vận đơn phải đi qua đúng workflow logistics có thể theo dõi và kiểm soát end-to-end, với authorization theo ngữ cảnh và dữ liệu tài chính nhất quán.

## Authority and planning rule

The canonical target state is governed by, in descending authority:

1. `docs/ARCHITECTURE_DECISIONS.md`
2. `docs/UTEExpress_Workflow.md`
3. `docs/ERD.md`
4. `docs/PROJECT_SPEC_KNOWLEDGE_BASE.md`
5. `docs/ENGINEERING_STANDARDS.md`
6. Supplementary engineering knowledge

Requirements below operationalize that target; they do not replace it. Supplementary knowledge may close an implementation gap but may not override a higher-authority decision or create a new business requirement. Brownfield code and `.planning/codebase/` describe current implementation only; a mismatch is **IMPLEMENTATION_DRIFT**.

## v1 Requirements

### Shared Walking Skeleton

- [x] **SKEL-01**: A browser user can complete one minimal, persisted shipment flow through JSP/UI, controller, DTO, service, repository/JPA, SQL Server, and tests.
- [x] **SKEL-02**: An authenticated actor can enter the application through the canonical six-role model (`ROLE_USER`, `ROLE_SHIPPER`, `ROLE_HUB_STAFF`, `ROLE_LINEHAUL_DRIVER`, `ROLE_MANAGER`, `ROLE_ADMIN`) without inventing stored roles.
- [x] **SKEL-03**: A shipment stores one of exactly 13 canonical `ShipmentStatus` values while operational activity is appended as `shipment_events.event_type`, not as extra statuses.
- [x] **SKEL-04**: The shared foundation exposes consistent validation/error handling, auditable timestamps/actors where the canonical schema requires them, and a secured WebSocket capability for workflow updates that need realtime delivery.
- [x] **SKEL-05**: The foundation enforces the canonical layered pattern and establishes authorization extension points for role, hub scope, position, assignment, location, and resource ownership checks.

### User and Shipment Intake — Person A

- [ ] **INTAKE-01**: A sender or recipient can register, authenticate, and access only the account functions permitted by the canonical role and ownership rules.
- [ ] **INTAKE-02**: A sender can create a shipment with sender/receiver details, parcel data, addresses, declared value, COD, service choice, and canonical initial state.
- [ ] **INTAKE-03**: A sender can obtain the Workflow-defined shipping fee and select the permitted fee payer/payment path for shipment creation.
- [ ] **INTAKE-04**: A sender and authorized shipper can complete HOME_PICKUP or SELF_DROPOFF intake, including assignment, timeout, cancellation, and race-condition handling defined by the Workflow.
- [ ] **INTAKE-05**: A sender can track own shipments, and an eligible recipient can be auto-linked and view masked shipment information according to canonical privacy rules.

### Hub and Linehaul Operations — Person B

- [ ] **OPS-01**: Authorized operations staff can work within the canonical hub hierarchy and route a shipment using `hub_routing_rules` from current hub plus destination region/province to next hub.
- [ ] **OPS-02**: Authorized hub staff can receive inbound shipments, record event-based sorting/outbound operations, and preserve the 13-status state machine.
- [ ] **OPS-03**: Authorized hub staff and linehaul drivers can create and operate parcel containers, container items, handovers, and unbagging with accountable custody records.
- [ ] **OPS-04**: Authorized linehaul drivers can accept assigned transport work, operate trips between allowed hubs/SOCs, and record arrival or discrepancy outcomes.
- [ ] **OPS-05**: Authorized operational roles can record missing-parcel and related incidents without corrupting shipment state, custody, or event history.

### Last-mile and Finance — Person C

- [ ] **LAST-01**: An authorized last-mile shipper can receive only assigned delivery work and record delivery attempts, success, refusal, or the Workflow-defined failure reason.
- [ ] **LAST-02**: The system applies the canonical retry and return workflow, including return routing and ownership-scoped handling of returned shipments.
- [ ] **FIN-01**: Permitted shipping-fee, COD, refund, return-fee, and compensation flows create traceable financial records that match the canonical payment and COD states.
- [ ] **FIN-02**: Each user has a separate `wallets` record with `balance` and `locked_balance`; wallet transactions preserve the canonical ledger and anti-double-spending rules.
- [ ] **FIN-03**: Authorized staff can complete shipper reconciliation, surface discrepancies through reconciliation items/financial incidents rather than a `shipper_debts` table, and resolve the defined financial outcome.
- [ ] **FIN-04**: An eligible wallet owner can request withdrawal and an authorized administrator can approve or reject it with correct locked-balance transitions.

### Cross-slice Integration

- [ ] **INTEG-01**: A canonical normal shipment journey works end-to-end from intake through origin hub, linehaul, destination hub, last-mile delivery, COD/payment outcome, tracking, and realtime updates where required.
- [ ] **INTEG-02**: Canonical exception journeys—pickup/dropoff expiry, delivery failure/return, missing parcel, COD discrepancy, and compensation—preserve workflow state, events, custody, and financial consistency across slices.
- [ ] **INTEG-03**: Authorized managers and administrators can access the Workflow-defined operational/reporting views across slices without bypassing contextual data scope.

### Manual Functional Test Guide (Phase 05.1, updated 2026-10-26)

- [ ] **GUIDE-01**: Member 3 has one practical manual guide to start the application with Spring Tools and test all currently implemented functions, with prerequisites, actions, expected results and a blank findings template. Member 3 independently evaluates results and lists inconsistencies; this phase changes no source/test/script/schema. Member 2 performs Phase06 in parallel; the user reviews both outputs last.

### Security, UAT, and Demo Hardening

- [ ] **HARD-01**: Every protected UI/API/workflow action enforces contextual authorization using the applicable combination of role, hub scope, position, assignment, shipment location, and resource ownership.
- [ ] **HARD-02**: The completed system has automated and manual verification across the vertical path, state transitions, financial invariants, authorization/data scope, error paths, and WebSocket behavior where used.
- [ ] **HARD-03**: Demo mode supports the canonical primary and exception scenarios with safe shortened timers/fast-track behavior only where the Workflow permits it.

## v2 Requirements

These items are intentionally deferred; they are not inputs to the initial roadmap.

- **V2-01**: Integrate a real banking payout provider or live VNPay refund API.
- **V2-02**: Add AI route optimization, realtime GPS tracking, or fraud detection.
- **V2-03**: Add microservices, Redis, Kafka, Docker, Kubernetes, or other infrastructure not required by canonical docs.
- **V2-04**: Introduce role/permission tables or additional ShipmentStatus values only through a future approved Project Decision.

## Out of Scope

| Feature | Reason |
|---------|--------|
| A seventh stored role or a stored `GUEST` role | ADR-002 fixes the six-role model; guest is not stored. |
| Additional status values for sorting, bagging, packing, or outbound | ADR-014 requires these to be operational events, not `ShipmentStatus`. |
| `users.e_wallet`, `users.locked_balance`, or a separate `shipper_debts` table | ADR-001 and ADR-006 define the wallet and discrepancy model. |
| Thymeleaf, a SPA, or a replacement frontend stack | ADR-007 fixes JSP/JSTL + Bootstrap as the primary frontend. |
| A non-SQL-Server project database | ADR-010 fixes SQL Server. |
| Rewriting lecture source examples to project naming | Source examples retain provenance; ADR-008 controls project service naming. |
| Application-code implementation during initialization | This milestone step creates planning artifacts only. |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| SKEL-01 | Phase 01 | Complete |
| SKEL-02 | Phase 01 | Complete |
| SKEL-03 | Phase 01 | Complete |
| SKEL-04 | Phase 01 | Complete |
| SKEL-05 | Phase 01 | Complete |
| INTAKE-01 | Phase 02 | Pending |
| INTAKE-02 | Phase 02 | Pending |
| INTAKE-03 | Phase 02 | Pending |
| INTAKE-04 | Phase 02 | Pending |
| INTAKE-05 | Phase 02 | Pending |
| OPS-01 | Phase 03 | Pending |
| OPS-02 | Phase 03 | Pending |
| OPS-03 | Phase 03 | Pending |
| OPS-04 | Phase 03 | Pending |
| OPS-05 | Phase 03 | Pending |
| LAST-01 | Phase 04 | Pending |
| LAST-02 | Phase 04 | Pending |
| FIN-01 | Phase 04 | Pending |
| FIN-02 | Phase 04 | Pending |
| FIN-03 | Phase 04 | Pending |
| FIN-04 | Phase 04 | Pending |
| INTEG-01 | Phase 05 | Pending |
| INTEG-02 | Phase 05 | Pending |
| INTEG-03 | Phase 05 | Pending |
| GUIDE-01 | Phase 05.1 | Guide prepared; TV3 assessment pending |
| HARD-01 | Phase 06 | Pending |
| HARD-02 | Phase 06 | Pending |
| HARD-03 | Phase 06 | Pending |

**Coverage:**

- v1 requirements: 27 original + 1 manual-guide requirement = 28 total
- Mapped to phases: 28
- Unmapped: 0

---
*Requirements defined: 2026-10-01*
*Last updated: 2026-10-26 — final user scope: one STS test guide for TV3, no source changes; TV2 runs Phase06 in parallel; user final review.*
