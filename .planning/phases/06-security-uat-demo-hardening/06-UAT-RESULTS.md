# Phase 06 — Bằng chứng UAT (HARD-02, plan 06-07)

Nhánh mã nguồn: `feature/phase-06-hardening`. Tổng số regression và commit: xem mục 2 và `06-REPORT.md`.

**Lần chạy tự động gần nhất (2026-10-26 23:46 +07:00, commit `b6e5a66`):** `mvn -B clean test`, Oracle JDK 22 (build 22+36-2370), Windows 11 Home 10.0.26200, SQL Server 2025 Developer cục bộ, database kiểm thử riêng `uteexpress_phase05_test_tv2` (schema + 16 migration áp bằng `sqlcmd`). Kết quả: **1710 test chạy, 1710 đạt, 0 failure, 0 error, 0 skip**; trong đó **50 case SQL Server thật chạy, 0 skip**. Kết quả thủ công ở mục 4 vẫn `NOT_RUN`.

## 1. Ranh giới bằng chứng

- **Tự động (Maven):** test unit, slice MVC và test tích hợp chạy trong `mvn test`. Đây là bằng chứng cho phần kỹ thuật.
- **Thủ công:** hành vi trên trình duyệt (JSP, form, bố cục, nhiều phiên đăng nhập cùng lúc) chỉ được ghi khi có người quan sát. Ô chưa quan sát để `NOT_RUN` (D-05).
- Test bị **skipped không phải là passed.** Các test SQL Server thật chỉ chạy khi môi trường riêng của owner được nạp và database là database kiểm thử chuyên dụng `uteexpress_phase05_test_*`; database ứng dụng không bao giờ bị reset (D-06).
- Không thêm framework E2E trình duyệt (D-07).

## 2. Bản đồ phủ test tự động theo nhóm (D-06)

| Nhóm | Test có sẵn | Test mới của Phase 06 | Chứng minh điều gì |
|---|---|---|---|
| Luồng dọc | Phase01WalkingSkeletonTest, ShipmentSqlServerIntegrationTest, Phase05NormalJourneySqlServerIntegrationTest, Phase05OperationsJspRenderingTest, Phase05CustodySqlServerIntegrationTest, Phase05DeliveryReturnSqlServerIntegrationTest | PermissionMatrixShipmentOwnershipSliceTest (6) | Tạo đơn → giao → đối soát → chi trả trên SQL thật; JSP render với session/CSRF thật; chain + controller + service thật cho quyền sở hữu đơn |
| Bất biến trạng thái và sự kiện | CanonicalDomainContractTest, HubRoutingServiceImplTest, LinehaulTripServiceImplTest, LinehaulCustodyCompletionTest, DestinationReceiptServiceImplTest | DemoProfilePropertiesTest (5); 7 test bypass trong HubRoutingServiceImplTest; 5 test mô phỏng trong LinehaulTripServiceImplTest | Đúng 13 trạng thái; cờ demo không thêm trạng thái hay loại sự kiện; cờ chỉ bật bằng profile demo |
| Bất biến tài chính | WithdrawalServiceImplTest, Phase04WalletSqlServerIntegrationTest, Phase04FinanceSqlServerIntegrationTest, Phase05FinanceSqlServerIntegrationTest, RefundProducerConsumerTest, FinanceReconciliationServiceImplTest, ShipmentFinanceServiceImplTest, ShipmentFeeServiceTest | các ô tài chính trong PermissionMatrixOwnershipServiceTest (8) và PermissionMatrixFinanceScopeServiceTest (10) | `balance`/`locked_balance` tách biệt; ghi sổ đúng một lần; chỉ admin duyệt rút tiền; phạm vi Manager trên đối soát, hoàn tiền, bồi thường |
| Phân quyền và phạm vi dữ liệu | 19 lớp `*SecurityTest`, Phase03ManagerScopeSqlServerIntegrationTest, Phase05ManagerScopeSqlServerIntegrationTest | PermissionMatrixRouteTest (1002), PermissionMatrixApiChainTest (56), PermissionMatrixShipmentOwnershipSliceTest (6), PermissionMatrixOwnershipServiceTest (8), PermissionMatrixFinanceScopeServiceTest (10), PermissionMatrixHubServiceTest (27), PermissionMatrixTripAndScopeServiceTest (14) | Mọi route × 6 vai trò × ẩn danh × CSRF; chuỗi `/api/**`; các chiều role/hub/position/assignment/location/ownership ở tầng service |
| Đường lỗi | ExceptionHandlingTest, ApiExceptionHandlingTest, LastMileCollectionValidationTest | các ô thiếu CSRF trong PermissionMatrixRouteTest | Lỗi phân quyền trả về view an toàn; POST thiếu CSRF bị 403; xác thực số tiền thu |
| WebSocket | OperationalRealtimePublisherTest, MissingParcelRealtimePublisherTest, StompNotificationEventPublisherTest, StompShipmentEventPublisherTest, WebSocketEndpointSecurityTest | StompMultiSessionDeliveryTest (12), WebSocketConfigTest (+19) | Đúng người nhận, phiên không liên quan bị loại, payload tối thiểu, chỉ gửi sau commit, không gửi khi rollback, danh sách SUBSCRIBE cho phép |

## 3. Đối chiếu mục 3 của 05-UAT

`05-UAT.md` không bị sửa. Mục 3 **remains pending** cho đến khi có người chạy phần thủ công.

