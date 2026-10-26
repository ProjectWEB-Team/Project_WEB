package vn.iotstar.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import vn.iotstar.dto.finance.ReconciliationSubmissionRequest;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

/** Retains Phase04's combined-source assertion using the current actor/custody/collection contracts. */
@EnabledIf(value="vn.iotstar.integration.Phase05SqlServerFixtureSupport#configured",
        disabledReason="REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
class Phase04FinanceSqlServerIntegrationTest extends Phase05SqlServerFixtureSupport {
    @Test void reconcilesCodAndFeeSourcesOnSqlServer() {
        Long id=delivered(); actor("G");
        var request=submission(id,new BigDecimal("100000"));
        Long payment=sql.queryForObject("SELECT id FROM dbo.payments WHERE shipment_id=? AND payment_type='SHIPPING_FEE'",Long.class,id);
        var rows=new java.util.ArrayList<>(request.getItems());
        rows.add(new ReconciliationSubmissionRequest.ItemRequest(id,"RECEIVER_CASH_SHIPPING_FEE_PAYMENT",payment,
                new BigDecimal("30000"),new BigDecimal("30000"),"Recorded receiver fee"));
        request.setItems(rows);
        var rec=reconciliation.reconcile(request);
        assertThat(rec.getStatus()).isEqualTo("COMPLETED");
        assertThat(rec.getExpectedAmount()).isEqualByComparingTo("130000");
        Long cod=sql.queryForObject("SELECT id FROM dbo.cod_transactions WHERE shipment_id=?",Long.class,id);
        assertThat(sql.queryForObject("SELECT status FROM dbo.cod_transactions WHERE id=?",String.class,cod)).isEqualTo("READY_FOR_PAYOUT");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.wallet_transactions WHERE user_id=?",Integer.class,user("S"))).isZero();
        reconciliation.payoutReadyCod(cod);
        assertThat(sql.queryForObject("SELECT balance FROM dbo.wallets WHERE user_id=?",BigDecimal.class,user("S"))).isEqualByComparingTo("100000");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.wallet_transactions WHERE user_id=? AND transaction_type='COD_PAYOUT'",Integer.class,user("S"))).isEqualTo(1);
    }
}
