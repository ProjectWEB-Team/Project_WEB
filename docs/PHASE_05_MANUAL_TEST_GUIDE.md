# Hướng dẫn test luồng hiện tại — Phase 01–05

Hướng dẫn đối chiếu controller, JSP và integration test hiện tại ngày 25/10/2026. Đây là checklist để chạy UAT; chưa phải kết quả UAT trình duyệt. Lần kiểm thử tự động đã ghi nhận trước đó: 504 passed, 0 skipped, gồm 50 ca SQL Server; xem [kết quả SQL](PHASE_05_SQL_RESULTS.md).

Cập nhật 26/10/2026: form địa chỉ mới dùng 34 tỉnh/thành → 3.321 phường/xã/đặc khu cho cả hai bên, server tự suy ra vùng cước. Xem [địa chỉ và mạng mô phỏng](VIETNAM_ADDRESS_2025.md). Bộ hồi quy mới trước khi lập Phase 5.1 đạt 516 test, gồm 50 ca SQL thật, không skip; xem [mốc kiểm chứng](PHASE_05_1_PREPLAN_REGRESSION.md). Theo phân công cuối, [hướng dẫn Phase 5.1](../.planning/phases/05.1-full-functional-acceptance/05.1-01-PLAN.md) dùng Spring Tools để TV3 tự kiểm thử, đánh giá và ghi điểm bất hợp lý, không sửa mã; TV2 làm Phase 6 song song, người dùng duyệt cuối. Hướng dẫn cũ bên dưới chỉ là tài liệu tham khảo, không yêu cầu TV3 chạy các bước quản trị SQL hoặc workaround trái phạm vi plan mới.

## 1. Chuẩn bị mã, database và tài khoản

Sau khi merge GitHub, nếu checkout local vẫn ở nhánh sửa lỗi, mở PowerShell tại thư mục dự án và chạy:

```powershell
rtk git status --short --branch
rtk git switch main
rtk git pull --ff-only origin main
```

Nếu Git báo thay đổi/conflict cần xử lý, giữ lại các thay đổi đó trước khi tiếp tục; không reset hoặc xóa các thư mục local để ép chuyển nhánh.

### Database chạy ứng dụng

Máy này đã có `src/main/resources/application-local.properties` chứa cấu hình SQL/JWT. Dùng profile `local` để nạp file, không cần nhập lại hoặc gửi các giá trị bí mật trong chat.

**Merge mã không tự chạy migration** vì ứng dụng dùng `ddl-auto=none`. Bộ test trước đó chạy trên database riêng `uteexpress_phase05_test_20261025_codex_01a17279`; kết quả đó không chứng minh database ứng dụng `ute_express` đã được cập nhật.

Trước UAT, dừng app và kiểm tra database thực tế trong SSMS bằng `SELECT DB_NAME();`. Với database đã có schema nền, áp dụng các script theo đúng thứ tự trong [manifest](../src/main/resources/db/phase-05-migration-manifest.txt) trên đúng database UAT. Có thể in danh sách theo thứ tự bằng:

```powershell
rtk proxy powershell -NoProfile -Command "Get-Content src/main/resources/db/phase-05-migration-manifest.txt"
```

Mỗi dòng `db/...sql` tương ứng file dưới `src/main/resources/db/`. Script có kiểm tra dữ liệu/ràng buộc; nếu một script báo lỗi thì dừng và xử lý nguyên nhân, không bỏ guard hoặc tiếp tục coi schema đã sẵn sàng. Database mới hoàn toàn cần schema nền trước; không chạy nguyên `docs/schema.sql` lên database đang có dữ liệu vì file có tên database cố định và không phải script reset an toàn. Manifest không tự seed bảng giá hay tài khoản.

### Dữ liệu cần có

Chọn tuyến thật đang được cấu hình, ví dụ ký hiệu `O → M → D`: hub gửi O, hub trung chuyển M, hub phát D. Ký hiệu này không phải ID cố định để nhập vào form.

