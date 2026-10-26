/*
 * Persist the actual entry into WAITING_FOR_HANDOVER for the 48-hour dropoff timer.
 * Existing rows remain NULL: creation time is not proof of handover-wait entry.
 * Run against the selected UTEExpress database before starting the updated app.
 */
SET XACT_ABORT ON;

IF OBJECT_ID(N'dbo.shipments', N'U') IS NULL
BEGIN
    THROW 51000, 'Apply the base shipment schema before the handover-wait migration.', 1;
END;

IF COL_LENGTH(N'dbo.shipments', N'waiting_for_handover_at') IS NULL
BEGIN
    ALTER TABLE dbo.shipments ADD waiting_for_handover_at DATETIME2(0) NULL;
END;
