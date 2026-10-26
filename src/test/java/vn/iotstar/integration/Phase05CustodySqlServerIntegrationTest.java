package vn.iotstar.integration;

import org.junit.jupiter.api.Test;
import vn.iotstar.dto.hub.*;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@org.junit.jupiter.api.condition.EnabledIf(value="vn.iotstar.integration.Phase05SqlServerFixtureSupport#configured", disabledReason="REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
class Phase05CustodySqlServerIntegrationTest extends Phase05SqlServerFixtureSupport {
    @org.springframework.beans.factory.annotation.Autowired vn.iotstar.service.TrackingService publicTracking;
    @Test void missingHoldFoundRecoveryPreservesOriginalDiscrepancyAndReplaysOnce() {
        Long id=create(); inbound(id); Hop hop=depart(List.of(id),"O","M");
        actor("M"); receipts.reconcileContainer(new DestinationReconciliationCommand(hop.handover(),hop.container(),List.of()));
        assertParcel(id,"IN_TRANSIT",null);
        assertThat(sql.queryForObject("SELECT on_hold FROM dbo.shipments WHERE id=?",Boolean.class,id)).isTrue();
        Long incident=sql.queryForObject("SELECT id FROM dbo.incidents WHERE shipment_id=? AND type='MISSING_PARCEL'",Long.class,id);
        var publicView=publicTracking.track(tracking(id));
        assertThat(publicView.events()).extracting("label").contains("Đơn hàng đang được kiểm tra hành trình");
        assertThat(publicView.toString()).doesNotContain("Destination receipt discrepancy","receipt:handover:","MISSING_PARCEL");
        actor("G");
        assertThatThrownBy(()->receipts.resolveMissing(new MissingParcelResolutionCommand(incident,hop.handover(),hop.container()+100000,id,hub("M"),"FOUND","Physical scan")))
                .isInstanceOf(RuntimeException.class);
        var recovery=new MissingParcelResolutionCommand(incident,hop.handover(),hop.container(),id,hub("M"),"FOUND","Physical scan by hub staff");
        receipts.resolveMissing(recovery); receipts.resolveMissing(recovery);
        assertParcel(id,"AT_HUB",hub("M"));
        assertThat(sql.queryForObject("SELECT on_hold FROM dbo.shipments WHERE id=?",Boolean.class,id)).isFalse();
        assertThat(sql.queryForObject("SELECT status FROM dbo.container_items WHERE container_id=? AND shipment_id=?",String.class,hop.container(),id)).isEqualTo("MISSING");
        assertThat(sql.queryForObject("SELECT found_at FROM dbo.container_items WHERE container_id=? AND shipment_id=?",Object.class,hop.container(),id)).isNotNull();
        assertThat(sql.queryForObject("SELECT status FROM dbo.handovers WHERE id=?",String.class,hop.handover())).isEqualTo("COMPLETED_WITH_DISCREPANCY");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.shipment_events WHERE shipment_id=? AND event_type='MISSING_PARCEL_FOUND'",Integer.class,id)).isEqualTo(1);
        receive(depart(List.of(id),"M","D"),id);
    }
    @Test void confirmedLossRequiresExactIncidentThenCompensatesOnce() {
        Long id=create(); inbound(id); Hop hop=depart(List.of(id),"O","M");
        actor("M"); receipts.reconcileContainer(new DestinationReconciliationCommand(hop.handover(),hop.container(),List.of()));
        Long incident=sql.queryForObject("SELECT id FROM dbo.incidents WHERE shipment_id=?",Long.class,id);
        actor("G");
        assertThatThrownBy(()->finance.compensateLostShipment(id,incident,"Unconfirmed")).isInstanceOf(IllegalStateException.class);
        receipts.resolveMissing(new MissingParcelResolutionCommand(incident,hop.handover(),hop.container(),id,hub("M"),"CONFIRMED_LOST","Inventory and driver evidence reviewed"));
        finance.compensateLostShipment(id,incident,"Approved loss");
        finance.compensateLostShipment(id,incident,"Approved loss");
        assertParcel(id,"CLOSED_EXCEPTION",null);
        assertThat(sql.queryForObject("SELECT balance FROM dbo.wallets WHERE user_id=?",BigDecimal.class,user("S"))).isEqualByComparingTo("200000");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.wallet_transactions WHERE reference_type='INCIDENT' AND reference_id=?",Integer.class,incident)).isEqualTo(1);
    }
}
