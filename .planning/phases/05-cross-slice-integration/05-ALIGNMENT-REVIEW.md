# Phase 05 — Kết quả đối chiếu với Phase 01–04, Workflow và database

**Ngày rà soát:** 2026-10-25  
**Snapshot:** `main` tại `ca2bbf8`  
**Kết luận:** `GAPS_FOUND` — research đã có kết quả; cần lập lại plan theo các khoảng trống tích hợp thực tế trước khi execute.

## 1. Trạng thái thực tế

- Phase 01 có `01-VERIFICATION.md` và `01-UAT.md` đã hoàn tất trong lịch sử.
- Source Phase 02 đã merge tại `cda0f32`; source Phase 04 đã merge tại `ca2bbf8`, trên nền Phase 03. Vì vậy phần “chưa merge” trong `v1-MILESTONE-AUDIT.md` là snapshot lịch sử, không còn mô tả checkout hiện tại.
- Có source của cả ba lát cắt không đồng nghĩa đã chứng minh được hành trình nối các lát cắt. Phase 02/04 chưa có SUMMARY/VERIFICATION tương ứng trong `.planning`; Phase 03 có SUMMARY nhưng chưa có `03-VERIFICATION.md`.
- Đã tồn tại bốn `05-01..04-PLAN.md` trước phiên rà soát này. Chúng chủ yếu đề xuất thêm fixture và test trên giả định service đã nối được với nhau. Các finding bên dưới bác bỏ giả định đó. Cần replan; không dùng chúng như một bộ plan đã được phê duyệt để execute.
- `.planning/codebase/*` là bản khảo sát ngày 01/10, khi dự án mới là bootstrap; không dùng các mô tả “chưa có service/entity” ở đó làm hiện trạng.

## 2. Bằng chứng kiểm thử trong phiên này

| Kiểm tra | Kết quả |
|---|---|
| `rtk mvn test` | `BUILD SUCCESS`, 337 test được báo cáo, 0 failures, 0 errors |
| Thực sự chạy | **317 test đạt** |
| Bị bỏ qua theo điều kiện môi trường | **20 test SQL Server**, thuộc 9 suite |
| Môi trường Maven | Maven 3.9.11, Oracle JDK 22; `pom.xml` đặt Java target 21 |
| Database SQL Server đang chạy / schema thực tế / dữ liệu thực tế | **Chưa kiểm chứng: `BLOCKED_BY_ENVIRONMENT`** |
| Browser và STOMP end-to-end hiện tại | **Chưa thực hiện trong phiên này** |

Các suite SQL bị bỏ qua: Phase03Custody (1), Phase03ManagerScope (1), Phase03Receipt (1), Phase03Routing (2), Phase03Trip (1), Phase04Finance (1), Phase04Wallet (4), Registration (6), Shipment (3). Số liệu lấy từ `target/surefire-reports/TEST-*.xml` sau khi Maven kết thúc.

Phiên chạy không có các biến môi trường `UTEEXPRESS_SQLSERVER_URL`, `UTEEXPRESS_SQLSERVER_USERNAME`, `UTEEXPRESS_SQLSERVER_PASSWORD`, `UTEEXPRESS_JWT_SECRET`. Không suy ra kết quả SQL Server của checkout hợp nhất từ các lần chạy riêng từng nhánh trước đây. Trong phiên này chỉ đối chiếu tĩnh Workflow/ERD/DDL/entity/repository, chưa áp dụng migration lên database.

## 3. Những hợp đồng nền tảng vẫn khớp

- Giữ đúng Spring Boot/JPA, SQL Server, JSP/JSTL; không cần thay stack để tích hợp.
- Sáu role và tập 13 `ShipmentStatus` vẫn có định nghĩa chung; event được map qua `shipment_events.event_type`.
- Ví dùng `wallets`, có `balance` và `locked_balance`; không tạo bảng `shipper_debts`.
- `Incident` hiện dùng shape canonical chung, thay vì hai entity vận hành/tài chính tranh chấp cùng bảng.
- `current_hub_id` có một mapping ghi bằng ID và một association đọc; đây không còn là lỗi duplicate writable column như handoff cũ lo ngại.
- Có các seam service, transaction, actor server-side và STOMP riêng tư để tái sử dụng. Việc có seam không chứng minh tất cả producer/consumer và scope đều đã đúng.

Authority áp dụng: ADR > Workflow > ERD > Knowledge Base > Engineering Standards. Đặc biệt ADR-004 xác định phần Database trong **Workflow** là nguồn field-level; `docs/schema.sql` là DDL cần được kiểm tra, không phải authority cao hơn Workflow.

