# BẢNG PHÂN CÔNG CÔNG VIỆC NHÓM PHÁT TRIỂN DỰ ÁN UTEEXPRESS

## 1. Mô hình Phân công Công việc (Working Model)

Dự án **UTEExpress - Hệ thống Quản lý Chuỗi Giao nhận Logistics** được phát triển bởi nhóm 3 thành viên:
- **Hoàng Phúc Khang** (Thành viên A)
- **Ngô Bá Vạn** (Thành viên B - Trưởng nhóm)
- **Huỳnh Tấn Anh Phát** (Thành viên C)

### Nguyên tắc Lát cắt dọc (Vertical Slice Ownership)
Dự án áp dụng mô hình phân chia trách nhiệm theo **Lát cắt dọc hoàn chỉnh (End-to-End Vertical Slice)**. Tuyệt đối không phân chia nằm ngang chỉ phụ trách riêng giao diện (frontend), logic dịch vụ (backend), hay cơ sở dữ liệu (database).

Mỗi thành viên khi đảm nhận phân hệ chức năng của mình đều làm chủ và triển khai trọn vẹn chuỗi thành phần kỹ thuật:

```text
JSP/UI → Controller → DTO → Service → Repository/JPA → SQL Server → authorization → tests
```

Cụ thể, mỗi lát cắt dọc của từng thành viên bao gồm đầy đủ:
1. **JSP / UI**: Giao diện người dùng sử dụng Bootstrap 5, JSTL chuẩn Jakarta 3.0, kế thừa layout chung (header, sidebar, footer), đảm bảo tính công thái học và hiển thị thông báo lỗi/thành công.
2. **Controller**: Tiếp nhận request HTTP, điều phối luồng xử lý, áp dụng mô hình Post/Redirect/Get (PRG) cho các thao tác thay đổi dữ liệu, xử lý Flash Attributes và trả về View/Model tương ứng.
3. **DTO (Data Transfer Object)**: Đóng gói dữ liệu đầu vào (Request DTO có Jakarta Validation) và dữ liệu hiển thị đầu ra (Response DTO), ngăn chặn rò rỉ cấu trúc thực thể ra tầng giao diện.
4. **Service Interface & ServiceImpl**: Xử lý toàn bộ logic nghiệp vụ, tính toán chi phí, chuyển đổi trạng thái đơn hàng, quản lý giao dịch với `@Transactional`, đảm bảo tính toàn vẹn dữ liệu.
5. **Repository / Spring Data JPA**: Truy vấn dữ liệu thông qua Spring Data JPA, cấu hình FetchType.LAZY hợp lý, sử dụng `@EntityGraph` hoặc `JOIN FETCH` để xử lý triệt để bài toán N+1 query.
6. **SQL Server**: Tương tác với cơ sở dữ liệu vật lý SQL Server, đảm bảo ràng buộc khóa chính, khóa ngoại, chỉ mục (Index) và toàn vẹn tham chiếu.
7. **Authorization**: Kiểm soát phân quyền chặt chẽ ở tầng backend (kết hợp Role, Hub Scope, Task Assignment và quyền sở hữu tài nguyên), không phụ thuộc vào ẩn/hiện trên giao diện.
8. **Tests**: Xây dựng đầy đủ bài kiểm thử đơn vị (Unit Test) cho Service/DTO và kiểm thử tích hợp (Integration/Slice Test) cho Controller/Security.

---

## 2. Chi tiết Phân công Thành viên

| Thành viên | Ký hiệu | Trách nhiệm dự án | Phân hệ Lát cắt dọc Phụ trách |
|---|---|---|---|
| **Hoàng Phúc Khang** | Thành viên A | Tiếp nhận đơn hàng, tài khoản người dùng và trải nghiệm giao diện người gửi/người nhận | **Phân hệ Tiếp nhận Đơn hàng & Người dùng (User & Shipment Intake)** |
| **Ngô Bá Vạn** | Thành viên B *(Trưởng nhóm)* | Điều phối kiến trúc kỹ thuật, chuẩn hóa hợp đồng giao tiếp giữa các phân hệ, quản lý luồng trung chuyển bưu cục | **Phân hệ Vận hành Bưu cục & Tuyến vận chuyển (Hub & Linehaul Operations)** |
| **Huỳnh Tấn Anh Phát** | Thành viên C | Quản lý chặng giao hàng cuối, quản lý dòng tiền thu hộ (COD) và toàn vẹn nghiệp vụ ví điện tử | **Phân hệ Giao hàng Chặng cuối & Tài chính (Last-mile & Finance)** |

