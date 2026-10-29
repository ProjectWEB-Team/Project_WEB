/* Demo-only follow-up to R__demo_c: fixes the handover senders (they must be employee profiles, not user ids),
   adds a second HCM driver and one handover nobody has accepted yet, so a manager can reassign its driver.
   Safe to run again: every step checks what already exists. */
SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @password NVARCHAR(255) = (SELECT TOP 1 password FROM dbo.users WHERE username = N'demo_sender');
DECLARE @hcm BIGINT = (SELECT id FROM dbo.hubs WHERE code = N'LOCAL2025-P79');
DECLARE @south BIGINT = (SELECT id FROM dbo.hubs WHERE code = N'LOCAL2025-SOUTH');

IF @password IS NOT NULL AND @hcm IS NOT NULL AND @south IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE username = N'demo_driver_hcm2')
        INSERT dbo.users (username, password, full_name, phone, email, role, status, phone_verified_at, email_verified_at)
        VALUES (N'demo_driver_hcm2', @password, N'Demo Tài xế TP.HCM 2', N'+84900000115', N'demo_driver_hcm2@example.test',
                N'ROLE_LINEHAUL_DRIVER', N'ACTIVE', SYSDATETIME(), SYSDATETIME());
    INSERT dbo.employee_profiles (user_id, employee_code, hub_id, position, status)
    SELECT u.id, N'DEMO_DRIVER_HCM2', @hcm, N'LINEHAUL_DRIVER', N'ACTIVE'
    FROM dbo.users u WHERE u.username = N'demo_driver_hcm2'
      AND NOT EXISTS (SELECT 1 FROM dbo.employee_profiles p WHERE p.user_id = u.id);

    /* R__demo_c stored user ids where the column points at employee profiles */
    UPDATE h SET sender_employee_id = p.id
    FROM dbo.handovers h
    JOIN dbo.transport_trips t ON t.handover_id = h.id AND t.trip_code IN (N'DEMO-TRIP-001', N'DEMO-TRIP-002')
    JOIN dbo.users su ON su.username = CASE t.trip_code WHEN N'DEMO-TRIP-001' THEN N'demo_hub_hcm' ELSE N'demo_hub_hn' END
    JOIN dbo.employee_profiles p ON p.user_id = su.id
    WHERE h.sender_employee_id <> p.id;

    IF EXISTS (SELECT 1 FROM dbo.shipments WHERE tracking_number = N'UTXDEMO0004')
       AND NOT EXISTS (SELECT 1 FROM dbo.parcel_containers WHERE container_code = N'DEMO-CNT-003')
    BEGIN
        DECLARE @staff BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_hub_hcm');
        DECLARE @staffProfile BIGINT = (SELECT id FROM dbo.employee_profiles WHERE user_id = @staff);
        DECLARE @driver BIGINT = (SELECT id FROM dbo.users WHERE username = N'demo_driver_hcm');
        INSERT dbo.parcel_containers (container_code, origin_hub_id, destination_hub_id, status, created_by, created_at)
        VALUES (N'DEMO-CNT-003', @hcm, @south, N'OPEN', @staff, DATEADD(MINUTE, -30, SYSDATETIME()));
        DECLARE @box BIGINT = (SELECT id FROM dbo.parcel_containers WHERE container_code = N'DEMO-CNT-003');
        INSERT dbo.container_items (container_id, shipment_id, status, scanned_at)
        SELECT @box, id, N'IN_CONTAINER', DATEADD(MINUTE, -25, SYSDATETIME()) FROM dbo.shipments WHERE tracking_number = N'UTXDEMO0004';
        UPDATE dbo.parcel_containers SET status = N'SEALED' WHERE id = @box;
        INSERT dbo.handovers (from_hub_id, to_hub_id, sender_employee_id, receiver_driver_id, expected_containers, accepted_containers, status, created_at)
        VALUES (@hcm, @south, @staffProfile, @driver, 1, 0, N'PENDING_ACCEPTANCE', DATEADD(MINUTE, -20, SYSDATETIME()));
        DECLARE @handover BIGINT = SCOPE_IDENTITY();
        INSERT dbo.handover_containers (handover_id, container_id, status) VALUES (@handover, @box, N'EXPECTED');
    END
END
COMMIT TRANSACTION;
