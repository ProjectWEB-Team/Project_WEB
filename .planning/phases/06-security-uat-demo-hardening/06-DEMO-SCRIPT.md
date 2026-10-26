# Phase 06 — Kịch bản demo UTEExpress (HARD-03, plan 06-06)

Nhánh mã nguồn: `feature/phase-06-hardening`. Tài liệu này là kịch bản để **người** chạy và ghi kết quả. Mỗi ô Result để `NOT_RUN` cho đến khi có người tự quan sát và ghi lại (D-05). Người soạn chưa chạy kịch bản này.

## 1. Mục đích và ranh giới bằng chứng

- Mục đích: chuẩn bị demo đồ án cho kịch bản chính (Workflow §106) và tám kịch bản ngoại lệ (Workflow §107) dưới **demo mode**.
- Demo mode **không phải production**: hai cờ demo và các mốc thời gian rút ngắn chỉ bật trong profile `demo`.
- Kết quả: chỉ ghi `PASS`/`FAIL` sau khi quan sát thật; điền cột Observed và Evidence (ảnh chụp, mã vận đơn, id bản ghi). Không ghi mật khẩu, OTP, token, cookie vào tài liệu.
- Kịch bản này là phần chuẩn bị demo của TV2; hướng dẫn kiểm thử Phase 05.1 của TV3 là tài liệu riêng (D-12).

## 2. Chuẩn bị

| Hạng mục | Cách làm |
|---|---|
| Mã nguồn | Checkout nhánh `feature/phase-06-hardening`, mở bằng Spring Tools (Maven → Update Project). Nếu chạy test Maven trên máy có JDK 25 thì dùng JDK 21/22 (Byte Buddy của dự án chưa hỗ trợ JDK 25). |
| Profile | Spring Tools → Run Configurations → Profiles: `demo,local` (thêm `otp-local` nếu cần đăng nhập OTP cục bộ), hoặc tham số `--spring.profiles.active=demo,local,otp-local`. |
| Cơ sở dữ liệu | Chỉ dùng database demo cục bộ `ute_express`. **Không** chạy bộ test, script reset hay `phase-05-integration-reset.sql` lên database này. Database kiểm thử riêng `uteexpress_phase05_test_*` chỉ dành cho test. |
| Dữ liệu mẫu | Chỉ khi thiếu bưu cục/vùng giao: chạy `docs/sql/local-demo-delivery-areas.sql` và/hoặc `docs/sql/local-demo-nationwide-2025.sql` (cả hai từ chối chạy nếu database không phải `ute_express`, chỉ thêm, không xóa). |
| Bí mật | Thông tin datasource/JWT nằm trong `application-local.properties` (git-ignored). Không dán vào tài liệu này. |

## 3. Thuộc tính demo mode

Các khóa thật trong `src/main/resources/application-demo.properties`:

| Khóa | Giá trị demo | Mặc định production | Tác dụng |
|---|---|---|---|
| `uteexpress.timeout.dropoff-hours` / `uteexpress.timeout.dropoff-minutes` | 0 / 2 | 48 giờ | Đơn SELF_DROPOFF không mang ra bưu cục sau 2 phút thì bị hủy `DROPOFF_TIMEOUT`. |
| `uteexpress.timeout.pickup-hours` / `uteexpress.timeout.pickup-minutes` | 0 / 2 | 48 giờ | Đơn HOME_PICKUP chưa lấy được sau 2 phút thì bị hủy `PICKUP_TIMEOUT`. |
| `uteexpress.timeout.disposed-grace-days` / `uteexpress.timeout.disposed-grace-minutes` | 0 / 2 | 7 ngày | Hàng hoàn về bưu cục gốc quá 2 phút thì Manager được duyệt tiêu hủy. |
| `uteexpress.delivery.attempt-day-lock` | false | true | Cho phép nhiều lần giao thất bại trong cùng một ngày. |
| `uteexpress.demo.fast-track-route-bypass` | true | false | Khi tra cứu tuyến tĩnh không ra đúng một quy tắc, sorter đi thẳng tới bưu cục đích của đơn (hoặc bưu cục gốc nếu là hàng hoàn); vẫn lưu ShipmentRoute + `SORTING_COMPLETED` như bình thường. |
| `uteexpress.demo.auto-linehaul-transit-simulation` | true | false | Bấm Start trip thì hệ thống ghi luôn arrival trong cùng giao dịch (trip `ARRIVED`); việc nhận hàng tại hub đích vẫn làm tay. |

