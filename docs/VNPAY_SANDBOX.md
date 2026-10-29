# Thanh toán VNPay (sandbox)

Khách tạo đơn chọn "Trực tuyến (VNPay)" rồi bấm **Thanh toán qua VNPay** ở trang chi tiết đơn. Hệ thống chuyển khách sang trang thanh toán thử của VNPay; trả tiền xong, đơn **tự động chuyển sang "chờ bàn giao"**, không cần ai duyệt. Đây là môi trường thử, tiền ảo.

Chưa cấu hình khóa thì nút VNPay không hiện, chỉ còn nút "Xác nhận thanh toán (demo)" như trước.

## 1. Lấy khóa sandbox

1. Đăng ký merchant thử miễn phí: `https://sandbox.vnpayment.vn/devreg`.
2. VNPay gửi email gồm **Terminal ID (TmnCode)** và **Secret Key (HashSecret)**.
3. Vào trang quản trị merchant sandbox, điền địa chỉ **IPN URL**: `https://uteexpress.event5g.fun/payment/vnpay/ipn`.

## 2. Cấu hình trên EC2

```bash
sudo nano /etc/uteexpress.env
```

Thêm (không đặt dấu nháy, không để dấu cách quanh dấu `=`):

```
UTEEXPRESS_VNPAY_TMN_CODE=<TmnCode>
UTEEXPRESS_VNPAY_HASH_SECRET=<HashSecret>
UTEEXPRESS_VNPAY_RETURN_URL=https://uteexpress.event5g.fun/payment/vnpay/return
```

Rồi `sudo systemctl restart uteexpress`. Địa chỉ cổng thanh toán mặc định đã là sandbox (`UTEEXPRESS_VNPAY_PAY_URL` chỉ cần đặt khi VNPay đổi địa chỉ).

## 3. Thẻ thử

Chọn ngân hàng **NCB**, nhập: số thẻ `9704198526191432198`, tên `NGUYEN VAN A`, ngày phát hành `07/15`, mã OTP `123456`.

## 4. Luồng hoạt động

1. `POST /user/shipments/{id}/vnpay`: kiểm tra chủ đơn và số tiền, tạo đường dẫn có chữ ký (HMAC-SHA512) rồi chuyển sang VNPay.
2. VNPay chuyển trình duyệt về `/payment/vnpay/return` và gọi `/payment/vnpay/ipn` (server báo server). Cả hai chỉ tin **chữ ký**, không tin phiên đăng nhập.
3. Hệ thống kiểm tra chữ ký, mã kết quả `00`, số tiền khớp cước, rồi đánh dấu đã thanh toán, ghi mã giao dịch `VNPAY-<mã VNPay>` và sự kiện "Thanh toán VNPay thành công". Gửi lại nhiều lần vẫn chỉ ghi một lần.

Mã trả lời IPN theo tài liệu VNPay: `00` thành công, `01` không thấy đơn, `02` đơn đã xử lý hoặc không còn thanh toán được, `04` sai số tiền, `97` sai chữ ký.

## Lưu ý khi demo

- Nếu bật khóa site bằng Nginx basic auth (mục 8b trong `DEPLOY_AWS.md`), phải **miễn trừ** hai đường `/payment/vnpay/return` và `/payment/vnpay/ipn`, nếu không VNPay không gọi vào được.
- Đơn chưa trả quá thời hạn thanh toán sẽ tự hủy. Nếu VNPay báo thành công sau khi đơn đã hủy, hệ thống **không** đánh dấu đã trả (IPN trả `02`), tiền thử không bị ghi nhận; ở môi trường thật cần có quy trình hoàn tiền.
- Sandbox đôi khi chậm hoặc lỗi. Khi đó dùng nút "Xác nhận thanh toán (demo)" làm đường dự phòng.
- Khóa nằm trong `/etc/uteexpress.env`, không đưa vào repo.
