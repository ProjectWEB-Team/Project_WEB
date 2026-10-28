# Technology Stack

**Analysis Date:** 2026-10-01

## Languages

**Primary:**
- Java 22 — configured by `pom.xml` and used by application code under `src/main/java/vn/iotstar/`.

**Secondary:**
- JSP/JSTL — server-rendered views in `src/main/webapp/WEB-INF/views/`.
- CSS — UTEExpress design styles in `src/main/webapp/assets/css/ute-express.css`.

## Runtime

**Environment:**
- Spring Boot 3.3.4, packaged as a WAR by `pom.xml`.
- Embedded Tomcat/JSP support through `tomcat-embed-jasper`.

**Package Manager:**
- Maven — `pom.xml` is the dependency and build manifest.
- Lockfile: not detected.

## Frameworks

**Core:**
- Spring MVC — `HomeController` in `src/main/java/vn/iotstar/controller/HomeController.java`.
- Spring Data JPA — dependency declared in `pom.xml`; domain repositories are not yet present.
- Spring Security — dual `SecurityFilterChain` setup in `src/main/java/vn/iotstar/config/SecurityConfig.java`.
- JSP/JSTL — views in `src/main/webapp/WEB-INF/views/`.

**Testing:**
- JUnit 5, Spring Boot Test, MockMvc, and Spring Security Test — dependencies in `pom.xml`, used in `src/test/java/vn/iotstar/controller/HomeControllerTest.java`.

**Build/Dev:**
- Maven Spring Boot plugin and Spring Boot DevTools — `pom.xml`.

## Key Dependencies

**Critical:**
- `spring-boot-starter-web` — MVC web application support.
- `spring-boot-starter-data-jpa` — planned persistence layer.
- `spring-boot-starter-security` — web and API access control foundation.
- `mssql-jdbc` — SQL Server driver.
- `spring-boot-starter-websocket` — WebSocket/STOMP capability required by canonical Workflow.

**Infrastructure:**
- `spring-boot-starter-validation` — Bean Validation capability.
- `spring-boot-starter-mail` — future OTP email capability.
- Jakarta JSTL API/implementation and Jasper — JSP rendering.
- Springdoc OpenAPI — API documentation capability.

## Configuration

**Environment:**
- Application configuration files exist at `src/main/resources/application.properties`, `src/main/resources/application-demo.properties`, and `src/test/resources/application.properties`.
- Their contents are not reproduced in this map because configuration can contain environment-sensitive values.

**Build:**
- Build configuration is centralized in `pom.xml`.

## Platform Requirements

**Development:**
- JDK 22 and Maven.
- SQL Server is the canonical project database per ADR-010.

**Production:**
- WAR-capable Spring Boot deployment with SQL Server connectivity and JSP support.

---
*Stack analysis: 2026-10-01*
