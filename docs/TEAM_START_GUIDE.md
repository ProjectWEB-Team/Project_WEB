# Hướng dẫn bắt đầu cho nhóm UTEExpress

## 1. Mục tiêu và điểm xuất phát chung

`main` đang chứa **Phase 01 — Shared Walking Skeleton** đã triển khai: xác thực phiên/JWT, sáu vai trò chuẩn, tạo và đọc vận đơn tối thiểu, JSP/JSTL, SQL Server, xử lý lỗi an toàn và STOMP riêng tư. Đây là nền tảng chung; không tự thay đổi hợp đồng nền tảng trước khi trao đổi với trưởng nhóm.

Stack đã xác nhận trong mã nguồn:

- Java 22, Maven và Spring Boot 3.3.4.
- Spring MVC, Spring Security, JPA và WebSocket/STOMP.
- SQL Server là cơ sở dữ liệu duy nhất.
- JSP/JSTL + Bootstrap cho giao diện trình duyệt.

Phân công theo lát cắt dọc:

| Người phụ trách | Phần việc | Tài liệu cần đọc trên nhánh `project-plans` |
|---|---|---|
| Hoàng Phúc Khang (Person A) | Phase 02 — User & Shipment Intake | Bộ tài liệu Phase 02 |
| Ngô Bá Vạn (Person B, trưởng nhóm) | Phase 03 — Hub & Linehaul Operations | Bộ tài liệu Phase 03 |
| Huỳnh Tấn Anh Phát (Person C) | Phase 04 — Last-mile & Finance | Bộ tài liệu Phase 04 |

Mỗi phần việc luôn đi xuyên: JSP/UI → Controller → DTO → Service/ServiceImpl → Repository/JPA → SQL Server → phân quyền/phạm vi dữ liệu → STOMP khi cần → kiểm thử. Không chia một người chỉ làm giao diện, backend hoặc database.

## 2. Cài đặt môi trường cục bộ

Kiểm tra Java và Maven:

```powershell
java -version
mvn -version
```

Thiết lập biến môi trường cho **terminal PowerShell hiện tại**. Thay các giá trị trong dấu ngoặc bằng cấu hình SQL Server và khóa riêng của máy; không chép mật khẩu hoặc khóa thật vào repository.

```powershell
$env:UTEEXPRESS_SQLSERVER_URL = "<SQL_SERVER_JDBC_URL>"
$env:UTEEXPRESS_SQLSERVER_USERNAME = "<SQL_SERVER_USERNAME>"
$env:UTEEXPRESS_SQLSERVER_PASSWORD = "<SQL_SERVER_PASSWORD>"
$env:UTEEXPRESS_JWT_SECRET = "<BASE64_JWT_SECRET_AT_LEAST_32_DECODED_BYTES>"
```

`src/main/resources/application-local.example.properties` là mẫu cục bộ. Có thể sao chép thành `application-local.properties` và điền dữ liệu riêng của máy; file này phải luôn không được theo dõi bởi Git. Cấu hình chạy chính vẫn yêu cầu các biến môi trường ở trên.

Không tự đặt tên database, URL, user hay mật khẩu theo suy đoán. `spring.jpa.hibernate.ddl-auto=none`, vì vậy Hibernate không tự tạo schema. Chuẩn bị SQL Server và áp dụng schema chuẩn theo `docs/schema.sql` cùng các tài liệu canonical trước khi chạy ứng dụng.

Chạy kiểm thử và ứng dụng sau khi môi trường đã sẵn sàng:

```powershell
mvn test
mvn spring-boot:run
```

## 3. Quy trình làm việc an toàn

1. Cập nhật mã nền tảng:

   ```powershell
   git fetch origin main
   git switch main
   git pull --ff-only origin main
   ```

2. Chuyển sang nhánh `project-plans` để đọc đầy đủ bộ tài liệu của Phase được phân công. Không viết mã trên nhánh này.

   ```powershell
   git fetch origin
   git switch project-plans
   git pull --ff-only origin project-plans
   ```

3. Quay về `main`, cập nhật lại, rồi tạo nhánh chức năng cá nhân.

   ```powershell
   git switch main
   git pull --ff-only origin main
   git switch -c feature/<ten-phan-viec>
   ```

