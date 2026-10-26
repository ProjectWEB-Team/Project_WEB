# Phase 04 — Handoff cho Phase 05 (Cross-slice Integration)

> **Phạm vi:** tài liệu này chỉ ghi lại các hợp đồng và điểm cần quyết định giữa các lát cắt dọc. Nó không thay thế quyền ưu tiên của `docs/ARCHITECTURE_DECISIONS.md` và `docs/UTEExpress_Workflow.md`.

Phase 04 (Last-mile & Finance — Huỳnh Tấn Anh Phát) đã hoàn tất trên nhánh `feature/lastmile-finance`. Tài liệu này mô tả những gì các lát cắt khác cần biết trước khi hợp nhất.

---

## 1. Hợp đồng với Phase 02 — User & Shipment Intake (Hoàng Phúc Khang)

### 1.1 Nghĩa vụ hoàn tiền (refund)

Khi người gửi hủy đơn đã thanh toán trước, Phase 02 ghi nhận nghĩa vụ hoàn tiền; Phase 04 áp dụng khoản hoàn vào ví.

| Hợp đồng | Chi tiết |
|---|---|
| API áp dụng hoàn tiền | `ShipmentFinanceService.refundShipmentPayment(Long paymentId, String reason)` |
| Điều kiện áp dụng | `payment.status = PAID` **và** `shipment.current_status = CANCELLED` |
| Hiệu ứng | `payments.status = REFUNDED`, `payments.refunded_at` được set, `shipments.payment_status = REFUNDED`, ví người gửi được cộng, ghi `wallet_transactions` với `transaction_type = REFUND`, `reference_type = PAYMENT` |
| Chống hoàn trùng | `wallet_transactions` có ràng buộc logic theo `(transaction_type, reference_type, reference_id)`; lần gọi lại sẽ ném `IllegalStateException` |
| Quyền thực hiện | `ROLE_ADMIN` hoặc `ROLE_MANAGER` |
| Danh sách nghĩa vụ chờ | `ShipmentFinanceService.listPendingRefundCandidates()` trả các `payments` `PAID` + `SHIPPING_FEE` của vận đơn `CANCELLED` |

**Quy ước bàn giao (theo `TEAM_EXECUTION_PLAN.md`):** bản ghi nghĩa vụ hoàn tiền của Phase 02 phải **chờ** cho tới khi Phase 04 ghi `wallet_transactions` loại `REFUND` và `payments.status = REFUNDED` được commit cùng nhau. Hai phần này không được commit tách rời.

### 1.2 Bảng giá và phí hoàn

Phase 04 đọc `shipments.return_fee` để tính phí hoàn. Nếu giá trị này chưa được điền, service dùng cấu hình `uteexpress.finance.default-return-fee` (mặc định `25000.00`) làm giá trị tạm. **Khi Phase 02 nối `pricing_policies`, giá trị đó sẽ thay thế hoàn toàn** và cấu hình tạm có thể bỏ.

### 1.3 Cột `shipments` mới

Phase 04 bổ sung các cột sau vào bảng `shipments` (đều đã có trong `docs/schema.sql`):

| Cột | Ý nghĩa | Phase nào dùng |
|---|---|---|
| `declared_value` | Giá trị khai giá, dùng để bồi thường kiện mất | Phase 04 (bồi thường) |
| `cancel_reason` | Lý do hủy | Phase 02 (hủy) + Phase 04 (hoàn tiền) |
| `closure_reason` | `LOST` hoặc `DISPOSED` | Phase 04 (đóng ngoại lệ) |

Các cột còn lại của intake (`weight`, `length`, `width`, `height`, `receiver_user_id`, `receiver_province`, `receiver_district`) **chưa** được map trong entity `Shipment` — thuộc Phase 02.

---

## 2. Hợp đồng với Phase 03 — Hub & Linehaul Operations (Ngô Bá Vạn)

### 2.1 Bảng `incidents` — cần một entity duy nhất ⚠️

Hai lát cắt đều cần ghi vào bảng `incidents`, nhưng mỗi bên đã tạo shape riêng:

| Nguồn | Shape |
|---|---|
| `docs/schema.sql` (canonical, ADR-004) | `shipment_id, trip_id, hub_id, type, description, reported_by, status, created_at, resolved_at` |
| `V8__phase_03_linehaul_trip_schema.sql` | `transport_trip_id, hub_id, actor_user_id, incident_type, note, evidence_reference` + `CHECK (incident_type = 'TRIP_DELAY')` |
| `V12__phase_04_finance_reconciliation_schema.sql` | theo `docs/schema.sql` ✔ |

**Phase 04 đã giải quyết theo hướng canonical:**

- Entity `vn.iotstar.entity.Incident` map đúng bộ cột của `docs/schema.sql`.
- Entity `FinancialIncident` cũ đã bị **xoá** (tránh hai entity JPA trên cùng một bảng).
- `V12__phase_04_finance_reconciliation_schema.sql` tự động **bổ sung cột canonical** nếu bảng đã tồn tại với shape khác, và **xoá** ràng buộc `CK_incidents_driver_type` để bảng không bị giới hạn vào một loại sự cố.

**Việc cần làm ở Phase 03 (của Vạn) khi hợp nhất:**

1. Bỏ entity `vn.iotstar.entity.Incident` cũ, dùng entity canonical của Phase 04.
2. Bỏ phần `CREATE TABLE dbo.incidents` và `CK_incidents_driver_type` khỏi `V8__phase_03_linehaul_trip_schema.sql` (giữ lại phần `ALTER` để thêm cột).
3. Bổ sung cột canonical (`type`, `description`, `reported_by`, `status`, `created_at`, `resolved_at`, `trip_id`) vào entity, và ánh xạ `actor_user_id` → `reported_by`, `note` → `description`, `incident_type` → `type`.
4. Cập nhật `OperationalRealtimePublisher` và `IncidentRepository` cho entity mới.

