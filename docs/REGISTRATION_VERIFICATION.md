# Kiểm chứng đăng ký local — 25/10/2026

## Nguyên nhân và sửa chữa

Database chạy ứng dụng `ute_express` chỉ có schema tài khoản nền, thiếu `users.phone_verified_at`, `users.email_verified_at`, `users.google_subject` và bảng `phone_otp_challenges`. Truy vấn JPA đọc User thất bại trước bước gửi OTP. Profile thiếu `otp-local` ở lần chạy trước cũng đã được sửa, nhưng bật profile không tự áp dụng migration vì `ddl-auto=none`.

SQL login ứng dụng không có quyền ALTER. Người dùng đã chạy script quản trị local bằng Windows Authentication: chuẩn hóa phone/email cho 10 tài khoản cũ sau khi kiểm tra không xung đột và áp dụng 16 migration từ manifest trong một outer transaction. Script đã báo commit thành công. Không sửa mật khẩu, role hoặc trạng thái kích hoạt của các tài khoản cũ. Bản sao định danh trước chuẩn hóa được lưu trong `.local/` trên máy, không đưa vào Git.

## Bằng chứng sau sửa trên database ứng dụng

Đã chạy ứng dụng với `local,otp-local` trên cổng 8080, trỏ trực tiếp tới `ute_express`, và dùng HTTP thực với cookie phiên cùng CSRF, không mock controller hay adapter OTP:

| Bước | Kết quả |
|---|---|
| GET form và POST đăng ký với thông tin thử nghiệm mới | Chuyển tới `/auth/verify-phone` |
| Adapter `local-inbox` ghi OTP | Đọc được file dành cho lượt đăng ký thử nghiệm |
| POST xác minh OTP | Chuyển tới `/login?registered=true` |
| POST đăng nhập bằng mật khẩu | Chuyển tới `/user/shipments/create` |
| GET `/user/shipments` bằng phiên vừa đăng nhập | HTTP 200, không bị chuyển về login |
| Đối chiếu JDBC trên `ute_express` | Tài khoản thử nghiệm `codexreg_283deecdc27d`, id 173, ROLE_USER, ACTIVE, phone_verified_at khác NULL, một challenge đã consumed |
| Kiểm tra định danh sau migration | Không còn dòng cần chuẩn hóa |

Thời điểm xác minh ghi trong database: 16:22:12 UTC, tương ứng 23:22:12 giờ Việt Nam ngày 25/10/2026. Tài khoản thử nghiệm được giữ lại làm bằng chứng; mật khẩu ngẫu nhiên, mã OTP và cookie không có trong báo cáo này.

Kiểm chứng bổ sung trước đó: 33 kiểm thử tài khoản hiện có đạt; toàn bộ script sửa schema đã thực thi trên database test với ba ví dụ phone/email legacy rồi rollback; luồng HTTP tương tự cũng đã đạt trên database test riêng. Những kết quả đó được phân biệt với lượt chạy thật trên `ute_express` ở bảng trên.

## Chạy lại bằng Spring Tools

Chạy main `vn.iotstar.UteExpressApplication` của đúng project với profile `local,otp-local`, bật Exclude test code và dùng working directory của checkout. Mở `http://localhost:8080/register` sau khi ứng dụng khởi động.

Đăng ký thông tin chưa thuộc tài khoản khác. Khi chuyển tới trang xác minh, mở file `.txt` mới nhất trong `.local/otp-inbox/` dưới working directory để lấy mã. Đây là OTP mô phỏng local; không có SMS thật. Không cần chạy lại migration sau mỗi lần khởi động.

Lượt kiểm chứng này xác nhận HTTP/session/JSP và database thực; không khẳng định đã thao tác trực tiếp nút Run trong IDE hoặc gửi SMS qua nhà cung cấp bên ngoài.

## Bổ sung: phản hồi số điện thoại không hợp lệ

Ảnh người dùng gửi tiếp theo nhập `0123456789`, không đúng quy tắc số di động Việt Nam mà service chấp nhận. Trước sửa, lỗi chuẩn hóa này cũng bị gom vào thông báo gửi OTP chung, khiến người dùng không xác định được ô cần sửa.

Đã thêm kiểm tra cú pháp bằng cùng `VietnamPhoneNormalizer` tại biên form đăng ký, đưa lỗi vào trường `phone`, giữ việc xóa mật khẩu sau lỗi và thêm gợi ý đầu số ngay dưới ô nhập. Kiểm tra định danh trùng và các lỗi gửi mã khác vẫn đi qua xử lý dịch vụ như trước.

Kiểm chứng sau thay đổi: 39 test của controller/service/normalizer/JSP đạt, không lỗi hoặc skip. Trên ứng dụng đang chạy tại cổng 8080, POST `0123456789` trả lỗi ngay tại ô điện thoại, có hướng dẫn đầu số, không có banner gửi OTP chung và không lộ mật khẩu. Một lượt thông tin thử nghiệm mới với số hợp lệ đã hoàn tất đăng ký, ghi OTP local, xác minh, đăng nhập mật khẩu và truy cập `/user/shipments` HTTP 200. Ứng dụng của người dùng được giữ nguyên trạng thái chạy.