---

## 3. Nội dung Chi tiết từng Phân hệ Chức năng

### 3.1. Hoàng Phúc Khang / Thành viên A: User & Shipment Intake
Phụ trách trọn vẹn luồng tiếp nhận đơn hàng từ phía khách hàng và người gửi:
- **Quản lý Tài khoản & Xác thực**: Đăng ký, đăng nhập tài khoản khách hàng (`ROLE_USER`), quản lý sổ địa chỉ người gửi và người nhận.
- **Tạo đơn hàng (Create Shipment)**: Form tạo đơn hàng với đầy đủ thông tin người gửi, người nhận, thông số kiện hàng (kích thước, khối lượng quy đổi), giá trị khai giá và tiền thu hộ (COD).
- **Tính cước phí tự động**: Tích hợp công thức tính cước vận chuyển, phụ phí hàng quá khổ/quá tải, cước bảo hiểm và phí thu hộ COD.
- **Phương thức gửi hàng**:
  - Lấy hàng tận nơi (`HOME_PICKUP`): Điều phối yêu cầu lấy hàng, xử lý gán Shipper lấy hàng, timeout và hủy yêu cầu.
  - Gửi hàng tại bưu cục (`SELF_DROPOFF`): Tạo mã vận đơn hẹn trước, tiếp nhận trực tiếp tại quầy giao dịch của Hub.
- **Khởi tạo Thanh toán**: Xử lý lựa chọn người thanh toán (người gửi trả ngay hoặc người nhận trả khi giao), khởi tạo trạng thái thanh toán ban đầu.
- **Tra cứu Hành trình (Tracking) & Bảo mật Thông tin**:
  - Giao diện tra cứu công khai theo mã vận đơn (`tracking_number`).
  - Giao diện quản lý danh sách đơn hàng đã gửi của khách hàng.
  - Cơ chế che mờ dữ liệu (Data Masking) số điện thoại và địa chỉ nhằm bảo vệ quyền riêng tư người nhận.
- **Trách nhiệm kỹ thuật**: Triển khai đầy đủ từ JSP/UI → Controller → DTO → Service → Repository/JPA → SQL Server → authorization → tests cho toàn bộ phân hệ.

### 3.2. Ngô Bá Vạn / Thành viên B: Hub & Linehaul Operations
Phụ trách trọn vẹn luồng xử lý hàng hóa tại mạng lưới bưu cục và trung tâm khai thác:
- **Mạng lưới Bưu cục & Quy tắc Điều hướng**: Cấu trúc phân cấp bưu cục (Hub cấp tỉnh/thành phố, Hub giao dịch quận/huyện), bảng quy tắc định tuyến bưu cục (`hub_routing_rules`).
- **Nghiệp vụ Kho bãi Bưu cục**:
  - Nhập kho (Inbound): Quét mã vạch tiếp nhận kiện hàng vào Hub, ghi nhận sự kiện lịch sử đơn hàng.
  - Phân loại (Sorting): Phân chia kiện hàng theo khu vực đích đến hoặc tuyến trung chuyển.
  - Xuất kho (Outbound): Xác nhận hàng rời khỏi Hub để luân chuyển hoặc giao hàng.
- **Quản lý Đóng bao & Bàn giao (Bag/Container & Handover)**:
  - Đóng gói nhiều kiện hàng vào bao hàng/thùng chứa (Container/Bag) có niêm phong seal.
  - Lập biên bản bàn giao (Handover Record) giữa các bên chịu trách nhiệm lưu trữ và vận chuyển.
- **Quản lý Chuyến xe Luân chuyển (Linehaul / Transport Trip)**:
  - Lập kế hoạch chuyến xe, gán tài xế đường dài (`ROLE_LINEHAUL_DRIVER`) và phương tiện vận tải.
  - Theo dõi hành trình chuyến xe di chuyển giữa các Hub và xác nhận đến Hub đích.
