/* Demo-only operating data: shipments in every state, payments, COD, wallets, withdrawals, linehaul trips.
   Runs after the demo accounts (R__demo_b). Inserts once, keyed on tracking number UTXDEMO0001; to rebuild, reset the database.
   Everything is relative to "now", so a fresh load always looks current. */
IF NOT EXISTS (SELECT 1 FROM dbo.shipments WHERE tracking_number = N'UTXDEMO0001')
AND EXISTS (SELECT 1 FROM dbo.users WHERE username = N'demo_sender')
BEGIN
SET XACT_ABORT ON;
BEGIN TRANSACTION;

/* ---------- one more customer, so two people have money and withdrawals ---------- */
INSERT dbo.users (username, password, full_name, phone, email, role, status, phone_verified_at, email_verified_at)
SELECT N'demo_customer', u.password, N'Demo Khách hàng 2', N'+84900000114', N'demo_customer@example.test', N'ROLE_USER', N'ACTIVE', SYSDATETIME(), SYSDATETIME()
FROM dbo.users u WHERE u.username = N'demo_sender' AND NOT EXISTS (SELECT 1 FROM dbo.users x WHERE x.username = N'demo_customer');
INSERT dbo.wallets (user_id, balance, locked_balance, status)
SELECT u.id, 0, 0, N'ACTIVE' FROM dbo.users u WHERE u.username = N'demo_customer' AND NOT EXISTS (SELECT 1 FROM dbo.wallets w WHERE w.user_id = u.id);

DECLARE @sender BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_sender');
DECLARE @cust BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_customer');
DECLARE @recv BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_receiver');
DECLARE @shipHcm BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_shipper_hcm');
DECLARE @shipHn BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_shipper_hn');
DECLARE @drvHcm BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_driver_hcm');
DECLARE @drvHn BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_driver_hn');
DECLARE @hubStaffHcm BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_hub_hcm');
DECLARE @hubStaffHn BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_hub_hn');
DECLARE @manager BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_manager');
DECLARE @admin BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_admin');
DECLARE @hcm BIGINT = (SELECT id FROM dbo.hubs WHERE code = N'LOCAL2025-P79');
DECLARE @hn BIGINT = (SELECT id FROM dbo.hubs WHERE code = N'LOCAL2025-P01');
DECLARE @south BIGINT = (SELECT id FROM dbo.hubs WHERE code = N'LOCAL2025-SOUTH');
DECLARE @north BIGINT = (SELECT id FROM dbo.hubs WHERE code = N'LOCAL2025-NORTH');

/* ---------- shipments ----------
   trk, sender, status, pickup, fee payer, payment, current hub, origin, destination, direction, fee, cod, return fee, hours since created */
DECLARE @s TABLE (trk NVARCHAR(30), sender BIGINT, st NVARCHAR(30), pickup NVARCHAR(20), payer NVARCHAR(10), pay NVARCHAR(20),
                  cur BIGINT, org BIGINT, dst BIGINT, dir NVARCHAR(10), fee DECIMAL(15,2), cod DECIMAL(15,2), retfee DECIMAL(15,2), age INT,
                  recvName NVARCHAR(100), recvAddr NVARCHAR(255), recvProv NVARCHAR(100), why NVARCHAR(255));
