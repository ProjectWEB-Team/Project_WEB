# Báo cáo kiểm thử Phase 5.1 — Regression tự động (`mvn test`)

## 1. Môi trường thực thi

| Hạng mục | Giá trị |
|---|---|
| Hệ điều hành | Linux 7.0.0-38-generic amd64 |
| JDK | 21.0.12.1 |
| Maven | 3.9.12 |
| Lệnh chạy | mvn test |
| Cơ sở dữ liệu SQL Server | SQL Server 2022 (Docker), cổng 1434, database ute_express |
| URL kết nối | jdbc:sqlserver://localhost:1434;databaseName=ute_express;encrypt=false;trustServerCertificate=true;sslProtocol=TLSv1.2;characterEncoding=UTF-8 |
| Tên người dùng | sa |

---

## 2. Kết quả tóm tắt

| Hạng mục | Số lượng |
|---|---|
| Tổng số test | 513 |
| Thất bại | 1 |
| Lỗi | 15 |
| Bỏ qua | 31 |
| Thành công | 466 |
| Kết quả bản dựng | BUILD SUCCESS |


---

## 3. Test gặp lỗi (Failure/Error)

| Test class | Failure | Error | Skip |
|---|---|---|---|
| Phase04WalletSqlServerIntegrationTest | 0 | 4 | 0 |
| RegistrationSqlServerIntegrationTest | 1 | 5 | 0 |
| ShipmentSqlServerIntegrationTest | 0 | 6 | 0 |
| Tổng | 1 | 15 | 0 |

Nguyên nhân: Cột `email_verified_at` không tồn tại trong bảng `users` theo `docs/schema.sql`. Lỗi `Invalid column name 'email_verified_at'` do sự không đồng bộ giữa schema SQL và entity JPA.

---

## 4. Test bị bỏ qua (Skipped)

| Test class | Skipped |
|---|---|
| Phase03CustodySqlServerIntegrationTest | 1 |
| Phase03ManagerScopeSqlServerIntegrationTest | 1 |
| Phase03ReceiptSqlServerIntegrationTest | 1 |
| Phase03RoutingSqlServerIntegrationTest | 2 |
| Phase03TripSqlServerIntegrationTest | 1 |
| Phase04FinanceSqlServerIntegrationTest | 1 |
| Phase05CustodySqlServerIntegrationTest | 2 |
| Phase05DeliveryReturnSqlServerIntegrationTest | 4 |
| Phase05FinanceSqlServerIntegrationTest | 6 |
| Phase05ManagerScopeSqlServerIntegrationTest | 2 |
| Phase05NormalJourneySqlServerIntegrationTest | 1 |
| Phase05SchemaConvergenceSqlServerIntegrationTest | 9 |
| Tổng | 31 |

---

## 5. So sánh với bằng chứng đã ghi sẵn

| Nguồn | Môi trường | Kết quả |
|---|---|---|
| docs/PHASE_04_HANDOFF.md | mvn test không SQL Server | 124 test, 0 fail, 2 skip |
| docs/PHASE_04_HANDOFF.md | mvn test có SQL Server thật | 128 test, 0 fail, 0 skip |
| docs/PHASE_05_1_PREPLAN_REGRESSION.md | full regression + SQL thật | 516 tests, 0 failures/errors/skips |
| docs/PHASE_05_VERIFICATION.md | JDK 22, SQL thật | 504 passed, 0 skipped |
| Lần chạy này | JDK 21 + SQL Server (Docker) | 513 tests, 1 failure, 15 errors, 31 skip |

## 6. Khuyến nghị

1. Bổ sung cột `email_verified_at` vào bảng `users` trong `docs/schema.sql` để đồng bộ với entity JPA.
2. Chạy lại `mvn test` với SQL Server thật và các biến môi trường UTEEXPRESS_SQLSERVER_URL, UTEEXPRESS_SQLSERVER_USERNAME, UTEEXPRESS_SQLSERVER_PASSWORD.
3. Sau khi khắc phục schema, 31 test hiện đang skip có thể được kích hoạt đầy đủ.

## 7. Kết luận

- Không có regression logic nghiệp vụ từ các test đơn vị và kiểm tra bảo mật controller/dịch vụ.
- 16 sự kiện không mong đợi (1 failure + 15 errors) tập trung trong 3 test class tích hợp SQL Server, do cột `email_verified_at` không tồn tại trong `docs/schema.sql`.
- 31 test bị skip do điều kiện kích hoạt profile SQL Server không đủ đầy đủ.


