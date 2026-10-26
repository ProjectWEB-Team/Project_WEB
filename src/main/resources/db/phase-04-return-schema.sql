-- Phase 04: Last-mile return schema
-- Synchronized with docs/schema.sql and Phase 04 decisions (D-01, D-04, D-05, D-07)

-- Add index on delivery_attempts for rapid attempt lookups by shipment and date
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_delivery_attempts_shipment_id' AND object_id = OBJECT_ID(N'dbo.delivery_attempts'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_delivery_attempts_shipment_id
    ON dbo.delivery_attempts (shipment_id, attempt_number);
END;
GO

-- Add index on shipment_assignments for active shipper assignments
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_shipment_assignments_assignee_status' AND object_id = OBJECT_ID(N'dbo.shipment_assignments'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_shipment_assignments_assignee_status
    ON dbo.shipment_assignments (assignee_user_id, status);
END;
GO

