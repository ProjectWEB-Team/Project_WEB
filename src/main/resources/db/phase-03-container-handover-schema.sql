/* Forward-only SQL Server migration for Phase 03 container-level custody. */
IF OBJECT_ID(N'dbo.parcel_containers', N'U') IS NULL
CREATE TABLE dbo.parcel_containers (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    container_code NVARCHAR(80) NOT NULL,
    origin_hub_id BIGINT NOT NULL,
    destination_hub_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    status NVARCHAR(30) NOT NULL,
    created_at DATETIME2(0) NOT NULL,
    version BIGINT NOT NULL CONSTRAINT DF_parcel_containers_version DEFAULT 0,
    CONSTRAINT UK_parcel_containers_code UNIQUE (container_code)
);
GO
IF OBJECT_ID(N'dbo.container_items', N'U') IS NULL
CREATE TABLE dbo.container_items (
    container_id BIGINT NOT NULL,
    shipment_id BIGINT NOT NULL,
    status NVARCHAR(30) NOT NULL,
    scanned_at DATETIME2(0) NOT NULL,
    CONSTRAINT PK_container_items PRIMARY KEY (container_id, shipment_id)
);
GO
IF OBJECT_ID(N'dbo.transport_trips', N'U') IS NULL
CREATE TABLE dbo.transport_trips (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    trip_code NVARCHAR(80) NOT NULL UNIQUE,
    origin_hub_id BIGINT NOT NULL,
    destination_hub_id BIGINT NOT NULL,
    driver_user_id BIGINT NULL,
    status NVARCHAR(30) NOT NULL
);
GO
IF OBJECT_ID(N'dbo.handovers', N'U') IS NULL
CREATE TABLE dbo.handovers (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    from_hub_id BIGINT NOT NULL,
    to_hub_id BIGINT NOT NULL,
    sender_employee_id BIGINT NOT NULL,
    receiver_driver_id BIGINT NOT NULL,
    transport_trip_id BIGINT NULL,
    expected_containers INT NOT NULL,
    accepted_containers INT NOT NULL,
    status NVARCHAR(30) NOT NULL,
    created_at DATETIME2(0) NOT NULL,
    accepted_at DATETIME2(0) NULL,
    cancelled_at DATETIME2(0) NULL,
    replaces_handover_id BIGINT NULL,
    replacement_handover_id BIGINT NULL,
    version BIGINT NOT NULL CONSTRAINT DF_handovers_version DEFAULT 0,
    CONSTRAINT CK_handovers_counts CHECK (expected_containers > 0 AND accepted_containers >= 0 AND accepted_containers <= expected_containers),
    CONSTRAINT CK_handovers_accepted_reconciled CHECK (status <> N'ACCEPTED' OR accepted_containers = expected_containers)
);
GO
IF OBJECT_ID(N'dbo.handover_containers', N'U') IS NULL
CREATE TABLE dbo.handover_containers (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    handover_id BIGINT NOT NULL,
    container_id BIGINT NOT NULL,
    status NVARCHAR(30) NOT NULL,
    scanned_at DATETIME2(0) NULL,
    CONSTRAINT UK_handover_container UNIQUE (handover_id, container_id)
);
GO
IF COL_LENGTH(N'dbo.parcel_containers', N'version') IS NULL
    ALTER TABLE dbo.parcel_containers ADD version BIGINT NOT NULL CONSTRAINT DF_parcel_containers_version DEFAULT 0;
IF COL_LENGTH(N'dbo.handovers', N'version') IS NULL
    ALTER TABLE dbo.handovers ADD version BIGINT NOT NULL CONSTRAINT DF_handovers_version DEFAULT 0;
/* Reconcile legacy handovers created before the custody audit timestamps existed. */
IF COL_LENGTH(N'dbo.handovers', N'accepted_at') IS NULL
    ALTER TABLE dbo.handovers ADD accepted_at DATETIME2(0) NULL;
IF COL_LENGTH(N'dbo.handovers', N'cancelled_at') IS NULL
    ALTER TABLE dbo.handovers ADD cancelled_at DATETIME2(0) NULL;
/* A handover is created before the assigned driver accepts and creates its trip. */
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.handovers') AND name = N'transport_trip_id' AND is_nullable = 0)
    ALTER TABLE dbo.handovers ALTER COLUMN transport_trip_id BIGINT NULL;
IF COL_LENGTH(N'dbo.parcel_containers', N'created_by') IS NULL
    ALTER TABLE dbo.parcel_containers ADD created_by BIGINT NULL;
IF EXISTS (SELECT 1 FROM dbo.parcel_containers WHERE created_by IS NULL)
    THROW 51013, 'Cannot enforce parcel_containers.created_by: legacy rows have no creator.', 1;
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.parcel_containers') AND name = N'created_by' AND is_nullable = 1)
    ALTER TABLE dbo.parcel_containers ALTER COLUMN created_by BIGINT NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_handovers_counts' AND parent_object_id = OBJECT_ID(N'dbo.handovers'))
    ALTER TABLE dbo.handovers ADD CONSTRAINT CK_handovers_counts CHECK (expected_containers > 0 AND accepted_containers >= 0 AND accepted_containers <= expected_containers);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_handovers_accepted_reconciled' AND parent_object_id = OBJECT_ID(N'dbo.handovers'))
    ALTER TABLE dbo.handovers ADD CONSTRAINT CK_handovers_accepted_reconciled CHECK (status <> N'ACCEPTED' OR accepted_containers = expected_containers);
