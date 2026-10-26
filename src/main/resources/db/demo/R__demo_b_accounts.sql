/* Demo-only accounts (demo profile; never in prod). Every account's password is Demo@12345 (BCrypt below).
   Inserts missing rows only; existing users, profiles, wallets and vehicles are left unchanged. */
DECLARE @password NVARCHAR(255) = N'$2a$10$hVKRAkvCfZqYoGSi/dK5ded1.LUAb7Qgw6V1zCAWfMQSeC17IygbW';
DECLARE @accounts TABLE (username NVARCHAR(50), full_name NVARCHAR(100), phone NVARCHAR(20), role NVARCHAR(50),
                         hub_code NVARCHAR(50), position NVARCHAR(50));
INSERT @accounts VALUES
 (N'demo_admin',       N'Demo Quản trị',              N'+84900000101', N'ROLE_ADMIN',           NULL,                 NULL),
 (N'demo_manager',     N'Demo Quản lý',               N'+84900000102', N'ROLE_MANAGER',         N'LOCAL2025-SOC',     N'MANAGER'),
 (N'demo_hub_hcm',     N'Demo NV bưu cục TP.HCM',     N'+84900000103', N'ROLE_HUB_STAFF',       N'LOCAL2025-P79',     N'GENERAL_STAFF'),
 (N'demo_hub_south',   N'Demo NV hub miền Nam',       N'+84900000104', N'ROLE_HUB_STAFF',       N'LOCAL2025-SOUTH',   N'GENERAL_STAFF'),
 (N'demo_hub_soc',     N'Demo NV trung tâm SOC',      N'+84900000105', N'ROLE_HUB_STAFF',       N'LOCAL2025-SOC',     N'GENERAL_STAFF'),
 (N'demo_hub_north',   N'Demo NV hub miền Bắc',       N'+84900000106', N'ROLE_HUB_STAFF',       N'LOCAL2025-NORTH',   N'GENERAL_STAFF'),
 (N'demo_hub_hn',      N'Demo NV bưu cục Hà Nội',     N'+84900000107', N'ROLE_HUB_STAFF',       N'LOCAL2025-P01',     N'GENERAL_STAFF'),
 (N'demo_shipper_hcm', N'Demo Shipper TP.HCM',        N'+84900000108', N'ROLE_SHIPPER',         N'LOCAL2025-P79',     N'DELIVERY_SHIPPER'),
 (N'demo_shipper_hn',  N'Demo Shipper Hà Nội',        N'+84900000109', N'ROLE_SHIPPER',         N'LOCAL2025-P01',     N'DELIVERY_SHIPPER'),
 (N'demo_driver_hcm',  N'Demo Tài xế TP.HCM',         N'+84900000110', N'ROLE_LINEHAUL_DRIVER', N'LOCAL2025-P79',     N'LINEHAUL_DRIVER'),
 (N'demo_driver_hn',   N'Demo Tài xế Hà Nội',         N'+84900000111', N'ROLE_LINEHAUL_DRIVER', N'LOCAL2025-P01',     N'LINEHAUL_DRIVER'),
 (N'demo_sender',      N'Demo Người gửi',             N'+84900000112', N'ROLE_USER',            NULL,                 NULL),
 (N'demo_receiver',    N'Demo Người nhận',            N'+84900000113', N'ROLE_USER',            NULL,                 NULL);

INSERT dbo.users (username, password, full_name, phone, email, role, status, phone_verified_at)
SELECT a.username, @password, a.full_name, a.phone, a.username + N'@example.test', a.role, N'ACTIVE', SYSDATETIME()
FROM @accounts a
WHERE NOT EXISTS (SELECT 1 FROM dbo.users u WHERE u.username = a.username OR u.phone = a.phone);

INSERT dbo.employee_profiles (user_id, employee_code, hub_id, position, status)
SELECT u.id, UPPER(a.username), h.id, a.position, N'ACTIVE'
FROM @accounts a JOIN dbo.users u ON u.username = a.username JOIN dbo.hubs h ON h.code = a.hub_code
WHERE NOT EXISTS (SELECT 1 FROM dbo.employee_profiles p WHERE p.user_id = u.id);

INSERT dbo.wallets (user_id, balance, locked_balance, status)
SELECT u.id, 0, 0, N'ACTIVE'
FROM @accounts a JOIN dbo.users u ON u.username = a.username
WHERE a.role IN (N'ROLE_USER', N'ROLE_SHIPPER') AND NOT EXISTS (SELECT 1 FROM dbo.wallets w WHERE w.user_id = u.id);

INSERT dbo.vehicles (plate_number, vehicle_type, capacity, status)
SELECT v.plate, N'TRUCK', 1000, N'AVAILABLE'
FROM (VALUES (N'51C-DEMO1'), (N'29C-DEMO1')) v(plate)
WHERE NOT EXISTS (SELECT 1 FROM dbo.vehicles x WHERE x.plate_number = v.plate);
