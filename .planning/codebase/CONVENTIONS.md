# Coding Conventions

**Analysis Date:** 2026-10-01

## Naming Patterns

**Files:**
- Java classes use PascalCase in `src/main/java/vn/iotstar/`, for example `HomeController.java`.
- JSP pages and fragments use lowercase names under `src/main/webapp/WEB-INF/views/`.

**Functions:**
- Controller methods use lower camel case, for example `index()` in `src/main/java/vn/iotstar/controller/HomeController.java`.

**Variables:**
- Local variables use lower camel case, for example the `model` parameter in `HomeController.index()`.

**Types:**
- Spring components are named by responsibility, for example `SecurityConfig` and `HomeController`.

## Code Style

**Formatting:**
- No dedicated formatter configuration detected.
- Java source uses four-space indentation and braces on the declaration line.

**Linting:**
- No linter configuration detected.

## Import Organization

**Order:**
1. Framework imports.
2. Project imports.
3. Static test imports in test classes.

**Path Aliases:**
- Java packages use `vn.iotstar`.

## Error Handling

**Patterns:**
- No global exception handler or domain exception pattern is present.
- Future implementation must follow `docs/ENGINEERING_STANDARDS.md` exception-boundary guidance.

## Logging

**Framework:**
- No explicit application logging calls detected.

**Patterns:**
- Logging conventions have not yet been established in source.

## Comments

**When to Comment:**
- Existing source documents architectural intent with JavaDoc in `HomeController.java` and `SecurityConfig.java`.

**Javadoc/TSDoc:**
- JavaDoc is used on classes and selected methods; TypeScript is not used.

## Function Design

**Size:**
- The current `HomeController.index()` endpoint is short and performs HTTP/view concerns only.

**Parameters:**
- MVC dependencies are method parameters, for example `Model`.

**Return Values:**
- MVC controller returns JSP logical view names.

## Module Design

**Exports:**
- Java classes are organized by package.

**Layering:**
- The current source has only bootstrap, controller, and configuration layers.
- New feature code must use the canonical Service Interface / ServiceImpl / Repository separation rather than placing business logic in controllers.

---
*Convention analysis: 2026-10-01*
