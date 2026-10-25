/* Apply after phase-03 destination receipt. Forward-only; never fabricate historical vehicle assignments. */
IF OBJECT_ID(N'dbo.vehicles', N'U') IS NULL
CREATE TABLE dbo.vehicles (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    plate_number NVARCHAR(20) NOT NULL UNIQUE,
    vehicle_type NVARCHAR(50) NOT NULL,
    capacity INT NULL,
    status NVARCHAR(50) NOT NULL
);
IF COL_LENGTH(N'dbo.transport_trips', N'vehicle_id') IS NULL ALTER TABLE dbo.transport_trips ADD vehicle_id BIGINT NULL;
IF COL_LENGTH(N'dbo.transport_trips', N'departure_time') IS NULL ALTER TABLE dbo.transport_trips ADD departure_time DATETIME2(0) NULL;
IF COL_LENGTH(N'dbo.transport_trips', N'arrival_time') IS NULL ALTER TABLE dbo.transport_trips ADD arrival_time DATETIME2(0) NULL;
IF COL_LENGTH(N'dbo.handovers', N'completed_at') IS NULL ALTER TABLE dbo.handovers ADD completed_at DATETIME2(0) NULL;
IF COL_LENGTH(N'dbo.container_items', N'found_at') IS NULL ALTER TABLE dbo.container_items ADD found_at DATETIME2(0) NULL;
IF COL_LENGTH(N'dbo.container_items', N'found_by_user_id') IS NULL ALTER TABLE dbo.container_items ADD found_by_user_id BIGINT NULL;
IF COL_LENGTH(N'dbo.incidents', N'resolution_outcome') IS NULL ALTER TABLE dbo.incidents ADD resolution_outcome NVARCHAR(30) NULL;
IF COL_LENGTH(N'dbo.incidents', N'resolution_evidence') IS NULL ALTER TABLE dbo.incidents ADD resolution_evidence NVARCHAR(500) NULL;
IF COL_LENGTH(N'dbo.incidents', N'investigated_by') IS NULL ALTER TABLE dbo.incidents ADD investigated_by BIGINT NULL;
IF COL_LENGTH(N'dbo.incidents', N'investigated_at') IS NULL ALTER TABLE dbo.incidents ADD investigated_at DATETIME2(0) NULL;
GO
IF EXISTS (SELECT 1 FROM dbo.transport_trips WHERE vehicle_id IS NULL)
    THROW 51020, 'Historical trips need a legitimate vehicle_id mapping before the custody migration can complete.', 1;