INSERT @s VALUES
 (N'UTXDEMO0001', @sender, N'WAITING_FOR_PAYMENT',  N'SELF_DROPOFF', N'SENDER',   N'UNPAID', NULL, @hcm, @hn, N'FORWARD', 45000,  0,       0,     1,  N'Trần Thị Bình',   N'12 Hàng Bài, Hoàn Kiếm, Hà Nội',        N'Hà Nội', NULL),
 (N'UTXDEMO0002', @sender, N'WAITING_FOR_HANDOVER', N'HOME_PICKUP',  N'SENDER',   N'PAID',   NULL, @hcm, @hn, N'FORWARD', 52000,  0,       0,     3,  N'Lê Văn Cường',    N'45 Lý Thường Kiệt, Hoàn Kiếm, Hà Nội', N'Hà Nội', NULL),
 (N'UTXDEMO0003', @sender, N'PICKED_UP',            N'HOME_PICKUP',  N'SENDER',   N'PAID',   NULL, @hcm, @hn, N'FORWARD', 48000,  0,       0,     8,  N'Phạm Thu Dung',   N'9 Nguyễn Trãi, Thanh Xuân, Hà Nội',     N'Hà Nội', NULL),
 (N'UTXDEMO0004', @sender, N'AT_HUB',               N'SELF_DROPOFF', N'SENDER',   N'PAID',   @hcm, @hcm, @hn, N'FORWARD', 50000,  0,       0,     12, N'Đỗ Quang Huy',    N'88 Cầu Giấy, Cầu Giấy, Hà Nội',        N'Hà Nội', NULL),
 (N'UTXDEMO0005', @sender, N'IN_TRANSIT',           N'SELF_DROPOFF', N'SENDER',   N'PAID',   @south, @hcm, @hn, N'FORWARD', 55000, 0,       0,     20, N'Vũ Minh Khang',   N'21 Tây Sơn, Đống Đa, Hà Nội',           N'Hà Nội', NULL),
 (N'UTXDEMO0006', @cust,   N'IN_TRANSIT',           N'HOME_PICKUP',  N'SENDER',   N'PAID',   @south, @hcm, @hn, N'FORWARD', 61000, 150000, 0,     22, N'Ngô Bảo Lâm',     N'5 Kim Mã, Ba Đình, Hà Nội',             N'Hà Nội', NULL),
 (N'UTXDEMO0007', @sender, N'AT_HUB',               N'SELF_DROPOFF', N'SENDER',   N'PAID',   @hn,  @hn, @hcm, N'FORWARD', 58000, 0,       0,     26, N'Hoàng Thị Mai',   N'30 Nguyễn Huệ, Quận 1, TP.HCM',         N'TP. Hồ Chí Minh', NULL),
 (N'UTXDEMO0008', @sender, N'OUT_FOR_DELIVERY',     N'HOME_PICKUP',  N'SENDER',   N'PAID',   @hn,  @hcm, @hn, N'FORWARD', 47000, 300000, 0,     40, N'Đặng Văn Nam',    N'17 Láng Hạ, Đống Đa, Hà Nội',           N'Hà Nội', NULL),
 (N'UTXDEMO0009', @sender, N'DELIVERED',            N'HOME_PICKUP',  N'SENDER',   N'PAID',   @hn,  @hcm, @hn, N'FORWARD', 46000, 800000, 0,     50, N'Bùi Thị Oanh',    N'62 Giải Phóng, Hai Bà Trưng, Hà Nội',   N'Hà Nội', NULL),
 (N'UTXDEMO0010', @sender, N'DELIVERED',            N'SELF_DROPOFF', N'SENDER',   N'PAID',   @hn,  @hcm, @hn, N'FORWARD', 44000, 500000, 0,     120, N'Cao Minh Phúc',  N'3 Phố Huế, Hai Bà Trưng, Hà Nội',       N'Hà Nội', NULL),
 (N'UTXDEMO0011', @sender, N'FAILED',               N'HOME_PICKUP',  N'SENDER',   N'PAID',   @hn,  @hcm, @hn, N'FORWARD', 49000, 250000, 0,     44, N'Lương Quốc Sơn',  N'101 Trường Chinh, Đống Đa, Hà Nội',     N'Hà Nội', N'Người nhận không nghe máy'),
 (N'UTXDEMO0012', @sender, N'RETURN_TO_SENDER',     N'HOME_PICKUP',  N'SENDER',   N'PAID',   @hcm, @hcm, @hn, N'RETURN',  50000,  0,       30000, 160, N'Tô Thị Quyên',    N'14 Bạch Mai, Hai Bà Trưng, Hà Nội',     N'Hà Nội', N'Người nhận từ chối nhận hàng'),
 (N'UTXDEMO0013', @sender, N'RETURNED_TO_SENDER',   N'SELF_DROPOFF', N'SENDER',   N'PAID',   @hcm, @hcm, @hn, N'RETURN',  50000,  0,       30000, 260, N'Mai Văn Rô',      N'77 Đội Cấn, Ba Đình, Hà Nội',           N'Hà Nội', N'Địa chỉ không tồn tại'),
 (N'UTXDEMO0014', @sender, N'CANCELLED',            N'SELF_DROPOFF', N'SENDER',   N'REFUNDED', NULL, @hcm, @hn, N'FORWARD', 45000, 0,       0,     30, N'Kiều Thị Sen',    N'8 Tôn Đức Thắng, Đống Đa, Hà Nội',      N'Hà Nội', NULL),
 (N'UTXDEMO0015', @sender, N'DELIVERED',            N'HOME_PICKUP',  N'SENDER',   N'PAID',   @hn,  @hcm, @hn, N'FORWARD', 52000, 1200000, 0,    200, N'Trương Gia Tuấn', N'26 Hồ Tùng Mậu, Cầu Giấy, Hà Nội',      N'Hà Nội', NULL),
 (N'UTXDEMO0016', @cust,   N'DELIVERED',            N'SELF_DROPOFF', N'SENDER',   N'PAID',   @hn,  @hcm, @hn, N'FORWARD', 53000, 2000000, 0,    210, N'Phan Thị Uyên',   N'4 Hàng Bông, Hoàn Kiếm, Hà Nội',        N'Hà Nội', NULL),
 (N'UTXDEMO0017', @sender, N'AT_HUB',               N'SELF_DROPOFF', N'RECEIVER', N'UNPAID', @hcm, @hn, @hcm, N'FORWARD', 56000, 0,       0,     6,  N'Demo Người nhận', N'123 Lê Lợi, Quận 1, TP.HCM',            N'TP. Hồ Chí Minh', NULL);

