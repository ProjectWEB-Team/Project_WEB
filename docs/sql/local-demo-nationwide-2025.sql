/* Opt-in LOCAL demo topology for the 2025 nationwide address catalog.
   34 provincial delivery hubs, 3 regional hubs, 1 SOC. These are simulated facilities.
   A NULL delivery-hub district explicitly means province-wide demo coverage.
   Run only against local ute_express. Does not alter existing non-LOCAL2025 data. */
SET XACT_ABORT ON;
IF DB_NAME()<>N'ute_express' THROW 51210,'Local ute_express required.',1;
BEGIN TRY
BEGIN TRANSACTION;
DECLARE @nodes TABLE(code NVARCHAR(50),name NVARCHAR(150),level INT,type NVARCHAR(30),region NVARCHAR(100),province NVARCHAR(100),parent_code NVARCHAR(50));
INSERT @nodes VALUES
(N'LOCAL2025-SOC',N'Mô phỏng - SOC toàn quốc',3,N'SOC',N'SOUTH',N'Thành phố Hồ Chí Minh',NULL),
(N'LOCAL2025-NORTH',N'Mô phỏng - Hub miền Bắc',2,N'REGIONAL_HUB',N'NORTH',N'Thành phố Hà Nội',N'LOCAL2025-SOC'),
(N'LOCAL2025-CENTRAL',N'Mô phỏng - Hub miền Trung',2,N'REGIONAL_HUB',N'CENTRAL',N'Thành phố Đà Nẵng',N'LOCAL2025-SOC'),
(N'LOCAL2025-SOUTH',N'Mô phỏng - Hub miền Nam',2,N'REGIONAL_HUB',N'SOUTH',N'Thành phố Hồ Chí Minh',N'LOCAL2025-SOC'),
(N'LOCAL2025-P92',N'Mô phỏng - Thành phố Cần Thơ',1,N'DELIVERY_HUB',N'SOUTH',N'Thành phố Cần Thơ',N'LOCAL2025-SOUTH'),
(N'LOCAL2025-P48',N'Mô phỏng - Thành phố Đà Nẵng',1,N'DELIVERY_HUB',N'CENTRAL',N'Thành phố Đà Nẵng',N'LOCAL2025-CENTRAL'),
(N'LOCAL2025-P01',N'Mô phỏng - Thành phố Hà Nội',1,N'DELIVERY_HUB',N'NORTH',N'Thành phố Hà Nội',N'LOCAL2025-NORTH'),
(N'LOCAL2025-P31',N'Mô phỏng - Thành phố Hải Phòng',1,N'DELIVERY_HUB',N'NORTH',N'Thành phố Hải Phòng',N'LOCAL2025-NORTH'),
(N'LOCAL2025-P79',N'Mô phỏng - Thành phố Hồ Chí Minh',1,N'DELIVERY_HUB',N'SOUTH',N'Thành phố Hồ Chí Minh',N'LOCAL2025-SOUTH'),
(N'LOCAL2025-P46',N'Mô phỏng - Thành phố Huế',1,N'DELIVERY_HUB',N'CENTRAL',N'Thành phố Huế',N'LOCAL2025-CENTRAL'),
(N'LOCAL2025-P91',N'Mô phỏng - Tỉnh An Giang',1,N'DELIVERY_HUB',N'SOUTH',N'Tỉnh An Giang',N'LOCAL2025-SOUTH'),
(N'LOCAL2025-P24',N'Mô phỏng - Tỉnh Bắc Ninh',1,N'DELIVERY_HUB',N'NORTH',N'Tỉnh Bắc Ninh',N'LOCAL2025-NORTH'),
(N'LOCAL2025-P96',N'Mô phỏng - Tỉnh Cà Mau',1,N'DELIVERY_HUB',N'SOUTH',N'Tỉnh Cà Mau',N'LOCAL2025-SOUTH'),
(N'LOCAL2025-P04',N'Mô phỏng - Tỉnh Cao Bằng',1,N'DELIVERY_HUB',N'NORTH',N'Tỉnh Cao Bằng',N'LOCAL2025-NORTH'),
(N'LOCAL2025-P66',N'Mô phỏng - Tỉnh Đắk Lắk',1,N'DELIVERY_HUB',N'CENTRAL',N'Tỉnh Đắk Lắk',N'LOCAL2025-CENTRAL'),
(N'LOCAL2025-P11',N'Mô phỏng - Tỉnh Điện Biên',1,N'DELIVERY_HUB',N'NORTH',N'Tỉnh Điện Biên',N'LOCAL2025-NORTH'),
(N'LOCAL2025-P75',N'Mô phỏng - Tỉnh Đồng Nai',1,N'DELIVERY_HUB',N'SOUTH',N'Tỉnh Đồng Nai',N'LOCAL2025-SOUTH'),
(N'LOCAL2025-P82',N'Mô phỏng - Tỉnh Đồng Tháp',1,N'DELIVERY_HUB',N'SOUTH',N'Tỉnh Đồng Tháp',N'LOCAL2025-SOUTH'),
(N'LOCAL2025-P52',N'Mô phỏng - Tỉnh Gia Lai',1,N'DELIVERY_HUB',N'CENTRAL',N'Tỉnh Gia Lai',N'LOCAL2025-CENTRAL'),
(N'LOCAL2025-P42',N'Mô phỏng - Tỉnh Hà Tĩnh',1,N'DELIVERY_HUB',N'CENTRAL',N'Tỉnh Hà Tĩnh',N'LOCAL2025-CENTRAL'),
(N'LOCAL2025-P33',N'Mô phỏng - Tỉnh Hưng Yên',1,N'DELIVERY_HUB',N'NORTH',N'Tỉnh Hưng Yên',N'LOCAL2025-NORTH'),
(N'LOCAL2025-P56',N'Mô phỏng - Tỉnh Khánh Hòa',1,N'DELIVERY_HUB',N'CENTRAL',N'Tỉnh Khánh Hòa',N'LOCAL2025-CENTRAL'),
(N'LOCAL2025-P12',N'Mô phỏng - Tỉnh Lai Châu',1,N'DELIVERY_HUB',N'NORTH',N'Tỉnh Lai Châu',N'LOCAL2025-NORTH'),
(N'LOCAL2025-P20',N'Mô phỏng - Tỉnh Lạng Sơn',1,N'DELIVERY_HUB',N'NORTH',N'Tỉnh Lạng Sơn',N'LOCAL2025-NORTH'),
(N'LOCAL2025-P15',N'Mô phỏng - Tỉnh Lào Cai',1,N'DELIVERY_HUB',N'NORTH',N'Tỉnh Lào Cai',N'LOCAL2025-NORTH'),
(N'LOCAL2025-P68',N'Mô phỏng - Tỉnh Lâm Đồng',1,N'DELIVERY_HUB',N'CENTRAL',N'Tỉnh Lâm Đồng',N'LOCAL2025-CENTRAL'),
(N'LOCAL2025-P40',N'Mô phỏng - Tỉnh Nghệ An',1,N'DELIVERY_HUB',N'CENTRAL',N'Tỉnh Nghệ An',N'LOCAL2025-CENTRAL'),
(N'LOCAL2025-P37',N'Mô phỏng - Tỉnh Ninh Bình',1,N'DELIVERY_HUB',N'NORTH',N'Tỉnh Ninh Bình',N'LOCAL2025-NORTH'),
(N'LOCAL2025-P25',N'Mô phỏng - Tỉnh Phú Thọ',1,N'DELIVERY_HUB',N'NORTH',N'Tỉnh Phú Thọ',N'LOCAL2025-NORTH'),
(N'LOCAL2025-P51',N'Mô phỏng - Tỉnh Quảng Ngãi',1,N'DELIVERY_HUB',N'CENTRAL',N'Tỉnh Quảng Ngãi',N'LOCAL2025-CENTRAL'),
(N'LOCAL2025-P22',N'Mô phỏng - Tỉnh Quảng Ninh',1,N'DELIVERY_HUB',N'NORTH',N'Tỉnh Quảng Ninh',N'LOCAL2025-NORTH'),
(N'LOCAL2025-P44',N'Mô phỏng - Tỉnh Quảng Trị',1,N'DELIVERY_HUB',N'CENTRAL',N'Tỉnh Quảng Trị',N'LOCAL2025-CENTRAL'),
(N'LOCAL2025-P14',N'Mô phỏng - Tỉnh Sơn La',1,N'DELIVERY_HUB',N'NORTH',N'Tỉnh Sơn La',N'LOCAL2025-NORTH'),
(N'LOCAL2025-P80',N'Mô phỏng - Tỉnh Tây Ninh',1,N'DELIVERY_HUB',N'SOUTH',N'Tỉnh Tây Ninh',N'LOCAL2025-SOUTH'),
(N'LOCAL2025-P19',N'Mô phỏng - Tỉnh Thái Nguyên',1,N'DELIVERY_HUB',N'NORTH',N'Tỉnh Thái Nguyên',N'LOCAL2025-NORTH'),
(N'LOCAL2025-P38',N'Mô phỏng - Tỉnh Thanh Hóa',1,N'DELIVERY_HUB',N'CENTRAL',N'Tỉnh Thanh Hóa',N'LOCAL2025-CENTRAL'),
(N'LOCAL2025-P08',N'Mô phỏng - Tỉnh Tuyên Quang',1,N'DELIVERY_HUB',N'NORTH',N'Tỉnh Tuyên Quang',N'LOCAL2025-NORTH'),
(N'LOCAL2025-P86',N'Mô phỏng - Tỉnh Vĩnh Long',1,N'DELIVERY_HUB',N'SOUTH',N'Tỉnh Vĩnh Long',N'LOCAL2025-SOUTH');
IF EXISTS(SELECT 1 FROM @nodes n JOIN dbo.hubs h WITH(UPDLOCK,HOLDLOCK) ON h.code=n.code WHERE h.level<>n.level OR h.type<>n.type OR h.region<>n.region OR h.province<>n.province OR h.status<>N'ACTIVE' OR h.district IS NOT NULL)
 THROW 51211,'Conflicting LOCAL2025 hub configuration; no changes applied.',1;
