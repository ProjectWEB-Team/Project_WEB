# UTEEXPRESS — SYSTEM REQUIREMENTS SPECIFICATION (SRS)
## Hệ thống Quản lý Chuỗi Giao nhận Logistics UTEExpress
**Framework Quản phối:** GSD Core on Antigravity  
**Phiên bản:** 1.0 (Ground Truth Synchronized)  
**Nguồn chuẩn:** `docs/rule.md` (Bài giảng đầy đủ ThS. Nguyễn Hữu Trung - HCMUTE), `docs/UTEExpress_Workflow.md`, `docs/schema.sql`, `docs/ERD.md`  
**Công nghệ & Runtime:** Spring Boot 4.x + Java 22 (JDK 22) + Maven 3.9.x + SQL Server 2019+  
**Quyết định phê duyệt:** Phương án 1 cho cả 7 điểm xung đột (Bảo toàn nguyên vẹn 25 bảng vật lý trong `schema.sql`).

---

## 1. TỔNG QUAN HỆ THỐNG & ĐẶC TẢ PHẠM VI

### 1.1. Bản chất Dự án
* **UTEExpress** là hệ thống giao nhận logistics **thuần vận chuyển hàng hóa** (Courier, Express & Parcel - CEP).
* **Tuyệt đối KHÔNG xây dựng:** Sàn thương mại điện tử, Shop, Giỏ hàng (Cart), Sản phẩm (Product), Đánh giá shop, Marketplace người bán.
* **Mục tiêu cốt lõi:** Quản lý hành trình kiện hàng từ lúc Người gửi (Sender) tạo đơn, qua mạng lưới bưu cục 3 cấp (Delivery Hub -> Regional Hub -> SOC), đóng bao gom kiện, vận chuyển bằng xe tải Linehaul, phát tới tay Người nhận (Receiver), thu hộ COD, đối soát ca và hoàn tất thanh toán/ví điện tử.

### 1.2. Nguồn Chân lý Duy nhất (Single Source of Truth)
1. **Cơ sở dữ liệu vật lý:** 25 bảng chuẩn hóa theo `docs/schema.sql` (hệ quản trị Microsoft SQL Server 2019+).
2. **Quy chuẩn lập trình toàn diện:** `docs/rule.md` (Toàn bộ bài giảng và quy chuẩn ThS. Nguyễn Hữu Trung - HCMUTE): Kiến trúc 3 tầng, PRG Pattern, Dual Validation, JPA Fetch LAZY, BCrypt, Clean Code, 10 nguyên tắc Clean Code và danh mục cấm tuyệt đối.
3. **Mô hình thực thể:** `docs/ERD.md` chia 4 cụm phân hệ.
4. **Nghiệp vụ chi tiết:** 109 mục nghiệp vụ trong `docs/UTEExpress_Workflow.md`.
5. **Nền tảng thực thi:** Spring Boot 4.x trên nền Java 22 (JDK 22) kết nối SQL Server 2019+.

---

## 2. KIẾN TRÚC PHÂN QUYỀN RBAC & DATA SCOPING

### 2.1. Danh mục 6 Role & Nguyên tắc Scoping
Hệ thống quản lý định danh người dùng qua bảng `dbo.users` với cột `role NVARCHAR(50)` gồm đúng 6 giá trị:

| Role | Định danh kỹ thuật | Phạm vi trách nhiệm chính |
|---|---|---|
| **Khách vãng lai** | `GUEST` *(Không lưu role trong DB)* | Xem trang chủ, ước tính cước phí, tra cứu hành trình Tracking công khai (Public Tracking), Đăng ký/Đăng nhập. |
| **Khách hàng** | `ROLE_USER` | Người gửi (Sender) và Người nhận (Receiver). Một User có thể vừa gửi đơn này vừa nhận đơn khác. Không tạo `ROLE_SENDER` / `ROLE_RECEIVER`. |
| **Nhân viên Giao nhận** | `ROLE_SHIPPER` | Lấy hàng tại nhà (First-mile) và Giao hàng tận tay (Last-mile). Không tham gia vận chuyển Linehaul. |
| **Nhân viên Bưu cục** | `ROLE_HUB_STAFF` | Xử lý nội bộ Hub/SOC theo vị trí (`position`): `INBOUND`, `SORTER`, `OUTBOUND`, `COORDINATOR`, `GENERAL_STAFF`. |
| **Tài xế Trung chuyển** | `ROLE_LINEHAUL_DRIVER` | Lái xe tải vận chuyển bao hàng giữa các bưu cục: Hub ↔ Hub, Hub ↔ SOC, SOC ↔ SOC. |
| **Quản lý Bưu cục** | `ROLE_MANAGER` | Quản lý Hub/SOC. Cấp quản lý xác định động qua `manager.hub_id -> hub.level` (Level 1: Delivery Hub, Level 2: Regional Hub, Level 3: SOC). |
| **Quản trị viên** | `ROLE_ADMIN` | Quản trị toàn hệ thống: User, Nhân sự, Cây bưu cục 3 cấp, Biểu phí, Phê duyệt lệnh rút tiền, Giám sát Incident. |

