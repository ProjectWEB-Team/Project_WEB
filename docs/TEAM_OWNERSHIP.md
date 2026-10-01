# UTEExpress Team Ownership

## Working model

UTEExpress has three members. Ownership is by vertical slice, never by frontend/backend/database layer alone. Every member must understand and implement through the same end-to-end path:

```text
JSP → Controller → DTO → Service → Repository → JPA → SQL Server
```

Each slice includes applicable security, RBAC/data scope, WebSocket integration, exception handling, and tests.

## Phase 01 — Shared Walking Skeleton

All three members jointly build the architecture foundation:

- JSP, Controller, DTO, Service, Repository, JPA, and SQL Server connection path.
- Spring Security/JWT, RBAC and data-scope foundation.
- WebSocket skeleton, exception handling, and basic tests.

Cross-review is mandatory:

```text
A reviews B
B reviews C
C reviews A
```

## Person A — User and Shipment Intake

Primary ownership:

- Register/Login
- Create Shipment
- Fee calculation
- Payment initiation
- `HOME_PICKUP`
- `SELF_DROPOFF`
- User/Public Tracking

Person A implements the full vertical slice: JSP, Controller, DTO, Service, Repository, Entity, Security, applicable WebSocket behavior, and tests.

## Person B — Hub and Linehaul Operations

Primary ownership:

- Inbound
- Sorting
- Route Planner
- Bag/Container
- Handover
- Transport Trip
- Destination Hub
- Operational Incident

Person B implements the full vertical slice: JSP, Controller, DTO, Service, Repository, Entity, Security, applicable WebSocket behavior, and tests.

## Person C — Last-mile and Finance

Primary ownership:

- Delivery assignment
- Delivery attempt
- Delivered/Failed
- Return
- COD
- Reconciliation
- Wallet
- Withdrawal

Person C implements the full vertical slice: JSP, Controller, DTO, Service, Repository, Entity, Security, applicable WebSocket behavior, and tests.

## Cross-slice review

```text
Person A slice → Person B architecture review
Person B slice → Person C architecture review
Person C slice → Person A architecture review
```

No member is limited to a single layer. Reviewers validate the slice against the architecture decisions, Workflow, ERD, engineering standards, data scope, and integration boundary.

## GSD Core roadmap shape

GSD planning for `open-gsd/gsd-core` prioritizes vertical MVP slices:

1. Phase 01 — Walking Skeleton
2. Phase 02 — User & Shipment Intake — Person A
3. Phase 03 — Hub & Linehaul Operations — Person B
4. Phase 04 — Last-mile & Finance — Person C
5. Phase 05 — Cross-slice Integration
6. Phase 06 — Security + UAT + Demo Hardening

Phases 02, 03, and 04 should depend only on Phase 01 where possible, allowing parallel worktree/branch execution. This document defines planning ownership only; it does not authorize application implementation.
