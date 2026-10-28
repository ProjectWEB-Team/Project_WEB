<!-- refreshed: 2026-10-01 -->
# Architecture

**Analysis Date:** 2026-10-01

## System Overview

```text
Browser / JSP
    |
    v
Spring Security filter chains
    |
    v
Spring MVC controllers
    |
    v
Service / Repository / JPA layers (planned; not yet present)
    |
    v
SQL Server (canonical target)
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| Application bootstrap | Starts Spring Boot and supports WAR deployment | `src/main/java/vn/iotstar/UteExpressApplication.java` |
| Home controller | Handles `/` and `/home`, adds view model values, returns JSP view | `src/main/java/vn/iotstar/controller/HomeController.java` |
| Security config | Defines API and web security chains plus BCrypt encoder | `src/main/java/vn/iotstar/config/SecurityConfig.java` |
| JSP views | Renders landing page and layout fragments | `src/main/webapp/WEB-INF/views/` |

## Pattern Overview

**Overall:** Spring Boot monolith with server-rendered JSP presentation and an intended layered architecture.

**Key Characteristics:**
- Existing code has an MVC entry controller and JSP layout fragments.
- The Service, Repository, Entity, DTO, and database implementations required by canonical docs have not yet been created.
- `SecurityConfig` already establishes distinct API and web request paths.

## Layers

**Presentation:**
- Purpose: accept web requests and render JSP views.
- Location: `src/main/java/vn/iotstar/controller/` and `src/main/webapp/WEB-INF/views/`.
- Depends on: Spring MVC and Spring Security.
- Used by: browser requests.

**Security:**
- Purpose: route-level access control and authentication foundations.
- Location: `src/main/java/vn/iotstar/config/SecurityConfig.java`.
- Depends on: Spring Security.
- Used by: all web and API requests.

**Domain and persistence:**
- Purpose: canonical Workflow requires DTO, Service, Repository/JPA, and SQL Server layers.
- Location: not yet implemented in `src/main/java/vn/iotstar/`.
- Used by: future feature controllers.

## Data Flow

### Current home-page request path

1. Browser requests `/` or `/home`.
2. `SecurityConfig` applies the web security chain in `src/main/java/vn/iotstar/config/SecurityConfig.java`.
3. `HomeController.index()` in `src/main/java/vn/iotstar/controller/HomeController.java` adds model attributes and returns `index`.
4. `src/main/webapp/WEB-INF/views/index.jsp` renders the page and includes layout fragments.

**State Management:**
- The web chain permits session creation; no application domain state is persisted yet.

## Key Abstractions

**Dual security chains:**
- Purpose: separates stateless `/api/**` traffic from session-backed JSP/web traffic.
- Examples: `src/main/java/vn/iotstar/config/SecurityConfig.java`.
- Pattern: ordered `SecurityFilterChain` beans.

## Entry Points

**Application:**
- Location: `src/main/java/vn/iotstar/UteExpressApplication.java`.
- Triggers: Java/Spring Boot startup or WAR container startup.
- Responsibilities: initializes the Spring application context.

**Home page:**
- Location: `src/main/java/vn/iotstar/controller/HomeController.java`.
- Triggers: GET `/` and `/home`.
- Responsibilities: returns the `index` JSP view.

## Architectural Constraints

- **Database:** SQL Server is canonical under ADR-010.
- **Frontend:** JSP/JSTL with Bootstrap is canonical under ADR-007.
- **Authorization:** canonical docs require role plus hub scope, position, assignment, and resource/location checks where applicable.
- **Shipment lifecycle:** future code must preserve the 13-status model and event/state separation.

## Anti-Patterns

### Role identifier drift

**What happens:** `SecurityConfig` uses `hasRole("DRIVER")` for `/driver/**`.
**Why it's wrong:** canonical role naming is `ROLE_LINEHAUL_DRIVER`, not `ROLE_DRIVER`.
**Do this instead:** Phase planning should align the configured role identifier with ADR-002 and the Workflow without creating a new role.

## Error Handling

**Strategy:** No application exception handler is present.

**Patterns:**
- `HomeController` has no error path because it only returns a static view.
- Canonical engineering standards require future exception handling at an appropriate boundary.

## Cross-Cutting Concerns

**Logging:** No explicit application logging calls detected.
**Validation:** Validation dependency is declared but no request DTO validation is implemented.
**Authentication:** Security chains are configured; concrete users, JWT authentication, and authorization services are not implemented.

---
*Architecture analysis: 2026-10-01*