INSERT dbo.shipments (tracking_number, sender_user_id, receiver_user_id, sender_name, sender_phone, sender_address,
        receiver_name, receiver_phone, receiver_address, receiver_province, receiver_district, pickup_method, fee_payer, inspection_policy,
        weight, declared_value, shipping_fee, cod_amount, return_fee, payment_status, origin_hub_id, destination_hub_id, current_hub_id,
        direction, current_status, on_hold, fail_reason, cancel_reason, payment_deadline, waiting_for_handover_at, created_at,
        picked_up_at, delivered_at, returned_at, destination_region, destination_province, service_code)
SELECT s.trk, s.sender, CASE WHEN s.trk = N'UTXDEMO0017' THEN @recv END,
       us.full_name, us.phone, N'25 Nguyễn Văn Cừ, Quận 5, TP.HCM',
       s.recvName, N'+8490000' + RIGHT(s.trk, 4), s.recvAddr, s.recvProv, N'',
       s.pickup, s.payer, N'INSPECTION_ONLY', 1500, s.cod + 500000, s.fee, s.cod, s.retfee, s.pay, s.org, s.dst, s.cur,
       s.dir, s.st, 0, CASE WHEN s.st IN (N'FAILED', N'RETURN_TO_SENDER', N'RETURNED_TO_SENDER') THEN s.why END,
       CASE WHEN s.st = N'CANCELLED' THEN N'Người gửi hủy đơn' END,
       CASE WHEN s.st = N'WAITING_FOR_PAYMENT' THEN DATEADD(HOUR, 23, SYSDATETIME()) END,
       CASE WHEN s.st <> N'WAITING_FOR_PAYMENT' AND s.st <> N'CANCELLED' THEN DATEADD(HOUR, -s.age + 1, SYSDATETIME()) END,
       DATEADD(HOUR, -s.age, SYSDATETIME()),
       CASE WHEN s.st NOT IN (N'WAITING_FOR_PAYMENT', N'WAITING_FOR_HANDOVER', N'CANCELLED') THEN DATEADD(HOUR, -s.age + 3, SYSDATETIME()) END,
       CASE WHEN s.st = N'DELIVERED' THEN DATEADD(HOUR, -(s.age / 3), SYSDATETIME()) END,
       CASE WHEN s.st = N'RETURNED_TO_SENDER' THEN DATEADD(HOUR, -10, SYSDATETIME()) END,
       CASE WHEN s.dst = @hcm THEN N'SOUTH' ELSE N'NORTH' END, s.recvProv, N'STANDARD'
FROM @s s JOIN dbo.users us ON us.id = s.sender;

