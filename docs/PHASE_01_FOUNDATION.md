# Phase 01 — Nền tảng vận hành tối thiểu

## Mục đích

Phase 01 là nền tảng nhỏ nhưng chạy xuyên suốt cho UTEExpress. Mã nguồn hiện có chứng minh luồng cơ bản từ trình duyệt đến cơ sở dữ liệu:

```text
JSP/JSTL + Bootstrap
→ Controller
→ DTO
→ Service / ServiceImpl
→ Repository / JPA
→ SQL Server
→ phản hồi trang và sự kiện riêng tư
```

Phạm vi này không phải là hệ thống vận hành logistics hoàn chỉnh. Nó tạo hợp đồng kỹ thuật chung để ba thành viên có thể phát triển các phần việc tiếp theo mà vẫn giữ cùng một kiến trúc.

## Chức năng nền tảng đã có

- Đăng nhập trình duyệt bằng phiên làm việc tại `/login`; sau khi đăng nhập thành công, người dùng được chuyển đến `/user/shipments/create`.
- Đăng nhập API tại `/api/auth/login` bằng JWT; các API được bảo vệ yêu cầu JWT hợp lệ.
- Trang JSP tạo vận đơn tối thiểu, kiểm tra dữ liệu đầu vào, chuyển yêu cầu qua DTO và chuyển hướng đến trang chi tiết khi tạo thành công.
- Vận đơn tối thiểu được lưu cùng sự kiện tạo vận đơn `SHIPMENT_CREATED`; trạng thái ban đầu là `CREATED`.
- Trang chi tiết chỉ cho người gửi đã xác thực xem vận đơn của chính mình; truy cập chéo bị từ chối tại tầng dịch vụ.
- Xử lý lỗi an toàn cho các tuyến JSP và API, không trả chi tiết kỹ thuật cho người dùng.
- Kết nối STOMP tại `/ws` yêu cầu phiên đã xác thực. Sự kiện tạo vận đơn được gửi sau khi giao dịch hoàn tất đến đích riêng của người tạo, không phát quảng bá thông tin vận đơn.

## Phân công Phase 01

Phase 01 là phần việc chung; mỗi người cùng nắm luồng đầy đủ và có trách nhiệm rà soát chéo.

| Thành viên | Vai trò trong Phase 01 | Phần việc chính đã phụ trách |
|---|---|---|
| Hoàng Phúc Khang — Person A | Nền tảng miền dữ liệu | Entity vận đơn/sự kiện, DTO, repository và luồng vận đơn tối thiểu. |
| Ngô Bá Vạn — Person B | Bảo mật và ranh giới truy cập | Sáu vai trò, xác thực phiên/JWT, điểm nối phân quyền theo ngữ cảnh và bảo vệ dữ liệu theo người dùng. |
| Huỳnh Tấn Anh Phát — Person C | Luồng trình duyệt và tích hợp đầu cuối | JSP/Controller, xử lý lỗi, STOMP tối thiểu và kiểm thử luồng xuyên tầng. |

Rà soát chéo bắt buộc:

```text
Khang rà soát phần của Vạn
Vạn rà soát phần của Phát
Phát rà soát phần của Khang
```

## Ranh giới kiến trúc cần giữ nguyên

### Vai trò và trạng thái

Hệ thống chỉ dùng sáu vai trò sau:

```text
ROLE_USER
ROLE_SHIPPER
ROLE_HUB_STAFF
ROLE_LINEHAUL_DRIVER
ROLE_MANAGER
ROLE_ADMIN
```

Tập trạng thái vận đơn cố định gồm 13 giá trị:

```text
CREATED
WAITING_FOR_PAYMENT
WAITING_FOR_HANDOVER
PICKED_UP
AT_HUB
IN_TRANSIT
OUT_FOR_DELIVERY
FAILED
DELIVERED
RETURN_TO_SENDER
RETURNED_TO_SENDER
CANCELLED
CLOSED_EXCEPTION
```

