# UTEEXPRESS — DEVELOPMENT ROADMAP & TASK GRAPH
## Kế hoạch Thực thi Dự án 5 Phase Phân bổ Theo 3 Phân hệ Dọc (Full-Stack)
**Framework Quản phối:** GSD Core on Antigravity  
**Chiến lược Thực thi:** Vertical Slicing Full-Stack + Atomic Commit Discipline  
**Phân bổ Nhân sự:**
- **[TV1] Thành viên 1 (Team Lead):** Phân hệ Vận hành Bưu cục & Trung chuyển Liên Hub (9 Entities)
- **[TV2] Thành viên 2:** Phân hệ Cổng Khách hàng, Hiện trường & Realtime (7 Entities)
- **[TV3] Thành viên 3:** Phân hệ Quản trị Mạng lưới, Báo cáo & Tài chính Đối soát (9 Entities)

---

## TỔNG QUAN 5 PHASE THỰC THI

```text
[Phase 0: Base Skeleton & DB Setup] (Team Lead khởi tạo nền móng chung)
  │
  ├───────────────────────────────┬───────────────────────────────┐
  ↓                               ↓                               ↓
[Phase 1: Vertical Core]        [Phase 1: Vertical Core]        [Phase 1: Vertical Core]
  TV1: 9 Entities/Repos           TV2: 7 Entities/Repos           TV3: 9 Entities/Repos
  TV1: Hub & Driver JSP           TV2: Customer & Shipper JSP     TV3: Admin Layout & Dashboard
  │                               │                               │
  ├───────────────────────────────┼───────────────────────────────┤
  ↓                               ↓                               ↓
[Phase 2: Business Services]    [Phase 2: Business Services]    [Phase 2: Business Services]
  TV1: Inbound/Route/Handover     TV2: Auth/Shipment/Fast-Fail    TV3: Hub Tree/Pricing/Reconcile
  │                               │                               │
  ├───────────────────────────────┼───────────────────────────────┤
  ↓                               ↓                               ↓
[Phase 3: Advanced Integration] [Phase 3: Advanced Integration] [Phase 3: Advanced Integration]
  TV1: Unbagging & Incidents      TV2: STOMP WebSocket & JWT     TV3: E-Wallet Ledger & Payout
  │                               │                               │
  └───────────────────────────────┴───────────────────────────────┘
                                  │
                                  ↓
       [Phase 4: Cross-Testing, Seed Data 8 Scenarios & Final Packaging]
                     (Cả 3 thành viên chạy kiểm thử tích hợp)
```

---

## KỶ LUẬT ATOMIC COMMIT BẮT BUỘC (MỌI TASK)
Mọi thành viên và Subagent khi hoàn thành một đơn vị công việc **bắt buộc commit ngay lập tức**; nghiêm cấm dồn nhiều tính năng hoặc làm xong cả Phase mới commit:
1. Xong 1 Entity + Repository -> Verify mapping SQL Server đạt -> `feat(entity): create {Name} entity and repository`
2. Xong 1 Service Interface + ServiceImpl -> Chạy Unit test đạt -> `feat(service): implement {Name} business logic`
3. Xong 1 Controller + Giao diện JSP -> Verify hiển thị trình duyệt đạt -> `feat(controller): add {Name} view and controller`
4. Sửa lỗi logic/database -> Verify đạt -> `fix(scope): fix {issue}`
5. Tối ưu mã nguồn chuẩn Clean Code -> `refactor(scope): optimize {Name}`

---

## PHASE 0: BASE SKELETON & DATABASE SETUP
* **Mục tiêu:** Thiết lập khung dự án Maven Spring Boot 4.x với Java 22 (JDK 22), kết nối SQL Server, cấu hình bảo mật kép Dual SecurityFilterChain, chuẩn bị master layout Bootstrap 5 và kiểm thử kết nối CSDL 25 bảng.
* **Trạng thái:** `HOÀN THÀNH (COMPLETE)` (Đã thẩm định kiểm thử tự động đạt 100%)
* **Người thực hiện chính:** `[TV1 - Team Lead]` (Hỗ trợ: `[TV2]`, `[TV3]` review)

