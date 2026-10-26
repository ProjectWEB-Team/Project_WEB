package vn.iotstar.service;
import org.junit.jupiter.api.*;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.iotstar.entity.*;
import vn.iotstar.dto.delivery.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class LastMileCollectionValidationTest {
    LastMileTestFixture f;
    @BeforeEach void setup(){
        f=new LastMileTestFixture();f.shipment.setCodAmount(new BigDecimal("100000"));f.shipment.setFeePayer("RECEIVER");
        f.money.put("SHIPPING_FEE",Payment.create(f.shipment,"RECEIVER","SHIPPING_FEE",new BigDecimal("35000"),"CASH","UNPAID"));f.outbound();
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    DeliveryCollectionRequest request(String cod,String fee,String method){return new DeliveryCollectionRequest(10L,new BigDecimal(cod),new BigDecimal(fee),method);}
    @Test void precreatedWaitingCodAcceptsActualFirstMethodThenReplaysWithoutDuplicateEvidence(){
        assertThat(f.cod.getStatus()).isEqualTo("WAITING_COLLECTION");
        f.service.recordCollection(request("100000","35000","VNPAY"));int events=f.history.size();
        var time=f.cod.getCollectedAt();f.service.recordCollection(request("100000","35000","VNPAY"));
        assertThat(f.cod.getMethod()).isEqualTo("VNPAY");assertThat(f.cod.getCollectedAt()).isEqualTo(time);assertThat(f.history).hasSize(events);
        assertThat(f.money.get("SHIPPING_FEE").getCollectedByUserId()).isEqualTo(2L);
        f.service.confirmDelivery(10L);assertThat(f.shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.DELIVERED);
    }
    @Test void submittedCodOrFeeMismatchRejectsBeforeEitherCollection(){
        assertThatThrownBy(()->f.service.recordCollection(request("1","35000","CASH"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->f.service.recordCollection(request("100000","1","CASH"))).isInstanceOf(IllegalArgumentException.class);
        assertThat(f.cod.getStatus()).isEqualTo("WAITING_COLLECTION");assertThat(f.money.get("SHIPPING_FEE").getStatus()).isEqualTo("UNPAID");
    }
    @Test void malformedExistingReceiverFeeCannotPartiallyCollectCod(){
        f.money.get("SHIPPING_FEE").reviseUnpaidAmount(BigDecimal.ONE);
        assertThatThrownBy(()->f.service.recordCollection(request("100000","35000","CASH"))).isInstanceOf(IllegalStateException.class);
        assertThat(f.cod.getStatus()).isEqualTo("WAITING_COLLECTION");
    }
    @Test void collectedReplayCannotChangeMethodOrAmount(){
        f.service.recordCollection(request("100000","35000","CASH"));var time=f.cod.getCollectedAt();
        assertThatThrownBy(()->f.service.recordCollection(request("100000","35000","VNPAY"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->f.service.recordCollection(request("1","35000","CASH"))).isInstanceOf(IllegalArgumentException.class);
        assertThat(f.cod.getMethod()).isEqualTo("CASH");assertThat(f.cod.getCollectedAt()).isEqualTo(time);
    }
    @Test void wrongCollectorCannotConfirmEvenWhenRecordSaysCollected(){
        f.cod=CodTransaction.createCollected(f.shipment,f.inbound,new BigDecimal("100000"),"CASH");
        assertThatThrownBy(()->f.service.confirmDelivery(10L)).isInstanceOf(IllegalStateException.class);
        assertThat(f.shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.OUT_FOR_DELIVERY);
    }
    @Test void waitingCodRebindsToNewAssignedShipperOnlyAfterPhysicalRetryReceipt(){
        f.service.recordFailure(new DeliveryFailureRequest(10L,"NO_ANSWER",null,null,null));
        LastMileTestFixture.login(f.inbound);f.service.receiveFailedDelivery(10L,"TRACK");
        f.cod.assignWaitingCollector(f.inbound); // Represents preceding assignment's collector, not cash collection.
        f.clock.advanceDays(1);
        f.outbound();assertThat(f.cod.getCollectedByShipper()).isEqualTo(f.shipper);assertThat(f.cod.getStatus()).isEqualTo("WAITING_COLLECTION");
    }
    @Test void transferCodAndCashReceiverFeeRemainSeparateAndReplaySafe(){
        var request=request("100000","35000","CASH");request.setCodPaymentMethod("TRANSFER");request.setShippingFeePaymentMethod("CASH");
        f.service.recordCollection(request);int eventCount=f.history.size();f.service.recordCollection(request);
        assertThat(f.cod.getMethod()).isEqualTo("TRANSFER");assertThat(f.money.get("SHIPPING_FEE").getMethod()).isEqualTo("CASH");assertThat(f.history).hasSize(eventCount);
        f.service.confirmDelivery(10L);assertThat(f.shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(f.cod.getStatus()).isEqualTo("COLLECTED"); // Finance verification, not the delivery actor, authorizes payout.
    }
    @Test void unsupportedReceiverFeeMethodCannotPartiallyCollectTransferCod(){
        var request=request("100000","35000","TRANSFER");
        assertThatThrownBy(()->f.service.recordCollection(request)).isInstanceOf(IllegalArgumentException.class);
        assertThat(f.cod.getStatus()).isEqualTo("WAITING_COLLECTION");assertThat(f.money.get("SHIPPING_FEE").getStatus()).isEqualTo("UNPAID");
    }
}
