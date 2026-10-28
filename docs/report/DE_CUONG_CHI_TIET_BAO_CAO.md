# ĐỀ CƯƠNG CHI TIẾT BÁO CÁO ĐỒ ÁN MÔN HỌC
## Đề tài: Xây dựng hệ thống quản lý chuỗi giao nhận Logistics UTEExpress
**Trường:** Đại học Công Nghệ Kỹ Thuật Thành phố Hồ Chí Minh (HCMUTE)  
**Khoa:** Công Nghệ Thông tin  
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

### CHƯƠNG 1: TỔNG QUAN ĐỀ TÀI
* **1.1. Giới thiệu đề tài:** Giới thiệu bài toán chuỗi giao nhận bưu chính đa trạm UTEExpress.
* **1.2. Lý do chọn đề tài:** Bối cảnh thương mại điện tử tại Việt Nam và yêu cầu kiểm soát dòng hàng hóa, công nợ COD liên tỉnh.
* **1.3. Mục tiêu của đề tài:** Xây dựng hệ thống logistics thuần vận chuyển, quản lý xuyên suốt vòng đời bưu gửi.
* **1.4. Phạm vi và đối tượng áp dụng:** Mạng lưới bưu cục đa cấp (Bưu cục phát/nhận cấp 1, Hub vùng cấp 2, Trung tâm khai thác SOC cấp 3).
* **1.5. Phân công nhiệm vụ các thành viên:** Bảng phân công nhiệm vụ và tỷ lệ hoàn thành 100% của 3 thành viên.

---

### CHƯƠNG 2: CƠ SỞ LÝ THUYẾT
* **2.1. Nền tảng Spring Framework và Spring Boot:**
  * 2.1.1. Tổng quan Spring Boot 3.3.4 và môi trường Java 21 LTS
  * 2.1.2. Cơ chế Dependency Injection (DI) và Inversion of Control (IoC)
  * 2.1.3. Mô hình MVC trong Spring (Model, View, Controller và lớp DTO)
* **2.2. Cơ chế bảo mật và phân quyền với Spring Security 6:**
  * 2.2.1. Kiểm soát truy cập dựa trên vai trò (RBAC) kết hợp phạm vi bưu cục (Data Scoping)
  * 2.2.2. Đăng nhập biểu mẫu và tích hợp Google OAuth2 / OpenID Connect
* **2.3. Tầng dữ liệu và quản trị migration:**
  * 2.3.1. Spring Data JPA và Hibernate ORM
  * 2.3.2. Hệ quản trị cơ sở dữ liệu Microsoft SQL Server 2019+
  * 2.3.3. Tự động hóa quản lý lược đồ dữ liệu với Flyway Migration
* **2.4. Công nghệ trình diễn giao diện và truyền thông:**
  * 2.4.1. JSP / JSTL 3.0 (Jakarta EE) kết hợp Bootstrap 5.3 và Bootstrap Icons
  * 2.4.2. Thiết kế nguyên mẫu giao diện UI/UX trên Figma
  * 2.4.3. Giao tiếp dữ liệu thời gian thực với WebSocket STOMP
  * 2.4.4. Dịch vụ thư điện tử Gmail SMTP
* **2.5. Hạ tầng điện toán đám mây và DevOps:**
  * 2.5.1. Máy chủ đám mây AWS EC2 và cơ sở dữ liệu Amazon RDS
  * 2.5.2. Máy chủ web Nginx làm reverse proxy và chứng chỉ bảo mật SSL
  * 2.5.3. Tự động hóa kiểm thử và triển khai qua GitHub Actions (CI/CD)

---

### CHƯƠNG 3: PHÂN TÍCH VÀ ĐẶC TẢ YÊU CẦU HỆ THỐNG
* **3.1. Phân tích tác nhân và 6 vai trò chuẩn trong hệ thống:**
  * Khách hàng (`ROLE_USER`), Bưu tá giao nhận (`ROLE_SHIPPER`), Nhân viên bưu cục (`ROLE_HUB_STAFF`), Tài xế trung chuyển (`ROLE_LINEHAUL_DRIVER`), Quản lý bưu cục (`ROLE_MANAGER`), Quản trị viên (`ROLE_ADMIN`).
* **3.2. Đặc tả yêu cầu chức năng theo từng phân hệ:**
  * 3.2.1. Phân hệ tiếp nhận và tạo đơn (Intake)
  * 3.2.2. Phân hệ tác nghiệp bưu cục và phân loại hàng (Hub Operations)
  * 3.2.3. Phân hệ trung chuyển xe tải liên tỉnh (Linehaul)
  * 3.2.4. Phân hệ giao hàng chặng cuối và tài chính COD (Last-mile và Finance)
  * 3.2.5. Phân hệ giám sát, điều hành và quản trị hệ thống
* **3.3. Đặc tả yêu cầu phi chức năng:**
  * An toàn bảo mật thông tin bưu gửi, tính toàn vẹn tài chính ví điện tử, hiệu năng xử lý quét mã vạch tốc độ cao và chuẩn hiển thị công thái học kho bãi (WCAG).
* **3.4. Mô hình hóa chức năng bằng lược đồ Use Case:**
  * 3.4.1. Lược đồ Use Case tổng quát
  * 3.4.2. Lược đồ Use Case chi tiết các phân hệ
* **3.5. Mô hình hóa quy trình nghiệp vụ:**
  * 3.5.1. Máy trạng thái bưu gửi (Shipment State Machine với 13 trạng thái chuẩn)
  * 3.5.2. Sơ đồ hoạt động (Activity Diagram) các luồng tác nghiệp chính