/* ---------- tracking events ---------- */
DECLARE @e TABLE (trk NVARCHAR(30), typ NVARCHAR(50), descr NVARCHAR(255), hoursAgo INT, hub BIGINT, actor BIGINT);
INSERT @e VALUES
 (N'UTXDEMO0001', N'SHIPMENT_CREATED', N'Đơn hàng được tạo, chờ thanh toán', 1, NULL, @sender),
 (N'UTXDEMO0002', N'SHIPMENT_CREATED', N'Đơn hàng được tạo', 3, NULL, @sender),
 (N'UTXDEMO0002', N'PAYMENT_CONFIRMED', N'Đã thanh toán, chờ shipper đến lấy hàng', 2, NULL, @sender),
 (N'UTXDEMO0003', N'SHIPMENT_CREATED', N'Đơn hàng được tạo', 8, NULL, @sender),
 (N'UTXDEMO0003', N'PAYMENT_CONFIRMED', N'Đã thanh toán', 7, NULL, @sender),
 (N'UTXDEMO0003', N'PICKED_UP', N'Shipper đã lấy hàng tại địa chỉ người gửi', 5, NULL, @shipHcm),
 (N'UTXDEMO0004', N'SHIPMENT_CREATED', N'Đơn hàng được tạo', 12, NULL, @sender),
 (N'UTXDEMO0004', N'PAYMENT_CONFIRMED', N'Đã thanh toán', 11, NULL, @sender),
 (N'UTXDEMO0004', N'ARRIVED_AT_HUB', N'Hàng đã nhập bưu cục TP.HCM', 9, @hcm, @hubStaffHcm),
 (N'UTXDEMO0005', N'SHIPMENT_CREATED', N'Đơn hàng được tạo', 20, NULL, @sender),
 (N'UTXDEMO0005', N'ARRIVED_AT_HUB', N'Hàng đã nhập bưu cục TP.HCM', 17, @hcm, @hubStaffHcm),
 (N'UTXDEMO0005', N'BAG_CREATED', N'Hàng được đóng vào container', 14, @hcm, @hubStaffHcm),
 (N'UTXDEMO0005', N'LINEHAUL_TRIP_STARTED', N'Xe trung chuyển đã xuất phát đi hub miền Nam', 3, @hcm, @drvHcm),
 (N'UTXDEMO0006', N'SHIPMENT_CREATED', N'Đơn hàng được tạo', 22, NULL, @cust),
 (N'UTXDEMO0006', N'PICKED_UP', N'Shipper đã lấy hàng', 19, NULL, @shipHcm),
 (N'UTXDEMO0006', N'ARRIVED_AT_HUB', N'Hàng đã nhập bưu cục TP.HCM', 16, @hcm, @hubStaffHcm),
 (N'UTXDEMO0006', N'LINEHAUL_TRIP_STARTED', N'Xe trung chuyển đã xuất phát đi hub miền Nam', 3, @hcm, @drvHcm),
 (N'UTXDEMO0007', N'SHIPMENT_CREATED', N'Đơn hàng được tạo', 26, NULL, @sender),
 (N'UTXDEMO0007', N'ARRIVED_AT_HUB', N'Hàng đã nhập bưu cục Hà Nội', 4, @hn, @hubStaffHn),
 (N'UTXDEMO0008', N'SHIPMENT_CREATED', N'Đơn hàng được tạo', 40, NULL, @sender),
 (N'UTXDEMO0008', N'PICKED_UP', N'Shipper đã lấy hàng', 37, NULL, @shipHcm),
 (N'UTXDEMO0008', N'ARRIVED_AT_HUB', N'Hàng đã nhập bưu cục Hà Nội', 5, @hn, @hubStaffHn),
 (N'UTXDEMO0008', N'DELIVERY_ASSIGNED', N'Đã giao cho shipper Hà Nội đi giao', 2, @hn, @hubStaffHn),
 (N'UTXDEMO0009', N'SHIPMENT_CREATED', N'Đơn hàng được tạo', 50, NULL, @sender),
 (N'UTXDEMO0009', N'PICKED_UP', N'Shipper đã lấy hàng', 47, NULL, @shipHcm),
 (N'UTXDEMO0009', N'DELIVERED', N'Giao hàng thành công, đã thu COD 800.000 đ', 14, @hn, @shipHn),
 (N'UTXDEMO0010', N'SHIPMENT_CREATED', N'Đơn hàng được tạo', 120, NULL, @sender),
 (N'UTXDEMO0010', N'DELIVERED', N'Giao hàng thành công, đã thu COD 500.000 đ', 40, @hn, @shipHn),
 (N'UTXDEMO0010', N'COD_PAYOUT_COMPLETED', N'Đã đối soát và chuyển COD về ví người gửi', 20, @hn, @manager),
 (N'UTXDEMO0011', N'SHIPMENT_CREATED', N'Đơn hàng được tạo', 44, NULL, @sender),
 (N'UTXDEMO0011', N'DELIVERY_ATTEMPT_FAILED', N'Giao thất bại lần 1: người nhận không nghe máy', 6, @hn, @shipHn),
 (N'UTXDEMO0012', N'SHIPMENT_CREATED', N'Đơn hàng được tạo', 160, NULL, @sender),
 (N'UTXDEMO0012', N'DELIVERY_FAILED', N'Người nhận từ chối nhận hàng', 70, @hn, @shipHn),
 (N'UTXDEMO0012', N'RETURN_TO_SENDER', N'Đơn được chuyển hoàn về người gửi', 60, @hn, @hubStaffHn),
 (N'UTXDEMO0013', N'SHIPMENT_CREATED', N'Đơn hàng được tạo', 260, NULL, @sender),
 (N'UTXDEMO0013', N'DELIVERY_FAILED', N'Địa chỉ không tồn tại', 200, @hn, @shipHn),
 (N'UTXDEMO0013', N'RETURNED_TO_SENDER', N'Đã hoàn hàng cho người gửi', 10, @hcm, @hubStaffHcm),
 (N'UTXDEMO0014', N'SHIPMENT_CREATED', N'Đơn hàng được tạo', 30, NULL, @sender),
 (N'UTXDEMO0014', N'SHIPMENT_CANCELLED', N'Người gửi hủy đơn', 29, NULL, @sender),
 (N'UTXDEMO0014', N'REFUND_COMPLETED', N'Đã hoàn tiền về ví người gửi', 28, NULL, @admin),
 (N'UTXDEMO0015', N'SHIPMENT_CREATED', N'Đơn hàng được tạo', 200, NULL, @sender),
 (N'UTXDEMO0015', N'DELIVERED', N'Giao hàng thành công, đã thu COD 1.200.000 đ', 150, @hn, @shipHn),
 (N'UTXDEMO0015', N'COD_PAYOUT_COMPLETED', N'Đã đối soát và chuyển COD về ví người gửi', 120, @hn, @manager),
 (N'UTXDEMO0016', N'SHIPMENT_CREATED', N'Đơn hàng được tạo', 210, NULL, @cust),
 (N'UTXDEMO0016', N'DELIVERED', N'Giao hàng thành công, đã thu COD 2.000.000 đ', 160, @hn, @shipHn),
 (N'UTXDEMO0016', N'COD_PAYOUT_COMPLETED', N'Đã đối soát và chuyển COD về ví người gửi', 130, @hn, @manager),
 (N'UTXDEMO0017', N'SHIPMENT_CREATED', N'Đơn hàng được tạo, người nhận trả phí', 6, NULL, @sender),
 (N'UTXDEMO0017', N'ARRIVED_AT_HUB', N'Hàng đã nhập bưu cục Hà Nội', 4, @hn, @hubStaffHn);
