/* Demo-only extra data so every hub screen has rows: counter intake, inbound queue, sorter queue, outbound queue,
   incidents, a routing exception and operation reports. Runs after R__demo_c / R__demo_d.
   Inserts once, keyed on tracking number UTXDEMO0018; to rebuild, reset the database. */
IF NOT EXISTS (SELECT 1 FROM dbo.shipments WHERE tracking_number = N'UTXDEMO0018')
AND EXISTS (SELECT 1 FROM dbo.shipments WHERE tracking_number = N'UTXDEMO0017')
AND EXISTS (SELECT 1 FROM dbo.users WHERE username = N'demo_driver_hcm2')
BEGIN
SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @sender BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_sender');
DECLARE @cust BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_customer');
DECLARE @shipHcm BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_shipper_hcm');
DECLARE @drvHcm2 BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_driver_hcm2');
DECLARE @drvHn BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_driver_hn');
DECLARE @hubStaffHcm BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_hub_hcm');
DECLARE @hubStaffHn BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_hub_hn');
DECLARE @manager BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_manager');
DECLARE @hcm BIGINT = (SELECT id FROM dbo.hubs WHERE code = N'LOCAL2025-P79');
DECLARE @hn BIGINT = (SELECT id FROM dbo.hubs WHERE code = N'LOCAL2025-P01');
DECLARE @south BIGINT = (SELECT id FROM dbo.hubs WHERE code = N'LOCAL2025-SOUTH');
DECLARE @north BIGINT = (SELECT id FROM dbo.hubs WHERE code = N'LOCAL2025-NORTH');
DECLARE @staffHcmProfile BIGINT = (SELECT id FROM dbo.employee_profiles WHERE user_id = @hubStaffHcm);
DECLARE @staffHnProfile BIGINT = (SELECT id FROM dbo.employee_profiles WHERE user_id = @hubStaffHn);

/* ---------- shipments ---------- */
DECLARE @s TABLE (trk NVARCHAR(30), sender BIGINT, st NVARCHAR(30), pickup NVARCHAR(20), cur BIGINT, org BIGINT, dst BIGINT,
                  fee DECIMAL(15,2), cod DECIMAL(15,2), age INT, recvName NVARCHAR(100), recvAddr NVARCHAR(255), recvProv NVARCHAR(100));
INSERT @s VALUES
 (N'UTXDEMO0018', @sender, N'IN_TRANSIT',          N'SELF_DROPOFF', @hcm, @hn,  @hcm, 54000, 0,      10, N'Nguyễn Hoài An',   N'12 Võ Văn Tần, Quận 3, TP.HCM',          N'TP. Hồ Chí Minh'),
 (N'UTXDEMO0019', @cust,   N'IN_TRANSIT',          N'HOME_PICKUP',  @hcm, @hn,  @hcm, 59000, 200000, 10, N'Lê Quốc Bảo',       N'88 Nguyễn Thị Minh Khai, Quận 3, TP.HCM', N'TP. Hồ Chí Minh'),
 (N'UTXDEMO0020', @sender, N'IN_TRANSIT',          N'SELF_DROPOFF', @hn,  @hcm, @hn,  52000, 0,      9,  N'Trần Minh Châu',    N'40 Hàng Gai, Hoàn Kiếm, Hà Nội',         N'Hà Nội'),
 (N'UTXDEMO0021', @sender, N'AT_HUB',              N'SELF_DROPOFF', @hcm, @hcm, @hn,  51000, 0,      5,  N'Phạm Đức Dũng',     N'15 Kim Mã, Ba Đình, Hà Nội',             N'Hà Nội'),
 (N'UTXDEMO0022', @cust,   N'AT_HUB',              N'HOME_PICKUP',  @hcm, @hcm, @north, 57000, 120000, 5, N'Võ Thị Em',        N'7 Lê Hồng Phong, Hải Phòng',             N'Hải Phòng'),
 (N'UTXDEMO0023', @sender, N'AT_HUB',              N'SELF_DROPOFF', @hcm, @hcm, @hn,  50000, 0,      7,  N'Đinh Văn Giang',    N'60 Tây Sơn, Đống Đa, Hà Nội',            N'Hà Nội'),
 (N'UTXDEMO0024', @sender, N'PICKED_UP',           N'HOME_PICKUP',  NULL,  @hcm, @hn,  49000, 0,      4,  N'Hồ Thu Hà',         N'3 Phạm Ngọc Thạch, Đống Đa, Hà Nội',     N'Hà Nội'),
 (N'UTXDEMO0025', @sender, N'WAITING_FOR_HANDOVER', N'SELF_DROPOFF', NULL, @hcm, @hn,  46000, 0,      2,  N'Lý Văn Hải',        N'21 Cầu Giấy, Cầu Giấy, Hà Nội',          N'Hà Nội'),
 (N'UTXDEMO0026', @cust,   N'WAITING_FOR_HANDOVER', N'SELF_DROPOFF', NULL, @hcm, @north, 55000, 0,    2,  N'Mạc Thị Hoa',       N'9 Điện Biên Phủ, Hải Phòng',             N'Hải Phòng'),
 (N'UTXDEMO0027', @sender, N'WAITING_FOR_HANDOVER', N'HOME_PICKUP',  NULL, @hcm, @hn,  48000, 150000, 3, N'Dương Quang Khải',  N'55 Trần Duy Hưng, Cầu Giấy, Hà Nội',     N'Hà Nội'),
 (N'UTXDEMO0028', @sender, N'WAITING_FOR_HANDOVER', N'SELF_DROPOFF', NULL, @hn,  @hcm, 47000, 0,      2,  N'Quách Minh Long',   N'18 Pasteur, Quận 1, TP.HCM',             N'TP. Hồ Chí Minh'),
 (N'UTXDEMO0029', @sender, N'AT_HUB',              N'SELF_DROPOFF', @hn,  @hn,  @hcm, 53000, 0,      6,  N'Ung Thị Mỹ',        N'102 Cách Mạng Tháng 8, Quận 3, TP.HCM',  N'TP. Hồ Chí Minh');

