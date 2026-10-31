/* Demo-only data for the video script: every step has TWO parcels ready, so each role can be shown twice.
   UTXDEMO0030-31  home pickup waiting at HCM        -> /hub/pickups assigns a shipper, shipper picks up
   UTXDEMO0036-37  at HCM, to be sorted              UTXDEMO0038-39  at HN, to be sorted
   UTXDEMO0040-41  sealed, handover to HN pending    UTXDEMO0042-43  sealed, handover to HCM pending  (driver accepts / manager reassigns)
   UTXDEMO0044-45  on the road, one to HCM, one to HN (inbound)
   UTXDEMO0046-47  arrived at HCM, assign delivery   UTXDEMO0048-49  arrived at HN, assign delivery
   UTXDEMO0050-53  out for delivery with COD (2 per shipper)
   UTXDEMO0054-55  incoming parcels for demo_receiver (receiver pays)
   Not seeded on purpose: counter drop-off and unpaid parcels. The demo profile cancels them after 2 minutes
   (uteexpress.timeout.*), so create those two live in the video and process them right away.
   Runs after R__demo_e. Inserts once, keyed on UTXDEMO0030; to rebuild, reset the database. */
IF NOT EXISTS (SELECT 1 FROM dbo.shipments WHERE tracking_number = N'UTXDEMO0030')
AND EXISTS (SELECT 1 FROM dbo.shipments WHERE tracking_number = N'UTXDEMO0029')
AND EXISTS (SELECT 1 FROM dbo.users WHERE username = N'demo_driver_hcm2')
BEGIN
SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @sender BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_sender');
DECLARE @recv BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_receiver');
DECLARE @shipHcm BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_shipper_hcm');
DECLARE @shipHn BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_shipper_hn');
DECLARE @drvHcm BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_driver_hcm');
DECLARE @drvHcm2 BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_driver_hcm2');
DECLARE @drvHn BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_driver_hn');
DECLARE @hubStaffHcm BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_hub_hcm');
DECLARE @hubStaffHn BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_hub_hn');
DECLARE @hcm BIGINT = (SELECT id FROM dbo.hubs WHERE code = N'LOCAL2025-P79');
DECLARE @hn BIGINT = (SELECT id FROM dbo.hubs WHERE code = N'LOCAL2025-P01');
DECLARE @staffHcmProfile BIGINT = (SELECT id FROM dbo.employee_profiles WHERE user_id = @hubStaffHcm);
DECLARE @staffHnProfile BIGINT = (SELECT id FROM dbo.employee_profiles WHERE user_id = @hubStaffHn);

/* ---------- shipments ---------- */
DECLARE @s TABLE (trk NVARCHAR(30), st NVARCHAR(30), pickup NVARCHAR(20), payer NVARCHAR(10), pay NVARCHAR(20),
                  cur BIGINT, org BIGINT, dst BIGINT, fee DECIMAL(15,2), cod DECIMAL(15,2), age INT, toRecv BIT,
                  recvName NVARCHAR(100), recvAddr NVARCHAR(255), recvProv NVARCHAR(100));
