# Đề tài 9: Xây dựng website quản lý chuỗi giao nhận Logistic UTEExpress
## WORKFLOW TOÀN HỆ THỐNG 6 ROLE – Phiên bản đề xuất hoàn chỉnh để triển khai đồ án

> **Đề tài:** 9. Xây dựng website quản lý chuỗi giao nhận Logistic UTEExpress bằng Spring boot + JSP/JSTL + Bootstrap + JPA + SQLServer/MySQL/ postgreSQL+JWT+Websocket
>
> **Kiến trúc đề xuất:** RBAC + Data Scoping + Hub Hierarchy + Task Assignment
>
> **Công nghệ:** Spring boot + JSP/JSTL + Bootstrap + JPA + SQLServer/MySQL/ postgreSQL + JWT + Websocket

---

# 1. Mục tiêu hệ thống

UTEExpress là **hệ thống giao nhận Logistics thuần vận chuyển**, không phải sàn thương mại điện tử.

Người dùng có thể:

```text
Tạo vận đơn
↓
Chọn Shipper tới lấy hoặc tự mang ra bưu cục
↓
Theo dõi kiện qua nhiều Hub / SOC
↓
Thu hộ COD nếu có
↓
Giao cho người nhận
↓
Đối soát COD
↓
Hoàn tiền / hoàn hàng khi phát sinh
```

Không cần các module:

```text
Shop
Product
Cart
Wishlist
Seller Marketplace
```

---

# 2. Sáu Role chính

| Role | Trách nhiệm chính |
|---|---|
| `ROLE_USER` | Người gửi / người nhận |
| `ROLE_SHIPPER` | First-mile + Last-mile |
| `ROLE_HUB_STAFF` | Xử lý hàng bên trong Hub/SOC |
| `ROLE_LINEHAUL_DRIVER` | Vận chuyển Hub ↔ Hub / SOC ↔ Hub |
| `ROLE_MANAGER` | Quản lý Hub cấp 1 / cấp 2 / SOC cấp 3 |
| `ROLE_ADMIN` | Quản trị toàn hệ thống |

`GUEST` không cần lưu thành Role trong database.

Guest chỉ được:

```text
Xem trang chủ
Đăng ký
Đăng nhập
Ước tính phí
Tra cứu mã vận đơn
```

---

# 3. Nguyên tắc phân quyền

Không chỉ kiểm tra:

```text
ROLE
```

Mà phải kiểm tra:

```text
ROLE
+
ASSIGNED HUB
+
POSITION
+
TASK ASSIGNMENT
+
CURRENT SHIPMENT LOCATION
```

Ví dụ:

```text
HUB_STAFF A
hub_id = DI_AN
position = INBOUND
```

được Scan kiện đang ở Dĩ An Hub:

```text
ALLOW
```

nhưng Scan kiện đang ở Hà Nội:

```text
FORBIDDEN
```

---

# 4. ROLE_USER – Người gửi và Người nhận

Một User có thể vừa là người gửi ở đơn này, vừa là người nhận ở đơn khác.

Không tạo:

```text
ROLE_SENDER
ROLE_RECEIVER
```

Ví dụ:

```text
Shipment #001
User A → User B
```

nhưng:

```text
Shipment #002
User C → User A
```

---

# 5. Quy tắc tài khoản Người gửi

Người gửi **bắt buộc phải có tài khoản**.

```text
VISITOR
   │
   ↓
MUỐN GỬI HÀNG
   │
   ↓
ĐÃ CÓ TÀI KHOẢN?
   │           │
  YES          NO
   │           │
   |        REGISTER
   |           │
   |           |
   |      VERIFY OTP
   |           │
   |           |
   └──────── LOGIN 
               │
               ↓
         CREATE SHIPMENT
```

---

# 6. Quy tắc tài khoản Người nhận

Người nhận **không bắt buộc phải có tài khoản**.

Nếu chưa có tài khoản:

```text
Người nhận
   ↓
Nhập mã vận đơn
   ↓
Xem Tracking
```

Nếu đã có tài khoản và SĐT trùng:

```text
receiver_phone
      ↓
SEARCH USER BY PHONE
      ↓
FOUND
      ↓
receiver_user_id = User.id
```

Người nhận đăng nhập sẽ tự thấy:

```text
ĐƠN ĐANG ĐẾN
```

---

# 7. Auto-link Shipment cho Người nhận & Chính sách Bảo mật (Data Masking)

Khi người gửi tạo đơn:

```text
NHẬP SĐT NGƯỜI NHẬN
        ↓
Chuẩn hóa SĐT
        ↓
SEARCH USER
        │
   ┌────┴────┐
   │         │
 FOUND    NOT FOUND
   │         │
   ↓         ↓
receiver_   receiver_
user_id     user_id
= USER.ID   = NULL
```

Dù có tài khoản hay không, vẫn lưu snapshot:

```text
receiver_name
receiver_phone
receiver_address
```

để thông tin vận đơn cũ không thay đổi khi User sửa Profile.

### Chính sách Che mờ thông tin (Data Masking):

Để phòng ngừa rủi ro Người gửi nhập nhầm SĐT khiến người lạ thấy thông tin nhạy cảm: - Khi đơn hàng tự động liên kết vào danh sách "Đơn đang đến" của User: - Tên người gửi: `Ng*** V** A**` - Địa chỉ chi tiết người nhận: `123 ***, Phường Linh Trung, TP. Thủ Đức` (Ẩn số nhà/tên đường) - Mô tả hàng hóa: `Ẩn chi tiết` - **Mở khóa thông tin đầy đủ**: Người nhận chỉ xem được đầy đủ chi tiết khi: - Nhập đúng **Mã vận đơn đầy đủ** (Tra cứu đích danh), hoặc - Xác thực qua **Mã OTP** gửi về số điện thoại nhận hàng.

---

# 8. Người nhận tạo tài khoản sau

```text
REGISTER
  ↓
VERIFY PHONE (OTP)
  ↓
ACCOUNT ACTIVE
  ↓
SEARCH SHIPMENT
WHERE receiver_phone = current_user.phone
AND receiver_user_id IS NULL
  ↓
LINK SHIPMENT (với cơ chế Data Masking ban đầu)
  ↓
Đơn xuất hiện trong "Đơn đang đến"
```

---

# 9. ROLE_SHIPPER – First-mile và Last-mile

Shipper chỉ làm hai loại nhiệm vụ:

```text
PICKUP
DELIVERY
```

Không làm Linehaul.

---

# 10. First-mile Shipper

```text
Coordinator Assign
      ↓
SHIPPER nhận Pickup Task
      ↓
Liên hệ Sender
      ↓
Đến địa chỉ lấy hàng
      ↓
Scan Tracking Number
      ↓
Kiểm tra kiện
      ↓
Thu Shipping Fee nếu Sender trả Cash
      ↓
Sender bàn giao
      ↓
PICKED_UP
      ↓
Đưa hàng về Origin Hub
```

---

# 11. Last-mile Shipper

```text
Delivery Hub
     ↓
Coordinator Assign
     ↓
SHIPPER nhận Delivery Task
     ↓
Scan kiện
     ↓
OUT_FOR_DELIVERY
     ↓
Liên hệ Receiver
     ↓
Thu COD / Shipping Fee nếu có
     ↓
DELIVERED
```

Nếu giao thất bại:

```text
DELIVERY_ATTEMPT_FAILED
```

---

# 12. ROLE_HUB_STAFF – Nhân viên vận hành Hub/SOC

Một Security Role:

```text
ROLE_HUB_STAFF
```

nhưng có `position`:

```text
INBOUND
SORTER
OUTBOUND
COORDINATOR
GENERAL_STAFF (Kiêm nhiệm cả 4 khâu trên - Dùng cho bưu cục nhỏ Delivery Hub Level 1)
```

---

# 13. HUB_STAFF – INBOUND

```text
Nhận kiện
↓
Scan Tracking / Bag
↓
Kiểm đếm
↓
Kiểm tra trạng thái
↓
Xác nhận nhập Hub
↓
AT_HUB
↓
Ghi shipment_event: ARRIVED_AT_HUB
```

---

# 14. HUB_STAFF – SORTER

```text
Kiện đã Inbound
      ↓
Scan Destination
      ↓
Route Planner
      ↓
Xác định Next Hub / SOC
      ↓
Sorting
      ↓
SORTED
```

---

# 15. HUB_STAFF – OUTBOUND

```text
SORTED
  ↓
Gom kiện cùng tuyến
  ↓
Tạo Bag / Container
  ↓
READY_FOR_OUTBOUND
  ↓
Tạo Handover
  ↓
Bàn giao Linehaul Driver
```

---

# 16. HUB_STAFF – COORDINATOR

```text
Theo dõi Pickup
Theo dõi Delivery
Assign Shipper
Theo dõi Backlog
Theo dõi Bag
Theo dõi Trip
Theo dõi Incident
Hỗ trợ Manager
```

---

# 17. ROLE_LINEHAUL_DRIVER

Driver chỉ vận chuyển giữa các trạm.

```text
Hub ↔ Hub
Hub ↔ SOC
SOC ↔ SOC
```

Không được:

```text
Sorting
Sửa Route
Sửa COD
Sửa Payment
Giao COD cho Receiver
Pickup tại nhà
```

---

# 18. Workflow Linehaul Driver

```text
OUTBOUND STAFF tạo HANDOVER
       ↓
Driver nhận yêu cầu
       ↓
Kiểm tra Bag / Container
       ↓
ACCEPT HANDOVER
       ↓
LOAD VEHICLE
       ↓
START TRIP
       ↓
IN_TRANSIT
       ↓
ARRIVED
       ↓
Bàn giao cho INBOUND STAFF Hub đích
```