Trạng thái hiện tại được lưu ở `shipments.current_status`. Hoạt động nghiệp vụ được lưu riêng tại `shipment_events.event_type`; vì vậy không được dùng sự kiện để thay thế trạng thái hoặc thêm trạng thái tiện lợi cho sorting, đóng bao hay outbound.

### Phân lớp và dữ liệu

- Controller chỉ tiếp nhận HTTP, kiểm tra DTO, gọi service và chọn view/response; không truy cập repository trực tiếp.
- Service/ServiceImpl thực hiện quy tắc nghiệp vụ, kiểm tra quyền truy cập dữ liệu và xác định ranh giới giao dịch.
- Repository/JPA chỉ truy cập dữ liệu. SQL Server là cơ sở dữ liệu triển khai.
- UI dùng JSP/JSTL và Bootstrap. Đối tượng persistence không được bind trực tiếp từ request.
- Danh tính người thực hiện được suy ra từ ngữ cảnh bảo mật trên máy chủ, không lấy user ID hoặc hub ID từ dữ liệu do trình duyệt gửi lên.

### Bảo mật và thông báo thời gian thực

- Tuyến trình duyệt dùng phiên làm việc và CSRF; tuyến API dùng JWT không trạng thái.
- JWT dùng khóa cấu hình từ môi trường; không trả khóa, mật khẩu hoặc hash trong DTO/API.
- Kết nối và đăng ký STOMP yêu cầu người dùng đã xác thực. Đích thông báo hiện tại là riêng tư theo người dùng.

## Hợp đồng để các phần tiếp theo sử dụng

Person A có thể dựa trên tài khoản đã xác thực, DTO/service/repository vận đơn, kiểm tra quyền người gửi và mẫu JSP tạo/chi tiết vận đơn để mở rộng User & Shipment Intake.

Person B có thể dựa trên sáu vai trò, danh tính máy chủ, phân lớp service và sự phân biệt giữa `current_status` với `event_type` để phát triển Hub & Linehaul Operations. Hub scope, vị trí công việc, phân công và vị trí kiện hàng phải được kiểm tra tại service khi chức năng đó được bổ sung.

Person C có thể dựa trên vận đơn, lịch sử sự kiện, STOMP riêng tư sau giao dịch và lớp bảo mật để phát triển Last-mile & Finance. Ví điện tử vẫn phải là bảng riêng với `balance` và `locked_balance` khi được bổ sung.

Mọi thay đổi dùng chung đối với entity lõi, DTO chung, trạng thái, sự kiện, quyền hoặc schema phải được cả nhóm rà soát trước khi hợp nhất.

## Ngoài phạm vi Phase 01

Các nội dung dưới đây chưa được triển khai trong nền tảng này:

- đăng ký, OTP, đặt lại mật khẩu và quản lý tài khoản;
- intake vận đơn đầy đủ, báo giá, thanh toán, pickup và dropoff;
- hub hierarchy, routing, sorting, container, handover, linehaul và incident;
- giao hàng cuối tuyến, hoàn hàng, COD, đối soát, ví và rút tiền.

## Kiểm chứng có trong mã nguồn

Các test tự động hiện có kiểm tra tối thiểu các hợp đồng sau:

- đúng sáu vai trò và đúng 13 trạng thái vận đơn;
- xác thực phiên/JWT, quyền truy cập API, CSRF và tuyến JSP được bảo vệ;
- tạo vận đơn với `CREATED` và sự kiện `SHIPMENT_CREATED`;
- bảo vệ quyền đọc vận đơn theo người gửi;
- sự kiện STOMP chỉ gửi sau khi giao dịch hoàn tất và chỉ dùng DTO an toàn;
- lỗi validation, không tìm thấy và không có quyền được chuyển thành phản hồi an toàn.

Repository có test tích hợp SQL Server cho thao tác ghi/đọc vận đơn và sự kiện, bao gồm kiểm tra ràng buộc tracking và khóa ngoại. Test này chỉ chạy khi môi trường SQL Server thực đã được cung cấp; kết quả xác nhận môi trường thật và UAT thủ công cần được lưu kèm bằng chứng khi thực hiện.
