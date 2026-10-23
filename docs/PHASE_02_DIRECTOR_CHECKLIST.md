# Phase 02 — Director Checklist: User & Shipment Intake

> **Owner:** Person A / Phase 02 owner
>
> **Purpose:** This is the single daily execution checklist for Phase 02. Open it at the start and end of each session and keep incomplete acceptance gates visible. On 2026-10-22 the user explicitly requested implementing account code/UI while deferring provider configuration; independent implementation may proceed while database setup remains pending.

> **Vietnamese scope guide (GitHub audit 2026-10-22):** Read [PHASE_02_SCOPE_VI.md](PHASE_02_SCOPE_VI.md) for the complete Person-A scope, existing Phase-03/04 contracts, database responsibilities, and unresolved decisions.
> **Current state (2026-10-23, after wizard implementation):** JDK-22 `mvn -B package` passes **171 tests, zero failures/errors/skips**, including **11 real SQL Server tests**. The four-step wizard now uses a session-owned quote reference, rejects changed inputs/prices and consumes the quote after successful creation. Ten headless Chrome interaction checks passed on Jasper-rendered fixtures. `waiting_for_handover_at` is persisted and its additive migration has been applied locally. Account/linking and payment concurrency evidence from the preceding fixes remains valid. The owner will supply the team's prices; none were seeded for UI use. Manual UAT, real providers, remaining intake contracts and first-mile/tracking/refund stay open. Setup: [PHASE_02_ACCOUNT_SETUP.md](PHASE_02_ACCOUNT_SETUP.md).

### Next actions after the 2026-10-23 fixes

1. Reopen/refresh Eclipse and manually check home, registration, local OTP activation and login. Its saved local launch now excludes test code and uses the project working directory; the actual IDE launch has not yet been observed.
2. Receive the team's pricing table (service, route, base weight/fee, extra-weight step/fee, insurance/COD percentages and any volumetric/rounding rule), validate it, then load the agreed data. The user explicitly declined demo pricing. The current local pricing table is empty.
3. With those prices, perform the three payment-branch browser checks. SQL tests already prove sender-online, sender-cash and receiver-cash timing using isolated test policies that are removed afterward.
4. Finish Day 06–09 acceptance: agree service/policy audit, hub handoff, shared version and payment-deadline duration. Wizard, handover-wait persistence and focused JSP/MVC/SQL tests now pass; group pricing and owner UAT remain required.
5. Continue to first-mile assignment/cancellation/timeouts, then tracking, refund outbox and recipient unlock/realtime in the dependency order below. Google credentials and real SMS/Zalo delivery remain deferred as requested; SMTP is not required for Google sign-in.

The developer handles code and automated tests. The owner checks the browser flow and coordinates unresolved pricing/hub contracts with the team. Do not recreate the database or rewrite the already implemented account/linking services.

---

## 1. Non-negotiable delivery rules

### 1.1 Scope

Phase 02 owns **account access, full shipment intake, quote/payment initiation, first-mile pickup/dropoff, tracking, recipient privacy, and safe realtime updates**.

It does **not** own:

- Hub routing, inbound/outbound, container, custody, handover, trip, or incidents — Phase 03.
- Last-mile delivery, returns, COD settlement, wallet, reconciliation, or withdrawal — Phase 04.
- A real VNPay/bank integration — V2 only.

### 1.2 Authority order

When documents disagree, stop and follow this order:

1. `docs/ARCHITECTURE_DECISIONS.md`
2. `docs/UTEExpress_Workflow.md`
3. `docs/ERD.md`
4. `docs/PROJECT_SPEC_KNOWLEDGE_BASE.md`
5. `docs/ENGINEERING_STANDARDS.md`
6. This checklist and the Phase-02 execution plans

### 1.3 Invariants that may never change

- Persist exactly six roles: `ROLE_USER`, `ROLE_SHIPPER`, `ROLE_HUB_STAFF`, `ROLE_LINEHAUL_DRIVER`, `ROLE_MANAGER`, `ROLE_ADMIN`.
- Persist exactly 13 `ShipmentStatus` values. Never add convenience statuses such as `PICKUP_TASK_ASSIGNED`, `SORTING`, or `RETURN_REQUESTED`.
- Log operational actions through `shipment_events.event_type`; do not use events as a replacement for shipment state.
- Use JSP/JSTL + Bootstrap, Spring Boot/JPA, SQL Server, session + CSRF for browser routes, JWT for `/api/**`.
- Controllers receive DTOs and call services only. Business rules, transactions, and authorization live in services.
- Derive actor identity from `AuthenticatedActor`; never trust user ID, hub ID, assignee ID, price, status, deadline, or authorization from the browser.
- Use DTO projections. Never expose entity objects, passwords, OTPs, addresses/contacts, finance, staff, or audit data outside their authorized scope.

### 1.4 Definition of “done”

An implementation checkbox records only the concrete action named on that line, such as adding a class or test. It does not certify the whole capability. A day/capability is accepted only when its exit condition and all applicable evidence below are satisfied:

- [ ] Implementation is limited to the listed Phase-02 scope.
- [ ] Relevant unit/MVC/security tests pass.
- [ ] Relevant SQL Server integration test passes when the shared SQL Server environment is available.
- [ ] Existing Phase-01 tests still pass.
- [ ] The browser flow has been manually checked with the required user roles.
- [ ] No secret, password, OTP, local config, or database credential is committed.
- [ ] The work has a focused commit and an end-of-day log entry below.

---

## 2. Daily operating protocol

Perform this at the beginning of **every** work session:

- [ ] Open this file and find the first incomplete day.
- [ ] Read the source documents named for that day before changing code.
- [ ] Review the current branch, `git status`, and the prior end-of-day log.
- [ ] Write or update the focused failing test first when the day includes a business rule.
- [ ] Make changes within the user-authorized scope; document independent work performed while environment or provider setup is pending.
- [ ] Run the named tests before marking any checkbox complete.
- [ ] Record actual result, blockers, test evidence, and next unchecked item in the daily log.

