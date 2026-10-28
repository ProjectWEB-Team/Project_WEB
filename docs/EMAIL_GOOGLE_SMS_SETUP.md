# Google, Gmail SMTP và OTP điện thoại

Thiết lập local hiện tại: `http://localhost:8080` (file chạy sẵn: `.local/run-google-mail.ps1`). Google và SMTP là hai cấu hình độc lập. Google dùng OAuth Client ID/Client secret; SMTP dùng địa chỉ Gmail và App Password. Không gửi các khóa này vào chat hoặc commit lên Git.

## 1. Điền thông tin riêng trên máy

Sao chép `deploy/account-providers.local.example.properties` thành `.local/account-providers.properties`. Thư mục `.local/` được Git bỏ qua và không đóng gói vào WAR. Bốn giá trị cần điền:

| Khóa | Lấy ở đâu |
|---|---|
| `UTEEXPRESS_GOOGLE_CLIENT_ID` | Google Auth Platform → Clients → OAuth client loại Web application |
| `UTEEXPRESS_GOOGLE_CLIENT_SECRET` | Cùng OAuth client ở trên |
| `MAIL_USERNAME` | Địa chỉ Gmail dùng làm người gửi |
| `MAIL_PASSWORD` | App Password của Gmail, không phải mật khẩu đăng nhập Google |

File `.properties` đọc ký tự `\` như dấu escape. Nếu secret có dấu này, dùng `\\` hoặc cấp secret qua biến môi trường. Không thêm dấu nháy quanh giá trị. App Password Gmail nhập liền, bỏ khoảng trắng phân nhóm.

## 2. Google login

1. Mở [Google Cloud Console](https://console.cloud.google.com/), tạo hoặc chọn project.
2. Trong Google Auth Platform, khai báo Branding/Audience và tài khoản thử nghiệm nếu ứng dụng ở chế độ Testing.
3. Tạo client loại **Web application** và thêm chính xác các Authorized redirect URIs sẽ dùng:
   - `http://localhost:8080/login/oauth2/code/google`
   - `http://127.0.0.1:8080/login/oauth2/code/google`
   - `https://uteexpress.event5g.fun/login/oauth2/code/google`
4. Điền Client ID/Client secret vào file riêng ở mục 1. Khi đưa lên server, thêm callback HTTPS theo đúng tên miền; không dùng callback localhost cho server.

Ứng dụng chỉ xin `openid,profile,email`, không xin quyền đọc Gmail. Lần đầu đăng nhập Google, khách hàng bổ sung tên và số liên hệ. Google xác thực email; số điện thoại vẫn chưa xác thực. Không tự ghép tài khoản chỉ vì trùng email/số điện thoại; tài khoản trùng sẽ được yêu cầu dùng phương thức đăng nhập đã có.

## 3. Gmail SMTP