| Thành phần | Điều kiện |
|---|---|
| Người gửi | Tài khoản `ROLE_USER`, đăng nhập được |
| Nhân viên tại O/M/D | `ROLE_HUB_STAFF`, hồ sơ ACTIVE, đúng hub và vị trí INBOUND/SORTER/OUTBOUND/COORDINATOR tương ứng; GENERAL_STAFF có thể kiêm các việc nếu chính sách cho phép |
| Tài xế | `ROLE_LINEHAUL_DRIVER`, hồ sơ hợp lệ, đúng người được giao phiếu bàn giao |
| Shipper phát tại D | `ROLE_SHIPPER`, hồ sơ ACTIVE tại D, đủ điều kiện nhận phân công phát |
| Quản lý | `ROLE_MANAGER`, hồ sơ/phạm vi quản lý chứa hub và nhân viên liên quan |
| Xe | Ít nhất một xe AVAILABLE được lưu trong database |
| Hub/bảng giá | Hub ACTIVE, địa chỉ người nhận ánh xạ duy nhất tới hub phát; bảng giá hợp lệ cho dịch vụ/vùng/cân nặng chọn |
| Routing | Tuyến tĩnh hợp lệ qua từng chặng; muốn test hoàn thì cần cả chiều D → M → O |

Tài khoản fixture của SQL test dùng mật khẩu bị vô hiệu hóa và dữ liệu được dọn cuối test. **Không dùng chúng làm tài khoản UAT.** Đăng ký thông thường tạo tài khoản khách hàng; không tự tạo đầy đủ nhân viên, tuyến và xe.

Dùng SSMS kiểm kê mà không đọc cột mật khẩu:

```sql
SELECT DB_NAME() AS current_database;
SELECT u.id AS user_id, u.username, u.role, u.status AS user_status,
       ep.id AS employee_profile_id, ep.hub_id, ep.position,
       ep.status AS employee_status
FROM dbo.users u
LEFT JOIN dbo.employee_profiles ep ON ep.user_id = u.id
ORDER BY u.role, ep.hub_id, u.id;

SELECT id, code, name, type, parent_hub_id, province, district, status
FROM dbo.hubs ORDER BY id;

SELECT id, plate_number, vehicle_type, status FROM dbo.vehicles ORDER BY id;
```

`Assigned driver id` và `shipperId` là **users.id**, không phải employee_profiles.id. Chuẩn bị sẵn bảng ghi O/M/D, ID tài xế, ID shipper, ID quản lý và tài khoản đăng nhập tương ứng.

## 2. Khởi động ứng dụng

Trong PowerShell tại thư mục dự án:

```powershell
rtk mvn "-Dspring-boot.run.profiles=local,otp-local" spring-boot:run
```

Chạy bằng Spring Tools Suite (STS):

1. Nếu project chưa có trong IDE: **File → Import → Maven → Existing Maven Projects**, chọn thư mục chứa `pom.xml`, rồi **Finish**. Với checkout hiện tại là `C:\Users\Admin\Desktop\Project WEB`.
2. Nhấn phải project → **Maven → Update Project… → OK**. Kiểm tra không còn lỗi build chặn chạy. `pom.xml` hiện khai báo Java 21; chọn JDK 21 hoặc JDK mới hơn tương thích trong cấu hình chạy, không dùng JRE 8/11/17.
3. Mở **Run → Run Configurations… → Spring Boot App**, tạo/chọn cấu hình của project. Main class: `vn.iotstar.UteExpressApplication`.
4. Trong mục **Profiles**, nhập `local,otp-local`. Nếu phiên bản STS không có ô này, vào **Arguments → Program arguments**, nhập `--spring.profiles.active=local,otp-local`.
5. **Arguments → Working directory**: chọn thư mục project hiện tại, không dùng đường dẫn `E:\Project_WEB` từ hướng dẫn cũ nếu checkout nằm trên ổ C.
6. Trong **Classpath** hoặc **Dependencies** (tùy bản IDE), bật **Exclude test code**. Không để `target/test-classes` hoặc `src/test/resources` che cấu hình chạy ứng dụng.
7. Chọn **Apply → Run**, hoặc dùng nút chạy tương ứng trong **Boot Dashboard** sau khi lưu cấu hình này. Dừng instance cũ trước để tránh trùng cổng 8080.