INSERT dbo.shipments (tracking_number, sender_user_id, receiver_user_id, sender_name, sender_phone, sender_address,
        receiver_name, receiver_phone, receiver_address, receiver_province, receiver_district, pickup_method, fee_payer, inspection_policy,
        weight, declared_value, shipping_fee, cod_amount, return_fee, payment_status, origin_hub_id, destination_hub_id, current_hub_id,
        direction, current_status, on_hold, payment_deadline, waiting_for_handover_at, created_at,
        picked_up_at, destination_region, destination_province, service_code)
SELECT s.trk, s.sender, NULL, us.full_name, us.phone, N'25 Nguyễn Văn Cừ, Quận 5, TP.HCM',
       s.recvName, N'+8490000' + RIGHT(s.trk, 4), s.recvAddr, s.recvProv, N'',
       s.pickup, N'SENDER', N'INSPECTION_ONLY', 1500, s.cod + 500000, s.fee, s.cod, 0, N'PAID', s.org, s.dst, s.cur,
       N'FORWARD', s.st, 0, NULL,
       CASE WHEN s.st <> N'WAITING_FOR_PAYMENT' THEN DATEADD(HOUR, -s.age + 1, SYSDATETIME()) END,
       DATEADD(HOUR, -s.age, SYSDATETIME()),
       CASE WHEN s.st NOT IN (N'WAITING_FOR_HANDOVER') THEN DATEADD(HOUR, -s.age + 3, SYSDATETIME()) END,
       CASE WHEN s.dst IN (@hcm, @south) THEN N'SOUTH' ELSE N'NORTH' END, s.recvProv, N'STANDARD'
FROM @s s JOIN dbo.users us ON us.id = s.sender;

