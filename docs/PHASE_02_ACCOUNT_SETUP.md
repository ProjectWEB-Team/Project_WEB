# Phase 02 — Cấu hình account: OTP điện thoại và Google

> Cập nhật 28/10/2026: email OTP, khôi phục mật khẩu và thông báo qua SMTP đã có luồng riêng. Xem [thiết lập Google, Gmail và SMS](EMAIL_GOOGLE_SMS_SETUP.md) cho cổng local hiện tại 8081. Phần bên dưới giữ lại lịch sử Phase 02; các ghi chú Spring Mail chưa được sử dụng là trạng thái trước cập nhật này.

Cập nhật trạng thái ngày **23/10/2026** theo [ADR-016](ARCHITECTURE_DECISIONS.md#adr-016--customer-phone-otp-and-google-oidc-sign-in). Database local là `ute_express`, SQL login `uteexpress_app`, cấu hình riêng trong `application-local.properties`. **Ngày 24/10: `mvn test` chạy 197 test, 0 lỗi, 9 SQL Server tests bỏ qua khi không bật biến môi trường.** Luồng account/OTP local đã qua HTTP thật; form tạo đơn đã có wizard 4 bước, báo giá gắn với session và mốc chờ bàn giao lưu trong DB. SQL tests kiểm tra linking, snapshot, rollback và cập nhật đồng thời. Bảng giá demo đã nạp (bước 9). Google/provider OTP thật, browser UAT theo vai trò và bằng chứng SQL cho first-mile/refund vẫn mở; chưa hoàn thành Phase 02.

## 1. Hai lựa chọn đăng nhập đã triển khai

| Lựa chọn | Hành vi trong mã hiện tại | Cần cấu hình thêm |
|---|---|---|
| OTP điện thoại | Đăng ký `ROLE_USER`/`INACTIVE`; mã đúng kích hoạt và đánh dấu phone đã xác thực. Đăng nhập OTP chỉ cho khách hàng `ACTIVE` có phone đã xác thực. | Mô phỏng bằng `otp-local`; gửi SMS/Zalo thật cần viết adapter và cấu hình provider. |
| Google OAuth2/OIDC | Google xác thực danh tính/email; tài khoản mới bổ sung họ tên và phone liên hệ rồi thành `ROLE_USER`/`ACTIVE`. Đăng nhập sau tra theo Google `sub`. | Google Web OAuth client ID/secret và profile `google`. |

“Đăng nhập Google/Gmail” là chuyển sang trang Google để đăng nhập, **không phải gửi OTP vào Gmail**. Không cần `MAIL_USERNAME`, `MAIL_PASSWORD`, Gmail App Password hoặc SMTP cho nhánh Google. Spring Mail có sẵn trong dự án nhưng không được account flow hiện tại sử dụng.

Google chỉ xác thực email. Phone nhập khi hoàn tất hồ sơ vẫn **chưa xác thực**, không được dùng cho public OTP login hoặc cấp quyền xem đơn của người nhận. Luồng xác thực phone khi đã đăng nhập bằng Google còn phải bổ sung. Không tự ghép tài khoản chỉ vì trùng email/phone; trùng contact hiện trả lỗi, không cấp quyền vào tài khoản đã tồn tại. Role nội bộ không dùng đường đăng nhập Google/OTP dành cho khách hàng.

Đường đăng nhập mật khẩu hiện có vẫn nằm trong mục “Đăng nhập bằng mật khẩu”; tài khoản đăng ký phone có mật khẩu đã chọn. Google account không được cấp mật khẩu dùng chung hoặc mật khẩu mặc định để đăng nhập đường này.

## 2. Chuẩn bị SQL Server trước khi chạy ứng dụng

1. Chọn rõ database local phục vụ dự án/kiểm thử và tài khoản truy cập. Trên máy đang thiết lập đã chọn `ute_express` theo Phase 01, với SQL login `uteexpress_app`.
2. Với database mới, chuẩn bị schema nền từ `docs/schema.sql` đúng database đã chọn. File gốc hard-code `ute_express` và không phải script reset an toàn cho DB đã có bảng; không chạy nguyên file lên một target chưa kiểm tra.
3. Áp dụng `src/main/resources/db/migration/V2__phase_01_shipment_events_event_type.sql` theo nền Phase 01.
4. Kiểm tra dữ liệu `users` sẵn có: phone theo dạng chuẩn `+84...`, email trim/lowercase, không có collision sau chuẩn hóa. Migration account sẽ dừng nếu chưa đáp ứng; nó không tự sửa hay xác thực dữ liệu cũ.
5. Áp dụng `src/main/resources/db/migration/V3__phase_02_account_otp.sql` trong database đã chọn. Script dùng transaction/TRY CATCH, thêm `phone_verified_at`, `email_verified_at`, `google_subject` với unique index có điều kiện, và bảng `phone_otp_challenges` có FK/unique theo account.
6. Kiểm chứng schema/ràng buộc rồi chạy integration tests. `ddl-auto=none` nghĩa là khởi động Spring Boot không tự tạo những bảng/cột này.
7. Với DB đã có schema nền, chạy `src/main/resources/db/migration/V4__phase_02_handover_wait.sql` trước bản có wizard mới. Script chỉ thêm cột nullable `shipments.waiting_for_handover_at` và chạy lại an toàn; không suy đoán mốc thời gian cho đơn cũ. Trên máy hiện tại, migration này đã được áp dụng ngày 23/10; DB mới tạo từ `docs/schema.sql` đã có cột.

8. Chạy `src/main/resources/db/migration/V5__phase_02_refund_outbox.sql` để tạo bảng `refund_settlement_outbox` (nghĩa vụ hoàn cước khi hủy đơn đã thanh toán). Script chạy lại an toàn. Phân công lấy hàng cần dữ liệu `hubs` và `employee_profiles` (coordinator `COORDINATOR`/`GENERAL_STAFF`, shipper `ROLE_SHIPPER`) cùng hub.

9. Chạy `src/main/resources/db/demo/R__demo_pricing.sql` để nạp bảng giá mẫu dev/demo: 27 dòng `ACTIVE` (STANDARD/FAST/EXPRESS × 3 vùng gửi × 3 vùng nhận). Không có bảng giá thì bước 3 của wizard tạo đơn báo "Chưa có bảng giá cho dịch vụ và tuyến này" và không sang được bước 4. Script chỉ thêm tổ hợp chưa có dòng `ACTIVE`, chạy lại an toàn và không sửa giá đã có.

Bảng giá mẫu (gói đầu 500g, mỗi bước 500g thêm; bảo hiểm 0,5% giá trị khai báo, phí COD 0,8% số tiền thu hộ). Đây là giá tham khảo trong khoảng thị trường GHTK/GHN/Viettel Post/SPX, nhóm có thể thay bằng bảng giá chính thức:

| Dịch vụ | Cùng miền | Liền kề (Bắc–Trung, Trung–Nam) | Bắc–Nam |
|---|---|---|---|
| STANDARD | 22.000 (+3.000) | 30.000 (+4.000) | 35.000 (+5.000) |
| FAST | 28.000 (+4.000) | 38.000 (+5.000) | 45.000 (+6.000) |
| EXPRESS | 40.000 (+6.000) | 55.000 (+8.000) | 65.000 (+10.000) |

Trạng thái `ACTIVE` cũ không tự trở thành bằng chứng phone/email đã xác thực. Migration không kích hoạt hoặc đánh dấu verified cho người dùng cũ. Ngày 23/10 đã kiểm tra các bảng/cột account, shipment, payment cần dùng trên DB này và chạy thành công 11 SQL integration tests; không chạy lại schema nền hoặc reset DB.

## 3. Biến môi trường

| Biến | Mục đích |
|---|---|
| `UTEEXPRESS_SQLSERVER_URL` | JDBC URL đến đúng database đã chọn. |
| `UTEEXPRESS_SQLSERVER_USERNAME` | SQL Server app/test username. |
| `UTEEXPRESS_SQLSERVER_PASSWORD` | Mật khẩu của tài khoản SQL đó. |
| `UTEEXPRESS_JWT_SECRET` | Secret Base64 giải mã được ít nhất 32 byte cho HS256; cần cả khi chỉ thử giao diện. |
| `UTEEXPRESS_JWT_EXPIRATION_SECONDS` | Tùy chọn; mặc định 1800 giây. |
| `SPRING_PROFILES_ACTIVE` | `otp-local`, `google`, hoặc `otp-local,google` theo mục đích bên dưới. |
| `UTEEXPRESS_GOOGLE_CLIENT_ID` | Chỉ cần khi bật profile `google`. |
| `UTEEXPRESS_GOOGLE_CLIENT_SECRET` | Chỉ cần khi bật profile `google`. |

`application.properties` hiện đọc trực tiếp ba biến `UTEEXPRESS_SQLSERVER_*`; profile test `sqlserver-verification` cũng dùng chúng và SQL tests kiểm tra sự tồn tại của chúng. Spring Boot còn hỗ trợ `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` để override cấu hình runtime, nhưng chỉ đặt bộ biến này **không đủ bật SQL integration tests**. Tránh đặt hai bộ biến trỏ tới database khác nhau.

Ví dụ dạng URL local: `jdbc:sqlserver://localhost:1433;databaseName=<database_đã_chọn>;encrypt=true;trustServerCertificate=true`. Thay tên DB/host/port theo instance thật; `trustServerCertificate=true` chỉ phù hợp cho chứng chỉ local đã kiểm soát, không phải cấu hình mặc định cho triển khai công khai.

Secret đặt trong môi trường hoặc file local đã Git ignore, không ghi vào Java/properties có commit. Cách chạy chính trên máy này là Eclipse/STS với file local ở mục 4; không cần launcher script.

## 4. Thử OTP mô phỏng local

### Cấu hình trực tiếp trên máy đang thiết lập

Người dùng chọn cấu hình bằng `src/main/resources/application-local.properties`. File này đã được Git ignore; nhập URL `jdbc:sqlserver://localhost:1433;databaseName=ute_express;encrypt=true;trustServerCertificate=true`, username `uteexpress_app`, mật khẩu SQL riêng và JWT secret local. File mẫu là `src/main/resources/application-local.example.properties`; mẫu phải được sao chép sang tên file local trước khi nhập thông tin riêng.

Chạy `vn.iotstar.UteExpressApplication` bằng JDK 22 trong IDE với program arguments `--spring.profiles.active=local,otp-local`. Spring Boot đọc file local, rồi bật OTP mô phỏng. Mở `http://localhost:8080/register` sau khi ứng dụng báo đã khởi động; dừng ứng dụng bằng nút Stop của IDE.

Trong Eclipse/STS: mở **Run → Run Configurations… → Java Application**, chọn cấu hình `UTEExpress` và main class `vn.iotstar.UteExpressApplication`. Ở **Arguments → Program arguments**, dùng `--spring.profiles.active=local,otp-local`; **Working directory** là `E:\Project_WEB`. Ở **JRE**, chọn JDK 22; bật **Exclude test code** để cấu hình trong test resources không che cấu hình runtime. Nếu dùng loại cấu hình **Spring Boot App** của STS, có thể nhập `local,otp-local` vào mục **Profiles** thay cho program arguments.

Cấu hình launch local đã được chỉnh sẵn trong workspace Eclipse. Đóng/mở lại Eclipse, chọn project và nhấn **F5**, kiểm tra cấu hình trên rồi **Apply → Run**. Chưa có bằng chứng chạy trực tiếp trong IDE sau chỉnh sửa; các kiểm thử HTTP đã chạy bằng WAR. `.metadata/` được Git ignore để giữ cấu hình IDE riêng trên máy.

Lần chạy IDE ngày 23/10 vẫn gặp `No static resource index`: classpath thực tế đặt `target/test-classes` trước `target/classes`, khiến file cấu hình test che cấu hình MVC. Đã sửa key launch thành `org.eclipse.jdt.launching.ATTR_EXCLUDE_TEST_CODE` theo plugin JDT đang cài (key cũ thiếu `ATTR_`). Chạy lại cùng classpath sau khi loại test classes cho kết quả HTTP 200 tại `/`, `/login`, `/register`; log `target/eclipse-classpath-fix.log`. Eclipse đang mở có thể giữ launch trong bộ nhớ: bật **Exclude test code** trong tab **Dependencies/Classpath**, **Apply**, dừng app cũ rồi chạy lại; hoặc thoát IDE trước khi nạp lại launch đã sửa. Không thêm cấu hình JSP trùng vào file local để che lỗi classpath.

`maven-war-plugin` loại `WEB-INF/classes/application-local.properties` khỏi WAR để cấu hình SQL/JWT riêng chỉ phục vụ chạy local. Khi triển khai, cấp biến môi trường SQL/JWT và credentials provider theo môi trường đó. Ngày 23/10/2026 đã build thành công và kiểm tra file local không có trong WAR.

Bộ script `Start-UTEExpress.cmd` / `scripts/start-local.ps1` không còn được sử dụng. Các test SQL yêu cầu ba biến `UTEEXPRESS_SQLSERVER_*` để được kích hoạt; chỉ nhập file local chưa đủ bật các test này. Trong lần build 151 test, cấu hình local đã được nạp riêng cho tiến trình Maven và không có SQL test bị skipped.

### Cấu hình thủ công khi cần kiểm tra từng bước

Chỉ thực hiện sau khi database/schema đã sẵn sàng. Mở PowerShell ở thư mục dự án; các giá trị nhập dưới đây chỉ tồn tại trong phiên terminal:

```powershell
Set-Location E:\Project_WEB
. .\.local\local-env.ps1
$env:UTEEXPRESS_SQLSERVER_URL = Read-Host 'JDBC URL của database đã chọn'
$env:UTEEXPRESS_SQLSERVER_USERNAME = Read-Host 'SQL username'
$env:UTEEXPRESS_SQLSERVER_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'SQL password' -AsSecureString)).Password
$env:SPRING_PROFILES_ACTIVE = 'otp-local'
mvn spring-boot:run
```

Nếu dùng máy khác không có helper local, cấu hình JDK 22/Maven và `UTEEXPRESS_JWT_SECRET` trước. Không bật `otp-local` trên môi trường dùng chung hoặc công khai: ai đọc được inbox local có thể lấy mã mô phỏng.

Mở [trang đăng ký local](http://localhost:8080/register), nhập thông tin và gửi form. Mã phát triển được ghi tại `.local/otp-inbox/<challenge-UUID>.txt` tính từ thư mục chạy ứng dụng. File chứa mã, hạn UTC và ba số cuối điện thoại; `.local/` được `.gitignore` loại trừ. Không có SMS hay tin Zalo nào được gửi. Nhà phát triển đọc file tương ứng rồi nhập mã ở trang xác minh; đây là kiểm thử logic, không chứng minh sở hữu điện thoại ngoài đời.

- Sau kích hoạt, ứng dụng về trang đăng nhập. Có thể dùng mật khẩu hoặc yêu cầu OTP login mới.
- Mã có 6 chữ số, hết hạn sau 5 phút; tối đa 5 lần thử sai cho mỗi mã. Chỉ mã mới nhất có hiệu lực, dùng đúng một lần và đúng purpose.
- Gửi lại cách tối thiểu 60 giây; tối đa 5 lần gửi trong một giờ theo account. Đăng ký và đăng nhập dùng chung cửa sổ gửi, nên thử OTP login ngay sau đăng ký có thể phải chờ cooldown.
- Nếu mất session khi đăng ký chưa xong, gửi lại form đăng ký với đúng username, password, phone/email sau chuẩn hóa. Mã hiện tại hỗ trợ tiếp tục account `INACTIVE` cũ, không đổi họ tên/thông tin đã lưu và không reset giới hạn gửi.
- Dọn file mã đã hết hạn trong inbox local khi kết thúc thử nghiệm. Chưa có job tự dọn file. Bảng challenge giữ tối đa một row/account; migration có ghi chú bảo trì hash đã vô hiệu sau tối thiểu 7 ngày, nhưng chưa có job thực hiện và không được xóa/tạo lại row để reset giới hạn gửi.

Nếu không bật `otp-local` và chưa cung cấp adapter thật, `delivery-mode=disabled`: gửi OTP báo không khả dụng, đăng ký mới không commit account sau lỗi delivery. Không đổi cấu hình SMTP để xử lý lỗi này.

## 5. Cấu hình Google sau khi có credentials

1. Tạo/chọn Google Cloud project và cấu hình màn hình consent/audience trong Google Auth Platform. Nếu ứng dụng còn ở chế độ Testing, thêm tài khoản sẽ dùng vào danh sách test users theo cấu hình của Google.
2. Tạo OAuth client loại **Web application**. Đăng ký authorized redirect URI đúng tuyệt đối: `http://localhost:8080/login/oauth2/code/google`.
3. Ứng dụng dùng scope tối thiểu `openid`, `profile`, `email`. Không yêu cầu quyền đọc/gửi Gmail.
4. Đặt client ID/secret qua môi trường và bật profile `google`. Trên cùng terminal đã có SQL/JWT ở mục trước:

```powershell
$env:UTEEXPRESS_GOOGLE_CLIENT_ID = Read-Host 'Google Web client ID'
$env:UTEEXPRESS_GOOGLE_CLIENT_SECRET = [System.Net.NetworkCredential]::new('', (Read-Host 'Google client secret' -AsSecureString)).Password
$env:SPRING_PROFILES_ACTIVE = 'otp-local,google'
mvn spring-boot:run
```

Dùng `google` nếu chỉ cần Google và muốn OTP delivery tiếp tục tắt. Dùng `otp-local,google` khi kiểm thử cả Google thật và OTP mô phỏng trên máy phát triển. Dừng phiên app cũ trước khi khởi động lại cùng port 8080.

Mở [trang đăng nhập local](http://localhost:8080/login), chọn **Đăng nhập bằng Google**. Nút chỉ xuất hiện khi có Google client registration. Lần đầu xác thực thành công sẽ đến `/auth/google/complete` để bổ sung họ tên/phone; bằng chứng onboarding lưu phía server và hết hạn sau 10 phút. Hoàn tất mới tạo local account/session. Lần sau đăng nhập dùng Google `sub` đã liên kết.

Callback thực tế được suy ra từ `{baseUrl}/login/oauth2/code/{registrationId}`. Nếu đổi host, port, context path hoặc deploy HTTPS, đăng ký chính xác callback tương ứng ở Google; localhost và 127.0.0.1 không phải cùng một redirect URI. Lỗi `redirect_uri_mismatch` cần sửa URI, không liên quan Gmail SMTP.

Nguồn chính thức: [Spring Security OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html), [Google OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect), [Google OAuth2 cho web server](https://developers.google.com/identity/protocols/oauth2/web-server).

## 6. OTP về Zalo và chi phí

| Phương án | Chi phí/điều kiện | Tình trạng dự án |
|---|---|---|
| OTP inbox local | Không phát sinh phí tin nhắn; chỉ mô phỏng trên máy phát triển. | Đã có. |
| Google OIDC | Không cần mua/gửi tin OTP từ ứng dụng cho mỗi lần đăng nhập; chi phí hạ tầng ứng dụng là phần riêng. | Đã có mã; chưa cấu hình OAuth client thật. |
| Zalo ZBS Template Message, tên cũ thường gặp là ZNS | Bảng giá tra cứu ngày 22/10/2026: mẫu xác thực **300 đồng/tin thành công, chưa VAT**; có thể có chi phí gói OA/điều kiện dịch vụ khác. | Chưa tích hợp adapter hoặc credentials. |

Ví dụ 1.000 tin xác thực thành công theo mức trên là khoảng **300.000 đồng trước VAT**, chưa tính chi phí OA/gói khác. Tra lại giá hiện hành trước khi mua. Zalo không có đường gửi OTP chính thức miễn phí bằng tài khoản Zalo cá nhân để thay phần tích hợp doanh nghiệp.

Để dùng kênh chính thức, cần OA doanh nghiệp đã xác thực, tài khoản dịch vụ ZBS/ZCA phù hợp, ứng dụng/AppID, access token và mẫu tin OTP được duyệt. Tài liệu đăng ký nêu thời gian xét duyệt mẫu khoảng 2–3 ngày làm việc; không nên xem đây là thao tác cấu hình tức thời. Khi triển khai sẽ viết `OtpDelivery` adapter và quản lý token/lỗi gửi theo provider, không chỉ đổi host SMTP.

Nguồn chính thức: [Bảng giá Zalo](https://zalo.cloud/zns/pricing), [Giới thiệu ZBS Template Message](https://zalo.solutions/business-message/guidelines/intro), [Hướng dẫn đăng ký dịch vụ](https://zalo.solutions/blog/huong-dan-dang-ky-su-dung-dich-vu-zns/jlm44tzzwffh0od10ix6rra7). Một số đường dẫn ZNS cũ chuyển sang dịch vụ ZBS hiện tại.

Để làm đồ án nhanh với chi phí thấp: dùng Google cho đăng nhập thật sau khi có client, dùng `otp-local` kiểm thử nhánh điện thoại; khi cần chứng minh nhận mã trên số điện thoại thật mới chọn/tích hợp nhà cung cấp.

## 7. Những việc vẫn cần hoàn tất

- Mở lại Eclipse, refresh project và kiểm thử trực tiếp đăng ký → OTP local → kích hoạt → đăng nhập trên trình duyệt. Automated HTTP và SQL tests đã đạt; vẫn cần nghiệm thu thao tác/giao diện thực tế.
- Cấu hình Google client và kiểm tra callback, onboarding, đăng nhập lại, collision và account bị khóa bằng trình duyệt.
- Chọn và triển khai provider OTP thật; thử delivery failure, resend, expiry, replay và giới hạn thử/gửi.
- Bổ sung phone verification an toàn khi đã đăng nhập bằng Google nếu cần liên kết số điện thoại để nhận quyền khác.
- Nhận bảng giá của nhóm do người dùng cung cấp để mở kiểm thử báo giá/tạo đơn trên giao diện. DB hiện chưa có policy; không seed giá mẫu. Fixture giá trong SQL tests được dọn sau kiểm thử.
- Form tạo đơn đã chia 4 bước. Sau khi có bảng giá, xem báo giá rồi mới xác nhận tạo đơn trong cùng phiên đăng nhập. Đổi thông tin hoặc bảng giá thay đổi sẽ yêu cầu báo giá lại; chọn cách bàn giao ở bước cuối không đổi giá hiện tại. Mỗi phiên giữ một báo giá mới nhất; hết phiên/mở báo giá mới ở tab khác thì cần báo giá lại.
- Hoàn thành UAT/review và tiếp tục các gói intake, wizard, first-mile, tracking/refund trong [checklist Phase 02](PHASE_02_DIRECTOR_CHECKLIST.md). `ReceiverLinkService` đã có unit/SQL evidence cho snapshot bất biến, rollback, phone không khớp, receiver đã liên kết và claim đồng thời.

Kiểm tra cuối ngày 22/10/2026: `mvn -B package` thành công, **118 test đạt, 4 test SQL Server bị bỏ qua** vì thiếu cấu hình. Bốn test HTTP thật đã biên dịch/render đủ năm trang JSP qua Tomcat/Jasper với service giả lập. Kết quả theo phiên nằm ở checklist; đây chưa phải bằng chứng SQL Server, đăng nhập Google thật hoặc UAT bằng trình duyệt.

Lần rà soát đầu ngày 23/10/2026: **127 test không dùng SQL Server đạt**; bốn SQL tests được loại khỏi lần chạy để sửa fixture cũ trước. Đây là kết quả lịch sử trước các sửa lỗi dưới đây.

Kết quả sau đợt sửa lỗi ngày 23/10/2026: **151 test đạt, 0 failures/errors/skips**, gồm 6 test SQL account/linking và 5 test SQL shipment/payment; WAR build thành công. HTTP trên WAR đã qua đăng ký, OTP local, kích hoạt, đăng nhập và thông báo an toàn khi chưa có bảng giá. Bằng chứng: `target/phase02-fix-final-build.log`, `target/phase02-fix-http.json`.

Kết quả mới nhất ngày 24/10: **197 test chạy, 0 lỗi, 9 SQL Server tests bỏ qua** khi không bật biến môi trường. Lần chạy SQL đạt gần nhất (171 test, 11 SQL, 23/10) là trước khi thêm first-mile/tracking/refund. Đây chưa thay thế UAT theo vai trò hoặc Google/provider thật. Xem [nhật ký checklist](PHASE_02_DIRECTOR_CHECKLIST.md).
