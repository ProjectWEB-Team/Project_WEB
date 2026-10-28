---
status: resolved
trigger: "Registration still cannot send OTP. User requests autonomous end-to-end testing and fixes until an account can be created."
created: 2026-10-25
updated: 2026-10-25
---

## Symptoms

- Expected: POST /register creates pending local customer, delivers OTP into local inbox, verification activates account and password login works.
- Actual: generic Vietnamese send failure on /register, including after enabling both profiles.
- Error: "Không thể gửi mã. Kiểm tra thông tin hoặc thử lại sau."
- Runtime: jps -ml now shows PID 18592, vn.iotstar.UteExpressApplication, --spring.profiles.active=local,otp-local.
- HTTP GET http://localhost:8080/register returns 200 with form/CSRF.
- User authorizes testing and fixing local application/database until registration succeeds. Keep all secrets private.
- Earlier lack of otp-local was established but is now corrected; do not repeat that diagnosis without live evidence.
- Native/browser GUI tools were previously blocked by approval service 403 SHOP_MODEL_UNAVAILABLE. HTTP via PowerShell works in default sandbox in this turn; no GUI needed to test MVC/CSRF/session workflow.

## Current Focus

hypothesis: Confirmed missing account/OTP migration in application database; applying the ordered migrations resolved actual-database registration.
next_action: None. Root verified complete real HTTP workflow and actual-database account persistence, stopped the owned temporary process, and will confirm port8080 clear for Spring Tools.

## Evidence

- jps -ml filtered runtime profiles confirms local,otp-local; GET /register HTTP200 on current process.
- timestamp: 2026-10-25; root HTTP reproduction with generated valid unique identity returned HTTP500 on POST /register.
- timestamp: 2026-10-25; root JDBC probe against configured application database ute_express found base users columns only (10), missing phone_verified_at/email_verified_at/google_subject/version and missing phone_otp_challenges (SQL208).
- timestamp: 2026-10-25; source investigator is validating account migration existing-user path; no speculative code change has been applied.
- timestamp: 2026-10-25; source investigator completed review: UserRepository.findByPhoneForUpdate selects missing account fields before save/delivery. phase-02-account-otp.sql transactionally adds required structures, validates normalization/collisions, and never activates or verifies legacy accounts. Focused RegistrationServiceImplTest, RegistrationControllerTest and VietnamPhoneNormalizerTest: 33 passed, 0 failures/errors/skips. No source fix indicated.
- timestamp: 2026-10-25; root preflight found 26 base tables, 10 users requiring phone/email canonicalization, zero normalized identity conflict groups, zero invalid normalized phones, 6 shipments, and zero routes/container/handover/trip/payment/COD/wallet rows. Application SQL login has neither db_owner nor ALTER permission.
- timestamp: 2026-10-25; root created .local/repair-local-app-schema.ps1 bounded to ute_express using integrated Windows auth, guarded identity normalization, identity snapshot without passwords and all 16 manifest migrations inside an outer transaction. Windows SSPI failed from sandbox; elevated read-only admin probe was not executed due automatic-review HTTP403 SHOP_MODEL_UNAVAILABLE. User execution requested; no permission grant or reset performed.
- timestamp: 2026-10-25; read-only source investigator review of repair script found required filtered-index SET options missing (notably QUOTED_IDENTIFIER), immediately reported to root before Apply. Recommended ANSI_NULLS/QUOTED_IDENTIFIER/ANSI_PADDING/ANSI_WARNINGS/CONCAT_NULL_YIELDS_NULL/ARITHABORT ON and NUMERIC_ROUNDABORT OFF; optional UTF8 snapshot. All 16 manifest scripts retain database target; GO/nested transaction/rollback/balance design reviewed as sound. No script edits or execution by investigator.
- timestamp: 2026-10-25; root fixed sqlcmd incompatible -h/-y0 options reported during user execution, changed snapshot to UTF8 wide raw then parsed JSON, and added required filtered-index SET options plus sqlcmd -I. Added Prepare mode for exact SQL inspection.
- timestamp: 2026-10-25; root .local/ValidateSchemaRepair.java ran exact prepared 16-script SQL on existing dedicated test database with only target guard adapted in memory, three legacy normalization examples (0/84/+84 phones and uppercase email), and final outer COMMIT replaced with ROLLBACK. Assertions passed for canonicalization, original account-state preservation and no persisted fixtures. Application migration still awaits user Apply; STS app currently stopped.
- timestamp: 2026-10-25; root live HTTP E2E on isolated localhost:18080 against migrated dedicated SQL test database used actual local-inbox adapter (not CapturingDelivery). .local/registration-smoke.ps1 returned PASS for generated user codexreg_4b40c9a0d8c3: OTPDelivered=true, OTPVerified=true, PasswordLogin=true, protected /user/shipments HTTP200. This proves same-code full workflow works when schema is ready. Application database ute_express still lacks OTP table and awaits user Windows-authenticated Apply; session is NOT resolved.
- timestamp: 2026-10-25; root stopped the owned isolated port18080 process via its exec session after verification. No appsource edits. Main database repair still awaits user rerun of corrected Apply; app SQL login has no ALTER, sandbox SSPI fails and approval-service HTTP403 prevents admin probe. This is an external administrative action checkpoint, not a resolved registration failure.
- timestamp: 2026-10-25; user successfully executed corrected Apply: all 16 migration scripts committed. Root independently verified actual ute_express now contains phone_otp_challenges and the three required account columns, with rows_to_normalize=0. Prior administrative blocker is resolved. App on8080 currently stopped; root owns starting temporary actual-database application and final HTTP/JDBC verification.
- timestamp: 2026-10-25T16:22:12Z; final actual-database E2E PASS. Root started temporary application on localhost8080 with configured ute_express and ran .local/registration-smoke.ps1. Generated account codexreg_283deecdc27d: OTPDelivered=true, OTPVerified=true, PasswordLogin=true, protected page HTTP200. This used actual local-inbox delivery, CSRF, HTTP session and password authentication, not a capture/mock adapter.
- timestamp: 2026-10-25; independent JDBC verified DB_NAME=ute_express, generated account id173 ROLE_USER ACTIVE, phone_verified_at=2026-10-25T16:22:12Z, exactly one OTP challenge and consumed_at=2026-10-25T16:22:12.2968176Z. rows_to_normalize remains0. Root saved sanitized docs/REGISTRATION_VERIFICATION.md, then stopped owned temporary app through execsession18182 so Spring Tools can reuse8080. No source-code fix or commit required.

