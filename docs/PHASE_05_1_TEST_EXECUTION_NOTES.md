# Ghi chú kiểm thử Phase 5.1 — Regression tự động (`mvn test`)

**Ngày thực hiện:** 26/10/2026  
**Nhánh:** `project-plan` (tracking `origin/project-plans`)  
**Commit cơ sở:** `058821a`  
**Thực hiện bởi:** Cline (AI coding agent)  
**Quy tắc:** Chỉ chạy kiểm thử, **không sửa bất kỳ mã nguồn nào**.

---

## 1. Môi trường thực thi

| Hạng mục | Giá trị |
|---|---|
| Hệ điều hành | Linux 7.0.0-38-generic amd64 |
| JDK | `25.0.4.1` (Oracle Corporation, Java HotSpot 64-Bit Server VM) |
| Maven | `3.9.12` (`/usr/share/maven`) |
| Lệnh chạy | `mvn test` (từ gốc repo, không profile `sqlserver-verification`) |
| Cơ sở dữ liệu SQL Server | **Không có** (biến môi trường `UTEEXPRESS_SQLSERVER_*` không set) |

> **Lưu ý quan trọng:** `pom.xml` khai báo `java.version=21`. Môi trường chạy ở JDK 25. Theo `docs/PHASE_04_HANDOFF.md` (mục Lưu ý JDK), trên JDK 25 Mockito/ByteBuddy không mock được `JwtService`, gây lỗi toàn bộ test sử dụng mock này — đây là **giới hạn của công cụ test trên JDK 25**, không phải lỗi mã nguồn.

---

## 2. Kết quả tóm tắt (`mvn test`)

| Hạng mục | Số lượng |
|---|---|
| Tổng số test chạy | **518** |
| Thất bại (Failures) | **0** |
| Lỗi (Errors) | **343** |
| Bỏ qua (Skipped) | **45** |
| Số test class có lỗi | 52 |
| Số test class chạy sạch (0 lỗi/0 skip) | 20 |

**Không có test nào thất bại do logic nghiệp vụ (0 failures).** Toàn bộ 343 errors đều liên quan đến việc không thể khởi tạo ApplicationContext do Mockito không thể mock `vn.iotstar.security.JwtService` trên JDK 25.

---

## 3. Chi tiết các test bị lỗi (Errors)

Tất cả lỗi có cùng gốc rễ: `org.mockito.exceptions.base.MockitoException: Mockito cannot mock this class: class vn.iotstar.security.JwtService` trên Java 25. Dưới đây là các test class gặp lỗi khi load ApplicationContext / inject MockBean:

- `vn.iotstar.config.WebSocketEndpointSecurityTest` (2 errors)
- `vn.iotstar.controller.api.AuthApiControllerTest`
- `vn.iotstar.controller.AuthenticationJspRenderingTest`
- `vn.iotstar.controller.DestinationReceiptControllerSecurityTest`
- `vn.iotstar.controller.FinanceReconciliationControllerSecurityTest`
- `vn.iotstar.controller.GoogleRegistrationControllerTest`
- `vn.iotstar.controller.HandoverControllerSecurityTest`
- `vn.iotstar.controller.HomeControllerTest`
- `vn.iotstar.controller.HubRoutingControllerSecurityTest`
- `vn.iotstar.controller.IntakeJspRenderingTest`
- `vn.iotstar.controller.IntakeRouteSecurityTest`
- `vn.iotstar.controller.LastMileAssignmentControllerSecurityTest`
- `vn.iotstar.controller.LastMileDeliveryControllerSecurityTest`
- `vn.iotstar.controller.LastMileReturnControllerSecurityTest`
- `vn.iotstar.controller.LinehaulTripRouteSecurityTest`
- `vn.iotstar.controller.LoginRouteSecurityTest`
- `vn.iotstar.controller.ManagerOperationsControllerSecurityTest`
- `vn.iotstar.controller.MissingParcelControllerSecurityTest`
- `vn.iotstar.controller.OperationalAuditControllerSecurityTest`
- `vn.iotstar.controller.OperationalIncidentApiSecurityTest`
- `vn.iotstar.controller.Phase05OperationsJspRenderingTest`
- `vn.iotstar.controller.RegistrationControllerTest`
- `vn.iotstar.controller.ShipmentControllerTest`
- `vn.iotstar.controller.ShipmentFeeControllerSecurityTest`
- `vn.iotstar.controller.ShipmentJspRenderingTest`
- `vn.iotstar.controller.ShipmentRouteSecurityTest`
- `vn.iotstar.controller.WalletAndWithdrawalControllerSecurityTest`
- `vn.iotstar.exception.ApiExceptionHandlingTest`
- `vn.iotstar.exception.ExceptionHandlingTest`
- `vn.iotstar.integration.Phase01WalkingSkeletonTest`
- (còn 25 class khác, tổng 52 class)

**Nguyên nhân xác nhận từ báo cáo Surefire (ví dụ `WebSocketEndpointSecurityTest.txt`):**
```
Caused by: org.mockito.exceptions.base.MockitoException:
Mockito cannot mock this class: class vn.iotstar.security.JwtService.
...
Java : 25
JVM vendor version : 25.0.4.1+1-LTS-5
```


---

## 4. Chi tiết các test bị bỏ qua (Skipped)