- **Xử lý Sự cố Vận hành (Operational Incidents)**: Ghi nhận sự cố hư hỏng, thất lạc, hàng hóa bất thường trong quá trình lưu kho hoặc luân chuyển.
- **Trách nhiệm kỹ thuật**: Triển khai đầy đủ từ JSP/UI → Controller → DTO → Service → Repository/JPA → SQL Server → authorization → tests cho toàn bộ phân hệ; điều phối kiến trúc tổng thể của nhóm.

### 3.3. Huỳnh Tấn Anh Phát / Thành viên C: Last-mile & Finance
Phụ trách trọn vẹn chặng giao hàng cuối cùng đến tay người nhận và nghiệp vụ đối soát tài chính:
- **Điều phối Giao hàng Chặng cuối (Last-mile Delivery)**:
  - Phân công danh sách kiện hàng cần giao cho Shipper (`ROLE_SHIPPER`) theo khu vực phụ trách.
  - Ghi nhận các lượt giao hàng (Delivery Attempts), lý do giao không thành công, số lần thử lại tối đa.
- **Xử lý Kết quả Giao hàng & Chuyển hoàn**:
  - Giao hàng thành công (`DELIVERED`): Thu tiền COD (nếu có), chữ ký/hình ảnh xác nhận.
  - Giao hàng thất bại (`FAILED`) và kích hoạt quy trình chuyển hoàn hàng về người gửi (`RETURN_TO_SENDER`, `RETURNED_TO_SENDER`).
- **Quản lý Tiền thu hộ COD & Đối soát Tài chính (Reconciliation)**:
  - Báo cáo thu nộp tiền mặt COD của từng Shipper sau ca làm việc.
  - Quy trình đối soát tiền COD, kiểm tra sai lệch (đúng số tiền, thiếu tiền `SHORTAGE`, thừa tiền `SURPLUS`), lập biên bản đối soát.
- **Quản lý Ví điện tử Khách hàng (Wallet & Transactions)**:
  - Quản lý tài khoản ví độc lập của khách hàng (`wallets`), tách biệt rõ ràng số dư khả dụng (`balance`) và số dư đóng băng chờ đối soát (`locked_balance`).
  - Lịch sử biến động số dư qua các giao dịch ví.
  - Quy trình tạo yêu cầu rút tiền (Withdrawal) về tài khoản ngân hàng, kiểm tra tính hợp lệ và cơ chế khóa chống chi tiêu kép (anti-double-spending).
- **Trách nhiệm kỹ thuật**: Triển khai đầy đủ từ JSP/UI → Controller → DTO → Service → Repository/JPA → SQL Server → authorization → tests cho toàn bộ phân hệ.

---

## 4. Quy trình Phối hợp và Đánh giá Chéo (Cross-Review Protocol)

Để bảo đảm tính tương thích giữa các phân hệ và duy trì chất lượng mã nguồn cao nhất, nhóm thực hiện cơ chế phản biện chéo bắt buộc trước khi tích hợp vào nhánh chính:

```text
Hoàng Phúc Khang (Thành viên A) ──[Đánh giá mã nguồn]──> Ngô Bá Vạn (Thành viên B)
Ngô Bá Vạn (Thành viên B)        ──[Đánh giá mã nguồn]──> Huỳnh Tấn Anh Phát (Thành viên C)
Huỳnh Tấn Anh Phát (Thành viên C) ──[Đánh giá mã nguồn]──> Hoàng Phúc Khang (Thành viên A)
```

### Tiêu chí Đánh giá Chéo:
1. **Tuân thủ Kiến trúc 3 tầng**: Tách bạch đúng Controller → Service → Repository; không có logic nghiệp vụ trong Controller/JSP; không có scriptlet trong JSP.
2. **Hợp đồng Dữ liệu & Toàn vẹn Mô hình**: Tuân thủ đúng danh mục 6 Roles chuẩn, 13 trạng thái đơn hàng bất biến, cấu trúc khóa ngoại và quan hệ thực thể trong cơ sở dữ liệu.
3. **Phân quyền & Bảo mật**: Kiểm tra phân quyền ở tầng Service/Controller theo đúng vai trò và phạm vi dữ liệu; dữ liệu nhạy cảm được che mờ thích hợp.
4. **Kiểm thử Đạt chuẩn**: Tất cả các unit test và integration test của phân hệ phải vượt qua thành công (100% Passed) trước khi gửi yêu cầu gộp nhánh.