If blocked by a missing decision, environment, or another phase:

- [ ] Do not invent a rule or implement a workaround outside this checklist.
- [ ] Record the exact question and source references in the blocker log.
- [ ] Check already recorded user/project decisions before asking again. Record unresolved team-owned decisions without claiming that a review or approval happened.

---

## 3. Delivery sequence

```text
Account + phone OTP / Google OIDC (02-01, ADR-016)
        ↓
Registration UI + late receiver link (02-02)
        ↓
Full intake / quote / payment records (02-03)
        ↓
Shipment-creation wizard (02-04)
        ├── Pickup assignment / completion (02-05)
        │       ↓
        │   Timeout / cancellation / race protection (02-06)
        │       ↓
        │   Pending refund obligation outbox (02-09)
        │
        └── Public + masked recipient tracking (02-07)
                         ↓
              Recipient unlock + private realtime + review (02-08)
```

The sequence above is intentionally conservative for one owner. Do not start Plan 02-08 until both Plan 02-07 and Plan 02-09 have passed their tests.

---

## 4. Daily checklist

### Day 00 — Prepare the workspace and database verification environment

Read: `TEAM_START_GUIDE.md`, `ARCHITECTURE_DECISIONS.md`, `PHASE_01_FOUNDATION.md`, `02-CONTEXT.md` on `origin/project-plans`.

- [x] Install/configure Maven; confirm `java -version` and `mvn -version` work.
- [x] Configure local SQL Server/JWT values without putting them in Git; the ignored local properties loaded successfully during the 2026-10-23 packaged-app check.
- [x] Confirm the chosen local database `ute_express` and current JDK-22 baseline; alignment with the fetched Phase-04 Java-21 branch remains a later integration decision.
- [x] Verify the account/shipment/payment schema and canonical event column on the existing local DB: metadata inspection and 11 SQL integration tests passed. The user applied the schema/migrations earlier; no schema reset was performed.
- [x] Create a dedicated Phase-02 feature branch from updated `main`.
- [x] Run the Phase-01 baseline tests before changing code.
- [x] Verify the baseline uses `shipment_events.event_type`, not the legacy `status` column.

**Exit condition:** the unchanged Phase-01 code is buildable/testable locally and the SQL Server verification environment is ready.

### Day 01 — Lock the account activation boundary (Plan 02-01, decision gate)

Read: ADR-002, ADR-016, Workflow §§5–8, Phase-02 decision D-01.

- [x] Record the user's 2026-10-22 decision in ADR-016: public accounts use `ROLE_USER`; phone registration starts `INACTIVE`, while Google verified-email onboarding may create `ACTIVE` with an unverified contact phone. Team-leader approval is not asserted.
- [x] Record OTP storage/policy: `phone_otp_challenges`, normalized phone on the user, hashed code, five-minute expiry, five attempts, 60-second resend cooldown, five sends/hour, consumed time and bounded one-row-per-account storage. Old-hash maintenance is documented but not automated.
- [x] Require a valid phone-registration OTP to activate that path; Google follows the separate verified-email path and never supplies phone ownership.
- [x] Keep role/status/user identity server-owned; OTP/password inputs are proof to validate, not authority supplied by the browser.
- [x] Inspect the selected local DB: users/shipments were empty before this run, so there were no legacy phone collisions to repair; required migrated account/event columns exist.

**Exit condition:** the account boundary is recorded and local SQL evidence now exists. ADR-016 records user authorization; team review has not been recorded.

### Day 02 — Implement registration and OTP persistence (Plan 02-01)

Read: Workflow §§5–8; `ENGINEERING_STANDARDS.md` DTO, transaction, validation, and security rules.

- [x] Add `phase-02-account-otp.sql` for OTP challenge persistence and verification/Google identity columns; migrated columns were inspected and the account SQL tests now pass.
- [x] Bring `User` mapping in line with registration data: username, BCrypt password, full name, normalized unique phone, unique email, role, status, timestamps.
- [x] Add `PhoneOtpChallenge` entity/repository and registration request/verification DTOs.
- [x] Implement `RegistrationService`/`RegistrationServiceImpl` with constructor injection and transactions.
- [x] Hash password/OTP with BCrypt; phone registration creates only `ROLE_USER`/`INACTIVE` server-side.
- [x] Add `OtpDelivery`; default disabled, explicit `otp-local` writes codes to the ignored local inbox and never to normal HTTP/log output.
- [x] Document local simulation and deferred real phone provider; SMTP cannot establish phone ownership.
- [x] Add Google OIDC account service/success handler and optional `google` profile. Match only validated Google `sub`; do not merge contacts, authenticate unfinished onboarding, or grant phone-verification rights.
- [ ] Configure and validate a real phone delivery adapter and Google credentials.

**Exit condition:** a correct, unexpired, unused OTP activates only its owning account.

### Day 03 — Prove registration and OTP rules (Plan 02-01)

- [x] Add service tests for registration conflicts and a phone-normalization test suite.
- [x] Add service tests for wrong, expired, replayed, over-attempted, wrong-purpose/unknown challenges and resend limits.
- [x] Add controller/security tests for forged account fields, server-session challenge binding and CSRF; phone-registration activation requires OTP.
- [x] Add Google tests for verified token identity, account collision rejection, local principal/session handling, expiring onboarding, optional route configuration and no phone login through an unverified contact number.
- [x] Add SQL Server integration evidence for registration, OTP, and activation, including failure rollback and attempt persistence.
- [x] Run `RegistrationServiceImplTest`: 18 tests passed on 2026-10-23.
- [x] Run the Phase-02 account SQL Server integration class with its environment gate enabled: six tests passed.

**Exit condition:** no registration/OTP behavior is marked complete without both unit and SQL Server evidence.

