---
phase: 00-base-skeleton-database-setup
plan: "01"
subsystem: foundation
tags:
  - spring-boot
  - java-22
  - jsp-jakarta
  - jstl-3.0
  - spring-security
  - sql-server
  - bootstrap-5
  - design-system

requires: []
provides:
  - "Spring Boot 3.3.4 / 4.x Maven project structure compiling cleanly on JDK 22"
  - "Eliminated Thymeleaf and configured standard JSP/JSTL 3.0 Jakarta with Tomcat Embed Jasper"
  - "SQL Server ute_express database connection with spring.jpa.hibernate.ddl-auto=none"
  - "Dual SecurityFilterChain: stateless REST API (/api/**) and stateful session Web SSR (/**) with BCrypt"
  - "Master layout JSP fragments (header, sidebar, footer) and ute-express.css with HCMUTE brand palette"
  - "High-contrast 13 ShipmentStatus badge utility styles and sample index.jsp view"
affects:
  - "Phase 1: Vertical Core Entities & UI Layouts"
  - "Phase 2: Business Services & Controller Workflows"
  - "Phase 3: Advanced Integration, Realtime & Reconciliation"
  - "Phase 4: Cross-Testing, Seed Data & Final Packaging"

actuals:
  tasks: 4
  commits: 4

tech-stack:
  added:
    - "Java 22 (JDK 22, bytecode release 22)"
    - "Spring Boot Starter Web, JPA, Security, Validation, Mail, WebSocket"
    - "Microsoft SQL Server JDBC Driver (mssql-jdbc)"
    - "Tomcat Embed Jasper & Jakarta JSTL 3.0 (jakarta.servlet.jsp.jstl-api:3.0.0, jakarta.servlet.jsp.jstl:3.0.1)"
    - "Bootstrap 5.3.3, Bootstrap Icons 1.11.3, FontAwesome 6.5.1"
  patterns:
    - "3-Tier Clean Architecture: Controller -> Service -> Repository"
    - "Dual SecurityFilterChain separation: stateless REST vs stateful session SSR"
    - "PRG Pattern readiness and Server-side form validation ergonomics"
    - "Zero-scriptlet JSP views using JSTL 3.0 Jakarta & EL ${}"

key-files:
  created:
    - pom.xml
    - src/main/java/vn/iotstar/UteExpressApplication.java
    - src/main/resources/application.properties
    - src/main/resources/application-demo.properties
    - src/main/java/vn/iotstar/config/SecurityConfig.java
    - src/main/java/vn/iotstar/controller/HomeController.java
    - src/main/webapp/assets/css/ute-express.css
    - src/main/webapp/WEB-INF/views/layout/header.jsp
    - src/main/webapp/WEB-INF/views/layout/sidebar.jsp
    - src/main/webapp/WEB-INF/views/layout/footer.jsp
    - src/main/webapp/WEB-INF/views/index.jsp
    - src/test/java/vn/iotstar/controller/HomeControllerTest.java
  modified:
    - .gitignore

key-decisions:
  - "Eliminated Thymeleaf in strict compliance with docs/rule.md line 4496; standardized on 100% JSP & JSTL 3.0 Jakarta."
  - "Standardized base package on vn.iotstar to align with HCMUTE Web Programming course standards."
  - "Configured spring.jpa.hibernate.ddl-auto=none and open-in-view=false to protect the 25 physical tables in docs/schema.sql."
  - "Implemented dual SecurityFilterChain with @Order(1) for /api/** (stateless) and @Order(2) for /** (stateful session with BCrypt)."
  - "Standardized brand color palette: Primary Deep Cobalt Blue #0F3D78, Accent Safety Orange #F26522, Canvas Industrial Gray #F1F5F9."

patterns-established:
  - "JSP Layout Inclusion: <jsp:include page='layout/header.jsp' /> and <jsp:include page='layout/footer.jsp' />."
  - "13 ShipmentStatus badge styles with high contrast for warehouse and shipper operations."
  - "Dual Validation and CSRF token propagation in all forms."

requirements-completed:
  - TASK-001
  - TASK-002
  - TASK-003
  - TASK-004