INSERT dbo.shipment_events (shipment_id, hub_id, actor_user_id, event_type, description, created_at)
SELECT sh.id, e.hub, e.actor, e.typ, e.descr, DATEADD(HOUR, -e.hoursAgo, SYSDATETIME())
FROM @e e JOIN dbo.shipments sh ON sh.tracking_number = e.trk;

/* ---------- payments (shipping fee) ---------- */
INSERT dbo.payments (shipment_id, payer_type, payment_type, amount, method, status, transaction_ref, paid_at, refunded_at, created_at)
SELECT sh.id, N'SENDER', N'SHIPPING_FEE', sh.shipping_fee,
       CASE WHEN sh.tracking_number IN (N'UTXDEMO0003', N'UTXDEMO0006', N'UTXDEMO0012') THEN N'E_WALLET'
            WHEN sh.tracking_number IN (N'UTXDEMO0004', N'UTXDEMO0009', N'UTXDEMO0011', N'UTXDEMO0013') THEN N'CASH'
            ELSE N'VNPAY' END,
       CASE WHEN sh.payment_status = N'UNPAID' THEN N'UNPAID' WHEN sh.payment_status = N'REFUNDED' THEN N'REFUNDED' ELSE N'PAID' END,
       CASE WHEN sh.payment_status = N'UNPAID' THEN NULL ELSE N'DEMO-' + sh.tracking_number END,
       CASE WHEN sh.payment_status = N'UNPAID' THEN NULL ELSE DATEADD(MINUTE, 20, sh.created_at) END,
       CASE WHEN sh.payment_status = N'REFUNDED' THEN DATEADD(HOUR, 1, sh.created_at) END,
       sh.created_at
FROM dbo.shipments sh WHERE sh.tracking_number LIKE N'UTXDEMO%' AND sh.fee_payer = N'SENDER';
INSERT dbo.payments (shipment_id, payer_type, payment_type, amount, method, status, created_at)
SELECT sh.id, N'SENDER', N'RETURN_FEE', sh.return_fee, N'E_WALLET', N'UNPAID', SYSDATETIME()
FROM dbo.shipments sh WHERE sh.tracking_number = N'UTXDEMO0012';
INSERT dbo.payments (shipment_id, payer_type, payment_type, amount, method, status, transaction_ref, paid_at, created_at)
SELECT sh.id, N'SENDER', N'RETURN_FEE', sh.return_fee, N'E_WALLET', N'PAID', N'DEMO-RET-0013', DATEADD(HOUR, -11, SYSDATETIME()), DATEADD(HOUR, -12, SYSDATETIME())
FROM dbo.shipments sh WHERE sh.tracking_number = N'UTXDEMO0013';
INSERT dbo.payments (shipment_id, payer_type, payment_type, amount, method, status, created_at)
SELECT sh.id, N'RECEIVER', N'SHIPPING_FEE', sh.shipping_fee, N'CASH', N'UNPAID', sh.created_at
FROM dbo.shipments sh WHERE sh.tracking_number = N'UTXDEMO0017';

/* ---------- shipper tasks ---------- */
DECLARE @a TABLE (trk NVARCHAR(30), who BIGINT, typ NVARCHAR(20), st NVARCHAR(20), hoursAgo INT);
INSERT @a VALUES
 (N'UTXDEMO0002', @shipHcm, N'PICKUP',   N'ASSIGNED',  1),
 (N'UTXDEMO0003', @shipHcm, N'PICKUP',   N'COMPLETED', 6),
 (N'UTXDEMO0006', @shipHcm, N'PICKUP',   N'COMPLETED', 21),
 (N'UTXDEMO0008', @shipHcm, N'PICKUP',   N'COMPLETED', 38),
 (N'UTXDEMO0008', @shipHn,  N'DELIVERY', N'ASSIGNED',  2),
 (N'UTXDEMO0009', @shipHcm, N'PICKUP',   N'COMPLETED', 48),
 (N'UTXDEMO0009', @shipHn,  N'DELIVERY', N'COMPLETED', 20),
 (N'UTXDEMO0010', @shipHn,  N'DELIVERY', N'COMPLETED', 45),
 (N'UTXDEMO0011', @shipHn,  N'DELIVERY', N'ASSIGNED',  10),
 (N'UTXDEMO0015', @shipHn,  N'DELIVERY', N'COMPLETED', 155),
 (N'UTXDEMO0016', @shipHn,  N'DELIVERY', N'COMPLETED', 165);
INSERT dbo.shipment_assignments (shipment_id, assignee_user_id, assignment_type, status, assigned_at, accepted_at, completed_at)
SELECT sh.id, a.who, a.typ, a.st, DATEADD(HOUR, -a.hoursAgo - 1, SYSDATETIME()),
       CASE WHEN a.st IN (N'ASSIGNED', N'COMPLETED') THEN DATEADD(HOUR, -a.hoursAgo, SYSDATETIME()) END,
       CASE WHEN a.st = N'COMPLETED' THEN DATEADD(HOUR, -a.hoursAgo + 1, SYSDATETIME()) END
