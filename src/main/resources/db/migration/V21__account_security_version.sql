/* Revoke credentials after password, role or account-status changes; protect concurrent edits. */
IF COL_LENGTH(N'dbo.users', N'security_version') IS NULL
    ALTER TABLE dbo.users ADD security_version BIGINT NOT NULL CONSTRAINT DF_users_security_version DEFAULT 0;
IF COL_LENGTH(N'dbo.users', N'version') IS NULL
    ALTER TABLE dbo.users ADD version BIGINT NOT NULL CONSTRAINT DF_users_version DEFAULT 0;