### 2.2. Nguyên tắc Data Scoping Bắt buộc tại Service Layer
Không chỉ kiểm tra Role ở Controller, tầng Service MUST thực thi kiểm tra 5 chiều:
$$\text{ALLOW} = \text{Role} \land \text{Assigned Hub} \land \text{Position} \land \text{Task Assignment} \land \text{Current Shipment Location}$$
* **Ví dụ:** Nhân viên bưu cục Dĩ An Hub chỉ được Inbound kiện hàng có đích đến hoặc đang hiện diện tại Dĩ An Hub; không được quét kiện đang ở Hà Nội Hub (báo lỗi HTTP 403 Forbidden).

---

## 3. MA TRẬN BẢO MẬT & PHÂN QUYỀN URL (DUAL SECURITY FILTER CHAIN)

Hệ thống thiết lập 2 `SecurityFilterChain` độc lập trong Spring Security:

```text
                                Client Request
                                      │
                 ┌────────────────────┴────────────────────┐
                 ↓                                         ↓
      [FilterChain 1: @Order(1)]                [FilterChain 2: @Order(2)]
       securityMatcher("/api/**")                 Mặc định mọi URL còn lại
       Session: STATELESS                         Session: IF_REQUIRED (Cookie)
       Auth: Bearer JWT                           Auth: Form Login (JSESSIONID)
       Target: Mobile Shipper / Driver            Target: Web JSP/JSTL SSR
```

### Bảng Phân quyền URL chi tiết

| URL Pattern | Giao thức xác thực | Quyền hạn truy cập (`hasRole`) | Trách nhiệm hiển thị / API |
|---|---|---|---|
| `/`, `/home`, `/index` | Web Session Cookie | `permitAll` | Trang chủ giới thiệu UTEExpress |
| `/auth/login`, `/auth/register` | Web Session Cookie | `permitAll` | Form đăng nhập, đăng ký tài khoản |
| `/auth/verify-otp` | Web Session Cookie | `permitAll` | Xác thực tài khoản qua OTP gửi về Email |
| `/tracking/**` | Web Session Cookie | `permitAll` | Tra cứu hành trình công khai (Data Masking) |
| `/fee-calculator` | Web Session Cookie | `permitAll` | Bảng tính cước vận chuyển dự kiến |
| `/ws/**` | WebSocket Handshake | `permitAll` (STOMP) | Kênh bắn thông báo thời gian thực |
| `/user/**` | Web Session Cookie | `ROLE_USER` | Cổng thông tin khách hàng: Tạo đơn, Quản lý đơn, Ví điện tử, Rút tiền |
| `/hub/**` | Web Session Cookie | `ROLE_HUB_STAFF` | Màn hình tác nghiệp Hub: Quét Inbound, Phân loại, Đóng bao, Bàn giao |
| `/driver/**` | Web Session Cookie | `ROLE_LINEHAUL_DRIVER` | Màn hình Tài xế xe tải: Nhận bàn giao, Quản lý chuyến xe, Nhật ký hành trình |
| `/manager/**` | Web Session Cookie | `ROLE_MANAGER` | Dashboard quản lý Hub: Đối soát ca Shipper, Báo cáo vận hành ngày, Xử lý tồn |
| `/admin/**` | Web Session Cookie | `ROLE_ADMIN` | Dashboard Admin: Cây mạng lưới, Biểu phí vùng, Duyệt rút tiền, Nhân sự |
| `/api/auth/**` | REST API | `permitAll` | Login API cấp JWT token cho Shipper/Driver |
| `/api/tracking/**` | REST API | `permitAll` | API tra cứu vận đơn JSON cho tích hợp ngoài |
| `/api/shipper/**` | Bearer JWT | `ROLE_SHIPPER` | Mobile App Shipper: Lấy hàng, Giao hàng, Ghi nhận lý do thất bại, QR Code |
| `/api/driver/**` | Bearer JWT | `ROLE_LINEHAUL_DRIVER` | Mobile App Tài xế: Xác nhận nhận bao, Báo cáo vị trí trung chuyển |

