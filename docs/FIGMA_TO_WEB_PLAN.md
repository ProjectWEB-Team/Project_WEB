# Kế hoạch chuyển Figma sang web (từng layer)

Figma: file `8rQ2SfsjqeSFDcGb7I3brh`, section `249:1587` (122 màn hình 1920px). Mỗi layer = 1 session. Trong mỗi session: lấy code từng màn hình bằng `get_design_context` (node ID bên dưới, đổi `-` thành `:`), sửa JSP/CSS cho khớp, chụp ảnh so sánh rồi mới sang màn tiếp theo. Đừng lấy cả section `249:1587` một lần (1,8 triệu ký tự).

## Layer 0 · Token & nền (đã khớp)
`assets/css/ute-modern.css` đã có `--ink #12233F`, `--accent #F26522`, `--accent-tint #FDE9DF`, `--line #DDE2E9`, `--muted #556275`. Chỉ cần đối chiếu lại khi gặp màu/khoảng cách lạ.

## Layer 1 · Khung chung
Figma nav `255:1904` (cao 90px, nội dung bắt đầu x=280), footer và sidebar. File: `layout/navbar.jsp`, `layout/footer.jsp`, `layout/logo.jsp`, `assets/js/sidebar-nav.js`. Làm xong layer này trước vì mọi trang dùng chung.

## Layer 2 · Công khai
File: index.jsp, pricing/public.jsp, tracking/search.jsp

- [x] `88:2` 01. Công khai · Trang chủ & Tra cứu nhanh
- [x] `101:18` 02. Công khai · Bảng giá cước & Ước tính nhanh
- [x] `105:21` 03. Công khai · Tra cứu vận đơn & Hành trình
- [x] `387:7984` 03.1 Công khai · Tra cứu vận đơn · Không tìm thấy vận đơn
- [x] `387:8121` 03.2 Công khai · Tra cứu vận đơn · Trạng thái ban đầu (chưa tra cứu)
- [x] `387:8590` 02.1 Công khai · Bảng giá · Báo giá thành công
- [x] `387:9088` 02.2 Công khai · Bảng giá · Dữ liệu không hợp lệ

