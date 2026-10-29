/* Google sign-in creates accounts without a phone; the customer links one later from the profile page. Safe to run again. */
DECLARE @uq sysname = (
    SELECT TOP 1 kc.name FROM sys.key_constraints kc
    JOIN sys.index_columns ic ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
    JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
    WHERE kc.parent_object_id = OBJECT_ID(N'dbo.users') AND kc.type = 'UQ' AND c.name = N'phone');
IF @uq IS NOT NULL EXEC(N'ALTER TABLE dbo.users DROP CONSTRAINT [' + @uq + N']');

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.users') AND name = N'phone' AND is_nullable = 0)
    ALTER TABLE dbo.users ALTER COLUMN phone NVARCHAR(20) NULL;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.users') AND name = N'UX_users_phone')
    EXEC(N'CREATE UNIQUE INDEX UX_users_phone ON dbo.users(phone) WHERE phone IS NOT NULL');
