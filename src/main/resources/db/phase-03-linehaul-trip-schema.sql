/* Forward-only SQL Server migration for assigned-driver linehaul transitions and delay evidence. */
IF COL_LENGTH(N'dbo.transport_trips', N'handover_id') IS NULL ALTER TABLE dbo.transport_trips ADD handover_id BIGINT NULL;
IF COL_LENGTH(N'dbo.transport_trips', N'accepted_at') IS NULL ALTER TABLE dbo.transport_trips ADD accepted_at DATETIME2(0) NULL;
IF COL_LENGTH(N'dbo.transport_trips', N'started_at') IS NULL ALTER TABLE dbo.transport_trips ADD started_at DATETIME2(0) NULL;
IF COL_LENGTH(N'dbo.transport_trips', N'arrived_at') IS NULL ALTER TABLE dbo.transport_trips ADD arrived_at DATETIME2(0) NULL;
IF COL_LENGTH(N'dbo.transport_trips', N'accepted_by_user_id') IS NULL ALTER TABLE dbo.transport_trips ADD accepted_by_user_id BIGINT NULL;
IF COL_LENGTH(N'dbo.transport_trips', N'started_by_user_id') IS NULL ALTER TABLE dbo.transport_trips ADD started_by_user_id BIGINT NULL;
IF COL_LENGTH(N'dbo.transport_trips', N'arrived_by_user_id') IS NULL ALTER TABLE dbo.transport_trips ADD arrived_by_user_id BIGINT NULL;
IF COL_LENGTH(N'dbo.transport_trips', N'version') IS NULL ALTER TABLE dbo.transport_trips ADD version BIGINT NOT NULL CONSTRAINT DF_transport_trips_version DEFAULT 0;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_transport_trips_handover' AND object_id = OBJECT_ID(N'dbo.transport_trips'))
    CREATE UNIQUE INDEX UX_transport_trips_handover ON dbo.transport_trips(handover_id) WHERE handover_id IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_transport_trips_handover') ALTER TABLE dbo.transport_trips ADD CONSTRAINT FK_transport_trips_handover FOREIGN KEY (handover_id) REFERENCES dbo.handovers(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_transport_trips_accepted_by') ALTER TABLE dbo.transport_trips ADD CONSTRAINT FK_transport_trips_accepted_by FOREIGN KEY (accepted_by_user_id) REFERENCES dbo.users(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_transport_trips_started_by') ALTER TABLE dbo.transport_trips ADD CONSTRAINT FK_transport_trips_started_by FOREIGN KEY (started_by_user_id) REFERENCES dbo.users(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_transport_trips_arrived_by') ALTER TABLE dbo.transport_trips ADD CONSTRAINT FK_transport_trips_arrived_by FOREIGN KEY (arrived_by_user_id) REFERENCES dbo.users(id);
GO
/* Canonical shared incidents schema: operations and finance use one record model. */
IF OBJECT_ID(N'dbo.incidents', N'U') IS NULL
CREATE TABLE dbo.incidents (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    shipment_id BIGINT NULL,
    trip_id BIGINT NULL,
    hub_id BIGINT NULL,
    type NVARCHAR(50) NOT NULL,
    description NVARCHAR(500) NULL,
    reported_by BIGINT NOT NULL,
    status NVARCHAR(50) NOT NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_incidents_created_at DEFAULT SYSDATETIME(),
    resolved_at DATETIME2(0) NULL,
    CONSTRAINT FK_incidents_shipment FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id),
    CONSTRAINT FK_incidents_trip FOREIGN KEY (trip_id) REFERENCES dbo.transport_trips(id),
    CONSTRAINT FK_incidents_hub FOREIGN KEY (hub_id) REFERENCES dbo.hubs(id),
    CONSTRAINT FK_incidents_reported_by FOREIGN KEY (reported_by) REFERENCES dbo.users(id)
);
GO
/* Forward migration from the earlier Phase-03-only representation, if present. */
IF COL_LENGTH(N'dbo.incidents', N'shipment_id') IS NULL ALTER TABLE dbo.incidents ADD shipment_id BIGINT NULL;
IF COL_LENGTH(N'dbo.incidents', N'trip_id') IS NULL ALTER TABLE dbo.incidents ADD trip_id BIGINT NULL;
IF COL_LENGTH(N'dbo.incidents', N'type') IS NULL ALTER TABLE dbo.incidents ADD type NVARCHAR(50) NULL;
IF COL_LENGTH(N'dbo.incidents', N'description') IS NULL ALTER TABLE dbo.incidents ADD description NVARCHAR(500) NULL;
IF COL_LENGTH(N'dbo.incidents', N'reported_by') IS NULL ALTER TABLE dbo.incidents ADD reported_by BIGINT NULL;
IF COL_LENGTH(N'dbo.incidents', N'status') IS NULL ALTER TABLE dbo.incidents ADD status NVARCHAR(50) NULL;
IF COL_LENGTH(N'dbo.incidents', N'resolved_at') IS NULL ALTER TABLE dbo.incidents ADD resolved_at DATETIME2(0) NULL;
IF COL_LENGTH(N'dbo.incidents', N'transport_trip_id') IS NOT NULL
    UPDATE dbo.incidents SET trip_id = COALESCE(trip_id, transport_trip_id),
        reported_by = COALESCE(reported_by, actor_user_id),
        type = COALESCE(type, incident_type),
        description = COALESCE(description, CONCAT(note, CASE WHEN evidence_reference IS NULL THEN N'' ELSE N' [evidence:' + evidence_reference + N']' END)),
        status = COALESCE(status, N'OPEN')
    WHERE trip_id IS NULL OR reported_by IS NULL OR type IS NULL OR status IS NULL;