---

# 19. ROLE_MANAGER

Chỉ dùng:

```text
ROLE_MANAGER
```

Không tạo:

```text
ROLE_HUB_MANAGER
ROLE_REGIONAL_MANAGER
ROLE_SOC_MANAGER
```

Cấp quản lý xác định bằng:

```text
manager.hub_id
      ↓
hub.level
```

---

# 20. Manager Level 1 – Delivery Hub Manager

Quản lý:

```text
Inbound Staff
Sorter
Outbound Staff
Coordinator
Pickup Shipper
Delivery Shipper
```

Dashboard:

```text
Inbound Today
Outbound Today
Waiting Sorting
Waiting Pickup
Ready For Delivery
Out For Delivery
Delivered
Failed
Backlog
Missing
Damaged
```

---

# 21. Manager Level 2 – Regional Manager

Ví dụ:

```text
Bình Dương Regional Hub
      │
      ├── Dĩ An Hub
      ├── TDM Hub
      └── Bến Cát Hub
```

Regional Manager:

```text
Theo dõi KPI các Hub con
Theo dõi Backlog
Theo dõi Trips
Theo dõi Incident
Yêu cầu giải trình
Theo dõi luồng lên SOC
```

---

# 22. Manager Level 3 – SOC Manager

Quản lý:

```text
SOC Staff
Regional Managers
Linehaul Trips
SOC Backlog
Inbound / Outbound
Incident liên vùng
KPI toàn vùng
```

---

# 23. ROLE_ADMIN

Admin:

```text
Quản lý User
Quản lý nhân viên
Gán Role
Gán Hub
Quản lý cây Hub
Quản lý bảng giá
Quản lý Route Rule
Quản lý SOC
Quản lý toàn bộ Dashboard
Duyệt Withdrawal
Giám sát Incident
```

---

# 24. Cây quản lý

```text
                         ROLE_ADMIN
                             │
                             ↓
                       ROLE_MANAGER
                         SOC Level 3
                             │
                             ↓
                       ROLE_MANAGER
                      Regional Level 2
                             │
                             ↓
                       ROLE_MANAGER
                         Hub Level 1
                             │
                   ┌─────────┼─────────┐
                   ↓         ↓         ↓
               INBOUND    SORTER    OUTBOUND
                   \         |         /
                    \        |        /
                     ROLE_HUB_STAFF
                           │
                      COORDINATOR
```

---

# 25. Cấu trúc mạng lưới Logistics

```text
LEVEL 1 = DELIVERY HUB
LEVEL 2 = REGIONAL HUB
LEVEL 3 = SOC
```

---

# 26. Hai SOC miền Nam

## Long An SOC

```text
WESTERN GATEWAY
```

Phụ trách demo:

```text
Tiền Giang
Bến Tre
Vĩnh Long
Cần Thơ
Đồng Tháp
An Giang
Kiên Giang
Hậu Giang
Sóc Trăng
Bạc Liêu
Cà Mau
```

---

## Bình Dương SOC

```text
CENTRAL / HIGHLANDS / NORTH GATEWAY
```

Phụ trách demo:

```text
Tây Nguyên
Miền Trung
Miền Bắc
```

---

# 27. Sơ đồ Network

```text
                              UTEEXPRESS
                                  │
                   ┌──────────────┴──────────────┐
                   │                             │
                   ↓                             ↓
              LONG AN SOC                 BINH DUONG SOC
                Level 3                       Level 3
             Miền Tây              Trung / Bắc / Tây Nguyên
                   │                             │
        ┌──────────┼─────────┐        ┌──────────┼──────────┐
        ↓          ↓         ↓        ↓          ↓          ↓
     Cần Thơ    An Giang   Cà Mau  Đà Nẵng   Đắk Lắk   North SOC
        │                                               │
        ↓                                               ↓
Regional Hub                                       Hà Nội Hub
        │                                               │
        ↓                                               ↓
Delivery Hub                                      Delivery Hub
```

---

# 28. Workflow tạo Shipment

```text
USER
 ↓
[ GỬI HÀNG ]
 ↓
Thông tin Sender
 ↓
Thông tin Receiver
 ↓
Auto-link Receiver by Phone
 ↓
Thông tin kiện
 ↓
Weight / Size / Declared Value
 ↓
COD?
 ↓
Chính sách đồng kiểm (Inspection Policy)?
 ├── INSPECTION_ONLY (Cho xem hàng không cho thử)
 ├── TRIAL_ALLOWED (Cho thử hàng)
 └── NO_INSPECTION (Không cho xem hàng)
 ↓
Ai trả Shipping Fee?
 ├── SENDER
 └── RECEIVER
 ↓
Pickup Method?
 ├── HOME_PICKUP
 └── SELF_DROPOFF
 ↓
CALCULATE FEE
 ↓
PAYMENT nếu cần
 ↓
CREATE SHIPMENT
 ↓
GENERATE TRACKING NUMBER
```

Ví dụ:

```text
UTEVN260910000001
```

---

# 29. HOME_PICKUP Workflow

```text
CREATE SHIPMENT
      ↓
WAITING_FOR_HANDOVER
      ↓
Coordinator Assign Shipper
      ↓
PICKUP TASK ASSIGNED
      ↓
Shipper liên hệ Sender
      ↓
Đến địa chỉ Sender
      ↓
Scan
      ↓
Thu phí nếu Cash + Sender trả
      ↓
Sender bàn giao
      ↓
PICKED_UP
      ↓
Shipper về Origin Hub
      ↓
Inbound Staff nhận
      ↓
AT_HUB
```

---

# 30. SELF_DROPOFF Workflow

```text
CREATE SHIPMENT
      ↓
WAITING_FOR_HANDOVER
      ↓
Sender mang kiện đến Hub
      ↓
Inbound Staff kiểm tra & Cân / đo lại
      │
      ├── Cân nặng/kích thước ĐÚNG hoặc NHỎ HƠN khai báo
      │      │
      │      ├── Đã thanh toán trước (VNPay) → Scan nhận hàng
      │      └── Chưa thanh toán (fee_payer = SENDER, CASH) → Thu cước gốc → Scan
      │
      └── Cân nặng/kích thước LỚN HƠN khai báo (Phát sinh chênh lệch cước)
             ↓
         Tính phụ phí vượt tải (WEIGHT_SURCHARGE)
             │
             ├── fee_payer = SENDER
             │      │
             │      ├── Sender đồng ý: Nộp phụ phí (Cash / VNPay / E-wallet)
             │      │      ↓
             │      │   Tạo payment (type = WEIGHT_SURCHARGE, status = PAID)
             │      │      ↓
             │      │   Scan nhận kiện
             │      │
             │      └── Sender từ chối: Hủy đơn tại quầy
             │             ↓
             │          CANCELLED (reason = WEIGHT_DISCREPANCY)
             │             ↓
             │          Hoàn 100% cước gốc vào E-Wallet nếu đã thanh toán trước
             │
             └── fee_payer = RECEIVER
                    ↓
                 Cập nhật tổng shipping_fee mới trên vận đơn
                    ↓
                 Gửi thông báo cập nhật cước cho Receiver qua SMS/App
                    ↓
                 Scan nhận kiện
      ↓
AT_HUB
```

Không có:

```text
PICKUP TASK ASSIGNED
PICKED_UP
```

---

# 31. HOME_PICKUP Timeout 48 giờ

```text
PICKUP TASK ASSIGNED
      ↓
Shipper liên hệ
      ↓
NO ANSWER
      ↓
Pickup Attempt #1
      ↓
Tiếp tục liên hệ
      ↓
Quá 48h chưa lấy được
      ↓
CANCELLED (reason = PICKUP_TIMEOUT)
```

Nếu lỗi UTEExpress:

```text
CANCELLED (reason = PICKUP_SERVICE_FAILURE)
```

---

# 32. SELF_DROPOFF Timeout 48 giờ

```text
WAITING_FOR_HANDOVER
      ↓
48 HOURS TIMER
      │
      ├── Sender tới Hub
      │      ↓
      │ AT_HUB
      │
      └── Không tới
             ↓
CANCELLED (reason = DROPOFF_TIMEOUT)
```

---

# 33. Sender chủ động hủy & Xử lý Xung đột Lấy hàng (Race Condition)

1.  Được hủy trực tiếp ngay lập tức khi:

```text
CREATED
WAITING_FOR_HANDOVER
```

$\rightarrow$ Hệ thống hủy đơn ngay lập tức (`CANCELLED (reason = SENDER_REQUEST)`), hoàn 100% cước vào E-Wallet nếu đã thanh toán trước.

2.  Khi đơn đã ở trạng thái `PICKUP TASK ASSIGNED` (Shipper đã nhận việc và đang di chuyển):

```text
SENDER BẤM HỦY ĐƠN TRÊN APP
        ↓
System kiểm tra Khóa lạc quan (Optimistic Locking):
Kiểm tra xem Shipper đã bấm PICKED_UP chưa?
        │
   ┌────┴──────────────────────────┐
   │                               │
CHƯA PICKED_UP                   ĐÃ BẤM PICKED_UP
   │ (Shipper đang trên đường)     │ (Shipper đã nhận hàng tận tay)
   ↓                               ↓
Hủy Task trong assignments       Từ chối yêu cầu hủy
Gửi WebSocket/Push báo Shipper   Đơn hàng tiếp tục đi vào mạng lưới
Dừng di chuyển lấy hàng          Nếu Sender muốn nhận lại hàng:
   ↓                               ↓
CANCELLED (reason = SENDER_REQUEST)              Phải chuyển sang RETURN_REQUESTED
Hoàn 100% cước vào Ví Sender     Sender chịu chi phí hoàn (Return Fee)
```

