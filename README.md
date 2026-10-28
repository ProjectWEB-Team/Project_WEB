# UTEExpress - Hệ thống quản lý vận hành logistics và chuyển phát nhanh

> **Đồ án môn học:** Lập trình Web  
> **Trường:** Đại học Sư phạm Kỹ thuật Thành phố Hồ Chí Minh (HCMUTE)  
> **Khoa:** Công nghệ Thông tin  
> **Giảng viên hướng dẫn:** ThS. Nguyễn Hữu Trung  
> **Đề tài:** Đề tài 9 - Xây dựng website quản lý chuỗi giao nhận Logistics UTEExpress  
> **Website triển khai thực tế:** [https://uteexpress.event5g.fun](https://uteexpress.event5g.fun)  
> **Kho mã nguồn GitHub:** [https://github.com/RVAN1102/Project_WEB](https://github.com/RVAN1102/Project_WEB)  

---

## 1. Thông tin nhóm và bảng phân công nhiệm vụ

### 1.1. Bảng tổng hợp phân công nhiệm vụ

| STT | Họ và tên | Mã số sinh viên | Vai trò | Nhiệm vụ đảm nhiệm chính | Tỷ lệ hoàn thành |
| :---: | :--- | :---: | :--- | :--- | :---: |
| 1 | **Ngô Bá Vạn** | 24162144 | Nhóm trưởng | Thống nhất quy trình vận hành; khởi tạo khung kiến trúc nền tảng; phát triển phân hệ bưu cục và trung chuyển liên tỉnh; phụ trách tích hợp chéo hệ thống; điều phối tiến độ, rà soát chức năng và tổng hợp báo cáo. | 100% |
| 2 | **Hoàng Phúc Khang** | 24162056 | Thành viên | Thống nhất quy trình vận hành; phát triển phân hệ tiếp nhận và tạo đơn hàng; thiết kế giao diện Figma (UI/UX), hạ tầng máy chủ, CI/CD, dịch vụ gửi email OTP và triển khai hệ thống; biên soạn nội dung báo cáo | 100% |
| 3 | **Huỳnh Tấn Anh Phát** | 24162091 | Thành viên | Thiết kế sơ đồ quan hệ thực thể (ERD) và cơ sở dữ liệu; phát triển phân hệ giao hàng chặng cuối và tài chính; thực hiện kiểm thử thực tế; biên soạn báo cáo và thiết kế slide thuyết trình. | 100% |

---

### 1.2. Chi tiết trách nhiệm và kết quả đóng góp của từng thành viên

#### Ngô Bá Vạn (Nhóm trưởng - MSSV: 24162144)
* **Kiến trúc và nền tảng:** Khởi tạo dự án Spring Boot 3, cấu hình bảo mật Spring Security 6, thiết lập cơ chế kiểm soát phiên đăng nhập và chuẩn hóa mô hình phân tầng Controller - Service - Repository.
* **Phân hệ bưu cục và trung chuyển:** Xây dựng thuật toán định tuyến trạm tự động, quy trình phân loại bưu gửi trên băng chuyền, đóng bao hàng container, niêm phong chì và điều phối các chuyến xe tải liên tỉnh.
* **Tích hợp hệ thống và quản trị:** Phụ trách tích hợp chéo các luồng dữ liệu, xử lý ngoại lệ liên trạm và xây dựng bảng điều hành tác nghiệp bưu cục cho vai trò quản lý.
* **Quản lý và kiểm duyệt:** Cùng nhóm phân tích nghiệp vụ, điều phối tiến độ chung, tham gia biên soạn, rà soát và kiểm duyệt nội dung báo cáo.

#### Hoàng Phúc Khang (Thành viên - MSSV: 24162056)
* **Quy trình và phân hệ tiếp nhận:** Cùng nhóm trưởng thống nhất quy trình vận hành; phát triển chức năng quản lý tài khoản, danh bạ sổ địa chỉ, tính cước vận chuyển tự động và luồng tạo đơn lấy hàng tận nơi hoặc gửi tại quầy.
* **Thiết kế giao diện UI/UX (Frontend):** Khảo sát hành trình người dùng và thiết kế nguyên mẫu giao diện trên Figma cho 7 vai trò trong hệ thống; phối hợp hiện thực hóa giao diện bằng JSP/JSTL và Bootstrap 5.3.
* **Hạ tầng đám mây và dịch vụ mạng:** Thiết lập quy trình tự động hóa CI/CD qua GitHub Actions, vận hành máy chủ AWS EC2 với Nginx reverse proxy, chứng chỉ SSL, cấu hình cơ sở dữ liệu Amazon RDS for SQL Server, tích hợp Gmail SMTP gửi mã OTP qua Outbox Worker và WebSocket STOMP.
* **Kiểm thử và tài liệu hạ tầng:** Tham gia viết bài kiểm thử đơn vị, kiểm thử tích hợp và biên soạn nội dung báo cáo.

#### Huỳnh Tấn Anh Phát (Thành viên - MSSV: 24162091)
* **Thiết kế cơ sở dữ liệu:** Khảo sát yêu cầu lưu trữ thông tin, thiết kế sơ đồ quan hệ thực thể (ERD) và xây dựng cấu trúc 25 bảng cơ sở dữ liệu ban đầu đạt chuẩn dạng chuẩn 3 (3NF) kèm các ràng buộc toàn vẹn dữ liệu.
* **Phân hệ giao hàng và tài chính:** Phát triển tác nghiệp phát hàng của shipper, xử lý giao thành công, ghi nhận giao thất bại, kích hoạt chuyển hoàn, quản lý tiền thu hộ COD và đối soát dòng tiền.
* **Ví điện tử và thanh toán:** Xây dựng tính năng ví điện tử khách hàng, lưu vết lịch sử biến động số dư tiền COD và quy trình gửi, phê duyệt yêu cầu rút tiền về tài khoản ngân hàng.
* **Kiểm thử thực tế và tài liệu báo cáo:** Thực hiện kiểm thử chấp nhận người dùng (UAT) cho đầy đủ 7 vai trò; biên soạn nội dung báo cáo và thiết kế slide thuyết trình.

---

## 2. Tổng quan dự án UTEExpress

### 2.1. Đặt vấn đề và bài toán thực tế
Ngành logistics và vận tải hàng hóa đòi hỏi sự chính xác tuyệt đối trong việc kiểm soát luồng hàng hóa và dòng tiền thu hộ. Một hệ thống bưu chính hoàn chỉnh không chỉ dừng lại ở việc tạo đơn và theo dõi trạng thái cơ bản, mà cốt lõi nằm ở năng lực vận hành tại các trạm trung chuyển, phân loại tự động, gom bao container, điều phối xe tải đường dài, và quản lý công nợ COD của từng bưu tá.

**UTEExpress** được xây dựng nhằm giải quyết toàn diện chuỗi cung ứng giao nhận bưu chính thuần vận chuyển (không phải sàn thương mại điện tử). Hệ thống bao quát trọn vẹn vòng đời của một bưu gửi:

```text
Khách hàng tạo đơn vận chuyển
              │
              ▼
Shipper lấy hàng tận nơi hoặc gửi tại bưu cục
              │
              ▼
Phân loại trên băng chuyền tại bưu cục / trung tâm khai thác (SOC)
              │
              ▼
Đóng bao hàng container và niêm phong chì bảo mật
              │
              ▼
Vận chuyển liên tỉnh bằng các chuyến xe tải trung chuyển đường dài
              │
              ▼
Bưu tá nhận tuyến phát hàng chặng cuối và thu tiền COD
              │
              ▼
Đối soát dòng tiền thu hộ và quyết toán vào ví điện tử khách hàng
```

### 2.2. Kiến trúc hệ thống
Dự án tuân thủ nghiêm ngặt mô hình kiến trúc ba tầng chuẩn mực trong kỹ thuật phần mềm doanh nghiệp:

* **Tầng trình diễn (Presentation Layer):**
  * Giao diện người dùng xây dựng trên nền tảng Spring MVC kết hợp JSP/JSTL 3.0 (Jakarta EE), Bootstrap 5.3 và Bootstrap Icons.
  * Thiết kế theo chuẩn nhận diện thương hiệu HCMUTE Logistics với hai tông màu chủ đạo: Xanh Coban (`#0F3D78`) và Cam Chuyển Phát (`#F26522`), bảo đảm độ tương phản cao và hiển thị tối ưu cho cả nhân viên kho lẫn tài xế ngoài hiện trường.
  * Kết nối WebSocket STOMP nhận thông báo sự kiện bưu gửi và trạng thái tác nghiệp tức thời theo thời gian thực mà không cần tải lại trang.
* **Tầng nghiệp vụ (Business Logic Layer):**
  * Nền tảng Spring Boot 3.3.4 trên nền Java 21 LTS, phân tách độc lập giữa các lớp Controller, DTO, Service Interface, Service Implementation và Repository.
  * Phân quyền bảo mật qua Spring Security 6 với cơ chế kiểm soát truy cập dựa trên vai trò (RBAC) kết hợp phạm vi bưu cục (Hub Scoping) và phân công nhiệm vụ (Task Assignment).
  * Áp dụng mẫu Transaction Outbox Pattern kết hợp tiến trình chạy ngầm định kỳ (Scheduled Worker) để xử lý gửi email xác thực mã OTP qua Gmail SMTP, tách biệt rủi ro gián đoạn mạng bên ngoài khỏi giao dịch cơ sở dữ liệu chính.
  * Sử dụng cơ chế khóa chống trùng lặp yêu cầu (Idempotency Key) cho các thao tác quét mã vạch và giao dịch tiền tệ nhằm loại bỏ hiện tượng ghi nhận dữ liệu hai lần do lỗi mạng hoặc thao tác người dùng.
* **Tầng dữ liệu (Data Access Layer):**
  * Quản trị dữ liệu qua Spring Data JPA và Hibernate ORM.
  * Cấu trúc cơ sở dữ liệu quan hệ gồm 25 bảng nghiệp vụ được chuẩn hóa, quản lý lịch sử thay đổi cấu trúc tự động qua Flyway migrations (từ V1 đến V22).
  * Sử dụng hệ quản trị cơ sở dữ liệu Microsoft SQL Server 2019+ trên cả môi trường phát triển cục bộ và dịch vụ Amazon RDS for SQL Server trên đám mây.

```text
Trình duyệt Web (JSP/JSTL 3.0, Bootstrap 5.3, WebSocket STOMP)
                           │
                           ▼
          Spring Security 6 (RBAC, Data Scoping, Google OAuth2)
                           │
                           ▼
                  Spring MVC Controllers
                           │
                           ▼
           Tầng nghiệp vụ (Service Interfaces và Service Impl)
               ├── Routing Engine (Thuật toán định tuyến trạm tự động)
               ├── Outbox Worker (Email OTP, thông báo chạy ngầm)
               └── Financial Engine (COD, Ví điện tử, Đối soát)
                           │
                           ▼
             Tầng dữ liệu (Spring Data JPA Repositories)
                           │
                           ▼
     Cơ sở dữ liệu quan hệ Microsoft SQL Server (Local / Amazon RDS)
```

### 2.3. Sáu vai trò chuẩn và nguyên tắc phân quyền

Hệ thống thiết lập 6 vai trò chính (Canonical Roles) lưu trữ trong cơ sở dữ liệu:

1. **Khách hàng (`ROLE_USER`):**
   * Đăng ký tài khoản qua số điện thoại kèm mã OTP hoặc đăng nhập nhanh bằng tài khoản Google (OAuth2).
   * Tạo đơn giao hàng, lựa chọn nhân viên lấy tận nơi hoặc tự mang đến bưu cục.
   * Tính toán cước phí tự động theo trọng lượng, thể tích quy đổi và bảng giá dịch vụ định sẵn.
   * Theo dõi hành trình chi tiết bằng mã vận đơn công khai hoặc xem thông tin bảo mật người nhận qua mã PIN.
   * Quản lý ví điện tử, theo dõi biến động số dư tiền thu hộ COD và gửi yêu cầu rút tiền về tài khoản ngân hàng.
2. **Nhân viên giao nhận (`ROLE_SHIPPER`):**
   * Tiếp nhận danh sách các đơn hàng cần lấy tại nhà khách hàng (first-mile) hoặc phát trong ngày theo tuyến phụ trách (last-mile).
   * Cập nhật kết quả giao hàng thành công kèm chữ ký xác nhận và thu tiền COD từ người nhận.
   * Ghi nhận giao hàng thất bại kèm lý do cụ thể, tự động kích hoạt số lần giao lại hoặc chuyển hoàn về người gửi.
3. **Nhân viên bưu cục (`ROLE_HUB_STAFF`):**
   * Tiếp nhận hàng trực tiếp tại quầy bưu cục, cân đo kích thước thực tế, thu tiền cước và in phiếu gửi.
   * Vận hành tại khu vực phân loại trên băng chuyền: quét mã đơn hàng, hệ thống tự động tra cứu bảng định tuyến để đề xuất trạm kế tiếp.
   * Thực hiện đóng nhiều đơn hàng vào bao hàng container lớn và quét mã chì niêm phong bảo đảm an toàn.
4. **Tài xế trung chuyển (`ROLE_LINEHAUL_DRIVER`):**
   * Tiếp nhận danh sách bao hàng được điều phối lên xe tải theo chuyến xe trung chuyển đường dài giữa các trạm Hub và trung tâm khai thác (SOC).
   * Xác nhận biên bản bốc xếp và thực hiện bàn giao các bao hàng khi xe đến bưu cục đích.
5. **Quản lý bưu cục (`ROLE_MANAGER`):**
   * Giám sát bảng điều hành tác nghiệp tại bưu cục: tồn kho tại chỗ, hàng đang trung chuyển đến và đi.
   * Phân công nhiệm vụ cho bưu tá, xử lý các sự cố đơn hàng ngoại lệ, kiểm duyệt và chốt bảng đối soát dòng tiền thu hộ trong ngày.
6. **Quản trị viên hệ thống (`ROLE_ADMIN`):**
   * Cấu hình danh mục trạm bưu cục, mạng lưới kết nối giữa các trạm, bảng giá dịch vụ và chính sách phụ phí.
   * Quản lý phân quyền tài khoản nhân sự trên hệ thống và theo dõi báo cáo phân tích hiệu suất hoạt động.

*(Khách vãng lai - Guest không lưu vai trò trong cơ sở dữ liệu; chỉ được phép xem trang chủ, ước tính phí vận chuyển và tra cứu mã vận đơn).*

### 2.4. Công nghệ và công cụ sử dụng

* **Nền tảng phát triển:** Java 21 LTS, Spring Boot 3.3.4, Apache Maven, Tomcat 10.1 (embedded).
* **Bảo mật và xác thực:** Spring Security 6, Google OAuth2 Client, thuật toán băm mật khẩu BCrypt.
* **Giao diện người dùng:** JSP / JSTL 3.0 (Jakarta EE), Bootstrap 5.3, Bootstrap Icons, bản vẽ thiết kế nguyên mẫu trên Figma.
* **Cơ sở dữ liệu:** Microsoft SQL Server 2019+ (phát triển nội bộ) và Amazon RDS for SQL Server (máy chủ đám mây), Flyway migration (V1 đến V22).
* **Dịch vụ mạng và truyền thông:** JavaMailSender với Gmail SMTP (cổng 587 STARTTLS gửi mã OTP qua Outbox Worker), Spring WebSocket STOMP (thông báo thời gian thực).
* **Kiểm thử tự động:** JUnit 5, Mockito, AssertJ, Spring Boot Test (tổng cộng 2.047 bài kiểm thử đơn vị và tích hợp chạy thành công 100%).
* **Hạ tầng và vận hành:** Máy chủ đám mây AWS EC2 (Ubuntu Server 24.04 LTS), Amazon RDS for SQL Server, Nginx reverse proxy với chứng chỉ SSL Certbot, quy trình tự động hóa CI/CD qua GitHub Actions.
* **Tên miền trực tuyến:** `https://uteexpress.event5g.fun`

---

## 3. Hướng dẫn cài đặt và chạy ứng dụng cục bộ

### 3.1. Yêu cầu môi trường
* Java Development Kit (JDK) phiên bản 21 trở lên.
* Apache Maven phiên bản 3.8 trở lên.
* Hệ quản trị cơ sở dữ liệu Microsoft SQL Server 2019 trở lên (cổng mặc định 1433).

### 3.2. Cấu hình cơ sở dữ liệu
Khởi tạo cơ sở dữ liệu `UTEExpress` trên hệ quản trị SQL Server cục bộ. Thiết lập các biến môi trường kết nối trong terminal:

```powershell
$env:UTEEXPRESS_SQLSERVER_URL = "jdbc:sqlserver://localhost:1433;databaseName=UTEExpress;encrypt=true;trustServerCertificate=true"
$env:UTEEXPRESS_SQLSERVER_USERNAME = "sa"
$env:UTEEXPRESS_SQLSERVER_PASSWORD = "YourStrongPassword!"
$env:UTEEXPRESS_JWT_SECRET = "UTEExpressSecureJWTKeyForDevelopmentAndTestingPurposesOnlyMustBeAtLeast32BytesLong"
```

### 3.3. Chạy kiểm thử tự động
Để kiểm tra tính toàn vẹn của mã nguồn và các trường hợp kiểm thử nghiệp vụ:

```bash
# Chạy kiểm thử đơn vị
mvn test

# Chạy kiểm thử tích hợp kết nối cơ sở dữ liệu SQL Server
mvn test -Dtest=SqlServerRealIntegrationTest
```

### 3.4. Khởi động ứng dụng
Chạy ứng dụng với cấu hình hồ sơ chạy cục bộ có hỗ trợ mã OTP mô phỏng và dữ liệu bưu cục mẫu:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local,otp-local,demo
```

Sau khi khởi động thành công, truy cập hệ thống tại địa chỉ: `http://localhost:8080`

---

## 4. Danh sách tài khoản thử nghiệm hệ thống

Khi kích hoạt hồ sơ dữ liệu mẫu (`demo`), hệ thống khởi tạo sẵn danh sách tài khoản cho từng vai trò với mật khẩu mặc định là **`Demo@12345`**:

| Vai trò | Tên đăng nhập | Mật khẩu mặc định | Mã bưu cục phụ trách | Trách nhiệm kiểm tra |
| :--- | :---: | :---: | :---: | :--- |
| **Quản trị viên (`ROLE_ADMIN`)** | `demo_admin` | `Demo@12345` | Hệ thống | Quản lý hệ thống, cấu hình bảng giá và mạng lưới trạm |
| **Quản lý bưu cục (`ROLE_MANAGER`)** | `demo_manager` | `Demo@12345` | `LOCAL2025-SOC` | Bảng điều hành tác nghiệp bưu cục và duyệt đối soát |
| **Nhân viên bưu cục (`ROLE_HUB_STAFF`)** | `demo_hub_hcm` | `Demo@12345` | `LOCAL2025-P79` | Tiếp nhận bưu gửi tại quầy và quét mã phân loại hàng |
| **Nhân viên giao nhận (`ROLE_SHIPPER`)** | `demo_shipper_hcm` | `Demo@12345` | `LOCAL2025-P79` | Giao hàng chặng cuối, thu tiền COD và cập nhật trạng thái |
| **Tài xế (`ROLE_LINEHAUL_DRIVER`)** | `demo_driver_hcm` | `Demo@12345` | `LOCAL2025-P79` | Vận chuyển các chuyến xe tải trung chuyển liên tỉnh |
| **Khách hàng gửi (`ROLE_USER`)** | `demo_sender` | `Demo@12345` | Khách hàng | Tạo đơn vận chuyển, tra cứu hành trình và quản lý ví COD |
| **Khách hàng nhận (`ROLE_USER`)** | `demo_receiver` | `Demo@12345` | Khách hàng | Tra cứu hành trình bưu gửi và xác nhận đơn giao đến |

---

## 5. Cấu trúc thư mục mã nguồn

```text
Project_WEB/
├── src/
│   ├── main/
│   │   ├── java/vn/iotstar/
│   │   │   ├── config/          # Cấu hình bảo mật Spring Security, WebSocket, dịch vụ gửi thư
│   │   │   ├── controller/      # Bộ điều hướng xử lý yêu cầu HTTP và điều hướng trang JSP
│   │   │   ├── dto/             # Đối tượng truyền dữ liệu (DTO) phân tách theo từng phân hệ
│   │   │   ├── entity/          # Các thực thể JPA ánh xạ 25 bảng cơ sở dữ liệu
│   │   │   ├── repository/      # Giao diện Spring Data JPA truy vấn dữ liệu SQL Server
│   │   │   ├── service/         # Giao diện và lớp cài đặt xử lý logic nghiệp vụ
│   │   │   └── worker/          # Tiến trình chạy ngầm quét hàng đợi Outbox gửi email OTP
│   │   ├── resources/
│   │   │   ├── db/migration/    # Kịch bản Flyway migration từ V1 đến V22
│   │   │   ├── db/demo/         # Dữ liệu bưu cục và tài khoản mẫu phục vụ thử nghiệm
│   │   │   └── application*.properties # Cấu hình ứng dụng theo từng môi trường chạy
│   │   └── webapp/
│   │       ├── assets/          # Biểu định kiểu CSS thương hiệu UTEExpress, JS, icons
│   │       └── WEB-INF/views/   # Giao diện JSP/JSTL phân cấp theo từng vai trò nghiệp vụ
│   └── test/                    # Bộ kiểm thử tự động hơn 2.000 trường hợp kiểm thử
├── deploy/                      # Kịch bản triển khai AWS EC2 và cấu hình Nginx reverse proxy
├── docs/                        # Tài liệu đặc tả kiến trúc (ADR), quy trình nghiệp vụ và ERD
├── pom.xml                      # Quản lý thư viện và cấu hình dự án Maven (Spring Boot 3.3.4, Java 21)
└── README.md                    # Tài liệu giới thiệu tổng quan và phân công nhiệm vụ đồ án
```
