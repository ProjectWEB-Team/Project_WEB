package vn.iotstar.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vn.iotstar.config.LogisticsProperties;
import vn.iotstar.dto.delivery.*;
import vn.iotstar.dto.returnflow.ReturnHandoverRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.condition.EnabledIf(value="vn.iotstar.integration.Phase05SqlServerFixtureSupport#configured", disabledReason="REAL_SQLSERVER_VERIFICATION = BLOCKED_BY_ENVIRONMENT")
class Phase05DeliveryReturnSqlServerIntegrationTest extends Phase05SqlServerFixtureSupport {
    @Autowired LogisticsProperties policy;
    @Test void retryRequiresPhysicalReceiptAndTheNextEligibleDay() {
        Long id=atDestination(); outbound(id);
        delivery.recordFailure(new DeliveryFailureRequest(id,"NO_ANSWER","No reply",null,null));
        assertParcel(id,"FAILED",null);
        actor("D"); assertThatThrownBy(()->assignments.assign(new DeliveryAssignmentRequest(id,user("P")))).isInstanceOf(RuntimeException.class);
        delivery.receiveFailedDelivery(id,tracking(id)); assertParcel(id,"AT_HUB",hub("D"));
        assertThatThrownBy(()->assignments.assign(new DeliveryAssignmentRequest(id,user("P")))).isInstanceOf(RuntimeException.class);
        doReturn(Instant.now().plus(2,ChronoUnit.DAYS)).when(clock).instant();
        assignments.assign(new DeliveryAssignmentRequest(id,user("P")));
        actor("P"); delivery.scanForDelivery(new DeliveryScanRequest(id,hub("D")));
        assertParcel(id,"OUT_FOR_DELIVERY",null);
    }
    Long returnAtOrigin() {
        Long id=atDestination(); outbound(id);
        delivery.recordFailure(new DeliveryFailureRequest(id,"RECEIVER_REFUSED","Refused at door",null,null));
        assertParcel(id,"RETURN_TO_SENDER",null);
        assertThat(sql.queryForObject("SELECT return_arrived_at_origin_at FROM dbo.shipments WHERE id=?",Object.class,id)).isNull();
        actor("D"); delivery.receiveFailedDelivery(id,tracking(id));
        receive(depart(List.of(id),"D","M"),id);
        receive(depart(List.of(id),"M","O"),id);
        assertParcel(id,"RETURN_TO_SENDER",hub("O"));
        assertThat(sql.queryForObject("SELECT return_arrived_at_origin_at FROM dbo.shipments WHERE id=?",Object.class,id)).isNotNull();
        return id;
    }
    @Test void senderSettlesUnpaidForwardAndDefaultReturnFeeBeforeStaffSelfPickup() {
        Long id=returnAtOrigin();
        assertThat(sql.queryForObject("SELECT return_fee FROM dbo.shipments WHERE id=?",BigDecimal.class,id)).isEqualByComparingTo("25000");
        Object arrival=sql.queryForObject("SELECT return_arrived_at_origin_at FROM dbo.shipments WHERE id=?",Object.class,id);
        var command=new ReturnHandoverRequest(id,"SELF_PICKUP","Verified sender reference",null,"Physical handover");
        assertThatThrownBy(()->delivery.processReturnHandover(command)).isInstanceOf(IllegalStateException.class);
        actor("S"); fees.payReturn(id,"VNPAY",true); fees.payReturn(id,"VNPAY",true);
        actor("O"); delivery.processReturnHandover(command); delivery.processReturnHandover(command);
        assertParcel(id,"RETURNED_TO_SENDER",null);
        assertThat(sql.queryForObject("SELECT SUM(amount) FROM dbo.payments WHERE shipment_id=? AND payment_type IN ('FORWARD_FEE_ON_RETURN','RETURN_FEE') AND status='PAID'",BigDecimal.class,id)).isEqualByComparingTo("55000");
        assertThat(sql.queryForObject("SELECT return_arrived_at_origin_at FROM dbo.shipments WHERE id=?",Object.class,id)).isEqualTo(arrival);
    }
    @Test void configuredReturnFeeAndHomeReturnRequireActualOutboundCustody() {
        BigDecimal original=policy.getFinance().getDefaultReturnFee();
        try {
            policy.getFinance().setDefaultReturnFee(new BigDecimal("32000"));
            Long id=returnAtOrigin();
            assertThat(sql.queryForObject("SELECT return_fee FROM dbo.shipments WHERE id=?",BigDecimal.class,id)).isEqualByComparingTo("32000");
            assignments.assign(new DeliveryAssignmentRequest(id,user("Q")));
            actor("Q"); assertThatThrownBy(()->delivery.confirmReturnDelivery(id)).isInstanceOf(IllegalStateException.class);
            delivery.scanReturnDelivery(id);
            assertThatThrownBy(()->delivery.confirmReturnDelivery(id)).isInstanceOf(IllegalStateException.class);
            actor("S"); fees.payReturn(id,"VNPAY",true);
            actor("Q"); delivery.confirmReturnDelivery(id); delivery.confirmReturnDelivery(id);
            assertParcel(id,"RETURNED_TO_SENDER",null);
        } finally { policy.getFinance().setDefaultReturnFee(original); }
    }
    @Test void overdueDisposalUsesImmutablePhysicalArrivalAndAuthorizedManagerAction() {
        Long id=returnAtOrigin(); Object arrived=sql.queryForObject("SELECT return_arrived_at_origin_at FROM dbo.shipments WHERE id=?",Object.class,id);
        actor("G"); assertThatThrownBy(()->returns.disposeOverdueReturn(id,"Overdue inventory")).isInstanceOf(IllegalStateException.class);
        doReturn(Instant.now().plus(60,ChronoUnit.DAYS)).when(clock).instant();
        String reason="R".repeat(255);
        returns.disposeOverdueReturn(id,reason);
        assertThat(sql.queryForObject("SELECT description FROM dbo.shipment_events WHERE shipment_id=? AND event_type='RETURN_DISPOSED'",String.class,id))
                .hasSize(255).isEqualTo(reason);
        assertThat(sql.queryForObject("SELECT LEN(description) FROM dbo.shipment_events WHERE shipment_id=? AND event_type='RETURN_DISPOSED'",Integer.class,id)).isEqualTo(255);
        assertParcel(id,"CLOSED_EXCEPTION",hub("O"));
        assertThat(sql.queryForObject("SELECT closure_reason FROM dbo.shipments WHERE id=?",String.class,id)).isEqualTo("DISPOSED");
        assertThat(sql.queryForObject("SELECT return_arrived_at_origin_at FROM dbo.shipments WHERE id=?",Object.class,id)).isEqualTo(arrived);
    }
}