### Day 04 — Link recipients safely after activation (Plan 02-02)

Read: Workflow §§6–8 and data-masking rules.

- [x] Add `ReceiverLinkService` and repository support to find eligible historical shipments.
- [x] On valid OTP activation, atomically link only shipments where normalized `receiver_phone` matches and `receiver_user_id IS NULL`.
- [x] Preserve sender/receiver snapshot fields exactly; only `receiver_user_id` may change.
- [x] Protect concurrent claims using a conditional update or lock.
- [x] Test receiver eligibility in unit tests and matching/unmatched/already-linked rows, snapshots, activation rollback and conditional concurrent claims on SQL Server.

**Exit condition:** late activation links the correct receiver without changing shipment history or leaking its content.

### Day 05 — Registration browser flow (Plan 02-02)

- [x] Add `/register`, `/auth/phone-login`, phone-verification JSP routes and `/auth/google/complete` with session/CSRF protection.
- [x] Use request DTO validation, escaped feedback, PRG, and no password/OTP echoing.
- [x] Keep `/api/**` stateless JWT behavior and browser session authentication.
- [x] Add MVC/security tests for registration, OTP verification, validation, CSRF and Google onboarding.
- [x] Add SQL Server integration evidence for late receiver linking, unchanged snapshots, rollback and competing claims.
- [ ] Complete browser UAT against SQL Server and the configured providers; Google phone verification/linking is still separate future work.

**Exit condition:** a visitor can register and verify a phone through JSP without exposure of credential/OTP data.

### Day 06 — Model full intake and authoritative quote (Plan 02-03)

Read: Workflow §§28, 49–54, 80, 83, 94, and pricing-policy fields.

- [ ] Add/extend SQL Server migrations for Phase-02 shipment and payment fields only.
- [x] Add nullable `waiting_for_handover_at` via guarded migration, set at cash creation or online payment confirmation; preserve it on retries and leave unknown legacy timestamps NULL. Applied locally and repeat execution verified.
- [ ] Expand `Shipment` mapping from Phase-01 defaults to canonical intake data: receiver link/snapshots, province/district, weight/dimensions, declared value, fees, COD, payer, pickup method, deadlines, reasons, lifecycle timestamps, and optimistic version.
- [ ] Add `PricingPolicy`, `Payment`, repositories, request DTO, quote DTO, and `ShipmentIntakeService`.
- [x] Calculate fees on the server from an `ACTIVE` pricing policy using `BigDecimal`.
- [ ] Record weight/dimension units, volumetric-weight rule if required, fee rounding, policy selection, and service/quote audit storage before relying on an unspecified formula or adding a column.
- [ ] Agree the origin/current-hub and destination-field handoff with Phase 03; its initial inbound path currently checks the shipment's current hub against the active operator hub.
- [x] Auto-link an already existing matching receiver account, while always persisting immutable receiver snapshots.
- [x] Use `AuthenticatedActor` as sender; never accept sender ID, total fee, or status from the request.

**Exit condition:** one transaction persists a full intake record, its server-derived quote, and valid initial payment/state branch.

### Day 07 — Enforce permitted payment timing (Plan 02-03)

- [x] Sender online demo payment: create `PENDING` / `WAITING_FOR_PAYMENT`; controlled confirmation moves the same payment to `PAID` / `WAITING_FOR_HANDOVER`; sequential retries are covered.
- [x] Protect concurrent payment confirmations with a shipment write lock; SQL tests prove one event and a stable payment reference/time on retries.
- [x] Sender cash: remain `UNPAID` at creation; do not collect early.
- [x] Receiver payer: remain uncollected until Phase 04 delivery.
- [ ] Reject inactive pricing policy, invalid payer/method combinations, client-total tampering, and duplicate confirmation.
- [x] Do not integrate a real provider, wallet, COD settlement, or financial ledger.
- [x] Add service and SQL Server tests for sender-online, sender-cash and receiver-cash branches, including refusal to confirm cash paths early.

**Exit condition:** payment timing is correct for every Phase-02 branch and no Phase-04 table is written.

### Day 08 — Build the shipment wizard (Plan 02-04)

Read: UI design rules and engineering rules for JSP/validation/PRG.

- [x] Replace/refactor the Phase-01 minimal create route without creating duplicate Spring mappings.
- [x] Build the Bootstrap/JSTL multi-step workflow: sender/receiver → parcel/service → quote/payer/payment → first-mile method.
- [x] Bind the latest quote to reviewed inputs in the session; reject missing/stale references, changed inputs and changed server prices. Consume after commit and serialize final submits within the same session.
- [x] Keep quote, payer, payment method, and state authoritative on the server; hidden browser values cannot override them.
- [x] Add quote, simulated confirmation, and first-mile instruction views.
- [x] Use CSRF, server validation, escaped output, field-level feedback, and PRG; verify Jasper rendering and keyboard/no-JS/hidden-field behavior.

**Exit condition:** a prepaid sender can finish the wizard and reach the correct first-mile instruction page.

### Day 09 — Verify all browser payment branches (Plan 02-04)

- [x] Verify sender online demo confirmation is offered only to the eligible prepaid sender path.
- [x] Verify sender-cash UI says collection happens at pickup/dropoff, not at creation.
- [x] Verify receiver-pays UI says collection happens at delivery, not at creation.
- [x] Add MVC tests for authentication redirect, CSRF, validation errors, quote feedback, confirmation PRG, actor identity and service-only controller behavior.
- [x] Confirm shipment controllers call the service layer without repository or calculation bypasses.

**Exit condition:** the wizard displays no premature finance action and has security regression coverage.

### Day 10 — Implement same-hub pickup assignment (Plan 02-05)

Read: Workflow §§9–10, 29, 82, 86–87; ADR-015.