**Thời gian chờ:** bộ quét timeout chạy mỗi `uteexpress.timeout.scan-interval-ms` = 60000 ms, bộ quét hoàn tiền mỗi `uteexpress.finance.refund-scan-interval-ms` (mặc định 60000 ms), bộ quét hàng hoàn mỗi `uteexpress.scheduler.timeout-check-interval-ms` = 30000 ms. Vì vậy sau mốc 2 phút cần chờ thêm tối đa khoảng 1–2 phút (tổng cộng khoảng 3–4 phút) mới thấy kết quả.

### Sơ đồ đặt tên app.demo → uteexpress (D-09)

Workflow ghi tên khóa `app.*`, nhưng Spring **bỏ qua im lặng** các khóa không có binding, nên chỉ các khóa `uteexpress.*` có tác dụng. Đừng chép khóa `app.*` vào file cấu hình.

| Workflow | Khóa có tác dụng |
|---|---|
| `app.demo.fast-track-route-bypass` | `uteexpress.demo.fast-track-route-bypass` |
| `app.demo.auto-linehaul-transit-simulation` | `uteexpress.demo.auto-linehaul-transit-simulation` |
| `app.demo.delivery-attempt-day-lock` | `uteexpress.delivery.attempt-day-lock` |
| `app.logistics.timeout.dropoff-expiration-minutes` | `uteexpress.timeout.dropoff-minutes` |
| `app.logistics.timeout.pickup-expiration-minutes` | `uteexpress.timeout.pickup-minutes` |
| `app.logistics.return.grace-period-minutes` | `uteexpress.timeout.disposed-grace-minutes` |
| `app.logistics.delivery.max-attempts` | `uteexpress.delivery.max-attempts` |
| `app.scheduler.timeout-check-interval-ms` | `uteexpress.scheduler.timeout-check-interval-ms` |

## 4. Vai trò và dữ liệu cần có

Chỉ ghi vai trò; không ghi tên đăng nhập hay mật khẩu.

| Tab | Vai trò | Hồ sơ nhân viên cần có |
|---|---|---|
| 1 | Người gửi `ROLE_USER` (đã xác thực SĐT) | — |
| 2 | Shipper lấy hàng `ROLE_SHIPPER` | Hồ sơ ACTIVE tại bưu cục gốc (khu Dĩ An) |
| 3 | Nhân viên hub gốc `ROLE_HUB_STAFF` | COORDINATOR, INBOUND, SORTER, OUTBOUND tại bưu cục gốc, hoặc một tài khoản GENERAL_STAFF |
| 4 | Tài xế `ROLE_LINEHAUL_DRIVER` | Hồ sơ ACTIVE, position `LINEHAUL_DRIVER`; có ít nhất một xe `AVAILABLE` |
| 5 | Nhân viên hub đích `ROLE_HUB_STAFF` | INBOUND, SORTER, COORDINATOR (hoặc GENERAL_STAFF) tại bưu cục Cần Thơ |
| 2' | Shipper giao hàng `ROLE_SHIPPER` | Hồ sơ ACTIVE tại bưu cục Cần Thơ |
| 6 | Manager `ROLE_MANAGER` | Hồ sơ ACTIVE ở hub cha bao trùm cả hai bưu cục |
| 7 | Admin `ROLE_ADMIN` | — |

Dữ liệu: bảng giá ACTIVE cho tuyến (dịch vụ, vùng gửi → vùng nhận); bưu cục giao hàng duy nhất cho địa chỉ nhận ở Cần Thơ; quy tắc định tuyến tĩnh giữa các hub (nếu thiếu, cờ bypass sẽ đi thẳng tới hub đích).

## 5. Kịch bản chính (Workflow §106)

Ghi mã vận đơn (UTX-…) và id đơn hiển thị ở trang chi tiết vào cột Evidence để dùng cho các bước sau.

