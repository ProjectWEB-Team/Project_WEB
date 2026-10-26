-- Keep the selected service for authoritative actual-weight pricing at physical intake.
-- Existing rows remain NULL: do not invent which service an old shipment purchased.
IF COL_LENGTH('shipments', 'service_code') IS NULL
    ALTER TABLE shipments ADD service_code NVARCHAR(50) NULL;
GO
