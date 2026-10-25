# UTEExpress Documentation Index

Tài liệu trong thư mục `docs/` là canonical context dùng cho cả nhóm khi phát triển **UTEExpress Logistic Management System**.

Các tài liệu này không phải tất cả kiến thức được phép sử dụng. Chúng là nguồn có thẩm quyền của project đối với những vấn đề mà chúng đã định nghĩa.

Supplementary engineering knowledge được phép sử dụng khi cần để triển khai project, nhưng không được mâu thuẫn hoặc override quyết định ở tầng authority cao hơn.

---

## 1. Authority Order

Khi có khác biệt giữa các tài liệu, sử dụng thứ tự ưu tiên sau:

```text
1. ARCHITECTURE_DECISIONS.md
2. UTEExpress_Workflow.md
3. ERD.md
4. PROJECT_SPEC_KNOWLEDGE_BASE.md
5. ENGINEERING_STANDARDS.md
6. Supplementary engineering knowledge
```

Không tự giải quyết conflict bằng suy đoán nếu các tài liệu có authority cao hơn chưa đưa ra quyết định.

---

## 2. Canonical Documents

### `ARCHITECTURE_DECISIONS.md`

Nguồn quyết định kiến trúc cao nhất của project.

Dùng để:

- giải quyết conflict giữa các tài liệu;
- ghi các quyết định đã được chấp nhận;
- xác định lựa chọn chính thức của UTEExpress;
- ngăn việc tự ý thay đổi một vấn đề đã được quyết định.

Nếu một ADR có trạng thái `ACCEPTED`, các tài liệu thấp hơn không được override quyết định đó.

---

### `UTEExpress_Workflow.md`

Ground Truth về **nghiệp vụ UTEExpress**.

Bao gồm:

- 6 canonical roles;
- RBAC và Data Scoping;
- Hub hierarchy;
- shipment lifecycle;
- pickup / self-dropoff;
- inbound / sorting / outbound;
- bag / container / handover;
- linehaul;
- last-mile delivery;
- return;
- COD;
- reconciliation;
- wallet;
- withdrawal;
- incident;
- routing;
- WebSocket events;
- business rules;
- database field-level specification.

Khi cần xác định hệ thống **phải làm gì**, đọc file này trước.

---

### `ERD.md`

Ground Truth về **logical data model và entity relationships**.

ERD dùng để hiểu:

- entity nào tồn tại;
- entity liên hệ với nhau như thế nào;
- module dữ liệu;
- cardinality;
- Hub hierarchy;
- shipment/tracking relationships;
- operations;
- financial relationships.

`ERD.md` KHÔNG phải full DDL.

Khi cần field-level chi tiết, ưu tiên phần Database trong `UTEExpress_Workflow.md`.

---

### `PROJECT_SPEC_KNOWLEDGE_BASE.md`

**Core Lecture Ground Truth** được tổng hợp từ các tài liệu bài giảng Lập trình Web.

Bao gồm các kiến thức/pattern như:

- Spring Boot;
- JSP/JSTL;
- Sitemesh;
- Spring Data JPA;
- DTO/Mapper;
- validation;
- pagination;
- upload;
- Spring Security;
- JWT;
- REST/AJAX;
- Maven/configuration;
- các source examples và source conflicts.

Tài liệu này:

```text
authoritative
but
not exhaustive
```

Các code example của bài giảng là bằng chứng về pattern từ source, nhưng không tự động trở thành UTEExpress domain rule.

`NOT_DEFINED_IN_SOURCE` chỉ có nghĩa:

```text
Lecture source does not define this topic.
```

Nó KHÔNG có nghĩa là project bị cấm triển khai topic đó.

---

### `ENGINEERING_STANDARDS.md`

Chuẩn tổ chức và chất lượng code của project.

Bao gồm:

```text
Controller
    ↓
Service Interface
    ↓
ServiceImpl
    ↓
Repository
    ↓
Database
```

cùng các rule về:

- separation of concerns;
- DTO boundary;
- constructor injection;
- validation;
- transaction;
- JPA/JDBC;
- exception handling;
- security;
- RBAC;
- Data Scoping;
- JSP/JSTL;
- Clean Code;
- testing considerations.

File này quy định **code nên được tổ chức như thế nào**, nhưng không được thay đổi nghiệp vụ được Workflow hoặc ADR quy định.

Các ví dụ generic như `Product`, `Order`, `ROLE_VENDOR` chỉ là:

```text
GENERIC EXAMPLE — NOT UTEEXPRESS DOMAIN TRUTH
```

---

### `TEAM_WORK_BREAKDOWN.md` & `TEAM_OWNERSHIP.md`

Quy định bảng phân công công việc chi tiết cho team 3 người.

Project KHÔNG chia theo:

```text
Frontend
Backend
Database
```

Mà chia theo **vertical slices**.

Mỗi thành viên phải đi xuyên kiến trúc:

```text
JSP / UI
   ↓
Controller
   ↓
DTO
   ↓
Service
   ↓
Repository / JPA
   ↓
Database
   ↓
Security / Data Scope
   ↓
WebSocket nếu applicable
   ↓
Tests
```

Roadmap định hướng:

```text
Phase 01 — Shared Walking Skeleton

Phase 02 — User & Shipment Intake

Phase 03 — Hub & Linehaul Operations

Phase 04 — Last-mile & Finance

Phase 05 — Cross-slice Integration

Phase 06 — Security + UAT + Demo Hardening
```

Phase 02/03/04 nên độc lập tối đa sau Phase 01 để team có thể làm song song bằng branch/worktree.

---

## 3. Canonical UTEExpress Domain Rules

Canonical roles:

```text
ROLE_USER
ROLE_SHIPPER
ROLE_HUB_STAFF
ROLE_LINEHAUL_DRIVER
ROLE_MANAGER
ROLE_ADMIN
```