1. Bật xác minh hai bước trong [bảo mật tài khoản Google](https://myaccount.google.com/security).
2. Tạo [App Password](https://myaccount.google.com/apppasswords) cho UTEExpress rồi điền `MAIL_USERNAME`/`MAIL_PASSWORD` vào file riêng. Nếu không có mục App Password, kiểm tra chính sách Workspace hoặc điều kiện tài khoản theo [hướng dẫn Google](https://support.google.com/accounts/answer/185833).
3. Profile `smtp` sử dụng `smtp.gmail.com:587`, bắt buộc STARTTLS và kiểm tra chứng chỉ, có giới hạn thời gian kết nối/đọc/ghi. Có thể thay `MAIL_HOST`/`MAIL_PORT` bằng nhà cung cấp hỗ trợ STARTTLS khác.

SMTP được dùng cho:

- Đăng ký qua `/auth/email/register`: tài khoản chỉ kích hoạt sau khi nhập đúng mã email.
- `/user/email`: khách hàng đã có tài khoản xác thực email liên hệ để nhận thông báo và khôi phục mật khẩu.
- `/auth/email/forgot-password`: khôi phục mật khẩu bằng email **đã xác thực** của khách hàng đang hoạt động. Email không tồn tại/chưa xác thực/bị khóa/tài khoản nhân viên không cấp quyền đặt lại mật khẩu.
- Các thông báo riêng đang được hệ thống phát về đơn hàng/tài khoản: gửi thêm email cho khách hàng có email đã xác thực, sau khi giao dịch dữ liệu thành công. Không gửi thông tin địa chỉ, tiền hoặc dữ liệu nội bộ ngoài payload thông báo sẵn có.

Email OTP lưu bản băm, dùng một lần, mặc định hết hạn sau 5 phút, tối đa 5 lần nhập sai, gửi lại sau 60 giây và tối đa 5 lần gửi/giờ/tài khoản. Xác thực email **không** xác thực điện thoại hoặc tự cấp quyền xem đơn dành cho người nhận. Các giới hạn email và điện thoại được lưu độc lập.

Thông báo email được ghi vào `notification_email_outbox` cùng giao dịch nghiệp vụ. Giao dịch bị hủy thì email cũng không được xếp hàng. Worker gửi sau khi dữ liệu đã lưu, tối đa 10 thư mỗi lượt, kiểm tra lại người nhận còn hoạt động và email còn xác thực. Nếu SMTP lỗi, hệ thống thử lại sau 60 giây rồi tăng dần, tối đa 1 giờ giữa các lần, tối đa 8 lần gửi. Lease 2 phút giúp worker khác tiếp tục nếu tiến trình bị dừng. Profile `smtp` dùng 2 luồng lịch chạy để SMTP không chiếm hết luồng xử lý thời hạn đơn hàng.

Email gửi xong hoặc bị hủy được xóa địa chỉ/nội dung khỏi hàng đợi; bản ghi kết thúc được dọn sau 30 ngày. Bản ghi `FAILED` giữ nội dung trong thời gian này để người vận hành xử lý. Nếu SMTP đã nhận thư rồi app dừng trước khi ghi nhận thành công, thư có thể được gửi lặp; đây là cơ chế ít nhất một lần, không cam kết gửi đúng một lần.

Theo dõi trên SQL Server bằng các truy vấn chỉ đọc:

```sql
SELECT status, COUNT(*) AS total FROM dbo.notification_email_outbox GROUP BY status;
SELECT id, attempts, last_error, created_at, available_at
FROM dbo.notification_email_outbox
WHERE status = 'FAILED' OR (status IN ('PENDING','SENDING') AND available_at < DATEADD(minute,-10,SYSUTCDATETIME()));
```

OTP vẫn gửi trực tiếp: gửi thất bại khi đăng ký sẽ rollback tài khoản mới; yêu cầu khôi phục luôn trả thông báo chung để không lộ việc email có tài khoản hay không. Thay mật khẩu, quyền hoặc khóa tài khoản làm mất hiệu lực JWT/phiên web cũ ở yêu cầu tiếp theo và chặn nội dung WebSocket ở lần gửi tiếp theo.

Gmail có hạn mức gửi, phù hợp thử nghiệm. Với AWS, có thể dùng Amazon SES SMTP: xác minh tên miền/người gửi, cấu hình SPF/DKIM/DMARC theo hướng dẫn của SES, xin ra khỏi sandbox trước khi gửi tới khách hàng, rồi đặt `MAIL_HOST`, `MAIL_PORT=587`, `MAIL_USERNAME`, `MAIL_PASSWORD` và **`MAIL_FROM`** là địa chỉ gửi đã xác minh. SMTP credentials của SES khác IAM access keys. Giá, hạn mức và quyền gửi phụ thuộc tài khoản AWS; CI/CD không tự tạo dịch vụ trả phí.

## 4. Khởi động sau khi đã điền đủ thông tin

Dừng phiên app cũ chiếm port 8081. Trên máy đã có cấu hình SQL/JWT local:

```powershell
mvn spring-boot:run '-Dspring-boot.run.profiles=local,demo,otp-local,google,smtp' '-Dspring-boot.run.arguments=--server.port=8081 --server.address=127.0.0.1 --spring.config.additional-location=file:.local/account-providers.properties'
```

Nếu chỉ bật Google, bỏ `smtp`; nếu chỉ bật email, bỏ `google`. Các khóa của phần chưa bật không cần điền. Profile `otp-local` vẫn chỉ ghi mã điện thoại vào inbox phát triển; không gửi SMS. Không bật `demo` hoặc `otp-local` cho website công khai. Database mới/cũ sẽ được Flyway thêm bảng `email_otp_challenges` qua V20; không xóa hoặc sửa dữ liệu người dùng cũ.

Trong Eclipse/STS, thêm cùng profiles và program arguments `--server.port=8081 --server.address=127.0.0.1 --spring.config.additional-location=file:.local/account-providers.properties`. Working directory là `E:\Project_WEB`. Không đưa file bí mật này vào source resources.

Kiểm tra thực tế sau khi có credentials:

1. Google: nút hiện tại `/login`, xác thực → bổ sung hồ sơ → đăng nhập lại bằng cùng Google account.
2. Email: đăng ký bằng địa chỉ bạn sở hữu, nhận mã thật, mã cũ không dùng lại được; phone chưa bị đánh dấu verified.
3. Quên mật khẩu bằng email đã xác thực, thử mã sai/hết hạn và đăng nhập bằng mật khẩu mới.
4. Tạo/cập nhật đơn trên tài khoản có email đã xác thực, kiểm tra thư nhận và thư rác.

## 5. Chọn dịch vụ OTP điện thoại ở Việt Nam

**Đã bổ sung adapter SpeedSMS cho Verify/Notify.** Xem [hướng dẫn cấu hình và kiểm tra SMS](SPEEDSMS_SETUP.md). Token điền riêng trong `.local/sms-provider.properties` hoặc `/etc/uteexpress.env`; bật profile `sms` và bỏ `otp-local`. Chưa gửi SMS thật cho tới khi bạn cấu hình và thử trên số của mình.

SMTP không gửi SMS. Chỉ bật gửi điện thoại thật sau khi chọn nhà cung cấp, được cấp khóa và hoàn tất kết nối API.

| Phương án | Phù hợp khi | Cần chuẩn bị |
|---|---|---|
| [eSMS](https://esms.vn/) hoặc [SpeedSMS](https://speedsms.vn/) | Muốn gửi SMS tới số Việt Nam | Tài khoản dịch vụ, xác nhận hỗ trợ SMS OTP và đối tượng cá nhân/đồ án, điều kiện sender/brandname, khóa API, ngân sách gửi |
| [Twilio Verify](https://www.twilio.com/en-us/verify) | Muốn dùng dịch vụ xác thực quốc tế | Tài khoản, kiểm tra khả năng gửi tới Việt Nam, giới hạn trial và các số được phép thử; Verify cần tích hợp cả API gửi và kiểm tra mã |
| [Zalo Cloud](https://zalo.cloud/) | Đã có OA doanh nghiệp và mẫu tin được duyệt | OA, ứng dụng/khóa hoặc token, mẫu xác thực, điều kiện dịch vụ và tài khoản thanh toán |

Khuyến nghị cho đồ án: làm Google + email thật trước; liên hệ eSMS/SpeedSMS để xác nhận có cấp thử nghiệm OTP cho tài khoản cá nhân hay không. Không mặc định có gói miễn phí, không cam kết giá hoặc khả năng nhận tin khi chưa được nhà cung cấp xác nhận. Chưa có OA thì Zalo thường cần thêm bước đăng ký/xác minh trước khi tích hợp.

Sau khi chọn dịch vụ, cần tài liệu API, khóa tài khoản, sender/template được chấp thuận và số điện thoại thuộc quyền sử dụng của bạn để thử. Code điện thoại hiện có boundary `OtpDelivery`; nhà cung cấp gửi SMS trực tiếp có thể nối vào boundary đó. Không đổi SMTP thành SMS và không dùng mã giả lập để tuyên bố đã xác thực số điện thoại thật.

Chưa bật Facebook theo lựa chọn Google trước. Không có khóa dịch vụ nào trong file mẫu này.
