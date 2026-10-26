# Codebase Concerns

**Analysis Date:** 2026-10-01

## Tech Debt

**Bootstrap-only implementation:**
- Issue: Current application source contains a home controller, security configuration, JSP layout, and tests, but lacks the canonical domain entity, DTO, service, repository, and persistence implementations.
- Files: `src/main/java/vn/iotstar/`, `src/main/webapp/WEB-INF/views/`.
- Impact: The canonical shipment, wallet, routing, reconciliation, and operational workflows are not executable.
- Fix approach: Build vertical slices according to `docs/TEAM_OWNERSHIP.md` and the GSD roadmap, beginning with the Walking Skeleton.

## Known Bugs

**Linehaul role identifier mismatch:**
- Symptoms: `/driver/**` authorization uses `hasRole("DRIVER")`.
- Files: `src/main/java/vn/iotstar/config/SecurityConfig.java`.
- Trigger: A canonical `ROLE_LINEHAUL_DRIVER` user reaches driver endpoints after concrete authentication is implemented.
- Workaround: No runtime workaround is present; future implementation should align this bootstrap route rule with ADR-002.

## Security Considerations

**Authorization is route-level only:**
- Risk: Current configuration cannot enforce canonical hub scope, position, task assignment, shipment location, or resource ownership.
- Files: `src/main/java/vn/iotstar/config/SecurityConfig.java`.
- Current mitigation: Route-level Spring Security role checks and BCrypt encoder exist.
- Recommendations: Add service-layer contextual authorization with the domain slices, as required by ADR-015.

## Performance Bottlenecks

**Not established:**
- Problem: No domain persistence/query paths exist to measure.
- Files: `src/main/java/vn/iotstar/`.
- Improvement path: Introduce pagination and fetch strategies with repository queries as slices are implemented.

## Fragile Areas

**JSP navigation ahead of features:**
- Files: `src/main/webapp/WEB-INF/views/layout/header.jsp`, `src/main/webapp/WEB-INF/views/layout/sidebar.jsp`.
- Why fragile: Navigation references feature routes without corresponding controllers.
- Safe modification: Add routes and views as their vertical slice becomes functional; avoid treating menu links as implemented capability.
- Test coverage: Only the home routes have MVC tests.

## Scaling Limits

**Not established:**
- Current capacity: Static home page and security bootstrap only.
- Limit: No operational data path exists.
- Scaling path: Design against canonical hub and routing architecture rather than introducing alternate infrastructure.

## Dependencies at Risk

**Java 22:**
- Risk: Runtime compatibility must match the project Java version set in `pom.xml`.
- Impact: Builds will fail on unsupported local or deployment JDKs.
- Migration plan: Use JDK 22 consistently unless an explicit project decision changes it.

## Missing Critical Features

**Canonical vertical capabilities:**
- Problem: Authentication flows, shipment intake, operational workflows, last-mile/finance, persistence, WebSocket implementation, and database migration are not implemented.
- Blocks: End-to-end verification of all UTEExpress business workflows.

## Test Coverage Gaps

**Domain and security behavior:**
- What's not tested: Authentication, API authentication, authorization scope, persistence, workflow state transitions, and WebSocket events.
- Files: `src/main/java/vn/iotstar/config/SecurityConfig.java` and absent domain packages.
- Risk: Future vertical slices could regress canonical business rules without automated detection.
- Priority: High.

---
*Concerns audit: 2026-10-01*