coverage:
  - id: D1
    description: "Maven project builds cleanly on JDK 22 with Spring Boot starter parent and dependencies"
    requirement: "TASK-001"
    verification:
      - kind: unit
        ref: "mvn clean compile"
        status: pass
    human_judgment: false
  - id: D2
    description: "SQL Server connection and demo timeout properties configured with ddl-auto=none"
    requirement: "TASK-002"
    verification:
      - kind: unit
        ref: "mvn process-resources"
        status: pass
    human_judgment: false
  - id: D3
    description: "Dual SecurityFilterChain configured with stateless REST and stateful session Web SSR with BCrypt"
    requirement: "TASK-003"
    verification:
      - kind: unit
        ref: "vn.iotstar.controller.HomeControllerTest"
        status: pass
    human_judgment: false
  - id: D4
    description: "Master layout JSP fragments, HCMUTE Logistics CSS, and index view render without scriptlets"
    requirement: "TASK-004"
    verification:
      - kind: unit
        ref: "vn.iotstar.controller.HomeControllerTest"
        status: pass
    human_judgment: false

status: complete
---

# Phase 0: Base Skeleton & Database Setup Summary

**Thiết lập thành công khung dự án Maven Spring Boot trên JDK 22, loại bỏ Thymeleaf chuyển sang 100% JSP & JSTL 3.0 Jakarta, cấu hình kết nối CSDL SQL Server 25 bảng với `ddl-auto=none`, bảo mật kép Dual SecurityFilterChain và bộ Master Layout chuẩn nhận diện Logistics HCMUTE.**

## Performance

- **Tasks:** 4/4 completed
- **Files created/modified:** 13 files
- **Automated Tests:** 2/2 passed (`vn.iotstar.controller.HomeControllerTest`)
- **Build Status:** `BUILD SUCCESS` (JDK 22, Maven 3.9.11)

## Accomplishments

1. **Khởi tạo Nền tảng Spring Boot & Java 22 (TASK-001):**
   - Cấu hình `pom.xml` với `vn.iotstar:project-web:1.0`, `<java.version>22</java.version>`.
   - Loại bỏ hoàn toàn Thymeleaf theo quy chuẩn bắt buộc của `docs/rule.md`, tích hợp `tomcat-embed-jasper` và `jakarta.servlet.jsp.jstl:3.0.0`.
   - Tạo class `UteExpressApplication` kế thừa `SpringBootServletInitializer` hỗ trợ runtime JSP nhúng.

2. **Cấu hình Kết nối CSDL SQL Server 2019+ (TASK-002):**
   - Thiết lập `application.properties` kết nối database `ute_express`.
   - Cấu hình bất biến `spring.jpa.hibernate.ddl-auto=none` bảo toàn cấu trúc 25 bảng vật lý trong `docs/schema.sql`.
   - Cấu hình `open-in-view=false`, cookie session an toàn `http-only=true, same-site=lax`, và file cấu hình demo `application-demo.properties` (timeout 2 phút).

3. **Cấu hình Dual SecurityFilterChain & BCrypt (TASK-003):**
   - `@Order(1)` matcher `/api/**`: Stateless, disable CSRF, dành cho Mobile Shipper & External API.
   - `@Order(2)` matcher `/**`: Stateful Session Cookie, enable CSRF, Form Login, Logout cookie cleanup, phân quyền 6 Roles.
   - Bean `BCryptPasswordEncoder` mã hóa mật khẩu một chiều đạt chuẩn LECTURE_STANDARDS.md.

4. **Master Layout JSP Fragments & CSS HCMUTE Logistics (TASK-004):**
   - `ute-express.css`: Bảng màu chuẩn nhận diện HCMUTE Primary Deep Cobalt Blue `#0F3D78`, Accent Safety Orange `#F26522`, Canvas Industrial Gray `#F1F5F9`.
   - Định nghĩa lớp CSS cho toàn bộ 13 Status Badges với độ tương phản cao cho kho vận và shipper.
   - 5 file view JSP (`header.jsp`, `sidebar.jsp`, `footer.jsp`, `index.jsp`) 100% không scriptlet Java, dùng JSTL 3.0 Jakarta.
   - `HomeController` điều hướng `/` và `/home` về `index.jsp` có test tự động kiểm chứng.

## Task Commits

Tất cả các task đều được commit nguyên tử (Atomic Commits):

1. **Task 1 (TASK-001):** `a28677e: feat(core): initialize maven pom with spring boot and java 22 skeleton`
2. **Task 2 (TASK-002):** `733a929: feat(db): configure sql server ute_express connection with ddl-auto none`
3. **Task 3 (TASK-003):** `12792ba: feat(security): implement dual security filter chain and bcrypt`
4. **Task 4 (TASK-004):** `5ad0c2b: feat(ui): create master layout fragments and hcmute logistics css`