| Bước | Vai trò | URL và thao tác | Cờ / thời gian | Quan sát cần ghi | Result | Observed | Evidence |
|---|---|---|---|---|---|---|---|
| 5.1 | USER (Tab 1) | `/user/shipments/create`: địa chỉ gửi Dĩ An, nhận Cần Thơ; COD 500000; người trả cước SENDER; phương thức VNPAY; HOME_PICKUP; bấm Báo giá | — | Hiện báo giá do server tính | NOT_RUN | | |
| 5.2 | USER | Bấm Tạo đơn (POST `/user/shipments`) | — | Chuyển tới `/user/shipments/{id}`, trạng thái `WAITING_FOR_PAYMENT` | NOT_RUN | | |
| 5.3 | USER | Trang chi tiết: Xác nhận thanh toán demo (POST `/user/shipments/{id}/confirm-payment`) | — | Thanh toán `PAID` (mã DEMO-…), trạng thái `WAITING_FOR_HANDOVER` | NOT_RUN | | |
| 5.4 | HUB_STAFF COORDINATOR hub gốc (Tab 3) | `/hub/pickups`: chọn shipper, bấm Phân công | Phải làm trong 2 phút (`uteexpress.timeout.pickup-minutes`) | Đơn rời hàng chờ; sự kiện `PICKUP_TASK_ASSIGNED` | NOT_RUN | | |
| 5.5 | SHIPPER (Tab 2) | `/shipper/tasks`: nhập mã vận đơn, bấm Hoàn tất lấy hàng | Trong 2 phút | Trạng thái `PICKED_UP`; USER nhận thông báo realtime | NOT_RUN | | |
| 5.6 | HUB_STAFF INBOUND hub gốc | `/hub/sorter`: form nhận hàng (POST `/hub/inbound`) với id đơn | — | Trạng thái `AT_HUB`; sự kiện `ARRIVED_AT_HUB` | NOT_RUN | | |
| 5.7 | HUB_STAFF SORTER | `/hub/sorter`: phân loại (POST `/hub/sorter`) | `uteexpress.demo.fast-track-route-bypass` nếu thiếu quy tắc tĩnh | Hiện mã hub kế tiếp; sự kiện `SORTING_COMPLETED`; không có lỗi "Sorting is temporarily unavailable" | NOT_RUN | | |
| 5.8 | HUB_STAFF OUTBOUND | `/hub/outbound`: tạo bao (containerCode, destinationHubId), nạp đơn vào bao (load), tạo bàn giao (destinationHubId, receiverDriverId, containerIds) | — | Bao `SEALED`; bàn giao `PENDING_ACCEPTANCE` | NOT_RUN | | |
| 5.9 | LINEHAUL_DRIVER (Tab 4) | Quét bao cho bàn giao: **GAP** — trang `/driver` không có form cho POST `/driver/handovers/scan` (controller có). Chỉ làm được bằng công cụ gửi POST kèm CSRF; ghi rõ cách đã dùng | — | Bàn giao `ACCEPTED` sau khi quét đủ bao | NOT_RUN | | |
| 5.10 | LINEHAUL_DRIVER | `/driver`: Accept handover (handoverId, chọn xe) | — | Trip `SCHEDULED`; xe `IN_USE` | NOT_RUN | | |
| 5.11 | LINEHAUL_DRIVER | `/driver`: Start trip (tripId) | `uteexpress.demo.auto-linehaul-transit-simulation` | Trip `ARRIVED` ngay; đơn `IN_TRANSIT`; sự kiện `LINEHAUL_TRIP_STARTED` rồi `LINEHAUL_ARRIVED`; nhân viên INBOUND hub đích nhận thông báo | NOT_RUN | | |
| 5.12 | HUB_STAFF INBOUND hub đích (Tab 5) | `/hub/inbound`: nhận từng kiện (POST `/hub/inbound/receive`) rồi đối soát bao (POST `/hub/inbound/reconcile`) | — | Đơn `AT_HUB` ở Cần Thơ; bàn giao/trip hoàn tất | NOT_RUN | | |
| 5.13 | HUB_STAFF SORTER hub đích | `/hub/sorter`: phân loại | — | Thông báo đã tới hub đích (không tạo tuyến mới) | NOT_RUN | | |
| 5.14 | HUB_STAFF COORDINATOR hub đích | `/hub/delivery-assignments`: chọn đơn và shipper giao | — | Sự kiện `DELIVERY_ASSIGNED` | NOT_RUN | | |
| 5.15 | SHIPPER giao hàng | `/shipper/delivery-tasks`: Quét xuất kho (scan) | — | Trạng thái `OUT_FOR_DELIVERY` | NOT_RUN | | |
| 5.16 | SHIPPER giao hàng | Ghi nhận thu tiền (collect): COD 500000, phương thức CASH | — | COD `COLLECTED`; sự kiện `COD_COLLECTED` | NOT_RUN | | |
| 5.17 | SHIPPER giao hàng | Xác nhận giao (confirm) | — | Trạng thái `DELIVERED` | NOT_RUN | | |
| 5.18 | MANAGER (Tab 6) | `/manager/reconciliation`: chọn shipper, ngày, CASH; nhập số thực thu = dự kiến; Submit | — | Ca đối soát hoàn tất, không lệch; COD `READY_FOR_PAYOUT` | NOT_RUN | | |
| 5.19 | MANAGER | Cùng trang: Payout COD (codTransactionId) | — | COD `PAID_TO_SENDER`; ví người gửi +500000 đúng một lần | NOT_RUN | | |
| 5.20 | USER (Tab 1) | Quan sát thông báo realtime; mở `/user/wallet` | — | Số dư tăng 500000; giao dịch `COD_PAYOUT` | NOT_RUN | | |
| 5.21 | USER | `/user/wallet`: Gửi yêu cầu rút tiền (≥ 10000) | — | Yêu cầu `PENDING`; `balance` giảm, `locked_balance` tăng | NOT_RUN | | |
| 5.22 | ADMIN (Tab 7) | `/admin/withdrawals`: Approve | — | Yêu cầu `COMPLETED`; `locked_balance` giảm; USER nhận thông báo | NOT_RUN | | |