## Layer 3 · Xác thực
File: auth/*.jsp

- [x] `106:23` 04. Xác thực · Đăng nhập tài khoản
- [x] `106:194` 05. Xác thực · Đăng ký · Nhập thông tin
- [ ] `381:870` 04.1 Xác thực · Đăng nhập · Sai thông tin
- [ ] `381:924` 04.2 Xác thực · Đăng nhập · Đổi mật khẩu thành công
- [x] `381:977` 04.3 Xác thực · Đăng nhập bằng OTP · Nhập số điện thoại
- [x] `381:1025` 04.4 Xác thực · Đăng nhập bằng OTP · Nhập mã OTP
- [ ] `381:1077` 04.5 Xác thực · Hoàn tất tài khoản Google
- [ ] `381:1133` 05.1 Xác thực · Đăng ký · Nhập mã OTP
- [ ] `381:1185` 05.2 Xác thực · Đăng ký · OTP sai mã
- [ ] `381:1242` 05.3 Xác thực · Đăng ký · OTP hết hạn
- [ ] `381:1293` 05.4 Xác thực · Đăng ký bằng email · Nhập thông tin
- [x] `381:1356` 06.1 Xác thực · Quên mật khẩu · Bước 1: Nhập số điện thoại
- [ ] `381:1403` 06.2 Xác thực · Quên mật khẩu · Bước 2: Xác minh mã OTP
- [ ] `381:1461` 06.4 Xác thực · Khôi phục bằng email · Bước 1: Nhập email
- [ ] `381:1508` 06.5 Xác thực · Khôi phục bằng email · Bước 2: Xác minh OTP email
- [ ] `393:2166` 06.2.1 Xác thực · Quên mật khẩu · Bước 2: OTP sai mã
- [ ] `393:2226` 06.2.2 Xác thực · Quên mật khẩu · Bước 2: OTP hết hạn
- [x] `393:2278` 06.3 Xác thực · Quên mật khẩu · Bước 3: Đặt mật khẩu mới
- [ ] `393:2347` 06.5.1 Xác thực · Khôi phục bằng email · Bước 2: OTP email sai mã
- [ ] `393:2407` 06.6 Xác thực · Khôi phục bằng email · Bước 3: Đặt mật khẩu mới

## Layer 4 · Khách hàng
File: shipment/create.jsp, shipment/list.jsp, shipment/detail.jsp, shipment/incoming*.jsp, user/shipment-fees.jsp, user/wallet.jsp

- [x] `108:28` 07. Khách hàng · Tạo đơn · Bước 1: Người gửi & Người nhận
- [x] `341:305` 08. Khách hàng · Tạo đơn · Bước 2: Kiện hàng & Dịch vụ
- [x] `341:484` 09. Khách hàng · Tạo đơn · Bước 3: Cước phí & Thanh toán
- [ ] `341:654` 10. Khách hàng · Tạo đơn · Bước 4: Cách gửi hàng & Xác nhận
- [x] `110:34` 11. Khách hàng · Đơn đã gửi (Danh sách vận đơn)
- [x] `342:151` 12. Khách hàng · Chi tiết vận đơn & Lịch sử hành trình
- [x] `342:365` 13.1 Khách hàng · Đơn đang đến · Danh sách
- [x] `342:561` 14. Khách hàng · Phí hoàn & phụ thu · Chưa thanh toán
- [x] `111:37` 15. Khách hàng · Ví điện tử, Nạp tiền & Rút tiền
- [ ] `382:942` 13.2 Khách hàng · Đơn đang đến · Chi tiết chưa mở khóa
- [ ] `382:1084` 13.3 Khách hàng · Đơn đang đến · Nhập sai mã vận đơn
- [ ] `382:1227` 13.4 Khách hàng · Đơn đang đến · Chi tiết đã mở khóa
- [ ] `382:1384` 14.1 Khách hàng · Phí hoàn & phụ thu · Thanh toán thành công
- [ ] `382:1472` 14.2 Khách hàng · Phí hoàn & phụ thu · Thanh toán thất bại
- [ ] `387:1389` 07.1 Khách hàng · Tạo đơn · Bước 1 · Lỗi trường người gửi / người nhận
- [ ] `387:1530` 08.1 Khách hàng · Tạo đơn · Bước 2 · Khối lượng / COD không hợp lệ
- [ ] `387:1647` 09.1 Khách hàng · Tạo đơn · Bước 3 · Báo giá thay đổi, cần tính lại
- [ ] `387:1768` 10.1 Khách hàng · Tạo đơn · Bước 4 · Chưa chọn cách gửi hàng
- [ ] `387:1881` 12.1 Khách hàng · Chi tiết vận đơn · Đã hủy đơn
- [ ] `387:2036` 12.2 Khách hàng · Chi tiết vận đơn · Lỗi khi hủy đơn
- [ ] `387:2191` 15.1 Khách hàng · Ví điện tử · Gửi yêu cầu rút tiền thành công
- [ ] `387:2294` 15.2 Khách hàng · Ví điện tử · Yêu cầu rút tiền bị lỗi
- [ ] `404:3182` 15.3 Khách hàng · Ví điện tử · Yêu cầu rút tiền hợp lệ
- [ ] `387:8252` 11.1 Khách hàng · Đơn đã gửi · Danh sách rỗng
- [ ] `387:8422` 11.2 Khách hàng · Đơn đã gửi · Không có kết quả lọc

## Layer 5 · Bưu cục (Hub)
File: hub/*.jsp

- [x] `343:2` 16. Bưu cục · Đơn chờ phân công Shipper lấy hàng
- [x] `343:214` 17. Bưu cục · Tiếp nhận vào Hub, Cân đo & Phân loại tuyến
- [ ] `116:45` 18. Bưu cục · Đóng bao & Lập phiếu bàn giao chuyến
- [ ] `112:40` 19. Bưu cục · Nhận bàn giao bao hàng & Đối chiếu tại Hub
- [ ] `343:414` 20. Bưu cục · Phân công Shipper giao hàng & Giao hoàn
- [ ] `343:638` 21. Bưu cục · Người gửi nhận lại hàng hoàn & Thu phí tại Hub
- [ ] `343:812` 22. Bưu cục · Lịch sử bàn giao & Truy vết kiện hàng
- [ ] `387:2397` 16.1 Bưu cục · Phân công lấy hàng · Chưa chọn shipper
- [ ] `387:2529` 16.2 Bưu cục · Phân công lấy hàng · Phân công thành công
- [ ] `387:2661` 16.3 Bưu cục · Phân công lấy hàng · Phân công thất bại
- [ ] `387:2793` 17.1 Bưu cục · Tiếp nhận & phân loại · Quét sai / không tìm thấy đơn
- [ ] `387:2915` 17.2 Bưu cục · Tiếp nhận & phân loại · Cân đo không hợp lệ
- [ ] `387:3037` 17.3 Bưu cục · Tiếp nhận & phân loại · Phân tuyến thành công
- [ ] `387:3159` 17.4 Bưu cục · Tiếp nhận & phân loại · Chưa có quy tắc tuyến
- [ ] `387:3281` 18.1 Bưu cục · Đóng bao & bàn giao · Thao tác thành công
- [ ] `387:3435` 18.2 Bưu cục · Đóng bao & bàn giao · Kiện / bao / phiếu không hợp lệ
- [ ] `387:3589` 19.1 Bưu cục · Nhận bàn giao tại Hub · Nhận đủ
- [ ] `387:3750` 19.2 Bưu cục · Nhận bàn giao tại Hub · Thiếu một phần
- [ ] `387:3911` 19.3 Bưu cục · Nhận bàn giao tại Hub · Thiếu cả bao
- [ ] `387:4072` 20.1 Bưu cục · Phân công giao / hoàn · Phân công thành công
- [ ] `387:4216` 20.2 Bưu cục · Phân công giao / hoàn · Chưa có shipper phù hợp
- [ ] `387:4450` 21.1 Bưu cục · Nhận lại hàng hoàn · Chưa thanh toán phí
- [ ] `387:4544` 21.2 Bưu cục · Nhận lại hàng hoàn · Chưa đủ điều kiện bàn giao
- [ ] `387:4638` 21.3 Bưu cục · Nhận lại hàng hoàn · Trả kiện thành công

## Layer 6 · Shipper & Tài xế
File: shipper/*.jsp, driver/trips.jsp

- [x] `344:2` 23. Shipper · Nhiệm vụ lấy hàng tại nhà người gửi
- [ ] `117:132` 24. Shipper · Giao hàng & Báo kết quả giao
- [ ] `344:212` 25. Shipper · Nhiệm vụ giao trả hàng hoàn cho người gửi
- [ ] `118:92` 26. Tài xế trung chuyển · Chuyến xe · Bước 1: Chọn phiếu bàn giao
- [ ] `383:1255` 24.1 Shipper · Giao hàng · Không giao được (nhập lý do, ngày hẹn)
- [ ] `383:1469` 24.2 Shipper · Giao hàng · Đã ghi nhận không giao được
- [ ] `384:1060` 26.1 Tài xế trung chuyển · Chuyến xe · Bước 2: Quét từng bao (chưa đủ)
- [ ] `384:1344` 26.2 Tài xế trung chuyển · Chuyến xe · Bước 3: Đủ bao, nhận bàn giao & chọn xe
- [ ] `384:1622` 26.3 Tài xế trung chuyển · Chuyến xe · Bước 4: Báo tới nơi
- [ ] `384:1870` 26.4 Tài xế trung chuyển · Chuyến xe · Đã tới nơi, chờ Hub xác nhận
- [ ] `387:4732` 23.1 Shipper · Lấy hàng · Lỗi mã quét
- [ ] `387:4847` 25.1 Shipper · Giao hoàn · Phí hoàn chưa thanh toán
- [ ] `387:4971` 25.2 Shipper · Giao hoàn · Xác nhận đã trả kiện

## Layer 7 · Quản lý
File: manager/*.jsp

- [x] `118:367` 27. Quản lý · Bảng điều hành trung tâm (KPI & Việc cần xử lý)
- [ ] `345:2` 28. Quản lý · Chi tiết việc cần xử lý · Thiếu kiện (MISSING)
- [ ] `345:117` 29. Quản lý · Điều tra kiện thất lạc · Danh sách hồ sơ
- [ ] `345:290` 30. Quản lý · Phê duyệt xử lý hàng hoàn quá hạn lưu kho
- [ ] `118:646` 31. Quản lý · Đối soát COD, Trả tiền, Hoàn phí & Bồi thường
- [ ] `383:995` 28.1 Quản lý · Chi tiết việc cần xử lý · Quy tắc tuyến (ROUTE)
- [ ] `383:1120` 28.2 Quản lý · Chi tiết việc cần xử lý · Bàn giao (HANDOVER)
- [ ] `385:1054` 31.1 Quản lý · Chi tiết phiếu đối soát
- [ ] `385:1272` 29.1 Quản lý · Điều tra kiện thất lạc · Chi tiết hồ sơ & kết luận
- [ ] `385:1403` 29.2 Quản lý · Điều tra kiện thất lạc · Kết luận xác nhận mất
- [ ] `385:1526` 29.3 Quản lý · Điều tra kiện thất lạc · Kết luận đã tìm thấy
- [ ] `387:5095` 30.1 Quản lý · Hoàn quá hạn · Thiếu lý do xử lý
- [ ] `387:5181` 30.2 Quản lý · Hoàn quá hạn · Xử lý thành công
- [ ] `387:5267` 31.2 Quản lý · Đối soát · Kết quả chi COD / hoàn phí / bồi thường thành công
- [ ] `387:5495` 31.3 Quản lý · Đối soát · Lỗi đối soát

## Layer 8 · Quản trị
File: admin/*.jsp

- [x] `120:65` 32. Quản trị · Quản lý Người dùng & Nhân sự
- [ ] `219:1389` 33. Quản trị · Người dùng (Chế độ Menu thu gọn)
- [ ] `121:94` 34. Quản trị · Mạng lưới Bưu cục & Trung tâm khai thác
- [x] `345:540` 35. Quản trị · Cấu hình Bảng giá cước & Phụ phí theo tuyến
- [ ] `345:1135` 36. Quản trị · Duyệt lệnh rút tiền ví điện tử khách hàng
- [ ] `387:5723` 32.1 Quản trị · Người dùng · Tạo nhân viên thành công
- [ ] `387:6103` 32.2 Quản trị · Người dùng · Trùng thông tin
- [ ] `387:6483` 34.1 Quản trị · Mạng lưới bưu cục · Không thể ngừng hub còn phụ thuộc
- [ ] `387:6722` 35.1 Quản trị · Giá cước · Lưu giá thành công
- [ ] `387:7025` 35.2 Quản trị · Giá cước · Giá trị không hợp lệ
- [ ] `387:7328` 36.1 Quản trị · Duyệt rút tiền · Từ chối cần lý do
- [ ] `387:7482` 36.2 Quản trị · Duyệt rút tiền · Duyệt thành công
- [ ] `387:7636` 36.3 Quản trị · Duyệt rút tiền · Yêu cầu đã được xử lý

## Layer 9 · Trang lỗi
File: error/*.jsp

- [x] `385:1667` 37.1 Hệ thống · Lỗi 400 · Yêu cầu không hợp lệ
- [x] `385:1757` 37.2 Hệ thống · Lỗi 403 · Không có quyền truy cập
- [x] `385:1847` 37.3 Hệ thống · Lỗi 404 · Không tìm thấy trang
- [x] `385:1937` 37.4 Hệ thống · Lỗi 409 · Dữ liệu đã thay đổi
- [x] `385:2027` 37.5 Hệ thống · Lỗi 500 · Lỗi hệ thống
