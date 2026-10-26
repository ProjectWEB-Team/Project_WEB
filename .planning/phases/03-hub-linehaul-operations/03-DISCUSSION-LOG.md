# Phase 03: Hub & Linehaul Operations - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-12
**Phase:** 03-hub-linehaul-operations
**Areas discussed:** Route lookup when sorting, Container and handover evidence, Linehaul trip flow, Operations UI and realtime

---

## Route lookup when sorting

Selected: block a missing `hub_routing_rules` match; only current-hub Manager/Admin resolve configuration. Sorter sees only next hub/SOC, destination grouping, and readiness. Repeated scans are idempotent and do not create duplicate operational records.

## Container and handover evidence

Selected: scan every container before driver acceptance and retain both actors/timestamps. Seal containers when entered into handover; correct through audited cancel/recreate. Continue valid goods after destination reconciliation while recording confirmed missing items through canonical `MISSING_PARCEL` evidence. Realtime remains role/hub scoped.

## Linehaul trip flow

Selected: manual accountable driver lifecycle Accept → Start → Arrive, with permitted demo time simulation only. Destination inbound reconciliation completes physical receipt. Drivers may report `TRIP_DELAY`; inbound evidence creates `MISSING_PARCEL`. Drivers see only assigned work in allowed route scope.

## Operations UI and realtime

Selected: workspace and queue by position, separate Manager dashboard, action-required realtime only, scoped KPIs/exceptions, and a scoped shipment audit timeline.

## the agent's Discretion

JSP presentation, DTO/service/repository naming, SQL Server locking, and STOMP destination/payload details remain flexible only within canonical Workflow/ADR/data-scope constraints.

## Deferred Ideas

None — discussion stayed within Phase 03.
