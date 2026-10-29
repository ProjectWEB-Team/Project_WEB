# Reset EC2 và nạp dữ liệu demo đầy đủ

Mục tiêu: một database mới, sạch, có sẵn tài khoản mọi vai trò (mật khẩu `Demo@12345`) và dữ liệu đang hoạt động ở mọi chức năng. Cách làm **không xóa database cũ**: tạo database mới `ute_express_demo` rồi trỏ ứng dụng sang đó. Muốn quay lại thì trỏ về `ute_express`.

Dữ liệu demo nằm trong `src/main/resources/db/demo/` và chỉ nạp khi chạy profile `demo`:
`R__demo_a_network.sql` (38 hub), `R__demo_b_accounts.sql` (tài khoản), `R__demo_pricing.sql` (bảng giá), `R__demo_c_activity.sql` (đơn hàng, ví, rút tiền, chuyến xe). Flyway tự nạp khi ứng dụng khởi động trên database trống.

## 1. Tạo database mới (trên EC2)

Thay `<RDS_ENDPOINT>`, `<MAT_KHAU_ADMIN>`:

```bash
/opt/mssql-tools18/bin/sqlcmd -S <RDS_ENDPOINT> -U admin -P '<MAT_KHAU_ADMIN>' -C -Q "CREATE DATABASE ute_express_demo;"
/opt/mssql-tools18/bin/sqlcmd -S <RDS_ENDPOINT> -U admin -P '<MAT_KHAU_ADMIN>' -C -d ute_express_demo -Q "CREATE USER uteexpress_app FOR LOGIN uteexpress_app; ALTER ROLE db_owner ADD MEMBER uteexpress_app;"
```

## 2. Sửa cấu hình

```bash
sudo nano /etc/uteexpress.env
```

- `UTEEXPRESS_SQLSERVER_URL`: đổi `databaseName=ute_express` thành `databaseName=ute_express_demo`.
- `SPRING_PROFILES_ACTIVE=prod,demo` (thêm `,otp-local` nếu cần đăng ký thử bằng OTP điện thoại).
- **Không** đặt `UTEEXPRESS_DEMO_PASSWORD_HASH` (để trống thì mật khẩu mặc định là `Demo@12345`).
- Giữ đơn "chờ thanh toán" và "chờ lấy hàng" không bị tự hủy sau 2 phút (profile `demo` rút thời gian chờ xuống 2 phút, sẽ hủy các đơn mẫu đó ngay). Thêm các dòng:

```
UTEEXPRESS_TIMEOUT_PAYMENT_HOURS=72
UTEEXPRESS_TIMEOUT_PAYMENT_MINUTES=0
UTEEXPRESS_TIMEOUT_PICKUP_HOURS=72
UTEEXPRESS_TIMEOUT_PICKUP_MINUTES=0
UTEEXPRESS_TIMEOUT_DROPOFF_HOURS=72
UTEEXPRESS_TIMEOUT_DROPOFF_MINUTES=0
```

Muốn demo cảnh tự hủy sau 2 phút thì bỏ các dòng này, rồi tạo một đơn mới ngay lúc demo (đơn mẫu cũ sẽ bị hủy theo).

## 3. Khởi động lại

```bash
sudo systemctl restart uteexpress
sudo journalctl -u uteexpress -f
```

Chờ các dòng `Migrating schema [dbo] ...` rồi `Started UteExpressApplication`. Lần đầu mất khoảng 1 đến 2 phút. Mở `https://uteexpress.event5g.fun` kiểm tra.

## 4. Tài khoản (mật khẩu `Demo@12345`)

| Vai trò | Tên đăng nhập |
|---|---|
| Quản trị | `demo_admin` |
| Quản lý | `demo_manager` |
| Nhân viên bưu cục | `demo_hub_hcm`, `demo_hub_hn`, `demo_hub_soc`, `demo_hub_south`, `demo_hub_north` |
| Shipper | `demo_shipper_hcm`, `demo_shipper_hn` |
| Tài xế trung chuyển | `demo_driver_hcm`, `demo_driver_hn` |
| Khách hàng | `demo_sender`, `demo_customer`, `demo_receiver` |

## 5. Dữ liệu mẫu có gì

17 đơn `UTXDEMO0001` đến `UTXDEMO0017` ở mọi trạng thái: chờ thanh toán, chờ lấy hàng, đã lấy, tại bưu cục, đang trung chuyển, đang phát, giao thành công, giao thất bại, hoàn hàng (đang hoàn và đã hoàn), hủy có hoàn tiền, người nhận trả cước.
- Thanh toán bằng ví, VNPay, tiền mặt; phí hoàn hàng (đã trả và chưa trả).
- COD: chờ thu, đã thu chưa đối soát, đã đối soát và chuyển về ví; một đợt đối soát shipper hoàn tất.
- Ví của `demo_sender` (750.000 đ khả dụng, 450.000 đ đang khóa) và `demo_customer`; lịch sử giao dịch khớp số dư.
- Rút tiền: chờ duyệt (2), đã duyệt (1), từ chối kèm lý do (1), hoàn tất (1). Đăng nhập `demo_admin`, vào `/admin/withdrawals` để duyệt hoặc từ chối.
- Chuyến xe `DEMO-TRIP-001` đang chạy (HCM đến hub miền Nam, 2 đơn trong container) và `DEMO-TRIP-002` đã lên lịch (Hà Nội đến hub miền Bắc, chờ tài xế nhận bàn giao); một báo cáo trễ chuyến đang mở.

Thời gian của mọi dữ liệu tính theo lúc nạp, nên luôn trông "mới".

## 6. Sau buổi demo

Đưa ứng dụng về chế độ thật: `SPRING_PROFILES_ACTIVE=prod`, xóa các dòng `UTEEXPRESS_TIMEOUT_*`, đổi URL về `databaseName=ute_express`, rồi `sudo systemctl restart uteexpress`. Xóa database demo khi không cần nữa:

```bash
/opt/mssql-tools18/bin/sqlcmd -S <RDS_ENDPOINT> -U admin -P '<MAT_KHAU_ADMIN>' -C -Q "DROP DATABASE ute_express_demo;"
```

## Lưu ý an toàn

Mật khẩu `Demo@12345` và tài khoản `demo_admin` nằm công khai trong repo. Khi bật demo trên tên miền công khai, bất kỳ ai cũng đăng nhập được với quyền quản trị. Chỉ bật trong ngày demo, hoặc khóa bằng Nginx basic auth như mục 8b trong `DEPLOY_AWS.md`, rồi tắt ngay sau đó.
