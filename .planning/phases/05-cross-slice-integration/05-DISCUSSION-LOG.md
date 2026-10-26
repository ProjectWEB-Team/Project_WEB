# Phase 05: Cross-slice Integration - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in `05-CONTEXT.md`; this log preserves the alternatives considered.

**Date:** 2026-10-25
**Phase:** 05-cross-slice-integration
**Areas discussed:** Integration proof, Normal journey contract, Exception consistency, Cross-slice management proof

---

## Integration proof

| Decision | Alternatives considered | Selected |
|---|---|---|
| Verification boundary | Real SQL Server automation + concise manual UAT; manual-first; automation only | Real SQL Server automation + browser/STOMP UAT |
| Fixture preparation | Idempotent seed/reset scripts; manual setup; tests only | Idempotent SQL Server seed/reset scripts |
| Exception organization | Separate test families; one long test; manual only | Custody, delivery/return, finance families with shared fixture builder |
| Realtime/management scope | Private scoped proof; full matrix; defer | Private action-required messages plus scoped views; full matrix deferred to Phase 06 |

**User's choice:** Recommended option for every decision.
**Notes:** No passwords, JWT secrets, or other credentials may enter scripts or planning artifacts.

---

## Normal journey contract

| Decision | Alternatives considered | Selected |
|---|---|---|
| Handoff evidence | Assert every handoff; start/end only; manual UI | Status + event type + custody/current hub + finance record per handoff |
| Tracking source | Persisted event/audit; current status only; JSP-derived | Persisted canonical event/audit timeline |
| Payload privacy | Minimal scoped DTOs; common full payload; hide only in JSP | Minimal purpose- and actor-scoped DTOs |
| Duplicate command behavior | Service idempotency; UI guard; warn but persist | Service-level idempotency with tests |

**User's choice:** Recommended option for every decision.
**Notes:** Current status is a snapshot, not a history reconstruction mechanism.

---

## Exception consistency

| Decision | Alternatives considered | Selected |
|---|---|---|
| Invalid action outcome | Atomic transaction; UI error only; persist then repair | Atomic transaction with safe error/no partial persistence |
| Missing/COD audit | Incident/reconciliation record; free-text event; new status | Canonical incident or reconciliation item with audit/reference |
| Recovery depth | Follow terminal outcome; stop at failure; direct admin adjustment | Follow canonical recovery through terminal outcome |
| Expiry verification | Expired fixture/time seam; shorten production timer; skip | Expired fixture or controlled time seam |

**User's choice:** Recommended option for every decision.
**Notes:** No new ShipmentStatus or `shipper_debts` model is allowed.

---

## Cross-slice management proof

| Decision | Alternatives considered | Selected |
|---|---|---|
| Aggregate scope | Service scope; all data for demo; JSP hiding | Service-enforced Manager/Admin contextual scope |
| Management content | KPI/work items scoped; full entities; totals only | KPI + actionable work items + minimal scoped drill-down DTOs |
| Management notification | Safe-summary private STOMP; full payload; refresh only | Identifier + action type + safe summary; scoped detail fetch |
| Denial evidence | Cross-hub/non-admin denial; success only; route-role only | Test cross-hub Manager and non-Admin denials before data/event disclosure |

**User's choice:** Recommended option for every decision.
**Notes:** A JSP menu or route guard is not the data-scope boundary.

---

## the agent's Discretion

- Concrete class names, SQL Server fixture layout, test decomposition, DTO shapes, and JSP presentation remain planner discretion inside the locked canonical contracts.

## Deferred Ideas

- Full authorization/WebSocket matrix and demo hardening belong to Phase 06.
- Live financial providers, AI routing, realtime GPS, and unrequested infrastructure remain outside this milestone.