- [ ] Add mappings/repositories for employee profiles, `shipment_assignments`, and `pickup_attempts`.
- [ ] Reconcile shared models first: Phase 03 already defines `Hub`, `EmployeeProfile`, and shipment `version`; Phase 04 defines `Payment` and `ShipmentAssignment`. Keep one agreed model per table when integrating; do not copy competing definitions.
- [ ] Add `FirstMileService` and DTOs for coordinator assignment and shipper pickup work.
- [ ] Allow assignment only for a `COORDINATOR`/`GENERAL_STAFF` with an active employee profile at the same applicable hub.
- [ ] Allow only an active same-hub `ROLE_SHIPPER` with the assigned task to read/complete it.
- [ ] Persist assignment, pickup attempt, `PICKUP_TASK_ASSIGNED`/`PICKED_UP` events, `PICKED_UP` status, and timestamps atomically.
- [ ] Record unsuccessful pickup contact attempts with canonical results. For sender-CASH pickup, collect only at actual handover and persist the server-derived shipping-fee payment exactly once in the pickup transaction (Workflow §§10, 52).

**Exit condition:** one authorized coordinator can safely assign one eligible `HOME_PICKUP` task to one eligible shipper.

### Day 11 — Prove pickup data scope (Plan 02-05)

- [ ] Test wrong role, wrong position, inactive profile, different hub, unassigned shipper, `SELF_DROPOFF`, and closed shipment denial before data is read/written.
- [ ] Test assignment and pickup completion with optimistic locking.
- [ ] Add SQL Server integration proof with two hubs and separate coordinator/shipper profiles.
- [ ] Test sender-CASH pickup amount/timing and retry protection, prepaid no-double-collection, and receiver-pays no-early-collection. Record the separate sender-cash accountability handoff with finance.
- [ ] Confirm Phase 02 does not implement hub inbound, routing, custody, or linehaul behavior.

**Exit condition:** role + position + active status + hub + assignment scope is enforced in the service layer.

### Day 12 — Implement timeout and verified service-failure logic (Plan 02-06)

Read: Workflow §§31–33 and §100.

- [ ] Add typed timeout configuration with production values fixed at 48 hours.
- [ ] Add a server-side timeout job/service; derive time solely from persisted timestamps and an injectable `Clock`.
- [ ] Persist the actual entry time into `WAITING_FOR_HANDOVER`; do not substitute creation time after a delayed prepaid confirmation. Agree automated-event actor/provenance because the current event entity requires a non-null actor.
- [ ] Expire unfinished `HOME_PICKUP` exactly 48 hours after `assigned_at` as `CANCELLED` / `PICKUP_TIMEOUT`.
- [ ] Expire `SELF_DROPOFF` exactly 48 hours after entering `WAITING_FOR_HANDOVER` as `CANCELLED` / `DROPOFF_TIMEOUT`.
- [ ] Allow only an authorized same-hub coordinator/`GENERAL_STAFF` to record verified `PICKUP_SERVICE_FAILURE` with bounded evidence.
- [ ] Append events/attempts; do not add a ShipmentStatus.

**Exit condition:** deadline and failure reasons come from server data, not browser input.

### Day 13 — Implement cancellation race protection and routes (Plan 02-06)

- [ ] Add CSRF/PRG routes for sender cancellation, coordinator assignment/failure, and assigned-shipper completion.
- [ ] Allow sender cancellation only at `CREATED` or `WAITING_FOR_HANDOVER`.
- [ ] Use optimistic/conditional updates so sender cancellation and shipper pickup have exactly one winner.
- [ ] If pickup wins, set `PICKED_UP` and refuse cancellation. If cancellation wins, cancel assignment and set `CANCELLED` / `SENDER_REQUEST`.
- [ ] Refuse direct cancellation at `PICKED_UP` or later.
- [ ] Add exact time-boundary tests: 47:59:59 does not expire; 48:00:00 expires.
- [ ] Add real SQL Server evidence for cancellation-versus-pickup single-winner behavior, not only mocked repository behavior.

**Exit condition:** no state transition permits two winners or a direct post-pickup cancellation.

### Day 14 — Implement public and masked tracking projections (Plan 02-07)

Read: Workflow §§7, 78–79.

- [ ] Add separate DTOs/services for public tracking, recipient-masked tracking, and sender-owned detail.
- [ ] Deliver sender-owned shipment listing and the linked-recipient incoming list with scoped/paginated queries; retain owner-only sender detail.
- [ ] Add a guest tracking search page and `/api/tracking/**`; the primary boundary is typed search, not a shareable direct URL.
- [ ] Public DTO includes only tracking number, current status, current hub, safe timeline, and ETA.
- [ ] Record the ETA source/unavailable behavior; do not invent a delivery promise. UI masking examples do not authorize extra public contact/address fields.
- [ ] Recipient incoming list shows masked sender/address/item information only.
- [ ] Redact unsafe event descriptions and never serialize entities.

**Exit condition:** guest and linked-recipient reads are least-privilege projections, while the sender retains ownership-scoped detail.

### Day 15 — Prove tracking privacy (Plan 02-07)

- [ ] Test unknown number, ID/phone enumeration, unlinked recipient, and existing/late receiver links.
- [ ] Test every prohibited field: full phone, full address, employee/driver, incident, finance, audit, assignment.
- [ ] Add SQL Server integration evidence that snapshots do not mutate and tracking writes no operations/finance records.
- [ ] Add MVC/API tests for guest and recipient routes.

**Exit condition:** tracking cannot disclose protected information through DTOs, event text, IDs, or guessed phone values.

### Day 16 — Persist pending refund obligations only (Plan 02-09)

Read: Workflow §§55–57; ADR-001; Phase-02 refund handoff.

