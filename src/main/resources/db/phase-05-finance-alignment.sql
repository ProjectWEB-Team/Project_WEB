-- Additive evidence fields. Legacy collections remain unattributed; never guess a collector.
IF COL_LENGTH(N'dbo.payments', N'collected_by_user_id') IS NULL
    ALTER TABLE dbo.payments ADD collected_by_user_id BIGINT NULL;
IF COL_LENGTH(N'dbo.shipments', N'return_arrived_at_origin_at') IS NULL
    ALTER TABLE dbo.shipments ADD return_arrived_at_origin_at DATETIME2(0) NULL;
GO
IF COL_LENGTH(N'dbo.refund_settlement_outbox', N'settled_at') IS NULL
    ALTER TABLE dbo.refund_settlement_outbox ADD settled_at DATETIME2(0) NULL;
IF COL_LENGTH(N'dbo.incidents', N'compensated_at') IS NULL
    ALTER TABLE dbo.incidents ADD compensated_at DATETIME2(0) NULL;
IF COL_LENGTH(N'dbo.shipper_reconciliations', N'hub_id') IS NULL
    ALTER TABLE dbo.shipper_reconciliations ADD hub_id BIGINT NULL;
IF COL_LENGTH(N'dbo.shipper_reconciliations', N'collection_method') IS NULL
    ALTER TABLE dbo.shipper_reconciliations ADD collection_method NVARCHAR(20) NULL;
IF COL_LENGTH(N'dbo.shipper_reconciliations', N'settlement_evidence') IS NULL
    ALTER TABLE dbo.shipper_reconciliations ADD settlement_evidence NVARCHAR(255) NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_reconciliation_scope_hub')
    ALTER TABLE dbo.shipper_reconciliations ADD CONSTRAINT FK_reconciliation_scope_hub FOREIGN KEY (hub_id) REFERENCES dbo.hubs(id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_payments_collector')
    ALTER TABLE dbo.payments ADD CONSTRAINT FK_payments_collector FOREIGN KEY (collected_by_user_id) REFERENCES dbo.users(id);
GO