---

## 4. CHUẨN HÓA STATE MACHINE: 13 TRẠNG THÁI VẬN ĐƠN DUY NHẤT

Hệ thống sử dụng đúng **13 trạng thái duy nhất** lưu tại `shipments.current_status`. Mọi hoạt động vận hành chi tiết được lưu trữ tại `shipment_events.status`.

```text
                             [CREATED]
                                 │
                 ┌───────────────┴───────────────┐
                 ↓                               ↓
       (Thanh toán Online)              (Tiền mặt / Trả sau)
      [WAITING_FOR_PAYMENT]                      │
                 │                               │
                 └───────────────┬───────────────┘
                                 ↓
                      [WAITING_FOR_HANDOVER]
                                 │
                 ┌───────────────┴───────────────┐
                 ↓ (HOME_PICKUP)                 ↓ (SELF_DROPOFF)
            [PICKED_UP]                          │
                 │                               │
                 └───────────────┬───────────────┘
                                 ↓
                             [AT_HUB] ◄──────────────────┐
                                 │                       │
                                 ├── (Linehaul chuyển tiếp)
                                 ↓                       │
                           [IN_TRANSIT] ─────────────────┘
                                 │ (Đã đến Hub phát)
                                 ↓
                         [OUT_FOR_DELIVERY]
                                 │
                 ┌───────────────┴───────────────┐
                 ↓ (Giao thành công)             ↓ (Giao thất bại)
            [DELIVERED]                       [FAILED]
                                                 │
                                                 ↓
                                         [RETURN_TO_SENDER]
                                                 │
                                 ┌───────────────┴───────────────┐
                                 ↓ (Sender nhận lại hàng)        ↓ (Quá hạn 7 ngày / Từ chối)
                       [RETURNED_TO_SENDER]              [CLOSED_EXCEPTION]
                                                          (closure_reason = DISPOSED)

   * HỦY ĐƠN (Trước khi nhận kiện):
     [CREATED] / [WAITING_FOR_HANDOVER] ──► [CANCELLED]

   * MẤT HÀNG TRUNG CHUYỂN:
     [IN_TRANSIT] (on_hold = 1) ──► [CLOSED_EXCEPTION] (closure_reason = LOST)
```

### Bảng Quy ước 13 Trạng thái Vận đơn (`ShipmentStatus`)

