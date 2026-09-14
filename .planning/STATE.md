# UTEEXPRESS — PROJECT STATE & MEMORY
## Trạng thái Quản phối Dự án (Project Memory)
**Dự án:** UTEExpress - Hệ thống Quản lý Chuỗi Giao nhận Logistics  
**Framework Quản phối:** GSD Core on Antigravity  
**Phiên bản Schema:** 1.0 (25 Bảng Vật lý theo `docs/schema.sql`)  
**Khởi tạo lúc:** 2026-09-15 01:25:00+07:00  
**Giai đoạn hiện tại:** **PHASE 0: BASE SKELETON & DATABASE SETUP** (Sẵn sàng chạy `/gsd-plan-phase 0`)

---

## 1. TỔNG QUAN VỊ TRÍ ROADMAP HIỆN TẠI

```text
[Phase 0: Base Skeleton & DB Setup] ◄── [VỊ TRÍ HIỆN TẠI - CHUẨN BỊ THỰC THI]
  ├── Task-001: Maven Spring Boot 4.x & Java 22 Skeleton
  ├── Task-002: Kết nối SQL Server ute_express
  ├── Task-003: Khung Dual SecurityFilterChain & BCrypt
  └── Task-004: Master Layout JSP Fragments Bootstrap 5

[Phase 1: Vertical Core Entities & UI Layouts] (Pending)
[Phase 2: Business Services & Controller Workflows] (Pending)
[Phase 3: Advanced Integration, Realtime & Reconciliation] (Pending)
[Phase 4: Cross-Testing, Seed Data 8 Scenarios & Final Packaging] (Pending)
```

---

## 2. PHÂN BỔ NHÂN SỰ & RANH GIỚI PHÂN HỆ DỌC (VERTICAL SLICES)

| Thành viên | Vai trò dự án | Phân hệ nghiệp vụ phụ trách | Danh mục 25 Thực thể JPA phụ trách | Trạng thái hiện tại |
|---|---|---|---|---|
| **Thành viên 1** | Team Lead / Lead Architect | Vận hành Bưu cục & Trung chuyển Liên Hub (Linehaul) | `hubs`, `employee_profiles`, `vehicles`, `transport_trips`, `parcel_containers`, `container_items`, `handovers`, `handover_containers`, `incidents` (9 entities) | Sẵn sàng khởi động Phase 0 |
| **Thành viên 2** | Full-stack Developer | Cổng Khách hàng, Hiện trường Shipper & Realtime | `users`, `shipments`, `shipment_routes`, `shipment_events`, `shipment_assignments`, `pickup_attempts`, `delivery_attempts` (7 entities) | Sẵn sàng nhận Phase 1 |
| **Thành viên 3** | Full-stack Developer | Quản trị Mạng lưới, Báo cáo & Tài chính Đối soát | `wallets`, `pricing_policies`, `payments`, `cod_transactions`, `shipper_reconciliations`, `shipper_reconciliation_items`, `withdrawal_requests`, `wallet_transactions`, `operation_reports` (9 entities) | Sẵn sàng nhận Phase 0 (Layout) & Phase 1 |

---

## 3. CÁC QUYẾT ĐỊNH KIẾN TRÚC ĐÃ KÝ DUYỆT (ACTIVE ADRs)

Toàn bộ 8 quyết định dưới đây đã được Trưởng nhóm (User) phê duyệt:

* **ADR-01: Định tuyến Ma trận Tĩnh (Route Matrix Rules):**
  - Giữ nguyên CSDL 25 bảng của `schema.sql`, không thêm bảng `hub_routing_rules`.
  - Triển khai ma trận định tuyến tĩnh dưới dạng **Java Service Logic In-Memory / Configuration Component** tại Phân hệ 1 theo đúng quy tắc Mục 39 của `UTEExpress_Workflow.md`.
* **ADR-02: Chuẩn hóa Bảng Ví Điện Tử `wallets` độc lập:**
  - Bảng `dbo.wallets` (`user_id UNIQUE`, `balance`, `locked_balance`) là nguồn chân lý duy nhất cho tài chính của người dùng.
  - Bảng `users` KHÔNG chứa cột `e_wallet`. Mọi logic tăng/giảm số dư thao tác qua `WalletService`.
* **ADR-03: Tên Cột Sự Kiện Vận Đơn `shipment_events.status`:**
  - JPA Entity `ShipmentEvent` map `@Column(name = "status")` tương thích 100% với DDL `schema.sql`, sử dụng Enum Java `ShipmentEventStatus`.
* **ADR-04: Khóa Ngoại Bảng `handovers`:**
  - `sender_employee_id` tham chiếu `dbo.employee_profiles(id)`.
  - `receiver_driver_id` tham chiếu `dbo.users(id)`.
  - Service layer của Thành viên 1 bắt buộc tra cứu `EmployeeProfile` của nhân viên xuất hàng trước khi gán.
