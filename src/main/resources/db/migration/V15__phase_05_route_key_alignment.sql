/*
 * Run after the Phase 03 routing and Phase 04 return schemas, before application startup.
 * Legacy forward-only producer used shipment:<id>:hub:<id>. Only exact keys on
 * still-FORWARD shipments with canonical decision status are safely attributable.
 * Returned shipments with unsuffixed history require operator evidence; never
 * infer that history from the shipment's current direction. Preserve all IDs,
 * status, timestamps, next hops and valid direction-suffixed keys.
 */
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    -- Prevent a writer introducing a conflicting key between preflight and update.
    DECLARE @lockedRows BIGINT;
    SELECT @lockedRows = COUNT_BIG(*) FROM dbo.shipments WITH (TABLOCKX, HOLDLOCK);
    SELECT @lockedRows = COUNT_BIG(*) FROM dbo.shipment_routes WITH (TABLOCKX, HOLDLOCK);

    IF EXISTS (
        SELECT 1 FROM dbo.shipment_routes r
        WHERE r.current_hub_id IS NULL OR r.route_decision_key IS NULL
           OR r.route_decision_key NOT IN (
               CONCAT('shipment:',r.shipment_id,':hub:',r.current_hub_id),
               CONCAT('shipment:',r.shipment_id,':hub:',r.current_hub_id,':FORWARD'),
               CONCAT('shipment:',r.shipment_id,':hub:',r.current_hub_id,':RETURN'))
    )
        THROW 51000, 'Ambiguous route identity: resolve shipment_routes current_hub_id and route_decision_key before retry.', 1;

    IF EXISTS (
        SELECT 1 FROM dbo.shipment_routes r
        LEFT JOIN dbo.shipments s ON s.id=r.shipment_id
        WHERE r.route_decision_key=CONCAT('shipment:',r.shipment_id,':hub:',r.current_hub_id)
          AND (s.id IS NULL OR s.direction IS NULL OR s.direction<>'FORWARD'
               OR r.status IS NULL OR r.status NOT IN ('PENDING','LOADED'))
    )
        THROW 51000, 'Ambiguous legacy route direction or status: verify historical forward evidence before retry.', 1;

    IF EXISTS (
        SELECT 1 FROM dbo.shipment_routes legacy
        JOIN dbo.shipment_routes canonical
          ON canonical.route_decision_key=CONCAT(legacy.route_decision_key,':FORWARD')
        WHERE legacy.route_decision_key=CONCAT('shipment:',legacy.shipment_id,':hub:',legacy.current_hub_id)
    )
        THROW 51000, 'Conflicting legacy and FORWARD route decisions: resolve duplicate history before retry.', 1;

    UPDATE r SET route_decision_key=CONCAT(r.route_decision_key,':FORWARD')
    FROM dbo.shipment_routes r
    WHERE r.route_decision_key=CONCAT('shipment:',r.shipment_id,':hub:',r.current_hub_id);
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
