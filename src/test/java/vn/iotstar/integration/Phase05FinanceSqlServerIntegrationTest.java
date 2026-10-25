package vn.iotstar.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.iotstar.dto.hub.InboundShipmentCommand;
import vn.iotstar.dto.delivery.DeliveryCollectionRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.condition.EnabledIf(value="vn.iotstar.integration.Phase05SqlServerFixtureSupport#configured", disabledReason="REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
class Phase05FinanceSqlServerIntegrationTest extends Phase05SqlServerFixtureSupport {
    @ParameterizedTest @ValueSource(strings={"SELF_DROPOFF","HOME_PICKUP"})
    void actualTimeoutProducerCancelsAndSettlesPaidFeeWithoutManufacturedState(String pickup) {
        actor("S"); var form=request("SENDER","VNPAY"); form.setPickupMethod(pickup);
        Long id=shipments.create(form); shipments.confirmPayment(id);
        if("HOME_PICKUP".equals(pickup)) { actor("O"); firstMile.assignPickup(id,user("Q")); }
        doReturn(Instant.now().plus(3,ChronoUnit.DAYS)).when(clock).instant();
        timeouts.cancelExpired();
        assertParcel(id,"CANCELLED",null);
        assertThat(sql.queryForObject("SELECT cancel_reason FROM dbo.shipments WHERE id=?",String.class,id))
                .isEqualTo("HOME_PICKUP".equals(pickup)?"PICKUP_TIMEOUT":"DROPOFF_TIMEOUT");
        Long payment=sql.queryForObject("SELECT id FROM dbo.payments WHERE shipment_id=? AND payment_type='SHIPPING_FEE'",Long.class,id);
        finance.settlePendingRefund(payment); finance.settlePendingRefund(payment);
        assertThat(sql.queryForObject("SELECT status FROM dbo.refund_settlement_outbox WHERE payment_id=?",String.class,payment)).isEqualTo("SETTLED");
        assertThat(sql.queryForObject("SELECT balance FROM dbo.wallets WHERE user_id=?",BigDecimal.class,user("S"))).isEqualByComparingTo("30000");
        if("HOME_PICKUP".equals(pickup)) assertThat(sql.queryForObject("SELECT status FROM dbo.shipment_assignments WHERE shipment_id=?",String.class,id)).isEqualTo("CANCELLED");
    }
    @Test void prepaidShippingAndPaidAssessedSurchargeBothRefundOnceAfterSenderCancellation() {
        actor("S"); Long id=shipments.create(request("SENDER","VNPAY")); shipments.confirmPayment(id);
        actor("O"); hubs.receiveAtHub(new InboundShipmentCommand(id,tracking(id),1500,10,10,10,BigDecimal.ZERO,"PAY_ONLINE"));
        assertParcel(id,"WAITING_FOR_HANDOVER",null);
        actor("S"); fees.paySurcharge(id,"VNPAY",true); shipments.cancel(id);
        var ids=sql.queryForList("SELECT payment_id FROM dbo.refund_settlement_outbox WHERE shipment_id=?",Long.class,id);
        assertThat(ids).hasSize(2);
        for(Long payment:ids) { finance.settlePendingRefund(payment); finance.settlePendingRefund(payment); }
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.refund_settlement_outbox WHERE shipment_id=? AND status='SETTLED'",Integer.class,id)).isEqualTo(2);
        assertThat(sql.queryForObject("SELECT balance FROM dbo.wallets WHERE user_id=?",BigDecimal.class,user("S"))).isEqualByComparingTo("35000");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.wallet_transactions WHERE user_id=? AND transaction_type='REFUND'",Integer.class,user("S"))).isEqualTo(2);
    }
    @ParameterizedTest @ValueSource(strings={"VNPAY","TRANSFER"})
    void electronicCodNeedsMethodSpecificManagerEvidenceAndSeparatePayout(String method) {
        Long id=atDestination(); outbound(id);
        var collection=new DeliveryCollectionRequest(id,new BigDecimal("100000"),new BigDecimal("30000"),"CASH");
        collection.setCodPaymentMethod(method); collection.setShippingFeePaymentMethod("CASH");
        delivery.recordCollection(collection); delivery.confirmDelivery(id);
        actor("G"); var request=submission(id,new BigDecimal("100000")); request.setCollectionMethod(method);
        assertThatThrownBy(()->reconciliation.reconcile(request)).isInstanceOf(IllegalArgumentException.class);
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.shipper_reconciliations WHERE shipper_id=?",Integer.class,user("P"))).isZero();
        request.setSettlementEvidence("Manager checked demonstration receipt "+method);
        Long rec=reconciliation.reconcile(request).getId();
        assertThat(sql.queryForObject("SELECT collection_method FROM dbo.shipper_reconciliations WHERE id=?",String.class,rec)).isEqualTo(method);
        assertThat(sql.queryForObject("SELECT settlement_evidence FROM dbo.shipper_reconciliations WHERE id=?",String.class,rec)).contains("demonstration receipt");
        Long cod=sql.queryForObject("SELECT id FROM dbo.cod_transactions WHERE shipment_id=?",Long.class,id);
        assertThat(sql.queryForObject("SELECT status FROM dbo.cod_transactions WHERE id=?",String.class,cod)).isEqualTo("READY_FOR_PAYOUT");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.wallet_transactions WHERE user_id=?",Integer.class,user("S"))).isZero();
        reconciliation.payoutReadyCod(cod);
        assertThat(sql.queryForObject("SELECT balance FROM dbo.wallets WHERE user_id=?",BigDecimal.class,user("S"))).isEqualByComparingTo("100000");
    }
    @Test void cancellationOutboxSettlesConcurrentlyToOneRefundAndOneWalletCredit() throws Exception {
        actor("S"); Long id=shipments.create(request("SENDER","VNPAY"));
        shipments.confirmPayment(id); shipments.cancel(id);
        Long payment=sql.queryForObject("SELECT id FROM dbo.payments WHERE shipment_id=? AND payment_type='SHIPPING_FEE'",Long.class,id);
        simultaneous("G",()->finance.settlePendingRefund(payment));
        assertThat(sql.queryForObject("SELECT status FROM dbo.refund_settlement_outbox WHERE payment_id=?",String.class,payment)).isEqualTo("SETTLED");
        assertThat(sql.queryForObject("SELECT status FROM dbo.payments WHERE id=?",String.class,payment)).isEqualTo("REFUNDED");
        assertThat(sql.queryForObject("SELECT balance FROM dbo.wallets WHERE user_id=?",BigDecimal.class,user("S"))).isEqualByComparingTo("30000");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.wallet_transactions WHERE reference_type='PAYMENT' AND reference_id=? AND transaction_type='REFUND'",Integer.class,payment)).isEqualTo(1);
    }
    @Test void concurrentReconciliationSourceAndPayoutCommitOnlyOneMovement() throws Exception {
        Long id=delivered(); var request=submission(id,new BigDecimal("100000"));
        simultaneous("G",()->reconciliation.reconcile(request));
        Long cod=sql.queryForObject("SELECT id FROM dbo.cod_transactions WHERE shipment_id=?",Long.class,id);
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.shipper_reconciliation_items WHERE source_type='COD_TRANSACTION' AND source_id=?",Integer.class,cod)).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.shipper_reconciliations WHERE shipper_id=?",Integer.class,user("P"))).isEqualTo(1);
        simultaneous("G",()->reconciliation.payoutReadyCod(cod));
        assertThat(sql.queryForObject("SELECT balance FROM dbo.wallets WHERE user_id=?",BigDecimal.class,user("S"))).isEqualByComparingTo("100000");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.wallet_transactions WHERE user_id=? AND transaction_type='COD_PAYOUT'",Integer.class,user("S"))).isEqualTo(1);
    }
    @ParameterizedTest @ValueSource(strings={"90000","110000"})
    void discrepancyCannotBePaidAndWrongSourceLeavesNoHeaderOrLedger(String actual) {
        Long id=delivered(); actor("G"); var invalid=submission(id,new BigDecimal("100000"));
        invalid.getItems().getFirst().setShipmentId(id+1000000);
        assertThatThrownBy(()->reconciliation.reconcile(invalid)).isInstanceOf(RuntimeException.class);
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.shipper_reconciliations WHERE shipper_id=?",Integer.class,user("P"))).isZero();
        reconciliation.reconcile(submission(id,new BigDecimal(actual)));
        Long cod=sql.queryForObject("SELECT id FROM dbo.cod_transactions WHERE shipment_id=?",Long.class,id);
        assertThatThrownBy(()->reconciliation.payoutReadyCod(cod)).isInstanceOf(RuntimeException.class);
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM dbo.wallet_transactions WHERE user_id=?",Integer.class,user("S"))).isZero();
    }
}