* **ADR-05: Lưu Vết Thông Tin Hub trong Sự Cố `incidents`:**
  - Giữ nguyên `schema.sql`: Cột `hub_id` lưu bưu cục phát hiện sự cố; các thông tin Hub đi / Hub đến được truy xuất qua quan hệ `@ManyToOne` với `TransportTrip`.
* **ADR-06: Enum Trạng Thái Đối Soát `shipper_reconciliation_items`:**
  - Sử dụng 3 trạng thái chi tiết theo DDL: `MATCHED` (Khớp), `SHORTAGE` (Thiếu tiền), `OVERAGE` (Thừa tiền).
* **ADR-07: Thống Nhất Quy Mô 25 Bảng Vật Lý:**
  - Xác nhận hệ thống có chính xác 25 bảng vật lý / 25 thực thể JPA chia đều cho 3 thành viên theo tỷ lệ 9 - 7 - 9.
* **ADR-08: Nền tảng Runtime Spring Boot 4.x và Java 22:**
  - Áp dụng Spring Boot 4.x và Java 22 (`<java.version>22</java.version>`) tương thích hoàn hảo với môi trường máy chủ phát triển (JDK 22, Maven 3.9.11).
  - Tuân thủ 100% toàn bộ bài giảng và danh mục cấm tại `docs/rule.md`.
* **ADR-09: Chuẩn hóa UI/UX Design System & Tự động hóa Thẩm định Giao diện:**
  - Thiết lập `docs/UI_DESIGN_SYSTEM.md` làm bộ quy chuẩn thiết kế duy nhất cho toàn bộ hệ thống (Bootstrap 5.3, bảng màu nhận diện HCMUTE Primary Deep Cobalt Blue `#0F3D78`, Accent Safety Orange `#F26522`, Background Industrial Gray `#F1F5F9`, 13 Status Badges tương phản cao cho kho/shipper, Form Ergonomics, Vertical Stepper Timeline, Data Masking).
  - Tự động hóa kiểm soát phân mảnh giao diện giữa 3 thành viên: Ràng buộc mọi task presentation với tiêu chuẩn `[UI-STANDARD]` (kích hoạt skill `gsd-ui-review` / `gsd-ui-phase` để audit 6 trụ cột thị giác) và quy trình 4 nhịp `[EXECUTION-PIPELINE]`.
  - Cấm tuyệt đối CSS inline và việc tự ý cài thêm CSS framework ngoại lai.

---

## 4. BỘ INVARIANTS KỸ THUẬT BẮT BUỘC TRONG TỪNG COMMIT

Mọi commit của cả 3 thành viên phải tuân thủ nghiêm ngặt:
1. **Kiến trúc 3 tầng:** Controller -> Service Interface + ServiceImpl -> Repository. Controller CẤM gọi Repository trực tiếp.
2. **Cấm Scriptlet trong JSP:** 100% dùng JSTL 3.0 Jakarta (`https://jakarta.ee/tags/jstl/core`) và EL `${}`. Thoát chuỗi an toàn chống XSS qua `<c:out value="${...}" />`.
3. **PRG Pattern:** POST thành công bắt buộc `redirect`. CẤM dùng `forward`.
4. **Dual Validation:** Server-side nạp `errors` và `formData` vào Model khi forward lại trang form lỗi. Không echo password.
5. **JPA LAZY:** 100% `@ManyToOne` và `@OneToOne` bắt buộc khai báo `fetch = FetchType.LAZY`. Dùng JPQL `JOIN FETCH` để triệt tiêu N+1 Query.
6. **Bảo mật:** Mật khẩu băm bằng `BCryptPasswordEncoder`. Cookie session có `HttpOnly = true, SameSite = Lax`.
7. **UI/UX Design System:** 100% các trang JSP sinh ra bắt buộc đối chiếu và tuân thủ `docs/UI_DESIGN_SYSTEM.md` (Bootstrap 5 native, bảng màu nhận diện HCMUTE `#0F3D78`, Accent `#F26522`, Background `#F1F5F9`, 13 Status Badges chuẩn tương phản cao, Form validation feedback, Stepper Timeline, Data Masking). Tuyệt đối cấm inline CSS và CSS framework ngoại lai.

---

## 5. HÀNH ĐỘNG TIẾP THEO (NEXT ACTION)

Hệ thống quản phối đã sẵn sàng 100%. Bước tiếp theo là chạy lệnh:

```bash
/gsd-plan-phase 0
```

để Team Lead (Thành viên 1) lập kế hoạch chi tiết các bước triển khai của **Phase 0: Base Skeleton & Database Setup**.
