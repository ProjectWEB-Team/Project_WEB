# Codebase Structure

**Analysis Date:** 2026-10-01

## Directory Layout

```text
Project WEB/
├── docs/                         # Canonical domain, ADR, ERD, and engineering context
├── src/main/java/vn/iotstar/     # Spring Boot application source
│   ├── config/                   # Security configuration
│   └── controller/               # MVC controllers
├── src/main/resources/           # Application and demo configuration
├── src/main/webapp/              # JSP views and static assets
│   ├── WEB-INF/views/            # JSP pages and layout fragments
│   └── assets/css/               # UTEExpress stylesheet
├── src/test/java/vn/iotstar/     # Spring MVC tests
├── pom.xml                       # Maven build and dependency manifest
└── .planning/codebase/           # Generated codebase map
```

## Directory Purposes

**`docs/`:**
- Purpose: canonical product and engineering constraints.
- Key files: `docs/README.md`, `docs/ARCHITECTURE_DECISIONS.md`, and `docs/UTEExpress_Workflow.md`.

**`src/main/java/vn/iotstar/`:**
- Purpose: application bootstrap and Java implementation.
- Contains: `UteExpressApplication`, `config/`, and `controller/`.

**`src/main/webapp/WEB-INF/views/`:**
- Purpose: non-public JSP views rendered by Spring MVC.
- Contains: `index.jsp` and reusable layout fragments in `layout/`.

## Key File Locations

**Entry Points:**
- `src/main/java/vn/iotstar/UteExpressApplication.java`: Spring Boot and WAR initializer.
- `src/main/java/vn/iotstar/controller/HomeController.java`: home-page MVC endpoint.

**Configuration:**
- `pom.xml`: dependencies and packaging.
- `src/main/java/vn/iotstar/config/SecurityConfig.java`: security chains.
- `src/main/resources/application.properties`: runtime configuration.

**Core Logic:**
- Current controller logic is limited to `src/main/java/vn/iotstar/controller/HomeController.java`.

**Testing:**
- `src/test/java/vn/iotstar/controller/HomeControllerTest.java`: MockMvc controller tests.

## Naming Conventions

**Files:**
- Java classes use PascalCase, for example `HomeController.java` and `SecurityConfig.java`.
- JSP layout fragments use lowercase names, for example `layout/header.jsp`.

**Directories:**
- Java packages are lowercase, for example `vn.iotstar.controller`.

## Where to Add New Code

**New Feature:**
- Controller: `src/main/java/vn/iotstar/controller/`.
- Service interface: `src/main/java/vn/iotstar/service/`.
- Service implementation: `src/main/java/vn/iotstar/service/impl/`.
- Repository: `src/main/java/vn/iotstar/repository/`.
- Entity: `src/main/java/vn/iotstar/entity/`.
- DTO: `src/main/java/vn/iotstar/dto/`.
- JSP: `src/main/webapp/WEB-INF/views/`.
- Tests: `src/test/java/vn/iotstar/`.

**New Component/Module:**
- Follow the Controller → Service Interface → ServiceImpl → Repository → Database flow required by `docs/ENGINEERING_STANDARDS.md`.

## Special Directories

**`.planning/codebase/`:**
- Purpose: generated current-state codebase reference for GSD planning.
- Generated: Yes.
- Committed: not committed in this reset task.

---
*Structure analysis: 2026-10-01*