INSERT @s VALUES
 (N'UTXDEMO0030', N'WAITING_FOR_HANDOVER', N'HOME_PICKUP',  N'SENDER', N'PAID', NULL, @hcm, @hn, 52000, 0,      2, 0, N'Nguyễn Thu Trang',   N'14 Hàng Trống, Hoàn Kiếm, Hà Nội',        N'Hà Nội'),
 (N'UTXDEMO0031', N'WAITING_FOR_HANDOVER', N'HOME_PICKUP',  N'SENDER', N'PAID', NULL, @hcm, @hn, 53000, 150000, 2, 0, N'Lê Hoàng Yến',       N'30 Đội Cấn, Ba Đình, Hà Nội',             N'Hà Nội'),
 (N'UTXDEMO0036', N'AT_HUB', N'SELF_DROPOFF', N'SENDER', N'PAID', @hcm, @hcm, @hn,  50000, 0,      4, 0, N'Bùi Văn Dũng',       N'8 Lê Thanh Nghị, Hai Bà Trưng, Hà Nội',   N'Hà Nội'),
 (N'UTXDEMO0037', N'AT_HUB', N'HOME_PICKUP',  N'SENDER', N'PAID', @hcm, @hcm, @hn,  52000, 100000, 4, 0, N'Hoàng Kim Em',       N'33 Kim Mã, Ba Đình, Hà Nội',              N'Hà Nội'),
 (N'UTXDEMO0038', N'AT_HUB', N'SELF_DROPOFF', N'SENDER', N'PAID', @hn,  @hn,  @hcm, 49000, 0,      4, 0, N'Lâm Văn Giàu',       N'66 Cách Mạng Tháng 8, Quận 3, TP.HCM',    N'TP. Hồ Chí Minh'),
 (N'UTXDEMO0039', N'AT_HUB', N'HOME_PICKUP',  N'SENDER', N'PAID', @hn,  @hn,  @hcm, 51000, 120000, 4, 0, N'Mai Thị Hạnh',       N'12 Võ Văn Tần, Quận 3, TP.HCM',           N'TP. Hồ Chí Minh'),
 (N'UTXDEMO0040', N'AT_HUB', N'SELF_DROPOFF', N'SENDER', N'PAID', @hcm, @hcm, @hn,  50000, 0,      5, 0, N'Ngô Quang Hiếu',     N'41 Tây Sơn, Đống Đa, Hà Nội',             N'Hà Nội'),
 (N'UTXDEMO0041', N'AT_HUB', N'SELF_DROPOFF', N'SENDER', N'PAID', @hcm, @hcm, @hn,  51000, 0,      5, 0, N'Đinh Thị Hương',     N'27 Láng Hạ, Đống Đa, Hà Nội',             N'Hà Nội'),
 (N'UTXDEMO0042', N'AT_HUB', N'SELF_DROPOFF', N'SENDER', N'PAID', @hn,  @hn,  @hcm, 50000, 0,      5, 0, N'Cao Văn Khoa',       N'88 Nguyễn Huệ, Quận 1, TP.HCM',           N'TP. Hồ Chí Minh'),
 (N'UTXDEMO0043', N'AT_HUB', N'SELF_DROPOFF', N'SENDER', N'PAID', @hn,  @hn,  @hcm, 52000, 0,      5, 0, N'Trương Mỹ Lan',      N'15 Pasteur, Quận 1, TP.HCM',              N'TP. Hồ Chí Minh'),
 (N'UTXDEMO0044', N'IN_TRANSIT', N'SELF_DROPOFF', N'SENDER', N'PAID', @hcm, @hn,  @hcm, 54000, 0,     8, 0, N'Phan Văn Minh',      N'200 Lê Văn Sỹ, Quận 3, TP.HCM',           N'TP. Hồ Chí Minh'),
 (N'UTXDEMO0045', N'IN_TRANSIT', N'SELF_DROPOFF', N'SENDER', N'PAID', @hn,  @hcm, @hn,  53000, 0,     8, 0, N'Tạ Thị Nga',         N'9 Hàng Bài, Hoàn Kiếm, Hà Nội',           N'Hà Nội'),
 (N'UTXDEMO0046', N'AT_HUB', N'SELF_DROPOFF', N'SENDER', N'PAID', @hcm, @hn,  @hcm, 54000, 0,      3, 0, N'Vương Hữu Phước',    N'55 Nguyễn Đình Chiểu, Quận 3, TP.HCM',    N'TP. Hồ Chí Minh'),
 (N'UTXDEMO0047', N'AT_HUB', N'HOME_PICKUP',  N'SENDER', N'PAID', @hcm, @hn,  @hcm, 56000, 200000, 3, 0, N'Lý Thị Quyên',       N'18 Trần Hưng Đạo, Quận 5, TP.HCM',        N'TP. Hồ Chí Minh'),
 (N'UTXDEMO0048', N'AT_HUB', N'SELF_DROPOFF', N'SENDER', N'PAID', @hn,  @hcm, @hn,  53000, 0,      3, 0, N'Hồ Văn Sơn',         N'70 Phố Huế, Hai Bà Trưng, Hà Nội',        N'Hà Nội'),
 (N'UTXDEMO0049', N'AT_HUB', N'HOME_PICKUP',  N'SENDER', N'PAID', @hn,  @hcm, @hn,  55000, 180000, 3, 0, N'Nông Thị Tâm',       N'6 Bạch Mai, Hai Bà Trưng, Hà Nội',        N'Hà Nội'),
 (N'UTXDEMO0050', N'OUT_FOR_DELIVERY', N'HOME_PICKUP', N'SENDER', N'PAID', @hcm, @hn,  @hcm, 54000, 150000, 6, 0, N'Dương Văn Úc',   N'12 Lý Tự Trọng, Quận 1, TP.HCM',          N'TP. Hồ Chí Minh'),
 (N'UTXDEMO0051', N'OUT_FOR_DELIVERY', N'HOME_PICKUP', N'SENDER', N'PAID', @hcm, @hn,  @hcm, 55000, 200000, 6, 0, N'Châu Thị Vân',   N'34 Nam Kỳ Khởi Nghĩa, Quận 3, TP.HCM',    N'TP. Hồ Chí Minh'),
 (N'UTXDEMO0052', N'OUT_FOR_DELIVERY', N'HOME_PICKUP', N'SENDER', N'PAID', @hn,  @hcm, @hn,  52000, 180000, 6, 0, N'Kiều Văn Xuân',   N'45 Giải Phóng, Hai Bà Trưng, Hà Nội',     N'Hà Nội'),
 (N'UTXDEMO0053', N'OUT_FOR_DELIVERY', N'HOME_PICKUP', N'SENDER', N'PAID', @hn,  @hcm, @hn,  53000, 250000, 6, 0, N'Lại Thị Yên',    N'23 Hồ Tùng Mậu, Cầu Giấy, Hà Nội',        N'Hà Nội'),
 (N'UTXDEMO0054', N'AT_HUB', N'SELF_DROPOFF', N'RECEIVER', N'UNPAID', @hcm, @hn, @hcm, 56000, 0,     5, 1, N'Demo Người nhận',    N'123 Lê Lợi, Quận 1, TP.HCM',              N'TP. Hồ Chí Minh'),
 (N'UTXDEMO0055', N'AT_HUB', N'SELF_DROPOFF', N'RECEIVER', N'UNPAID', @hcm, @hn, @hcm, 57000, 0,     5, 1, N'Demo Người nhận',    N'123 Lê Lợi, Quận 1, TP.HCM',              N'TP. Hồ Chí Minh');