/* A previous interrupted forward migration can leave the handover table without
   its optional correction links. Add them before creating the self-referencing FKs. */
IF COL_LENGTH(N'dbo.handovers', N'replaces_handover_id') IS NULL
    ALTER TABLE dbo.handovers ADD replaces_handover_id BIGINT NULL;
IF COL_LENGTH(N'dbo.handovers', N'replacement_handover_id') IS NULL
    ALTER TABLE dbo.handovers ADD replacement_handover_id BIGINT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_container_items_container') ALTER TABLE dbo.container_items ADD CONSTRAINT FK_container_items_container FOREIGN KEY (container_id) REFERENCES dbo.parcel_containers(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_container_items_shipment') ALTER TABLE dbo.container_items ADD CONSTRAINT FK_container_items_shipment FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_parcel_containers_origin') ALTER TABLE dbo.parcel_containers ADD CONSTRAINT FK_parcel_containers_origin FOREIGN KEY (origin_hub_id) REFERENCES dbo.hubs(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_parcel_containers_destination') ALTER TABLE dbo.parcel_containers ADD CONSTRAINT FK_parcel_containers_destination FOREIGN KEY (destination_hub_id) REFERENCES dbo.hubs(id);
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_key_columns fkc
    JOIN sys.columns c ON c.object_id = fkc.parent_object_id AND c.column_id = fkc.parent_column_id
    WHERE fkc.parent_object_id = OBJECT_ID(N'dbo.parcel_containers') AND c.name = N'created_by'
) ALTER TABLE dbo.parcel_containers ADD CONSTRAINT FK_parcel_containers_creator FOREIGN KEY (created_by) REFERENCES dbo.users(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_handovers_from_hub') ALTER TABLE dbo.handovers ADD CONSTRAINT FK_handovers_from_hub FOREIGN KEY (from_hub_id) REFERENCES dbo.hubs(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_handovers_to_hub') ALTER TABLE dbo.handovers ADD CONSTRAINT FK_handovers_to_hub FOREIGN KEY (to_hub_id) REFERENCES dbo.hubs(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_handovers_sender') ALTER TABLE dbo.handovers ADD CONSTRAINT FK_handovers_sender FOREIGN KEY (sender_employee_id) REFERENCES dbo.employee_profiles(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_handovers_driver') ALTER TABLE dbo.handovers ADD CONSTRAINT FK_handovers_driver FOREIGN KEY (receiver_driver_id) REFERENCES dbo.users(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_handovers_trip') ALTER TABLE dbo.handovers ADD CONSTRAINT FK_handovers_trip FOREIGN KEY (transport_trip_id) REFERENCES dbo.transport_trips(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_handovers_replaces') ALTER TABLE dbo.handovers ADD CONSTRAINT FK_handovers_replaces FOREIGN KEY (replaces_handover_id) REFERENCES dbo.handovers(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_handovers_replacement') ALTER TABLE dbo.handovers ADD CONSTRAINT FK_handovers_replacement FOREIGN KEY (replacement_handover_id) REFERENCES dbo.handovers(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_handover_containers_handover') ALTER TABLE dbo.handover_containers ADD CONSTRAINT FK_handover_containers_handover FOREIGN KEY (handover_id) REFERENCES dbo.handovers(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_handover_containers_container') ALTER TABLE dbo.handover_containers ADD CONSTRAINT FK_handover_containers_container FOREIGN KEY (container_id) REFERENCES dbo.parcel_containers(id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_handover_containers_container') CREATE INDEX IX_handover_containers_container ON dbo.handover_containers(container_id);
GO
CREATE OR ALTER TRIGGER dbo.TR_handover_containers_no_competing_active
ON dbo.handover_containers AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1 FROM dbo.handover_containers a
        JOIN dbo.handovers ha ON ha.id = a.handover_id AND ha.status IN (N'PENDING_ACCEPTANCE', N'ACCEPTED')
        JOIN dbo.handover_containers b ON b.container_id = a.container_id AND b.handover_id <> a.handover_id
        JOIN dbo.handovers hb ON hb.id = b.handover_id AND hb.status IN (N'PENDING_ACCEPTANCE', N'ACCEPTED')
        JOIN inserted i ON i.container_id = a.container_id
    )
    BEGIN
        THROW 51010, 'A container cannot be in competing active handovers.', 1;
    END
END;
GO
CREATE OR ALTER TRIGGER dbo.TR_container_items_sealed
ON dbo.container_items AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1 FROM (SELECT container_id FROM inserted UNION SELECT container_id FROM deleted) changed
        JOIN dbo.handover_containers hc ON hc.container_id = changed.container_id
        JOIN dbo.handovers h ON h.id = hc.handover_id AND h.status IN (N'PENDING_ACCEPTANCE', N'ACCEPTED')
    )
    BEGIN
        THROW 51011, 'Container membership is sealed while custody is active.', 1;
    END
END;
GO