`GUEST` không được lưu thành database role.

Canonical Shipment State Machine có đúng 13 trạng thái:

```text
CREATED
WAITING_FOR_PAYMENT
WAITING_FOR_HANDOVER
PICKED_UP
AT_HUB
IN_TRANSIT
OUT_FOR_DELIVERY
FAILED
DELIVERED
RETURN_TO_SENDER
RETURNED_TO_SENDER
CANCELLED
CLOSED_EXCEPTION
```

Operational actions như:

```text
SORTING
SORTED
PACKED
READY_FOR_OUTBOUND
ARRIVED_AT_HUB
```

không được tự biến thành `ShipmentStatus`.

Chúng được biểu diễn bằng operational records / `shipment_events.event_type` khi phù hợp.

---

## 4. Authorization Principle

Authorization của UTEExpress không chỉ dựa vào Role.

Tùy workflow phải xem xét:

```text
ROLE
+
HUB SCOPE
+
POSITION
+
TASK ASSIGNMENT
+
CURRENT SHIPMENT LOCATION
/
RESOURCE OWNERSHIP
```

Frontend visibility không thay thế backend authorization.

---

## 5. Supplementary Knowledge Policy

Nhóm phát triển MAY sử dụng kiến thức kỹ thuật bổ sung khi:

- project yêu cầu feature mà lecture chưa định nghĩa;
- canonical docs chưa mô tả đủ implementation detail;
- cần giải quyết vấn đề kỹ thuật implementation.

Ví dụ:

```text
WebSocket = required by Project Workflow
Lecture = NOT_DEFINED_IN_SOURCE

=> WebSocket vẫn được phép implement bằng supplementary knowledge.
```

Supplementary knowledge MUST NOT:

- thay đổi explicit business rule;
- thay đổi canonical roles;
- thay đổi Shipment State Machine;
- tự redesign database architecture;
- tự đổi technology stack;
- override ACCEPTED ADR;
- phát minh thêm business requirement.

---

## 6. Implementation Rules

Trước khi phát triển hoặc triển khai một phân hệ:

1. Đọc documentation liên quan.
2. Xác định ADR liên quan.
3. Xác định Workflow sections liên quan.
4. Xác định entities/relationships liên quan.
5. Áp dụng Lecture Ground Truth khi source định nghĩa pattern.
6. Áp dụng Engineering Standards.
7. Dùng supplementary knowledge cho khoảng trống nếu cần.

Nếu phát hiện conflict chưa có quyết định:

```text
DO NOT SILENTLY RESOLVE
```

Ghi conflict và yêu cầu Project Decision.

---

## 7. Planning Rule

Kế hoạch phát triển phải theo **vertical capability**, không theo technical layer.

Ưu tiên:

```text
user-visible capability
+
business workflow
+
end-to-end architecture
```

thay vì:

```text
all entities first
all repositories second
all controllers third
all UI last
```

Phân hệ hoàn thành phải có khả năng verify end-to-end khi phù hợp.

---

## 8. Purpose

Bộ tài liệu này tồn tại để đảm bảo:

```text
Business consistency
+
Architecture consistency
+
Lecture alignment
+
Team shared understanding
+
Team context stability
```

Mọi thành viên đều phải hiểu project như một hệ thống thống nhất, không chỉ phần code mà mình trực tiếp phụ trách.

## 9. Hướng dẫn thực hiện Phase 02 — Person A

- [Cấu hình và kiểm thử phần account](PHASE_02_ACCOUNT_SETUP.md): OTP điện thoại mô phỏng bằng `otp-local`, Google OAuth2/OIDC qua profile `google`, biến môi trường, migration và các bước chưa làm. Đăng nhập Google không cần SMTP.
- [Phạm vi Phase 02 bằng tiếng Việt](PHASE_02_SCOPE_VI.md): chín gói việc, phần đã có, trách nhiệm database, ranh giới với Phase 03/04 và các quyết định cần thống nhất.
- [Checklist thực hiện theo ngày](PHASE_02_DIRECTOR_CHECKLIST.md): tiến độ thực tế, điều kiện hoàn thành và nhật ký từng phiên.

Các tài liệu thực hiện này tuân theo authority order ở trên; không thay thế ADR, Workflow hoặc phê duyệt của người phụ trách nhóm.

Ngày 22/10/2026, người dùng yêu cầu triển khai account với hai lựa chọn **OTP điện thoại hoặc Google**, cấu hình provider sau; [ADR-016](ARCHITECTURE_DECISIONS.md#adr-016--customer-phone-otp-and-google-oidc-sign-in) ghi đúng nguồn quyết định này. Cập nhật 24/10: trên nhánh `feature/phase-02-intake`, `mvn test` chạy **197 test, 0 lỗi**; 9 SQL Server tests bị bỏ qua khi không bật biến môi trường. Lần chạy SQL gần nhất (23/10, 11 test đạt) là trước khi thêm first-mile/hủy/timeout/outbox, nên phần mới chưa có bằng chứng SQL Server. Đã có code first-mile, hủy đơn, timeout 48 giờ, outbox hoàn tiền, tracking công khai và mở khóa cho người nhận. Bảng giá demo đã nạp bằng `phase-02-pricing-seed.sql` (chờ nhóm xác nhận). Đăng ký → OTP local → đăng nhập và báo giá đã thử trên trình duyệt. Còn mở: bảng `refund_settlement_outbox` chưa áp dụng trên DB local, dữ liệu hub/nhân sự để UAT first-mile, UAT theo vai trò, test SQL cho phần mới, lưu mã dịch vụ và optimistic version, ETA, provider thật, review chéo; **chưa hoàn thành toàn Phase 02.**
