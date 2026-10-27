/* Allow phone OTP challenges for self-service password reset. Safe to run again. */
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_phone_otp_purpose' AND parent_object_id = OBJECT_ID(N'dbo.phone_otp_challenges'))
    ALTER TABLE dbo.phone_otp_challenges DROP CONSTRAINT CK_phone_otp_purpose;
ALTER TABLE dbo.phone_otp_challenges ADD CONSTRAINT CK_phone_otp_purpose
    CHECK (purpose IN ('REGISTRATION', 'LOGIN', 'PASSWORD_RESET'));
