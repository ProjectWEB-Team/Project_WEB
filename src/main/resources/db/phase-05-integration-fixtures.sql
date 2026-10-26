/* Dedicated test DB only. Caller sets SESSION_CONTEXT(N'phase05_fixture') to p05 + 12 hex chars.
   No business state is manufactured: only actors, hub topology, pricing, routing and fleet. */
DECLARE @fixture NVARCHAR(40) = CAST(SESSION_CONTEXT(N'phase05_fixture') AS NVARCHAR(40));
IF DB_NAME() NOT LIKE N'uteexpress[_]phase05[_]test[_]%' OR @fixture IS NULL OR LEN(@fixture) <> 15
   OR LEFT(@fixture,3) <> N'p05' OR SUBSTRING(@fixture,4,12) LIKE N'%[^0-9a-f]%'
    THROW 51100, 'Dedicated test database and generated fixture identity required.', 1;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @hubs TABLE (suffix NVARCHAR(2), level INT, type NVARCHAR(30));
INSERT @hubs VALUES (N'R',3,N'SOC'),(N'M',2,N'REGIONAL_HUB'),(N'O',1,N'DELIVERY_HUB'),(N'D',1,N'DELIVERY_HUB'),(N'X',1,N'DELIVERY_HUB');
INSERT dbo.hubs(code,name,level,type,region,province,district,status)
SELECT @fixture+N'_'+suffix,N'Phase05 '+suffix,level,type,N'SOUTH',@fixture+N'_'+suffix,@fixture+N'_'+suffix,N'ACTIVE'
FROM @hubs h WHERE NOT EXISTS (SELECT 1 FROM dbo.hubs existing WHERE existing.code=@fixture+N'_'+h.suffix);
UPDATE dbo.hubs SET parent_hub_id=(SELECT id FROM dbo.hubs WHERE code=@fixture+N'_R')
WHERE code IN (@fixture+N'_M',@fixture+N'_O',@fixture+N'_D');
DECLARE @actors TABLE (suffix NVARCHAR(2), role NVARCHAR(30), hub NVARCHAR(2), position NVARCHAR(30));
INSERT @actors VALUES (N'S',N'ROLE_USER',NULL,NULL),(N'O',N'ROLE_HUB_STAFF',N'O',N'GENERAL_STAFF'),
(N'M',N'ROLE_HUB_STAFF',N'M',N'GENERAL_STAFF'),(N'D',N'ROLE_HUB_STAFF',N'D',N'GENERAL_STAFF'),
(N'L',N'ROLE_LINEHAUL_DRIVER',N'O',N'LINEHAUL_DRIVER'),(N'P',N'ROLE_SHIPPER',N'D',N'DELIVERY_SHIPPER'),
(N'Q',N'ROLE_SHIPPER',N'O',N'DELIVERY_SHIPPER'),(N'G',N'ROLE_MANAGER',N'R',N'MANAGER'),
(N'X',N'ROLE_MANAGER',N'X',N'MANAGER');
INSERT dbo.users(username,password,full_name,phone,email,role,status)
SELECT @fixture+N'_'+suffix,N'!'+CONVERT(NVARCHAR(36),NEWID()),N'Phase05 '+suffix,
N'+849'+RIGHT(N'00000000'+CONVERT(NVARCHAR(8),ABS(CONVERT(BIGINT,CHECKSUM(@fixture+suffix)))%100000000),8),
LOWER(@fixture+N'_'+suffix+N'@example.test'),role,N'ACTIVE'
FROM @actors a WHERE NOT EXISTS(SELECT 1 FROM dbo.users u WHERE u.username=@fixture+N'_'+a.suffix);
INSERT dbo.employee_profiles(user_id,employee_code,hub_id,position,status)
SELECT u.id,u.username,h.id,a.position,N'ACTIVE' FROM @actors a
JOIN dbo.users u ON u.username=@fixture+N'_'+a.suffix JOIN dbo.hubs h ON h.code=@fixture+N'_'+a.hub
WHERE NOT EXISTS(SELECT 1 FROM dbo.employee_profiles p WHERE p.employee_code=u.username);
IF NOT EXISTS(SELECT 1 FROM dbo.pricing_policies WHERE service_code=@fixture)
INSERT dbo.pricing_policies(service_code,from_region,to_region,base_weight_g,base_price,extra_weight_step_g,extra_weight_price,insurance_rate_percent,cod_fee_percent,status)
VALUES(@fixture,N'SOUTH',N'SOUTH',1000,30000,500,5000,0,0,N'ACTIVE');
DECLARE @routes TABLE (origin NVARCHAR(2), target NVARCHAR(2), nextHub NVARCHAR(2));
INSERT @routes VALUES (N'O',N'D',N'M'),(N'M',N'D',N'D'),(N'D',N'O',N'M'),(N'M',N'O',N'O');
INSERT dbo.hub_routing_rules(current_hub_id,destination_region,destination_province,next_hub_id,status,created_at,updated_at)
SELECT o.id,N'SOUTH',@fixture+N'_'+r.target,n.id,N'ACTIVE',SYSDATETIME(),SYSDATETIME() FROM @routes r
JOIN dbo.hubs o ON o.code=@fixture+N'_'+r.origin JOIN dbo.hubs n ON n.code=@fixture+N'_'+r.nextHub
WHERE NOT EXISTS(SELECT 1 FROM dbo.hub_routing_rules x WHERE x.current_hub_id=o.id AND x.destination_province=@fixture+N'_'+r.target);
IF NOT EXISTS(SELECT 1 FROM dbo.vehicles WHERE plate_number=@fixture)
INSERT dbo.vehicles(plate_number,vehicle_type,capacity,status) VALUES(@fixture,N'TRUCK',1000,N'AVAILABLE');
COMMIT TRANSACTION;
