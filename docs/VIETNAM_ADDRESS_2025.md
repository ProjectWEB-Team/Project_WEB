# Địa chỉ Việt Nam và giao diện phí

Form tạo vận đơn sử dụng mô hình hai cấp từ 01/07/2025: **34 tỉnh/thành → 3.321 phường, xã, đặc khu**, áp dụng cho cả người gửi và người nhận. Danh sách có tìm tên phường/xã không dấu; đổi tỉnh sẽ xóa lựa chọn phường/xã cũ. Không yêu cầu quận/huyện cho đơn mới.

## Nguồn và cách cập nhật

- Nguồn dữ liệu công khai: https://provinces.open-api.vn/api/v2/?depth=2
- Snapshot được tải ngày 26/10/2026; URL, ngày và SHA-256 của file nguồn nằm trong `src/main/resources/geography/source.json`.
- Dữ liệu đã chuẩn hóa ở `src/main/resources/geography/vietnam-admin-2025.json`. Mã tỉnh giữ 2 chữ số, mã phường/xã giữ 5 chữ số, gồm cả số 0 ở đầu.
- Đây là snapshot của API cộng đồng, không phải kết nối trực tiếp với cơ sở dữ liệu địa giới của cơ quan nhà nước. Khi cập nhật, kiểm tra mã không trùng, quan hệ tỉnh/phường và nguồn công bố thay đổi hành chính trước khi thay snapshot.
- Ứng dụng phục vụ dữ liệu từ `/user/shipments/address-catalog`; khi tạo đơn không gọi Internet.

## Tính cước và lưu địa chỉ

Server kiểm tra phường/xã thuộc tỉnh đã chọn và suy ra vùng tính cước; vùng do client gửi không quyết định giá. Ba vùng cước là cấu hình của ứng dụng, tách khỏi mã địa giới: các tỉnh có mã dưới 38 thuộc Bắc; từ 38 đến 68 thuộc Trung; phần còn lại thuộc Nam. Đây là quy ước cho bảng giá mẫu và có thể điều chỉnh theo đơn vị vận chuyển.

Địa chỉ đầy đủ được ghép tại server: `số nhà, tên đường/thôn ấp, phường/xã, tỉnh/thành`. Chuỗi này được lưu vào các cột `sender_address`, `receiver_address` hiện có để các màn hình lấy hàng, giao hàng và hoàn hàng đọc được. Server kiểm tra tổng độ dài không vượt 255 ký tự.

Đơn mới lưu tên tỉnh chuẩn vào `receiver_province` và để `receiver_district` NULL. Không ghi tên phường vào cột quận/huyện. Mã tỉnh/phường phục vụ xác thực form, chưa có cột riêng trong bảng shipments; muốn báo cáo theo mã phường cần một migration riêng. Đơn cũ giữ nguyên địa chỉ và thông tin quận/huyện lịch sử. Các luồng nội bộ cũ của service vẫn được kiểm thử với dữ liệu cũ; form web mới bắt buộc đủ bốn mã địa chỉ.

Thay đổi địa chỉ sau khi báo giá yêu cầu báo giá lại. Với địa chỉ hai cấp, bưu cục giao đích được chọn từ một `DELIVERY_HUB`, cấp 1, ACTIVE, có tỉnh chuẩn khớp và `district IS NULL`: đây là cấu hình phục vụ toàn tỉnh. Không có hoặc có nhiều bưu cục phù hợp thì từ chối báo giá; không tự chọn hub đầu tiên.

## Mạng lưới mô phỏng phục vụ đồ án

`docs/sql/local-demo-nationwide-2025.sql` là script **chạy chủ động trên database local `ute_express`**, không phải migration tự chạy. Script thêm 34 bưu cục cấp tỉnh, 3 hub vùng, 1 SOC và 145 quy tắc định tuyến. Tên bưu cục có chữ “Mô phỏng”, mã bắt đầu `LOCAL2025-`. Các bưu cục cấp tỉnh có phạm vi mô phỏng toàn tỉnh để thử tất cả phường/xã.

Định tuyến mẫu: bưu cục → hub vùng → SOC nếu liên vùng → hub vùng đích → bưu cục đích. Script chạy lại không tạo trùng; cấu hình xung đột sẽ rollback. Script không tự tạo nhân viên, tài xế, phương tiện hay đơn hàng. Khi thử các bước kho và giao hàng, cần gán nhân sự/phương tiện demo đúng phạm vi. Mạng lưới này chứng minh luồng phần mềm, không đại diện cho cơ sở hay năng lực vận chuyển ngoài đời.

Bảng giá mẫu vẫn dùng script có sẵn `src/main/resources/db/demo/R__demo_pricing.sql` (27 tổ hợp dịch vụ/vùng). Không thay bảng giá thương mại bằng giá mẫu.

## Trang phí hoàn và phụ thu

`/user/shipment-fees/{id}` có bố cục responsive, chi tiết khoản phát sinh, tổng tiền, nút quay về đúng chi tiết vận đơn và danh sách đơn. Trường hợp 0đ được giải thích là không có khoản hoàn/phụ thu phải trả; không suy diễn rằng cước gốc đã thanh toán. VNPay trên trang này vẫn là mô phỏng, yêu cầu người dùng xác nhận riêng. Quyền sở hữu, số tiền do server tính và CSRF giữ theo service hiện có.
