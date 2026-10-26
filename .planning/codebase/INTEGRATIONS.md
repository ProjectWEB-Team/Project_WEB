# External Integrations

**Analysis Date:** 2026-10-01

## APIs & External Services

**Realtime:**
- Spring WebSocket/STOMP capability is declared in `pom.xml`.
  - SDK/Client: `spring-boot-starter-websocket`.
  - Auth: WebSocket authorization implementation is not present yet.

**Email:**
- Spring Mail capability is declared in `pom.xml` for planned OTP email verification.
  - SDK/Client: `spring-boot-starter-mail`.
  - Auth: configuration is externalized in `src/main/resources/application.properties`.

**API documentation:**
- Springdoc OpenAPI UI is declared in `pom.xml`.
  - SDK/Client: `springdoc-openapi-starter-webmvc-ui`.
  - Auth: not configured in current source.

## Data Storage

**Databases:**
- SQL Server is the project decision (ADR-010).
  - Connection: application property files under `src/main/resources/`.
  - Client: Spring Data JPA dependency in `pom.xml`.

**File Storage:**
- No application file-storage implementation is present.

**Caching:**
- None detected.

## Authentication & Identity

**Auth Provider:**
- Custom Spring Security foundation.
  - Implementation: `src/main/java/vn/iotstar/config/SecurityConfig.java` configures a stateless `/api/**` chain and a session-based JSP/web chain.

## Monitoring & Observability

**Error Tracking:**
- None detected.

**Logs:**
- No explicit logging framework usage is present in the application source.

## CI/CD & Deployment

**Hosting:**
- Not detected.

**CI Pipeline:**
- Not detected.

## Environment Configuration

**Required env vars:**
- Not detected; configuration files exist under `src/main/resources/` but values are not inspected here.

**Secrets location:**
- Environment-sensitive values may be held in application property files; no secret values are documented in this map.

## Webhooks & Callbacks

**Incoming:**
- None implemented.

**Outgoing:**
- None implemented.

---
*Integration audit: 2026-10-01*