## 4. Các điểm đứt luồng cần đưa vào plan mới

| Mã | Bàn giao | Canonical yêu cầu / source hiện tại | Hướng xử lý cần plan |
|---|---|---|---|
| A-01 | Intake → Routing | `ShipmentServiceImpl:94–122` và `Shipment.applyIntake:94–105` lưu địa chỉ người nhận nhưng không gán destination grouping / destination hub. `HubOperationsServiceImpl:115–120` cần destination grouping để lookup. | Chuẩn hóa và xác định đích bằng dữ liệu server-side trước khi routing; chứng minh điểm dừng tại Delivery Hub đích. Không điền hộ các field nghiệp vụ bằng fixture rồi gọi đó là E2E. |
| A-02 | Self-dropoff → Origin Hub | Workflow mục 53 yêu cầu tiếp nhận kiện và thu phí cash tại Hub. Shipment mới chưa có current hub; generic inbound yêu cầu bằng chứng hub đã gắn trước đó (`HubOperationsServiceImpl:159–167`). First-mile hiện có seam pickup nhưng thiếu seam hoàn chỉnh cho self-dropoff mới tạo. | Bổ sung tiếp nhận có scope, xác thực payment/handover, gán origin/location và event nguyên tử. Không bỏ kiểm tra scope để làm demo chạy. |
| A-03 | Handover → Đang vận chuyển → Receipt | `LinehaulTripServiceImpl:74–82,143–152` chỉ đổi trip và append event, chưa đổi shipment sang `IN_TRANSIT`. `DestinationReceiptServiceImpl:109–115` chốt handover nhưng chưa chốt container/trip theo Workflow mục 37. | Hoàn thiện state/custody tại từng mốc, container unpacked và trip terminal; thống nhất tên trạng thái trip/handover với Workflow. |
| A-04 | Destination Hub → Shipper | Last-mile cần assignment `DELIVERY`; chưa có luồng production tạo assignment lần đầu bởi Coordinator. Assignment `RETURN_DELIVERY` cũng chưa có producer production. Factory/entity/test fixture không thay thế thao tác nghiệp vụ. | Thêm đầy đủ service/controller/JSP phân công theo position, hub, địa điểm, trạng thái và chống phân công trùng; tái sử dụng assignment chung. |
| A-05 | Thiếu kiện → Điều tra → Khôi phục/bồi thường | `DestinationReceiptServiceImpl:78–85` ghi missing item/incident nhưng chưa đặt shipment `on_hold`; `receiveItem:60` chặn missing item và chưa có recovery riêng. Workflow mục 38 yêu cầu tìm thấy thì nhập kho hợp lệ, mất thật mới đóng ngoại lệ/bồi thường. | Plan riêng cho found/lost recovery, liên kết đúng incident/shipment/custody, authority và terminal outcome; không sửa trực tiếp DB để thoát ngoại lệ. |
| A-06 | Hủy/timeout → Hoàn ví | `ShipmentCancellation:46–53` tạo nghĩa vụ outbox. `ShipmentFinanceServiceImpl.refundShipmentPayment:64–108` có refund nhưng chưa settle nghĩa vụ đó trong cùng transaction. | Nối producer/consumer outbox; wallet credit, payment REFUNDED và trạng thái nghĩa vụ commit cùng nhau, có retry và chống trùng. |
| A-07 | Return → Origin → Bàn giao/tiêu hủy | Cần route RETURN, custody, khoản phí đúng và mốc về Origin Hub trước khi bắt đầu grace period. Các service return còn thiếu bằng chứng vị trí/hub và điều kiện quá hạn khi disposal. | Hoàn thiện cả self-pickup và RETURN_DELIVERY, thu phí theo Workflow mục 48/64/65, bảo đảm không giao trả/tiêu hủy trước điều kiện. |
| A-08 | Collection → Đối soát → Ví | Cần đối chiếu nguồn tiền server-side, nguồn thuộc đúng shipment/shipper, amount thực tế, scope Manager và incident đúng tham chiếu. Một số đường hiện tin dữ liệu đối soát đầu vào hoặc chưa ràng buộc scope/tham chiếu đầy đủ. | Đưa các sửa lỗi tài chính đã xác nhận trong `05-RESEARCH.md` vào files/tasks cụ thể, kèm rollback, replay, concurrent ledger proof. Không coi đây chỉ là bài test happy path. |
| A-09 | Events → Tracking / STOMP | `TrackingServiceImpl:31–37,102–106` chỉ cho qua sáu event intake; event vận hành/giao hàng không xuất hiện trong timeline công khai/người nhận. Notification tài chính cần người nhận đúng nghiệp vụ, không mặc định là actor thực hiện. | Thêm public-label mapping an toàn cho hành trình sau intake và server-resolved recipient; kiểm tra DTO tối thiểu và sau commit. |