### Danh sách Task Phase 0:
* [x] `TASK-001` `[TV1]` **Khởi tạo Maven Project Spring Boot 4.x & Java 22:**
  - Thiết lập `pom.xml` với `spring-boot-starter-parent`, `<java.version>22</java.version>`, loại bỏ Thymeleaf, tích hợp Tomcat Embed Jasper & JSTL 3.0 Jakarta (`jakarta.servlet.jsp.jstl`), `mssql-jdbc`.
  - Deliverable: Project build thành công bằng `mvn clean compile` trên JDK 22. (Commit `a28677e`).
* [x] `TASK-002` `[TV1]` **Cấu hình Kết nối CSDL SQL Server:**
  - Tạo `src/main/resources/application.properties` kết nối SQL Server cổng 1433, database `ute_express`.
  - Cấu hình `spring.jpa.hibernate.ddl-auto=none` (bảo toàn schema.sql vật lý, cấm tự ý drop/alter table).
  - Cấu hình JPA: `spring.jpa.show-sql=true`, `spring.jpa.properties.hibernate.format_sql=true`.
  - Deliverable: Chạy ứng dụng kết nối SQL Server thành công không lỗi connection. (Commit `733a929`).
* [x] `TASK-003` `[TV1]` **Cấu hình Dual SecurityFilterChain Base:**
  - Tạo `SecurityConfig.java` với 2 filter chain:
    - `@Order(1)` matcher `/api/**` với `SessionCreationPolicy.STATELESS` cho REST API.
    - `@Order(2)` matcher `/**` với Session Cookie, form login cho Web SSR JSP.
  - Cấu hình `BCryptPasswordEncoder` bean.
  - Deliverable: Khung bảo mật khởi động đạt chuẩn, các URL public truy cập được. (Commit `12792ba`).
* [x] `TASK-004` `[TV3]` **Thiết lập Master Layout JSP Fragments & Bootstrap 5:**
  - Tạo cấu trúc thư mục `/WEB-INF/views/layout/`: `header.jsp`, `sidebar.jsp`, `footer.jsp`.
  - Nhúng CDN Bootstrap 5.3, Bootstrap Icons, FontAwesome, Google Fonts theo đúng `docs/UI_DESIGN_SYSTEM.md`.
  - Khai báo đúng chuẩn Taglib JSTL 3.0 Jakarta: `<%@ taglib prefix="c" uri="https://jakarta.ee/tags/jstl/core" %>`.
  - Thiết lập cấu trúc thẻ bảng màu chuẩn Logistics UTEExpress: Primary Deep Cobalt Blue `#0F3D78`, Accent Safety Orange `#F26522`, Background Industrial Gray `#F1F5F9`.
  - `[UI-STANDARD]`: Kích hoạt skill `gsd-ui-phase` / `gsd-ui-review` để thẩm định và kiểm tra độ tương thích responsive trên 3 kích thước (Mobile, Tablet, Desktop) và 6 trụ cột thị giác.
  - `[EXECUTION-PIPELINE]`: Tuân thủ quy trình 4 nhịp: (1) Dựng cấu trúc layout base -> (2) Tích hợp fragments và menu điều hướng -> (3) Kích hoạt Skill UI/UX để áp chuẩn Bootstrap & `docs/UI_DESIGN_SYSTEM.md` -> (4) Kiểm thử và Atomic Commit.
  - Deliverable: Màn hình mẫu `index.jsp` sử dụng `<jsp:include>` render layout hoàn hảo không vỡ CSS, đạt audit giao diện. (Commit `5ad0c2b`).

---

## PHASE 1: VERTICAL CORE ENTITIES & UI LAYOUTS
* **Mục tiêu:** Cả 3 thành viên làm song song: Xây dựng toàn bộ 25 JPA Entities, mapping khóa ngoại LAZY, viết Spring Data JPA Repositories và dựng khung các màn hình JSP thô của phân hệ mình.
* **Thời gian thực hiện:** Song song 3 thành viên.

### Phân hệ 1 — `[TV1]` Vận hành Bưu cục & Linehaul (9 Entities)
* [ ] `TASK-101` `[TV1]` **Entity & Repo: Hubs & EmployeeProfiles:**
  - Tạo `Hub.java` (quan hệ cây tự thân `@ManyToOne parentHub`), `EmployeeProfile.java`.
  - Map LAZY 100%, viết `HubRepository`, `EmployeeProfileRepository`.
* [ ] `TASK-102` `[TV1]` **Entity & Repo: Vehicles & TransportTrips:**
  - Tạo `Vehicle.java`, `TransportTrip.java` (map FK to Hubs, Driver User, Vehicle).
  - Viết `VehicleRepository`, `TransportTripRepository`.
