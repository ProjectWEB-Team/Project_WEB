/* Optimistic locking for money records: a concurrent edit now fails instead of silently overwriting. Safe to run again. */
IF COL_LENGTH(N'dbo.wallets', N'version') IS NULL ALTER TABLE dbo.wallets ADD version BIGINT NOT NULL CONSTRAINT DF_wallets_version DEFAULT 0;
IF COL_LENGTH(N'dbo.withdrawal_requests', N'version') IS NULL ALTER TABLE dbo.withdrawal_requests ADD version BIGINT NOT NULL CONSTRAINT DF_withdrawal_requests_version DEFAULT 0;
IF COL_LENGTH(N'dbo.payments', N'version') IS NULL ALTER TABLE dbo.payments ADD version BIGINT NOT NULL CONSTRAINT DF_payments_version DEFAULT 0;
IF COL_LENGTH(N'dbo.cod_transactions', N'version') IS NULL ALTER TABLE dbo.cod_transactions ADD version BIGINT NOT NULL CONSTRAINT DF_cod_transactions_version DEFAULT 0;
