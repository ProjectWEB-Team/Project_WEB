IF OBJECT_ID(N'dbo.notification_email_outbox', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.notification_email_outbox (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        user_id BIGINT NOT NULL,
        email NVARCHAR(100) NOT NULL,
        subject NVARCHAR(200) NOT NULL,
        body NVARCHAR(2000) NOT NULL,
        status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
        attempts INT NOT NULL DEFAULT 0,
        available_at DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
        lease_token UNIQUEIDENTIFIER NULL,
        last_error VARCHAR(100) NULL,
        created_at DATETIME2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
        completed_at DATETIME2(7) NULL,
        CONSTRAINT CK_notification_email_status CHECK (status IN ('PENDING','SENDING','SENT','FAILED','CANCELLED')),
        CONSTRAINT CK_notification_email_attempts CHECK (attempts BETWEEN 0 AND 8)
    );
    CREATE INDEX IX_notification_email_due ON dbo.notification_email_outbox(status, available_at);
END;