ALTER TABLE dbo.transport_trips ALTER COLUMN vehicle_id BIGINT NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.transport_trips') AND referenced_object_id = OBJECT_ID(N'dbo.vehicles'))
    ALTER TABLE dbo.transport_trips ADD CONSTRAINT FK_transport_trips_vehicle FOREIGN KEY (vehicle_id) REFERENCES dbo.vehicles(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_container_items_found_by')
    ALTER TABLE dbo.container_items ADD CONSTRAINT FK_container_items_found_by FOREIGN KEY (found_by_user_id) REFERENCES dbo.users(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_incidents_investigated_by')
    ALTER TABLE dbo.incidents ADD CONSTRAINT FK_incidents_investigated_by FOREIGN KEY (investigated_by) REFERENCES dbo.users(id);
GO
/* Pending membership has no scan. The timestamp appears only when the driver actually scans. */
ALTER TABLE dbo.handover_containers ALTER COLUMN scanned_at DATETIME2(0) NULL;
UPDATE dbo.handover_containers SET scanned_at = NULL WHERE status = N'EXPECTED';
UPDATE dbo.handover_containers SET status = N'LOADED' WHERE status = N'SCANNED';
UPDATE dbo.transport_trips SET status = N'SCHEDULED' WHERE status = N'ACCEPTED';
UPDATE dbo.transport_trips SET status = N'IN_TRANSIT' WHERE status = N'STARTED';
UPDATE dbo.handovers SET status = N'COMPLETED_WITH_DISCREPANCY' WHERE status = N'DISCREPANCY';
UPDATE h SET status = N'IN_TRANSIT' FROM dbo.handovers h JOIN dbo.transport_trips t ON t.id = h.transport_trip_id
WHERE h.status = N'ACCEPTED' AND t.status IN (N'DEPARTED', N'IN_TRANSIT', N'ARRIVED');
UPDATE t SET status = h.status FROM dbo.transport_trips t JOIN dbo.handovers h ON h.transport_trip_id = t.id
WHERE t.status = N'ARRIVED' AND h.status IN (N'COMPLETED', N'COMPLETED_WITH_DISCREPANCY');
IF COL_LENGTH(N'dbo.transport_trips', N'started_at') IS NOT NULL
    EXEC(N'UPDATE dbo.transport_trips SET departure_time = COALESCE(departure_time, started_at)');
IF COL_LENGTH(N'dbo.transport_trips', N'arrived_at') IS NOT NULL
    EXEC(N'UPDATE dbo.transport_trips SET arrival_time = COALESCE(arrival_time, arrived_at)');
GO
/* Reconcile old departed manifests using actual mapped custody. No IDs or scans are invented.
   Receipt/found evidence is authoritative and is never rewound. Ambiguity requires operator repair. */
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF EXISTS (SELECT vehicle_id FROM dbo.transport_trips
           WHERE status IN (N'SCHEDULED', N'IN_TRANSIT', N'ARRIVED', N'DEPARTED')
           GROUP BY vehicle_id HAVING COUNT(*) > 1)
    THROW 51021, 'Conflicting active vehicle allocations: resolve transport_trips grouped by vehicle_id before retry.', 1;
IF EXISTS (
    SELECT ci.shipment_id FROM dbo.container_items ci
    JOIN dbo.handover_containers hc ON hc.container_id = ci.container_id
    JOIN dbo.handovers h ON h.id = hc.handover_id
    JOIN dbo.transport_trips t ON t.id = h.transport_trip_id
    WHERE t.status IN (N'IN_TRANSIT', N'ARRIVED', N'DEPARTED') AND h.status = N'IN_TRANSIT'
      AND ci.status <> N'RECEIVED' AND ci.found_at IS NULL
    GROUP BY ci.shipment_id HAVING COUNT(*) > 1)
    THROW 51022, 'Ambiguous departed parcel custody: resolve competing active manifests before retry.', 1;
IF EXISTS (
    SELECT 1 FROM dbo.container_items ci
    JOIN dbo.shipments s ON s.id = ci.shipment_id
    JOIN dbo.handover_containers hc ON hc.container_id = ci.container_id
    JOIN dbo.handovers h ON h.id = hc.handover_id
    JOIN dbo.transport_trips t ON t.id = h.transport_trip_id
    JOIN dbo.parcel_containers c ON c.id = ci.container_id
    WHERE t.status IN (N'IN_TRANSIT', N'ARRIVED', N'DEPARTED') AND h.status = N'IN_TRANSIT'
      AND ci.status <> N'RECEIVED' AND ci.found_at IS NULL
      AND (hc.scanned_at IS NULL OR hc.status NOT IN (N'LOADED',N'DISCREPANCY',N'DELIVERED')
           OR c.origin_hub_id <> h.from_hub_id OR c.destination_hub_id <> h.to_hub_id
           OR t.origin_hub_id <> h.from_hub_id OR t.destination_hub_id <> h.to_hub_id
           OR (s.current_hub_id IS NOT NULL AND s.current_hub_id <> h.from_hub_id)
           OR NOT ((s.direction = N'FORWARD' AND s.current_status IN (N'AT_HUB', N'IN_TRANSIT'))
                OR (s.direction = N'RETURN' AND s.current_status = N'RETURN_TO_SENDER'))))
    THROW 51023, 'Departed custody lacks consistent scan, route or parcel state evidence; repair the manifest before retry.', 1;
UPDATE s SET current_hub_id = NULL,
    current_status = CASE WHEN s.direction = N'FORWARD' THEN N'IN_TRANSIT' ELSE s.current_status END,
    on_hold = CASE WHEN ci.status = N'MISSING' THEN 1 ELSE s.on_hold END
FROM dbo.shipments s JOIN dbo.container_items ci ON ci.shipment_id = s.id
JOIN dbo.handover_containers hc ON hc.container_id = ci.container_id
JOIN dbo.handovers h ON h.id = hc.handover_id
JOIN dbo.transport_trips t ON t.id = h.transport_trip_id
WHERE t.status IN (N'IN_TRANSIT', N'ARRIVED', N'DEPARTED') AND h.status = N'IN_TRANSIT'
  AND ci.status <> N'RECEIVED' AND ci.found_at IS NULL;
UPDATE c SET status = N'LOADED'
FROM dbo.parcel_containers c JOIN dbo.handover_containers hc ON hc.container_id = c.id
JOIN dbo.handovers h ON h.id = hc.handover_id
JOIN dbo.transport_trips t ON t.id = h.transport_trip_id
WHERE c.status = N'SEALED' AND h.status = N'IN_TRANSIT'
  AND t.status IN (N'IN_TRANSIT', N'ARRIVED', N'DEPARTED');
UPDATE v SET status = N'IN_USE' FROM dbo.vehicles v
WHERE v.status = N'AVAILABLE' AND EXISTS (SELECT 1 FROM dbo.transport_trips t WHERE t.vehicle_id = v.id
    AND t.status IN (N'SCHEDULED', N'IN_TRANSIT', N'ARRIVED', N'DEPARTED'));
UPDATE dbo.transport_trips SET status = N'IN_TRANSIT' WHERE status = N'DEPARTED';
/* Old discrepancy completion can also leave a missing parcel notionally at origin.
   Only an unresolved, exact incident/custody reference authorizes this correction. */
IF EXISTS (
    SELECT 1 FROM dbo.container_items ci JOIN dbo.shipments s ON s.id=ci.shipment_id
    JOIN dbo.handover_containers hc ON hc.container_id=ci.container_id
    JOIN dbo.handovers h ON h.id=hc.handover_id
    WHERE ci.status=N'MISSING' AND ci.found_at IS NULL AND h.status=N'COMPLETED_WITH_DISCREPANCY'
      AND s.current_status IN (N'AT_HUB',N'IN_TRANSIT',N'RETURN_TO_SENDER')
      AND (hc.scanned_at IS NULL OR (s.current_hub_id IS NOT NULL AND s.current_hub_id<>h.from_hub_id)
           OR NOT EXISTS (SELECT 1 FROM dbo.incidents i WHERE i.shipment_id=s.id AND i.trip_id=h.transport_trip_id
               AND i.hub_id=h.to_hub_id AND i.type=N'MISSING_PARCEL' AND i.status=N'INVESTIGATING'
               AND CHARINDEX(N'[evidence:receipt:handover:'+CONVERT(NVARCHAR(20),h.id)+N':container:'
                   +CONVERT(NVARCHAR(20),ci.container_id)+N':shipment:'+CONVERT(NVARCHAR(20),s.id)+N']',i.description)>0)))
    THROW 51024, 'Legacy missing parcel lacks exact unresolved incident/scan evidence; investigate before retry.', 1;
UPDATE s SET current_hub_id=NULL, on_hold=1,
    current_status=CASE WHEN s.direction=N'FORWARD' THEN N'IN_TRANSIT' ELSE N'RETURN_TO_SENDER' END
FROM dbo.shipments s JOIN dbo.container_items ci ON ci.shipment_id=s.id
JOIN dbo.handover_containers hc ON hc.container_id=ci.container_id
JOIN dbo.handovers h ON h.id=hc.handover_id
WHERE ci.status=N'MISSING' AND ci.found_at IS NULL AND h.status=N'COMPLETED_WITH_DISCREPANCY'
  AND s.current_status IN (N'AT_HUB',N'IN_TRANSIT',N'RETURN_TO_SENDER');
UPDATE c SET status=CASE WHEN EXISTS(SELECT 1 FROM dbo.container_items i WHERE i.container_id=c.id AND i.status=N'MISSING')
    THEN N'UNPACKED_WITH_DISCREPANCY' ELSE N'UNPACKED_FULL' END
FROM dbo.parcel_containers c JOIN dbo.handover_containers hc ON hc.container_id=c.id
JOIN dbo.handovers h ON h.id=hc.handover_id
WHERE c.status IN(N'SEALED',N'LOADED') AND h.status IN(N'COMPLETED',N'COMPLETED_WITH_DISCREPANCY')
  AND EXISTS(SELECT 1 FROM dbo.container_items i WHERE i.container_id=c.id)
  AND NOT EXISTS(SELECT 1 FROM dbo.container_items i WHERE i.container_id=c.id AND i.status NOT IN(N'RECEIVED',N'MISSING'));
COMMIT TRANSACTION;
GO
CREATE OR ALTER TRIGGER dbo.TR_handover_containers_no_competing_active
ON dbo.handover_containers AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1 FROM dbo.handover_containers a
        JOIN dbo.handovers ha ON ha.id = a.handover_id AND ha.status IN (N'PENDING_ACCEPTANCE', N'ACCEPTED', N'IN_TRANSIT')
        JOIN dbo.handover_containers b ON b.container_id = a.container_id AND b.handover_id <> a.handover_id
        JOIN dbo.handovers hb ON hb.id = b.handover_id AND hb.status IN (N'PENDING_ACCEPTANCE', N'ACCEPTED', N'IN_TRANSIT')
        JOIN inserted i ON i.container_id = a.container_id
    ) THROW 51010, 'A container cannot be in competing active handovers.', 1;
END;
GO
/* Receipt/recovery evidence may change, but neither parcel identity nor sealed membership may change. */
CREATE OR ALTER TRIGGER dbo.TR_container_items_sealed
ON dbo.container_items AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1 FROM (
            SELECT container_id, shipment_id FROM (SELECT container_id, shipment_id FROM inserted EXCEPT SELECT container_id, shipment_id FROM deleted) added
            UNION
            SELECT container_id, shipment_id FROM (SELECT container_id, shipment_id FROM deleted EXCEPT SELECT container_id, shipment_id FROM inserted) removed
        ) changed
        JOIN dbo.handover_containers hc ON hc.container_id = changed.container_id
        JOIN dbo.handovers h ON h.id = hc.handover_id
        WHERE h.status <> N'CANCELLED'
    ) THROW 51011, 'Container membership is immutable after custody is accepted.', 1;
END;
GO
