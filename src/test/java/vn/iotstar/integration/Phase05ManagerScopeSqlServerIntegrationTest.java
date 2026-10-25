package vn.iotstar.integration;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import vn.iotstar.dto.hub.DestinationReconciliationCommand;
import static org.assertj.core.api.Assertions.*;

@org.junit.jupiter.api.condition.EnabledIf(value="vn.iotstar.integration.Phase05SqlServerFixtureSupport#configured", disabledReason="REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
class Phase05ManagerScopeSqlServerIntegrationTest extends Phase05SqlServerFixtureSupport {
    @Test void outsideSubtreeAndSenderCannotReadOrResolveRealIncident() {
        Long id=create(); inbound(id); Hop hop=depart(List.of(id),"O","M");
        actor("M"); receipts.reconcileContainer(new DestinationReconciliationCommand(hop.handover(),hop.container(),List.of()));
        Long incident=sql.queryForObject("SELECT id FROM dbo.incidents WHERE shipment_id=?",Long.class,id);
        actor("G"); assertThat(receipts.missingCases()).extracting("incidentId").contains(incident);
        actor("X"); assertThat(receipts.missingCases()).isEmpty(); assertThat(managers.getWorkItems()).isEmpty();
        assertThatThrownBy(()->receipts.missingCase(incident)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->managers.getWorkItem("MISSING",incident)).isInstanceOf(RuntimeException.class);
        actor("S"); assertThatThrownBy(()->receipts.missingCase(incident)).isInstanceOf(RuntimeException.class);
    }
    @Test void crossHubManagerCannotDiscloseReconciliationOrPayOut() {
        Long id=delivered(); actor("G"); Long rec=reconciliation.reconcile(submission(id,new BigDecimal("100000"))).getId();
        Long cod=sql.queryForObject("SELECT id FROM dbo.cod_transactions WHERE shipment_id=?",Long.class,id);
        actor("X");
        assertThatThrownBy(()->reconciliation.getReconciliationItems(rec)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->reconciliation.getReconciliationsByShipper(user("P"))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->reconciliation.payoutReadyCod(cod)).isInstanceOf(RuntimeException.class);
        assertThat(sql.queryForObject("SELECT status FROM dbo.cod_transactions WHERE id=?",String.class,cod)).isEqualTo("READY_FOR_PAYOUT");
    }
}