/* ---------- tracking events ---------- */
DECLARE @e TABLE (trk NVARCHAR(30), typ NVARCHAR(50), descr NVARCHAR(255), hoursAgo INT, hub BIGINT, actor BIGINT);
INSERT @e VALUES
 (N'UTXDEMO0018', N'ARRIVED_AT_HUB', N'Hàng đã nhập bưu cục Hà Nội', 9, @hn, @hubStaffHn),
 (N'UTXDEMO0018', N'BAG_CREATED', N'Hàng được đóng vào container', 6, @hn, @hubStaffHn),
 (N'UTXDEMO0018', N'LINEHAUL_TRIP_STARTED', N'Xe trung chuyển đã xuất phát đi bưu cục TP.HCM', 2, @hn, @drvHn),
 (N'UTXDEMO0019', N'ARRIVED_AT_HUB', N'Hàng đã nhập bưu cục Hà Nội', 9, @hn, @hubStaffHn),
 (N'UTXDEMO0019', N'BAG_CREATED', N'Hàng được đóng vào container', 6, @hn, @hubStaffHn),
 (N'UTXDEMO0019', N'LINEHAUL_TRIP_STARTED', N'Xe trung chuyển đã xuất phát đi bưu cục TP.HCM', 2, @hn, @drvHn),
 (N'UTXDEMO0020', N'ARRIVED_AT_HUB', N'Hàng đã nhập bưu cục TP.HCM', 8, @hcm, @hubStaffHcm),
 (N'UTXDEMO0020', N'BAG_CREATED', N'Hàng được đóng vào container', 5, @hcm, @hubStaffHcm),
 (N'UTXDEMO0020', N'LINEHAUL_TRIP_STARTED', N'Xe trung chuyển đã xuất phát đi bưu cục Hà Nội', 2, @hcm, @drvHcm2),
 (N'UTXDEMO0021', N'ARRIVED_AT_HUB', N'Hàng đã nhập bưu cục TP.HCM', 4, @hcm, @hubStaffHcm),
 (N'UTXDEMO0022', N'ARRIVED_AT_HUB', N'Hàng đã nhập bưu cục TP.HCM', 4, @hcm, @hubStaffHcm),
 (N'UTXDEMO0023', N'ARRIVED_AT_HUB', N'Hàng đã nhập bưu cục TP.HCM', 6, @hcm, @hubStaffHcm),
 (N'UTXDEMO0023', N'SORTING_COMPLETED', N'Đã phân loại: chuyển tiếp trung tâm SOC', 5, @hcm, @hubStaffHcm),
 (N'UTXDEMO0024', N'PICKED_UP', N'Shipper đã lấy hàng tại địa chỉ người gửi', 3, @hcm, @shipHcm),
 (N'UTXDEMO0029', N'ARRIVED_AT_HUB', N'Hàng đã nhập bưu cục Hà Nội', 5, @hn, @hubStaffHn);
INSERT dbo.shipment_events (shipment_id, hub_id, actor_user_id, event_type, description, created_at)
SELECT sh.id, e.hub, e.actor, e.typ, e.descr, DATEADD(HOUR, -e.hoursAgo, SYSDATETIME())
FROM @e e JOIN dbo.shipments sh ON sh.tracking_number = e.trk;

/* ---------- shipping-fee payments ---------- */
INSERT dbo.payments (shipment_id, payer_type, payment_type, amount, method, status, transaction_ref, paid_at, created_at)
SELECT sh.id, N'SENDER', N'SHIPPING_FEE', sh.shipping_fee,
       CASE WHEN RIGHT(sh.tracking_number, 1) IN (N'1', N'5', N'9') THEN N'CASH' ELSE N'VNPAY' END,
       N'PAID', N'DEMO-' + sh.tracking_number, DATEADD(MINUTE, 20, sh.created_at), sh.created_at
FROM dbo.shipments sh WHERE sh.tracking_number BETWEEN N'UTXDEMO0018' AND N'UTXDEMO0029';

/* ---------- shipper pickup task for the picked-up parcel ---------- */
INSERT dbo.shipment_assignments (shipment_id, assignee_user_id, assignment_type, status, assigned_at, accepted_at, completed_at)
SELECT sh.id, @shipHcm, N'PICKUP', N'COMPLETED', DATEADD(HOUR, -5, SYSDATETIME()), DATEADD(HOUR, -4, SYSDATETIME()), DATEADD(HOUR, -3, SYSDATETIME())
FROM dbo.shipments sh WHERE sh.tracking_number = N'UTXDEMO0024';
INSERT dbo.pickup_attempts (assignment_id, shipment_id, shipper_id, attempt_number, attempt_time, result, note)
SELECT sa.id, sa.shipment_id, sa.assignee_user_id, 1, sa.assigned_at, N'SUCCESS', N'Đã lấy hàng'
FROM dbo.shipment_assignments sa JOIN dbo.shipments sh ON sh.id = sa.shipment_id WHERE sh.tracking_number = N'UTXDEMO0024';