* [ ] `TASK-103` `[TV1]` **Entity & Repo: ParcelContainers & ContainerItems:**
  - Tạo `ParcelContainer.java`, `ContainerItem.java` (Composite PK `@IdClass(ContainerItemId.class)`).
  - Viết `ParcelContainerRepository`, `ContainerItemRepository`.
* [ ] `TASK-104` `[TV1]` **Entity & Repo: Handovers, HandoverContainers & Incidents:**
  - Tạo `Handover.java`, `HandoverContainer.java`, `Incident.java`.
  - Viết Repositories với custom queries `JOIN FETCH`.
* [ ] `TASK-105` `[TV1]` **UI Skeleton: Màn hình Hub Staff & Linehaul Driver:**
  - Tạo các view JSP thô trong `/WEB-INF/views/hub/`: `inbound.jsp`, `sorting.jsp`, `outbound.jsp`.
  - Tạo view JSP trong `/WEB-INF/views/driver/`: `trips.jsp`, `handover.jsp`.
  - Bắt buộc áp dụng 13 Status Badges, Card Stat Counter, Action Card quét mã theo `docs/UI_DESIGN_SYSTEM.md`.
  - `[UI-STANDARD]`: Bắt buộc kích hoạt skill `gsd-ui-review` (hoặc `gsd-ui-phase`) để thẩm định và chấm điểm 6 trụ cột thị giác đạt chuẩn trước khi hoàn thành task.
  - `[EXECUTION-PIPELINE]`: Tuân thủ quy trình 4 nhịp: (1) Dựng Entity/Service -> (2) Dựng form/bảng JSP thô bắt đúng JSTL/EL -> (3) Kích hoạt Skill UI/UX để áp chuẩn Bootstrap & `docs/UI_DESIGN_SYSTEM.md` -> (4) Kiểm thử và Atomic Commit.

### Phân hệ 2 — `[TV2]` Cổng Khách hàng, Hiện trường & Realtime (7 Entities)
* [ ] `TASK-106` `[TV2]` **Entity & Repo: Users:**
  - Tạo `User.java` (id, username, password, full_name, phone, email, role, status, created_at).
  - Viết `UserRepository` (`findByUsername`, `findByPhone`, `findByEmail`).
* [ ] `TASK-107` `[TV2]` **Entity & Repo: Shipments & ShipmentRoutes:**
  - Tạo `Shipment.java` (13 trạng thái chuẩn, snapshot thông tin, cờ direction, on_hold).
  - Tạo `ShipmentRoute.java`. Viết `ShipmentRepository`, `ShipmentRouteRepository`.
* [ ] `TASK-108` `[TV2]` **Entity & Repo: ShipmentEvents:**
  - Tạo `ShipmentEvent.java` (map `@Column(name = "status")` với enum `ShipmentEventStatus`).
  - Viết `ShipmentEventRepository` truy vấn lịch sử sắp xếp `created_at ASC`.
* [ ] `TASK-109` `[TV2]` **Entity & Repo: ShipmentAssignments & Attempts:**
  - Tạo `ShipmentAssignment.java`, `PickupAttempt.java`, `DeliveryAttempt.java`.
  - Viết Repositories tương ứng.
* [ ] `TASK-110` `[TV2]` **UI Skeleton: Trang chủ, Public Tracking & Mobile Shipper:**
  - Tạo view `/WEB-INF/views/guest/`: `home.jsp`, `tracking.jsp` (Data Masking & Vertical Stepper Timeline), `calculator.jsp`.
  - Tạo view `/WEB-INF/views/user/`: `create-shipment.jsp`, `my-shipments.jsp`.
  - Tạo view `/WEB-INF/views/shipper/`: `tasks.jsp`, `delivery-action.jsp`.
  - Bắt buộc áp dụng Data Masking và Timeline CSS class theo `docs/UI_DESIGN_SYSTEM.md`.
  - `[UI-STANDARD]`: Bắt buộc kích hoạt skill `gsd-ui-review` (hoặc `gsd-ui-phase`) để thẩm định và chấm điểm 6 trụ cột thị giác đạt chuẩn trước khi hoàn thành task.
  - `[EXECUTION-PIPELINE]`: Tuân thủ quy trình 4 nhịp: (1) Dựng Entity/Service -> (2) Dựng form/bảng JSP thô bắt đúng JSTL/EL -> (3) Kích hoạt Skill UI/UX để áp chuẩn Bootstrap & `docs/UI_DESIGN_SYSTEM.md` -> (4) Kiểm thử và Atomic Commit.