- [ ] Add a SQL Server refund-outbox migration, entity, repository, DTO, and `RefundObligationService`.
- [ ] For an already `PAID` shipping fee cancelled by `SENDER_REQUEST`, `PICKUP_TIMEOUT`, `PICKUP_SERVICE_FAILURE`, or `DROPOFF_TIMEOUT`, write exactly one pending wallet-credit obligation in the same transaction.
- [ ] Persist shipment, sender, payment, amount, cancel reason, idempotency key, and delivery state.
- [ ] Keep `Payment` at `PAID` while this outbox record is pending.
- [ ] Create no obligation for sender-cash or receiver-pays unpaid paths.

**Exit condition:** Phase 02 records an idempotent obligation; it does not claim a refund has completed.

### Day 17 — Prove the Phase-04 refund handoff (Plan 02-09)

- [ ] Test all four paid cancellation paths and every unpaid non-refund path.
- [ ] Test duplicate cancellation/scheduler execution creates only one obligation.
- [ ] Test rollback leaves neither cancellation nor outbox record; commit leaves both.
- [ ] Add SQL Server unique-constraint and integration evidence.
- [ ] Confirm no Phase-02 code changes `wallets`, creates ledger entries, or marks payment `REFUNDED`.
- [ ] Record the remaining consumer integration with Person C: the fetched Phase-04 service discovers PAID/CANCELLED payment candidates and refunds by payment ID; it does not yet consume or mark the Phase-02 outbox delivered. Do not claim the cross-slice handoff is implemented merely because that refund method exists.

**Exit condition:** the Phase-04 consumer can later atomically credit the wallet, create the ledger entry, mark the outbox delivered, and then mark the payment `REFUNDED`.

### Day 18 — Add recipient unlock and safe realtime (Plan 02-08)

- [ ] Add recipient detail unlock using either the exact full tracking number or a recipient-and-shipment-bound OTP proof.
- [ ] Require exact tracking number, not prefix/partial matching.
- [ ] Make OTP proof expiring and one-time; reject wrong recipient, unlinked recipient, replay, partial value, or cross-shipment use.
- [ ] Extend STOMP only with private, DTO-only, after-commit intake/assignment/pickup/cancel messages.
- [ ] Ensure STOMP payloads contain no contacts, address, OTP, payment/refund amount, finance, staff, incidents, or audit information.

**Exit condition:** recipient unlock and realtime messages cannot disclose information to an unintended user.

### Day 19 — Final regression, UAT, and review gate (Plan 02-08)

- [ ] Run the complete Maven test suite.
- [ ] Run every Phase-02 SQL Server integration test in a configured environment.
- [ ] Perform UAT as guest, sender, recipient, coordinator, assigned same-hub shipper, cross-hub shipper, and unauthorized user.
- [ ] Verify the three payment branches, both first-mile methods, cancellation race, 48-hour boundaries, public tracking, recipient masking, unlock, and private STOMP behavior.
- [ ] Ask Person B to review roles/status/events, timeout, payment timing, and same-hub scope.
- [ ] Ask Person C to review recipient privacy and the pending refund-outbox contract.
- [ ] Resolve every finding before declaring Phase 02 complete.

**Exit condition:** all five requirements `INTAKE-01` through `INTAKE-05` have automated evidence, UAT evidence, and both required cross-reviews.

---

## 5. Explicit blockers requiring a leader decision

Do not decide these silently:

| Topic | Required decision |
|---|---|
| Self-registration | User decision recorded in ADR-016: phone OTP and Google OIDC, `ROLE_USER` only, separate phone/email verification. Account/linking SQL evidence exists; team review, real providers and browser UAT remain pending. |
| OTP persistence/delivery | Local DB connectivity, required schema and SQL transactions are verified. Real phone provider and old-hash/local-file maintenance remain pending. No real adapter has been integrated. |
| Service selection audit | The workflow requires a service choice but the authoritative shipment field list does not list `service_code`; confirm how that choice is retained before adding an unapproved shipment column. |
| Scheduler operation | Confirm the deployment-approved scheduler trigger/profile for executing the timeout service; preserve the required 48-hour production rule. |
| Shared baseline/models | Agree Java 21/22 and the single Shipment/Hub/EmployeeProfile/Payment/ShipmentAssignment/event/version contract before integrating remote slices. |
| Origin/destination handoff | Agree how intake derives the origin/current hub and supplies Phase-03 destination region/province without treating browser IDs as authority. |
| Quote policy gaps | Record units, volumetric-weight requirements, rounding, policy selection, and audit storage for fields absent from canonical shipments. |
| SELF_DROPOFF boundary | Confirm ownership of counter collection, reweigh/surcharge, WEIGHT_DISCREPANCY cancellation/refund, and actual AT_HUB acceptance; Phase 02 must not silently implement Phase-03 inbound or Phase-04 wallet mutation. |
| Sender-CASH accountability | Agree how Phase-02 pickup cash records reach finance; the fetched Phase-04 reconciliation source path currently distinguishes COD and receiver-cash fees. |
| Refund consumer | Agree consumption and atomic outbox delivery/credit/ledger/payment completion with Person C; no consumer currently exists in the reviewed Phase-04 code. |
| Account/UI scope gaps | Phone-provider configuration and authenticated Google-account phone verification remain pending. Confirm address-book/profile expectations, guest calculator scope and ETA source before expanding those capabilities. |
| Scheduler event provenance | Agree server-owned automated-event actor/provenance and treatment of legacy NULL timestamps before implementing timeout jobs. Handover-wait timestamp now exists, using the existing local-server time convention. |
| Payment expiry | `payment_deadline` exists in canonical SQL, but no duration is specified; record that decision before setting deadlines or cancelling unpaid orders. |
| Existing database data | Stop if migration of existing user/status/event data would break the canonical role, activation, or event-type contracts. |

---

## 6. End-of-day log

Append one entry at the end of every work session. Do not remove historical entries.

```text
Date:
Checklist day:
Branch / commit:
Completed today:
Tests run and result:
Manual verification:
Blockers / decision needed:
Next unchecked checkbox:
```