/* ---------- linehaul: two more trips on the road, so both big hubs have something to receive ---------- */
INSERT dbo.vehicles (plate_number, vehicle_type, capacity, status)
SELECT v.plate, N'TRUCK', 1000, N'AVAILABLE' FROM (VALUES (N'51C-DEMO2'), (N'29C-DEMO2')) v(plate)
WHERE NOT EXISTS (SELECT 1 FROM dbo.vehicles x WHERE x.plate_number = v.plate);
DECLARE @v3 BIGINT = (SELECT id FROM dbo.vehicles WHERE plate_number = N'29C-DEMO2');
DECLARE @v4 BIGINT = (SELECT id FROM dbo.vehicles WHERE plate_number = N'51C-DEMO2');

INSERT dbo.parcel_containers (container_code, origin_hub_id, destination_hub_id, status, created_by, created_at)
VALUES (N'DEMO-CNT-004', @hn,  @hcm, N'LOADED', @hubStaffHn,  DATEADD(HOUR, -6, SYSDATETIME())),
       (N'DEMO-CNT-005', @hcm, @hn,  N'LOADED', @hubStaffHcm, DATEADD(HOUR, -5, SYSDATETIME())),
       (N'DEMO-CNT-006', @hcm, @hn,  N'OPEN',   @hubStaffHcm, DATEADD(MINUTE, -45, SYSDATETIME())),
       (N'DEMO-CNT-007', @hcm, @north, N'SEALED', @hubStaffHcm, DATEADD(MINUTE, -90, SYSDATETIME()));
DECLARE @c4 BIGINT = (SELECT id FROM dbo.parcel_containers WHERE container_code = N'DEMO-CNT-004');
DECLARE @c5 BIGINT = (SELECT id FROM dbo.parcel_containers WHERE container_code = N'DEMO-CNT-005');
DECLARE @c6 BIGINT = (SELECT id FROM dbo.parcel_containers WHERE container_code = N'DEMO-CNT-006');
DECLARE @c7 BIGINT = (SELECT id FROM dbo.parcel_containers WHERE container_code = N'DEMO-CNT-007');
INSERT dbo.container_items (container_id, shipment_id, status, scanned_at)
SELECT CASE sh.tracking_number WHEN N'UTXDEMO0018' THEN @c4 WHEN N'UTXDEMO0019' THEN @c4 WHEN N'UTXDEMO0020' THEN @c5
                               WHEN N'UTXDEMO0023' THEN @c6 ELSE @c7 END,
       sh.id, N'IN_CONTAINER', DATEADD(HOUR, -2, SYSDATETIME())
FROM dbo.shipments sh WHERE sh.tracking_number IN (N'UTXDEMO0018', N'UTXDEMO0019', N'UTXDEMO0020', N'UTXDEMO0023', N'UTXDEMO0022');

INSERT dbo.handovers (from_hub_id, to_hub_id, sender_employee_id, receiver_driver_id, expected_containers, accepted_containers, status, created_at, accepted_at)
VALUES (@hn,  @hcm, @staffHnProfile,  @drvHn,   1, 1, N'IN_TRANSIT', DATEADD(HOUR, -3, SYSDATETIME()), DATEADD(HOUR, -2, SYSDATETIME())),
       (@hcm, @hn,  @staffHcmProfile, @drvHcm2, 1, 1, N'IN_TRANSIT', DATEADD(HOUR, -3, SYSDATETIME()), DATEADD(HOUR, -2, SYSDATETIME()));
DECLARE @h4 BIGINT = (SELECT TOP 1 id FROM dbo.handovers WHERE from_hub_id = @hn  AND to_hub_id = @hcm AND receiver_driver_id = @drvHn   ORDER BY id DESC);
DECLARE @h5 BIGINT = (SELECT TOP 1 id FROM dbo.handovers WHERE from_hub_id = @hcm AND to_hub_id = @hn  AND receiver_driver_id = @drvHcm2 ORDER BY id DESC);
INSERT dbo.transport_trips (trip_code, origin_hub_id, destination_hub_id, driver_user_id, vehicle_id, departure_time, status, handover_id, accepted_at, started_at, accepted_by_user_id, started_by_user_id)
VALUES (N'DEMO-TRIP-003', @hn,  @hcm, @drvHn,   @v3, DATEADD(HOUR, -2, SYSDATETIME()), N'IN_TRANSIT', @h4, DATEADD(HOUR, -2, SYSDATETIME()), DATEADD(HOUR, -2, SYSDATETIME()), @drvHn,   @drvHn),
       (N'DEMO-TRIP-004', @hcm, @hn,  @drvHcm2, @v4, DATEADD(HOUR, -2, SYSDATETIME()), N'IN_TRANSIT', @h5, DATEADD(HOUR, -2, SYSDATETIME()), DATEADD(HOUR, -2, SYSDATETIME()), @drvHcm2, @drvHcm2);