| Mã trạng thái Enum | Ý nghĩa nghiệp vụ | Điều kiện kích hoạt | Cờ liên quan |
|---|---|---|---|
| `CREATED` | Vận đơn mới tạo | Sender tạo đơn thành công trên hệ thống. | `payment_status = UNPAID/PENDING` |
| `WAITING_FOR_PAYMENT` | Chờ thanh toán cước | Khi `fee_payer = SENDER` và chọn thanh toán Online qua VNPay. | Cước chưa thanh toán |
| `WAITING_FOR_HANDOVER` | Chờ bàn giao hàng | Đã giải quyết cước (hoặc trả sau), chờ Shipper lấy hoặc Sender mang ra Hub. | Chờ tiếp nhận vật lý |
| `PICKED_UP` | Shipper đã lấy hàng tận nhà | Áp dụng cho `HOME_PICKUP`: Shipper quét mã nhận kiện từ Sender. | `picked_up_at = NOW()` |
| `AT_HUB` | Kiện hàng đang nằm trong bưu cục | Nhân viên Hub quét mã Inbound (nhận từ Shipper, Sender hoặc xe tải Linehaul). | `current_hub_id = Hub.id` |
| `IN_TRANSIT` | Kiện đang trung chuyển trên xe tải | Kiện đã đóng trong Bao (`Container`) và xe tải đã bắt đầu chuyến xe (`Trip`). | `direction = FORWARD/RETURN` |
| `OUT_FOR_DELIVERY` | Đang trên đường phát tận tay | Shipper bưu cục phát nhận kiện và bắt đầu ca giao cho Receiver. | Assignment `DELIVERY` |
| `FAILED` | Giao hàng thất bại | Hết 3 lần giao (Nhóm 1) hoặc kích hoạt Fast-Fail ngay lần đầu (Nhóm 2). | Ghi nhận `fail_reason` |
| `DELIVERED` | Phát hàng thành công | Người nhận đồng ý nhận và nộp đủ tiền COD/Cước (nếu có). | `delivered_at = NOW()` |
| `RETURN_TO_SENDER` | Đang chuyển hoàn về người gửi | Hàng đang trên đường quay lại bưu cục gốc hoặc đã về bưu cục gốc chờ Sender nhận. | `direction = RETURN` |
| `RETURNED_TO_SENDER` | Đã hoàn trả tận tay người gửi | Người gửi đã nộp phí hoàn (Return Fee) và nhận lại kiện hàng. | `returned_at = NOW()` |
| `CANCELLED` | Đơn hàng đã bị hủy | Hủy trước khi kiện vào mạng lưới vận chuyển (Mục 33). | Ghi nhận `cancel_reason` |
| `CLOSED_EXCEPTION` | Đóng kết thúc do sự cố | Hàng bị mất mát xác nhận bồi thường (`LOST`) hoặc tồn kho tiêu hủy (`DISPOSED`). | Ghi nhận `closure_reason` |

### Danh mục Enum Lý do phụ trợ (Reason Codes)
1. **Lý do giao thất bại (`fail_reason`):**
   * *Nhóm 1 (Cho phép hẹn lại tối đa 3 lần / 3 ngày):* `NO_ANSWER`, `PAYMENT_NOT_AVAILABLE`, `RESCHEDULED`, `OTHER`, `DELIVERY_ATTEMPTS_EXHAUSTED`.
   * *Nhóm 2 (Kích hoạt Fast-Fail chuyển hoàn ngay):* `RECEIVER_REFUSED` (Khách từ chối nhận/Bom hàng), `WRONG_ADDRESS` (Địa chỉ không tồn tại).
2. **Lý do hủy đơn (`cancel_reason`):**
   * `SENDER_REQUEST` (Người gửi chủ động hủy), `PAYMENT_TIMEOUT` (Quá hạn trả cước), `PICKUP_TIMEOUT` (Quá hạn lấy hàng), `WEIGHT_DISCREPANCY` (Chênh lệch khối lượng từ chối gửi).
3. **Lý do đóng ngoại lệ (`closure_reason`):**
   * `LOST` (Hàng thất lạc được bồi thường 100%), `DISPOSED` (Hàng tồn kho quá hạn thanh lý/tiêu hủy).

---

## 5. PHÂN BỔ 25 THỰC THỂ THEO 3 PHÂN HỆ DỌC (VERTICAL FULL-STACK SLICES)

Tuyệt đối cấm chia theo tầng ngang. Cả 3 thành viên thực thi Full-stack trên phân hệ của mình:

```text
+----------------------------------------------------------------------------------------------------+
|                                    UTEEXPRESS 25 JPA ENTITIES                                      |
+---------------------------------+--------------------------------+---------------------------------+
|   THÀNH VIÊN 1 (9 Thực thể)     |   THÀNH VIÊN 2 (7 Thực thể)    |   THÀNH VIÊN 3 (9 Thực thể)     |
|   Vận hành Bưu cục & Linehaul   |   Khách hàng, Shipper & RT     |   Mạng lưới & Tài chính Đối soát|
+---------------------------------+--------------------------------+---------------------------------+
| 1. hubs                         | 1. users                       | 1. wallets                      |
| 2. employee_profiles            | 2. shipments                   | 2. pricing_policies             |
| 3. vehicles                     | 3. shipment_routes             | 3. payments                     |
| 4. transport_trips              | 4. shipment_events             | 4. cod_transactions            |
| 5. parcel_containers            | 5. shipment_assignments        | 5. shipper_reconciliations      |
| 6. container_items              | 6. pickup_attempts             | 6. shipper_reconciliation_items |
| 7. handovers                    | 7. delivery_attempts           | 7. withdrawal_requests          |
| 8. handover_containers          |                                | 8. wallet_transactions         |
| 9. incidents                    |                                | 9. operation_reports            |
+---------------------------------+--------------------------------+---------------------------------+
```