### Phân hệ 3 — `[TV3]` Quản trị Mạng lưới, Báo cáo & Tài chính Đối soát (9 Entities)
* [ ] `TASK-111` `[TV3]` **Entity & Repo: Wallets & PricingPolicies:**
  - Tạo `Wallet.java` (quan hệ `@OneToOne` với User, balance, locked_balance).
  - Tạo `PricingPolicy.java`. Viết `WalletRepository`, `PricingPolicyRepository`.
* [ ] `TASK-112` `[TV3]` **Entity & Repo: Payments & CodTransactions:**
  - Tạo `Payment.java`, `CodTransaction.java`. Viết `PaymentRepository`, `CodTransactionRepository`.
* [ ] `TASK-113` `[TV3]` **Entity & Repo: ShipperReconciliations & Items:**
  - Tạo `ShipperReconciliation.java`, `ShipperReconciliationItem.java`.
  - Viết Repositories tính tổng tiền kỳ vọng và thực tế.
* [ ] `TASK-114` `[TV3]` **Entity & Repo: WithdrawalRequests, WalletTransactions, OperationReports:**
  - Tạo `WithdrawalRequest.java`, `WalletTransaction.java` (sổ cái đa hình), `OperationReport.java`.
  - Viết Repositories tương ứng.
* [ ] `TASK-115` `[TV3]` **UI Skeleton: Admin & Manager Dashboard:**
  - Tạo view `/WEB-INF/views/admin/`: `hubs.jsp`, `pricing.jsp`, `employees.jsp`, `withdrawals.jsp`.
  - Tạo view `/WEB-INF/views/manager/`: `dashboard.jsp`, `reconciliation.jsp`, `daily-report.jsp`.
  - Bắt buộc dùng chuẩn Card Stat Counter, Table Striped Hover, 13 Status Badges theo `docs/UI_DESIGN_SYSTEM.md`.
  - `[UI-STANDARD]`: Bắt buộc kích hoạt skill `gsd-ui-review` (hoặc `gsd-ui-phase`) để thẩm định và chấm điểm 6 trụ cột thị giác đạt chuẩn trước khi hoàn thành task.
  - `[EXECUTION-PIPELINE]`: Tuân thủ quy trình 4 nhịp: (1) Dựng Entity/Service -> (2) Dựng form/bảng JSP thô bắt đúng JSTL/EL -> (3) Kích hoạt Skill UI/UX để áp chuẩn Bootstrap & `docs/UI_DESIGN_SYSTEM.md` -> (4) Kiểm thử và Atomic Commit.

---

## PHASE 2: BUSINESS SERVICES & CONTROLLER WORKFLOWS
* **Mục tiêu:** Cả 3 thành viên cài đặt tầng Business Logic (Service Interface + ServiceImpl) tuân thủ Clean Code, Dual Validation, PRG Pattern và kết nối Controller với JSP.
* **Thời gian thực hiện:** Song song 3 thành viên.

### Phân hệ 1 — `[TV1]` Nghiệp vụ Bưu cục & Trung chuyển
* [ ] `TASK-201` `[TV1]` **InboundScanService & RouteMatrixService:**
  - Xây dựng `InboundScanService`: Quét mã đơn/bao, cập nhật `AT_HUB`, sinh sự kiện `ARRIVED_AT_HUB`.
  - Xây dựng `RouteMatrixRules` (Service in-memory tĩnh theo Mục 39): Nhận `(currentHubId, destRegion)` -> trả về `nextHubId` và mã khay chỉ định.
* [ ] `TASK-202` `[TV1]` **BaggingOutboundService & HandoverService:**
  - Xây dựng `BaggingService`: Gom các kiện `SORTED` cùng tuyến vào Bao mới, niêm phong `SEALED`.
  - Xây dựng `HandoverService`: Tạo phiếu bàn giao Outbound Staff -> Linehaul Driver, kiểm đếm số bao.