FROM @a a JOIN dbo.shipments sh ON sh.tracking_number = a.trk;

INSERT dbo.pickup_attempts (assignment_id, shipment_id, shipper_id, attempt_number, attempt_time, result, note)
SELECT sa.id, sa.shipment_id, sa.assignee_user_id, 1, sa.assigned_at, N'SUCCESS', N'Đã lấy hàng'
FROM dbo.shipment_assignments sa JOIN dbo.shipments sh ON sh.id = sa.shipment_id
WHERE sh.tracking_number LIKE N'UTXDEMO%' AND sa.assignment_type = N'PICKUP' AND sa.status = N'COMPLETED';
INSERT dbo.delivery_attempts (assignment_id, shipment_id, shipper_id, attempt_number, attempt_date, attempt_time, result, note)
SELECT sa.id, sa.shipment_id, sa.assignee_user_id, 1, CAST(sa.completed_at AS DATE), sa.completed_at, N'SUCCESS', N'Giao thành công'
FROM dbo.shipment_assignments sa JOIN dbo.shipments sh ON sh.id = sa.shipment_id
WHERE sh.tracking_number LIKE N'UTXDEMO%' AND sa.assignment_type = N'DELIVERY' AND sa.status = N'COMPLETED';
INSERT dbo.delivery_attempts (assignment_id, shipment_id, shipper_id, attempt_number, attempt_date, attempt_time, result, note)
SELECT sa.id, sa.shipment_id, sa.assignee_user_id, 1, CAST(SYSDATETIME() AS DATE), DATEADD(HOUR, -6, SYSDATETIME()), N'NO_ANSWER', N'Người nhận không nghe máy'
FROM dbo.shipment_assignments sa JOIN dbo.shipments sh ON sh.id = sa.shipment_id
WHERE sh.tracking_number = N'UTXDEMO0011' AND sa.assignment_type = N'DELIVERY';

/* ---------- COD ---------- */
INSERT dbo.cod_transactions (shipment_id, collected_by_shipper_id, amount, method, status, collected_at, payout_at)
SELECT sh.id, @shipHn, sh.cod_amount, N'CASH',
       CASE sh.tracking_number WHEN N'UTXDEMO0008' THEN N'WAITING_COLLECTION' WHEN N'UTXDEMO0009' THEN N'COLLECTED' ELSE N'PAID_TO_SENDER' END,
       CASE WHEN sh.tracking_number <> N'UTXDEMO0008' THEN sh.delivered_at END,
       CASE WHEN sh.tracking_number IN (N'UTXDEMO0010', N'UTXDEMO0015', N'UTXDEMO0016') THEN DATEADD(HOUR, 24, sh.delivered_at) END
FROM dbo.shipments sh WHERE sh.tracking_number IN (N'UTXDEMO0008', N'UTXDEMO0009', N'UTXDEMO0010', N'UTXDEMO0015', N'UTXDEMO0016');

/* a finished cash reconciliation for the paid-out COD */
INSERT dbo.shipper_reconciliations (shipper_id, manager_id, reconciliation_date, expected_amount, actual_amount, difference_amount, status, completed_at, hub_id, collection_method)
VALUES (@shipHn, @manager, CAST(DATEADD(DAY, -1, SYSDATETIME()) AS DATE), 500000, 500000, 0, N'COMPLETED', DATEADD(HOUR, -20, SYSDATETIME()), @hn, N'CASH');
INSERT dbo.shipper_reconciliation_items (reconciliation_id, shipment_id, expected_amount, actual_amount, difference_amount, status, source_type, source_id)
SELECT SCOPE_IDENTITY(), c.shipment_id, c.amount, c.amount, 0, N'MATCHED', N'COD_TRANSACTION', c.id
FROM dbo.cod_transactions c JOIN dbo.shipments sh ON sh.id = c.shipment_id WHERE sh.tracking_number = N'UTXDEMO0010';