### 5.1. Phân hệ 1: Vận hành Bưu cục & Trung chuyển Liên Hub (Thành viên 1 - Team Lead)
* **Entities phụ trách:** `hubs`, `employee_profiles`, `vehicles`, `transport_trips`, `parcel_containers`, `container_items`, `handovers`, `handover_containers`, `incidents`.
* **Nghiệp vụ chi tiết:**
  1. *Quét mã nhập bưu cục (Inbound Scan):* Đưa trạng thái về `AT_HUB`, sinh sự kiện `ARRIVED_AT_HUB`.
  2. *Định tuyến ma trận tĩnh (Route Matrix Rules):* Tra cứu mã đích (`destination_region`) theo Mục 39 để hiển thị chỉ định Khay/Bao và Next Hub.
  3. *Đóng bao niêm phong (Outbound Bagging):* Gom nhiều kiện cùng tuyến vào `parcel_containers`, tạo mã bao `BAG-xxx`, trạng thái `SEALED`.
  4. *Biên bản bàn giao xe tải (Handover):* Bàn giao bao hàng giữa Outbound Staff (`employee_profiles`) và Driver (`users`), ghi nhận số lượng bao dự kiến/thực nhận.
  5. *Hành trình chuyến xe (Linehaul Trip):* Quản lý xe tải (`vehicles`) chạy tuyến Hub ↔ Hub / Hub ↔ SOC, cập nhật `DEPARTED`, `IN_TRANSIT`, `ARRIVED`.
  6. *Quy trình Dỡ bao (Unpack Bag) & Biên bản sự cố (Incidents):* Đối chiếu số lượng thực tế:
     - Đủ 50/50: Cập nhật toàn bộ về `AT_HUB`.
     - Thiếu 49/50: 49 kiện hợp lệ cho đi tiếp ngay; 1 kiện thiếu đặt `current_status = IN_TRANSIT, on_hold = 1`, mở `Incident (type = MISSING_PARCEL)`.

### 5.2. Phân hệ 2: Cổng Khách hàng, Hiện trường & Realtime (Thành viên 2)
* **Entities phụ trách:** `users`, `shipments`, `shipment_routes`, `shipment_events`, `shipment_assignments`, `pickup_attempts`, `delivery_attempts`.
* **Nghiệp vụ chi tiết:**
  1. *Xác thực & Quản lý tài khoản:* Đăng ký, đăng nhập, mã hóa mật khẩu `BCrypt`, gửi mã OTP xác thực email bằng `JavaMailSender`.
  2. *Tự động liên kết theo SĐT (Auto-link Receiver by Phone) & Data Masking:* Tra cứu `receiver_phone`, ẩn thông tin nhạy cảm trên web nếu chưa xác thực OTP hoặc mã vận đơn đầy đủ.
  3. *Tạo vận đơn & Hủy đơn:* Chọn lấy hàng tại nhà (`HOME_PICKUP`) hoặc tự gửi tại bưu cục (`SELF_DROPOFF`), chọn đồng kiểm, chọn người trả cước. Kiểm tra Optimistic Locking chống Race Condition khi Sender hủy đơn lúc Shipper đang lấy hàng.
  4. *Tác nghiệp Shipper Lấy hàng / Giao hàng:*
     - Lấy hàng: Kiểm tra kiện, thu cước nếu tiền mặt, cập nhật `PICKED_UP`.
     - Giao hàng: Thu tiền COD + cước (nếu có), cập nhật `DELIVERED`.
     - Giao thất bại: Áp dụng quy tắc Thử lại 3 lần/3 ngày (Nhóm 1) hoặc Fast-Fail chuyển hoàn ngay (Nhóm 2: Bom hàng/Sai địa chỉ).
  5. *Hạ tầng Realtime STOMP WebSocket:* Cấu hình `/ws`, bắn notification tức thì đến User/Shipper khi đơn chuyển trạng thái.

