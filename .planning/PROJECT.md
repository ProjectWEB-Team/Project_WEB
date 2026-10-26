# UTEExpress — Hệ thống Quản lý Chuỗi Giao nhận Logistics

## What This Is

UTEExpress là hệ thống logistics thuần vận chuyển cho người gửi, người nhận và đội ngũ vận hành Hub/SOC. Hệ thống quản lý toàn bộ vòng đời vận đơn: tạo đơn, pickup/self-dropoff, trung chuyển, last-mile, COD, đối soát, ví điện tử, hoàn hàng, incident và tracking thời gian thực.

Canonical documentation trong `docs/` mô tả **target state**. Mã nguồn hiện tại đã hoàn thành triển khai Phase 01–05 trên `main`; việc rà soát thủ công Phase 05.1 và hardening Phase 06 còn được phân công song song. Mã nguồn không override canonical documentation. Planning được duy trì riêng trên `project-plans`.

## Core Value

Vận đơn phải đi qua đúng workflow logistics có thể theo dõi và kiểm soát end-to-end, với authorization theo ngữ cảnh và dữ liệu tài chính nhất quán.

## Business Context

## Phase 01 Outcome

The shared walking skeleton is now validated. It provides a real authenticated browser path from JSP/Bootstrap through controller, DTO, service, JPA, and SQL Server; it also establishes the six-role security foundation, server-derived actor context, safe error boundary, and a private STOMP shipment-created event. This is infrastructure for the later vertical slices, not an implementation of their pickup, hub, linehaul, delivery, return, COD, wallet, reconciliation, or withdrawal workflows.

### Validated after Phase 01

- ✓ Shared Walking Skeleton — Phase 01: authenticated minimal shipment creation/detail, canonical state/event persistence, real SQL Server proof, cross-user data scope, and authenticated private STOMP proof.

- **Customer**: Người gửi/người nhận, Shipper, nhân sự Hub/SOC, Manager và Admin của UTEExpress.
- **Revenue model**: Shipping fee, COD collection và return fee theo Workflow.
- **Success metric**: Một vận đơn demo hoàn tất từ intake đến delivery/COD payout với tracking, authorization và audit trail đúng canonical workflow.
- **Strategy notes**: Canonical target state được định nghĩa trong `docs/README.md` và các tài liệu authority liên kết.

## Requirements

### Validated

- ✓ Spring Boot WAR bootstrap, JSP landing page, dual web/API security foundation và MVC test cơ bản — existing brownfield source.

### Delivered implementation — Phase 01–05

- [x] Xây Walking Skeleton xuyên JSP/UI → Controller → DTO → Service → Repository/JPA → SQL Server, có auth/security, shipment persistence/tracking tối thiểu và tests.
- [x] Cung cấp User & Shipment Intake theo Workflow: account access, shipment creation, fee/payment initiation, pickup/self-dropoff và public/user tracking.
- [x] Cung cấp Hub & Linehaul Operations theo Workflow: inbound, event-based sorting/outbound, route planner, containers, handover, trip và incidents.
- [x] Cung cấp Last-mile & Finance theo Workflow: delivery, return, COD, reconciliation, wallets và withdrawal.
- [x] Hoàn thiện cross-slice integration — Phase 05.

### Active follow-up

- [ ] TV3 kiểm thử và tự đánh giá theo một hướng dẫn Spring Tools — Phase 05.1; tài liệu hướng dẫn đã giao.
- [ ] TV2 thực hiện security/UAT và demo hardening — Phase 06, song song TV3.
- [ ] Người dùng duyệt kết quả cuối của cả hai.

### Out of Scope

- Microservices, Redis, Kafka, Docker và Kubernetes — không được canonical docs yêu cầu.
- ShipmentStatus bổ sung cho sorting/bagging/outbound — ADR-014 quy định đây là operational events.
- Role/permission tables riêng trong MVP — ADR-002 quy định role nằm tại `users.role`.
- Bảng `shipper_debts` riêng — ADR-006 quy định discrepancy dùng reconciliation items và financial incidents.
- Ngân hàng payout thật, VNPay refund API thật, AI routing optimization, realtime GPS, fraud detection và các Phase 1 exclusions trong Workflow — chưa thuộc MVP hiện tại.

## Context

### Authority and target state

1. `docs/ARCHITECTURE_DECISIONS.md`
2. `docs/UTEExpress_Workflow.md`
3. `docs/ERD.md`
4. `docs/PROJECT_SPEC_KNOWLEDGE_BASE.md`
5. `docs/ENGINEERING_STANDARDS.md`
6. Supplementary engineering knowledge

Canonical docs are authoritative but not exhaustive. Supplementary engineering knowledge may fill implementation gaps, such as WebSocket details, but must not override higher authority or invent business requirements.

