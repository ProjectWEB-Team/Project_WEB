/* Forward-only SQL Server migration for assigned-driver linehaul transitions and delay evidence. */
IF COL_LENGTH(N'dbo.transport_trips', N'handover_id') IS NULL ALTER TABLE dbo.transport_trips ADD handover_id BIGINT NULL;
IF COL_LENGTH(N'dbo.transport_trips', N'accepted_at') IS NULL ALTER TABLE dbo.transport_trips ADD accepted_at DATETIME2(0) NULL;
IF COL_LENGTH(N'dbo.transport_trips', N'started_at') IS NULL ALTER TABLE dbo.transport_trips ADD started_at DATETIME2(0) NULL;
IF COL_LENGTH(N'dbo.transport_trips', N'arrived_at') IS NULL ALTER TABLE dbo.transport_trips ADD arrived_at DATETIME2(0) NULL;
IF COL_LENGTH(N'dbo.transport_trips', N'accepted_by_user_id') IS NULL ALTER TABLE dbo.transport_trips ADD accepted_by_user_id BIGINT NULL;
IF COL_LENGTH(N'dbo.transport_trips', N'started_by_user_id') IS NULL ALTER TABLE dbo.transport_trips ADD started_by_user_id BIGINT NULL;
IF COL_LENGTH(N'dbo.transport_trips', N'arrived_by_user_id') IS NULL ALTER TABLE dbo.transport_trips ADD arrived_by_user_id BIGINT NULL;
IF COL_LENGTH(N'dbo.transport_trips', N'version') IS NULL ALTER TABLE dbo.transport_trips ADD version BIGINT NOT NULL CONSTRAINT DF_transport_trips_version DEFAULT 0;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_transport_trips_handover' AND object_id = OBJECT_ID(N'dbo.transport_trips'))
    CREATE UNIQUE INDEX UX_transport_trips_handover ON dbo.transport_trips(handover_id) WHERE handover_id IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_transport_trips_handover') ALTER TABLE dbo.transport_trips ADD CONSTRAINT FK_transport_trips_handover FOREIGN KEY (handover_id) REFERENCES dbo.handovers(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_transport_trips_accepted_by') ALTER TABLE dbo.transport_trips ADD CONSTRAINT FK_transport_trips_accepted_by FOREIGN KEY (accepted_by_user_id) REFERENCES dbo.users(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_transport_trips_started_by') ALTER TABLE dbo.transport_trips ADD CONSTRAINT FK_transport_trips_started_by FOREIGN KEY (started_by_user_id) REFERENCES dbo.users(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_transport_trips_arrived_by') ALTER TABLE dbo.transport_trips ADD CONSTRAINT FK_transport_trips_arrived_by FOREIGN KEY (arrived_by_user_id) REFERENCES dbo.users(id);
GO
IF OBJECT_ID(N'dbo.incidents', N'U') IS NULL
CREATE TABLE dbo.incidents (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    transport_trip_id BIGINT NOT NULL,
    hub_id BIGINT NOT NULL,
    actor_user_id BIGINT NOT NULL,
    incident_type NVARCHAR(50) NOT NULL,
    note NVARCHAR(500) NOT NULL,
    evidence_reference NVARCHAR(120) NULL,
    created_at DATETIME2(0) NOT NULL,
    CONSTRAINT CK_incidents_driver_type CHECK (incident_type = N'TRIP_DELAY')
);
GO
/* Repair a table left by an interrupted earlier migration before adding its FKs. */
IF COL_LENGTH(N'dbo.incidents', N'transport_trip_id') IS NULL ALTER TABLE dbo.incidents ADD transport_trip_id BIGINT NULL;
IF COL_LENGTH(N'dbo.incidents', N'hub_id') IS NULL ALTER TABLE dbo.incidents ADD hub_id BIGINT NULL;
IF COL_LENGTH(N'dbo.incidents', N'actor_user_id') IS NULL ALTER TABLE dbo.incidents ADD actor_user_id BIGINT NULL;
IF COL_LENGTH(N'dbo.incidents', N'incident_type') IS NULL ALTER TABLE dbo.incidents ADD incident_type NVARCHAR(50) NULL;
IF COL_LENGTH(N'dbo.incidents', N'note') IS NULL ALTER TABLE dbo.incidents ADD note NVARCHAR(500) NULL;
IF COL_LENGTH(N'dbo.incidents', N'evidence_reference') IS NULL ALTER TABLE dbo.incidents ADD evidence_reference NVARCHAR(120) NULL;
IF COL_LENGTH(N'dbo.incidents', N'created_at') IS NULL ALTER TABLE dbo.incidents ADD created_at DATETIME2(0) NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_incidents_trip') ALTER TABLE dbo.incidents ADD CONSTRAINT FK_incidents_trip FOREIGN KEY (transport_trip_id) REFERENCES dbo.transport_trips(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_incidents_hub') ALTER TABLE dbo.incidents ADD CONSTRAINT FK_incidents_hub FOREIGN KEY (hub_id) REFERENCES dbo.hubs(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_incidents_actor') ALTER TABLE dbo.incidents ADD CONSTRAINT FK_incidents_actor FOREIGN KEY (actor_user_id) REFERENCES dbo.users(id);
GO