* [ ] `TASK-203` `[TV1]` **TripManagementService & Controllers:**
  - Xây dựng `TransportTripService`: Khởi hành chuyến xe (`IN_TRANSIT`), đến nơi (`ARRIVED`).
  - Xây dựng `HubOperationController` và `LinehaulDriverController` xử lý PRG pattern sau mỗi lần quét mã.
  - Tích hợp Dual Validation: validate Server-side, binding `errors` và `formData` vào view khi quét mã sai hoặc thiếu dữ liệu.
  - `[UI-STANDARD]`: Kích hoạt skill `gsd-ui-review` kiểm tra thông báo flash messages, feedback lỗi form và trạng thái xe/kiện.
  - `[EXECUTION-PIPELINE]`: Tuân thủ quy trình 4 nhịp: (1) Dựng Entity/Service -> (2) Dựng form/bảng JSP thô bắt đúng JSTL/EL -> (3) Kích hoạt Skill UI/UX để áp chuẩn Bootstrap & `docs/UI_DESIGN_SYSTEM.md` -> (4) Kiểm thử và Atomic Commit.

### Phân hệ 2 — `[TV2]` Nghiệp vụ Khách hàng & Hiện trường Shipper
* [ ] `TASK-204` `[TV2]` **AuthService & OTP Email Verification:**
  - Xây dựng `AuthService`: Đăng ký, đăng nhập Spring Security, mã hóa mật khẩu `BCrypt`.
  - Tích hợp `JavaMailSender` sinh OTP 6 số ngẫu nhiên, lưu session và xác thực kích hoạt tài khoản.
* [ ] `TASK-205` `[TV2]` **ShipmentCreationService & Auto-link by Phone:**
  - Xây dựng `ShipmentService`: Tạo đơn (`HOME_PICKUP` / `SELF_DROPOFF`), tính cước động từ `PricingPolicy`.
  - Logic Auto-link: Tìm User theo `receiver_phone`, nếu có -> gán `receiver_user_id`; nếu chưa -> để NULL và lưu snapshot.
* [ ] `TASK-206` `[TV2]` **ShipperFieldService & Fast-Fail Logic:**
  - Xây dựng `ShipperService`:
    - Lấy hàng tận nhà (`PICKED_UP`), thu cước nếu tiền mặt.
    - Giao hàng (`DELIVERED`), thu COD.
    - Xử lý thất bại: Phân loại Nhóm 1 (Hẹn lại tối đa 3 lần/3 ngày) vs Nhóm 2 (Fast-fail bom hàng/sai địa chỉ chuyển hoàn ngay).
* [ ] `TASK-207` `[TV2]` **Customer & Shipper Controllers:**
  - Xây dựng `CustomerShipmentController`, `PublicTrackingController` (áp dụng che mờ Data Masking), `ShipperController` (kèm REST API `/api/shipper/**`).
  - Tích hợp Dual Validation: hiển thị lỗi form tạo đơn, giữ nguyên dữ liệu đã nhập, không echo password.
  - `[UI-STANDARD]`: Kích hoạt skill `gsd-ui-review` kiểm tra Form Ergonomics (giữ giá trị cũ, feedback lỗi đỏ d-block), Stepper Timeline và Data Masking.
  - `[EXECUTION-PIPELINE]`: Tuân thủ quy trình 4 nhịp: (1) Dựng Entity/Service -> (2) Dựng form/bảng JSP thô bắt đúng JSTL/EL -> (3) Kích hoạt Skill UI/UX để áp chuẩn Bootstrap & `docs/UI_DESIGN_SYSTEM.md` -> (4) Kiểm thử và Atomic Commit.

### Phân hệ 3 — `[TV3]` Nghiệp vụ Quản trị, Báo cáo & Tài chính Đối soát
* [ ] `TASK-208` `[TV3]` **NetworkManagementService & PricingPolicyService:**
  - Xây dựng `HubManagementService`: Quản lý cây bưu cục 3 cấp, kiểm tra ràng buộc cấp cha - con.
  - Xây dựng `PricingPolicyService`: Tính cước gốc và cước phụ trội theo vùng miền.
* [ ] `TASK-209` `[TV3]` **ShipperReconciliationService (Đối soát ca):**
  - Xây dựng `ReconciliationService`:
    - Tổng hợp danh sách đơn đã giao trong ca của Shipper.
    - Nhập số tiền thực tế nộp:
      * Khớp tiền: Toàn bộ items `MATCHED`, COD chuyển `RECONCILED -> READY_FOR_PAYOUT`.
      * Lệch tiền: Tách luồng: Đơn đủ tiền giải ngân ngay; đơn thiếu tiền giữ `HOLD_DISPUTE` và ghi công nợ Shipper.