## Eliminated

- Missing otp-local command argument is not sufficient to explain current failure.

## Resolution

root_cause: Application database ute_express was not migrated for account/OTP schema; successful SQL tests used a separately migrated dedicated database.
fix: Existing ordered 16-script migration repair applied and committed to actual application database, including guarded canonicalization. No production source change indicated.
verification: Focused source tests33passed; exact 16-script repair validation passed transactionally with rollback; isolated local-inbox HTTP flow passed on migrated testDB; complete real HTTP register/OTP-inbox/verify/password-login/protected200 flow passed on actual ute_express at8080. Independent JDBC confirmed generated account173 ROLE_USER ACTIVE, phone verified, exactly one consumed challenge and zero remaining noncanonical identities. Root stopped temporary application for user's STS.
files_changed: Local repair/smoke/validation helpers owned by root; docs/REGISTRATION_VERIFICATION.md; resolved debug checkpoint. No production source edits or commits.

## Specialist Review

Hint general maps to engineering:debug, which is not available in the current skill catalog. Source investigation and concrete JDBC evidence independently agree on applying existing migration; no speculative implementation change is proposed.

## Blameless Postmortem

why_not_caught: SQL integration tests correctly used a separately migrated dedicated test database; no deployment gate compared the configured application database schema before browser UAT. Generic registration feedback and missing runtime logs initially obscured the database error.
guard: The bounded repair script preflight/Prepare mode and real registration smoke script verify the selected actual database. Sanitized docs/REGISTRATION_VERIFICATION.md records runtime profiles, target identity, HTTP workflow and persisted-account evidence. Future local UAT should verify actual-database migrations before starting STS.
