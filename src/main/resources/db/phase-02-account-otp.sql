/*
 * Phase 02 account authentication. Apply only to the selected verification DB.
 * User decision 2026-10-22: phone OTP OR Google OIDC; Google verifies email only.
 * Does not activate existing users or infer phone verification from ACTIVE status.
 * Idempotent DDL; legacy identity normalization needs separate reviewed data migration.
 */
SET XACT_ABORT ON;
BEGIN TRY
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.users', N'U') IS NULL
    THROW 51020, 'Apply the canonical users schema first.', 1;

/* Stop before DDL if local 0... / 84... / +84... forms would identify the same phone. */
IF EXISTS (
    SELECT normalized_phone FROM (
        SELECT CASE
            WHEN phone LIKE N'0[35789]%' AND LEN(phone) = 10 THEN N'+84' + SUBSTRING(phone, 2, 9)
            WHEN phone LIKE N'84[35789]%' AND LEN(phone) = 11 THEN N'+' + phone
            ELSE phone END AS normalized_phone
        FROM dbo.users
    ) normalized GROUP BY normalized_phone HAVING COUNT(*) > 1
)
    THROW 51021, 'Duplicate normalized phones require a reviewed data migration.', 1;
IF EXISTS (SELECT LOWER(LTRIM(RTRIM(email))) FROM dbo.users GROUP BY LOWER(LTRIM(RTRIM(email))) HAVING COUNT(*) > 1)
    THROW 51022, 'Duplicate normalized emails require a reviewed data migration.', 1;
/* Do not silently rewrite existing identities or permit new duplicates under different formats. */
IF EXISTS (SELECT 1 FROM dbo.users WHERE phone NOT LIKE N'+84[35789]%' OR LEN(phone) <> 12
           OR SUBSTRING(phone, 2, LEN(phone)) COLLATE Latin1_General_100_BIN2 LIKE N'%[^0-9]%'
           OR email COLLATE Latin1_General_100_BIN2 <> LOWER(LTRIM(RTRIM(email))) COLLATE Latin1_General_100_BIN2)
    THROW 51023, 'Normalize existing user phone/email values with a reviewed data migration before this script.', 1;

IF COL_LENGTH(N'dbo.users', N'phone_verified_at') IS NULL
    ALTER TABLE dbo.users ADD phone_verified_at DATETIME2(0) NULL;
IF COL_LENGTH(N'dbo.users', N'email_verified_at') IS NULL
    ALTER TABLE dbo.users ADD email_verified_at DATETIME2(0) NULL;
IF COL_LENGTH(N'dbo.users', N'google_subject') IS NULL
    ALTER TABLE dbo.users ADD google_subject NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.users') AND name = N'UX_users_google_subject')
    EXEC sys.sp_executesql N'CREATE UNIQUE INDEX UX_users_google_subject ON dbo.users(google_subject) WHERE google_subject IS NOT NULL;';

IF OBJECT_ID(N'dbo.phone_otp_challenges', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.phone_otp_challenges (
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        user_id BIGINT NOT NULL,
        purpose VARCHAR(20) NOT NULL,
        code_hash VARCHAR(255) NOT NULL,
        expires_at DATETIME2(7) NOT NULL,
        consumed_at DATETIME2(7) NULL,
        failed_attempts INT NOT NULL,
        last_sent_at DATETIME2(7) NOT NULL,
        send_window_started_at DATETIME2(7) NOT NULL,
        send_count INT NOT NULL,
        version BIGINT NOT NULL CONSTRAINT DF_phone_otp_version DEFAULT 0,
        CONSTRAINT FK_phone_otp_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
        CONSTRAINT UQ_phone_otp_user UNIQUE (user_id),
        CONSTRAINT CK_phone_otp_purpose CHECK (purpose IN ('REGISTRATION', 'LOGIN')),
        CONSTRAINT CK_phone_otp_counts CHECK (failed_attempts >= 0 AND send_count > 0)
    );
END;
COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
/* One bounded row per account. Consumed hashes carry no valid proof; erase old hashes
 * during maintenance after at least 7 days while preserving send counters/window.
 * Never delete/recreate an active-window row to reset rate limiting.
 */