UPDATE dbo.handovers SET transport_trip_id = (SELECT id FROM dbo.transport_trips WHERE trip_code = N'DEMO-TRIP-003') WHERE id = @h4;
UPDATE dbo.handovers SET transport_trip_id = (SELECT id FROM dbo.transport_trips WHERE trip_code = N'DEMO-TRIP-004') WHERE id = @h5;
INSERT dbo.handover_containers (handover_id, container_id, status, scanned_at)
VALUES (@h4, @c4, N'LOADED', DATEADD(HOUR, -3, SYSDATETIME())), (@h5, @c5, N'LOADED', DATEADD(HOUR, -3, SYSDATETIME()));

/* ---------- routing exception, incidents, operation reports ---------- */
INSERT dbo.routing_exceptions (shipment_id, hub_id, exception_type, status, opened_by_user_id, opened_at)
SELECT sh.id, @hcm, N'ROUTE_LOOKUP_BLOCKED', N'OPEN', @hubStaffHcm, DATEADD(HOUR, -4, SYSDATETIME())
FROM dbo.shipments sh WHERE sh.tracking_number = N'UTXDEMO0022';

INSERT dbo.incidents (shipment_id, hub_id, type, description, reported_by, status, created_at)
SELECT sh.id, @hn, N'MISSING_PARCEL', N'Bao hàng thiếu 1 kiện khi đối chiếu tại bưu cục Hà Nội', @hubStaffHn, N'OPEN', DATEADD(HOUR, -7, SYSDATETIME())
FROM dbo.shipments sh WHERE sh.tracking_number = N'UTXDEMO0007'
UNION ALL
SELECT sh.id, @hcm, N'DAMAGED_PARCEL', N'Kiện bị móp góc khi nhập kho, đã chụp ảnh', @hubStaffHcm, N'INVESTIGATING', DATEADD(HOUR, -5, SYSDATETIME())
FROM dbo.shipments sh WHERE sh.tracking_number = N'UTXDEMO0021'
UNION ALL
SELECT sh.id, @hn, N'WRONG_ROUTE', N'Kiện gửi nhầm hướng, đã chuyển lại đúng tuyến', @hubStaffHn, N'RESOLVED', DATEADD(HOUR, -30, SYSDATETIME())
FROM dbo.shipments sh WHERE sh.tracking_number = N'UTXDEMO0029';

INSERT dbo.operation_reports (hub_id, manager_user_id, report_date, inbound_count, outbound_count, delivered_count, failed_count, backlog_count,
                              missing_count, damaged_count, problem, action, status, created_at)
VALUES (@hcm, @manager, CAST(DATEADD(DAY, -1, SYSDATETIME()) AS DATE), 18, 15, 12, 1, 3, 0, 1, N'Một kiện móp góc khi nhập kho', N'Đã chụp ảnh, liên hệ người gửi', N'SUBMITTED', DATEADD(HOUR, -18, SYSDATETIME())),
       (@hn,  @manager, CAST(DATEADD(DAY, -1, SYSDATETIME()) AS DATE), 14, 12, 10, 2, 2, 1, 0, N'Bao hàng thiếu 1 kiện',         N'Đã mở điều tra kiện thất lạc',  N'ACKNOWLEDGED', DATEADD(HOUR, -17, SYSDATETIME())),
       (@hcm, @manager, CAST(SYSDATETIME() AS DATE),                    6,  4,  3,  0, 2, 0, 0, NULL,                             NULL,                             N'DRAFT', DATEADD(HOUR, -1, SYSDATETIME()));

COMMIT TRANSACTION;
END