Nếu không có loại **Spring Boot App**, dùng **Java Application** với cùng main class, JDK, working directory và program arguments ở trên.

Đợi log báo ứng dụng khởi động thành công, mở <http://localhost:8080/login>. Profile `otp-local` phục vụ OTP mô phỏng local. Chưa bật `demo` trong lần đi luồng đầu vì profile đó rút một số thời hạn xuống 2 phút.

Tách phiên bằng Chrome/Edge hoặc các browser profile riêng. Nhiều tab trong cùng profile dùng chung cookie nên không giữ được nhiều vai trò độc lập. Có thể đăng xuất/đăng nhập lần lượt khi test tuần tự; riêng realtime cần các phiên đồng thời.

Các URL dưới đây giả định Spring Boot chạy ở context gốc. Nếu triển khai WAR có context path, thêm context path vào trước đường dẫn.

## 3. Luồng A — gửi, vận chuyển, phát và trả COD

Ghi lại `shipmentId`, `trackingNumber`, cước báo giá `F`, container/handover/trip của **từng chặng**. Nhiều màn hình vận hành nhập ID số, không nhận tracking number thay cho ID.

### A1. Người gửi tạo đơn

Mở <http://localhost:8080/user/shipments/create> bằng tài khoản người gửi. Điền đầy đủ thông tin, chọn:

- Dịch vụ STANDARD. Với cả người gửi và người nhận, chọn tỉnh/thành rồi phường/xã/đặc khu, nhập riêng số nhà/tên đường; không nhập quận/huyện hoặc vùng cước bằng tay.
- Tỉnh người nhận khớp duy nhất một hub D cấp 1, DELIVERY_HUB, ACTIVE, tên tỉnh chuẩn và `district IS NULL`. Vùng cước được server tính từ mã tỉnh. Các hub toàn tỉnh trong script LOCAL2025 là mô phỏng, cần bổ sung nhân sự/xe đúng phạm vi trước khi chạy luồng kho/giao hàng.
- Tự mang ra bưu cục — SELF_DROPOFF.
- Người nhận trả cước — RECEIVER; tiền mặt — CASH.
- Khối lượng 800 gram, dài/rộng/cao 10 cm.
- COD 100.000đ; giá trị khai báo 200.000đ.

Bấm **Xem báo giá**, ghi `F` đang hiển thị rồi **Xác nhận tạo vận đơn**. Không giả định cước luôn là 30.000đ: nó phụ thuộc bảng giá hiện có. Lấy `shipmentId` từ URL chi tiết `/user/shipments/{shipmentId}` và lưu mã tracking.

Kỳ vọng: đơn thuộc đúng người gửi, trạng thái `WAITING_FOR_HANDOVER`, chưa có tiền COD trong ví. Thử tra cứu mã ở <http://localhost:8080/tracking>.

### A2. Nhân viên O tiếp nhận vật lý

Đăng nhập nhân viên tiếp nhận tại O, mở <http://localhost:8080/hub/sorter>. Trong form **Receive at hub**, nhập shipmentId, trackingNumber và số đo thực tế 800 gram / 10 × 10 × 10 cm. Với ca không chênh lệch và người nhận trả cước, nhập `cashReceived = 0`; không chọn xử lý phụ thu không phát sinh.

Kỳ vọng: `AT_HUB`, vị trí hiện tại O. Đây mới là tiếp nhận đơn tự mang tới; trang GET `/hub/inbound` dùng để nhận kiện từ chuyến xe đến, không phải form tiếp nhận đơn mới.

### A3. Phân loại và đóng bao tại O