### 2.2 `shipments.current_hub_id` — xung đột kiểu mapping ⚠️

| Lát cắt | Cách map |
|---|---|
| Phase 04 | `Long currentHubId` — cột số thuần |
| Phase 03 | `@ManyToOne Hub currentHub` — cùng cột `current_hub_id` |

Hai entity map cùng một cột sẽ gây lỗi JPA khi hợp nhất. **Đề xuất:** giữ `@ManyToOne Hub currentHub` của Phase 03 làm chuẩn (đúng với `@JoinColumn` và `FetchType.LAZY` theo chuẩn dự án), và Phase 04 chuyển sang dùng `shipment.getCurrentHub().getId()`. Việc này cần một lượt refactor nhỏ ở Phase 04, được ghi nhận tại đây.

### 2.3 `shipment_events.hub_id`

Phase 03 đã bổ sung `Hub hub` vào `ShipmentEvent` cùng nhóm factory `operational(...)`. Phase 04 **không** tạo entity `Hub` (thuộc Phase 03) và ghi event không kèm hub. Khi hợp nhất, cần thống nhất cách ghi event có hub.

### 2.4 Điểm tiếp nhận chặng giao hàng

Luồng last-mile của Phase 04 cần vận đơn ở trạng thái `AT_HUB` và một assignment `DELIVERY` còn hiệu lực cho shipper. Việc **Coordinator gán task** thuộc Phase 03 (`ROLE_HUB_STAFF` vị trí `COORDINATOR`). Phase 04 chỉ đọc và cập nhật assignment, không tạo assignment `DELIVERY` hay `RETURN_DELIVERY` mới.

---

## 3. Hợp đồng với Phase 05 — Tích hợp chéo

### 3.1 Trạng thái COD — hai bước

Workflow (Mục 67–69) tách rõ hai bước, và code Phase 04 giữ nguyên trạng thái này:

```text
reconcile()       -> COD = RECONCILED -> READY_FOR_PAYOUT   (chưa credit ví)
payoutReadyCod()  -> credit ví -> COD = PAID_TO_SENDER      (payout_at được set)
```

Hai bước là **hai transaction boundary riêng biệt**. Nếu một bước khác cần kích hoạt giải ngân, hãy gọi `FinanceReconciliationService.payoutReadyCod(codTransactionId)`, không credit ví trực tiếp.

### 3.2 Trạng thái phiếu đối soát theo item

`shipper_reconciliations.status` được quyết định **theo từng item**, không theo tổng (Mục 68). Một ca có một khoản thiếu và một khoản thừa bù nhau vẫn là `COMPLETED_WITH_DISCREPANCY`.

### 3.3 Wallet tách riêng

`wallets.balance` là số dư khả dụng, `wallets.locked_balance` là số dư đang khóa (ADR-001). Khi rút tiền:

```text
requestWithdrawal  -> balance -= amount, locked_balance += amount  (WITHDRAWAL_LOCK)
approveWithdrawal  -> locked_balance -= amount                     (WITHDRAWAL_DEDUCT)
rejectWithdrawal   -> locked_balance -= amount, balance += amount  (WITHDRAWAL_REFUND)
```

Không có số dư ví nào trên `users`.

---

## 4. Bằng chứng kiểm chứng

| Hạng mục | Kết quả |
|---|---|
| `mvn test` (không có SQL Server) | 124 test, 0 fail, 2 skip (2 test tích hợp cần môi trường SQL Server) |
| `mvn test` (có SQL Server thật) | **128 test, 0 fail, 0 skip** |
| Schema thực tế | 25 bảng, khớp `docs/schema.sql` |
| Migration Phase 04 | 6 script apply thành công, 0 lỗi |

Lệnh chạy kiểm chứng với SQL Server thật:

```bash
export UTEEXPRESS_SQLSERVER_URL='jdbc:sqlserver://localhost:1434;databaseName=ute_express;encrypt=false;trustServerCertificate=true;sslProtocol=TLSv1.2;characterEncoding=UTF-8'
export UTEEXPRESS_SQLSERVER_USERNAME=sa
export UTEEXPRESS_SQLSERVER_PASSWORD='<password>'
mvn test
```

**Lưu ý JDK:** dự án yêu cầu JDK 21 (`pom.xml` khai báo `java.version=21`). Trên JDK 25, Mockito/ByteBuddy không mock được `JwtService` và toàn bộ test sẽ báo lỗi — đây là giới hạn của công cụ test, không phải lỗi mã nguồn.

---

## 5. Danh sách thay đổi schema của Phase 04

Tất cả đều là cột **đã có** trong `docs/schema.sql`, không thêm bảng mới:

| Bảng | Cột | Lý do |
|---|---|---|
| `shipments` | `declared_value` | Bồi thường kiện mất (Mục 38) |
| `shipments` | `cancel_reason` | Lý do hủy (Mục 74) |
| `shipments` | `closure_reason` | `LOST` / `DISPOSED` (Mục 48, 74) |
| `withdrawal_requests` | `request_key` | Idempotency khi gửi lệnh rút tiền |
| `shipper_reconciliation_items` | `source_type`, `source_id` | Tham chiếu nguồn tiền (COD / phí) |
| `delivery_attempts` | `evidence_url` | Ảnh bằng chứng giao thất bại (Mục 46) |

Ngoài ra `shipper_reconciliation_items` được bổ sung ràng buộc unique `(source_type, source_id)` để một nguồn tiền không thể được đối soát hai lần.


