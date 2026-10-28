# ĐỀ CƯƠNG CHI TIẾT BÁO CÁO ĐỒ ÁN MÔN HỌC
## Đề tài: Xây dựng hệ thống quản lý chuỗi giao nhận Logistics UTEExpress
**Trường:** Đại học Sư phạm Kỹ thuật Thành phố Hồ Chí Minh (HCMUTE)  
**Khoa:** Công nghệ Thông tin  
**Giảng viên hướng dẫn:** ThS. Nguyễn Hữu Trung  
**Nhóm sinh viên thực hiện:**
1. Ngô Bá Vạn (Nhóm trưởng) - MSSV: 24162144
2. Hoàng Phúc Khang (Thành viên) - MSSV: 24162056
3. Huỳnh Tấn Anh Phát (Thành viên) - MSSV: 24162091  

---

## MỤC LỤC TỔNG QUAN

* **LỜI CẢM ƠN**
* **NHẬN XÉT CỦA GIẢNG VIÊN HƯỚNG DẪN**
* **DANH MỤC TỪ VIẾT TẮT** (API, COD, CSRF, DTO, ERD, JPA, JWT, OIDC, OTP, RBAC, RDS, SOC, STOMP, UAT, WCAG...)
* **DANH MỤC BẢNG BIỂU**
* **DANH MỤC HÌNH VẼ**

---

### CHƯƠNG 1: TỔNG QUAN ĐỀ TÀI VÀ BÀI TOÁN NGHIỆP VỤ
* **1.1. Đặt vấn đề và tính cấp thiết của đề tài:**
  * Sự bùng nổ của thương mại điện tử và nhu cầu quản lý chuỗi cung ứng giao nhận bưu chính đa trạm tại Việt Nam.
  * Các thách thức thực tế: kiểm soát hàng hóa qua nhiều trạm trung chuyển, hạn chế thất lạc, quản lý dòng tiền thu hộ COD và bảo mật thông tin bưu gửi.
* **1.2. Mục tiêu và phạm vi đề tài:**
  * Mục tiêu: Xây dựng hệ thống quản lý logistics thuần vận chuyển hoàn chỉnh, vận hành xuyên suốt vòng đời kiện hàng.
  * Phạm vi: Áp dụng mô hình Hub đa cấp (Bưu cục phát/nhận cấp 1, Hub vùng cấp 2, Trung tâm khai thác SOC cấp 3).
* **1.3. Khảo sát quy trình nghiệp vụ chuỗi giao nhận UTEExpress:**
  * Quy trình tạo đơn và tiếp nhận bưu gửi (Khách gửi tại quầy hoặc Bưu tá lấy tận nơi).
  * Quy trình quét mã phân loại trên băng chuyền, đóng bao hàng container và niêm phong chì bảo mật.
  * Quy trình điều phối chuyến xe tải trung chuyển đường dài (Linehaul trips).
  * Quy trình giao hàng chặng cuối của bưu tá (Last-mile delivery), xử lý giao thành công, giao thất bại và chuyển hoàn.
  * Quy trình tài chính: thu hộ COD, đối soát tiền mặt tại bưu cục và quản lý ví điện tử khách hàng.
* **1.4. Phân công nhiệm vụ và tỷ lệ hoàn thành của các thành viên trong nhóm:**
  * Trình bày bảng phân công nhiệm vụ và tỷ lệ hoàn thành 100% của 3 thành viên theo đúng tài liệu dự án.

---

### CHƯƠNG 2: PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG
* **2.1. Phân tích yêu cầu chức năng theo 6 vai trò chuẩn (Canonical Roles):**
  * Khách hàng (`ROLE_USER`): Quản lý sổ địa chỉ, tính cước tự động, tạo đơn, tra cứu vận đơn, ví điện tử và rút tiền COD.
  * Nhân viên giao nhận (`ROLE_SHIPPER`): Tiếp nhận đơn lấy/phát, cập nhật trạng thái giao hàng, thu tiền COD.
  * Nhân viên bưu cục (`ROLE_HUB_STAFF`): Tiếp nhận hàng tại quầy và quét mã phân loại trên băng chuyền.
  * Tài xế trung chuyển (`ROLE_LINEHAUL_DRIVER`): Tiếp nhận chuyến xe tải và bàn giao bao hàng liên tỉnh.
  * Quản lý bưu cục (`ROLE_MANAGER`): Giám sát bảng điều hành tác nghiệp, phân tuyến bưu tá, xử lý hàng ngoại lệ và duyệt đối soát.
  * Quản trị viên hệ thống (`ROLE_ADMIN`): Cấu hình danh mục trạm, định tuyến và bảng giá cước dịch vụ.
* **2.2. Sơ đồ Use Case tổng quát và các Use Case phân hệ.**
* **2.3. Sơ đồ tuần tự (Sequence Diagram) và sơ đồ hoạt động (Activity Diagram) các luồng chính:**
  * Luồng tạo đơn và tính cước vận chuyển.
  * Luồng phân loại, đóng bao và xuất bến trung chuyển.
  * Luồng giao hàng chặng cuối, thu tiền COD và đối soát tài chính.
* **2.4. Thiết kế cơ sở dữ liệu quan hệ (ERD):**
  * Sơ đồ quan hệ thực thể (ERD) đạt chuẩn dạng chuẩn 3 (3NF).
  * Từ điển dữ liệu 25 bảng chính và các bảng mở rộng (users, hubs, shipments, containers, linehaul_trips, delivery_tasks, wallets...).