Nhân viên SORTER/GENERAL_STAFF tại O dùng **Resolve static next hop** ở `/hub/sorter`, nhập shipmentId. Ghi hub kế tiếp hệ thống trả về, ví dụ M.

Nhân viên OUTBOUND/GENERAL_STAFF tại O mở <http://localhost:8080/hub/outbound>:

1. **Create container**: code duy nhất, ví dụ `UAT-20261025-O-M-01`; destinationHubId là **hub kế tiếp M**, không mặc định hub phát cuối D.
2. **Load shipment**: containerId vừa tạo + shipmentId.
3. **Prepare handover**: destinationHubId M, receiverDriverId là users.id tài xế được chọn, containerIds là containerId vừa dùng. Ca đầu chỉ dùng một bao.

JSP hiện chỉ báo thao tác thành công, chưa hiển thị đầy đủ ID vừa tạo. Dùng các truy vấn chỉ đọc sau trên database ứng dụng để lấy đúng bản ghi của ca UAT:

```sql
SELECT id, container_code, origin_hub_id, destination_hub_id, status
FROM dbo.parcel_containers
WHERE container_code = N'UAT-20261025-O-M-01';

-- Thay 123 bằng containerId vừa tìm, không dùng giá trị ví dụ này nguyên xi.
SELECT h.id AS handover_id, hc.container_id, h.receiver_driver_id,
       h.from_hub_id, h.to_hub_id, h.transport_trip_id, h.status
FROM dbo.handovers h
JOIN dbo.handover_containers hc ON hc.handover_id = h.id
WHERE hc.container_id = 123
ORDER BY h.id DESC;
```

### A4. Tài xế quét bao, nhận xe và chạy chuyến

Đăng nhập đúng tài xế được phân công, mở <http://localhost:8080/driver>.

**Giới hạn UI hiện tại:** đã có endpoint quét bao nhưng JSP tài xế chưa có form/nút quét. Trước **Accept handover**, dùng DevTools → Console ngay tại trang `/driver` để gửi form cùng phiên đăng nhập và CSRF hiện tại. Đoạn này chỉ phục vụ UAT local; thay hai ID theo dữ liệu A3:

```javascript
(() => {
  const handoverId = 456; // thay bằng ID thật
  const containerId = 123; // thay bằng ID thật
  const csrf = document.querySelector('form input[type="hidden"][name]');
  if (!csrf) throw new Error('Mở trang /driver đã đăng nhập trước');
  const existingForm = csrf.closest('form');
  const form = document.createElement('form');
  form.method = 'POST';
  form.action = existingForm.action.replace(/\/handovers\/accept$/, '/handovers/scan');
  for (const [name, value] of [[csrf.name, csrf.value], ['handoverId', handoverId], ['containerId', containerId]]) {
    const input = document.createElement('input');
    input.type = 'hidden'; input.name = name; input.value = String(value);
    form.appendChild(input);
  }
  document.body.appendChild(form);
  form.submit();
})();
```

Nếu có nhiều bao, phải quét từng bao. Sau đó dùng form có sẵn **Accept handover**, nhập handoverId, chọn một xe AVAILABLE. Lấy tripId liên kết:

```sql
-- Thay 456 bằng handoverId thật.
SELECT h.id AS handover_id, h.transport_trip_id, t.trip_code,
       t.status, t.vehicle_id
FROM dbo.handovers h
LEFT JOIN dbo.transport_trips t ON t.id = h.transport_trip_id
WHERE h.id = 456;
```

Nhập tripId vào **Start trip**. Kỳ vọng đơn `IN_TRANSIT`, không còn vị trí hub vật lý cũ. Tiếp tục **Record arrival** với cùng tripId. Đến nơi chưa đồng nghĩa đã nhận kiện; không được phân công phát chỉ vì tài xế đã bấm đến.

### A5. Nhân viên M nhận kiện từ chuyến

Đăng nhập nhân viên INBOUND/GENERAL_STAFF tại M, mở <http://localhost:8080/hub/inbound>:

- **Record physical receipt**: nhập handoverId, containerId, shipmentId của chặng O → M.
- Đối chiếu form **Finish unpacking and reconcile the manifest** bằng đúng danh sách shipmentId thực nhận; ca một kiện dùng đúng một ID. Không chọn **Report all contents missing** cho ca đã nhận đủ.

Kỳ vọng: kiện `AT_HUB` tại M; custody/manifest nhận đủ, không phát sinh incident mất.

Nếu tuyến thực tế còn M → D, lặp A3–A5 tại M và D: sorter xác định D, tạo **bao và handover mới**, tài xế quét/nhận xe/xuất phát/đến, nhân viên D tiếp nhận. Không dùng lại bao cũ hoặc tự cập nhật vị trí bằng SQL. Nếu tuyến chỉ có O → D thì thực hiện nhận tại D ngay, bỏ vòng trung chuyển.

### A6. Điều phối D phân công phát

Mở <http://localhost:8080/hub/delivery-assignments> bằng nhân viên COORDINATOR/GENERAL_STAFF tại D. Tìm đúng tracking, chọn shipper tại D rồi **Phân công**.

Kỳ vọng: có assignment cho đúng người; đơn chỉ sẵn sàng sau khi đã nhận vật lý tại hub phát và giải phóng custody.

### A7. Shipper lấy kiện và phát thành công

Đăng nhập shipper vừa được giao, mở <http://localhost:8080/shipper/delivery-tasks>:

1. **Scan Out**: shipmentId và Current Hub ID = D. Kỳ vọng `OUT_FOR_DELIVERY`.
2. **Collect**: COD amount received = `100000`, Receiver fee received = `F`, cả hai phương thức CASH. Phải nhập đúng từng nghĩa vụ, không nhập tổng `100000 + F` vào ô COD.
3. **Confirm Delivered** sau khi thu đủ. Kỳ vọng `DELIVERED`; tracking và chi tiết người gửi phản ánh giao thành công.

Ví người gửi chưa được cộng COD ở bước shipper xác nhận giao.

### A8. Quản lý đối soát rồi trả COD

Mở <http://localhost:8080/manager/reconciliation> bằng quản lý đúng phạm vi:

1. Nhập users.id shipper, ngày thu tiền thực tế, phương thức CASH; tải các khoản thu.
2. Nhập số thực tế bằng kỳ vọng cho từng dòng COD và cước. **Xác nhận đối soát**.
3. Kiểm tra COD sẵn sàng payout; ví người gửi vẫn chưa tăng chỉ vì đối soát xong.
4. Bấm **Trả COD vào ví người gửi**.
5. Người gửi mở <http://localhost:8080/user/wallet>: balance tăng đúng 100.000đ so với trước, chỉ một giao dịch COD_PAYOUT của đơn. Thao tác lặp không được cộng tiền lần hai.

Muốn kiểm tra realtime, mở kết nối ở mục 5 **trước** khi thực hiện payout, giữ nguyên các tab đang kết nối.

## 4. Các nhánh cần test bằng đơn riêng

Không dùng đơn A đã DELIVERED để ép chạy các nhánh thất bại.