IF EXISTS(SELECT 1 FROM @nodes n JOIN dbo.hubs h WITH(UPDLOCK,HOLDLOCK) ON h.province=n.province WHERE n.level=1 AND h.type=N'DELIVERY_HUB' AND h.status=N'ACTIVE' AND h.district IS NULL AND h.code<>n.code)
 THROW 51212,'Province already has another active province-wide delivery hub.',1;
DECLARE @level INT=3;
WHILE @level>=1
BEGIN
 INSERT dbo.hubs(code,name,level,type,parent_hub_id,region,province,status)
 SELECT n.code,n.name,n.level,n.type,p.id,n.region,n.province,N'ACTIVE'
 FROM @nodes n LEFT JOIN dbo.hubs p ON p.code=n.parent_code
 WHERE n.level=@level AND NOT EXISTS(SELECT 1 FROM dbo.hubs h WITH(UPDLOCK,HOLDLOCK) WHERE h.code=n.code);
 SET @level=@level-1;
END;
IF EXISTS(SELECT 1 FROM @nodes n JOIN dbo.hubs h ON h.code=n.code LEFT JOIN dbo.hubs p ON p.code=n.parent_code WHERE ISNULL(h.parent_hub_id,-1)<>ISNULL(p.id,-1))
 THROW 51213,'Conflicting LOCAL2025 hierarchy; no changes applied.',1;