Chi tiết source, tác động, mức tin cậy và hướng kiểm chứng bổ sung nằm trong [05-RESEARCH.md](05-RESEARCH.md). Các finding trên là kết luận từ source, chưa phải báo cáo tái hiện lỗi trên database thật.

## 5. Database: schema tạo mới và schema nâng cấp chưa tương đương

| Hợp đồng | Bằng chứng tĩnh | Hệ quả cần xác minh trên SQL Server |
|---|---|---|
| `transport_trips.vehicle_id` | Workflow mục 92 và `docs/schema.sql:243` có vehicle; DDL canonical bắt `NOT NULL`. `TransportTrip` hiện không map vehicle; các migration Phase 03 không nối trường này vào tạo trip. | Trip có thể không INSERT được trên DB tạo từ canonical base; DB do migration bootstrap tạo ra có thể che lỗi do thiếu cột/ràng buộc. Cần seam gán vehicle hợp lệ. |
| `handover_containers.scanned_at` | `docs/schema.sql:305` là `NOT NULL`; migration tạo bảng mới (`phase-03-container-handover-schema.sql:61`) lại cho NULL; `HandoverContainer.expected()` chưa có thời gian scan. | Cần xác định ngữ nghĩa pending/scan và forward migration thống nhất; không mặc định default SQL sẽ chạy khi JPA gửi NULL. |
| Event column | `docs/schema.sql` còn `shipment_events.status`; ADR-003/Workflow dùng `event_type`; migration Phase 01 mới chuyển tiếp dữ liệu. | Base DDL một mình chưa đủ để chạy ứng dụng hiện tại. Phải có manifest thứ tự migration và kiểm tra chạy lại. |
| Nguồn migration | Các SQL test Phase 03 chủ yếu áp script/kiểm tra metadata; Phase 04 giả định nhiều schema đã có sẵn. Chưa có builder Phase 05 chung. | Phải kiểm cả clean canonical base và legacy upgrade, không tạo schema tối giản riêng từng suite. |

Đây là rủi ro tương thích có bằng chứng tĩnh; chưa gắn nhãn “SQL lỗi đã tái hiện”. Cần chọn database kiểm thử riêng, chạy migration hai lần, kiểm FK/nullability/index/trigger, sau đó chạy journey bằng service thật. Không chạy nguyên `docs/schema.sql` lên DB bất kỳ: script chứa `CREATE DATABASE` và `USE [ute_express]`, không phải reset script an toàn/idempotent cho mọi môi trường.

## 6. Độ phủ yêu cầu Phase 05

| Requirement | Kết luận hiện tại | Điều kiện để xác nhận |
|---|---|---|
| `INTEG-01` | Chưa đạt; luồng normal còn thiếu producer/consumer và state/custody transitions. | Sửa A-01..04/A-09 và schema; journey thật từ tạo đơn qua service tới delivery, payout/tracking. |
| `INTEG-02` | Chưa đạt; exception chưa đạt terminal outcome xuyên lát cắt. | Sửa A-05..08; expiry/refund, delivery retry/return, missing found/lost, COD discrepancy và compensation có atomicity/idempotency. |
| `INTEG-03` | Có Manager operations và Admin withdrawal, chưa đủ bằng chứng aggregate/scope xuyên vận hành và tài chính. | Service-scope theo subtree/assignment/resource, negative tests và thông báo đúng người; browser/STOMP UAT có evidence. |

## 7. Đầu vào cho bước lập plan tiếp theo

Thứ tự phụ thuộc nên là: **hợp đồng DB/fixture → sửa bàn giao intake/routing → hoàn thiện linehaul/receipt/assignment → normal tracer → exception/refund/return/finance → tracking/realtime/management và UAT**. Đây là định hướng từ research, không phải một bộ PLAN mới.

Plan mới phải khai báo production files bị sửa cho từng finding, tiêu chí acceptance cụ thể, actor/scenario và lệnh kiểm chứng. Bốn plan hiện có chỉ khai báo test/fixture là không đủ cho những thay đổi production đã phát hiện. Không đánh dấu Phase 02–04 hoàn thành hồi tố để ép dependency pass; phân biệt rõ source đã merge, formal verification còn thiếu và lỗi tích hợp còn mở.

Phạm vi Phase 06 vẫn giữ exhaustive security matrix, demo hardening và UAT toàn hệ thống. Những kiểm tra scope/transaction là điều kiện trực tiếp để một handoff Phase 05 hoạt động đúng thì cần xử lý ở Phase 05.