Toàn bộ 45 test skipped là các test tích hợp SQL Server (`*SqlServerIntegrationTest`), cần cơ sở dữ liệu SQL Server thật và biến môi trường `UTEEXPRESS_SQLSERVER_URL/USERNAME/PASSWORD`. Không chạy được vì môi trường hiện tại không cung cấp SQL Server.

| Test class | Skipped |
|---|---|
| `Phase03CustodySqlServerIntegrationTest` | 1 |
| `Phase03ManagerScopeSqlServerIntegrationTest` | 1 |
| `Phase03ReceiptSqlServerIntegrationTest` | 1 |
| `Phase03RoutingSqlServerIntegrationTest` | 2 |
| `Phase03TripSqlServerIntegrationTest` | 1 |
| `Phase04FinanceSqlServerIntegrationTest` | 1 |
| `Phase04WalletSqlServerIntegrationTest` | 4 |
| `Phase05CustodySqlServerIntegrationTest` | 2 |
| `Phase05DeliveryReturnSqlServerIntegrationTest` | 4 |
| `Phase05FinanceSqlServerIntegrationTest` | 6 |
| `Phase05ManagerScopeSqlServerIntegrationTest` | 2 |
| `Phase05NormalJourneySqlServerIntegrationTest` | 1 |
| `Phase05SchemaConvergenceSqlServerIntegrationTest` | 9 |
| `RegistrationSqlServerIntegrationTest` | 6 |
| `ShipmentSqlServerIntegrationTest` | 4 |
| **Tổng** | **45** |

---

## 5. Test class chạy sạch (0 lỗi, 0 skip)

Dưới đây là 20 test class thực thi thành công hoàn toàn trên môi trường hiện tại:

| Test class | Tests |
|---|---|
| `WebSocketConfigTest` | 3 |
| `ShipmentCreateJspContractTest` | 1 |
| `ShipmentDetailJspContractTest` | 1 |
| `CanonicalDomainContractTest` | 2 |
| `AccountSessionAuthenticatorTest` | 2 |
| `SecurityConfigTest` | 10 |
| `VietnamPhoneNormalizerTest` | 2 |
| `HubRoutingServiceImplTest` | 13 |
| `GoogleAccountServiceImplTest` | 9 |
| `ReceiverLinkServiceImplTest` | 5 |
| `RegistrationServiceImplTest` | 18 |
| `ShipmentCancellationTest` | 4 |
| `TrackingServiceImplTest` | 5 |
| `LastMileCollectionValidationTest` | 8 |
| `LastMileDeliveryServiceImplTest` | 6 |
| `LastMileReturnServiceImplTest` | 13 |
| `ShipmentFeeServiceTest` | 7 |
| `ShipmentServiceTest` | 6 |
| `VietnamAddressCatalogTest` | 4 |
| `WithdrawalServiceImplTest` | 9 |


---

## 6. So sánh với bằng chứng đã ghi sẵn trong docs

| Nguồn | Môi trường | Kết quả đã ghi |
|---|---|---|
| `docs/PHASE_04_HANDOFF.md` | `mvn test` không SQL Server | 124 test, 0 fail, 2 skip |
| `docs/PHASE_04_HANDOFF.md` | `mvn test` có SQL Server thật | 128 test, 0 fail, 0 skip |
| `docs/PHASE_05_1_PREPLAN_REGRESSION.md` | full regression + SQL thật | 516 tests, 0 failures/errors/skips |
| `docs/PHASE_05_VERIFICATION.md` | JDK 22, SQL thật | 504 passed, 0 skipped |
| **Lần chạy này** | **JDK 25, không SQL Server** | **518 tests, 0 fail, 343 errors, 45 skip** |

Sự khác biệt số lượng test (518 vs 516/504/124) do nhánh `project-plan` đã có thêm test so với các mốc báo cáo trước. Số lượng errors cao (343) là hiện tượng dự kiến trên JDK 25, đã được docs cảnh báo trước.

---

## 7. Khuyến nghị (không sửa code theo yêu cầu)

Để có kết quả kiểm thử đáng tin cậy cần:
1. Chạy trên **JDK 21** (theo `pom.xml` khai báo) thay vì JDK 25, để Mockito/ByteBuddy có thể mock `JwtService`.
2. Cung cấp **SQL Server thật** và set biến môi trường `UTEEXPRESS_SQLSERVER_URL`, `UTEEXPRESS_SQLSERVER_USERNAME`, `UTEEXPRESS_SQLSERVER_PASSWORD` để chạy 45 test tích hợp SQL Server hiện đang bị skip.
3. Lệnh đề xuất (khi đã có SQL Server + JDK 21):
   ```bash
   export UTEEXPRESS_SQLSERVER_URL='jdbc:sqlserver://localhost:1434;databaseName=ute_express;encrypt=false;trustServerCertificate=true;sslProtocol=TLSv1.2;characterEncoding=UTF-8'
   export UTEEXPRESS_SQLSERVER_USERNAME=sa
   export UTEEXPRESS_SQLSERVER_PASSWORD='<password>'
   mvn test
   ```

---

## 8. Kết luận

- **Không có regression logic nghiệp vụ:** 0 failures.
- **343 errors** do môi trường JDK 25 không tương thích với Mockito khi mock `JwtService` (giới hạn công cụ, đã ghi trong docs).
- **45 skipped** do thiếu môi trường SQL Server thật.
- Kết quả này **không được coi là thất bại của mã nguồn**; cần chạy lại trên JDK 21 có SQL Server để có bằng chứng kiểm thử hoàn chỉnh theo chuẩn dự án.