## 6. Kịch bản ngoại lệ (Workflow §107)

Định dạng bảng giống mục 5. Bắt đầu mỗi case bằng một đơn mới để kết quả không lẫn nhau.

### 6.1 Case 1 — Self-dropoff quá hạn

Tiền điều kiện: USER có thể tạo đơn SELF_DROPOFF, VNPAY.

| Bước | Vai trò | URL và thao tác | Cờ / thời gian | Quan sát cần ghi | Result | Observed | Evidence |
|---|---|---|---|---|---|---|---|
| C1.1 | USER | `/user/shipments/create`: SELF_DROPOFF, SENDER trả, VNPAY; tạo đơn | — | `WAITING_FOR_PAYMENT` | NOT_RUN | | |
| C1.2 | USER | Xác nhận thanh toán demo | — | `PAID`, `WAITING_FOR_HANDOVER` | NOT_RUN | | |
| C1.3 | — | Không mang hàng ra bưu cục; chờ | `uteexpress.timeout.dropoff-minutes` = 2 + quét 60 s | Đơn `CANCELLED` lý do `DROPOFF_TIMEOUT` | NOT_RUN | | |
| C1.4 | USER | Chờ thêm một chu kỳ quét hoàn tiền; mở `/user/wallet` | refund scan 60 s | Ví +100% cước, một giao dịch `REFUND` duy nhất | NOT_RUN | | |

### 6.2 Case 2 — Pickup thất bại

| Bước | Vai trò | URL và thao tác | Cờ / thời gian | Quan sát cần ghi | Result | Observed | Evidence |
|---|---|---|---|---|---|---|---|
| C2.1 | USER | Tạo đơn HOME_PICKUP, VNPAY; xác nhận thanh toán demo | — | `WAITING_FOR_HANDOVER` | NOT_RUN | | |
| C2.2 | COORDINATOR hub gốc | `/hub/pickups`: phân công shipper | — | `PICKUP_TASK_ASSIGNED` | NOT_RUN | | |
| C2.3 | SHIPPER | `/shipper/tasks`: ghi lần liên hệ thất bại (FAILED_NO_ANSWER) | — | Sự kiện `PICKUP_ATTEMPT_FAILED` | NOT_RUN | | |
| C2.4 | — | Chờ | `uteexpress.timeout.pickup-minutes` = 2 + quét 60 s | `CANCELLED` lý do `PICKUP_TIMEOUT` | NOT_RUN | | |
| C2.5 | USER | `/user/wallet` sau chu kỳ hoàn tiền | refund scan | Ví +100% cước, một lần | NOT_RUN | | |