| Ca | Thao tác | Kết quả cần quan sát |
|---|---|---|
| Lấy hàng tại nhà | Tạo HOME_PICKUP; điều phối O vào `/hub/pickups` phân công; shipper vào `/shipper/tasks` ghi nhận lấy; sau đó tiếp nhận/cân tại O | Không được bỏ qua assignment/lấy vật lý; sau tiếp nhận mới tiếp tục tuyến A3 |
| Hủy/hoàn cước | Tạo đơn người gửi trả VNPAY demo, xác nhận thanh toán demo; hủy khi UI/nghiệp vụ còn cho phép; quản lý `/manager/reconciliation` hoàn khoản đang chờ | Hoàn đúng khoản đủ điều kiện vào ví; chạy lại không hoàn hai lần |
| Giao bị từ chối | Đưa đơn mới tới A7 Scan Out; chưa thu tiền; ghi thất bại `RECEIVER_REFUSED`, kèm ghi chú/bằng chứng phù hợp | Chuyển chiều RETURN, không tự xuất hiện lại tại hub |
| Nhận kiện thất bại về hub | Nhân viên D vào `/hub/inbound`, form **Nhận lại kiện giao thất bại**, nhập shipmentId và quét tracking | Ghi nhận vật lý tại D trước khi đóng bao hoàn |
| Hoàn về người gửi | Sorter xác định tuyến ngược D → M → O, thực hiện đóng bao/quét/chuyến/nhận vật lý như A3–A5 theo chiều RETURN | Chỉ khi nhận thật tại O mới bắt đầu mốc grace nhận hoàn; không tạo trạng thái shipment mới ngoài bộ trạng thái chuẩn |
| Thanh toán và nhận hoàn | Người gửi mở `/user/shipment-fees/{shipmentId}` thanh toán từng khoản còn nợ bằng ví hoặc VNPAY demo; nhân viên INBOUND O vào `/hub/return-collection`, nhập chứng cứ xác minh người nhận | Ca người nhận trả cước chưa thu: phải giải quyết cả cước đi và phí hoàn; mặc định phí hoàn 25.000đ, có thể cấu hình. Đủ điều kiện mới `RETURNED_TO_SENDER` |
| Shipper hoàn tận nhà | Thay vì tự nhận, điều phối O phân công return delivery ở `/hub/delivery-assignments`; shipper được giao vào `/shipper/return-tasks` quét rồi xác nhận, giải quyết phí theo form | Không thể xác nhận trả khi chưa quét hoặc còn nghĩa vụ phí |
| Mất toàn bộ bao | Đơn riêng: làm tới xe đến hub kế tiếp nhưng chưa nhận kiện; tại `/hub/inbound` chọn **Report all contents missing**, xác nhận checkbox | Incident missing; tracking công khai chỉ ghi “Đơn hàng đang được kiểm tra hành trình”; không lộ biên bản/chi tiết nội bộ |
| Tìm thấy/xác nhận mất | Quản lý đúng phạm vi mở `/manager/incidents/missing`, xem incident và chọn xử lý theo chứng cứ | Tìm thấy có kiểm soát hub vật lý; xác nhận mất mới đủ điều kiện bồi thường ở `/manager/reconciliation`; ví chỉ nhận một lần |

Mặc định thời hạn nhận hoàn là 7 ngày, retry có khóa theo ngày. Ca quá hạn/disposal cần môi trường UAT và thời gian kiểm thử phù hợp; không sửa trực tiếp timestamp/status trên database đang dùng để làm giả kết quả. Profile `demo` rút một số timer còn 2 phút và tắt khóa retry theo ngày, chỉ dùng khi chủ động test timer với đơn riêng.

## 5. Realtime riêng tư — hai phiên khách hàng

Dùng phiên người gửi của đơn A và một phiên ROLE_USER không liên quan. Mỗi phiên mở một trang cùng ứng dụng, đăng nhập, mở Console rồi chạy đoạn STOMP tối thiểu dưới đây. Giữ tab đứng yên, không refresh/chuyển trang sau khi kết nối. Đây là client quan sát local, không cần cài thư viện hoặc gửi token ra ngoài.

```javascript
var uatMessages = [];
var uatSocket = new WebSocket(
  `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws`,
  ['v12.stomp']
);
uatSocket.onopen = () => uatSocket.send(
  `CONNECT\naccept-version:1.2\nhost:${location.host}\nheart-beat:0,0\n\n\0`
);
uatSocket.onmessage = ({data}) => {
  const frame = String(data);
  if (frame.startsWith('CONNECTED')) {
    uatSocket.send('SUBSCRIBE\nid:uat-private\ndestination:/user/queue/notifications\nack:auto\nreceipt:uat-ready\n\n\0');
  } else if (frame.startsWith('RECEIPT')) {
    console.log('UAT notification subscription ready', frame);
  } else if (frame.startsWith('MESSAGE')) {
    uatMessages.push(frame);
    console.log('UAT private notification', frame);
  } else if (frame.startsWith('ERROR')) {
    console.error(frame);
  }
};
uatSocket.onerror = console.error;
```