INSERT dbo.shipments (tracking_number, sender_user_id, receiver_user_id, sender_name, sender_phone, sender_address,
        receiver_name, receiver_phone, receiver_address, receiver_province, receiver_district, pickup_method, fee_payer, inspection_policy,
        weight, declared_value, shipping_fee, cod_amount, return_fee, payment_status, origin_hub_id, destination_hub_id, current_hub_id,
        direction, current_status, on_hold, payment_deadline, waiting_for_handover_at, created_at,
        picked_up_at, destination_region, destination_province, service_code)
SELECT s.trk, @sender, CASE WHEN s.toRecv = 1 THEN @recv END, us.full_name, us.phone, N'25 Nguyễn Văn Cừ, Quận 5, TP.HCM',
       s.recvName, CASE WHEN s.toRecv = 1 THEN ur.phone ELSE N'+8490000' + RIGHT(s.trk, 4) END, s.recvAddr, s.recvProv, N'',
       s.pickup, s.payer, N'INSPECTION_ONLY', 1500, s.cod + 500000, s.fee, s.cod, 0, s.pay, s.org, s.dst, s.cur,
       N'FORWARD', s.st, 0,
       CASE WHEN s.st = N'WAITING_FOR_PAYMENT' THEN DATEADD(HOUR, 72, SYSDATETIME()) END,
       CASE WHEN s.st <> N'WAITING_FOR_PAYMENT' THEN DATEADD(HOUR, -s.age + 1, SYSDATETIME()) END,
       DATEADD(HOUR, -s.age, SYSDATETIME()),
       CASE WHEN s.st NOT IN (N'WAITING_FOR_PAYMENT', N'WAITING_FOR_HANDOVER') THEN DATEADD(HOUR, -s.age + 1, SYSDATETIME()) END,
       CASE WHEN s.dst = @hcm THEN N'SOUTH' ELSE N'NORTH' END, s.recvProv, N'STANDARD'