3.  Không được Cancel trực tiếp khi:

```text
PICKED_UP trở đi
```

Nếu đã vào mạng vận chuyển:

```text
RETURN_REQUESTED
```

---

# 34. Origin Hub Workflow & Cơ chế Tự động hóa Route Planner

```text
AT_HUB
      ↓
INBOUND STAFF (Quét mã nhập Hub)
      ↓
SORTER QUÉT MÃ VẬN ĐƠN
      ↓
HỆ THỐNG ROUTE PLANNER (Tự động tra cứu Ma trận định tuyến tĩnh)
      ↓
Màn hình hiển thị chỉ định:
├── Next Hop Hub: LONG_AN_SOC
└── Khay / Bao chỉ định: BAG-LONGAN-01
      ↓
Sorter đặt kiện vào đúng Bao chỉ định
      ↓
Kiện đạt trạng thái: SORTED
      ↓
OUTBOUND STAFF (Đóng niêm phong Bag khi đủ kiện/hết giờ)
      ↓
BAG / CONTAINER
      ↓
READY_FOR_OUTBOUND
```

---

# 35. Bag / Container

Ví dụ:

```text
BAG-DIAN-BD-001
```

Chứa:

```text
UTE001
UTE002
UTE003
...
UTE050
```

Thông tin:

```text
Origin Hub
Destination Hub
Parcel Count
Created By
Status
```

---

# 36. Handover Staff A → Driver B

```text
OUTBOUND STAFF A
      ↓
CREATE HANDOVER
      ↓
WAITING_DRIVER_CONFIRMATION
      ↓
LINEHAUL DRIVER B
      ↓
CHECK BAG / COUNT
      ↓
ACCEPT HANDOVER
      ↓
HANDOVER_ACCEPTED
```

Lưu:

```text
sender_employee_id
receiver_driver_id
expected_containers
accepted_at
```

---

# 37. Driver B → Staff C & Quy trình Mở bao (UNPACK_BAG)

```text
Driver B
   ↓
START TRIP
   ↓
IN_TRANSIT
   ↓
ARRIVED DESTINATION HUB
   ↓
INBOUND STAFF C
   ↓
SCAN BAG (Mã bao/container)
   ↓
THỰC HIỆN DỠ BAO (UNPACK_BAG)
   ↓
Quét Barcode từng kiện bên trong
   ↓
Đối chiếu: Số lượng kiện thực tế vs Danh sách trong Bag
   │
   ├── ĐỦ (Expected == Received, vd 50/50)
   │      ↓
   │   Container status = UNPACKED_FULL
   │      ↓
   │   Cập nhật toàn bộ 50 kiện: current_status = AT_HUB
   │      ↓
   │   Ghi shipment_event: ARRIVED_AT_HUB
   │      ↓
   │   Handover = COMPLETED
   │      ↓
   │   Trip = COMPLETED
   │
   └── THIẾU KIỆN (Expected != Received, vd nhận 49/50)
          ↓
       Container status = UNPACKED_WITH_DISCREPANCY
          ↓
       TÁCH LUỒNG XỬ LÝ (Không phong tỏa toàn bộ):
       ├── 49 KIỆN HỢP LỆ:
       │      ↓
       │   Cập nhật current_status = AT_HUB
       │      ↓
       │   Ghi shipment_event: ARRIVED_AT_HUB
       │      ↓
       │   Chuyển ngay sang Sorter để phân loại tiếp
       │
       └── 1 KIỆN THẤT LẠC (vd: UTE043):
              ↓
           Cập nhật current_status = IN_TRANSIT
           Đặt on_hold = true
              ↓
           Tạo INCIDENT (type = MISSING_PARCEL)
              ↓
           Handover = COMPLETED_WITH_DISCREPANCY
              ↓
           Trip = COMPLETED_WITH_DISCREPANCY
```

---

# 38. Xử lý Incident Kiện thất lạc (MISSING_PARCEL)

Ví dụ:

```text
Expected = 50 kiện (Bag: BAG-DIAN-BD-001)
Received = 49 kiện
Missing = UTE043
```

1.  Giải phóng hàng hóa:

```text
49 kiện hợp lệ lập tức cập nhật:
shipments.current_status = AT_HUB
shipment_events.event_type = ARRIVED_AT_HUB
và tiếp tục luồng chia chọn (Sorting), không làm chậm trễ tiến độ giao hàng.
```

2.  Cô lập kiện thiếu:

```text
shipments (id = UTE043).current_status = IN_TRANSIT
shipments (id = UTE043).on_hold = true
container_items (BAG-DIAN-BD-001, UTE043).status = MISSING
```

3.  Mở biên bản sự cố:

```text
INCIDENT
--------------------------------
type = MISSING_PARCEL
shipment_id = UTE043
trip_id = TRIP_01
from_hub_id = DI_AN
to_hub_id = LONG_AN_SOC
reported_by = STAFF_C
status = INVESTIGATING
```

4.  Thông báo tự động:

```text
Origin Hub Manager (Kiểm tra camera khu vực xuất hàng)
Linehaul Driver (Kiểm tra lại thùng xe tải)
Destination Hub Manager (Ghi nhận biên bản)
User Tracking: Hiển thị "Đơn hàng đang được kiểm tra hành trình" (Không lộ chi tiết nội bộ)
```

5.  Kết luận điều tra & Quy trình Bồi thường (Compensation):

```text
HỘI ĐỒNG KIỂM SOÁT ĐIỀU TRA (Hub Managers + Admin)
            │
   ┌────────┴────────────────────────┐
   │                                 │
TÌM THẤY KIỆN HÀNG                 XÁC NHẬN MẤT HÀNG
   ↓                                 ↓
Nhập kho lại: AT_HUB                 Chuyển trạng thái DUY NHẤT:
Ghi event: ARRIVED_AT_HUB            CLOSED_EXCEPTION (closure_reason = LOST)
Tiếp tục luồng giao bình thường      ↓
                                   TÍNH TOÁN BỒI THƯỜNG:
                                   ├── Bồi thường 100% declared_value (Giá trị khai giá)
                                   └── Hoàn 100% shipping_fee (Nếu Sender đã thanh toán)
                                     ↓
                                   Tự động CREDIT vào Ví điện tử Sender:
                                   users.e_wallet += declared_value + (shipping_fee nếu fee_payer = SENDER)
                                     ↓
                                   Ghi nhận: wallet_transactions (type = COMPENSATION)
                                     ↓
                                   incidents.status = RESOLVED
                                     ↓
                                   Giữ nguyên vận đơn ở CLOSED_EXCEPTION (closure_reason = LOST)
```

---

# 39. Regional Sorting & Ma trận Định tuyến (Route Matrix)

Hệ thống sử dụng bảng tra cứu ma trận định tuyến tĩnh `hub_routing_rules`:

```text
Cấu trúc Rule: (Current Hub, Destination Region/Province) → Next Hub
```

Ví dụ tại Bình Dương / Dĩ An:

```text
Đích: Cần Thơ, An Giang, Cà Mau... (Miền Tây)
→ NEXT_HUB = LONG_AN_SOC
```

```text
Đích: Đà Nẵng, Hà Nội, Đắk Lắk... (Miền Trung / Bắc / Tây Nguyên)
→ NEXT_HUB = BINH_DUONG_SOC
```

Ví dụ phân loại 100 kiện tại Hub:

```text
100 kiện
 ├── 40 kiện đi Miền Tây   → Gán vào Bao đi LONG_AN_SOC
 ├── 35 kiện đi Miền Trung → Gán vào Bao đi BINH_DUONG_SOC
 └── 25 kiện đi Miền Bắc   → Gán vào Bao đi BINH_DUONG_SOC
```

Khi Sorter quét mã, hệ thống tự động sinh `shipment_routes` tương ứng cho toàn bộ hành trình. Sorter không cần quyết định thủ công.

---

# 40. Long An SOC Workflow

```text
INBOUND
 ↓
SCAN
 ↓
SORT BY DESTINATION
 ↓
CREATE BAG
 ↓
OUTBOUND
 ↓
LINEHAUL
```

Ví dụ:

```text
Cần Thơ     30
An Giang    20
Kiên Giang  15
Cà Mau      15
Vĩnh Long   20
```

---

# 41. Bình Dương SOC Workflow

```text
INBOUND
 ↓
SORT
 ↓
Tây Nguyên / Miền Trung / Miền Bắc
 ↓
CREATE BAG
 ↓
OUTBOUND
 ↓
LINEHAUL
```

Ví dụ Hà Nội:

```text
Bình Dương SOC
   ↓
North SOC
   ↓
Hà Nội Regional Hub
   ↓
Cầu Giấy Delivery Hub
```

---

# 42. Workflow miền Tây

```text
Sender
 ↓
Pickup Shipper / Self Dropoff
 ↓
Dĩ An Delivery Hub
 ↓
Bình Dương Regional Hub
 ↓
Long An SOC
 ↓
Cần Thơ Regional Hub
 ↓
Ninh Kiều Delivery Hub
 ↓
Delivery Shipper
 ↓
Receiver
```

---

# 43. Workflow miền Bắc

```text
Sender
 ↓
Origin Delivery Hub
 ↓
Regional Hub
 ↓
Bình Dương SOC
 ↓
North SOC
 ↓
Hà Nội Regional Hub
 ↓
Destination Delivery Hub
 ↓
Delivery Shipper
 ↓
Receiver
```

---

# 44. Last-mile Workflow

