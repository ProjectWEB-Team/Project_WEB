-- Phase 04: Finance reconciliation schema
-- Synchronized with docs/schema.sql and Phase 04 decisions (D-06, D-08, D-09, D-10, D-13)

-- 1. wallets table (ADR-001)
IF OBJECT_ID(N'dbo.wallets', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.wallets (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL UNIQUE,
        balance DECIMAL(15,2) NOT NULL DEFAULT 0,
        locked_balance DECIMAL(15,2) NOT NULL DEFAULT 0,
        status NVARCHAR(20) NOT NULL DEFAULT N'ACTIVE', -- ACTIVE, LOCKED
        created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_wallets_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
    );
END;
GO

-- 2. wallet_transactions table
IF OBJECT_ID(N'dbo.wallet_transactions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.wallet_transactions (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        transaction_type NVARCHAR(50) NOT NULL, -- COD_PAYOUT, SHIPPING_FEE_PAYMENT, RETURN_FEE_PAYMENT, WEIGHT_SURCHARGE_PAYMENT, REFUND, WITHDRAWAL_LOCK, WITHDRAWAL_DEDUCT, WITHDRAWAL_REFUND, COMPENSATION
        amount DECIMAL(15,2) NOT NULL,
        balance_before DECIMAL(15,2) NOT NULL,
        balance_after DECIMAL(15,2) NOT NULL,
        reference_id BIGINT NOT NULL,
        reference_type NVARCHAR(50) NOT NULL, -- SHIPMENT, WITHDRAWAL, INCIDENT, PAYMENT
        description NVARCHAR(255) NULL,
        created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_wallet_transactions_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
    );
END;
GO

-- 3. shipper_reconciliations table
IF OBJECT_ID(N'dbo.shipper_reconciliations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.shipper_reconciliations (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        shipper_id BIGINT NOT NULL,
        manager_id BIGINT NOT NULL,
        reconciliation_date DATE NOT NULL,
        expected_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
        actual_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
        difference_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
        status NVARCHAR(50) NOT NULL, -- PENDING, COMPLETED, COMPLETED_WITH_DISCREPANCY
        created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
        completed_at DATETIME2(0) NULL,
        CONSTRAINT FK_shipper_reconciliations_shipper FOREIGN KEY (shipper_id) REFERENCES dbo.users(id),
        CONSTRAINT FK_shipper_reconciliations_manager FOREIGN KEY (manager_id) REFERENCES dbo.users(id)
    );
END;
GO

-- 4. shipper_reconciliation_items table (with source_type and source_id)
IF OBJECT_ID(N'dbo.shipper_reconciliation_items', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.shipper_reconciliation_items (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        reconciliation_id BIGINT NOT NULL,
        shipment_id BIGINT NOT NULL,
        source_type NVARCHAR(50) NOT NULL, -- COD_TRANSACTION, RECEIVER_CASH_SHIPPING_FEE_PAYMENT
        source_id BIGINT NOT NULL,
        expected_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
        actual_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
        difference_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
        status NVARCHAR(50) NOT NULL, -- MATCHED, SHORTAGE, OVERAGE
        note NVARCHAR(255) NULL,
        CONSTRAINT FK_shipper_reconciliation_items_reconciliation FOREIGN KEY (reconciliation_id) REFERENCES dbo.shipper_reconciliations(id),
        CONSTRAINT FK_shipper_reconciliation_items_shipment FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id),
        CONSTRAINT UQ_shipper_reconciliation_items_source UNIQUE (source_type, source_id)
    );
END;
GO

-- 4.1 The canonical schema creates shipper_reconciliation_items without the
-- source reference, so an existing table has to be reconciled as well.
IF COL_LENGTH(N'dbo.shipper_reconciliation_items', N'source_type') IS NULL
BEGIN
    ALTER TABLE dbo.shipper_reconciliation_items ADD source_type NVARCHAR(50) NULL;
END;
GO

IF COL_LENGTH(N'dbo.shipper_reconciliation_items', N'source_id') IS NULL
BEGIN
    ALTER TABLE dbo.shipper_reconciliation_items ADD source_id BIGINT NULL;
END;
GO

IF COL_LENGTH(N'dbo.shipper_reconciliation_items', N'difference_amount') IS NULL
BEGIN
    ALTER TABLE dbo.shipper_reconciliation_items ADD difference_amount DECIMAL(15,2) NOT NULL
        CONSTRAINT DF_spri_difference_amount DEFAULT 0;
END;
GO

IF COL_LENGTH(N'dbo.shipper_reconciliation_items', N'status') IS NULL
BEGIN
    ALTER TABLE dbo.shipper_reconciliation_items ADD status NVARCHAR(50) NOT NULL
        CONSTRAINT DF_spri_status DEFAULT N'MATCHED';
END;
GO

-- The source reference becomes mandatory once the existing rows are cleared.
IF NOT EXISTS (SELECT 1 FROM dbo.shipper_reconciliation_items WHERE source_type IS NULL)
BEGIN
    ALTER TABLE dbo.shipper_reconciliation_items ALTER COLUMN source_type NVARCHAR(50) NOT NULL;
    ALTER TABLE dbo.shipper_reconciliation_items ALTER COLUMN source_id BIGINT NOT NULL;
END;
GO

IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_shipper_reconciliation_items_source')
BEGIN
    ALTER TABLE dbo.shipper_reconciliation_items DROP CONSTRAINT UQ_shipper_reconciliation_items_source;
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_shipper_reconciliation_items_source')
BEGIN
    ALTER TABLE dbo.shipper_reconciliation_items ADD CONSTRAINT UQ_shipper_reconciliation_items_source
        UNIQUE (source_type, source_id);
END;
GO

-- 5. incidents table (shared canonical incident record)
-- The column set follows docs/schema.sql, which is the field-level authority
-- (ADR-004). Operational incidents of the hub slice and financial incidents of
-- this slice share one table, so ADR-006 stays satisfied.
IF OBJECT_ID(N'dbo.incidents', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.incidents (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        shipment_id BIGINT NULL,
        trip_id BIGINT NULL,
        hub_id BIGINT NULL,
        type NVARCHAR(50) NOT NULL, -- MISSING_PARCEL, DAMAGED_PARCEL, WRONG_ROUTE, TRIP_DELAY, MONEY_SHORTAGE, MONEY_OVERAGE, OTHER
        description NVARCHAR(500) NULL,
        reported_by BIGINT NOT NULL,
        status NVARCHAR(50) NOT NULL DEFAULT N'OPEN', -- OPEN, INVESTIGATING, RESOLVED, CLOSED
        created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
        resolved_at DATETIME2(0) NULL,
        CONSTRAINT FK_incidents_shipment FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id),
        CONSTRAINT FK_incidents_reported_by FOREIGN KEY (reported_by) REFERENCES dbo.users(id)
    );
END;
GO

-- 5.1 Align an already existing incidents table with the canonical columns.
IF COL_LENGTH(N'dbo.incidents', N'trip_id') IS NULL
BEGIN
    ALTER TABLE dbo.incidents ADD trip_id BIGINT NULL;
END;
GO

IF COL_LENGTH(N'dbo.incidents', N'description') IS NULL
BEGIN
    ALTER TABLE dbo.incidents ADD description NVARCHAR(500) NULL;
END;
GO

IF COL_LENGTH(N'dbo.incidents', N'reported_by') IS NULL
BEGIN
    ALTER TABLE dbo.incidents ADD reported_by BIGINT NULL;
END;
GO

IF COL_LENGTH(N'dbo.incidents', N'status') IS NULL
BEGIN
    ALTER TABLE dbo.incidents ADD status NVARCHAR(50) NOT NULL CONSTRAINT DF_incidents_status DEFAULT N'OPEN';
END;
GO

IF COL_LENGTH(N'dbo.incidents', N'created_at') IS NULL
BEGIN
    ALTER TABLE dbo.incidents ADD created_at DATETIME2(0) NOT NULL CONSTRAINT DF_incidents_created_at DEFAULT SYSDATETIME();
END;
GO

IF COL_LENGTH(N'dbo.incidents', N'resolved_at') IS NULL
BEGIN
    ALTER TABLE dbo.incidents ADD resolved_at DATETIME2(0) NULL;
END;
GO

-- 5.2 The canonical type column carries every incident type, so the legacy
-- single-type check constraint of the hub slice must not restrict the table.
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_incidents_driver_type')
BEGIN
    ALTER TABLE dbo.incidents DROP CONSTRAINT CK_incidents_driver_type;
END;
GO

-- 6. shipments columns used by the compensation and closure rules of this slice.
IF COL_LENGTH(N'dbo.shipments', N'declared_value') IS NULL
BEGIN
    ALTER TABLE dbo.shipments ADD declared_value DECIMAL(15,2) NULL;
END;
GO

IF COL_LENGTH(N'dbo.shipments', N'cancel_reason') IS NULL
BEGIN
    ALTER TABLE dbo.shipments ADD cancel_reason NVARCHAR(255) NULL;
END;
GO

IF COL_LENGTH(N'dbo.shipments', N'closure_reason') IS NULL
BEGIN
    ALTER TABLE dbo.shipments ADD closure_reason NVARCHAR(50) NULL;
END;
GO

