/*
 * Phase 03 forward-only SQL Server migration.
 * Applies ADR-003 event_type reconciliation before adding the canonical hub,
 * profile and static current-next-hop routing persistence.
 */
IF COL_LENGTH(N'dbo.shipment_events', N'event_type') IS NULL
BEGIN
    ALTER TABLE dbo.shipment_events ADD event_type NVARCHAR(50) NULL;
END;
GO
IF COL_LENGTH(N'dbo.shipment_events', N'status') IS NOT NULL
BEGIN
    UPDATE dbo.shipment_events SET event_type = status WHERE event_type IS NULL;
    ALTER TABLE dbo.shipment_events ALTER COLUMN status NVARCHAR(50) NULL;
END;
GO
IF EXISTS (SELECT 1 FROM dbo.shipment_events WHERE event_type IS NULL)
BEGIN
    THROW 51000, 'Cannot enforce shipment_events.event_type: legacy rows have no event type.', 1;
END;
GO
IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.shipment_events')
      AND name = N'event_type'
      AND is_nullable = 1
)
BEGIN
    ALTER TABLE dbo.shipment_events ALTER COLUMN event_type NVARCHAR(50) NOT NULL;
END;
GO

IF OBJECT_ID(N'dbo.hubs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.hubs (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        code NVARCHAR(50) NOT NULL UNIQUE,
        name NVARCHAR(150) NOT NULL,
        level INT NOT NULL,
        type NVARCHAR(30) NOT NULL,
        parent_hub_id BIGINT NULL,
        region NVARCHAR(100) NOT NULL,
        province NVARCHAR(100) NOT NULL,
        district NVARCHAR(100) NULL,
        address NVARCHAR(255) NULL,
        status NVARCHAR(20) NOT NULL
    );
END;
GO
IF OBJECT_ID(N'dbo.employee_profiles', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.employee_profiles (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        user_id BIGINT NOT NULL,
        employee_code NVARCHAR(50) NOT NULL UNIQUE,
        hub_id BIGINT NOT NULL,
        position NVARCHAR(30) NULL,
        status NVARCHAR(20) NOT NULL
    );
END;
GO
IF OBJECT_ID(N'dbo.hub_routing_rules', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.hub_routing_rules (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        current_hub_id BIGINT NOT NULL,
        destination_region NVARCHAR(100) NOT NULL,
        destination_province NVARCHAR(100) NULL,
        next_hub_id BIGINT NOT NULL,
        status NVARCHAR(20) NOT NULL,
        created_at DATETIME2(0) NOT NULL,
        updated_at DATETIME2(0) NOT NULL
    );
END;
GO
IF OBJECT_ID(N'dbo.shipment_routes', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.shipment_routes (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        shipment_id BIGINT NOT NULL,
        hub_id BIGINT NOT NULL,
        current_hub_id BIGINT NOT NULL,
        sequence_number INT NOT NULL,
        status NVARCHAR(20) NOT NULL,
        route_decision_key NVARCHAR(120) NOT NULL,
        created_at DATETIME2(0) NOT NULL
    );
END;
GO
IF COL_LENGTH(N'dbo.shipments', N'current_hub_id') IS NULL ALTER TABLE dbo.shipments ADD current_hub_id BIGINT NULL;
IF COL_LENGTH(N'dbo.shipments', N'destination_region') IS NULL ALTER TABLE dbo.shipments ADD destination_region NVARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.shipments', N'destination_province') IS NULL ALTER TABLE dbo.shipments ADD destination_province NVARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.shipments', N'version') IS NULL ALTER TABLE dbo.shipments ADD version BIGINT NOT NULL CONSTRAINT DF_shipments_version DEFAULT 0;
IF COL_LENGTH(N'dbo.shipment_events', N'hub_id') IS NULL ALTER TABLE dbo.shipment_events ADD hub_id BIGINT NULL;
IF COL_LENGTH(N'dbo.shipment_routes', N'current_hub_id') IS NULL ALTER TABLE dbo.shipment_routes ADD current_hub_id BIGINT NULL;
IF COL_LENGTH(N'dbo.shipment_routes', N'route_decision_key') IS NULL ALTER TABLE dbo.shipment_routes ADD route_decision_key NVARCHAR(120) NULL;
IF COL_LENGTH(N'dbo.shipment_routes', N'created_at') IS NULL ALTER TABLE dbo.shipment_routes ADD created_at DATETIME2(0) NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_shipment_routes_route_decision_key' AND object_id = OBJECT_ID(N'dbo.shipment_routes'))
    CREATE UNIQUE INDEX UX_shipment_routes_route_decision_key ON dbo.shipment_routes(route_decision_key) WHERE route_decision_key IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_hub_routing_rules_active_province' AND object_id = OBJECT_ID(N'dbo.hub_routing_rules'))
    CREATE UNIQUE INDEX UX_hub_routing_rules_active_province ON dbo.hub_routing_rules(current_hub_id, destination_region, destination_province)
    WHERE status = N'ACTIVE' AND destination_province IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_hub_routing_rules_active_region' AND object_id = OBJECT_ID(N'dbo.hub_routing_rules'))
    CREATE UNIQUE INDEX UX_hub_routing_rules_active_region ON dbo.hub_routing_rules(current_hub_id, destination_region)
    WHERE status = N'ACTIVE' AND destination_province IS NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_hub_routing_rules_lookup' AND object_id = OBJECT_ID(N'dbo.hub_routing_rules'))
    CREATE INDEX IX_hub_routing_rules_lookup ON dbo.hub_routing_rules(current_hub_id, destination_region, destination_province, status);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_employee_profiles_user')
    ALTER TABLE dbo.employee_profiles ADD CONSTRAINT FK_employee_profiles_user FOREIGN KEY (user_id) REFERENCES dbo.users(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_employee_profiles_hub')
    ALTER TABLE dbo.employee_profiles ADD CONSTRAINT FK_employee_profiles_hub FOREIGN KEY (hub_id) REFERENCES dbo.hubs(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_hub_routing_rules_current_hub')
    ALTER TABLE dbo.hub_routing_rules ADD CONSTRAINT FK_hub_routing_rules_current_hub FOREIGN KEY (current_hub_id) REFERENCES dbo.hubs(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_hub_routing_rules_next_hub')
    ALTER TABLE dbo.hub_routing_rules ADD CONSTRAINT FK_hub_routing_rules_next_hub FOREIGN KEY (next_hub_id) REFERENCES dbo.hubs(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_hubs_parent_hub')
    ALTER TABLE dbo.hubs ADD CONSTRAINT FK_hubs_parent_hub FOREIGN KEY (parent_hub_id) REFERENCES dbo.hubs(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_shipments_current_hub')
    ALTER TABLE dbo.shipments ADD CONSTRAINT FK_shipments_current_hub FOREIGN KEY (current_hub_id) REFERENCES dbo.hubs(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_shipment_events_hub')
    ALTER TABLE dbo.shipment_events ADD CONSTRAINT FK_shipment_events_hub FOREIGN KEY (hub_id) REFERENCES dbo.hubs(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_shipment_routes_shipment')
    ALTER TABLE dbo.shipment_routes ADD CONSTRAINT FK_shipment_routes_shipment FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_shipment_routes_hub')
    ALTER TABLE dbo.shipment_routes ADD CONSTRAINT FK_shipment_routes_hub FOREIGN KEY (hub_id) REFERENCES dbo.hubs(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_shipment_routes_current_hub')
    ALTER TABLE dbo.shipment_routes ADD CONSTRAINT FK_shipment_routes_current_hub FOREIGN KEY (current_hub_id) REFERENCES dbo.hubs(id);
GO