```text
AT_HUB (Phân loại trạm phát)
       ↓
Coordinator Assign Delivery Task
       ↓
Shipper Scan nhận kiện
       ↓
OUT_FOR_DELIVERY
       ↓
CONTACT RECEIVER
```

---

# 45. Giao thành công

```text
Receiver đồng ý nhận
      ↓
Có tiền cần thu?
   │          │
  NO         YES
   │          │
   │      Collect Money
   │          │
   └────┬─────┘
        ↓
CONFIRM DELIVERY
        ↓
DELIVERED
```

---

# 46. Delivery Attempt thất bại & Phân loại Lý do

Lý do thất bại chia thành 2 nhóm:

```text
NHÓM 1 – TẠM HOÃN / CÓ THỂ THỬ LẠI:
├── NO_ANSWER (Không nghe máy)
├── PAYMENT_NOT_AVAILABLE (Chưa có tiền mặt, hẹn hôm sau)
├── RESCHEDULED (Khách hẹn ngày cụ thể → Shipper bắt buộc chọn rescheduled_date)
└── OTHER (Lý do khách quan: thời tiết, xe hỏng...)

NHÓM 2 – TỪ CHỐI DỨT KHOÁT / LỖI ĐỊA CHỈ (FAST-FAIL):
├── RECEIVER_REFUSED (Khách từ chối nhận hàng dứt khoát / Bom hàng)
└── WRONG_ADDRESS (Địa chỉ không có thật / Sai tỉnh thành không thể phát)
```

Workflow xử lý:

```text
OUT_FOR_DELIVERY
      ↓
GIAO THẤT BẠI → TẠO DELIVERY_ATTEMPT (kèm lý do + ảnh chụp xác thực)
      │
      ├── NẾU THUỘC NHÓM 1 (Có thể thử lại):
      │      ↓
      │   RETURN PARCEL TO HUB (Cuối ca giao hàng)
      │      ↓
      │   RESCHEDULE (Áp dụng quy tắc 3 lần / 3 ngày ở Mục 47)
      │
      └── NẾU THUỘC NHÓM 2 (RECEIVER_REFUSED hoặc WRONG_ADDRESS):
             ↓
          KÍCH HOẠT CƠ CHẾ FAST-FAIL (Không bắt buộc phát lại):
             ↓
          Gửi thông báo xác nhận cho Người gửi qua App/SMS
             ↓
          Chuyển ngay sang: FAILED (fail_reason = RECEIVER_REFUSED hoặc WRONG_ADDRESS tương ứng)
             ↓
          Kích hoạt quy trình chuyển hoàn: RETURN_TO_SENDER
```

---

# 47. Rule Giao hàng 3 lần / 3 ngày (Chỉ áp dụng cho Nhóm 1)

```text
DAY 1 → ATTEMPT #1 FAILED (Lý do: Không nghe máy / Hẹn lại)
        ↓ (Hẹn lại theo rescheduled_date hoặc tự động gán ngày hôm sau)
DAY 2 → ATTEMPT #2 FAILED
        ↓
DAY 3 → ATTEMPT #3 FAILED
        ↓
FAILED (fail_reason = DELIVERY_ATTEMPTS_EXHAUSTED)
        ↓
RETURN_TO_SENDER
```

Business Rule:

```text
Max 1 Failed Attempt / day
Max 3 Failed Attempts / 3 days (Đối với Nhóm 1)
Fast-fail ngay lần đầu tiên đối với Nhóm 2 (RECEIVER_REFUSED, WRONG_ADDRESS)
```

---

# 48. Return Workflow & Xử lý kiện hoàn

```text
RETURN_TO_SENDER
      ↓
Destination Delivery Hub
      ↓
Regional Hub
      ↓
SOC
      ↓
Origin Region
      ↓
Origin Delivery Hub
      ↓
RETURN_TO_SENDER (kiện đã về Origin Hub, chờ Sender nhận)
      ↓
Notify Sender (SMS / App)
      │
      ├── HÌNH THỨC 1: SENDER ĐẾN HUB NHẬN (SELF_PICKUP)
      │      ↓
      │   Sender xuất trình CMND/CCCD hoặc OTP
      │      ↓
      │   Nộp Return Fee (nếu chưa thanh toán)
      │      ↓
      │   RETURNED_TO_SENDER
      │
      ├── HÌNH THỨC 2: SHIPPER PHÁT TRẢ TẬN NHÀ (RETURN_DELIVERY)
      │      ↓
      │   Coordinator gán Shipper (Task Assignment status = ASSIGNED + direction=RETURN)
      │      ↓
      │   Shipper xuất kho (OUT_FOR_DELIVERY + direction=RETURN)
      │      ↓
      │   Thu Return Fee nếu Cash
      │      ↓
      │   Bàn giao Sender → RETURNED_TO_SENDER
      │
      └── HÌNH THỨC 3: SENDER TỪ CHỐI / QUÁ HẠN NHẬN (GRACE PERIOD TIMEOUT)
             ↓
          Quá 7 ngày (hoặc 2 phút Demo) không nhận / từ chối nộp Return Fee
             ↓
          Giữ status = RETURN_TO_SENDER (quá hạn nhận, chờ Manager xử lý)
             ↓
          Hub Manager lập biên bản tồn kho quá hạn và phê duyệt thanh lý / tiêu hủy
             ↓
          CLOSED_EXCEPTION (closure_reason = DISPOSED) (Tiêu hủy / Thanh lý sung quỹ bù cước)
```

---

# 49. Payment – Các dòng tiền

Tách riêng:

```text
SHIPPING FEE
COD
RETURN FEE
E-WALLET
```

---

# 50. Shipping Fee Payer

```text
shipping_fee_payer =
SENDER
RECEIVER
```

---

# 51. Sender trả phí Online

```text
CREATE SHIPMENT
      ↓
CALCULATE FEE
      ↓
VNPAY
      ↓
PENDING
      ↓
CALLBACK / IPN
   │          │
 SUCCESS    FAILED
   │          │
   ↓          ↓
 PAID       FAILED
              ↓
          RETRY
```

---

# 52. Sender trả Cash – HOME_PICKUP

```text
Shipper tới lấy
      ↓
Sender giao kiện
      ↓
Shipper thu Shipping Fee
      ↓
Payment = PAID
      ↓
PICKED_UP
```

Nếu không lấy được:

```text
Không thu tiền
```

---

# 53. Sender trả Cash – SELF_DROPOFF

```text
Sender tới Hub
      ↓
Hub Staff nhận kiện
      ↓
Thu Shipping Fee
      ↓
Payment = PAID
      ↓
AT_HUB
```

---

# 54. Receiver trả Shipping Fee

Không thu trước.

Khi giao:

```text
Amount To Collect =
shipping_fee
+
cod_amount
```

nếu có COD.

---

# 55. Prepaid nhưng Pickup thất bại

Ví dụ:

```text
Shipping Fee = 45.000
Payment = PAID
Pickup Method = HOME_PICKUP
```

Sau 48h không liên lạc được Sender:

```text
CANCELLED (reason = PICKUP_TIMEOUT)
      ↓
Kiện chưa PICKED_UP
      ↓
REFUND 100%
      ↓
CREDIT Sender.e_wallet
      ↓
Payment = REFUNDED
```

---

# 56. Prepaid nhưng Self Dropoff quá hạn

```text
Payment = PAID
      ↓
WAITING_FOR_HANDOVER
      ↓
Quá 48h
      ↓
CANCELLED (reason = DROPOFF_TIMEOUT)
      ↓
REFUND 100%
      ↓
CREDIT e_wallet
```

---

# 57. Sender tự hủy trước Pickup

Nếu Payment chưa thu:

```text
CANCELLED
↓
Không Refund
```

Nếu đã Paid:

```text
CANCELLED
↓
Refund 100%
↓
e_wallet
```

---

# 58. Sau Pickup Sender muốn hủy

```text
PICKED_UP
↓
RETURN_REQUESTED
↓
RETURN_TO_SENDER
```

Tài chính:

```text
Shipping Fee chiều đi = không hoàn
COD = CANCELLED nếu chưa thu
Return Fee = Sender chịu
```

---

# 59. Giao thành công – Không COD

Nếu Sender đã trả phí:

```text
DELIVERED
↓
FINANCIAL COMPLETE
```

Nếu Receiver trả phí:

```text
Receiver trả Shipping Fee
↓
Shipper thu
↓
DELIVERED
↓
WAITING_SHIPPER_RECONCILIATION
```

---

# 60. Giao thành công – Có COD

Ví dụ:

```text
Shipping Fee = 35.000
COD = 500.000
Sender đã trả Shipping Fee
```

Receiver trả:

```text
500.000
```

Workflow:

```text
Shipper Collect COD
      ↓
COD = COLLECTED
      ↓
DELIVERED
      ↓
WAITING_SHIPPER_RECONCILIATION
```

---

# 61. Receiver trả Shipping Fee + COD

Ví dụ:

```text
Shipping Fee = 35.000
COD = 500.000
```

Receiver trả:

```text
535.000
```

Shipper app phải hiện:

```text
Shipping Fee: 35.000
COD:         500.000
Total:       535.000
```

---

# 62. Receiver từ chối COD

```text
OUT_FOR_DELIVERY
      ↓
Receiver không trả COD
      ↓
Không giao kiện
      ↓
DELIVERY_ATTEMPT_FAILED
```

Nếu hết 3 lần:

```text
RETURN_TO_SENDER
COD = CANCELLED
```

---

# 63. Receiver không đủ tiền

```text
DELIVERY_ATTEMPT_FAILED
reason = PAYMENT_NOT_AVAILABLE
```

Nếu đủ 3 lần:

```text
RETURN_TO_SENDER
```

