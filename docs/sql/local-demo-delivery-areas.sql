/* Opt-in local demo data. Not an application migration.
   Adds delivery coverage only; existing hubs, accounts and routing rules are preserved. */
SET XACT_ABORT ON;
IF DB_NAME() <> N'ute_express'
    THROW 51200, 'Local demo seed requires database ute_express.', 1;

BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @areas TABLE (
        code NVARCHAR(50), name NVARCHAR(150), province NVARCHAR(100),
        district NVARCHAR(100), parent_code NVARCHAR(50)
    );
    INSERT @areas VALUES
        (N'UAT-HCM-Q1', N'Bưu cục mẫu Quận 1', N'TP. Hồ Chí Minh', N'Quận 1', N'UAT-ORG'),
        (N'UAT-CT-NINHKIEU', N'Bưu cục mẫu Ninh Kiều', N'Cần Thơ', N'Ninh Kiều', N'UAT-DST');

    IF EXISTS (
        SELECT 1 FROM @areas a LEFT JOIN dbo.hubs p ON p.code = a.parent_code
        WHERE p.id IS NULL OR p.level <> 2 OR p.type <> N'REGIONAL_HUB'
            OR p.status <> N'ACTIVE' OR p.region <> N'SOUTH'
    )
        THROW 51201, 'Expected active SOUTH regional parents UAT-ORG and UAT-DST.', 1;

    IF EXISTS (
        SELECT 1 FROM @areas a JOIN dbo.hubs h WITH (UPDLOCK, HOLDLOCK) ON h.code = a.code
        WHERE h.level <> 1 OR h.type <> N'DELIVERY_HUB' OR h.status <> N'ACTIVE'
            OR h.region <> N'SOUTH' OR h.province <> a.province OR h.district IS NULL
            OR h.district <> a.district OR h.parent_hub_id IS NULL
            OR h.parent_hub_id <> (SELECT id FROM dbo.hubs WHERE code = a.parent_code)
    )
        THROW 51202, 'Demo hub code exists with conflicting configuration; no changes applied.', 1;

    IF EXISTS (
        SELECT 1 FROM @areas a JOIN dbo.hubs h WITH (UPDLOCK, HOLDLOCK)
            ON h.province = a.province AND h.district = a.district
        WHERE h.status = N'ACTIVE' AND h.type = N'DELIVERY_HUB' AND h.code <> a.code
    )
        THROW 51203, 'An active delivery hub already covers a demo area; no changes applied.', 1;

    INSERT dbo.hubs(code, name, level, type, parent_hub_id, region, province, district, status)
    SELECT a.code, a.name, 1, N'DELIVERY_HUB', p.id, N'SOUTH', a.province, a.district, N'ACTIVE'
    FROM @areas a JOIN dbo.hubs p ON p.code = a.parent_code
    WHERE NOT EXISTS (SELECT 1 FROM dbo.hubs h WITH (UPDLOCK, HOLDLOCK) WHERE h.code = a.code);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