DECLARE @routes TABLE(current_hub_id BIGINT,destination_region NVARCHAR(100),destination_province NVARCHAR(100),next_hub_id BIGINT);
/* First mile: delivery hub to its regional parent. */
INSERT @routes
SELECT h.id,r.region,NULL,h.parent_hub_id FROM @nodes n JOIN dbo.hubs h ON h.code=n.code
CROSS JOIN (VALUES(N'NORTH'),(N'CENTRAL'),(N'SOUTH'))r(region) WHERE n.level=1;
/* In-region regional routing to final provincial hub. */
INSERT @routes SELECT h.parent_hub_id,h.region,h.province,h.id FROM @nodes n JOIN dbo.hubs h ON h.code=n.code WHERE n.level=1;
/* Other regions go through the SOC. */
INSERT @routes SELECT h.id,r.region,NULL,h.parent_hub_id FROM @nodes n JOIN dbo.hubs h ON h.code=n.code
CROSS JOIN (VALUES(N'NORTH'),(N'CENTRAL'),(N'SOUTH'))r(region) WHERE n.level=2 AND h.region<>r.region;
INSERT @routes SELECT h.parent_hub_id,h.region,NULL,h.id FROM @nodes n JOIN dbo.hubs h ON h.code=n.code WHERE n.level=2;
IF EXISTS(SELECT 1 FROM @routes r JOIN dbo.hub_routing_rules x WITH(UPDLOCK,HOLDLOCK) ON x.current_hub_id=r.current_hub_id AND x.destination_region=r.destination_region AND ISNULL(x.destination_province,N'')=ISNULL(r.destination_province,N'') AND x.status=N'ACTIVE' WHERE x.next_hub_id<>r.next_hub_id)
 THROW 51214,'Conflicting LOCAL2025 routing; no changes applied.',1;
INSERT dbo.hub_routing_rules(current_hub_id,destination_region,destination_province,next_hub_id,status,created_at,updated_at)
SELECT r.current_hub_id,r.destination_region,r.destination_province,r.next_hub_id,N'ACTIVE',SYSDATETIME(),SYSDATETIME() FROM @routes r
WHERE NOT EXISTS(SELECT 1 FROM dbo.hub_routing_rules x WITH(UPDLOCK,HOLDLOCK) WHERE x.current_hub_id=r.current_hub_id AND x.destination_region=r.destination_region AND ISNULL(x.destination_province,N'')=ISNULL(r.destination_province,N'') AND x.status=N'ACTIVE');
IF (SELECT COUNT(*) FROM @nodes n JOIN dbo.hubs h ON h.code=n.code)<>38 THROW 51215,'Incomplete demo network.',1;
COMMIT TRANSACTION;
END TRY
BEGIN CATCH
IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
THROW;
END CATCH;
