# Testing Patterns

**Analysis Date:** 2026-10-01

## Test Framework

**Runner:**
- JUnit 5 through `spring-boot-starter-test` in `pom.xml`.
- Spring MVC slice tests through `@WebMvcTest`.

**Assertion Library:**
- Spring MockMvc result matchers.

**Run Commands:**
```bash
mvn test                 # Run tests
mvn -Dtest=HomeControllerTest test  # Run the current controller test class
mvn verify               # Run Maven verification lifecycle
```

## Test File Organization

**Location:**
- Tests mirror the Java package structure under `src/test/java/vn/iotstar/`.

**Naming:**
- Test classes end in `Test`, for example `HomeControllerTest.java`.

**Structure:**
```text
src/test/java/vn/iotstar/controller/HomeControllerTest.java
```

## Test Structure

**Suite Organization:**
```java
@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class HomeControllerTest {
    @Autowired
    private MockMvc mockMvc;
}
```

**Patterns:**
- Exercise endpoints with MockMvc `get()`.
- Assert status, returned view name, and model attributes.
- Import security configuration when testing protected request pipelines.

## Mocking

**Framework:**
- No mock framework usage is present in current tests.

**Patterns:**
- Current controller has no service dependency to mock.

**What to Mock:**
- Future controller tests should mock service interfaces and retain real MVC/validation behavior.

**What NOT to Mock:**
- Do not mock the controller's contract assertions or security access rules being tested.

## Fixtures and Factories

**Test Data:**
- No fixture or factory pattern exists yet.

**Location:**
- Test properties live at `src/test/resources/application.properties`.

## Coverage

**Requirements:**
- No coverage threshold or coverage tool detected.

**View Coverage:**
```bash
mvn test
```

## Test Types

**Unit Tests:**
- MVC slice coverage is implemented in `src/test/java/vn/iotstar/controller/HomeControllerTest.java`.

**Integration Tests:**
- No persistence or full-context integration test is present.

**E2E Tests:**
- Not used.

## Common Patterns

**HTTP Testing:**
```java
mockMvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(view().name("index"));
```

**Error Testing:**
- No controller error-path test pattern is present yet.

---
*Testing analysis: 2026-10-01*