---

## 7. Completion sign-off

- [ ] `INTAKE-01` — registration, authentication boundary, OTP activation, and late receiver link.
- [ ] `INTAKE-02` — complete server-validated shipment intake.
- [ ] `INTAKE-03` — authoritative quote and correct payment initiation/timing.
- [ ] `INTAKE-04` — first-mile pickup/dropoff, assignment, cancellation, timeout, race protection, and refund handoff.
- [ ] `INTAKE-05` — sender, recipient, and guest tracking with privacy, unlock, and safe realtime.
- [ ] Person B architecture review completed.
- [ ] Person C privacy/finance-boundary review completed.
- [ ] Full test suite, SQL Server evidence, UAT evidence, and clean Git status recorded.

**Phase 02 is not complete until every item above is checked.**

### 2026-10-22 — Day 00 environment preparation (in progress)

- Branch / baseline commit: `codex/phase-02-intake` / `358655c`.
- Installed Apache Maven 3.9.9 under the current user's LocalAppData; verified the distribution SHA-512 checksum and added its bin directory to the user PATH.
- Verified existing JDK 22; project-local ignored `.codex/local-env.ps1` selects it without changing system Java. Generated an ignored local JWT signing secret.
- Tests: unchanged Phase-01 `mvn test` using JDK 22: BUILD SUCCESS, 58 tests, 0 failures, 0 errors, 1 skipped (SQL Server environment not configured). Local output: `.codex/day00-baseline.log`.
- Manual verification: local SQL Server service is running; Windows-authenticated read-only connection succeeds. Entity maps `shipment_events.event_type`.
- User requested time to update GitHub before database setup. No database, SQL login, or schema has been created or modified.
- Remote Phase-04 branch is present and contains schema scripts and `docs/PHASE_04_HANDOFF.md`; its POM targets Java 21, while current main targets Java 22. Re-check the selected baseline after the user's update.
- Next unchecked items: confirm database target/credentials, apply and verify the agreed canonical schema and migrations, then run the real SQL Server integration test. Day 00 remains incomplete; no completion checkbox ticked.

### 2026-10-22 — Phase-02 scope audit requested by the owner

- Branch: `codex/phase-02-intake`; baseline remains `358655c`.
- Fetched origin and inventoried 336 distinct Git file versions across main, project-plans, hub-linehaul and phase-04-last-mile-finance.
- Added Vietnamese scope guide and source inventory; linked them from docs/README.md and this checklist.
- Clarified missing explicit pickup-cash/attempt tests, sender listing, persisted wait timestamp, shared entity/JDK/hub contracts, ETA, scheduler provenance and pending refund consumer integration.
- Kept self-dropoff acceptance/reweigh/surcharge ownership, address book, calculator scope, OTP delivery and pricing gaps as recorded decisions rather than silently adding implementation scope.
- Validation: documentation links/plan coverage/checkbox preservation and whitespace checked; no application changes, migration execution or remote push. Earlier baseline result remains 58 tests / 0 failures / 0 errors / 1 SQL Server skip; other branches were inspected, not executed.
- Next unchecked item remains Day 00 database target/credentials/schema verification, then D-01/OTP decisions for 02-01.

### 2026-10-22 — Account core/UI implemented; provider and database setup deferred

- User request: implement phone OTP plus Google login first, configure providers later. ADR-016 records this request; no team-leader review/approval is claimed.
- Added account mapping, phone normalization, BCrypt passwords/OTP, purpose/expiry/attempt/resend controls, server-session challenge binding and resuming pending registration with matching credentials/normalized contacts while retaining counters.
- Added phone registration/login/resend JSP flows, optional Google OIDC onboarding/login keyed by verified `sub`, local application session creation and automated account/MVC/security coverage. Google email verification does not grant phone ownership; internal roles cannot use these customer login paths.
- Added disabled-by-default `OtpDelivery`, explicit ignored local inbox simulation, optional Google profile and setup/cost documentation. Gmail SMTP is not used for Google sign-in. A real SMS/Zalo adapter is not implemented.
- Prepared transactional SQL Server account migration with preflight checks for existing phone/email normalization and collisions. No target database has been selected or migration executed; SQL integration success and browser UAT remain unproven.
- Final validation: `mvn -B package` on JDK 22 — **BUILD SUCCESS**, 122 tests discovered, **118 passed**, 0 failures/errors, **4 skipped** (three registration SQL Server tests and the existing shipment SQL Server test, all missing the opt-in environment). Built `target/project-web-1.0.war`. Local evidence: `.codex/part01-final-build.log`.
- Four real-HTTP tests started embedded Tomcat/Jasper and compiled/rendered all five authentication JSPs, checking CSRF submission, escaped values and no password/OTP echo. These use mocked services and do not replace SQL Server/provider/browser UAT. Source and new-file whitespace checks passed.
- Documentation updated: ADR, Vietnamese scope, this checklist, docs index and `PHASE_02_ACCOUNT_SETUP.md`. Checked boxes identify concrete implementation work, not completion of a whole day/requirement.
- Next environment step: select DB/credentials and validate migrations/SQL tests. Next feature step: `ReceiverLinkService` and late recipient linking; account core/UI does not complete 02-02, `INTAKE-01` or Phase 02.

### 2026-10-22 — Day 04 receiver linking implemented (not yet compiled or run)

- Added `ReceiverLinkService`/`ReceiverLinkServiceImpl`, `Shipment.receiver` (`receiver_user_id`, LAZY) and `ShipmentRepository.linkReceiver`, a single conditional bulk update (`receiver_user_id IS NULL`) that changes no snapshot column.
- `RegistrationServiceImpl.verify` calls it in the activation transaction for phone-registration OTP only. Login, Google (unverified phone) and internal roles never link.
- Matches the three stored phone forms (`+84…`, `0…`, `84…`) because Phase-01 snapshots keep the typed format; intake (Day 06) should normalize snapshots.
- Tests added/updated: `ReceiverLinkServiceImplTest`, `RegistrationServiceImplTest` (link on activation only). Maven could not run in the cloud session (Maven Central blocked), so run `mvn test` in STS first.
- Not done: tests for already-linked/mismatched/concurrent rows and SQL Server evidence (need a real database). Day 04 stays open.
- Next unchecked: Day 04 tests, then Day 06 once the Section 5 decisions are made.