### 5.3. Phân hệ 3: Quản trị Mạng lưới, Báo cáo & Tài chính Đối soát (Thành viên 3)
* **Entities phụ trách:** `wallets`, `pricing_policies`, `payments`, `cod_transactions`, `shipper_reconciliations`, `shipper_reconciliation_items`, `withdrawal_requests`, `wallet_transactions`, `operation_reports`.
* **Nghiệp vụ chi tiết:**
  1. *Quản trị Mạng lưới & Nhân sự:* CRUD cây bưu cục 3 cấp (Level 1: Delivery Hub, Level 2: Regional Hub, Level 3: SOC), phân bổ hồ sơ nhân viên (`employee_profiles`).
  2. *Cấu hình Biểu phí linh động (`pricing_policies`):* Định mức cước gốc, cước vượt cân theo vùng (Nội tỉnh, Nội vùng, Liên vùng Bắc - Trung - Nam).
  3. *Báo cáo ca vận hành ngày (`operation_reports`):* Tổng kết số lượng Inbound, Outbound, Delivered, Failed, Backlog, Missing, Damaged theo Hub.
  4. *Phê duyệt xử lý hàng tồn quá hạn (Disposed Exception):* Sau 7 ngày (hoặc 2 phút Demo) Sender không nhận hàng hoàn, Manager duyệt thanh lý/tiêu hủy sung quỹ bù cước.
  5. *Đối soát ca Shipper (Reconciliation):* Đối chiếu tiền COD + cước mặt:
     - Khớp tiền: Toàn bộ đơn `MATCHED`, COD chuyển `RECONCILED -> READY_FOR_PAYOUT`.
     - Lệch tiền: Đơn đủ tiền giải ngân ngay cho User vô tội; đơn thiếu tiền giữ `HOLD_DISPUTE`, lập Incident công nợ Shipper.
  6. *Ví điện tử & Rút tiền (E-Wallet Ledger):*
     - Tự động cộng COD Payout và tiền bồi thường mất hàng vào `wallets.balance`.
     - Chống Double-Spending khi User rút tiền: Khóa tức thì `balance -= amount`, `locked_balance += amount`. Khi Admin Duyệt -> trừ đứt; Từ chối -> hoàn lại số dư khả dụng.

---

## 6. ĐẶC TẢ 8 KỊCH BẢN NGOẠI LỆ & DEMO (8 GROUND TRUTH SCENARIOS)

Hệ thống thiết lập cấu hình `application-demo.properties` cho phép co ngắn timeout từ 48 giờ xuống **2 phút**:

1. **Scenario 1 — Tự mang hàng ra bưu cục quá hạn (Self Dropoff Timeout):** Đơn tạo sau 2 phút Sender không mang hàng ra bưu cục -> Tự động chuyển `CANCELLED (reason = DROPOFF_TIMEOUT)`, hoàn 100% cước vào ví điện tử.
2. **Scenario 2 — Lấy hàng tại nhà quá hạn (Pickup Timeout):** Shipper không liên hệ được Sender sau 2 phút -> Tự động chuyển `CANCELLED (reason = PICKUP_TIMEOUT)`, hoàn 100% cước vào ví điện tử.
3. **Scenario 3 — Giao hàng thất bại 3 lần (Delivery Exhausted):** Shipper giao không thành công 3 lần (lý do tạm hoãn) -> Chuyển `FAILED (fail_reason = DELIVERY_ATTEMPTS_EXHAUSTED)` -> Tự động tạo tuyến hoàn `RETURN_TO_SENDER`.
4. **Scenario 4 — Thiếu kiện khi Dỡ bao (Unbagging Missing Parcel):** Bao 50 kiện nhưng nhận 49 kiện -> 49 kiện hợp lệ cho đi tiếp bình thường; 1 kiện thiếu đặt `IN_TRANSIT, on_hold = 1`, mở `Incident (type = MISSING_PARCEL)`. Chuyến xe hoàn thành dạng `COMPLETED_WITH_DISCREPANCY`.
5. **Scenario 5 — Lệch tiền đối soát kết ca (Reconciliation Shortage):** Shipper nộp thiếu tiền -> Tách luồng: Đơn đủ tiền lập tức duyệt COD Payout vào ví Người gửi; đơn thiếu tiền giữ `HOLD_DISPUTE` và ghi nhận công nợ Shipper.
6. **Scenario 6 — Hàng hoàn quá hạn không nhận (Abandoned Parcel Disposed):** Hàng hoàn về bưu cục gốc quá 2 phút Sender không đến lấy và không đóng phí hoàn -> Manager duyệt chuyển sang `CLOSED_EXCEPTION (closure_reason = DISPOSED)` để thanh lý bù cước.
7. **Scenario 7 — Khách từ chối nhận dứt khoát / Bom hàng (Fast-Fail):** Shipper chọn lý do `RECEIVER_REFUSED` kèm ảnh bằng chứng -> Kích hoạt Fast-Fail: Không bắt giao lại lần 2-3, lập tức chuyển `FAILED (fail_reason = RECEIVER_REFUSED)` và quay đầu chuyển hoàn ngay trong ngày.
8. **Scenario 8 — Bồi thường mất hàng vào Ví điện tử (Compensation Payout):** Kiện hàng thất lạc xác nhận không thể tìm thấy -> Chuyển `CLOSED_EXCEPTION (closure_reason = LOST)` -> Hệ thống tự động tính tiền bồi thường = 100% `declared_value` + hoàn cước gửi, cộng trực tiếp vào `wallets.balance` của Sender, lưu vết sổ cái `wallet_transactions (type = COMPENSATION)`.

