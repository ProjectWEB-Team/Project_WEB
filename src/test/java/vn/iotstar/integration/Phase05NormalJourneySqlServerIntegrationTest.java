package vn.iotstar.integration;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

@org.junit.jupiter.api.condition.EnabledIf(value="vn.iotstar.integration.Phase05SqlServerFixtureSupport#configured", disabledReason="REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
class Phase05NormalJourneySqlServerIntegrationTest extends Phase05SqlServerFixtureSupport {
    @Test void intakeTwoPhysicalHopsLocalDeliveryReconciliationAndSeparatePayout() {
        Long id=delivered();
        actor("G");
        var result=reconciliation.reconcile(submission(id,new BigDecimal("100000")));
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        Long cod=sql.queryForObject("SELECT id FROM dbo.cod_transactions WHERE shipment_id=?",Long.class,id);
        assertThat(sql.queryForObject("SELECT status FROM dbo.cod_transactions WHERE id=?",String.class,cod)).isEqualTo("READY_FOR_PAYOUT");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.wallet_transactions WHERE user_id=?",Integer.class,user("S"))).isZero();
        reconciliation.payoutReadyCod(cod);
        reconciliation.payoutReadyCod(cod);
        assertThat(sql.queryForObject("SELECT balance FROM dbo.wallets WHERE user_id=?",BigDecimal.class,user("S"))).isEqualByComparingTo("100000");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.wallet_transactions WHERE user_id=? AND transaction_type='COD_PAYOUT'",Integer.class,user("S"))).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.shipment_routes WHERE shipment_id=?",Integer.class,id)).isEqualTo(2);
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.container_items WHERE shipment_id=? AND status='RECEIVED'",Integer.class,id)).isEqualTo(2);
        assertThat(sql.queryForObject("SELECT status FROM dbo.vehicles WHERE id=?",String.class,vehicle())).isEqualTo("AVAILABLE");
        actor("S");
        assertThat(shipments.getDetail(id).events()).anySatisfy(event->assertThat(event.eventType()).isEqualTo("DELIVERED"));
    }
}