### 2026-10-22 — Day 06–09 implemented (not yet compiled or run)

- Added `PricingPolicy`, `Payment` (read/insert only), their repositories, `QuoteResponse`, the full `CreateShipmentRequest`, and quote/create/confirm-payment in `ShipmentService`; single-page form with a server quote step (`/user/shipments/quote`), detail page with the next-step instructions.
- Status mapping: SENDER+VNPAY → payment `PENDING`, shipment `WAITING_FOR_PAYMENT`, demo confirm → `PAID`/`WAITING_FOR_HANDOVER` (idempotent, owner only); every other branch → `UNPAID`/`WAITING_FOR_HANDOVER`; RECEIVER payer with a non-CASH method is rejected.
- Assumptions to confirm (Section 5 still open): weight in grams, dimensions in cm, actual weight only (no volumetric rule), whole-VND HALF_UP rounding, service code not stored on the shipment, from/to regions chosen by the user, hubs and `payment_deadline` left null, single-page form instead of a multi-step wizard.
- Not done: SQL migrations (schema is assumed to already have these columns/tables), `payment_deadline`/WAITING_FOR_HANDOVER timestamp, SQL Server tests, MVC tests for the new branches, the multi-step wizard UI. Days 06–09 stay open for those.
- Run `mvn test` in STS (Maven Central is blocked in the cloud) and fix any compile errors first.

### 2026-10-23 — Current-source review and local runtime verification

- Reviewed current account, shipment, configuration/security, DTO/repository, JSP and test source against ownership, workflow and checklist. This review did not fetch newer remote branches or implement feature fixes.
- Branch / baseline: `codex/phase-02-intake` / `358655c`; existing local implementation changes remain uncommitted.
- Ran JDK-22 Maven `-B -Dtest=!ShipmentSqlServerIntegrationTest,!RegistrationSqlServerIntegrationTest package`: **BUILD SUCCESS; 127 tests passed, zero failures/errors/skips among the selected tests. Four SQL tests were excluded, not passed.** Evidence: `target/checklist-review-tests.log`.
- Inspected the built WAR: authentication JSPs and Jasper are included; `WEB-INF/classes/application-local.properties` is absent.
- Started the WAR temporarily on port 18080 with `local,otp-local` and external local configuration. Hikari established a SQL Server connection. HTTP checks: `/login`, `/register`, `/auth/phone-login`, and `/assets/css/ute-express.css` returned 200; `/` returned 500 with an unresolved `https://jakarta.ee/tags/jstl/core` URI. Stopped the temporary process afterward. Evidence: `target/checklist-review-runtime.log`.
- Connection success is not proof that all migrations/constraints are correct. No SQL integration tests, registration/payment writes, real Google/phone-provider flow or manual browser UAT were performed in this review. The prior Eclipse-specific 404 was not reproduced in the packaged-app launch; Eclipse itself was not retested.
- Corrected stale status text: late receiver linking and quote/payment code exist, but SQL/concurrency/complete intake acceptance is still open. Do not repeat those implementations from scratch.
- Next work: fix home rendering and SQL-test fixtures, prove account/linking on SQL Server, protect concurrent payment confirmation and finish Day 06–09 before first-mile work. All Phase-02 sign-off boxes remain open.

### 2026-10-23 — Home, SQL verification and payment confirmation fixed

- Fixed the header JSTL URI, removed unused taglib declarations and permitted internal JSP `INCLUDE` dispatches alongside `FORWARD`/`ERROR`. A real HTTP/Jasper regression first caught the home 500, then the missing layout, and now checks `/` and `/home` with their full layout.
- Added a `PESSIMISTIC_WRITE` shipment query used before payment confirmation. The real SQL concurrent-confirmation test commits one event and preserves the first transaction reference/time on repeated calls.
- Repaired shipment SQL fixtures/expectations/foreign-key cleanup and added all three payment branches. Extended account SQL coverage for receiver snapshots, mismatches/already-owned rows, activation/link rollback and competing conditional claims. Fixtures use exact isolated identities and remove their rows.
- Added quote/confirmation MVC and security regressions for validation, missing-policy feedback, CSRF, session actor, redirects and denied ownership.
- Final validation: JDK 22, local SQL credentials passed only to the test process, `mvn -B package` — **151 tests, zero failures/errors/skips**, including six registration/linking and five shipment/payment SQL tests. Evidence: `target/phase02-fix-final-build.log` and Surefire reports.
- Ran the actual WAR on temporary port 18080: home HTTP 200 with layout; registration → local OTP activation → password login → shipment form and logout succeeded. With no pricing policy, quote returns a safe message without creating an order. Evidence: `target/phase02-fix-http.json`. Stopped the app and removed the exact test account/challenge/local OTP file afterward.
- All six touched business tables were empty before tests; fixture cleanup was verified after the run. No schema reset, shared-price replacement, provider setup or real payment occurred.
- Updated the existing private Eclipse launch to exclude test code and use `E:\Project_WEB`; ignored `.metadata/` in Git while preserving IDE workspace data. Manual Eclipse execution remains to be checked by the owner.
- Final cleanup check after the 151-test run: users, shipments, payments, pricing policies, OTP challenges and shipment events all returned to their initial zero-row counts. The temporary HTTP server is stopped; the final WAR excludes private `application-local.properties`.

### 2026-10-23 — Eclipse test-classpath shadowing diagnosed