FROM @s s JOIN dbo.users us ON us.id = @sender LEFT JOIN dbo.users ur ON ur.id = @recv;

/* ---------- events ---------- */
INSERT dbo.shipment_events (shipment_id, hub_id, actor_user_id, event_type, description, created_at)
SELECT sh.id, NULL, @sender, N'SHIPMENT_CREATED', N'Đơn hàng được tạo', sh.created_at
FROM dbo.shipments sh WHERE sh.tracking_number BETWEEN N'UTXDEMO0030' AND N'UTXDEMO0057';
INSERT dbo.shipment_events (shipment_id, hub_id, actor_user_id, event_type, description, created_at)
SELECT sh.id, sh.current_hub_id, CASE WHEN sh.current_hub_id = @hcm THEN @hubStaffHcm ELSE @hubStaffHn END, N'ARRIVED_AT_HUB',
       CASE WHEN sh.current_hub_id = @hcm THEN N'Hàng đã nhập bưu cục TP.HCM' ELSE N'Hàng đã nhập bưu cục Hà Nội' END,
       DATEADD(HOUR, 1, sh.created_at)
FROM dbo.shipments sh WHERE sh.tracking_number BETWEEN N'UTXDEMO0030' AND N'UTXDEMO0057'
  AND sh.current_status IN (N'AT_HUB', N'OUT_FOR_DELIVERY');
INSERT dbo.shipment_events (shipment_id, hub_id, actor_user_id, event_type, description, created_at)
SELECT sh.id, sh.current_hub_id, CASE WHEN sh.current_hub_id = @hcm THEN @hubStaffHcm ELSE @hubStaffHn END, N'DELIVERY_ASSIGNED',
       N'Đã giao cho shipper đi giao', DATEADD(HOUR, -1, SYSDATETIME())
FROM dbo.shipments sh WHERE sh.tracking_number BETWEEN N'UTXDEMO0050' AND N'UTXDEMO0053';

/* ---------- payments ---------- */
INSERT dbo.payments (shipment_id, payer_type, payment_type, amount, method, status, transaction_ref, paid_at, created_at)
SELECT sh.id, N'SENDER', N'SHIPPING_FEE', sh.shipping_fee,
       CASE WHEN RIGHT(sh.tracking_number, 1) IN (N'1', N'3', N'5', N'7', N'9') THEN N'CASH' ELSE N'VNPAY' END,
       N'PAID', N'DEMO-' + sh.tracking_number, DATEADD(MINUTE, 20, sh.created_at), sh.created_at
FROM dbo.shipments sh WHERE sh.tracking_number BETWEEN N'UTXDEMO0030' AND N'UTXDEMO0053';
INSERT dbo.payments (shipment_id, payer_type, payment_type, amount, method, status, created_at)
SELECT sh.id, N'RECEIVER', N'SHIPPING_FEE', sh.shipping_fee, N'CASH', N'UNPAID', sh.created_at
FROM dbo.shipments sh WHERE sh.tracking_number IN (N'UTXDEMO0054', N'UTXDEMO0055');

