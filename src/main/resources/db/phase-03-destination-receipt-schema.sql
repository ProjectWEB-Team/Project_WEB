/* Forward-only SQL Server migration for destination physical receipt. */
IF COL_LENGTH(N'dbo.container_items', N'received_at') IS NULL ALTER TABLE dbo.container_items ADD received_at DATETIME2(0) NULL;
IF COL_LENGTH(N'dbo.container_items', N'received_by_user_id') IS NULL ALTER TABLE dbo.container_items ADD received_by_user_id BIGINT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_container_items_received_by')
    ALTER TABLE dbo.container_items ADD CONSTRAINT FK_container_items_received_by FOREIGN KEY (received_by_user_id) REFERENCES dbo.users(id);
GO
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_incidents_driver_type')
    ALTER TABLE dbo.incidents DROP CONSTRAINT CK_incidents_driver_type;
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_incidents_receipt_type')
    ALTER TABLE dbo.incidents ADD CONSTRAINT CK_incidents_receipt_type CHECK (incident_type IN (N'TRIP_DELAY', N'MISSING_PARCEL'));
GO
CREATE OR ALTER TRIGGER dbo.TR_container_items_receipt_evidence
ON dbo.container_items AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted WHERE status IN (N'RECEIVED', N'MISSING') AND (received_at IS NULL OR received_by_user_id IS NULL))
        THROW 51012, 'Destination receipt outcome requires attributable evidence.', 1;
END;
GO
/* Preserve sealed membership while allowing a destination operator to record receipt outcome. */
CREATE OR ALTER TRIGGER dbo.TR_container_items_sealed
ON dbo.container_items AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1
        FROM (SELECT container_id FROM inserted EXCEPT SELECT container_id FROM deleted
              UNION
              SELECT container_id FROM deleted EXCEPT SELECT container_id FROM inserted) changed
        JOIN dbo.handover_containers hc ON hc.container_id = changed.container_id
        JOIN dbo.handovers h ON h.id = hc.handover_id AND h.status IN (N'PENDING_ACCEPTANCE', N'ACCEPTED')
    ) THROW 51011, 'Container membership is sealed while custody is active.', 1;
END;
GO