### 6.3 Case 3 — Giao hàng thất bại 3 lần

Tiền điều kiện: một đơn đã ở `OUT_FOR_DELIVERY` (làm như 5.1–5.15), có COD.

| Bước | Vai trò | URL và thao tác | Cờ / thời gian | Quan sát cần ghi | Result | Observed | Evidence |
|---|---|---|---|---|---|---|---|
| C3.1 | SHIPPER giao | `/shipper/delivery-tasks`: Failure, lý do NO_ANSWER (lần 1) | `uteexpress.delivery.attempt-day-lock`=false | Lần thử 1 được ghi | NOT_RUN | | |
| C3.2 | INBOUND hub đích | `/hub/inbound`: nhận lại kiện từ shipper (POST `/hub/delivery-returns/receive`) | — | Đơn về hub, `RETURNED_TO_HUB` | NOT_RUN | | |
| C3.3 | COORDINATOR + SHIPPER | Phân công lại (`/hub/delivery-assignments`), scan, Failure lần 2; lặp lại cho lần 3 | Cùng ngày nhờ day-lock tắt | Sau lần 3: `FAILED` lý do `DELIVERY_ATTEMPTS_EXHAUSTED`, chuyển `RETURN_TO_SENDER`, COD bị hủy, có phí hoàn | NOT_RUN | | |
| C3.4 | Nhân viên các hub | Đưa hàng hoàn qua các bước sorter/outbound/driver như mục 5 (chiều RETURN) | bypass + auto transit | Hàng về bưu cục gốc; sự kiện `RETURNED_AT_ORIGIN_HUB` | NOT_RUN | | |
| C3.5 | USER | `/user/shipment-fees/{id}`: trả phí hoàn (ví hoặc VNPay demo) | — | Phí hoàn `PAID` | NOT_RUN | | |
| C3.6 | INBOUND hub gốc | `/hub/return-collection`: trao hàng cho người gửi (SELF_PICKUP, ghi giấy tờ đã kiểm) | — | `RETURNED_TO_SENDER` | NOT_RUN | | |

### 6.4 Case 4 — Thiếu kiện khi dỡ bao

50/49 kiện khó làm tay; dùng bản thu nhỏ: một bao có **2** kiện, ở hub đích chỉ nhận 1.

| Bước | Vai trò | URL và thao tác | Cờ / thời gian | Quan sát cần ghi | Result | Observed | Evidence |
|---|---|---|---|---|---|---|---|
| C4.1 | Hub gốc + driver | Làm 5.1–5.11 với 2 đơn cùng bao | auto transit rút ngắn vận chuyển | Trip `ARRIVED` | NOT_RUN | | |
| C4.2 | INBOUND hub đích | `/hub/inbound`: nhận 1 kiện (receive) | — | Kiện đó `AT_HUB`, `ARRIVED_AT_HUB` | NOT_RUN | | |
| C4.3 | INBOUND hub đích | Đối soát bao (reconcile) chỉ đánh dấu kiện đã nhận | — | Kiện thiếu `IN_TRANSIT` + on hold; incident `MISSING_PARCEL`; bàn giao/trip `COMPLETED_WITH_DISCREPANCY` | NOT_RUN | | |
| C4.4 | MANAGER, driver | Quan sát thông báo realtime | — | Manager hai đầu và tài xế được báo đúng một lần; người khác không nhận | NOT_RUN | | |

### 6.5 Case 5 — COD lệch tiền

| Bước | Vai trò | URL và thao tác | Cờ / thời gian | Quan sát cần ghi | Result | Observed | Evidence |
|---|---|---|---|---|---|---|---|
| C5.1 | SHIPPER giao | Giao thành công ít nhất 2 đơn COD trong ca (như 5.15–5.17) | — | COD `COLLECTED` | NOT_RUN | | |
| C5.2 | MANAGER | `/manager/reconciliation`: nhập thực thu đúng cho một đơn, thiếu 50000 cho đơn kia; Submit | — | Ca `COMPLETED_WITH_DISCREPANCY`; đơn đủ `READY_FOR_PAYOUT`; đơn thiếu `HOLD_DISPUTE`; có financial incident gắn shipper; không có bảng nợ shipper | NOT_RUN | | |
| C5.3 | MANAGER | Thử Payout đơn `HOLD_DISPUTE` | — | Bị từ chối | NOT_RUN | | |