---

# 64. Return Fee

Nếu hàng đã vào mạng vận chuyển và giao thất bại:

```text
Shipping Fee chiều đi
+
Return Fee
```

Ví dụ:

```text
Shipping Fee = 35.000
Return Fee = 25.000
```

Nếu Sender đã trả chiều đi:

```text
Sender chỉ còn trả Return Fee = 25.000
```

Nếu Receiver lẽ ra trả chiều đi nhưng không nhận:

```text
Sender phải trả:
35.000 + 25.000 = 60.000
```

---

# 65. Thanh toán Return Fee & Xử lý nợ cước

```text
RETURN_TO_SENDER (kiện đã về Origin Hub, chờ Sender nhận)
      ↓
Return Fee Paid?
   │          │
  YES         NO
   │          │
   ↓          ↓
Sẵn sàng    Notify Sender
bàn giao      │
   │          ├── Sender thanh toán (VNPay / e_wallet / Cash tại Hub hoặc nộp cho Shipper)
   │          │      ↓
   │          │   Return Fee = PAID
   │          │      ↓
   │          └── Sender từ chối thanh toán / Quá thời hạn Grace Period
   │                 ↓
   │              Giữ status = RETURN_TO_SENDER
   │                 ↓
   │              Không hoàn trả kiện hàng, chờ Manager phê duyệt xử lý tồn kho
   │                 ↓
   │              CLOSED_EXCEPTION (closure_reason = DISPOSED)
   ↓
RETURNED_TO_SENDER (Nếu đã hoàn tất phí)
```

---

# 66. Shipper Reconciliation (Đối soát ca chi tiết)

Cuối ca Shipper nộp:

```text
COD
+
Shipping Fee đã thu Cash
```

Manager mở màn hình đối soát:

```text
Manager Open Reconciliation
      ↓
System hiển thị:
├── Tổng tiền kỳ vọng (Expected Amount)
└── Danh sách chi tiết từng đơn trong ca (Reconciliation Items)
      ↓
Manager nhập số tiền thực tế nhận (Actual Amount)
```

---

# 67. Reconciliation khớp đủ (Expected == Actual)

```text
Expected = Actual
      ↓
Toàn bộ Items trong ca được đánh dấu: MATCHED
      ↓
SHIPPER_RECONCILED (Ca đối soát thành công)
      ↓
Toàn bộ COD transactions trong ca = RECONCILED
      ↓
READY_FOR_PAYOUT (Sẵn sàng giải ngân vào ví Sender)
```

---

# 68. Reconciliation lệch tiền – Tách luồng xử lý chi tiết (Expected != Actual)

```text
Expected != Actual (vd: nộp thiếu 20.000đ tiền lẻ)
      ↓
Manager đối soát chi tiết từng đơn (Checklist từng mã vận đơn)
      │
      ├── CÁC ĐƠN ĐÃ THU ĐỦ TIỀN (Hợp lệ):
      │      ↓
      │   Item status = MATCHED
      │      ↓
      │   COD = RECONCILED → READY_FOR_PAYOUT NGAY LẬP TỨC
      │      ↓
      │   Người gửi vô tội nhận đủ tiền COD vào Ví, không bị giam vốn.
      │
      └── ĐƠN BỊ LỆCH TIỀN / THIẾU TIỀN:
             ↓
          Item status = DISCREPANCY
             ↓
          Tạo FINANCIAL INCIDENT (type = MONEY_SHORTAGE hoặc MONEY_OVERAGE)
             ↓
          Ghi nhận công nợ cá nhân của Shipper (shipper_debt)
             ↓
          Riêng đơn này: COD status = HOLD_DISPUTE
             ↓
          Ca nộp tiền: RECONCILIATION_COMPLETED_WITH_DISCREPANCY
```

---

# 69. COD Payout

```text
READY_FOR_PAYOUT
      ↓
System lấy cod_amount
      ↓
CREDIT Sender.e_wallet
      ↓
COD = PAID_TO_SENDER
      ↓
cod_transactions.payout_at = CURRENT_TIMESTAMP
```

---

# 70. Withdrawal & Cơ chế Khóa số dư (Anti-Double-Spending)

```text
USER GỬI YÊU CẦU RÚT TIỀN (Nhập số tiền + Thông tin Ngân hàng thụ hưởng)
      ↓
System kiểm tra số dư: amount <= available_balance?
   │                   │
  YES                  NO
   │                   │
   ↓                   ↓
KHÓA SỐ DƯ TỨC THÌ:     BÁO LỖI: Không đủ số dư khả dụng
available_balance -= amount
locked_balance += amount
Tạo withdrawal_request (status = PENDING)
Ghi nhận: wallet_transactions (type = WITHDRAWAL_LOCK)
      ↓
ADMIN REVIEW
   │
   ├── ADMIN APPROVE (Chấp thuận rút tiền):
   │      ↓
   │   locked_balance -= amount
   │      ↓
   │   status = COMPLETED
   │      ↓
   │   Ghi nhận: wallet_transactions (type = WITHDRAWAL_DEDUCT)
   │
   └── ADMIN REJECT (Từ chối, vd: Sai số tài khoản / Tên không khớp):
          ↓
       Nhập lý do từ chối (rejection_reason)
          ↓
       locked_balance -= amount
       available_balance += amount (HOÀN TIỀN LẠI VÍ KHẢ DỤNG)
          ↓
       status = REJECTED
          ↓
       Ghi nhận: wallet_transactions (type = WITHDRAWAL_REFUND)
```

Bản demo không cần chuyển khoản ngân hàng thật qua Open Banking API, Admin bấm duyệt để hoàn tất nghiệp vụ.

---

# 71. E-Wallet Architecture

Số dư ví được quản lý theo 2 trường:

```text
users.e_wallet        → Số dư khả dụng (available_balance) dùng để tạo đơn, rút tiền
users.locked_balance  → Số dư đang bị đóng băng chờ Admin duyệt rút tiền
```

Ví dùng cho các nghiệp vụ:

```text
COD Payout (Cộng available_balance)
Refund Shipping Fee / Bồi thường (Cộng available_balance)
Pay Return Fee / Weight Surcharge (Trừ available_balance)
Withdrawal (Khóa sang locked_balance → Trừ đứt khi duyệt / Hoàn lại khi từ chối)
```

---

# 72. Payment Status

```text
UNPAID
PENDING
PAID
FAILED
REFUNDED
```

---

# 73. COD Status

```text
NOT_APPLICABLE
WAITING_COLLECTION
COLLECTED
RECONCILED
HOLD_DISPUTE
READY_FOR_PAYOUT
PAID_TO_SENDER
CANCELLED
```

---

# 74. Shipment Status (Chuẩn hóa State Machine)

Hệ thống sử dụng **13 ShipmentStatus duy nhất**. Các hoạt động vận hành chi tiết không tạo thành trạng thái mới mà ghi nhận bằng `shipment_events`. Ví dụ: `ARRIVED_AT_HUB` là `event_type`, còn trạng thái lưu trong `shipments.current_status` là `AT_HUB`.

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

## Nguyên tắc thiết kế

Không dùng State để mô tả:

```text
SORTING
SORTED
PACKED
READY_FOR_OUTBOUND
LAST_MILE_SORTING
```

Các thao tác này được lưu:

```text
shipment_events
+
event_type
+
actor_user_id
+
hub_id
+
timestamp
```

Ví dụ:

```text
Shipment.status = AT_HUB

Events:
- INBOUND_SCAN
- SORTING_COMPLETED
- BAG_CREATED
- OUTBOUND_SCANNED
```

## Direction cho chiều vận chuyển

Không tạo các enum riêng:

```text
RETURN_IN_TRANSIT
RETURN_ASSIGNED
RETURN_OUT_FOR_DELIVERY
```

Luồng hoàn sử dụng kết hợp:

```text
direction = RETURN
+
IN_TRANSIT
+
OUT_FOR_DELIVERY
```

## Lý do hủy / đóng ngoại lệ

Không tạo nhiều trạng thái:

```text
CANCELLED (reason = SENDER_REQUEST)
CANCELLED (reason = DROPOFF_TIMEOUT)
CANCELLED (reason = PICKUP_TIMEOUT)
...
```

Sử dụng:

```text
status = CANCELLED

cancel_reason =
- SENDER_REQUEST
- PAYMENT_TIMEOUT
- PICKUP_TIMEOUT
- WEIGHT_DISCREPANCY
```

Sự cố nghiêm trọng:

```text
status = CLOSED_EXCEPTION

closure_reason =
- LOST
- DISPOSED
```

# 75. Management Reporting

Hub Staff không báo trực tiếp Admin.

```text
HUB STAFF
   ↓
HUB MANAGER L1
   ↓
REGIONAL MANAGER L2
   ↓
SOC MANAGER L3
   ↓
ADMIN
```

---

# 76. Daily Hub Report

```text
Hub
Date
Inbound Count
Outbound Count
Delivered
Failed
Backlog
Missing
Damaged
Delayed Trips
Notes
Action
```

---

# 77. WebSocket Events

```text
PICKUP TASK ASSIGNED
PICKED_UP
ARRIVED_AT_HUB
HANDOVER_ACCEPTED
TRIP_STARTED
ARRIVED_AT_SOC
OUT_FOR_DELIVERY
DELIVERY_FAILED
DELIVERED
RETURN_STARTED
REFUND_COMPLETED
COD_PAYOUT_COMPLETED
INCIDENT_CREATED
REPORT_SUBMITTED
```