* **2.5. Thiết kế giao diện UI/UX trên Figma:**
  * Nghiên cứu trải nghiệm người dùng theo từng vai trò tác nghiệp.
  * Bộ nhận diện thương hiệu HCMUTE Logistics: Deep Cobalt Blue (`#0F3D78`) và Safety Orange (`#F26522`), chuẩn độ tương phản cao WCAG AA/AAA.

---

### CHƯƠNG 3: KIẾN TRÚC KỸ THUẬT VÀ GIẢI PHÁP CÔNG NGHỆ
* **3.1. Mô hình kiến trúc ba tầng (Three-Tier Architecture):**
  * Presentation Layer: Spring MVC kết hợp JSP/JSTL 3.0 (Jakarta EE), Bootstrap 5.3 và Bootstrap Icons.
  * Business Logic Layer: Spring Boot 3.3.4 trên nền Java 21 LTS, cấu trúc phân tách rõ giữa Controller, DTO, Service Interface, Service Implementation và Repository.
  * Data Access Layer: Spring Data JPA, Hibernate ORM và hệ quản trị Microsoft SQL Server 2019+.
* **3.2. Cơ chế phân quyền và bảo mật nâng cao:**
  * Spring Security 6 với cơ chế RBAC kết hợp Data Scoping (phạm vi bưu cục và chức vụ nhân sự).
  * Đăng nhập biểu mẫu truyền thống, xác thực OTP và tích hợp đăng nhập Google OAuth2 / OpenID Connect.
* **3.3. Các giải pháp kỹ thuật nâng cao:**
  * **Transaction Outbox Pattern:** Tách rời tiến trình gửi email OTP và thông báo đơn hàng qua Gmail SMTP (cổng 587 STARTTLS) bằng Scheduled Worker chạy ngầm, bảo đảm không gián đoạn giao dịch dữ liệu.
  * **Cơ chế Idempotency Key:** Khóa chống trùng lặp yêu cầu cho các thao tác quét mã vạch và giao dịch tiền tệ tốc độ cao.
  * **Thuật toán định tuyến trạm tự động:** Định tuyến bưu gửi tối ưu qua mạng lưới trạm Hub và SOC.
  * **Cập nhật thời gian thực:** Tích hợp WebSocket STOMP thông báo biến động đơn hàng tức thì.
* **3.4. Quản lý phiên bản cơ sở dữ liệu tự động với Flyway:**
  * Quản trị lịch sử schema đồng bộ từ `V1__baseline.sql` đến `V22__notification_email_outbox.sql`.

---

### CHƯƠNG 4: HIỆN THỰC HÓA VÀ KẾT QUẢ KIỂM THỬ
* **4.1. Hiện thực hóa các phân hệ chức năng (kèm hình ảnh minh họa giao diện và mã nguồn tiêu biểu):**
  * Phân hệ Khách hàng (Đăng ký, xác thực OTP, tạo đơn, tra cứu vận đơn, ví điện tử).
  * Phân hệ Bưu cục và Phân loại (Quét mã phân loại, đóng bao container, niêm phong chì).
  * Phân hệ Trung chuyển liên tỉnh (Điều phối xe tải, bàn giao hàng hóa giữa các trạm).
  * Phân hệ Bưu tá và Tài chính (Tác nghiệp giao hàng, thu COD, biên bản đối soát tiền mặt).
  * Phân hệ Quản lý và Quản trị (Bảng điều hành tác nghiệp bưu cục, cấu hình bảng giá và mạng lưới trạm).
* **4.2. Chiến lược và kết quả kiểm thử tự động:**
  * Bộ kiểm thử tự động hơn 2.000 ca kiểm thử (Unit Test, Security Permission Matrix, Realtime STOMP).
  * Kiểm thử tích hợp truy vấn trực tiếp trên cơ sở dữ liệu Microsoft SQL Server thật đạt tỷ lệ thành công 100%.
* **4.3. Kiểm thử chấp nhận người dùng thực tế (Manual UAT):**
  * Kịch bản kiểm thử thủ công trên máy thật theo 7 vai trò người dùng.
  * Dữ liệu mẫu kiểm thử và tài khoản demo nghiệm thu (`Demo@12345`).

---

### CHƯƠNG 5: TRIỂN KHAI HỆ THỐNG VÀ ĐÁNH GIÁ TỔNG KẾT
* **5.1. Triển khai hạ tầng đám mây:**
  * Thuê và cấu hình máy chủ AWS EC2 (Ubuntu Server 24.04 LTS) và Amazon RDS for SQL Server Express.
  * Cấu hình máy chủ web Nginx làm reverse proxy và cài đặt chứng chỉ bảo mật SSL Let's Encrypt / Certbot.
  * Vận hành hệ thống thực tế trên tên miền: `https://uteexpress.event5g.fun`.
* **5.2. Tự động hóa quy trình CI/CD qua GitHub Actions:**
  * Pipeline `.github/workflows/ci-cd.yml` tự động chạy kiểm thử trên runner container SQL Server, đóng gói WAR và triển khai an toàn qua SSH.
* **5.3. Đánh giá ưu điểm và hạn chế của hệ thống:**
  * Ưu điểm: Kiến trúc chuẩn mực, hiệu năng cao, độ tin cậy dữ liệu lớn, giao diện tối ưu cho tác nghiệp kho bãi.
  * Hạn chế: Chưa tích hợp bản đồ số theo dõi GPS xe tải thời gian thực, chưa tích hợp cổng thanh toán trực tuyến bên thứ ba (VNPay/Momo thật).
* **5.4. Hướng phát triển trong tương lai.**
* **5.5. Kết luận.**

---

* **TÀI LIỆU THAM KHẢO**
* **PHỤ LỤC**