- Owner's IDE launch returned `No static resource index`. Read-only inspection of its live JVM showed `target/test-classes` preceding `target/classes`; test configuration hid MVC view prefix/suffix.
- Corrected the private launch key from `org.eclipse.jdt.launching.EXCLUDE_TEST_CODE` to `org.eclipse.jdt.launching.ATTR_EXCLUDE_TEST_CODE`, verified against the installed JDT plugin. An open Eclipse may cache the launch; the owner must enable **Exclude test code** in its launch dialog and Apply before restarting the app.
- Reused the live classpath without test classes for an isolated local run: `/`, `/login`, `/register` returned HTTP 200 and home included its footer. Evidence: `target/eclipse-classpath-fix.log`. Stopped the temporary server. Actual rerun through the IDE remains pending; no business logic or DB data changed.

### 2026-10-23 — Shipment wizard and handover-wait persistence

- Continued at the owner's request without seeding UI pricing. Reused existing JSP/controller/service for four steps, backward navigation, reviewed summary, keyboard access, field-error focus and a full-form fallback without JavaScript.
- Stored the reviewed inputs/fee behind a random quote reference in the session. Final POST must match it; pickup method remains selectable at the last step. Service recalculates the price and rejects changes before persistence. Successful creation consumes the draft; repeated same-session submits cannot create another shipment from it. This is single-instance/session protection, not durable cross-session idempotency.
- Added required choice validation and DECIMAL(15,2)-compatible money bounds. Removed the redundant JSP source-substring test after replacing it with real Jasper/form-action evidence.
- Added nullable `waiting_for_handover_at` and the guarded `phase-02-handover-wait.sql` migration; applied and reran it safely on local `ute_express`. Cash creation starts the timer immediately; online creation starts it at payment confirmation; retries keep its original value. Existing rows are not guessed/backfilled. Clock uses the existing server-local operational time convention at second precision.
- Final `mvn -B package`: **171 tests, 0 failures/errors/skips**, including 11 SQL Server tests. Updated the previous tracer test for Clock injection and the quote-before-create flow. Evidence: `target/phase02-wizard-final-build.log`.
- Seven real HTTP/Jasper shipment tests exercise login/CSRF, quote/reference/create, escaping, errors and payment instructions. Ten headless Chrome interaction checks passed on those synthetic pages, including mobile/no-JS/hidden-error behavior. Desktop/mobile screenshots were inspected; see `target/shipment-wizard-desktop.png`, `target/shipment-wizard-mobile.png` and `target/check-shipment-wizard.mjs`.
- Post-test DB counts match the pre-test snapshot: one existing user/challenge retained, zero shipments/payments/pricing/events. Final WAR includes wizard JS and excludes private local properties. No Git commit/push or real-provider configuration performed.
- Test browser/server stopped. Automatic approval review rejected removal of `target/wizard-chrome-profile` with `blocked by policy`; the generated profile remains in ignored build output.
- Next: owner's Eclipse/UAT plus the agreed team prices; record payment-deadline duration, quote/service audit and hub/version contracts before implementing their dependent first-mile/timeout work. Full Phase 02 remains incomplete.
- User decision: use the team's pricing table, not sample prices. No pricing rows were seeded. Next: obtain that table and complete owner browser UAT, then resolve the remaining intake/wizard/hub/timestamp work. Providers remain deferred and Phase 02 remains incomplete.

### 2026-10-23 — First-mile, cancellation/timeout, refund outbox and tracking implemented

- Implemented `FirstMileService` (same-hub coordinator assignment, shipper failed attempts, scan-to-complete with one-time sender-cash collection, coordinator `PICKUP_SERVICE_FAILURE`), sender cancellation, `IntakeTimeoutService` (48 h from `assigned_at` / `waiting_for_handover_at`, demo minutes), `refund_settlement_outbox` (`PENDING_WALLET_CREDIT`, unique per payment), guest/API tracking with allowlisted timeline, sender list, recipient masked list/unlock, and post-commit STOMP notifications to sender/assigned shipper.
- New migration: `src/main/resources/db/phase-02-refund-outbox.sql`. Shipment columns used (`origin_hub_id`, `current_hub_id`, `picked_up_at`, `cancel_reason`) already exist in `docs/schema.sql`.
- Tests: 193 run, 0 failures/errors, 9 SQL Server tests skipped in an environment without SQL Server (container build, JDK 21). New unit, MVC-security and Jasper rendering tests cover the new paths. No SQL Server evidence yet for the new tables/locks.
- Defaults chosen without a leader decision (record or change them): origin hub is bound to the first coordinator who assigns; scheduler runs every 60 s in-process; automated events use a NULL actor; unlock uses the exact full tracking number per session (no OTP); ETA is shown as "chưa có ước tính"; SELF_DROPOFF counter acceptance stays with Phase 03.
- Next: apply the outbox migration, run the SQL suite, add SQL race tests (cancel vs pickup, two hubs), browser UAT per role, Person B/C reviews.

### 2026-10-23 — Demo pricing table seeded (needs team confirmation)

- Without an ACTIVE `pricing_policies` row the wizard cannot quote (step 3 error "Chưa có bảng giá cho dịch vụ và tuyến này"), so step 4 is unreachable on a fresh database.
- New seed: `src/main/resources/db/phase-02-pricing-seed.sql` (27 ACTIVE rows, idempotent, adds only missing combinations). Prices are a market-range reference (GHTK, GHN, Viettel Post, SPX), not an official tariff: 500 g base, 500 g steps, insurance 0.5 %, COD 0.8 %. Table is in `PHASE_02_ACCOUNT_SETUP.md` step 9.
- Known limits versus carriers: pricing uses actual weight only (no volumetric weight), no free-insurance threshold under 1M VND, no free-COD tier, no intra-province/ward split. Add them later only if the team wants them.
- Decision needed from the team: keep these demo prices or replace them with the official table.