* [ ] `TASK-210` `[TV3]` **OperationReportService & DisposedTimeoutService:**
  - Xây dựng `OperationReportService`: Quản lý báo cáo ca ngày của Hub Manager.
  - Xây dựng `DisposedExceptionService`: Quản lý hàng hoàn quá hạn 7 ngày (hoặc 2 phút Demo) không nhận -> duyệt tiêu hủy/thanh lý `CLOSED_EXCEPTION (closure_reason = DISPOSED)`.
* [ ] `TASK-211` `[TV3]` **Admin & Manager Controllers:**
  - Xây dựng `AdminController` và `ManagerController`, tuân thủ PRG pattern và Dual Validation.
  - Xử lý các luồng duyệt rút tiền, đối soát ca Shipper lệch tiền, hiển thị flash message Toast/Alert chuẩn UX.
  - `[UI-STANDARD]`: Kích hoạt skill `gsd-ui-review` kiểm tra các bảng đối soát, badge màu lệch tiền (`SHORTAGE`), modal xác nhận duyệt.
  - `[EXECUTION-PIPELINE]`: Tuân thủ quy trình 4 nhịp: (1) Dựng Entity/Service -> (2) Dựng form/bảng JSP thô bắt đúng JSTL/EL -> (3) Kích hoạt Skill UI/UX để áp chuẩn Bootstrap & `docs/UI_DESIGN_SYSTEM.md` -> (4) Kiểm thử và Atomic Commit.

---

## PHASE 3: ADVANCED INTEGRATION, REALTIME & RECONCILIATION
* **Mục tiêu:** Cài đặt các tính năng nâng cao, xử lý biên bản sự cố, tích hợp Realtime WebSocket, hoàn tất sổ cái ví điện tử và kịch bản demo mode.
* **Thời gian thực hiện:** Song song 3 thành viên.

### Phân hệ 1 — `[TV1]` Quy trình Dỡ bao & Biên bản Sự cố
* [ ] `TASK-301` `[TV1]` **UnpackBagService & Kiện Thất lạc (Incident Missing Parcel):**
  - Cài đặt quy trình dỡ bao (`UNPACK_BAG`):
    - Đủ kiện: 50/50 cập nhật `AT_HUB`.
    - Thiếu kiện (Case 4): 49 kiện cho đi tiếp; 1 kiện thiếu cô lập `IN_TRANSIT, on_hold = 1`, mở `Incident (type = MISSING_PARCEL)`.
  - Cập nhật Chuyến xe / Phiếu bàn giao sang `COMPLETED_WITH_DISCREPANCY`.
* [ ] `TASK-302` `[TV1]` **Incident Resolution & Điều tra Chuyến xe:**
  - Xây dựng quy trình xử lý Incident: Nếu tìm thấy kiện -> gỡ `on_hold` nhập Hub; nếu xác nhận mất hàng -> kích hoạt chuyển sang `CLOSED_EXCEPTION (closure_reason = LOST)` và chuyển thông tin sang Phân hệ 3 bồi thường.

### Phân hệ 2 — `[TV2]` Hạ tầng Realtime STOMP & Mobile Shipper
* [ ] `TASK-303` `[TV2]` **Spring WebSocket STOMP Configuration:**
  - Cấu hình WebSocket message broker: endpoint `/ws`, prefix `/topic` và `/queue`.
  - Bắn sự kiện realtime khi đơn hàng chuyển trạng thái (`ARRIVED_AT_HUB`, `OUT_FOR_DELIVERY`, `DELIVERED`, `COD_PAYOUT`).
* [ ] `TASK-304` `[TV2]` **Optimistic Locking & Race Condition Handler:**
  - Xử lý tình huống Sender bấm hủy đơn trên Web đúng lúc Shipper đang lấy hàng trên Mobile:
    - Nếu Shipper chưa bấm `PICKED_UP`: Hủy thành công, báo realtime Shipper dừng lấy hàng.
    - Nếu Shipper đã bấm `PICKED_UP`: Chặn hủy, thông báo kiện đã vào mạng lưới vận chuyển.

### Phân hệ 3 — `[TV3]` Sổ cái Ví điện tử, Bồi thường & Demo Scheduler
* [ ] `TASK-305` `[TV3]` **E-Wallet Ledger & Anti-Double-Spending Withdrawal:**
  - Nghiệp vụ COD Payout: Tự động cộng số dư ví Sender khi đối soát hoàn tất.
  - Nghiệp vụ Rút tiền: Khóa tức thì `balance -= amount`, `locked_balance += amount`.
  - Admin duyệt rút tiền: Trừ hẳn `locked_balance`, ghi `wallet_transactions (type = WITHDRAWAL_DEDUCT)`.
  - Admin từ chối: Hoàn lại `balance`, ghi `wallet_transactions (type = WITHDRAWAL_REFUND)`.
