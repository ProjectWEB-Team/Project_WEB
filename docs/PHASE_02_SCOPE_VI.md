# Phase 02 — Bạn cần làm những gì?

> Người phụ trách theo tài liệu nhóm: **Hoàng Phúc Khang — Person A**.
> Đối chiếu GitHub ngày **22/10/2026**, sau `git fetch origin`.
> Đã cập nhật phần account theo yêu cầu người dùng ngày **22/10/2026**: OTP điện thoại hoặc đăng nhập Google, cấu hình nhà cung cấp sau. Quyết định được ghi ở [ADR-016](ARCHITECTURE_DECISIONS.md#adr-016--customer-phone-otp-and-google-oidc-sign-in); không ghi nhận thay phê duyệt của trưởng nhóm.
> Mã account, giao diện và liên kết người nhận cũ đã có bằng chứng SQL; cấu hình provider thật và browser UAT vẫn còn. Hướng dẫn chạy: [PHASE_02_ACCOUNT_SETUP.md](PHASE_02_ACCOUNT_SETUP.md).
> **Cập nhật local ngày 23/10/2026 sau wizard:** 171 test đạt, gồm 11 SQL Server tests, không test nào bị bỏ qua; WAR build thành công. Wizard 4 bước, báo giá gắn với phiên và thời điểm chờ bàn giao đã có. 10 kiểm tra Chrome trên trang Jasper với dữ liệu kiểm thử đạt. Đang chờ bảng giá nhóm để UAT báo giá/tạo đơn thật. Xem mục 9 và [checklist](PHASE_02_DIRECTOR_CHECKLIST.md). Các commit remote dưới đây vẫn là snapshot ngày 22/10, không phải lần fetch mới.

> **Cập nhật 23/10/2026 (tối):** đã code 02-05, 02-06, 02-07, 02-09 và phần realtime/unlock của 02-08: phân công lấy hàng cùng hub, shipper ghi lần liên hệ và quét mã hoàn tất (thu tiền mặt người gửi một lần), hủy đơn của người gửi, job timeout 48 giờ, outbox hoàn tiền `PENDING_WALLET_CREDIT`, tra cứu công khai `/tracking` và `/api/tracking/{mã}`, danh sách đơn đã gửi, “Đơn đang đến” có che và mở khóa bằng mã vận đơn đầy đủ. 193 test chạy, 0 lỗi, 9 test SQL bị bỏ qua vì môi trường build không có SQL Server. Ô ✔ chỉ ghi nhận mã đã có; SQL evidence, UAT và review vẫn còn. Chi tiết và các mặc định đã chọn: nhật ký cuối [checklist](PHASE_02_DIRECTOR_CHECKLIST.md).

## 1. Kết quả bạn phải bàn giao

Bạn làm **User & Shipment Intake**: khách đăng ký/xác thực → tạo vận đơn đầy đủ → tính cước/chọn người trả → thanh toán mô phỏng hoặc chờ thu đúng thời điểm → lấy hàng/tự mang ra bưu cục → theo dõi đơn theo đúng quyền riêng tư.

Mỗi chức năng do bạn phụ trách phải đi xuyên:

```text
JSP/JSTL + Bootstrap
→ Controller → DTO → Service/ServiceImpl
→ Repository/JPA → SQL Server
→ phân quyền/phạm vi dữ liệu → realtime khi cần → test và UAT
```

Bạn có trách nhiệm database và backend cho phần của mình, cùng với giao diện. Không phải thiết kế lại toàn bộ database hoặc làm hết giao diện của Phase 03/04.

Nguồn quyết định khi có khác biệt: **ADR → Workflow → ERD → Knowledge Base → Engineering Standards → kế hoạch/checklist**. Ví dụ bài giảng, ảnh giao diện và ghi chú bàn giao không tự thay thế nghiệp vụ chuẩn.

## 2. Hiện trạng thực tế của các nhánh

| Nhánh đã fetch | Commit đối chiếu | Thực tế liên quan đến bạn |
|---|---|---|
| `origin/main` | `358655c` | Nền Phase 01: login session/JWT, tạo/xem đơn tối thiểu, security, lỗi an toàn, STOMP riêng tư. |
| `origin/project-plans` | `e2bc2e0` | Có requirements, roadmap, context/research và 9 plan Phase 02; đây là tài liệu kế hoạch. |
| `origin/feature/hub-linehaul` | `03dd999` | Có mã Phase 03: hub, nhân sự, routing, inbound, container, custody, trip, receipt, dashboard/audit; có migration và test. |
| `origin/feature/phase-04-last-mile-finance` | `8f7bf46` | Có mã Phase 04: delivery/return, payment, assignment, COD, ví/ledger, đối soát, rút tiền và tài liệu bàn giao. |

Hai nhánh tính năng đều đang đi trước `main` 6 commit và chưa nằm trong `main` tại thời điểm rà soát. Có mã hoặc báo cáo test trên nhánh riêng **không đồng nghĩa** đã tích hợp và được kiểm chứng chung trên máy bạn.

Nhánh làm việc hiện tại: `feature/phase-02-intake`, nền `358655c`. Chưa merge/cherry-pick Phase 03/04. Đã có account/OTP/Google, late receiver linking và phần intake/quote/payment. Các bảng/cột cần dùng trên DB `ute_express` đã được kiểm tra và 11 SQL integration tests đã đạt. Provider thật và browser UAT còn thiếu; kết quả này không nghiệm thu thay phần nghiệp vụ chưa triển khai.

Phần đã bổ sung và phần Phase 02 còn thiếu:

- `User` đã có dữ liệu đăng ký, timestamp xác thực phone/email và Google `sub`; repository có tra cứu/kiểm tra trùng, phone Việt Nam được chuẩn hóa về `+84...`.
- OTP có mã băm, expiry, giới hạn thử/gửi lại, purpose và session binding; delivery thật chưa tích hợp. Google dùng OIDC, credentials được bật qua profile riêng, không dùng SMTP.
- Giao diện có đăng ký, xác minh, đăng nhập OTP, Google onboarding và đường đăng nhập mật khẩu hiện có. Google chỉ xác thực email; phone liên hệ chưa xác thực không được dùng đăng nhập OTP hay nhận quyền xem đơn.
- `ShipmentServiceImpl` đã có báo giá từ policy `ACTIVE`, tạo shipment/payment và xác nhận thanh toán demo. Đã lưu `waiting_for_handover_at`; còn thiếu các phần lifecycle khác/hub/deadline/version, chốt quy tắc giá và lưu dấu dịch vụ/policy để hoàn tất intake.
- `ShipmentRepository` chưa có truy vấn danh sách theo sender/receiver, tracking và timeout.
- `ShipmentServiceImpl.getDetail(...)` chỉ phục vụ người gửi sở hữu đơn; không dùng DTO đó cho public/recipient.
- Late recipient linking đã có bằng chứng SQL cho snapshot, rollback và claim đồng thời; xác nhận payment cũng đã được khóa và kiểm thử đồng thời. Form tạo đơn đã là wizard 4 bước, giữ báo giá trong session và kiểm tra lại giá trước khi ghi đơn. First-mile service, timeout, refund outbox và tracking riêng tư mở rộng vẫn chưa có.

## 3. Chín gói công việc bắt buộc

### 02-01 — Account: OTP điện thoại và Google

- [x] Ghi yêu cầu người dùng ở ADR-016: chỉ tạo `ROLE_USER`; nhánh phone bắt đầu `INACTIVE`, OTP hợp lệ mới thành `ACTIVE`; nhánh Google có verified email được tạo account `ACTIVE` với phone chưa xác thực.
- [x] Viết `phase-02-account-otp.sql`: thêm `phone_otp_challenges`, timestamps xác thực và Google subject; kiểm tra dữ liệu legacy trước DDL. Chính sách hiện tại: 5 phút, 5 lần thử, 60 giây gửi lại, 5 lần gửi/giờ.
- [x] Mở rộng `User`, chuẩn hóa SĐT Việt Nam, tra cứu/kiểm tra trùng username/phone/email. Schema account và các giao dịch đã được kiểm tra trên SQL Server local.
- [x] BCrypt cho password và OTP; challenge gắn tài khoản, purpose, expiry, dùng một lần; session giữ challenge ID.
- [x] Có `OtpDelivery` và profile `otp-local` để mô phỏng bằng file local bị Git ignore; mặc định tắt delivery, không trả/log mã trong HTTP thông thường.
- [x] Giới hạn DTO/binding; role/status/user ID do browser gửi không cấp quyền hoặc kích hoạt account.
- [x] Google OIDC dùng verified ID token và `sub`, có onboarding; không tự ghép tài khoản theo email/phone và không xem Google là xác thực SĐT.
- [x] Kiểm tra các bảng/cột account sau migration trên `ute_express` và chạy 6 account/linking SQL Server integration tests thành công.
- [ ] Cấu hình Google client thật và kiểm chứng đăng nhập/callback trong trình duyệt.
- [ ] Chọn và viết adapter gửi OTP điện thoại thật; cấu hình credentials và kiểm thử delivery.
- [ ] Hoàn tất UAT account và review của nhóm; không coi các ô mã đã viết là nghiệm thu toàn bộ gói.

Đầu ra đã có: `RegistrationService`, `PhoneOtpChallenge`, DTO/repository, `GoogleAccountService`, Google success handler, `AccountSessionAuthenticator`, migration, unit/MVC/security tests. Kết quả test thực tế được ghi ở checklist theo phiên; test dùng mock không chứng minh transaction/ràng buộc SQL Server đã chạy đúng.

**Điểm cần lưu ý:** “Google/Gmail” trong lựa chọn đăng nhập là **Google OAuth2/OIDC**, không phải gửi OTP vào Gmail. Không cần SMTP cho nhánh này. OTP điện thoại vẫn cần SMS/Zalo hoặc provider phù hợp; file local chỉ phục vụ kiểm thử phát triển.

### 02-02 — Liên kết người nhận và giao diện đăng ký

- [x] JSP đăng ký/xác minh/đăng nhập điện thoại và hoàn tất hồ sơ Google; controller gọi service.
- [x] Validation, CSRF, thông báo escaped, POST thành công dùng PRG; không echo password/OTP; có MVC/security tests.
- [x] Khi OTP kích hoạt account, gọi conditional update trong cùng transaction cho phone khớp và `receiver_user_id IS NULL`; hiện hỗ trợ ba biểu diễn `+84…`, `0…`, `84…`.
- [x] Câu update chỉ đổi liên kết receiver, giữ nguyên các cột snapshot và sender ownership; đã kiểm tra trên SQL Server.
- [x] Chặn claim đồng thời, claim sai phone, inactive account hoặc ghi đè receiver đã liên kết; có unit/SQL tests theo từng điều kiện.
- [x] Test account MVC/security và SQL cho activation/linking, gồm rollback và hai transaction tranh claim.
- [ ] Hoàn thiện recipient tracking với dữ liệu masked; kiểm thử quyền sau linking và browser UAT.

Đã có `RegistrationController`, `GoogleRegistrationController`, `ReceiverLinkService` và các JSP account. **Late linking đã có test SQL/concurrency/rollback**; browser UAT và recipient tracking còn mở nên 02-02 và `INTAKE-01` chưa được nghiệm thu toàn bộ. Google email verification không được thay điều kiện xác thực phone cho linking. Snapshot phone cũ chứa dấu cách/gạch nối cần được kiểm tra riêng.

### 02-03 — Intake đầy đủ, tính cước và payment records

- [x] Sender identity lấy từ `AuthenticatedActor`, không nhận sender ID làm bằng chứng quyền từ request.
- [ ] Nhận và kiểm tra dữ liệu sender/receiver; lưu snapshot không thay đổi theo profile.
- [ ] Auto-link receiver đã có account khi tạo đơn theo quy tắc phone chuẩn hóa được chấp thuận.
- [ ] Lưu địa chỉ nhận, tỉnh/quận, cân nặng, dài/rộng/cao, khai giá, COD, chính sách đồng kiểm, lựa chọn dịch vụ, người trả phí và phương thức first-mile.
- [ ] Map đầy đủ các field intake trong `Shipment`, deadline/lifecycle timestamp cần thiết và optimistic version.
- [ ] Tra `pricing_policies` đang `ACTIVE`; tính cước ở server bằng `BigDecimal`: giá cơ bản, bậc cân vượt, bảo hiểm và phí COD theo policy đã chốt.
- [ ] Tính lại/kiểm tra báo giá khi xác nhận; browser không quyết định tổng phí hoặc trạng thái.
- [ ] Persist shipment, event và payment theo đúng transaction và nhánh thanh toán.
- [ ] Thanh toán online demo dùng method chuẩn như `VNPAY`; không tự thêm method `DEMO` và không gọi provider thật.
- [ ] Xác nhận demo chỉ cho sender sở hữu đơn đủ điều kiện; idempotent, không tạo thanh toán lần hai.
- [ ] Test chính sách inactive/thiếu, dữ liệu sai, sửa total từ client, duplicate confirmation và SQL Server.

Đầu ra chính: `ShipmentIntakeService`, `PricingPolicy`, request/quote DTO, `phase-02-intake-payment.sql`; `Payment`/repository cần thống nhất với Phase 04 trước khi dùng chung.

| Nhánh | Lúc tạo đơn | Thời điểm thu/xác nhận |
|---|---|---|
| Sender online demo | Payment `PENDING`, shipment `WAITING_FOR_PAYMENT` | Xác nhận demo hợp lệ: chính payment đó thành `PAID`, shipment `WAITING_FOR_HANDOVER`. |
| Sender cash + home pickup | `UNPAID`, chờ bàn giao | Thu lúc shipper thực sự nhận kiện; payment `PAID` cùng kết quả pickup. |
| Sender cash + self-dropoff | `UNPAID`, chờ bàn giao | Thu tại quầy lúc nhận kiện; cần contract với inbound Phase 03. |
| Receiver trả cước | Chưa thu | Phase 04 thu khi giao, cộng COD nếu có. |

**Không tự chọn công thức còn thiếu:** hệ số cân quy đổi, đơn vị kích thước, làm tròn tiền/bậc cân, cách chọn policy trùng và cách lưu dấu lựa chọn dịch vụ phải được ghi nhận trước khi áp dụng. Tài liệu có field policy nhưng chưa đủ để suy ra mọi hằng số nghiệp vụ.

### 02-04 — Wizard tạo vận đơn

- [x] Đi theo D-04: sender/receiver → kiện hàng/dịch vụ → báo giá/người trả/thanh toán → first-mile method và hướng dẫn.
- [x] Dùng server-owned draft/quote reference để bảo vệ các bước; xử lý quay lại, submit lặp trong cùng phiên và báo giá đã thay đổi. Báo giá được tiêu thụ sau commit; giá tính lại khác giá đã xem thì không ghi đơn.
- [x] Refactor route tạo đơn Phase 01, không để hai controller trùng mapping.
- [x] Hiển thị báo giá, thanh toán mô phỏng đúng nhánh và hướng dẫn lấy hàng/tự mang tới hub.
- [x] Sender cash thấy thông báo thu lúc bàn giao; receiver payer thấy thu lúc giao hàng.
- [x] Giữ session + CSRF cho JSP; API JWT tiếp tục stateless.
- [x] Dùng CSS chung, JSTL Jakarta, output escaped và lỗi tại từng field; hỗ trợ bàn phím, lỗi ở bước ẩn và form đầy đủ khi tắt JavaScript.
- [x] Test MVC validation/PRG/CSRF/ownership và render JSP thật cho các nhánh người trả phí.
- [ ] UAT đủ ba nhánh với bảng giá thật của nhóm và xác nhận của người dùng.

Triển khai tái sử dụng `ShipmentController`, `ShipmentService`, `create.jsp` và `detail.jsp`; không tạo controller/route trùng chỉ để khớp tên trong plan. Báo giá mới nhất lưu trong session và gắn với các field đã xem, ngoại trừ cách bàn giao chọn ở bước cuối. Chống lặp này có phạm vi cùng phiên trên ứng dụng một instance; chưa có khóa idempotency bền vững qua nhiều phiên/máy chủ.

### 02-05 — HOME_PICKUP: giao việc và lấy hàng

- [x] Có coordinator action tối thiểu để gán pickup thật trong ứng dụng, không chỉ seed assignment.
- [x] Người gán là `ROLE_HUB_STAFF`, profile `ACTIVE`, position `COORDINATOR` hoặc `GENERAL_STAFF`, đúng applicable hub.
- [x] Shipper là `ROLE_SHIPPER`, profile `ACTIVE`, đúng hub và đúng assignment.
- [x] Shipper chỉ liệt kê/xem/hoàn thành nhiệm vụ của chính mình.
- [x] Lưu `shipment_assignments` loại `PICKUP`, `pickup_attempts`, event, timestamp và thay đổi trạng thái trong transaction.
- [x] Có thao tác ghi lần liên hệ không thành công; dùng result chuẩn (`FAILED_NO_ANSWER`, v.v.).
- [x] Khi pickup thành công, kiểm tra/scan mã, thu cước sender cash nếu cần rồi `PICKED_UP`.
- [x] Thu cash phải tạo/cập nhật payment đúng số tiền server tính; không nhận số tiền browser làm nguồn sự thật, không thu trùng khi retry.
- [x] Ghi `PICKUP_TASK_ASSIGNED` dưới dạng event; shipment vẫn dùng 13 status chuẩn.
- [ ] Test khác hub, sai position/role, inactive profile, khác assignee, đơn self-dropoff/đã đóng, cạnh tranh cập nhật và SQL Server hai hub.

Đầu ra chính: `FirstMileService`, `PickupAttempt`, DTO assignment/work/completion, `phase-02-first-mile.sql`. `ShipmentAssignment`, hub/profile phải phối hợp với code đã có ở Phase 03/04.

Phần pickup kết thúc ở việc nhận kiện và bàn giao dữ liệu để inbound xử lý. Bạn không làm routing, đóng bao hoặc linehaul trong gói này.

### 02-06 — Timeout, hủy đơn, lỗi dịch vụ và chống race

- [x] HOME_PICKUP: đúng 48 giờ từ `assigned_at`, chưa lấy được thì hủy `PICKUP_TIMEOUT`.
- [x] SELF_DROPOFF: đúng 48 giờ từ lúc vào `WAITING_FOR_HANDOVER`, chưa bàn giao thì hủy `DROPOFF_TIMEOUT`.
- [x] Có timestamp bền vững ghi thời điểm vào trạng thái chờ; không dùng `created_at` thay thế khi payment được xác nhận muộn.
- [ ] Dùng `Clock`/thời gian server và scheduler/config được chấp thuận; production giữ 48 giờ, demo rút ngắn có kiểm soát.
- [x] Coordinator đúng hub được xác nhận lỗi dịch vụ UTEExpress với evidence giới hạn, ghi `PICKUP_SERVICE_FAILURE`.
- [x] Sender chỉ hủy trực tiếp ở `CREATED` hoặc `WAITING_FOR_HANDOVER` theo quyền sở hữu.
- [x] Hủy cạnh tranh pickup chỉ có một kết quả thắng: hủy đơn/task hoặc `PICKED_UP`; không được vừa hủy vừa nhận hàng.
- [x] Không hủy trực tiếp từ `PICKED_UP` trở đi; không thêm `RETURN_REQUESTED` vào enum.
- [x] Có controller/JSP actions cho sender cancel, coordinator assignment/failure, shipper attempts/completion; CSRF và PRG.
- [ ] Test mốc `47:59:59`/`48:00:00`, job chạy lại, đơn đã pickup, sai quyền và race thực trên SQL Server.

Đầu ra chính: `IntakeTimeoutService`, typed properties, scheduler được thống nhất, các controller first-mile/cancellation và tests.

### 02-07 — Tracking người gửi, người nhận và khách

- [x] Danh sách đơn đã gửi có phân trang/lọc hợp lý và chi tiết theo sender ownership.
- [x] Người nhận đăng nhập thấy danh sách “Đơn đang đến” đã liên kết, mặc định che thông tin.
- [x] Tách DTO public, recipient masked và sender detail; không serialize entity.
- [x] Trang tìm kiếm tracking cho guest và `/api/tracking/**`, người dùng nhập mã để tra cứu.
- [ ] Public chỉ có tracking number, status, current hub, timeline an toàn và ETA theo chính sách được chấp thuận.
- [x] Không lộ full phone/address, nhân viên/tài xế, assignment, incident nội bộ, finance hoặc audit.
- [x] Timeline dùng allowlist event/label an toàn; không đưa nguyên văn mọi `description` nội bộ lên public.
- [ ] Test guessed ID/phone, user khác, receiver chưa link, late link và mọi trường cấm; tracking không ghi dữ liệu operations/finance.

Đầu ra chính: `TrackingService`, `GuestTrackingController`, public API controller, recipient controller và các trang search/result/incoming; cần thêm màn hình danh sách đơn gửi để đầy đủ chức năng quản lý của người dùng.

**ETA:** cần thống nhất nguồn và cách hiển thị khi chưa có estimate; không tự đặt lời hứa ngày giao từ dữ liệu không tồn tại.

### 02-09 — Ghi nhận nghĩa vụ hoàn tiền, trước khi làm 02-08

- [x] Tạo `RefundSettlementOutbox` và migration `phase-02-refund-outbox.sql` theo plan.
- [x] Chỉ shipping fee đã `PAID` thuộc nhánh hợp lệ mới sinh nghĩa vụ hoàn.
- [x] Bốn trigger trong plan: `SENDER_REQUEST`, `PICKUP_TIMEOUT`, `PICKUP_SERVICE_FAILURE`, `DROPOFF_TIMEOUT`.
- [x] Cùng transaction với cancellation/event tạo đúng một row `PENDING_WALLET_CREDIT`; unique theo payment/idempotency key.
- [ ] Payload có shipment, sender, payment, amount, cancel reason và delivery state.
- [x] Payment vẫn `PAID` khi outbox pending; không tuyên bố hoàn tiền xong.
- [x] Sender cash chưa thu/receiver chưa thu không sinh refund.
- [ ] Test duplicate retry/job, unique constraint, commit/rollback đồng thời trên SQL Server.
- [ ] Bàn giao cho Phase 04 việc credit ví, ledger, mark delivered và chuyển payment `REFUNDED` trong transaction thích hợp.

Đầu ra chính: `RefundObligationService`, entity/repository/DTO outbox, SQL và tests. Bạn không viết wallet consumer hoặc tự cộng ví trong Phase 02.

**Khoảng trống đang có:** `ShipmentFinanceServiceImpl` của Phase 04 hiện tìm candidate từ payment `PAID` + shipment `CANCELLED`, rồi refund theo payment ID. Chưa có consumer đọc/đánh dấu outbox của Phase 02. Cần thống nhất contract với Phát; không xem đó là phần đã nối xong.

### 02-08 — Mở khóa chi tiết, realtime và nghiệm thu

- [ ] Chỉ linked recipient đủ điều kiện được unlock bằng mã vận đơn đầy đủ, khớp chính xác, hoặc OTP bound đúng recipient + shipment + purpose.
- [ ] Không dùng OTP đăng ký làm bằng chứng unlock shipment khác; OTP có expiry, giới hạn thử và dùng một lần.
- [x] Chi tiết sau unlock vẫn là DTO dành cho recipient, không phải toàn bộ audit/finance/staff nội bộ.
- [x] Dùng STOMP hiện có; destination do server xác định và chỉ publish sau commit thành công.
- [x] Payload intake/assignment/pickup/cancel không chứa contact/address/OTP, payment/refund amount hoặc staff/incident/audit.
- [x] Hủy pickup gửi tín hiệu cho shipper được phân công; người không liên quan không nhận được.
- [ ] Test rollback không gửi event, cross-user subscription, proof sai/partial/replay/cross-shipment.
- [ ] Chạy full Maven suite và mọi SQL integration test cần thiết; ghi skipped đúng là chưa có bằng chứng môi trường.
- [ ] UAT với guest, sender, recipient, coordinator, shipper được gán, shipper khác hub và người không có quyền.
- [ ] Ghi review kiến trúc của Vạn và review privacy/finance boundary của Phát, giải quyết findings trước khi hoàn thành.

## 4. Thứ tự thực hiện và cách biết mình đã xong

```text
Day 00: môi trường + baseline
02-01 → 02-02 → 02-03 → 02-04
                           ↓
                         02-05 → 02-06
                           ↓       ↓
                         02-07   02-09
                            \     /
                             02-08 → review/UAT
```

Checklist dùng Day 00, 01…19 để nhóm việc và điều kiện nghiệm thu, không phải cam kết mỗi nhóm chỉ mất một ngày. Theo yêu cầu người dùng ngày 22/10/2026, mã account/UI được làm trước trong lúc hoãn cấu hình provider và chọn DB. Ngày 23/10 đã bổ sung bằng chứng SQL cho account/linking/payment; các bước UAT và nghiệp vụ chưa triển khai vẫn để trống.

| Requirement | Gói việc chứng minh |
|---|---|
| `INTAKE-01` | 02-01, 02-02: account, OTP, session/JWT boundary. |
| `INTAKE-02` | 02-03, 02-04: full intake, snapshot, server validation. |
| `INTAKE-03` | 02-03, 02-04: quote, payment state và đúng thời điểm thu. |
| `INTAKE-04` | 02-04, 02-05, 02-06, 02-09: first-mile, timeout/race, pending refund. |
| `INTAKE-05` | 02-02, 02-07, 02-08: receiver link, masked tracking/unlock/realtime. |

Ô đã tick ở phần account ghi nhận đầu việc triển khai cụ thể đã có trong mã. Nghiệm thu mỗi gói vẫn cần unit/service tests, MVC/security khi có route, SQL Server evidence khi có persistence, UAT phù hợp, commit tập trung và ghi log. Có class hoặc JSP chưa đủ để tuyên bố hoàn tất gói hay toàn Phase 02.

## 5. Database: phần bạn cần làm và phần dùng chung

**Bạn cần một SQL Server database cục bộ để chạy/test và viết migration cho Phase 02.** Không phải dựng máy chủ chung cho cả nhóm hoặc tự tạo lại toàn bộ mô hình logistics.

| Nhóm dữ liệu | Việc của bạn |
|---|---|
| `users` | Mapping và logic đăng ký/kích hoạt/tra phone, không tự thêm role. |
| `shipments` | Mở rộng intake/snapshot/receiver link/fee/first-mile/deadline/version; thống nhất field dùng chung. |
| `shipment_events` | Ghi event chuẩn và tạo tracking projection an toàn. |
| `pricing_policies` | Đọc policy active để tính cước, seed fixture/dev được chấp thuận; không tự nhận thêm admin CRUD bảng giá. |
| `payments` | Shipping fee initiation/confirmation và thu sender cash đúng lúc; cùng model với Phase 04. |
| `shipment_assignments`, `pickup_attempts` | Pickup assignment/attempt/completion/cancellation. |
| `hubs`, `employee_profiles` | Đọc/seed test để kiểm tra hub/position; không làm toàn bộ quản trị mạng lưới/nhân sự. |
| OTP challenge, refund outbox | Bổ sung có migration/contract rõ ràng vì schema gốc chưa có. |
| `wallets`, ledger, COD settlement/reconciliation/withdrawal | Phase 04; Phase 02 không cập nhật các bảng này. |

Schema và migration cần phân biệt:

1. `docs/schema.sql` là DDL nền, hiện hard-code database `ute_express`, tạo bảng không idempotent toàn bộ. Không chạy lại lên DB đã có bảng như một thao tác reset.
2. Schema này còn cột legacy `shipment_events.status`; phải áp dụng migration Phase 01 về `event_type`, theo ADR-003.
3. Main có schema nền; không cần lấy toàn bộ code Phase 04 chỉ để có file tạo DB.
4. Nhánh Phase 03 có thêm hub-routing/container/trip/receipt migrations; nhánh Phase 04 có delivery/return/reconciliation/withdrawal migrations. Chỉ áp dụng bộ migration theo baseline được chọn và thứ tự đã kiểm chứng.
5. “25 bảng” là mô tả schema nền ở thời điểm đó; OTP/outbox và migration được duyệt có thể thêm bảng. Không coi 25 là lý do cấm bảng bổ sung đã được plan yêu cầu.
6. `ddl-auto=none`: Hibernate không tự tạo/sửa schema. Cấu hình local/secret không commit.
7. Trước SQL integration test, cần DB kiểm chứng riêng và account phù hợp vì test có tạo/xóa fixture. Không dùng database của các bài tập khác.

**Trạng thái local ngày 23/10/2026:** đã chọn `ute_express`, SQL login `uteexpress_app`, JDK 22 và nhập config local. Schema cần cho account/shipment/payment đã được kiểm tra; 11 SQL integration tests đạt, gồm rollback/concurrency. Fixture được dọn sau kiểm thử. DB chưa có bảng giá để dùng trên giao diện; người dùng chọn cung cấp bảng giá của nhóm, không seed giá mẫu. Hibernate vẫn dùng `ddl-auto=none`; không chạy lại schema nền như một thao tác reset. Hướng dẫn ở [PHASE_02_ACCOUNT_SETUP.md](PHASE_02_ACCOUNT_SETUP.md).

## 6. Các điểm phải phối hợp với Vạn/Phát

| Điểm | Đã thấy trong code/tài liệu | Việc cần chốt trước phần liên quan |
|---|---|---|
| Java | Main dùng Java 22; Phase 04 đổi POM sang 21. | Thống nhất baseline JDK, không tự đổi theo một ghi chú cũ. Local đã có cả 21/22. |
| `Shipment.current_hub_id` | Phase 03 map `Hub currentHub`; Phase 04 map `Long currentHubId`. | Chỉ một mapping ghi cho cùng column và API tương thích sau tích hợp. |
| Optimistic locking | Phase 03 đã có `@Version Long version` và migration. | Tái sử dụng contract/version column; không tạo version thứ hai khi nối Phase 02. |
| Destination | Workflow có `receiver_province/district`; Phase 03 đọc `destination_region/province`. | Intake phải bàn giao dữ liệu địa lý tương thích, có nguồn chuẩn và migration được thống nhất. |
| Hub gốc trước inbound | `HubOperationsServiceImpl.receiveAtHub` yêu cầu hub hiện tại trùng hub nhân viên cho initial intake. | Chốt cách chọn/derive origin hub và field current hub trước nhận vật lý; không tin hub ID browser như quyền. |
| `Payment`, `ShipmentAssignment` | Đã có entity/repository ở Phase 04, trong khi plan Phase 02 liệt kê tạo mới từ nền Phase 01. | Dùng một class/model chung khi tích hợp; ghi rõ dependency hoặc shared-contract PR, không copy hai bản. |
| `Hub`, `EmployeeProfile` | Đã có ở Phase 03. | Dùng model chung hoặc seam tối thiểu được chấp thuận; không làm lại hub admin. |
| `ShipmentEvent` và STOMP | Phase 03 bổ sung hub/factories; Phase 04 có factory chung và queue tài chính riêng. | Giữ các factory cần thiết và payload/queue riêng theo quyền; không đưa notification tài chính vào public tracking. |
| Cash first-mile | Workflow §§10, 30, 52–53 bắt buộc thu đúng lúc; checklist cũ chưa ghi rõ pickup cash implementation. | Phase 02 làm pickup cash; contract thu tại quầy phải phối hợp Phase 03, chống thu trùng và bàn giao record cho finance. |
| Reconciliation cash | Phase 04 đang có nguồn đối soát COD và receiver cash shipping fee. | Thống nhất trách nhiệm tiền sender-cash do pickup shipper thu; không giả định đã nằm trong receiver-cash path. |
| Self-dropoff cân lại | Workflow §30 có surcharge hoặc hủy `WEIGHT_DISCREPANCY`; plan Phase 02 loại trừ hub acceptance. | Ghi rõ owner của cân/đo lại, surcharge, thông báo và refund cho nhánh này. Không tự nhận inbound/thu ví vào Phase 02. |
| Refund outbox | Plan 02-09 yêu cầu outbox; Phase 04 hiện refund trực tiếp từ candidate payment. | Phase 02 ghi nghĩa vụ; owner finance/integration nối consumer với credit/ledger/payment và outbox delivery atomically. |
| `incidents` | Phase 03 dùng shape vận hành khác canonical; Phase 04 có phần reconcile shape. | Đây là xung đột Phase 03/04 cần leader xử lý khi tích hợp; không phải yêu cầu bạn viết lại incidents. |
| Review | `TEAM_OWNERSHIP.md` có sơ đồ review khác `TEAM_EXECUTION_PLAN.md` và plan 02-08. | Checklist Phase 02 vẫn yêu cầu cả Vạn và Phát; ghi review thực tế, không tự khẳng định đã được duyệt. |

Những điểm này là dependency/decision ở từng gói; không chặn mọi việc Phase 02. Vẫn có thể làm account/OTP và phần độc lập sau khi đủ quyết định tương ứng.

## 7. Chỗ tài liệu chưa đủ chi tiết — không tự mở rộng scope

- **Account/OTP:** ADR-016 đã ghi hai lựa chọn theo yêu cầu người dùng. Policy OTP, mô phỏng local và bằng chứng SQL đã có; provider thật, credentials, browser UAT và lịch bảo trì dữ liệu OTP chưa hoàn tất. Phone verification khi đã đăng nhập bằng Google là phần bổ sung còn thiếu.
- **Báo giá:** chốt đơn vị, cân quy đổi, làm tròn, lựa chọn policy và lưu trace dịch vụ/báo giá. `shipments` chuẩn hiện không có `service_code`, mô tả hàng hoặc fee breakdown riêng.
- **Timestamps:** đã thêm migration nullable `waiting_for_handover_at`, set khi đơn cash được tạo hoặc online được xác nhận; retry giữ mốc cũ. Dùng `Clock` và giờ local của server như các timestamp vận hành hiện có, cắt tới giây theo `DATETIME2(0)`; cần thống nhất timezone triển khai với nhóm. Đơn legacy không có bằng chứng thời điểm vẫn để NULL. `payment_deadline` có trong schema nhưng chưa có quy định độ dài thời hạn; chưa tự đặt số phút hoặc bật scheduler.
- **ETA:** public DTO cần estimate nhưng schema/plan chưa định nghĩa SLA/công thức. Ghi rõ lựa chọn hiển thị nếu chưa có.
- **Scheduler actor:** event entity nền yêu cầu actor không null, trong khi SQL có thể nullable và job không có người dùng đăng nhập. Chốt cách ghi automated event có provenance, không giả mạo principal hoặc bỏ authorization.
- **Sổ địa chỉ/profile:** bảng phân công có nhắc sổ địa chỉ, nhưng 9 plan chưa có thiết kế persistence/luồng CRUD riêng. Đưa vào câu hỏi phạm vi với leader; chưa coi là tính năng bắt buộc đã lên kế hoạch chi tiết.
- **Quên mật khẩu/admin user management:** không có trong 9 plan Phase 02; không tự thêm để thay thế phần còn thiếu của OTP/intake/tracking.
- **Guest estimate:** Workflow cho phép guest ước tính phí và security đã mở `/calculator/**`/`/api/calculator/**`; chưa có endpoint triển khai ở main. Cần xác nhận có bàn giao public calculator trong Phase 02 hay chỉ quote của wizard; tái sử dụng quote service khi được đưa vào scope.
- **UI spec có ví dụ lệch chuẩn:** dòng Spring Boot 4.x không thay được POM 3.3.4; ví dụ masked contact ở public không cho phép thêm contact vào DTO public chỉ có whitelist của Workflow; ví dụ inline style không thay quy tắc CSS class chung.

## 8. Những việc không thuộc phần triển khai Phase 02

- Routing matrix/hierarchy administration, inbound `AT_HUB`, sorting, container, custody, handover, trip và điều tra incident: Phase 03.
- Delivery, failed delivery/retry, return, COD collection/payout, wallet/ledger, đối soát và withdrawal: Phase 04.
- Credit ví khi refund, consumer finance và kết nối full journey: phối hợp owner Phase 04/Phase 05.
- Cổng VNPay/ngân hàng thật, GPS/AI route optimization hoặc đổi frontend/database stack: ngoài phạm vi hiện tại.

Bạn vẫn cần hiểu interface, đọc/review mã của đồng đội và tham gia Phase 05/06. Điều đó không biến toàn bộ công việc của họ thành phần phải tự viết trong Phase 02.

## 9. Việc cần làm tiếp

1. Mở lại Eclipse, refresh project và chạy `UteExpressApplication` với `local,otp-local`; kiểm thử trang chủ và đăng ký → OTP local → kích hoạt → đăng nhập trên trình duyệt. Launch local đã cấu hình loại test code và working directory; vẫn cần xác nhận thực tế trong IDE.
2. Nhận bảng giá của nhóm do người dùng cung cấp: dịch vụ, tuyến, cân cơ bản/giá cơ bản, bậc cân thêm/giá thêm, tỷ lệ bảo hiểm/COD và quy tắc áp dụng. Kiểm tra rồi nạp dữ liệu đã thống nhất; không seed giá demo.
3. Khi có giá, thử báo giá/tạo đơn và ba nhánh thanh toán trên trình duyệt. Test SQL đã qua sender-online, sender-cash và receiver-cash với fixture riêng, cùng rollback/linking và payment đồng thời. Google/provider thật tiếp tục hoãn theo yêu cầu.
4. Hoàn thiện intake: chốt quy tắc giá/service/hub với nhóm, lưu dấu policy/dịch vụ và quy định thời hạn thanh toán. Wizard và mốc chờ bàn giao đã có; scheduler, version và các field lifecycle còn lại vẫn mở.
5. Sau đó làm first-mile/phân công shipper, hủy/timeout 48 giờ; tiếp theo tracking, refund outbox và recipient unlock/realtime theo thứ tự trong checklist.
6. Bạn kiểm thử giao diện/vận hành và phối hợp quyết định với nhóm; phần sửa code và automated tests do trợ lý thực hiện khi tiếp tục triển khai. Ghi bằng chứng thật vào [PHASE_02_DIRECTOR_CHECKLIST.md](PHASE_02_DIRECTOR_CHECKLIST.md), không tick nghiệm thu chỉ vì đã có class.

Baseline đã chạy trên máy này với JDK 22: **58 test, 0 failures, 0 errors, 1 skipped**, `BUILD SUCCESS`. Test bị skip là SQL Server integration; đây không phải bằng chứng DB đã sẵn sàng. Các con số Phase 04 trong handoff là báo cáo của nhánh đó, chưa được chạy lại trong phiên rà soát này.

Kết quả mới nhất ngày 23/10/2026: **171 test đạt, 0 failures/errors/skips**, gồm **11 SQL Server tests**, WAR build thành công. Có wizard, kiểm tra quote session/giá đã xem, 7 test Jasper shipment và 10 kiểm tra Chrome. Đã áp dụng migration chờ bàn giao. Các kết quả 127/151 test là lịch sử trước wizard, lưu trong nhật ký checklist. Browser UAT với bảng giá nhóm vẫn chưa hoàn tất.

## 10. Nguồn và phạm vi rà soát

- Đã fetch 4 nhánh trong bảng hiện trạng; lập danh mục và đọc nội dung bằng công cụ cho **336 phiên bản file khác nhau theo Git blob** (file chung giữa các nhánh chỉ tính một lần). Không gồm `.git`, dependencies, `target` hoặc secret local.
- Đọc sâu các nguồn quyết định/phân công, Workflow, schema/ERD, 9 plan và context/research Phase 02; đối chiếu implementation và test contracts, các thay đổi entity/service/security/migration liên quan trên Phase 03/04.
- Nguồn local: `ARCHITECTURE_DECISIONS.md`, `UTEExpress_Workflow.md`, `ERD.md`, `PROJECT_SPEC_KNOWLEDGE_BASE.md`, `ENGINEERING_STANDARDS.md`, `UI_DESIGN_SYSTEM.md`, `TEAM_OWNERSHIP.md`, `TEAM_EXECUTION_PLAN.md`, `TEAM_WORK_BREAKDOWN.md`, `TEAM_START_GUIDE.md`, `PHASE_01_FOUNDATION.md` và mã/test trong `src`.
- Nguồn planning: `.planning/REQUIREMENTS.md`, `.planning/ROADMAP.md`, `.planning/phases/02-user-shipment-intake/02-CONTEXT.md`, `02-RESEARCH.md`, `02-01-PLAN.md` … `02-09-PLAN.md` trên `origin/project-plans`.
- Nguồn handoff: `docs/PHASE_04_HANDOFF.md` trên `origin/feature/phase-04-last-mile-finance`; đã kiểm tra lại các nhận định quan trọng bằng source, không dùng trạng thái “hoàn tất” trong handoff để tự tick checklist của bạn.