/* ---------- delivery tasks already assigned (the shipper collects COD in the app) ---------- */
INSERT dbo.shipment_assignments (shipment_id, assignee_user_id, assignment_type, status, assigned_at, accepted_at)
SELECT sh.id, CASE WHEN sh.current_hub_id = @hcm THEN @shipHcm ELSE @shipHn END, N'DELIVERY', N'ASSIGNED',
       DATEADD(HOUR, -1, SYSDATETIME()), DATEADD(MINUTE, -50, SYSDATETIME())
FROM dbo.shipments sh WHERE sh.tracking_number BETWEEN N'UTXDEMO0050' AND N'UTXDEMO0053';

/* ---------- linehaul: four sealed containers waiting for a driver (two per direction) ---------- */
INSERT dbo.parcel_containers (container_code, origin_hub_id, destination_hub_id, status, created_by, created_at)
VALUES (N'DEMO-CNT-008', @hcm, @hn,  N'OPEN', @hubStaffHcm, DATEADD(MINUTE, -60, SYSDATETIME())),
       (N'DEMO-CNT-009', @hcm, @hn,  N'OPEN', @hubStaffHcm, DATEADD(MINUTE, -55, SYSDATETIME())),
       (N'DEMO-CNT-010', @hn,  @hcm, N'OPEN', @hubStaffHn,  DATEADD(MINUTE, -60, SYSDATETIME())),
       (N'DEMO-CNT-011', @hn,  @hcm, N'OPEN', @hubStaffHn,  DATEADD(MINUTE, -55, SYSDATETIME())),
       (N'DEMO-CNT-012', @hn,  @hcm, N'LOADED', @hubStaffHn,  DATEADD(HOUR, -4, SYSDATETIME())),
       (N'DEMO-CNT-013', @hcm, @hn,  N'LOADED', @hubStaffHcm, DATEADD(HOUR, -4, SYSDATETIME()));
INSERT dbo.container_items (container_id, shipment_id, status, scanned_at)
SELECT c.id, sh.id, N'IN_CONTAINER', DATEADD(MINUTE, -50, SYSDATETIME())
FROM (VALUES (N'UTXDEMO0040', N'DEMO-CNT-008'), (N'UTXDEMO0041', N'DEMO-CNT-009'), (N'UTXDEMO0042', N'DEMO-CNT-010'),
             (N'UTXDEMO0043', N'DEMO-CNT-011'), (N'UTXDEMO0044', N'DEMO-CNT-012'), (N'UTXDEMO0045', N'DEMO-CNT-013')) m(trk, code)
JOIN dbo.shipments sh ON sh.tracking_number = m.trk
JOIN dbo.parcel_containers c ON c.container_code = m.code;
UPDATE dbo.parcel_containers SET status = N'SEALED' WHERE container_code IN (N'DEMO-CNT-008', N'DEMO-CNT-009', N'DEMO-CNT-010', N'DEMO-CNT-011');

/* four handovers nobody has accepted: a driver accepts them, or a manager reassigns the driver */
INSERT dbo.handovers (from_hub_id, to_hub_id, sender_employee_id, receiver_driver_id, expected_containers, accepted_containers, status, created_at)
VALUES (@hcm, @hn,  @staffHcmProfile, @drvHcm, 1, 0, N'PENDING_ACCEPTANCE', DATEADD(MINUTE, -40, SYSDATETIME())),
       (@hcm, @hn,  @staffHcmProfile, @drvHcm, 1, 0, N'PENDING_ACCEPTANCE', DATEADD(MINUTE, -35, SYSDATETIME())),
       (@hn,  @hcm, @staffHnProfile,  @drvHn,  1, 0, N'PENDING_ACCEPTANCE', DATEADD(MINUTE, -40, SYSDATETIME())),
       (@hn,  @hcm, @staffHnProfile,  @drvHn,  1, 0, N'PENDING_ACCEPTANCE', DATEADD(MINUTE, -35, SYSDATETIME()));