`PROJECT_SPEC_KNOWLEDGE_BASE.md` is Core Lecture Ground Truth: lecture code and names (for example `IUserService`, `ICategoryService`, and `IStorageService`) remain **SOURCE EXAMPLE**, not a project naming rule where an ADR differs. `NOT_DEFINED_IN_SOURCE` records provenance rather than prohibiting implementation, and source REST-wrapper/raw-list examples are resolved as source-example distinctions rather than a universal project response rule.

### Canonical architecture

- Six roles: `ROLE_USER`, `ROLE_SHIPPER`, `ROLE_HUB_STAFF`, `ROLE_LINEHAUL_DRIVER`, `ROLE_MANAGER`, `ROLE_ADMIN`; `GUEST` is not stored.
- Shipment lifecycle has exactly 13 ShipmentStatus values. Operational activity is recorded through `shipment_events.event_type`, not additional shipment states.
- Wallets use a separate `wallets` table with `balance` and `locked_balance`; all financial flows use it.
- Route Planner uses `hub_routing_rules` for current hub + destination region/province → next hub.
- Stack: Spring Boot, JPA, JSP/JSTL, Bootstrap, JWT, WebSocket, and SQL Server.
- Authorization requires role plus applicable hub scope, position, task assignment, shipment location, and resource ownership.
- Team ownership is vertical: A owns User & Shipment Intake, B owns Hub & Linehaul Operations, C owns Last-mile & Finance; all slices cross every relevant layer.

### Brownfield current state and implementation drift

**Historical implementation after Phase 01 (superseded by Phase01–05 completion on 2026-10-26):** the initial bootstrap has been extended with canonical user/security, shipment/event JPA, DTO/service/repository layers, JSP login/create/detail routes, SQL Server integration verification, safe exception handling, and authenticated STOMP. The initial brownfield-only description below is historical context and does not describe the current implementation.

`.planning/codebase/` documents the current source state. It has a Spring Boot application entry point, `HomeController`, JSP layout, dual security chains, Maven dependencies, and two MockMvc home-page tests. Domain entities, DTOs, services, repositories, SQL Server persistence, Workflow execution, and WebSocket implementation are not yet present.

Existing source never changes target-state decisions. Any mismatch is **IMPLEMENTATION_DRIFT**, including `hasRole("DRIVER")` in `src/main/java/vn/iotstar/config/SecurityConfig.java` versus canonical `ROLE_LINEHAUL_DRIVER`; it must be aligned by a future canonical-guided phase rather than treated as a new role requirement.

## Constraints

- **Authority**: ADR, Workflow, ERD, Knowledge Base, and Engineering Standards determine project behavior in order — prevents domain redesign.
- **Architecture**: Controller → Service Interface → ServiceImpl → Repository → Database — keeps business logic out of controllers and views.
- **Frontend**: JSP/JSTL + Bootstrap — ADR-007 accepted decision.
- **Database**: SQL Server — ADR-010 accepted decision.
- **State machine**: Exactly 13 ShipmentStatus values; events use `event_type` — ADR-003 and ADR-014.
- **Finance**: Separate wallets and reconciliation/incident model — ADR-001 and ADR-006.
- **Planning**: Phase 01 is Shared Walking Skeleton; Phases 02–04 must be independently executable after it where possible.
- **Source provenance**: ADR-008 governs project service naming; lecture patterns and source conflicts retain their example/provenance status rather than silently redefining UTEExpress.
- **Delivery**: This initialization creates planning artifacts only; it must not execute implementation phases or modify application code.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Canonical docs define target state | Prevent stale bootstrap code from redefining UTEExpress | ✓ Good |
| Keep six canonical roles in `users.role` for MVP | Workflow and ADR-002 define authorization model | ✓ Good |
| Keep 13 ShipmentStatus and event/state separation | Workflow and ADR-003/014 distinguish state from operational event | ✓ Good |
| Use separate `wallets` table | Financial integrity and ledger lifecycle | ✓ Good |
| Use static `hub_routing_rules` | Workflow-defined next-hop routing | ✓ Good |
| Use JSP/JSTL + Bootstrap and SQL Server | Accepted ADR-007 and ADR-010 decisions | ✓ Good |
| Plan by vertical slice | Three-member ownership requires end-to-end capability and parallel workstreams | ✓ Good |
| Treat source mismatch as IMPLEMENTATION_DRIFT | Canonical target state has higher authority than brownfield bootstrap | ✓ Good |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason.
2. Requirements validated? → Move to Validated with phase reference.
3. New requirements emerged? → Add to Active.
4. Decisions to log? → Add to Key Decisions.
5. What This Is still accurate? → Update if drifted.

**After each milestone** (via `/gsd-complete-milestone`):
1. Review all sections.
2. Confirm the Core Value remains the priority.
3. Audit Out of Scope boundaries.
4. Update Context with current implementation and validation state.

---
*Last updated: 2026-10-26 — Phase01–05 implementation complete; TV3 review / TV2 Phase06 parallel; planning on project-plans.*