* [ ] `TASK-306` `[TV3]` **Tự động Bồi thường Mất hàng (Compensation Service):**
  - Nhận sự kiện kiện hàng mất từ `Incident (type = MISSING_PARCEL)` -> Tính tiền bồi thường = 100% `declared_value` + hoàn cước gửi -> Cộng trực tiếp vào ví Sender, ghi sổ cái `COMPENSATION`.
* [ ] `TASK-307` `[TV3]` **Cấu hình Demo Mode Scheduler:**
  - Tạo profile `application-demo.properties` với timeout rút ngắn còn 2 phút cho Dropoff, Pickup, và Grace Period hàng hoàn.

---

## PHASE 4: CROSS-TESTING, SEED DATA 8 DEMO SCENARIOS & ĐÓNG GÓI
* **Mục tiêu:** Cả 3 thành viên phối hợp kiểm thử liên hoàn toàn hệ thống, nạp dữ liệu mẫu seed data cho 8 kịch bản demo và đóng gói báo cáo đồ án.
* **Thời gian thực hiện:** Toàn đội ngũ.

### Danh sách Task Phase 4:
* [ ] `TASK-401` `[Cả 3 TV]` **Tạo Bộ Dữ liệu Seed Data Chuẩn (data-seed.sql):**
  - Nạp danh mục Bưu cục (Cây 3 cấp: SOC Long An, SOC Bình Dương, Regional Hubs, Delivery Hubs).
  - Nạp 6 tài khoản mẫu tương ứng 6 Roles (đã hash BCrypt).
  - Nạp Biểu phí chuẩn (`pricing_policies`), Xe tải (`vehicles`).
* [ ] `TASK-402` `[Cả 3 TV]` **Kiểm thử Liên hoàn Kịch bản Demo Chính (End-to-End Happy Path):**
  - Sender tạo đơn Dĩ An -> Cần Thơ có COD 500k -> Shipper lấy hàng -> Hub Dĩ An Inbound & Đóng bao -> Xe tải chạy qua Long An SOC -> Hub Cần Thơ dỡ bao -> Shipper phát hàng thành công thu 500k -> Manager đối soát khớp tiền -> Sender nhận thông báo WebSocket và tiền nạp vào ví -> Sender tạo lệnh rút tiền -> Admin duyệt rút tiền.
* [ ] `TASK-403` `[Cả 3 TV]` **Kiểm thử 8 Kịch bản Demo Ngoại lệ:**
  - [ ] Case 1: Tự gửi tại bưu cục quá hạn (Self-dropoff timeout 2 phút).
  - [ ] Case 2: Shipper lấy hàng quá hạn (Pickup timeout 2 phút).
  - [ ] Case 3: Giao hàng thất bại 3 lần/3 ngày chuyển hoàn.
  - [ ] Case 4: Dỡ bao thiếu kiện (49/50), cô lập kiện mất, mở Incident.
  - [ ] Case 5: Đối soát Shipper nộp thiếu tiền lẻ, tách luồng giải ngân.
  - [ ] Case 6: Hàng hoàn về bưu cục quá hạn 2 phút không lấy -> Manager duyệt tiêu hủy.
  - [ ] Case 7: Khách bom hàng -> Kích hoạt Fast-fail quay đầu chuyển hoàn ngay.
  - [ ] Case 8: Kiện hàng mất tích xác nhận bồi thường 100% vào Ví điện tử.
* [ ] `TASK-404` `[TV1]` **Rà soát Clean Code & Kiểm tra Bất biến LECTURE_STANDARDS.MD:**
  - Quét toàn bộ project đảm bảo không có Controller gọi Repository, không có Scriptlet `<% ... %>`, không có `@ManyToOne` thiếu LAZY.
* [ ] `TASK-405` `[Cả 3 TV]` **Đóng gói Báo cáo Kỹ thuật & Slide Thuyết minh:**
  - Hoàn thiện tài liệu kiến trúc, sơ đồ ERD, State Machine và bộ kịch bản demo cho buổi bảo vệ đồ án.