INSERT dbo.handover_containers (handover_id, container_id, status)
SELECT h.id, c.id, N'EXPECTED'
FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) rn FROM dbo.handovers WHERE status = N'PENDING_ACCEPTANCE' AND created_at >= DATEADD(MINUTE, -45, SYSDATETIME())
      AND from_hub_id IN (@hcm, @hn) AND transport_trip_id IS NULL AND id > (SELECT ISNULL(MAX(id), 0) - 4 FROM dbo.handovers)) h
JOIN (VALUES (1, N'DEMO-CNT-008'), (2, N'DEMO-CNT-009'), (3, N'DEMO-CNT-010'), (4, N'DEMO-CNT-011')) m(rn, code) ON m.rn = h.rn
JOIN dbo.parcel_containers c ON c.container_code = m.code;

/* two more parcels on the road (one per direction) so inbound has work at both hubs */
INSERT dbo.handovers (from_hub_id, to_hub_id, sender_employee_id, receiver_driver_id, expected_containers, accepted_containers, status, created_at, accepted_at)
VALUES (@hn,  @hcm, @staffHnProfile,  @drvHn,   1, 1, N'IN_TRANSIT', DATEADD(HOUR, -3, SYSDATETIME()), DATEADD(HOUR, -2, SYSDATETIME())),
       (@hcm, @hn,  @staffHcmProfile, @drvHcm2, 1, 1, N'IN_TRANSIT', DATEADD(HOUR, -3, SYSDATETIME()), DATEADD(HOUR, -2, SYSDATETIME()));
DECLARE @h6 BIGINT = (SELECT TOP 1 id FROM dbo.handovers WHERE status = N'IN_TRANSIT' AND from_hub_id = @hn  AND to_hub_id = @hcm AND transport_trip_id IS NULL ORDER BY id DESC);
DECLARE @h7 BIGINT = (SELECT TOP 1 id FROM dbo.handovers WHERE status = N'IN_TRANSIT' AND from_hub_id = @hcm AND to_hub_id = @hn  AND transport_trip_id IS NULL ORDER BY id DESC);
DECLARE @v3 BIGINT = (SELECT id FROM dbo.vehicles WHERE plate_number = N'29C-DEMO2');
DECLARE @v4 BIGINT = (SELECT id FROM dbo.vehicles WHERE plate_number = N'51C-DEMO2');
INSERT dbo.transport_trips (trip_code, origin_hub_id, destination_hub_id, driver_user_id, vehicle_id, departure_time, status, handover_id, accepted_at, started_at, accepted_by_user_id, started_by_user_id)
VALUES (N'DEMO-TRIP-005', @hn,  @hcm, @drvHn,   @v3, DATEADD(HOUR, -2, SYSDATETIME()), N'IN_TRANSIT', @h6, DATEADD(HOUR, -2, SYSDATETIME()), DATEADD(HOUR, -2, SYSDATETIME()), @drvHn,   @drvHn),
       (N'DEMO-TRIP-006', @hcm, @hn,  @drvHcm2, @v4, DATEADD(HOUR, -2, SYSDATETIME()), N'IN_TRANSIT', @h7, DATEADD(HOUR, -2, SYSDATETIME()), DATEADD(HOUR, -2, SYSDATETIME()), @drvHcm2, @drvHcm2);
UPDATE dbo.handovers SET transport_trip_id = (SELECT id FROM dbo.transport_trips WHERE trip_code = N'DEMO-TRIP-005') WHERE id = @h6;
UPDATE dbo.handovers SET transport_trip_id = (SELECT id FROM dbo.transport_trips WHERE trip_code = N'DEMO-TRIP-006') WHERE id = @h7;
INSERT dbo.handover_containers (handover_id, container_id, status, scanned_at)
SELECT @h6, id, N'LOADED', DATEADD(HOUR, -3, SYSDATETIME()) FROM dbo.parcel_containers WHERE container_code = N'DEMO-CNT-012'
UNION ALL
SELECT @h7, id, N'LOADED', DATEADD(HOUR, -3, SYSDATETIME()) FROM dbo.parcel_containers WHERE container_code = N'DEMO-CNT-013';

COMMIT TRANSACTION;
END
