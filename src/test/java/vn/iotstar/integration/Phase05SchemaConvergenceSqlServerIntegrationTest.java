package vn.iotstar.integration;

import org.junit.jupiter.api.Test;
import vn.iotstar.dto.hub.CreateContainerCommand;
import vn.iotstar.dto.hub.LoadContainerCommand;
import vn.iotstar.dto.hub.SortShipmentCommand;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@org.junit.jupiter.api.condition.EnabledIf(value="vn.iotstar.integration.Phase05SqlServerFixtureSupport#configured", disabledReason="REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
class Phase05SchemaConvergenceSqlServerIntegrationTest extends Phase05SqlServerFixtureSupport {
    private static final String ROUTE_MIGRATION="db/phase-05-route-key-alignment.sql";
    private String legacyKey(Long id) { return "shipment:"+id+":hub:"+hub("O"); }
    private void legacyRoute(Long id) {
        // Explicit pre-upgrade snapshot: the old producer omitted FORWARD only.
        sql.update("UPDATE dbo.shipment_routes SET route_decision_key=? WHERE shipment_id=?",legacyKey(id),id);
    }
    @Test void legacyPendingRouteRemainsLoadableAfterRepeatedKeyNormalization() throws Exception {
        Long id=create(); inbound(id); hubs.sortAtHub(new SortShipmentCommand(id)); legacyRoute(id);
        Long route=sql.queryForObject("SELECT id FROM dbo.shipment_routes WHERE shipment_id=?",Long.class,id);
        Object created=sql.queryForObject("SELECT created_at FROM dbo.shipment_routes WHERE id=?",Object.class,route);
        SqlServerMigrationSupport.apply(source,ROUTE_MIGRATION,ROUTE_MIGRATION);
        assertThat(hubs.sortAtHub(new SortShipmentCommand(id)).reused()).isTrue();
        Long container=custody.createContainer(new CreateContainerCommand(fixture+"_PENDING",hub("M")));
        custody.loadShipment(new LoadContainerCommand(container,id));
        assertThat(sql.queryForObject("SELECT route_decision_key FROM dbo.shipment_routes WHERE id=?",String.class,route)).isEqualTo(legacyKey(id)+":FORWARD");
        assertThat(sql.queryForObject("SELECT status FROM dbo.shipment_routes WHERE id=?",String.class,route)).isEqualTo("LOADED");
        assertThat(sql.queryForObject("SELECT created_at FROM dbo.shipment_routes WHERE id=?",Object.class,route)).isEqualTo(created);
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.shipment_routes WHERE shipment_id=?",Integer.class,id)).isEqualTo(1);
    }
    @Test void legacyLoadedRouteIsReusedAndCannotCreateSecondActiveContainer() throws Exception {
        Long id=create(); inbound(id); hubs.sortAtHub(new SortShipmentCommand(id));
        Long first=custody.createContainer(new CreateContainerCommand(fixture+"_FIRST",hub("M")));
        custody.loadShipment(new LoadContainerCommand(first,id)); legacyRoute(id);
        SqlServerMigrationSupport.apply(source,ROUTE_MIGRATION,ROUTE_MIGRATION);
        assertThat(hubs.sortAtHub(new SortShipmentCommand(id)).reused()).isTrue();
        Long second=custody.createContainer(new CreateContainerCommand(fixture+"_SECOND",hub("M")));
        assertThatThrownBy(()->custody.loadShipment(new LoadContainerCommand(second,id)))
                .isInstanceOf(vn.iotstar.exception.HubOperationAccessDeniedException.class);
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.shipment_routes WHERE shipment_id=?",Integer.class,id)).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT status FROM dbo.shipment_routes WHERE shipment_id=?",String.class,id)).isEqualTo("LOADED");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.container_items ci JOIN dbo.parcel_containers c ON c.id=ci.container_id WHERE ci.shipment_id=? AND ci.status='IN_CONTAINER' AND c.status IN ('OPEN','SEALED','LOADED')",Integer.class,id)).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.container_items WHERE container_id=?",Integer.class,second)).isZero();
    }
    @Test void conflictingLegacyAndCanonicalKeysFailBeforeAnyDecisionIsChanged() {
        Long id=create(); inbound(id); hubs.sortAtHub(new SortShipmentCommand(id));
        // Conflicting historical input: do not choose/delete/merge one decision automatically.
        sql.update("INSERT dbo.shipment_routes(shipment_id,hub_id,current_hub_id,sequence_number,status,route_decision_key,created_at) SELECT shipment_id,hub_id,current_hub_id,sequence_number,status,?,created_at FROM dbo.shipment_routes WHERE shipment_id=?",legacyKey(id),id);
        Long independent=create(); inbound(independent); hubs.sortAtHub(new SortShipmentCommand(independent)); legacyRoute(independent);
        assertThatThrownBy(()->SqlServerMigrationSupport.apply(source,ROUTE_MIGRATION))
                .isInstanceOf(java.sql.SQLException.class)
                .hasRootCauseMessage("Conflicting legacy and FORWARD route decisions: resolve duplicate history before retry.");
        assertThat(sql.queryForList("SELECT route_decision_key FROM dbo.shipment_routes WHERE shipment_id=?",String.class,id))
                .containsExactlyInAnyOrder(legacyKey(id),legacyKey(id)+":FORWARD");
        assertThat(sql.queryForObject("SELECT route_decision_key FROM dbo.shipment_routes WHERE shipment_id=?",String.class,independent)).isEqualTo(legacyKey(independent));
    }
    @Test void unsuffixedReturnHistoryIsAmbiguousAndNeverRelabelledFromCurrentDirection() {
        Long id=create(); inbound(id); hubs.sortAtHub(new SortShipmentCommand(id)); legacyRoute(id);
        // Historical direction ambiguity must be reconciled from operator evidence.
        sql.update("UPDATE dbo.shipments SET direction='RETURN' WHERE id=?",id);
        assertThatThrownBy(()->SqlServerMigrationSupport.apply(source,ROUTE_MIGRATION))
                .isInstanceOf(java.sql.SQLException.class)
                .hasRootCauseMessage("Ambiguous legacy route direction or status: verify historical forward evidence before retry.");
        assertThat(sql.queryForObject("SELECT route_decision_key FROM dbo.shipment_routes WHERE shipment_id=?",String.class,id)).isEqualTo(legacyKey(id));
    }
    @Test void canonicalManifestAndFixtureAreRepeatableAndMappedCustodyInsertSucceeds() throws Exception {
        SqlServerMigrationSupport.applyPhase05(source); fixtureScript("db/phase-05-integration-fixtures.sql");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.users WHERE LEFT(username,16)=?",Integer.class,fixture+"_")).isEqualTo(9);
        assertThat(sql.queryForObject("SELECT is_nullable FROM sys.columns WHERE object_id=OBJECT_ID('dbo.transport_trips') AND name='vehicle_id'",Boolean.class)).isFalse();
        assertThat(sql.queryForObject("SELECT is_nullable FROM sys.columns WHERE object_id=OBJECT_ID('dbo.handover_containers') AND name='scanned_at'",Boolean.class)).isTrue();
        assertThat(sql.queryForObject("SELECT is_nullable FROM sys.columns WHERE object_id=OBJECT_ID('dbo.shipment_events') AND name='event_type'",Boolean.class)).isFalse();
        Long id=create(); inbound(id); Hop hop=depart(List.of(id),"O","M"); receive(hop,id);
        assertThat(sql.queryForObject("SELECT vehicle_id FROM dbo.transport_trips WHERE id=?",Long.class,hop.trip())).isEqualTo(vehicle());
    }
    @Test void legacyPartiallyReceivedArrivedManifestPreservesReceiptAndCompletesRemainingItem() throws Exception {
        Long received=create(); inbound(received); Long pending=create(); inbound(pending);
        Hop hop=depart(List.of(received,pending),"O","M"); receive(hop,received);
        Object receivedAt=sql.queryForObject("SELECT received_at FROM dbo.container_items WHERE container_id=? AND shipment_id=?",Object.class,hop.container(),received);
        // Historical input ONLY: old service left these departed facts at their origin.
        sql.update("UPDATE dbo.shipments SET current_status='AT_HUB',current_hub_id=? WHERE id=?",hub("O"),pending);
        sql.update("UPDATE dbo.parcel_containers SET status='SEALED' WHERE id=?",hop.container());
        sql.update("UPDATE dbo.vehicles SET status='AVAILABLE' WHERE id=?",vehicle());
        SqlServerMigrationSupport.apply(source,"db/phase-05-custody-alignment.sql");
        SqlServerMigrationSupport.apply(source,"db/phase-05-custody-alignment.sql");
        assertParcel(pending,"IN_TRANSIT",null); assertParcel(received,"AT_HUB",hub("M"));
        assertThat(sql.queryForObject("SELECT received_at FROM dbo.container_items WHERE container_id=? AND shipment_id=?",Object.class,hop.container(),received)).isEqualTo(receivedAt);
        assertThat(sql.queryForObject("SELECT status FROM dbo.vehicles WHERE id=?",String.class,vehicle())).isEqualTo("IN_USE");
        receive(hop,pending);
        assertThat(sql.queryForObject("SELECT status FROM dbo.transport_trips WHERE id=?",String.class,hop.trip())).isEqualTo("COMPLETED");
    }
    @Test void legacyStartedManifestCanArriveAndReceiveAfterMigration() throws Exception {
        Long id=create(); inbound(id); Hop hop=depart(List.of(id),"O","M");
        sql.update("UPDATE dbo.transport_trips SET status='STARTED',arrival_time=NULL,arrived_at=NULL WHERE id=?",hop.trip());
        sql.update("UPDATE dbo.handovers SET status='ACCEPTED' WHERE id=?",hop.handover());
        sql.update("UPDATE dbo.shipments SET current_status='AT_HUB',current_hub_id=? WHERE id=?",hub("O"),id);
        sql.update("UPDATE dbo.parcel_containers SET status='SEALED' WHERE id=?",hop.container());
        SqlServerMigrationSupport.apply(source,"db/phase-05-custody-alignment.sql");
        SqlServerMigrationSupport.apply(source,"db/phase-05-custody-alignment.sql");
        assertParcel(id,"IN_TRANSIT",null);
        actor("L"); linehaul.arriveTrip(hop.trip()); receive(hop,id);
    }
    @Test void legacyMissingEvidenceAndHoldSurviveMigrationAndRecoveryStillRequiresInvestigation() throws Exception {
        Long id=create(); inbound(id); Hop hop=depart(List.of(id),"O","M");
        actor("M"); receipts.reconcileContainer(new vn.iotstar.dto.hub.DestinationReconciliationCommand(hop.handover(),hop.container(),List.of()));
        Long incident=sql.queryForObject("SELECT id FROM dbo.incidents WHERE shipment_id=?",Long.class,id);
        String evidence=sql.queryForObject("SELECT description FROM dbo.incidents WHERE id=?",String.class,incident);
        Object missingAt=sql.queryForObject("SELECT received_at FROM dbo.container_items WHERE container_id=? AND shipment_id=?",Object.class,hop.container(),id);
        // Legacy snapshot: historical receipt discrepancy recorded, but parcel physical facts stale.
        sql.update("UPDATE dbo.handovers SET status='DISCREPANCY' WHERE id=?",hop.handover());
        sql.update("UPDATE dbo.transport_trips SET status='ARRIVED' WHERE id=?",hop.trip());
        sql.update("UPDATE dbo.shipments SET current_status='AT_HUB',current_hub_id=?,on_hold=0 WHERE id=?",hub("O"),id);
        sql.update("UPDATE dbo.parcel_containers SET status='SEALED' WHERE id=?",hop.container());
        SqlServerMigrationSupport.apply(source,"db/phase-05-custody-alignment.sql");
        SqlServerMigrationSupport.apply(source,"db/phase-05-custody-alignment.sql");
        assertParcel(id,"IN_TRANSIT",null);
        assertThat(sql.queryForObject("SELECT on_hold FROM dbo.shipments WHERE id=?",Boolean.class,id)).isTrue();
        assertThat(sql.queryForObject("SELECT description FROM dbo.incidents WHERE id=?",String.class,incident)).isEqualTo(evidence);
        assertThat(sql.queryForObject("SELECT received_at FROM dbo.container_items WHERE container_id=? AND shipment_id=?",Object.class,hop.container(),id)).isEqualTo(missingAt);
        assertThat(sql.queryForObject("SELECT found_at FROM dbo.container_items WHERE container_id=? AND shipment_id=?",Object.class,hop.container(),id)).isNull();
        assertThatThrownBy(()->receive(hop,id)).isInstanceOf(RuntimeException.class);
        actor("G"); receipts.resolveMissing(new vn.iotstar.dto.hub.MissingParcelResolutionCommand(incident,hop.handover(),hop.container(),id,hub("M"),"FOUND","Actual parcel located after migration"));
        assertParcel(id,"AT_HUB",hub("M"));
    }
    @Test void conflictingLegacyVehicleAllocationsFailClosedUntilLegitimateMappingIsRestored() throws Exception {
        Long first=create(); inbound(first); Hop one=depart(List.of(first),"O","M");
        sql.update("INSERT dbo.vehicles(plate_number,vehicle_type,status) VALUES(?,'TRUCK','AVAILABLE')",fixture+"_2");
        Long secondVehicle=sql.queryForObject("SELECT id FROM dbo.vehicles WHERE plate_number=?",Long.class,fixture+"_2");
        Long second=create(); inbound(second); Hop two=depart(List.of(second),"O","M",secondVehicle);
        sql.update("UPDATE dbo.transport_trips SET vehicle_id=? WHERE id=?",vehicle(),two.trip());
        assertThatThrownBy(()->SqlServerMigrationSupport.apply(source,"db/phase-05-custody-alignment.sql"))
                .isInstanceOf(java.sql.SQLException.class).hasRootCauseMessage("Conflicting active vehicle allocations: resolve transport_trips grouped by vehicle_id before retry.");
        assertParcel(first,"IN_TRANSIT",null); assertParcel(second,"IN_TRANSIT",null);
        // The original service-selected vehicle is known; never fabricate an assignment.
        sql.update("UPDATE dbo.transport_trips SET vehicle_id=? WHERE id=?",secondVehicle,two.trip());
        SqlServerMigrationSupport.apply(source,"db/phase-05-custody-alignment.sql");
        SqlServerMigrationSupport.apply(source,"db/phase-05-custody-alignment.sql");
        receive(one,first); receive(two,second);
        assertThat(sql.queryForObject("SELECT status FROM dbo.vehicles WHERE id=?",String.class,secondVehicle)).isEqualTo("AVAILABLE");
    }
}