Chỉ thực hiện A8 payout sau khi **cả hai phiên** đã báo subscription ready. Kỳ vọng người gửi nhận notification sau commit, khách hàng không liên quan không nhận sự kiện của đơn. Không nhầm “không có message” với kết nối chưa thành công. Payload chỉ chứa thông tin thông báo tối thiểu; mở detail vẫn phải kiểm tra quyền.

Thực hiện một thao tác tài chính bị từ chối trong phiên quản lý, kiểm tra không có thay đổi tài chính/message thành công của thao tác đó. Dừng quan sát bằng `uatSocket.close()` trong từng Console. Nếu dùng context path khác gốc, thêm context path trước `/ws` trong URL.

## 6. Test từ chối và ghi nhận kết quả

Thử có kiểm soát trên dữ liệu UAT: tài xế khác nhận handover; nhân viên khác hub nhận kiện; shipper chưa được giao quét đơn; nhập COD/cước thiếu hoặc thừa; payout trước đối soát; xác nhận trả hoàn trước thanh toán; người dùng không liên quan mở chi tiết đơn. Kỳ vọng bị từ chối theo quyền/nghiệp vụ và không đổi dữ liệu từng phần. Mã HTTP cụ thể có thể khác nhau giữa form redirect và API; kiểm tra cả thông báo và trạng thái sau thao tác.

Nếu nhận 403 dù đúng role, kiểm tra ACTIVE employee profile, position, hub và phạm vi quản lý. Nếu không tìm được next hop, kiểm tra tuyến tĩnh. Nếu không có xe trong select, kiểm tra AVAILABLE và chuyến đang giữ xe. Nếu `/hub/delivery-assignments` trống, kiểm tra nhận vật lý tại terminal hub/custody/assignment. Không dựa vào mọi link sidebar: có link dự kiến cho phase sau.

Mỗi ca ghi: thời điểm, tài khoản/role/hub, URL/nút bấm, shipment/tracking/container/handover/trip, kết quả kỳ vọng, kết quả thực tế, PASS/FAIL và lỗi server nếu có. Với realtime ghi cả phiên nhận và phiên không được nhận. Không đưa mật khẩu, JWT hoặc chuỗi kết nối có secret vào báo cáo.

## 7. Chạy lại kiểm thử tự động trên máy hiện tại

Helper `.local/phase05-sql.ps1` là file local đang có trên máy này, không phải file được commit để máy khác tự có sau pull. Nó đọc cấu hình hiện có vào riêng tiến trình test và chuyển target sang database kiểm thử chuyên biệt; không đổi database chạy ứng dụng trong file local.

```powershell
# Kiểm tra schema/migration; với database test trống phải chạy bước này trước.
rtk proxy powershell -NoProfile -ExecutionPolicy Bypass -File .local/phase05-sql.ps1 -Action Schema

# Chỉ các hành trình tích hợp Phase05.
rtk proxy powershell -NoProfile -ExecutionPolicy Bypass -File .local/phase05-sql.ps1 -Action Journeys

# Toàn bộ regression, gồm SQL.
rtk proxy powershell -NoProfile -ExecutionPolicy Bypass -File .local/phase05-sql.ps1 -Action All
```

Không cần Provision lại database đã tạo. Chạy `mvn test` đơn thuần trong terminal chưa có bộ biến môi trường kiểm thử có thể làm SQL test bị skip dù app đọc được file local. Kiểm tra báo cáo mới trong `target/surefire-reports`: có SQL suites thực thi, failures/errors/skipped bằng 0. Số 504 là bằng chứng lần chạy trước, không phải lời khẳng định đã chạy lại trong lúc viết hướng dẫn này.