| Ý trong mục 3 | Bằng chứng tự động | Trạng thái |
|---|---|---|
| Form chạy với session thật và CSRF | PermissionMatrixRouteTest#everyWebPostWithoutACsrfTokenIsForbidden, Phase05OperationsJspRenderingTest | Phần kỹ thuật có; quan sát trên trình duyệt: Manual only |
| Màn hình manager/finance/return có dữ liệu dùng được | Phase05OperationsJspRenderingTest (render với dữ liệu mẫu) | Manual only |
| Tra cứu công khai/người nhận hiện nhãn an toàn khi đang điều tra | TrackingServiceImplTest (danh sách PUBLIC_LABELS) | Phần kỹ thuật có; quan sát: Manual only |
| Cảnh báo kiện thiếu tới tài xế được giao và Manager hai đầu | MissingParcelRealtimePublisherTest#missingReceiptNotifiesSiblingHubSupervisorsAndAssignedDriverOncePrivately | Tự động có; hai trình duyệt: Manual only |
| Phiên không liên quan không nhận | StompMultiSessionDeliveryTest#shipmentEventReachesOnlyTheOwnerAndOnlyAfterCommit, #handoverAcceptedReachesOnlyOutboundStaffOfThatHubAndTheReceivingDriver | Tự động có; hai trình duyệt: Manual only |
| Payload riêng tư tối thiểu, sau commit | StompMultiSessionDeliveryTest#payloadsSerializeToTheMinimalFieldSets, #destinationArrivalThroughTheSpringListenerWaitsForCommitAndIsSilentOnRollback | Tự động có |
| Rollback không phát gì | StompMultiSessionDeliveryTest#shipmentEventIsNeverSentOnRollback, MissingParcelRealtimePublisherTest#actualSpringListenerDefersUntilCommitAndSuppressesRollbackAndNoTransaction | Tự động có |
| Trang chi tiết tự kiểm tra phạm vi | PermissionMatrixShipmentOwnershipSliceTest#strangerCannotReadTheDetail, PermissionMatrixFinanceScopeServiceTest, PermissionMatrixHubServiceTest | Tự động có |

## 4. Checklist thủ công (trình duyệt và nhiều phiên)

Thực hiện theo `docs/PHASE_05_VERIFICATION.md` (bảng "Bounded live browser and STOMP acceptance") và `06-DEMO-SCRIPT.md`. Ghi id/vai trò, thời điểm, URL, tham chiếu tài nguyên, trạng thái quan sát và người nhận/không nhận.

| # | Hạng mục | Kỳ vọng | Result | Observed | Evidence |
|---|---|---|---|---|---|
| M1 | Người gửi đăng nhập, tạo đơn, CSRF hoạt động | Tạo đơn thành công, không lỗi 403 | NOT_RUN | | |
| M2 | Nhân viên hub gốc nhận, phân loại, đóng bao, bàn giao | Đúng trạng thái và sự kiện | NOT_RUN | | |
| M3 | Tài xế quét bao, chọn xe, khởi hành | Bàn giao ACCEPTED; xe IN_USE | NOT_RUN | | |
| M4 | Hub đích nhận kiện; điều phối giao; shipper thu COD | Thu đúng số tiền; DELIVERED | NOT_RUN | | |
| M5 | Manager đối soát CASH rồi VNPAY/TRANSFER, chi trả COD riêng | Ví chỉ đổi khi chi trả | NOT_RUN | | |
| M6 | Đối soát thiếu toàn bộ; hai Manager, tài xế, một phiên không liên quan đang mở | Tài xế và Manager có quyền nhận đúng một cảnh báo; phiên không liên quan không nhận | NOT_RUN | | |
| M7 | Tra cứu công khai trong lúc điều tra | Hiện "Đơn hàng đang được kiểm tra hành trình", không lộ chi tiết | NOT_RUN | | |
| M8 | Giao thất bại, hàng hoàn về gốc, người gửi trả phí, nhận lại | Không thử lại trước ngày/khi chưa có custody; phí hoàn mặc định 25000 | NOT_RUN | | |
| M9 | Hai trình duyệt (chủ ví và phiên không liên quan) cùng mở; Manager chi trả | Chỉ chủ ví nhận thông báo | NOT_RUN | | |
| M10 | Gây một thao tác bị từ chối/rollback | Không phát sự kiện tài chính; không thay đổi sổ | NOT_RUN | | |
| M11 | Kịch bản demo chính dưới profile demo (06-DEMO-SCRIPT mục 5) | Các bước 5.1–5.22 đạt kỳ vọng | NOT_RUN | | |
| M12 | Tám kịch bản ngoại lệ dưới profile demo (06-DEMO-SCRIPT mục 6) | Mỗi case đạt kỳ vọng | NOT_RUN | | |
| M13 | Bố cục giao diện trên màn hình nhỏ | Không vỡ bố cục | NOT_RUN | | |

## 5. Cách ghi kết quả và bàn giao

- Chỉ đổi `NOT_RUN` thành `PASS`/`FAIL` sau khi tự quan sát; điền Observed và Evidence.
- Phát hiện mới ghi theo mẫu ở mục 8 của `06-DEMO-SCRIPT.md`, kèm mức độ theo thang ASVS L1 trong `06-REPORT.md`.
- Kết quả hướng dẫn Phase 05.1 của TV3 là đầu vào riêng; owner xem cả hai khi review cuối (D-12).
