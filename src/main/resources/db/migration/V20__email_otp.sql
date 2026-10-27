/* Email proof never marks phone_verified_at or links receiver shipments. */
IF OBJECT_ID(N'dbo.email_otp_challenges', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.email_otp_challenges (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        user_id BIGINT NOT NULL,
        email NVARCHAR(100) NOT NULL,
        purpose VARCHAR(20) NOT NULL,
        code_hash VARCHAR(255) NOT NULL,
        expires_at DATETIME2(7) NOT NULL,
        consumed_at DATETIME2(7) NULL,
        failed_attempts INT NOT NULL,
        last_sent_at DATETIME2(7) NOT NULL,
        send_window_started_at DATETIME2(7) NOT NULL,
        send_count INT NOT NULL,
        version BIGINT NOT NULL CONSTRAINT DF_email_otp_version DEFAULT 0,
        CONSTRAINT FK_email_otp_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
        CONSTRAINT UQ_email_otp_user UNIQUE (user_id),
        CONSTRAINT CK_email_otp_purpose CHECK (purpose IN ('REGISTRATION', 'PASSWORD_RESET', 'VERIFY_EMAIL')),
        CONSTRAINT CK_email_otp_counts CHECK (failed_attempts >= 0 AND send_count > 0)
    );
END;
