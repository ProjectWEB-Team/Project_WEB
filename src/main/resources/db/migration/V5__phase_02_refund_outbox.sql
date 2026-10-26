/*
 * Phase 02 refund obligation outbox (plan 02-09).
 * One PENDING_WALLET_CREDIT row per paid sender shipping fee that is cancelled before pickup.
 * Phase 04 owns the consumer: wallet credit, ledger and payment REFUNDED. Safe to run again.
 */
SET XACT_ABORT ON;

IF OBJECT_ID(N'dbo.shipments', N'U') IS NULL OR OBJECT_ID(N'dbo.payments', N'U') IS NULL
BEGIN
    THROW 51000, 'Apply the base shipment/payment schema before the refund outbox migration.', 1;
END;

IF OBJECT_ID(N'dbo.refund_settlement_outbox', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.refund_settlement_outbox (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        shipment_id BIGINT NOT NULL,
        payment_id BIGINT NOT NULL,
        sender_user_id BIGINT NOT NULL,
        amount DECIMAL(15,2) NOT NULL,
        cancel_reason NVARCHAR(50) NOT NULL, -- SENDER_REQUEST, PICKUP_TIMEOUT, PICKUP_SERVICE_FAILURE, DROPOFF_TIMEOUT
        status NVARCHAR(50) NOT NULL,        -- PENDING_WALLET_CREDIT, then Phase-04 states
        created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT uq_refund_outbox_payment UNIQUE (payment_id),
        CONSTRAINT fk_refund_outbox_shipment FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id),
        CONSTRAINT fk_refund_outbox_payment FOREIGN KEY (payment_id) REFERENCES dbo.payments(id),
        CONSTRAINT fk_refund_outbox_sender FOREIGN KEY (sender_user_id) REFERENCES dbo.users(id)
    );
END;