`DELIVERY_FAILED` chỉ là tên event STOMP. Chi tiết nguyên nhân được gửi trong payload, ví dụ `fail_reason = DELIVERY_ATTEMPTS_EXHAUSTED`, `RECEIVER_REFUSED` hoặc `WRONG_ADDRESS`.

---

# 78. Public Tracking

Không login vẫn xem:

```text
Tracking Number
Current Status
Current Hub
Timeline
Estimated Delivery
```

Không public:

```text
SĐT đầy đủ
Địa chỉ đầy đủ
Employee ID
Driver Info
Internal Incident
Internal Finance
Audit Log
```

---

# 79. Security URL Matrix

Hệ thống sử dụng hai lớp Security độc lập:

## 1. Web Layer – JSP/JSTL

Cơ chế:

```text
Spring Security Session
+
JSESSIONID Cookie
```

Dùng cho giao diện SSR:

URL             Role
---
`/guest/**`     permitAll `/auth/**`      permitAll `/ws/**`        permitAll `/user/**`      USER `/hub/**`       HUB_STAFF `/driver/**`    LINEHAUL_DRIVER `/manager/**`   MANAGER `/admin/**`     ADMIN

## 2. REST API Layer – JWT

Cơ chế:

```text
Authorization: Bearer <JWT>
```

Dùng cho thao tác hiện trường:

URL                  Role
---
`/api/auth/**`       permitAll `/api/tracking/**`   permitAll (Public Tracking API) `/api/shipper/**`    SHIPPER `/api/driver/**`     LINEHAUL_DRIVER

Spring Security phải cấu hình:

```text
SecurityFilterChain API
@Order(1)
securityMatcher("/api/**")
SessionCreationPolicy.STATELESS

Authorization rules:
- /api/auth/** -> permitAll
- /api/tracking/** -> permitAll
- Các endpoint /api/** còn lại -> yêu cầu Bearer JWT theo Role


SecurityFilterChain WEB
@Order(2)
Session Cookie
formLogin()
```

Service Layer vẫn bắt buộc kiểm tra:

```text
ROLE
+
HUB SCOPE
+
TASK ASSIGNMENT
+
CURRENT LOCATION
```

# 80. Database – users

```text
users
--------------------------------
id
username
password
full_name
phone UNIQUE
email UNIQUE

role
status
created_at
```

Bảng ví điện tử (chỉ tạo cho những user cần thiết như Người gửi hàng để nhận COD):

```text
wallets
--------------------------------
id
user_id UNIQUE
balance
locked_balance
status
created_at
```

Role:

```text
ROLE_USER
ROLE_SHIPPER
ROLE_HUB_STAFF
ROLE_LINEHAUL_DRIVER
ROLE_MANAGER
ROLE_ADMIN
```

Status:
- `ACTIVE`
- `INACTIVE`
- `LOCKED`

---

# 81. Database – hubs

```text
hubs
--------------------------------
id
code
name

level
type

parent_hub_id

region
province
district
address

status
```

Type:

```text
DELIVERY_HUB
REGIONAL_HUB
SOC
```

Status: - `ACTIVE` - `INACTIVE`

---

# 82. Database – employee_profiles

Dùng cho TẤT CẢ nhân viên nội bộ (kể cả Shipper và Driver để có Base Hub):

```text
employee_profiles
--------------------------------
id
user_id

employee_code
hub_id

position

status
```

Position:

```text
INBOUND
SORTER
OUTBOUND
COORDINATOR
GENERAL_STAFF
PICKUP_SHIPPER
DELIVERY_SHIPPER
LINEHAUL_DRIVER
```

Status: - `ACTIVE` - `ON_LEAVE` - `RESIGNED`

Manager:

```text
position có thể NULL
```

cấp Manager lấy từ:

```text
hub.level
```

---

# 83. Database – shipments

```text
shipments
--------------------------------
id

tracking_number

sender_user_id
receiver_user_id NULL

sender_name
sender_phone
sender_address

receiver_name
receiver_phone
receiver_address
receiver_province
receiver_district

pickup_method
fee_payer
inspection_policy

weight
length
width
height

declared_value

shipping_fee
cod_amount
return_fee

payment_status

origin_hub_id
destination_hub_id
current_hub_id

direction

current_status
on_hold BOOLEAN DEFAULT FALSE

fail_reason
cancel_reason
closure_reason

payment_deadline

created_at
picked_up_at
delivered_at
returned_at
```

Direction:

```text
FORWARD
RETURN
```

Fail Reason (`fail_reason`) lưu lý do thất bại giao hàng gần nhất hoặc lý do kết thúc thất bại:

```text
NO_ANSWER
PAYMENT_NOT_AVAILABLE
RESCHEDULED
OTHER
DELIVERY_ATTEMPTS_EXHAUSTED
RECEIVER_REFUSED
WRONG_ADDRESS
```

Quy ước:

```text
Lần giao #1 / #2 thất bại tạm thời:
- current_status chưa bắt buộc chuyển sang FAILED
- fail_reason lưu NO_ANSWER / PAYMENT_NOT_AVAILABLE / RESCHEDULED / OTHER

Hết tối đa 3 lần:
- current_status = FAILED
- fail_reason = DELIVERY_ATTEMPTS_EXHAUSTED

Fast-fail:
- current_status = FAILED
- fail_reason = RECEIVER_REFUSED hoặc WRONG_ADDRESS
```

Mục đích:

```text
Không tạo state riêng cho chiều hoàn.
ShipmentStatus + Direction điều khiển workflow.
```

# 84. Database – shipment_routes

```text
shipment_routes
--------------------------------
id
shipment_id
hub_id
sequence_number

status
arrival_time
departure_time
```

Status: - `PENDING` - `IN_TRANSIT` - `COMPLETED` - `SKIPPED`

---

# 85. Database – shipment_events

```text
shipment_events
--------------------------------
id
shipment_id

hub_id
actor_user_id

status
description

created_at
```

Dùng dựng Tracking Timeline.

---

# 86. Database – assignments

Có thể dùng một bảng chung:

```text
shipment_assignments
--------------------------------
id

shipment_id
assignee_user_id

assignment_type
status

assigned_at
accepted_at
completed_at
```

Type:

```text
PICKUP
DELIVERY
RETURN_DELIVERY
```

Status: - `ASSIGNED` - `ACCEPTED` - `REJECTED` - `IN_PROGRESS` - `COMPLETED` - `CANCELLED`

---

# 87. Database – pickup_attempts

```text
pickup_attempts
--------------------------------
id
assignment_id
shipment_id
shipper_id

attempt_number
attempt_time

result
note
```

Result: - `SUCCESS` - `FAILED_NO_ANSWER` - `FAILED_SENDER_REFUSED` - `FAILED_CANCELLED` - `FAILED_OTHER`

---

# 88. Database – delivery_attempts

```text
delivery_attempts
--------------------------------
id
assignment_id
shipment_id
shipper_id

attempt_number
attempt_date
attempt_time
rescheduled_date

result
note
```

Result: - `DELIVERED` - `NO_ANSWER` - `RECEIVER_REFUSED` - `PAYMENT_NOT_AVAILABLE` - `WRONG_ADDRESS` - `RESCHEDULED` - `OTHER`

---

# 89. Database – parcel_containers

```text
parcel_containers
--------------------------------
id
container_code

origin_hub_id
destination_hub_id

status
created_by
created_at
```

Status: - `OPEN` - `SEALED` - `LOADED` - `UNPACKED_FULL` - `UNPACKED_WITH_DISCREPANCY`

---

# 90. Database – container_items

```text
container_items
--------------------------------
container_id
shipment_id
status
scanned_at
```

Status: - `IN_CONTAINER` - `RECEIVED` - `MISSING` - `ON_HOLD`

---

# 91. Database – handovers

```text
handovers
--------------------------------
id

from_hub_id
to_hub_id

sender_employee_id
receiver_driver_id

transport_trip_id

expected_containers
accepted_containers

status

created_at
accepted_at
completed_at
```

`from_hub_id` và `to_hub_id` giúp:

```text
Hub Staff lọc bàn giao theo Hub hiện tại
Không cần JOIN vòng qua Trip
```

# 91.1 Database – handover_containers (Chi tiết Bao hàng bàn giao)

```text
handover_containers
--------------------------------
id
handover_id
container_id

status
scanned_at
```

Status: - `LOADED` (Đã chất lên xe) - `DELIVERED` (Đã giao tới Hub đích) - `DISCREPANCY` (Bị thiếu / Sai lệch)

---

# 92. Database – transport_trips

```text
transport_trips
--------------------------------
id
trip_code

origin_hub_id
destination_hub_id

driver_user_id
vehicle_id

departure_time
arrival_time

status
```

Status: - `SCHEDULED` - `DEPARTED` - `IN_TRANSIT` - `ARRIVED` - `COMPLETED` - `COMPLETED_WITH_DISCREPANCY` - `DELAYED` - `CANCELLED`

---

# 93. Database – vehicles

```text
vehicles
--------------------------------
id
plate_number
vehicle_type
capacity
status
```

Status: - `AVAILABLE` - `IN_USE` - `MAINTENANCE` - `INACTIVE`

---

# 94. Database – payments

```text
payments
--------------------------------
id
shipment_id

payer_type
payment_type

amount
method

status
transaction_ref

paid_at
refunded_at
created_at
```

Payment Type:

```text
SHIPPING_FEE
RETURN_FEE
WEIGHT_SURCHARGE
```

Method:

```text
CASH
VNPAY
E_WALLET
```

---

# 95. Database – cod_transactions

```text
cod_transactions
--------------------------------
id

shipment_id

collected_by_shipper_id

amount
method (CASH, VNPAY, TRANSFER)

status

collected_at
reconciled_at
payout_at TIMESTAMP NULL
```