4. Triển khai trọn lát cắt được giao, có kiểm thử tập trung và không lấn sang phần của người khác.
5. Trước khi commit, thực hiện kiểm tra; sau đó dùng commit rõ ràng theo đúng lát cắt.

   ```powershell
   mvn test
   git diff --check
   git status --short
   git add <chi-cac-file-da-kiem-tra>
   git commit -m "feat(<pham-vi>): <mo-ta-ngan>"
   git push -u origin feature/<ten-phan-viec>
   ```

6. Gửi phần việc cho người rà soát được chỉ định: Khang rà soát Vạn, Vạn rà soát Phát, Phát rà soát Khang. Chỉ hợp nhất sau khi rà soát chéo và xử lý nhận xét.

## 4. Các bất biến bắt buộc

Đọc theo thứ tự ưu tiên: `ARCHITECTURE_DECISIONS.md` → `UTEExpress_Workflow.md` → `ERD.md` → `PROJECT_SPEC_KNOWLEDGE_BASE.md` → `ENGINEERING_STANDARDS.md`.

- Chỉ có 6 vai trò: `ROLE_USER`, `ROLE_SHIPPER`, `ROLE_HUB_STAFF`, `ROLE_LINEHAUL_DRIVER`, `ROLE_MANAGER`, `ROLE_ADMIN`.
- `ShipmentStatus` có đúng 13 giá trị; không thêm trạng thái tiện lợi cho sorting, đóng bao hoặc outbound.
- Lịch sử nghiệp vụ dùng `shipment_events.event_type`, không dùng event để thay thế trạng thái vận đơn.
- Ví là bảng riêng, giữ `balance` và `locked_balance`; không đặt số dư ví vào `users`.
- Hub hierarchy và `hub_routing_rules` là mô hình routing bắt buộc.
- Phân quyền tại service phải xét role cùng phạm vi dữ liệu theo ownership, hub, vị trí, phân công và vị trí vận đơn khi applicable.
- Không mở rộng sang Phase khác khi phần việc hiện tại chưa yêu cầu.

## 5. Hướng dẫn cho trợ lý lập trình

Trước mọi thay đổi, phải đọc các tài liệu canonical ở `docs/`, tài liệu Phase được giao trên `project-plans`, và mã nguồn hiện tại trên nhánh chức năng.

- Không được tự thiết kế lại role, 13 `ShipmentStatus`, kiến trúc database, hub/routing hoặc workflow đã chốt.
- Controller chỉ xử lý HTTP/DTO và gọi service; không gọi repository trực tiếp, không chứa quy tắc nghiệp vụ hoặc phân quyền dữ liệu phức tạp.
- Danh tính, quyền và phạm vi dữ liệu phải được kiểm tra ở service; transaction đặt tại service; dùng DTO ở ranh giới web.
- Không để lộ mật khẩu, hash, JWT secret, dữ liệu địa chỉ/liên hệ hoặc chi tiết tài chính không được phép trong response, log hay STOMP broadcast.
- Chỉ dùng SQL Server; thêm kiểm thử tập trung cho quy tắc mới và giữ các kiểm thử nền tảng chạy xanh.
- Không commit hoặc push khi chưa có xác nhận của người phụ trách. Nếu tài liệu canonical mâu thuẫn hoặc thiếu quyết định, báo trưởng nhóm để chốt thay vì tự đoán.

## 6. Checklist trước khi push

- [ ] Đã cập nhật từ `main` và chỉ thay đổi lát cắt được phân công.
- [ ] Đã đọc lại các bất biến, workflow và mô hình dữ liệu có liên quan.
- [ ] Controller → DTO → Service → Repository/JPA đúng ranh giới; service có transaction và kiểm tra phạm vi dữ liệu.
- [ ] Không thêm role/status trái chuẩn, không đổi `event_type`, không làm sai ví hoặc routing.
- [ ] Không có secret, password, JWT key hay `application-local.properties` trong thay đổi.
- [ ] `mvn test` chạy đạt và `git diff --check` không báo lỗi.
- [ ] Đã có commit rõ ràng, push branch riêng và yêu cầu đúng người rà soát chéo.
