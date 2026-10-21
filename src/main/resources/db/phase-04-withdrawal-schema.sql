-- Phase 04: Withdrawal schema
-- Synchronized with docs/schema.sql and Phase 04 decisions (D-11, D-12)

IF OBJECT_ID(N'dbo.withdrawal_requests', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.withdrawal_requests (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        amount DECIMAL(15,2) NOT NULL,
        bank_name NVARCHAR(100) NOT NULL,
        account_number NVARCHAR(50) NOT NULL,
        account_holder_name NVARCHAR(100) NOT NULL,
        status NVARCHAR(50) NOT NULL DEFAULT N'PENDING', -- PENDING, APPROVED, REJECTED, COMPLETED
        rejection_reason NVARCHAR(255) NULL,
        request_key NVARCHAR(100) NULL,
        requested_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
        approved_at DATETIME2(0) NULL,
        rejected_at DATETIME2(0) NULL,
        completed_at DATETIME2(0) NULL,
        CONSTRAINT FK_withdrawal_requests_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
        CONSTRAINT UQ_withdrawal_requests_user_key UNIQUE (user_id, request_key)
    );
END;
GO

-- The canonical schema already creates withdrawal_requests, so the columns this
-- slice adds have to be reconciled on an existing table as well.
IF COL_LENGTH(N'dbo.withdrawal_requests', N'request_key') IS NULL
BEGIN
    ALTER TABLE dbo.withdrawal_requests ADD request_key NVARCHAR(100) NULL;
END;
GO

IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_withdrawal_requests_user_key')
BEGIN
    ALTER TABLE dbo.withdrawal_requests DROP CONSTRAINT UQ_withdrawal_requests_user_key;
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_withdrawal_requests_user_key')
BEGIN
    ALTER TABLE dbo.withdrawal_requests ADD CONSTRAINT UQ_withdrawal_requests_user_key UNIQUE (user_id, request_key);
END;
GO