`collected_by_shipper_id` bắt buộc để:

```text
Manager đối soát cuối ca
Biết Shipper nào thu khoản COD nào
Tạo bảng kê nộp tiền
```

# 96. Database – shipper_reconciliations & Items

```text
shipper_reconciliations
--------------------------------
id

shipper_id
manager_id

reconciliation_date

expected_amount
actual_amount
difference_amount

status
created_at
completed_at
```

Status: - `PENDING` - `COMPLETED` - `COMPLETED_WITH_DISCREPANCY`

Bảng chi tiết từng đơn đối soát trong ca:

```text
shipper_reconciliation_items
--------------------------------
id
reconciliation_id
shipment_id

expected_amount
actual_amount
difference_amount

status
note
```

Status Items: - `MATCHED` - `SHORTAGE` - `OVERAGE`

---

# 97. Database – withdrawal_requests

```text
withdrawal_requests
--------------------------------
id
user_id

amount
bank_name
account_number
account_holder_name

status
rejection_reason

requested_at
approved_at
rejected_at
completed_at
```

Status: - `PENDING` - `APPROVED` - `REJECTED` - `COMPLETED`

---

# 97.1 Database – wallet_transactions (Sổ cái ví điện tử)

```text
wallet_transactions
--------------------------------
id
user_id

transaction_type
amount
balance_before
balance_after

reference_id
reference_type
description

created_at
```

Transaction Types: - `COD_PAYOUT` (+) - `SHIPPING_FEE_PAYMENT` (-) - `RETURN_FEE_PAYMENT` (-) - `WEIGHT_SURCHARGE_PAYMENT` (-) - `REFUND` (+) - `WITHDRAWAL_LOCK` (Khóa số dư sang locked_balance) - `WITHDRAWAL_DEDUCT` (Trừ hoàn toàn khi Admin duyệt chuyển khoản) - `WITHDRAWAL_REFUND` (Hoàn tiền về ví khả dụng khi Admin từ chối) - `COMPENSATION` (+)

Reference Types: - `SHIPMENT` - `WITHDRAWAL` - `INCIDENT`

---

# 98. Database – incidents

```text
incidents
--------------------------------
id

shipment_id
trip_id
hub_id

type
description

reported_by

status

created_at
resolved_at
```

Type:

```text
MISSING_PARCEL
DAMAGED_PARCEL
WRONG_ROUTE
TRIP_DELAY
MONEY_SHORTAGE
MONEY_OVERAGE
OTHER
```

Status: - `OPEN` - `INVESTIGATING` - `RESOLVED` - `CLOSED`

---

# 99. Database – operation_reports

```text
operation_reports
--------------------------------
id

hub_id
manager_user_id

report_date

inbound_count
outbound_count
delivered_count
failed_count
backlog_count
missing_count
damaged_count

problem
action

status
created_at
```

Status: - `DRAFT` - `SUBMITTED` - `ACKNOWLEDGED`

---

# 99.1 Database – pricing_policies (Cấu hình Biểu phí tính cước)

```text
pricing_policies
--------------------------------
id
service_code

from_region
to_region

base_weight_g
base_price

extra_weight_step_g
extra_weight_price

insurance_rate_percent
cod_fee_percent

status
created_at
updated_at
```

Status: - `ACTIVE` - `INACTIVE`

Service Code: - `STANDARD` (Giao tiêu chuẩn) - `FAST` (Giao nhanh) - `EXPRESS` (Hỏa tốc)

Region: - `NORTH` (Miền Bắc) - `CENTRAL` (Miền Trung / Tây Nguyên) - `SOUTH` (Miền Nam)

---

# 99.2 Design Philosophy & Notes for VibeCoders

> **WARNING FOR AI ASSISTANTS / VIBECODERS:** Do NOT attempt to "fix" or normalize the following design choices. They are deliberate trade-offs for performance and business logic in a distributed logistics system.

1. **Deliberate 3NF Violations (Data Snapshotting):**
   The `shipments` table stores `sender_name`, `receiver_name`, `address` even though we have `sender_user_id`. **DO NOT NORMALIZE THIS.** Shipments are legal contracts. If a user updates their profile 6 months later, historical shipment documents must remain unchanged.
2. **Polymorphic Relationships:**
   The `wallet_transactions` table uses `reference_id` and `reference_type` (`SHIPMENT`, `WITHDRAWAL`, `INCIDENT`). **DO NOT** try to replace this with explicit foreign keys (like `shipment_id`, `withdrawal_id`) with NULLs, as it breaks scalability when new transaction sources are added.
3. **Surrogate Keys for Events:**
   Tables like `handovers` represent atomic events. Do not attempt to decompose them to achieve 4NF/5NF.
4. **Denormalized Hub Locations:**
   `hubs` stores `province`, `district`, `region` directly. Do not create separate `provinces` or `districts` tables to save space. The flat structure reduces excessive JOINs during heavy queries like routing.
5. **Bagging Concept (Handovers):**
   `handovers` (Linehaul) tracks `expected_containers`, NOT individual parcels. Parcels are tracked when they are put into bags (`parcel_containers` + `container_items`). Linehaul drivers only scan Bags/Containers.

# 100. Scheduler

Production:

```text
SELF_DROPOFF timeout = 48h
HOME_PICKUP timeout = 48h

Delivery:
Max 1 failed attempt/day
Max 3 failed attempts/3 days
```

---

# 101. Demo Mode

Không thể chờ thật khi bảo vệ.

Ví dụ profile Demo:

```properties
app.scheduler.timeout-check-interval-ms=30000

# Rút ngắn các bộ đếm thời gian
app.logistics.timeout.dropoff-expiration-minutes=2
app.logistics.timeout.pickup-expiration-minutes=2
app.logistics.return.grace-period-minutes=2

app.logistics.delivery.max-attempts=3
app.demo.delivery-attempt-day-lock=false

# Fast-track Demo Routing (Cho phép chạy mô phỏng trung chuyển liên Hub nhanh)
app.demo.fast-track-route-bypass=true
app.demo.auto-linehaul-transit-simulation=true
```

Production profile:

```properties
app.logistics.timeout.dropoff-expiration-hours=48
app.logistics.timeout.pickup-expiration-hours=48
app.logistics.return.grace-period-days=7
app.logistics.delivery.max-attempts=3
app.demo.delivery-attempt-day-lock=true
app.demo.fast-track-route-bypass=false
```

---

# 102. Master Workflow – Toàn hệ thống

```text
                               USER / SENDER
                                    │
                                    ↓
                             CREATE SHIPMENT
                                    │
                                    ↓
                       AUTO-LINK RECEIVER BY PHONE
                                    │
                                    ↓
                            CALCULATE SHIPPING FEE
                                    │
                                    ↓
                              CHỌN FEE PAYER
                            /               \
                        SENDER             RECEIVER
                          │                   │
                    PAYMENT FLOW             │
                          \___________________/
                                    │
                                    ↓
                             PICKUP METHOD
                          /                 \
                  HOME_PICKUP             SELF_DROPOFF
                       │                       │
             ROLE_SHIPPER PICKUP          Sender tới Hub
                       │                       │
                       └───────────┬───────────┘
                                   ↓
                           ORIGIN DELIVERY HUB
                                   │
                           ROLE_HUB_STAFF
                      INBOUND → SORTER → OUTBOUND
                                   │
                                   ↓
                           BAG / CONTAINER
                                   │
                                   ↓
                           HANDOVER STAFF A
                                   │
                                   ↓
                      ROLE_LINEHAUL_DRIVER B
                                   │
                                   ↓
                         REGIONAL HUB LEVEL 2
                                   │
                           ROLE_HUB_STAFF
                                   │
                                   ↓
                             ROUTE PLANNER
                          /                 \
                   LONG AN SOC         BINH DUONG SOC
                    Miền Tây           Trung/Bắc/TN
                          \                 /
                           \_______________/
                                   │
                                   ↓
                         DESTINATION REGION
                                   │
                                   ↓
                         REGIONAL HUB LEVEL 2
                                   │
                                   ↓
                       DESTINATION DELIVERY HUB
                                   │
                           ROLE_HUB_STAFF
                          LAST-MILE SORTING
                                   │
                                   ↓
                            ROLE_SHIPPER
                                   │
                                   ↓
                          OUT_FOR_DELIVERY
                           /               \
                      SUCCESS              FAILED
                        │                    │
                        ↓                    ↓
                  COLLECT MONEY       ATTEMPT 1/2/3
                        │                    │
                        ↓                    ↓
                    DELIVERED          FAILED (fail_reason = DELIVERY_ATTEMPTS_EXHAUSTED)
                        │                    │
                        ↓                    ↓
                  RECONCILIATION       RETURN_TO_SENDER
                        │                    │
                        ↓                    ↓
                   COD PAYOUT          RETURN LINEHAUL
                        │                    │
                        ↓                    ↓
                  SENDER E-WALLET      ORIGIN HUB
                                             │
                                             ↓
                                      PAY RETURN FEE
                                             │
                                             ↓
                                     RETURNED_TO_SENDER
```

---

# 103. Master Payment Workflow