---

## 7. QUY CHUẨN KỸ THUẬT BẤT BIẾN (THEO LECTURE_STANDARDS.MD)

Tất cả mã nguồn do các thành viên và Agent sinh ra phải vượt qua bộ lọc kiểm tra sau:

```text
[BẮT BUỘC] 1. KIẾN TRÚC 3 TẦNG: Controller chỉ xử lý HTTP; Service Interface + ServiceImpl xử lý logic và Transaction; Repository chỉ truy vấn DB. Controller CẤM TUYỆT ĐỐI gọi Repository.
[BẮT BUỘC] 2. CẤM SCRIPTLET TRONG JSP: 100% dùng JSTL 3.0 Jakarta và Expression Language ${}. Xuất chuỗi an toàn qua <c:out value="${...}" /> hoặc fn:escapeXml.
[BẮT BUỘC] 3. MÔ HÌNH PRG: Mọi request POST thay đổi dữ liệu thành công bắt buộc redirect, CẤM forward để tránh lỗi F5 submit trùng đơn.
[BẮT BUỘC] 4. DUAL VALIDATION: Validate cả Client (HTML5/JS) và Server (Spring Validation/Map errors). Khi có lỗi, nạp `errors` và `formData` vào Model rồi forward lại trang để giữ lại dữ liệu đã nhập (cấm echo password).
[BẮT BUỘC] 5. JPA LAZY & TRÁNH N+1: Toàn bộ @ManyToOne và @OneToOne bắt buộc fetch = FetchType.LAZY. Mọi truy vấn lấy quan hệ phải dùng JPQL JOIN FETCH hoặc @EntityGraph.
[BẮT BUỘC] 6. BẢO MẬT: Mật khẩu băm bằng BCryptPasswordEncoder. Session cookie cấu hình HttpOnly = true, SameSite = Lax.
[BẮT BUỘC] 7. DI CONSTRUCTOR: 100% dùng Constructor Injection, CẤM dùng Field Injection (@Autowired trên thuộc tính).
[BẮT BUỘC] 8. NAMESPACE JAKARTA: Dùng jakarta.servlet.*, jakarta.persistence.*, jakarta.validation.*. CẤM dùng javax.* (ngoại trừ javax.sql.DataSource).
```

---

## 8. DANH MỤC CẤM TUYỆT ĐỐI (PROHIBITED CHECKLIST)

1. **CẤM** Controller gọi trực tiếp `Repository` hoặc `EntityManager`.
2. **CẤM** viết SQL, JDBC hoặc mã Java scriptlet `<% ... %>` bên trong các file `.jsp`.
3. **CẤM** đổi cấu hình quan hệ JPA từ `LAZY` sang `EAGER` để né lỗi LazyInitializationException.
4. **CẤM** lưu trữ mật khẩu dưới dạng Plaintext hoặc băm bằng thuật toán yếu (MD5, SHA-1, SHA-256 thuần).
5. **CẤM** thực hiện lệnh POST thành công mà dùng `forward` thay vì `redirect` (vi phạm PRG).
6. **CẤM** nuốt lỗi Exception (Empty catch block) hoặc trả thông tin Stack trace kỹ thuật cho Client.
7. **CẤM** làm việc theo tầng ngang; phân chia công việc sai lệch ranh giới thực thể giữa 3 thành viên.
