# Phase 06: Security + UAT + Demo Hardening - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-26
**Phase:** 06-security-uat-demo-hardening
**Areas discussed:** Security review scope, UAT strategy and evidence, Demo mode, Handling findings

---

## Security review scope (HARD-01)

| Option | Description | Selected |
|--------|-------------|----------|
| Full sweep + matrix | Matrix over all controllers/API/JSP/STOMP with positive and negative tests | ✓ |
| High-risk flows only | Finance, custody, private tracking only | |

**User's choice:** Full sweep + matrix (recommended)

## UAT strategy and evidence (HARD-02)

| Option | Description | Selected |
|--------|-------------|----------|
| Automate STOMP + manual checklist | Multi-session STOMP tests in Maven; JSP via manual checklist | ✓ |
| Existing Maven tests only | No multi-session STOMP automation | |
| Full browser automation | Selenium/Playwright | |

**User's choice:** Automate STOMP + manual checklist (recommended)

## Demo mode (HARD-03)

| Option | Description | Selected |
|--------|-------------|----------|
| Full Workflow §101 flags | Add fast-track-route-bypass and auto-linehaul-transit-simulation, demo-only | ✓ |
| Adjust existing timeouts only | Keep current profile, manual scenarios | |

**User's choice:** Full Workflow §101 flags (recommended)

## Handling findings

| Option | Description | Selected |
|--------|-------------|----------|
| Fix high/critical, record rest | Matches security_block_on=high | ✓ |
| Record only | Report, no fixes | |
| Fix all | Including low severity | |

**User's choice:** Fix high/critical, record rest (recommended)

## Claude's Discretion

Matrix format, STOMP test harness, scenario-script format, severity details.

## Deferred Ideas

Real banking/VNPay, AI routing, GPS, fraud detection, browser E2E framework, mass `@PreAuthorize` migration.