```text
                     SHIPMENT CREATED
                           │
                           ↓
                      SHIPPING FEE
                           │
                 ┌─────────┴─────────┐
                 │                   │
              SENDER              RECEIVER
                 │                   │
        ┌────────┴────────┐          │
        │                 │          │
      VNPAY             CASH         │
        │                 │          │
      PAID        Pickup / Hub       │
        │                 │          │
        └──────────┬──────┘          │
                   │                 │
                   ↓                 │
               LOGISTICS             │
                   │                 │
                   ↓                 │
             OUT_FOR_DELIVERY ◄──────┘
                   │
                   ↓
              AMOUNT TO COLLECT
                   │
         ┌─────────┴──────────┐
         │                    │
Sender paid fee        Receiver pays fee
         │                    │
  Collect COD       Collect COD + Fee
         │                    │
         └──────────┬─────────┘
                    ↓
                DELIVERED
                    │
                    ↓
             SHIPPER HOLDS CASH
                    │
                    ↓
             MANAGER RECONCILES
                    │
             ┌──────┴──────┐
             │             │
            OK          MISMATCH
             │             │
             ↓             ↓
      COD RECONCILED     INCIDENT
             │
             ↓
      READY_FOR_PAYOUT
             │
             ↓
      CREDIT E-WALLET
             │
             ↓
      PAID_TO_SENDER
```

---

# 104. Master Cancellation & Refund Workflow

```text
                       BEFORE UTEEXPRESS ACCEPTS PARCEL
                                      │
                 ┌────────────────────┼────────────────────┐
                 │                    │                    │
          Sender Cancel        Dropoff > 48h       Pickup Fail > 48h
                 │                    │                    │
                 └────────────────────┼────────────────────┘
                                      ↓
                                   CANCEL
                                      │
                              PAYMENT ALREADY PAID?
                               │               │
                              NO              YES
                               │               │
                               ↓               ↓
                           FINISH         REFUND 100%
                                               │
                                               ↓
                                        CREDIT E-WALLET
```

---

# 105. Master Return Workflow

```text
                      PARCEL ALREADY ACCEPTED
                                │
                                ↓
                     DELIVERY / CANCEL REQUEST
                                │
                                ↓
                          RETURN_TO_SENDER
                                │
                                ↓
                       COD = CANCELLED
                                │
                                ↓
                     CALCULATE RETURN FEE
                                │
                                ↓
                  REVERSE LOGISTICS THROUGH HUB
                                │
                                ↓
                      RETURN_TO_SENDER
                   (Đã về Origin Hub, chờ Sender nhận)
                                │
                 ┌──────────────┴──────────────┐
                 │                             │
        Sender pays Return Fee        Refuse / Timeout (> Grace Period)
                 │                             │
                 ↓                             ↓
         RETURNED_TO_SENDER             Giữ RETURN_TO_SENDER
   (Tại Hub hoặc Shipper giao trả)             ↓
                                      Manager phê duyệt xử lý tồn kho
                                                ↓
                                  CLOSED_EXCEPTION (closure_reason = DISPOSED)
```

---

# 106. Kịch bản Demo chính

## Tab 1 – USER

```text
Login
↓
Create Shipment Dĩ An → Cần Thơ
↓
COD = 500.000
↓
Sender trả Shipping Fee bằng VNPay
↓
HOME_PICKUP
```

## Tab 2 – SHIPPER

```text
Receive Pickup Assignment
↓
PICKED_UP
↓
Bring to Dĩ An Hub
```

## Tab 3 – HUB_STAFF Dĩ An

```text
Inbound Scan
↓
Sorting
↓
Create Bag
↓
Create Handover
```

## Tab 4 – LINEHAUL_DRIVER

```text
Accept Handover
↓
Start Trip
↓
Arrive Bình Dương Regional Hub / Long An SOC
```

## Tab 5 – HUB_STAFF Destination

```text
Inbound
↓
Sorting
↓
Ready For Delivery
```

## Tab 2 – SHIPPER Last-mile

```text
Receive Delivery Assignment
↓
OUT_FOR_DELIVERY
↓
Collect 500.000 COD
↓
DELIVERED
```

## Tab 6 – MANAGER

```text
Open Shipper Reconciliation
↓
Expected = Actual
↓
Confirm
↓
COD READY_FOR_PAYOUT
```

## Tab 1 – USER

```text
WebSocket notification
↓
COD +500.000 vào e_wallet
↓
Request Withdrawal
```

---

# 107. Kịch bản Demo ngoại lệ

## Case 1 – Self Dropoff quá hạn

```text
Create Shipment
↓
Pay VNPay
↓
SELF_DROPOFF
↓
Timeout Demo
↓
CANCELLED (reason = DROPOFF_TIMEOUT)
↓
Refund 100% e_wallet
```

## Case 2 – Pickup thất bại

```text
Create Shipment
↓
Pay VNPay
↓
HOME_PICKUP
↓
Shipper không liên hệ được Sender
↓
Timeout Demo
↓
CANCELLED (reason = PICKUP_TIMEOUT)
↓
Refund 100%
```

## Case 3 – Delivery thất bại

```text
Attempt #1
↓
Attempt #2
↓
Attempt #3
↓
FAILED (fail_reason = DELIVERY_ATTEMPTS_EXHAUSTED)
↓
RETURN_TO_SENDER
↓
COD CANCELLED
↓
Return Fee
↓
RETURNED_TO_SENDER
```

## Case 4 – Thiếu kiện khi Dỡ bao (Unbagging)

```text
Expected = 50 kiện
Received = 49 kiện (Thiếu UTE043)
↓
DỠ BAO (UNPACK_BAG):
├── 49 kiện hợp lệ: current_status = AT_HUB; event = ARRIVED_AT_HUB → Tiếp tục phân loại ngay
└── 1 kiện thiếu: container_items.status = MISSING; shipment.current_status = IN_TRANSIT; shipment.on_hold = true
↓
INCIDENT (type = MISSING_PARCEL, shipment_id = UTE043)
↓
Handover / Trip = COMPLETED_WITH_DISCREPANCY
↓
Notify Managers & Điều tra tài xế
```

## Case 5 – COD lệch tiền (Đối soát chi tiết)

```text
Expected COD Cash = 500.000
Actual = 450.000 (Thiếu 50.000 tiền lẻ)
↓
Manager đối soát chi tiết từng đơn:
├── Các đơn nộp đủ: COD = RECONCILED → READY_FOR_PAYOUT ngay lập tức
└── Đơn bị thiếu tiền: HOLD_DISPUTE
↓
Tạo FINANCIAL INCIDENT gắn với Shipper
↓
Ghi nhận công nợ Shipper (shipper_debt)
↓
Ca đối soát = COMPLETED_WITH_DISCREPANCY
```

## Case 6 – Kiện hàng hoàn quá hạn / Khách từ bỏ (Abandoned Parcel)

```text
Hàng hoàn về bưu cục gốc: RETURN_TO_SENDER (chờ Sender nhận)
↓
Notify Sender nộp Return Fee để nhận lại hàng
↓
Hết thời hạn Grace Period (2 phút demo / 7 ngày production)
Sender không nhận / từ chối thanh toán
↓
Giữ status = RETURN_TO_SENDER
↓
Manager phê duyệt biên bản xử lý tồn kho / thanh lý / tiêu hủy
↓
CLOSED_EXCEPTION (closure_reason = DISPOSED) (Tiêu hủy / Thanh lý sung quỹ bù cước)
```

## Case 7 – Khách từ chối nhận dứt khoát / Bom hàng (Fast-Fail)

```text
Shipper giao hàng: RECEIVER_REFUSED
↓
Shipper chụp ảnh bằng chứng + chọn lý do Bom hàng
↓
Hệ thống KÍCH HOẠT FAST-FAIL:
Không bắt phát lại Lần 2 & 3
↓
Thông báo xác nhận đến Sender qua SMS/App
↓
FAILED (fail_reason = RECEIVER_REFUSED) ngay trong ngày
↓
RETURN_TO_SENDER (Chuyển hoàn ngay lập tức)
```

## Case 8 – Kiện hàng mất tích và Bồi thường ví điện tử (Compensation)

```text
Kiện hàng: current_status = IN_TRANSIT, on_hold = true sau 3 ngày điều tra
↓
Hội đồng điều tra xác nhận mất hàng
↓
Vận đơn chuyển trạng thái DUY NHẤT:
CLOSED_EXCEPTION (closure_reason = LOST)
↓
Hệ thống tự động tính:
Compensation = declared_value (500.000) + shipping_fee (35.000) = 535.000
↓
CREDIT Ví điện tử Sender: users.e_wallet += 535.000
↓
Ghi nhận: wallet_transactions (type = COMPENSATION)
↓
incidents.status = RESOLVED
↓
Giữ nguyên vận đơn ở CLOSED_EXCEPTION (closure_reason = LOST)
```

---

# 108. Các phần không cần làm ở Phase 1

Để nhóm 3 người không bị quá tải:

```text
VNPay Refund API thật
Bank Payout thật
Double-entry Accounting Ledger
AI Route Optimization
Realtime GPS xe tải
Insurance Compensation
Chargeback
Fraud Detection nâng cao
OCR Barcode Camera phức tạp
```

Có thể ghi:

```text
Future Development
```

---

# 109. Kết luận mô hình chốt

UTEExpress sử dụng:

```text
6 SECURITY ROLE

USER
SHIPPER
HUB_STAFF
LINEHAUL_DRIVER
MANAGER
ADMIN
```

Trong đó:

```text
HUB_STAFF.position
=
INBOUND / SORTER / OUTBOUND / COORDINATOR
```

và:

```text
MANAGER.hub_id
+
HUB.level
=
Hub Manager / Regional Manager / SOC Manager
```

Mô hình này giữ được:

```text
RBAC đơn giản
+
Data Scoping rõ
+
Phân tách trách nhiệm
+
Workflow logistics thực tế
+
Audit Handover rõ
+
Payment/COD đầy đủ
+
Demo được trong thời gian ngắn
```

Đây là phương án đề xuất để dùng làm **workflow chính thức** trước khi bắt đầu ERD, Use Case, Database và code.
