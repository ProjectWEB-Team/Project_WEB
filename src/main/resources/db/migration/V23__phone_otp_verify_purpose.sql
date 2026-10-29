/* Allow phone OTP challenges for profile phone verification and updates. */
SET XACT_ABORT ON;
BEGIN TRY
BEGIN TRANSACTION;

IF COL_LENGTH(N'dbo.phone_otp_challenges', N'target_phone') IS NULL
    ALTER TABLE dbo.phone_otp_challenges ADD target_phone NVARCHAR(20) NULL;

IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_phone_otp_purpose' AND parent_object_id = OBJECT_ID(N'dbo.phone_otp_challenges'))
    ALTER TABLE dbo.phone_otp_challenges DROP CONSTRAINT CK_phone_otp_purpose;

ALTER TABLE dbo.phone_otp_challenges ADD CONSTRAINT CK_phone_otp_purpose
    CHECK (purpose IN ('REGISTRATION', 'LOGIN', 'PASSWORD_RESET', 'VERIFY_PHONE'));

COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

