# SMS OTP bằng SpeedSMS

Phần code đã có adapter gửi SMS qua SpeedSMS cho đăng ký, đăng nhập bằng số điện thoại và quên mật khẩu. Ứng dụng tự tạo, lưu bản băm và kiểm tra mã OTP; SpeedSMS chỉ gửi tin qua SMS API. Google/Gmail không cần thiết cho luồng này.

## 1. Việc bạn thực hiện trên SpeedSMS

1. Vào [Settings → Profile](https://connect.speedsms.vn/) để lấy API Access Token. Token đã xuất hiện trong chat cần được đổi/thu hồi; nếu không có chức năng đổi trên dashboard, nhờ hỗ trợ SpeedSMS thực hiện.
2. Xác nhận tài khoản được bật **SMS API dùng tên gửi Verify hoặc Notify** và mẫu tin OTP được chấp thuận. Đăng ký tài khoản hoặc có số dư chưa đồng nghĩa API đã được cấp quyền gửi.
3. Xác nhận giá mỗi SMS, mức nạp tối thiểu, hạn mức gửi/chi tiêu; sau đó nạp số tiền bạn chọn. Không cần dùng mục lập lịch SMS tự động cho OTP.
4. Nếu bật giới hạn IP gọi API, cho phép IP đi ra Internet của máy chạy app. Khi chạy trên EC2 hiện tại là `54.253.15.25` nếu máy đi Internet trực tiếp bằng IP đó; khi chạy local, dùng IP mạng hiện tại của bạn.

[Tài liệu API](https://speedsms.vn/sms-api-service/) và [dịch vụ OTP](https://speedsms.vn/sms-otp-service/) là nguồn tham khảo. Các mã lỗi tài khoản/quyền gửi cần nhà cung cấp hỗ trợ; code không tự mua gói hoặc kích hoạt dịch vụ.

## 2. Điền token trên máy cá nhân

File riêng đã chuẩn bị: **`E:\Project_WEB\.local\sms-provider.properties`**. Nếu dùng máy khác, sao chép `deploy/sms-provider.local.example.properties` vào `.local/sms-provider.properties` trước.

Điền token mới sau dấu `=` của dòng `SPEEDSMS_ACCESS_TOKEN`; giữ `SPEEDSMS_SENDER=Verify`, hoặc đổi thành `Notify` nếu nhà cung cấp yêu cầu. File này được Git bỏ qua và không nằm trong WAR. Không điền token vào file mẫu trong `deploy/`, source code, GitHub issue hay chat.

```properties
SPEEDSMS_ACCESS_TOKEN=TOKEN_MOI_CUA_BAN
SPEEDSMS_SENDER=Verify
```

Mẫu gửi mặc định:

```text
UTEExpress: Ma xac thuc cua ban la {code}. Hieu luc {minutes} phut. Khong chia se ma nay.
```

Nếu SpeedSMS duyệt mẫu khác, đặt `SPEEDSMS_MESSAGE_TEMPLATE` trong cùng file. Giữ `{code}`; `{minutes}` là tùy chọn. Nội dung chỉ dùng chữ không dấu, số và dấu câu thông dụng, tối đa 160 ký tự sau thay mã để gửi trong một SMS. Tên thương hiệu riêng như UTEExpress cần đăng ký và một kiểu gửi khác; adapter hiện chỉ dùng Verify/Notify.

Sau khi điền đủ thông tin, dừng phiên app cũ rồi chạy từ `E:\Project_WEB`:

```powershell
mvn spring-boot:run '-Dspring-boot.run.profiles=local,demo,sms' '-Dspring-boot.run.arguments=--server.port=8081 --server.address=127.0.0.1 --spring.config.additional-location=file:.local/sms-provider.properties'
```

`demo` chỉ phục vụ thử nghiệm trên máy cá nhân. **Bỏ `otp-local` khi bật `sms`**; app sẽ từ chối khởi động nếu hai profile cùng bật hoặc token còn trống. Không cần bật `smtp` hay `google` để thử SMS.

## 3. Khi bạn triển khai lên AWS

Sau khi bản có adapter này đã được triển khai, chỉnh `/etc/uteexpress.env` trên VPS, thêm `SPEEDSMS_ACCESS_TOKEN` và `SPEEDSMS_SENDER`, giữ file chỉ root đọc/ghi (`chmod 600`). Thêm `sms` vào `SPRING_PROFILES_ACTIVE`: ví dụ `prod,sms`, hoặc `prod,google,smtp,sms` nếu các dịch vụ còn lại đã cấu hình. Bỏ `demo` và `otp-local` cho website phục vụ khách hàng thật.

```bash
sudo nano /etc/uteexpress.env
sudo chmod 600 /etc/uteexpress.env
sudo systemctl restart uteexpress
sudo journalctl -u uteexpress -n 100 --no-pager
```

Không đưa token SMS vào biến build trong GitHub Actions. Cấu hình ở VPS giúp token nằm ngoài artifact. Đảm bảo Nginx giới hạn yêu cầu đăng nhập/OTP như hướng dẫn trong `GITHUB_ACTIONS_CICD.md`; đồng thời đặt hạn mức chi tiêu ở nhà cung cấp.

## 4. Kiểm tra nhận mã thật do bạn thực hiện

1. Đăng ký bằng số điện thoại bạn kiểm soát; kiểm tra mã thực sự đến máy và tài khoản chỉ kích hoạt sau khi nhập đúng mã.
2. Thử đăng nhập bằng số vừa xác thực và khôi phục mật khẩu. Số chưa xác thực không được dùng để khôi phục tài khoản.
3. Thử gửi lại sau 60 giây; mã trước đó phải hết hiệu lực. Mặc định mã hết hạn sau 5 phút, tối đa 5 lần nhập sai và 5 lần gửi/giờ/tài khoản.
4. Xem lịch sử gửi/số dư trong dashboard SpeedSMS. Ứng dụng báo gửi được nghĩa là nhà cung cấp đã nhận yêu cầu, chưa bảo đảm điện thoại đã nhận tin.

Nếu báo không thể gửi: xem quyền API, số dư, mẫu tin và lịch sử tại SpeedSMS. Các mã lỗi trong tài liệu gồm `007` (IP bị chặn), `008` (tài khoản bị khóa), `009` (chưa được phép gọi API), `300` (không đủ số dư). Ứng dụng trả lỗi chung để tránh lộ token, số điện thoại và mã OTP trong thông báo.

Kết nối dùng HTTPS có kiểm tra chứng chỉ, token trong header Basic authentication, POST JSON; giới hạn kết nối 5 giây và chờ phản hồi 10 giây. Không tự gửi lại khi timeout vì nhà cung cấp có thể đã nhận yêu cầu tính phí. Không bật HTTP wire logging chứa headers/nội dung tin trên môi trường thật.

Chưa thử gửi tới điện thoại thật và chưa phát sinh phí SMS trong quá trình viết/kiểm thử code. Cấu hình token, kích hoạt dịch vụ, nạp tiền và triển khai do bạn thực hiện.