/* ---------- wallets, ledger and withdrawals (ledger adds up: balance and locked_balance match the rows) ---------- */
INSERT dbo.withdrawal_requests (user_id, amount, bank_name, account_number, account_holder_name, status, rejection_reason, requested_at, approved_at, rejected_at, completed_at, request_key)
VALUES
 (@sender, 500000, N'Vietcombank', N'0071000123456', N'DEMO NGUOI GUI', N'COMPLETED', NULL, DATEADD(DAY, -3, SYSDATETIME()), DATEADD(DAY, -3, SYSDATETIME()), NULL, DATEADD(DAY, -2, SYSDATETIME()), N'demo-wd-1'),
 (@sender, 300000, N'Techcombank',  N'19031234567890', N'DEMO NGUOI GUI', N'PENDING',   NULL, DATEADD(HOUR, -5, SYSDATETIME()), NULL, NULL, NULL, N'demo-wd-2'),
 (@sender, 150000, N'MB Bank',      N'0987654321012',  N'DEMO NGUOI GUI', N'APPROVED',  NULL, DATEADD(HOUR, -30, SYSDATETIME()), DATEADD(HOUR, -26, SYSDATETIME()), NULL, NULL, N'demo-wd-3'),
 (@sender, 100000, N'ACB',          N'45678912',       N'DEMO NGUOI GUI', N'REJECTED',  N'Tên chủ tài khoản không khớp với tên đăng ký', DATEADD(DAY, -1, SYSDATETIME()), NULL, DATEADD(HOUR, -20, SYSDATETIME()), NULL, N'demo-wd-4'),
 (@cust,   400000, N'BIDV',         N'31410001234567', N'DEMO KHACH HANG', N'PENDING',  NULL, DATEADD(HOUR, -2, SYSDATETIME()), NULL, NULL, NULL, N'demo-wd-5');
DECLARE @w1 BIGINT = (SELECT id FROM dbo.withdrawal_requests WHERE request_key = N'demo-wd-1');
DECLARE @w2 BIGINT = (SELECT id FROM dbo.withdrawal_requests WHERE request_key = N'demo-wd-2');
DECLARE @w3 BIGINT = (SELECT id FROM dbo.withdrawal_requests WHERE request_key = N'demo-wd-3');
DECLARE @w4 BIGINT = (SELECT id FROM dbo.withdrawal_requests WHERE request_key = N'demo-wd-4');
DECLARE @w5 BIGINT = (SELECT id FROM dbo.withdrawal_requests WHERE request_key = N'demo-wd-5');
DECLARE @t10 BIGINT = (SELECT id FROM dbo.shipments WHERE tracking_number = N'UTXDEMO0010');
DECLARE @t15 BIGINT = (SELECT id FROM dbo.shipments WHERE tracking_number = N'UTXDEMO0015');
DECLARE @t16 BIGINT = (SELECT id FROM dbo.shipments WHERE tracking_number = N'UTXDEMO0016');
INSERT dbo.wallet_transactions (user_id, transaction_type, amount, balance_before, balance_after, reference_id, reference_type, description, created_at)
VALUES
 (@sender, N'COD_PAYOUT',        500000,  0,       500000,  @t10, N'SHIPMENT',  N'Đối soát COD đơn UTXDEMO0010',  DATEADD(HOUR, -20, SYSDATETIME())),
 (@sender, N'COD_PAYOUT',        1200000, 500000,  1700000, @t15, N'SHIPMENT',  N'Đối soát COD đơn UTXDEMO0015',  DATEADD(HOUR, -120, SYSDATETIME())),
 (@sender, N'WITHDRAWAL_LOCK',   500000,  1700000, 1200000, @w1,  N'WITHDRAWAL', N'Khóa tiền rút #' + CAST(@w1 AS NVARCHAR(20)), DATEADD(DAY, -3, SYSDATETIME())),
 (@sender, N'WITHDRAWAL_DEDUCT', 500000,  1200000, 1200000, @w1,  N'WITHDRAWAL', N'Đã chuyển khoản rút tiền #' + CAST(@w1 AS NVARCHAR(20)), DATEADD(DAY, -2, SYSDATETIME())),
 (@sender, N'WITHDRAWAL_LOCK',   300000,  1200000, 900000,  @w2,  N'WITHDRAWAL', N'Khóa tiền rút #' + CAST(@w2 AS NVARCHAR(20)), DATEADD(HOUR, -5, SYSDATETIME())),
 (@sender, N'WITHDRAWAL_LOCK',   150000,  900000,  750000,  @w3,  N'WITHDRAWAL', N'Khóa tiền rút #' + CAST(@w3 AS NVARCHAR(20)), DATEADD(HOUR, -30, SYSDATETIME())),
 (@sender, N'WITHDRAWAL_LOCK',   100000,  750000,  650000,  @w4,  N'WITHDRAWAL', N'Khóa tiền rút #' + CAST(@w4 AS NVARCHAR(20)), DATEADD(DAY, -1, SYSDATETIME())),
 (@sender, N'WITHDRAWAL_REFUND', 100000,  650000,  750000,  @w4,  N'WITHDRAWAL', N'Hoàn tiền do từ chối rút #' + CAST(@w4 AS NVARCHAR(20)), DATEADD(HOUR, -20, SYSDATETIME())),
 (@cust,   N'COD_PAYOUT',        2000000, 0,       2000000, @t16, N'SHIPMENT',  N'Đối soát COD đơn UTXDEMO0016',  DATEADD(HOUR, -130, SYSDATETIME())),
 (@cust,   N'WITHDRAWAL_LOCK',   400000,  2000000, 1600000, @w5,  N'WITHDRAWAL', N'Khóa tiền rút #' + CAST(@w5 AS NVARCHAR(20)), DATEADD(HOUR, -2, SYSDATETIME()));