---

### CHƯƠNG 4: THIẾT KẾ HỆ THỐNG
* **4.1. Thiết kế kiến trúc tổng thể:**
  * 4.1.1. Kiến trúc phân tầng Controller - Service - Repository
  * 4.1.2. Mẫu kiến trúc Transaction Outbox Pattern gửi email OTP chạy ngầm
  * 4.1.3. Cơ chế khóa chống trùng lặp yêu cầu (Idempotency Key)
  * 4.1.4. Thuật toán định tuyến bưu cục tự động
* **4.2. Thiết kế cơ sở dữ liệu quan hệ (ERD):**
  * 4.2.1. Sơ đồ quan hệ thực thể (ERD) chuẩn hóa dạng chuẩn 3 (3NF)
  * 4.2.2. Bốn phân hệ dữ liệu: Cốt lõi, Đơn hàng và Bảng giá, Vận hành trung chuyển, Giao hàng và Tài chính
* **4.3. Từ điển dữ liệu và mô tả các bảng:**
  * Đặc tả chi tiết các trường, kiểu dữ liệu, khóa chính, khóa ngoại của 25 bảng nghiệp vụ chính.
* **4.4. Thiết kế các ràng buộc toàn vẹn và giao dịch an toàn:**
  * Ràng buộc trạng thái đơn hàng, bất biến tài chính ví điện tử, xử lý đồng thời và kiểm soát xung đột dữ liệu (Optimistic Locking).
* **4.5. Thiết kế giao diện nguyên mẫu UI/UX trên Figma:**
  * Bộ nhận diện thương hiệu HCMUTE Logistics: Deep Cobalt Blue (`#0F3D78`) và Safety Orange (`#F26522`).

---

### CHƯƠNG 5: XÂY DỰNG, TRIỂN KHAI VÀ ĐÁNH GIÁ HỆ THỐNG
* **5.1. Xây dựng các phân hệ chức năng và giao diện người dùng:**
  * 5.1.1. Phân hệ Khách hàng (Tạo đơn, tính cước tự động, theo dõi hành trình bảo mật bằng mã PIN, ví điện tử)
  * 5.1.2. Phân hệ Bưu cục (Quét mã phân loại trên băng chuyền, gom bao container, niêm phong chì)
  * 5.1.3. Phân hệ Trung chuyển liên tỉnh (Điều phối chuyến xe tải, bốc dỡ và biên bản bàn giao)
  * 5.1.4. Phân hệ Giao nhận bưu tá (Tuyến phát hàng di động, thu tiền COD, ghi nhận giao thất bại, chuyển hoàn)
  * 5.1.5. Phân hệ Quản lý và Quản trị (Bảng điều hành bưu cục trực quan, đối soát dòng tiền, cấu hình mạng lưới)
* **5.2. Xây dựng các dịch vụ tích hợp nâng cao:**
  * 5.2.1. Đăng ký, xác thực Email OTP qua Gmail SMTP Outbox Worker và đăng nhập Google OAuth2
  * 5.2.2. Thông báo trạng thái thời gian thực qua WebSocket STOMP
* **5.3. Triển khai hệ thống thực tế trên nền tảng đám mây AWS:**
  * 5.3.1. Mô hình vận hành trên AWS EC2 kết hợp Amazon RDS for SQL Server
  * 5.3.2. Cấu hình máy chủ web Nginx làm reverse proxy và bảo mật HTTPS / SSL Certbot
  * 5.3.3. Vận hành tên miền chính thức: `https://uteexpress.event5g.fun`
  * 5.3.4. Vận hành quy trình tự động hóa CI/CD với GitHub Actions
* **5.4. Đánh giá chất lượng hệ thống qua kiểm thử:**
  * 5.4.1. Chiến lược và kết quả kiểm thử tự động (Unit Test, Security Matrix, kiểm thử tích hợp trên SQL Server thật - 2.047 ca kiểm thử pass 100%)
  * 5.4.2. Kiểm thử chấp nhận người dùng thực tế (Manual UAT Acceptance Test theo 7 vai trò)

---

### CHƯƠNG 6: TỔNG KẾT VÀ HƯỚNG PHÁT TRIỂN
*(Chương này không chia thành các mục con vụn vặt, được trình bày liền mạch thành các đoạn văn văn xuôi tổng kết toàn diện đề tài)*
* Đoạn văn đánh giá tổng quan kết quả đạt được so với mục tiêu và phạm vi đặt ra ban đầu.
* Đoạn văn phân tích, đúc kết các ưu điểm nổi bật của hệ thống và thẳng thắn chỉ ra các mặt còn hạn chế trong quá trình thực hiện.
* Đoạn văn định hướng nghiên cứu và mở rộng hệ thống trong tương lai (tích hợp định vị GPS xe tải theo thời gian thực, mở rộng tích hợp cổng thanh toán trực tuyến bên thứ ba).
* Đoạn văn kết luận toàn văn đồ án và lời cảm ơn kết thúc.

---

### PHỤ LỤC
* **Phụ lục A:** Hướng dẫn cài đặt và cấu hình chạy cục bộ.
* **Phụ lục B:** Danh sách tài khoản thử nghiệm hệ thống (Mật khẩu `Demo@12345`).
* **Phụ lục C:** Cấu trúc tổ chức mã nguồn dự án.

### TÀI LIỆU THAM KHẢO
