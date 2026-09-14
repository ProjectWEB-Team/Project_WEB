# UTEExpress — Hệ thống Quản lý Chuỗi Giao nhận Logistics

## What This Is
UTEExpress là hệ thống website quản lý chuỗi giao nhận Logistics thuần vận chuyển hàng hóa (Courier, Express & Parcel - CEP) liên tỉnh và nội đô. Hệ thống quản lý toàn diện vòng đời vận đơn từ lúc tiếp nhận, phân loại qua mạng lưới bưu cục 3 cấp (Delivery Hub -> Regional Hub -> SOC), đóng bao gom kiện, vận chuyển xe tải Linehaul, phát tận tay người nhận, thu hộ COD, đối soát ca và vận hành sổ cái ví điện tử.

## Core Value
Đảm bảo tính toàn vẹn và minh bạch của chuỗi cung ứng logistics: Mỗi kiện hàng đều được theo dõi vết trạng thái chính xác qua 13 State Machine chuẩn, kiểm soát chặt chẽ quá trình đóng bao/dỡ bao chống thất lạc, đối soát dòng tiền COD chuẩn xác và tự động bồi thường khi phát sinh sự cố.

## Architecture & Technology Stack
- **Framework:** Spring Boot 3.x (Java 17+)
- **Presentation:** Spring MVC + JSP/JSTL 3.0 Jakarta + Bootstrap 5.3 Admin Layout
- **Data Access:** Spring Data JPA / Hibernate 6.x + Microsoft SQL Server 2019+
- **Security:** Dual SecurityFilterChain (@Order(1) JWT Stateless cho REST API Mobile; @Order(2) Session Cookie cho Web SSR)
- **Realtime:** Spring WebSocket STOMP (/ws)
- **Database Schema:** 25 bảng vật lý chuẩn hóa (docs/schema.sql)
- **Architecture Standards:** Tuân thủ 100% docs/LECTURE_STANDARDS.md (Kiến trúc 3 tầng, PRG Pattern, Dual Validation, JPA Fetch LAZY, BCrypt, Clean Code)

## Team Allocation (Vertical Full-Stack Slices)
1. **Thành viên 1 (Team Lead):** Phân hệ Vận hành Bưu cục & Trung chuyển Liên Hub (9 Entities: `hubs`, `employee_profiles`, `vehicles`, `transport_trips`, `parcel_containers`, `container_items`, `handovers`, `handover_containers`, `incidents`).
2. **Thành viên 2:** Phân hệ Cổng Khách hàng, Hiện trường Shipper & Realtime (7 Entities: `users`, `shipments`, `shipment_routes`, `shipment_events`, `shipment_assignments`, `pickup_attempts`, `delivery_attempts`).
3. **Thành viên 3:** Phân hệ Quản trị Mạng lưới, Báo cáo & Tài chính Đối soát (9 Entities: `wallets`, `pricing_policies`, `payments`, `cod_transactions`, `shipper_reconciliations`, `shipper_reconciliation_items`, `withdrawal_requests`, `wallet_transactions`, `operation_reports`).

## Requirements

### Validated
- ✓ Cấu trúc CSDL vật lý 25 bảng chuẩn hóa (`docs/schema.sql`)
- ✓ Sơ đồ quan hệ thực thể 4 cụm phân hệ (`docs/ERD.md`)
- ✓ Quy chuẩn giảng dạy và danh mục cấm tuyệt đối (`docs/LECTURE_STANDARDS.md`)
- ✓ Ký duyệt Phương án 1 cho cả 7 điểm xung đột kỹ thuật (ADR-01 đến ADR-07)

### Active
- [ ] Phase 0: Base Skeleton & Database Setup (Maven, SQL Server, Dual Security, Base Layout)
- [ ] Phase 1: Vertical Core Entities & UI Layouts (25 JPA Entities, Repositories, JSP thô)
- [ ] Phase 2: Business Services & Controller Workflows (Service logic, Dual validation, PRG)
- [ ] Phase 3: Advanced Integration, Realtime & Reconciliation (Unbagging, STOMP WS, E-Wallet)
- [ ] Phase 4: Cross-Testing, Seed Data 8 Demo Scenarios & Đóng gói Báo cáo

### Out of Scope
- Sàn thương mại điện tử (Shop, Product, Cart, Wishlist)
- Tích hợp cổng thanh toán ngân hàng thật ngoài đời (dùng mô phỏng VNPay callback & sandbox)
- GPS Realtime xe tải theo bản đồ vệ tinh trực tiếp
- Nhận diện OCR Barcode camera phức tạp

## Key Decisions
| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Giữ nguyên 25 bảng trong schema.sql | Schema đã chuẩn hóa chặt chẽ, tránh phình to CSDL không cần thiết | Áp dụng (ADR-07) |
| Định tuyến tĩnh bằng Service in-memory | Đúng bản chất Route Matrix Rules, tốc độ O(1), không lo mất seed data | Áp dụng (ADR-01) |
| Bảng wallets độc lập quản lý số dư | Tách bạch tài khoản và tài chính, hỗ trợ khóa số dư chống Double-Spending | Áp dụng (ADR-02) |
| Vertical Slicing Full-stack cho 3 người | Tránh phân tầng ngang, bảo đảm cả 3 thành viên làm chủ trọn vẹn 3 tầng | Áp dụng (Nguyên tắc 3) |
| Atomic Commit sau từng Entity/Service/Controller | Tránh commit dồn, bảo đảm tính truy vết và an toàn mã nguồn | Áp dụng (Nguyên tắc 2) |

---
*Last updated: 2026-09-15 after project initialization*
