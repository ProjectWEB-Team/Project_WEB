# Phase 04: Last-mile & Finance - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents. Decisions are captured in `04-CONTEXT.md`; this log preserves the alternatives considered.

**Date:** 2026-10-13
**Phase:** 04-last-mile-finance
**Areas discussed:** Delivery attempt & return, COD & financial lifecycle, Wallet/reconciliation/withdrawal, Last-mile workspace/privacy/realtime

---

## Delivery attempt & return

| Decision | Selected |
|---|---|
| Failure handling | Canonical retry/fast-fail split with attempt evidence and no extra ShipmentStatus |
| Custody before delivery | Assigned shipper scans each shipment at applicable delivery hub |
| Money collection | Record due collection before confirmation of delivery |
| Return completion | Support both origin-hub collection and assigned home return delivery |

**User's choice:** Option 1 for all four decisions.

---

## COD & financial lifecycle

| Decision | Selected |
|---|---|
| COD status | Full canonical lifecycle through reconciliation and payout |
| Return liability | Canonical fee-payer and return-fee rules |
| Refund/compensation | Atomic wallet credit plus referenced ledger record |
| Duplicate protection | Service-level idempotency/transaction/locking |

**User's choice:** Option 1 for all four decisions.

---

## Wallet, reconciliation & withdrawal

| Decision | Selected |
|---|---|
| Reconciliation mismatch | Resolve item-by-item; only discrepant COD enters dispute/incident |
| Withdrawal request | Lock available balance atomically on request |
| Withdrawal review | `ROLE_ADMIN` only; canonical approve/reject balance transitions |
| Wallet creation | Lazy creation for users eligible for financial flows |

**User's choice:** Option 1 for all four decisions.

---

## Last-mile workspace, privacy & realtime

| Decision | Selected |
|---|---|
| Workspace scope | Dedicated role workspaces with Service-enforced contextual scope |
| STOMP events | Private action-required only |
| Personal data | Least privilege by active assignment |
| Delivery failure evidence | Nullable attachment/evidence reference, not raw SQL Server image binary |

**User's choice:** Option 1 for all four decisions.

## the agent's Discretion

- Concrete view, DTO, service, repository, notification-destination, and storage-adapter naming within the constraints in `04-CONTEXT.md`.

## Deferred Ideas

- Live banking/VNPay payment provider work remains V2.
- Cross-slice integration remains Phase 05; system-wide hardening remains Phase 06.
