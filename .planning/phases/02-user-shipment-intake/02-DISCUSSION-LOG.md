# Phase 02: User & Shipment Intake - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents. Decisions are captured in `02-CONTEXT.md`.

**Date:** 2026-10-12  
**Phase:** 02-user-shipment-intake  
**Areas discussed:** Account access and intake wizard, tracking and recipient privacy, pickup assignment boundary

---

## Account access and intake wizard

| Option | Description | Selected |
|---|---|---|
| Multi-step wizard | Sender/receiver → parcel/service → quote/payment → first-mile method | ✓ |
| Single long form | All intake fields in one screen | |

**User's choice:** Multi-step wizard.

## Demo payment initiation

| Option | Description | Selected |
|---|---|---|
| Internal demo payment | Payment record plus explicit demo confirmation/state transition | ✓ |
| Cash-only | Defer online payment initiation | |

**User's choice:** Internal demo payment; live VNPay/bank remains V2.

## First-mile method

| Option | Description | Selected |
|---|---|---|
| Final wizard step | Choose `HOME_PICKUP`/`SELF_DROPOFF` after payment handling | ✓ |
| Shipment detail later | Choose the method after initial creation | |

**User's choice:** Final wizard step.

## Tracking and recipient privacy

| Option | Description | Selected |
|---|---|---|
| Guest tracking page | Visitor enters tracking number and receives only public fields | ✓ |
| Shareable direct URL | A direct public link encodes the tracking lookup | |

**User's choice:** Guest tracking page.

## Pickup assignment boundary

| Option | Description | Selected |
|---|---|---|
| Minimal coordinator assignment | Assign a pickup task to `ROLE_SHIPPER`; shipper sees only assigned tasks | ✓ |
| Seed-only assignment | Use database/test fixtures without an application action | |

**User's choice:** Minimal coordinator assignment.

## Deferred Ideas

- Live VNPay/bank-provider integration remains V2.
