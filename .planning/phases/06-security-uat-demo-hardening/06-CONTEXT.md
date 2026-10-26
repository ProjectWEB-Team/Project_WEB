# Phase 06: Security + UAT + Demo Hardening - Context

**Gathered:** 2026-10-26
**Status:** Ready for planning

<domain>
## Phase Boundary

Verify and harden the delivered Phase 01–05 source (HARD-01, HARD-02, HARD-03): contextual authorization everywhere, automated + manual UAT evidence, and Workflow-permitted demo mode for the canonical primary/exception scenarios. Owner: Person B (TV2), parallel to Phase 05.1 (TV3); no dependency on TV3 finishing. No new business capabilities, roles, states or schema redesign.

</domain>

<decisions>
## Implementation Decisions

### Security review scope (HARD-01)
- **D-01:** Full sweep with a permission matrix: action × (role, hub scope, position, task assignment, shipment location, resource ownership) for every controller, API endpoint, JSP route and STOMP destination/subscription. Each matrix cell gets a positive test and a negative (out-of-scope access) test.
- **D-02:** Authorization is currently route-level (`SecurityConfig`) plus service-layer checks; there is no `@PreAuthorize`. Do not mass-migrate to method security; verify and fill gaps in the existing pattern (service-layer contextual checks, ADR-015).
- **D-03:** The web chain keeps CSRF; the `/api/**` JWT chain keeps CSRF disabled (stateless). Verify, do not change, unless a finding requires it.

### UAT strategy and evidence (HARD-02)
- **D-04:** Automate multi-session STOMP checks inside Maven (right recipient receives, unrelated sessions excluded, minimal private payload, nothing emitted on rollback). This closes the technical part of the pending `05-UAT.md` item 3.
- **D-05:** The remaining JSP/browser behavior stays a manual checklist with a result template; Phase 05.1 (TV3) owns executing its guide. Do not mark unobserved checks as passed.
- **D-06:** Coverage must span vertical paths, state/event invariants, financial invariants (wallet `balance`/`locked_balance`, ledger once-only), data scope, error paths and applicable WebSocket behavior. Existing baseline: 516 tests incl. 50 real SQL Server cases — extend, do not duplicate; use the dedicated verification database, never reset the application database.
- **D-07:** No E2E browser framework (Selenium/Playwright) is added.

### Demo mode (HARD-03)
- **D-08:** Implement the Workflow §101 flags `fast-track-route-bypass` and `auto-linehaul-transit-simulation`, enabled only under the demo profile; production defaults stay safe (bypass off, day-lock on). Tests prove they are inert outside demo and do not alter real domain rules, the 13 statuses, events or wallet invariants.
- **D-09:** Reconcile property naming: code uses `uteexpress.*` (`application-demo.properties`, `LogisticsProperties`), Workflow documents `app.logistics.*` / `app.demo.*`. Planner chooses the minimal consistent approach (prefer keeping working `uteexpress.*` keys or mapping both) and documents it; do not break existing configuration.
- **D-10:** Cover the primary (Workflow §106) and exception (§107) demo scenarios with a runnable scenario script/checklist under demo mode.

### Handling findings
- **D-11:** Fix high/critical findings in this phase (matches `security_block_on: high`); lower-severity findings are recorded in the report for the owner's review. Source fixes go on a dedicated branch from `main` (never directly on `project-plans`); planning artifacts stay on `project-plans`.
- **D-12:** TV3's Phase 05.1 findings are inputs only; Phase 06 does not wait for them.

### Claude's Discretion
- Matrix file format and test organisation; exact STOMP test harness; scenario-script format; severity classification details within ASVS L1.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Authority (in order)
- `docs/ARCHITECTURE_DECISIONS.md` — ADR-002 (roles in `users.role`), ADR-003/014 (13 statuses, events), ADR-006 (no `shipper_debts`), ADR-015 (contextual authorization)
- `docs/UTEExpress_Workflow.md` §101 (Demo Mode), §106 (primary demo), §107 (exception demo) — demo flags and scenarios
- `docs/ERD.md`, `docs/PROJECT_SPEC_KNOWLEDGE_BASE.md`, `docs/ENGINEERING_STANDARDS.md`

### Planning and evidence
- `.planning/ROADMAP.md` — Phase 06 goal and success criteria
- `.planning/REQUIREMENTS.md` — HARD-01, HARD-02, HARD-03
- `.planning/STATE.md`, `.planning/BRANCH_POLICY.md` — branch rules (planning on `project-plans`, source on `main`)
- `.planning/phases/05-cross-slice-integration/05-UAT.md` — pending item 3 (browser/STOMP)
- `.planning/phases/05.1-full-functional-acceptance/05.1-01-PLAN.md` — TV3 manual guide
- `docs/PHASE_05_VERIFICATION.md`, `docs/PHASE_05_1_PREPLAN_REGRESSION.md`, `docs/PHASE_05_SQL_RESULTS.md` — current test evidence
- `.planning/config.json` — security_enforcement, ASVS level 1, block on high

Note: `.planning/codebase/*.md` is dated 2026-10-01 and describes the pre-Phase-01 bootstrap; treat as stale and rely on current source.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `src/main/java/vn/iotstar/config/SecurityConfig.java`: dual chains (API order 1 JWT/no CSRF; web order 2 with CSRF; `/ws/**` authenticated)
- `src/main/java/vn/iotstar/config/LogisticsProperties.java` (+ `Timeout`), `src/main/resources/application-demo.properties`: existing demo timeout/day-lock profile
- `src/main/java/vn/iotstar/security/` (`AuthenticatedActor`, `JwtAuthenticationFilter`), `config/AuthenticatedShipmentStompInterceptor`, `config/WebSocketConfig`
- Exceptions `ShipmentAccessDeniedException`, `HubOperationAccessDeniedException` — existing denial pattern
- `src/test/java/vn/iotstar/{config,controller,integration,realtime,security,service}` — 88 test files incl. SQL integration suites

### Established Patterns
- Service-layer contextual authorization; operational activity as `shipment_events.event_type`; private after-commit STOMP

### Integration Points
- Hardening touches every slice's controllers/services; demo flags touch route planning/linehaul (`LinehaulTripService`, hub routing rules) and `IntakeTimeoutService`

</code_context>

<specifics>
## Specific Ideas

No specific requirements beyond the Workflow §101/§106/§107 definitions — open to standard approaches.

</specifics>

<deferred>
## Deferred Ideas

- Real banking payout / VNPay refund integration, AI routing, realtime GPS, fraud detection (out of MVP)
- Browser E2E automation framework (declined for this phase)
- Mass migration to `@PreAuthorize` method security

</deferred>

---

*Phase: 06-security-uat-demo-hardening*
*Context gathered: 2026-10-26*