GO
IF EXISTS (SELECT 1 FROM dbo.incidents WHERE reported_by IS NULL OR type IS NULL OR status IS NULL)
    THROW 51013, 'Canonical incident migration requires reporter, type, and status.', 1;
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_incidents_trip')
    AND COL_LENGTH(N'dbo.incidents', N'transport_trip_id') IS NOT NULL
    ALTER TABLE dbo.incidents DROP CONSTRAINT FK_incidents_trip;
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_incidents_actor') ALTER TABLE dbo.incidents DROP CONSTRAINT FK_incidents_actor;
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_incidents_driver_type') ALTER TABLE dbo.incidents DROP CONSTRAINT CK_incidents_driver_type;
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_incidents_receipt_type') ALTER TABLE dbo.incidents DROP CONSTRAINT CK_incidents_receipt_type;
/* Remove dependencies before deleting superseded Phase-03-only columns. */
DECLARE @legacyIncidentColumns TABLE (name SYSNAME NOT NULL PRIMARY KEY);
INSERT INTO @legacyIncidentColumns(name)
SELECT name FROM (VALUES (N'transport_trip_id'), (N'actor_user_id'), (N'incident_type'), (N'note'), (N'evidence_reference')) AS legacy(name)
WHERE COL_LENGTH(N'dbo.incidents', name) IS NOT NULL;

DECLARE @dropLegacyDependencies NVARCHAR(MAX) = N'';
SELECT @dropLegacyDependencies = @dropLegacyDependencies +
    N'ALTER TABLE dbo.incidents DROP CONSTRAINT ' + QUOTENAME(foreignKey.name) + N';'
FROM sys.foreign_keys foreignKey
INNER JOIN sys.foreign_key_columns foreignKeyColumn ON foreignKeyColumn.constraint_object_id = foreignKey.object_id
WHERE foreignKey.parent_object_id = OBJECT_ID(N'dbo.incidents')
  AND COL_NAME(foreignKeyColumn.parent_object_id, foreignKeyColumn.parent_column_id) IN (SELECT name FROM @legacyIncidentColumns);
IF @dropLegacyDependencies <> N'' EXEC sys.sp_executesql @dropLegacyDependencies;

SET @dropLegacyDependencies = N'';
SELECT @dropLegacyDependencies = @dropLegacyDependencies +
    N'ALTER TABLE dbo.incidents DROP CONSTRAINT ' + QUOTENAME(defaultConstraint.name) + N';'
FROM sys.default_constraints defaultConstraint
WHERE defaultConstraint.parent_object_id = OBJECT_ID(N'dbo.incidents')
  AND COL_NAME(defaultConstraint.parent_object_id, defaultConstraint.parent_column_id) IN (SELECT name FROM @legacyIncidentColumns);
IF @dropLegacyDependencies <> N'' EXEC sys.sp_executesql @dropLegacyDependencies;

SET @dropLegacyDependencies = N'';
SELECT @dropLegacyDependencies = @dropLegacyDependencies +
    N'ALTER TABLE dbo.incidents DROP CONSTRAINT ' + QUOTENAME(checkConstraint.name) + N';'
FROM sys.check_constraints checkConstraint
WHERE checkConstraint.parent_object_id = OBJECT_ID(N'dbo.incidents')
  AND EXISTS (SELECT 1 FROM @legacyIncidentColumns legacy WHERE checkConstraint.definition LIKE N'%' + QUOTENAME(legacy.name) + N'%');
IF @dropLegacyDependencies <> N'' EXEC sys.sp_executesql @dropLegacyDependencies;

IF COL_LENGTH(N'dbo.incidents', N'transport_trip_id') IS NOT NULL ALTER TABLE dbo.incidents DROP COLUMN transport_trip_id;
IF COL_LENGTH(N'dbo.incidents', N'actor_user_id') IS NOT NULL ALTER TABLE dbo.incidents DROP COLUMN actor_user_id;
IF COL_LENGTH(N'dbo.incidents', N'incident_type') IS NOT NULL ALTER TABLE dbo.incidents DROP COLUMN incident_type;
IF COL_LENGTH(N'dbo.incidents', N'note') IS NOT NULL ALTER TABLE dbo.incidents DROP COLUMN note;
IF COL_LENGTH(N'dbo.incidents', N'evidence_reference') IS NOT NULL ALTER TABLE dbo.incidents DROP COLUMN evidence_reference;

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_incidents_shipment') ALTER TABLE dbo.incidents ADD CONSTRAINT FK_incidents_shipment FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_incidents_trip') ALTER TABLE dbo.incidents ADD CONSTRAINT FK_incidents_trip FOREIGN KEY (trip_id) REFERENCES dbo.transport_trips(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_incidents_hub') ALTER TABLE dbo.incidents ADD CONSTRAINT FK_incidents_hub FOREIGN KEY (hub_id) REFERENCES dbo.hubs(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_incidents_reported_by') ALTER TABLE dbo.incidents ADD CONSTRAINT FK_incidents_reported_by FOREIGN KEY (reported_by) REFERENCES dbo.users(id);
GO
