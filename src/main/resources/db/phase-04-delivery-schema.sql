-- Phase 04: Last-mile delivery schema
-- Synchronized with docs/schema.sql and Phase 04 decisions (D-01..D-05)

-- 1. Ensure shipments table has Phase-04 operational and financial columns
IF COL_LENGTH(N'dbo.shipments', N'current_hub_id') IS NULL
BEGIN
    ALTER TABLE dbo.shipments ADD current_hub_id BIGINT NULL;
END;
GO

IF COL_LENGTH(N'dbo.shipments', N'origin_hub_id') IS NULL
BEGIN
    ALTER TABLE dbo.shipments ADD origin_hub_id BIGINT NULL;
END;
GO

IF COL_LENGTH(N'dbo.shipments', N'destination_hub_id') IS NULL
BEGIN
    ALTER TABLE dbo.shipments ADD destination_hub_id BIGINT NULL;
END;
GO

IF COL_LENGTH(N'dbo.shipments', N'direction') IS NULL
BEGIN
    ALTER TABLE dbo.shipments ADD direction NVARCHAR(20) NOT NULL CONSTRAINT DF_shipments_direction DEFAULT N'FORWARD';
END;
GO

IF COL_LENGTH(N'dbo.shipments', N'shipping_fee') IS NULL
BEGIN
    ALTER TABLE dbo.shipments ADD shipping_fee DECIMAL(15,2) NULL;
END;
GO

IF COL_LENGTH(N'dbo.shipments', N'cod_amount') IS NULL
BEGIN
    ALTER TABLE dbo.shipments ADD cod_amount DECIMAL(15,2) NULL;
END;
GO

IF COL_LENGTH(N'dbo.shipments', N'return_fee') IS NULL
BEGIN
    ALTER TABLE dbo.shipments ADD return_fee DECIMAL(15,2) NULL;
END;
GO

IF COL_LENGTH(N'dbo.shipments', N'fail_reason') IS NULL
BEGIN
    ALTER TABLE dbo.shipments ADD fail_reason NVARCHAR(50) NULL;
END;
GO

IF COL_LENGTH(N'dbo.shipments', N'delivered_at') IS NULL
BEGIN
    ALTER TABLE dbo.shipments ADD delivered_at DATETIME2(0) NULL;
END;
GO

IF COL_LENGTH(N'dbo.shipments', N'returned_at') IS NULL
BEGIN
    ALTER TABLE dbo.shipments ADD returned_at DATETIME2(0) NULL;
END;
GO

-- 1.1 The canonical schema creates delivery_attempts without the evidence
-- reference, so an existing table has to be reconciled as well.
IF COL_LENGTH(N'dbo.delivery_attempts', N'evidence_url') IS NULL
BEGIN
    ALTER TABLE dbo.delivery_attempts ADD evidence_url NVARCHAR(255) NULL;
END;
GO

-- 2. shipment_assignments table
IF OBJECT_ID(N'dbo.shipment_assignments', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.shipment_assignments (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        shipment_id BIGINT NOT NULL,
        assignee_user_id BIGINT NOT NULL,
        assignment_type NVARCHAR(50) NOT NULL, -- PICKUP, DELIVERY, RETURN_DELIVERY
        status NVARCHAR(50) NOT NULL, -- ASSIGNED, ACCEPTED, REJECTED, IN_PROGRESS, COMPLETED, CANCELLED
        assigned_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
        accepted_at DATETIME2(0) NULL,
        completed_at DATETIME2(0) NULL,
        CONSTRAINT FK_shipment_assignments_shipment FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id),
        CONSTRAINT FK_shipment_assignments_assignee FOREIGN KEY (assignee_user_id) REFERENCES dbo.users(id)
    );
END;
GO

-- 3. delivery_attempts table (with evidence_url per D-05)
IF OBJECT_ID(N'dbo.delivery_attempts', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.delivery_attempts (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        assignment_id BIGINT NOT NULL,
        shipment_id BIGINT NOT NULL,
        shipper_id BIGINT NOT NULL,
        attempt_number INT NOT NULL,
        attempt_date DATE NOT NULL,
        attempt_time DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
        rescheduled_date DATE NULL,
        result NVARCHAR(50) NOT NULL, -- DELIVERED, NO_ANSWER, RECEIVER_REFUSED, PAYMENT_NOT_AVAILABLE, WRONG_ADDRESS, RESCHEDULED, OTHER
        note NVARCHAR(255) NULL,
        evidence_url NVARCHAR(255) NULL,
        CONSTRAINT FK_delivery_attempts_assignment FOREIGN KEY (assignment_id) REFERENCES dbo.shipment_assignments(id),
        CONSTRAINT FK_delivery_attempts_shipment FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id),
        CONSTRAINT FK_delivery_attempts_shipper FOREIGN KEY (shipper_id) REFERENCES dbo.users(id)
    );
END;
GO

-- 4. cod_transactions table
IF OBJECT_ID(N'dbo.cod_transactions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.cod_transactions (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        shipment_id BIGINT NOT NULL,
        collected_by_shipper_id BIGINT NOT NULL,
        amount DECIMAL(15,2) NOT NULL,
        method NVARCHAR(50) NOT NULL, -- CASH, VNPAY, TRANSFER
        status NVARCHAR(50) NOT NULL, -- NOT_APPLICABLE, WAITING_COLLECTION, COLLECTED, RECONCILED, HOLD_DISPUTE, READY_FOR_PAYOUT, PAID_TO_SENDER, CANCELLED
        collected_at DATETIME2(0) NULL,
        reconciled_at DATETIME2(0) NULL,
        payout_at DATETIME2(0) NULL,
        CONSTRAINT FK_cod_transactions_shipment FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id),
        CONSTRAINT FK_cod_transactions_shipper FOREIGN KEY (collected_by_shipper_id) REFERENCES dbo.users(id)
    );
END;
GO

-- 5. payments table
IF OBJECT_ID(N'dbo.payments', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.payments (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        shipment_id BIGINT NOT NULL,
        payer_type NVARCHAR(50) NOT NULL, -- SENDER, RECEIVER
        payment_type NVARCHAR(50) NOT NULL, -- SHIPPING_FEE, RETURN_FEE, WEIGHT_SURCHARGE
        amount DECIMAL(15,2) NOT NULL,
        method NVARCHAR(50) NOT NULL, -- CASH, VNPAY, E_WALLET
        status NVARCHAR(50) NOT NULL, -- UNPAID, PENDING, PAID, FAILED, REFUNDED
        transaction_ref NVARCHAR(100) NULL,
        paid_at DATETIME2(0) NULL,
        refunded_at DATETIME2(0) NULL,
        created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_payments_shipment FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id)
    );
END;
GO