UPDATE dbo.wallets SET balance = 750000,  locked_balance = 450000 WHERE user_id = @sender;
UPDATE dbo.wallets SET balance = 1600000, locked_balance = 400000 WHERE user_id = @cust;

/* ---------- linehaul: one trip on the road (HCM to the south hub), one scheduled (Hanoi to the north hub) ---------- */
DECLARE @v1 BIGINT = (SELECT id FROM dbo.vehicles WHERE plate_number = N'51C-DEMO1');
DECLARE @v2 BIGINT = (SELECT id FROM dbo.vehicles WHERE plate_number = N'29C-DEMO1');
INSERT dbo.parcel_containers (container_code, origin_hub_id, destination_hub_id, status, created_by, created_at)
VALUES (N'DEMO-CNT-001', @hcm, @south, N'OPEN', @hubStaffHcm, DATEADD(HOUR, -14, SYSDATETIME())),
       (N'DEMO-CNT-002', @hn,  @north, N'OPEN', @hubStaffHn,  DATEADD(HOUR, -3, SYSDATETIME()));
DECLARE @c1 BIGINT = (SELECT id FROM dbo.parcel_containers WHERE container_code = N'DEMO-CNT-001');
DECLARE @c2 BIGINT = (SELECT id FROM dbo.parcel_containers WHERE container_code = N'DEMO-CNT-002');
INSERT dbo.container_items (container_id, shipment_id, status, scanned_at)
SELECT @c1, id, N'IN_CONTAINER', DATEADD(HOUR, -13, SYSDATETIME()) FROM dbo.shipments WHERE tracking_number IN (N'UTXDEMO0005', N'UTXDEMO0006');
INSERT dbo.container_items (container_id, shipment_id, status, scanned_at)
SELECT @c2, id, N'IN_CONTAINER', DATEADD(HOUR, -2, SYSDATETIME()) FROM dbo.shipments WHERE tracking_number = N'UTXDEMO0007';
UPDATE dbo.parcel_containers SET status = N'LOADED' WHERE id = @c1;
UPDATE dbo.parcel_containers SET status = N'SEALED' WHERE id = @c2;

INSERT dbo.handovers (from_hub_id, to_hub_id, sender_employee_id, receiver_driver_id, expected_containers, accepted_containers, status, created_at, accepted_at)
VALUES (@hcm, @south, @hubStaffHcm, @drvHcm, 1, 1, N'IN_TRANSIT', DATEADD(HOUR, -4, SYSDATETIME()), DATEADD(HOUR, -3, SYSDATETIME())),
       (@hn,  @north, @hubStaffHn,  @drvHn,  1, 0, N'PENDING_ACCEPTANCE', DATEADD(HOUR, -1, SYSDATETIME()), NULL);
DECLARE @h1 BIGINT = (SELECT TOP 1 id FROM dbo.handovers WHERE from_hub_id = @hcm AND receiver_driver_id = @drvHcm ORDER BY id DESC);
DECLARE @h2 BIGINT = (SELECT TOP 1 id FROM dbo.handovers WHERE from_hub_id = @hn AND receiver_driver_id = @drvHn ORDER BY id DESC);
INSERT dbo.transport_trips (trip_code, origin_hub_id, destination_hub_id, driver_user_id, vehicle_id, departure_time, status, handover_id, accepted_at, started_at, accepted_by_user_id, started_by_user_id)
VALUES (N'DEMO-TRIP-001', @hcm, @south, @drvHcm, @v1, DATEADD(HOUR, -3, SYSDATETIME()), N'IN_TRANSIT', @h1, DATEADD(HOUR, -3, SYSDATETIME()), DATEADD(HOUR, -3, SYSDATETIME()), @drvHcm, @drvHcm),
       (N'DEMO-TRIP-002', @hn,  @north, @drvHn,  @v2, DATEADD(HOUR, 2, SYSDATETIME()),  N'SCHEDULED',  @h2, NULL, NULL, NULL, NULL);
UPDATE dbo.handovers SET transport_trip_id = (SELECT id FROM dbo.transport_trips WHERE trip_code = N'DEMO-TRIP-001') WHERE id = @h1;
UPDATE dbo.handovers SET transport_trip_id = (SELECT id FROM dbo.transport_trips WHERE trip_code = N'DEMO-TRIP-002') WHERE id = @h2;
INSERT dbo.handover_containers (handover_id, container_id, status, scanned_at)
VALUES (@h1, @c1, N'LOADED', DATEADD(HOUR, -4, SYSDATETIME())), (@h2, @c2, N'EXPECTED', NULL);

/* a delay report on the trip that is on the road, for the manager to follow up */
INSERT dbo.incidents (trip_id, hub_id, type, description, reported_by, status, created_at)
SELECT t.id, @hcm, N'TRIP_DELAY', N'Xe bị kẹt xe cao tốc, dự kiến đến trễ 90 phút', @drvHcm, N'OPEN', DATEADD(MINUTE, -40, SYSDATETIME())
FROM dbo.transport_trips t WHERE t.trip_code = N'DEMO-TRIP-001';

COMMIT TRANSACTION;
END
