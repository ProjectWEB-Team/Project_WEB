/*
 * ADR-003 reconciliation for databases provisioned from legacy docs/schema.sql.
 * This deliberately changes only shipment_events: event_type is canonical and
 * old status data is copied forward before the old column becomes nullable.
 */
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

ALTER TABLE dbo.shipment_events ALTER COLUMN event_type NVARCHAR(50) NOT NULL;
GO