### 6.6 Case 6 — Hàng hoàn bị bỏ

| Bước | Vai trò | URL và thao tác | Cờ / thời gian | Quan sát cần ghi | Result | Observed | Evidence |
|---|---|---|---|---|---|---|---|
| C6.1 | — | Có đơn hoàn đã thật sự về bưu cục gốc (C3.4) và chưa trả phí | — | `RETURN_TO_SENDER` tại hub gốc | NOT_RUN | | |
| C6.2 | — | Chờ | `uteexpress.timeout.disposed-grace-minutes` = 2 + quét 30 s | Đơn xuất hiện ở trang quá hạn | NOT_RUN | | |
| C6.3 | MANAGER | `/manager/overdue-returns`: nhập lý do, bấm xử lý | — | `CLOSED_EXCEPTION` lý do `DISPOSED`; trang chuyển về `/` | NOT_RUN | | |

### 6.7 Case 7 — Khách từ chối nhận (fast-fail)

| Bước | Vai trò | URL và thao tác | Cờ / thời gian | Quan sát cần ghi | Result | Observed | Evidence |
|---|---|---|---|---|---|---|---|
| C7.1 | SHIPPER giao | Đơn ở `OUT_FOR_DELIVERY`; `/shipper/delivery-tasks`: Failure, lý do `RECEIVER_REFUSED`, đính kèm ảnh bằng chứng | — | `FAILED` ngay lần 1, chuyển `RETURN_TO_SENDER`, không yêu cầu lần 2 và 3 | NOT_RUN | | |
| C7.2 | USER (người gửi) | Quan sát thông báo | — | Nhận thông báo realtime | NOT_RUN | | |

### 6.8 Case 8 — Mất hàng và bồi thường

| Bước | Vai trò | URL và thao tác | Cờ / thời gian | Quan sát cần ghi | Result | Observed | Evidence |
|---|---|---|---|---|---|---|---|
| C8.1 | — | Có incident `MISSING_PARCEL` từ C4.3 (giá trị khai báo ví dụ 500000, cước người gửi đã trả) | — | Kiện on hold | NOT_RUN | | |
| C8.2 | MANAGER có phạm vi | `/manager/incidents/missing`: chọn incident, outcome `CONFIRMED_LOST`, ghi bằng chứng điều tra | — | Incident ghi kết quả điều tra | NOT_RUN | | |
| C8.3 | MANAGER | `/manager/reconciliation`: Compensate (shipmentId, incidentId, lý do) — tạo COMPENSATION | — | Ví người gửi + giá trị khai báo + cước đã trả, mỗi khoản đúng một lần; `CLOSED_EXCEPTION` lý do `LOST`; incident `RESOLVED` | NOT_RUN | | |
| C8.4 | MANAGER | Bấm Compensate lần nữa | — | Không cộng tiền lần hai | NOT_RUN | | |

## 7. Giới hạn đã biết

- Quyết định do cờ demo tạo ra dùng đúng loại sự kiện bình thường, nên lịch sử không phân biệt được với thao tác tay (F-06-D-01).
- Cờ demo là cấu hình của người vận hành; biến môi trường cũng bật được (F-06-D-02).
- Chỉ khóa `uteexpress.*` có tác dụng; khóa `app.*` trong Workflow bị bỏ qua (F-06-D-03).
- Hoàn tiền VNPay thật và chi trả ngân hàng thật nằm ngoài phạm vi; VNPay/TRANSFER chỉ là bằng chứng demo/thủ công do Manager ghi.
- Trang `/driver` không có form quét bao cho bàn giao (bước 5.9, GAP) và không liệt kê handover/trip được giao, nên phải nhập id tay.
- Sau khi tiêu hủy hàng hoàn hoặc thu phí hoàn tiền mặt, trang chuyển về `/` thay vì quay lại trang thao tác.

## 8. Mẫu ghi phát hiện

| Case hoặc bước | Kỳ vọng | Quan sát | Mức độ | Bằng chứng | Quyết định của owner |
|---|---|---|---|---|---|
| | | | | | |
