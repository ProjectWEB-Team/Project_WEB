---
phase: 01-shared-walking-skeleton
plan: 01
subsystem: security
tags: [spring-security, jjwt, jwt, jsp-session, csrf, data-scope]
requires: []
provides:
  - Canonical six-role UserRole model and persisted user authentication baseline
  - Stateless JWT API login/filter path using JJWT 0.12.6
  - Session-backed JSP login with CSRF protection and server-derived AuthenticatedActor seam
affects: [01-02, 01-03, phase-02, phase-03, phase-04]
tech-stack:
  added: [jjwt-api-0.12.6, jjwt-impl-0.12.6, jjwt-jackson-0.12.6]
  patterns: [dual-security-filter-chains, constructor-injection, service-level-data-scope]
key-files:
  created:
    - src/main/java/vn/iotstar/security/AuthenticatedActor.java
    - src/main/java/vn/iotstar/security/JwtService.java
    - src/main/java/vn/iotstar/security/JwtAuthenticationFilter.java
    - src/main/java/vn/iotstar/controller/api/AuthApiController.java
  modified:
    - src/main/java/vn/iotstar/config/SecurityConfig.java
    - src/main/java/vn/iotstar/entity/UserRole.java
    - src/main/java/vn/iotstar/service/impl/AuthServiceImpl.java
key-decisions:
  - "Keep JSP routes session-backed with CSRF while /api/** is stateless JWT."
  - "Accept only the six canonical role values; legacy DRIVER is not an active role."
  - "Derive actor identity from Spring Security context, never from a caller-supplied user or hub id."
patterns-established:
  - "Controllers use DTO/service boundaries and do not access repositories directly."
  - "JWT configuration requires an environment-provided Base64 HS256 secret with at least 32 decoded bytes."
requirements-completed: [SKEL-02, SKEL-05]
status: complete
---

# Plan 01-01 Summary

**Canonical six-role authentication with separate JSP-session and JWT-API paths, plus a trusted service-level actor seam.**

## Accomplishments

- Added the six canonical roles: `ROLE_USER`, `ROLE_SHIPPER`, `ROLE_HUB_STAFF`, `ROLE_LINEHAUL_DRIVER`, `ROLE_MANAGER`, and `ROLE_ADMIN`.
- Implemented DTO-based API authentication, JJWT 0.12.6 token issuance/validation, and a filter that rejects missing, malformed, expired, and wrong-signature credentials on protected API routes.
- Preserved session/CSRF security for JSP routes and introduced `AuthenticatedActor` for later contextual authorization without trusting client identity fields.
- Hardened invalid-credential responses to a generic 401 result and kept production JWT secrets environment-only.

## Verification

- Targeted auth and security tests passed, including valid login, generic invalid credentials, protected endpoint access, token expiry/signature/malformed-token rejection, and CSRF protection.
- Current full-suite regression: `mvn test` completed successfully on 2026-10-07; the normal suite has 57 passing tests and one expected environment-gated SQL Server skip.
- Person A security review and the subsequent hardening re-review were approved before the shipment tracer proceeded.

## Deviations from Plan

Security hardening was added within Plan 01-01 scope: the default JWT secret was removed, secret validation was made fail-fast, login error behavior was normalized, and filter-level tests exercise the real JJWT validation path.

## Next Phase Readiness

Plan 01-02 can consume the canonical user/role model and `AuthenticatedActor` ownership seam. No Phase 02–04 business workflow was introduced.

---
*Retrospective execution record created from approved reviews, current code, and UAT evidence on 2026-10-07. Manual Git checkpoints are intentionally not reproduced here.*
