/*
 * ADR-003 reconciliation for databases provisioned from legacy docs/schema.sql.
 * This deliberately changes only shipment_events: event_type is canonical and
 * old status data is copied forward before the old column becomes nullable.
 *
 * A partially provisioned local database may contain users and shipments but
 * lack shipment_events entirely. Create that canonical baseline first so this
 * migration remains forward-only and safe to re-run.
 */
IF OBJECT_ID(N'dbo.shipment_events', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.shipment_events (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        shipment_id BIGINT NOT NULL,
        actor_user_id BIGINT NULL,
        event_type NVARCHAR(50) NOT NULL,
        description NVARCHAR(255) NULL,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_shipment_events_created_at DEFAULT SYSDATETIME(),
        CONSTRAINT FK_shipment_events_shipment FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id),
        CONSTRAINT FK_shipment_events_actor FOREIGN KEY (actor_user_id) REFERENCES dbo.users(id)
    );
END;
GO

IF COL_LENGTH(N'dbo.shipment_events', N'event_type') IS NULL
BEGIN
    ALTER TABLE dbo.shipment_events ADD event_type NVARCHAR(50) NULL;
END;
GO

IF COL_LENGTH(N'dbo.shipment_events', N'status') IS NOT NULL
BEGIN
    UPDATE dbo.shipment_events
    SET event_type = status
    WHERE event_type IS NULL;

    ALTER TABLE dbo.shipment_events ALTER COLUMN status NVARCHAR(50) NULL;
END;
GO

IF EXISTS (SELECT 1 FROM dbo.shipment_events WHERE event_type IS NULL)
BEGIN
    THROW 51000, 'Cannot enforce shipment_events.event_type: legacy rows have no event type.', 1;
END;
GO

IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.shipment_events')
      AND name = N'event_type'
      AND is_nullable = 1
)
BEGIN
    ALTER TABLE dbo.shipment_events ALTER COLUMN event_type NVARCHAR(50) NOT NULL;
END;
GO
