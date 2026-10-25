package vn.iotstar.service;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.iotstar.dto.delivery.*;
import vn.iotstar.dto.returnflow.ReturnHandoverRequest;
import vn.iotstar.entity.*;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class LastMileReturnServiceImplTest {
    LastMileTestFixture f;
    @BeforeEach void setup(){f=new LastMileTestFixture();}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    void fail(String reason){f.service.recordFailure(new DeliveryFailureRequest(10L,reason,"Failed","evidence.jpg",null));}
    @Test void retryRequiresRealInboundReceiptBeforeNewAssignment(){
        f.outbound();fail("NO_ANSWER");assertThat(f.shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.FAILED);
        assertThat(f.shipment.getCurrentHubId()).isNull();assertThat(f.tasks).hasSize(1);
        LastMileTestFixture.login(f.coordinator);assertThatThrownBy(()->f.dispatch.assign(new DeliveryAssignmentRequest(10L,2L))).isInstanceOf(AccessDeniedException.class);
        LastMileTestFixture.login(f.inbound);f.service.receiveFailedDelivery(10L,"TRACK");
        assertThat(f.shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.AT_HUB);
        LastMileTestFixture.login(f.coordinator);assertThatThrownBy(()->f.dispatch.assign(new DeliveryAssignmentRequest(10L,2L))).isInstanceOf(IllegalStateException.class);
        f.clock.advanceDays(1);f.dispatch.assign(new DeliveryAssignmentRequest(10L,2L));assertThat(f.tasks).hasSize(2);
    }
    @Test void thirdFailureCancelsWaitingCodAndStartsReturn(){
        f.outbound();when(f.attempts.countByShipmentId(10L)).thenReturn(2L);
        var cod=CodTransaction.createWaiting(f.shipment,f.shipper,BigDecimal.TEN,"CASH");when(f.cods.findByShipmentId(10L)).thenReturn(Optional.of(cod));
        fail("NO_ANSWER");assertThat(f.shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.RETURN_TO_SENDER);
        assertThat(cod.getStatus()).isEqualTo("CANCELLED");assertThat(f.shipment.getCurrentHubId()).isNull();
    }
    @Test void fastFailureUsesAuthorizedDefaultAndReceiptStartsGraceOnlyOnce(){
        f.outbound();fail("RECEIVER_REFUSED");assertThat(f.shipment.getReturnFee()).isEqualByComparingTo("25000");
        assertThat(f.shipment.getReturnArrivedAtOriginAt()).isNull();LastMileTestFixture.login(f.inbound);
        f.service.receiveFailedDelivery(10L,"TRACK");var first=f.shipment.getReturnArrivedAtOriginAt();
        f.service.receiveFailedDelivery(10L,"TRACK");assertThat(f.shipment.getReturnArrivedAtOriginAt()).isEqualTo(first);
        assertThat(f.history.stream().filter(e->e.getEventType().equals("RETURNED_AT_ORIGIN_HUB"))).hasSize(1);
    }
    @Test void configuredReturnFeeOverrideApplied(){f.config.getFinance().setDefaultReturnFee(new BigDecimal("28000"));f.outbound();fail("WRONG_ADDRESS");assertThat(f.shipment.getReturnFee()).isEqualByComparingTo("28000");}
    @Test void alreadyPricedReturnFeePreserved(){f.shipment.setReturnFee(new BigDecimal("18000"));f.outbound();fail("WRONG_ADDRESS");assertThat(f.shipment.getReturnFee()).isEqualByComparingTo("18000");}
    @Test void senderCannotClaimCashOrReleaseOwnParcel(){
        f.returnAtOrigin();LastMileTestFixture.login(f.sender);
        assertThatThrownBy(()->f.fees.payReturn(10L,"CASH",false)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->f.service.processReturnHandover(new ReturnHandoverRequest(10L,"SELF_PICKUP","id",new BigDecimal("25000"),null))).isInstanceOf(AccessDeniedException.class);
    }
    @Test void hubReleaseRequiresRecordedFeesAndVerifiedIdentity(){
        f.returnAtOrigin();LastMileTestFixture.login(f.inbound);var request=new ReturnHandoverRequest(10L,"SELF_PICKUP","checked-id",new BigDecimal("25000"),null);
        assertThatThrownBy(()->f.service.processReturnHandover(request)).isInstanceOf(IllegalStateException.class);
        f.fees.payReturn(10L,"CASH",false);f.service.processReturnHandover(request);f.service.processReturnHandover(request);
        assertThat(f.shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.RETURNED_TO_SENDER);
        assertThat(f.money.get("RETURN_FEE").getCollectedByUserId()).isEqualTo(3L);
        assertThat(f.history.stream().filter(e->e.getEventType().equals("RETURNED_TO_SENDER"))).hasSize(1);
    }
    @Test void returnDeliveryNeedsScanAndFeesBeforeConfirmation(){
        f.returnAtOrigin();LastMileTestFixture.login(f.coordinator);f.dispatch.assign(new DeliveryAssignmentRequest(10L,2L));
        LastMileTestFixture.login(f.shipper);assertThatThrownBy(()->f.service.confirmReturnDelivery(10L)).isInstanceOf(IllegalStateException.class);
        f.service.scanReturnDelivery(10L);assertThat(f.shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.OUT_FOR_DELIVERY);
        assertThat(f.shipment.getCurrentHubId()).isNull();assertThatThrownBy(()->f.service.confirmReturnDelivery(10L)).isInstanceOf(IllegalStateException.class);
        f.fees.payReturn(10L,"CASH",false);f.service.confirmReturnDelivery(10L);f.service.confirmReturnDelivery(10L);
        assertThat(f.tasks.getFirst().getStatus()).isEqualTo("COMPLETED");assertThat(f.money.get("RETURN_FEE").getCollectedByUserId()).isEqualTo(2L);
    }
    @Test void unpaidReceiverFeeAndReturnFeeSettleSeparatelyWithWalletOnce(){
        f.returnAtOrigin();f.shipment.setFeePayer("RECEIVER");f.money.put("SHIPPING_FEE",Payment.create(f.shipment,"RECEIVER","SHIPPING_FEE",new BigDecimal("35000"),"CASH","UNPAID"));
        LastMileTestFixture.login(f.sender);f.fees.payReturn(10L,"E_WALLET",false);f.fees.payReturn(10L,"E_WALLET",false);
        verify(f.wallet).recordFeeDebit(eq(1L),eq(new BigDecimal("35000")),eq("FORWARD_FEE_ON_RETURN_PAYMENT"),eq(10L),anyString());
        verify(f.wallet).recordFeeDebit(eq(1L),eq(new BigDecimal("25000")),eq("RETURN_FEE_PAYMENT"),eq(10L),anyString());
        assertThat(f.money.get("SHIPPING_FEE").getStatus()).isEqualTo("UNPAID");f.fees.requireReturnSettled(f.shipment);
    }
    @Test void unknownHandoverAndNullOriginDenied(){
        f.returnAtOrigin();LastMileTestFixture.login(f.inbound);
        assertThatThrownBy(()->f.service.processReturnHandover(new ReturnHandoverRequest(10L,"OTHER","id",null,null))).isInstanceOf(IllegalArgumentException.class);
        f.shipment.setOriginHubId(null);assertThatThrownBy(()->f.service.processReturnHandover(new ReturnHandoverRequest(10L,"SELF_PICKUP","id",null,null))).isInstanceOf(AccessDeniedException.class);
    }
    @Test void duplicateAssignmentAndWrongPositionDenied(){
        LastMileTestFixture.login(f.inbound);assertThatThrownBy(()->f.dispatch.assign(new DeliveryAssignmentRequest(10L,2L))).isInstanceOf(AccessDeniedException.class);
        LastMileTestFixture.login(f.coordinator);f.dispatch.assign(new DeliveryAssignmentRequest(10L,2L));
        assertThatThrownBy(()->f.dispatch.assign(new DeliveryAssignmentRequest(10L,2L))).isInstanceOf(IllegalStateException.class);assertThat(f.tasks).hasSize(1);
    }
    @Test void heldAndWrongHubAssignmentsDenied(){
        LastMileTestFixture.login(f.coordinator);f.shipment.setOnHold(true);
        assertThatThrownBy(()->f.dispatch.assign(new DeliveryAssignmentRequest(10L,2L))).isInstanceOf(AccessDeniedException.class);
        f.shipment.setOnHold(false);f.shipment.setCurrentHubId(999L);assertThatThrownBy(()->f.dispatch.assign(new DeliveryAssignmentRequest(10L,2L))).isInstanceOf(AccessDeniedException.class);
    }
    @Test void rescheduledRetryRequiresDateAtBothAssignmentAndOutboundScan(){
        f.outbound();var promised=f.access.today().plusDays(3);
        f.service.recordFailure(new DeliveryFailureRequest(10L,"RESCHEDULED","Customer request",null,promised));
        LastMileTestFixture.login(f.inbound);f.service.receiveFailedDelivery(10L,"TRACK");
        LastMileTestFixture.login(f.coordinator);
        assertThatThrownBy(()->f.dispatch.assign(new DeliveryAssignmentRequest(10L,2L))).isInstanceOf(IllegalStateException.class);
        when(f.shipments.findAssignableLastMile(eq(100L),any())).thenReturn(java.util.List.of(f.shipment));
        assertThat(f.dispatch.candidates()).isEmpty();f.clock.advanceDays(2);
        assertThatThrownBy(()->f.dispatch.assign(new DeliveryAssignmentRequest(10L,2L))).isInstanceOf(IllegalStateException.class);
        // A pre-existing malformed assignment must not bypass the outbound custody guard.
        ShipmentAssignment premature=ShipmentAssignment.create(f.shipment,f.shipper,"DELIVERY");f.tasks.add(premature);LastMileTestFixture.login(f.shipper);
        assertThatThrownBy(()->f.service.scanForDelivery(new DeliveryScanRequest(10L,100L))).isInstanceOf(IllegalStateException.class);
        assertThat(f.shipment.getCurrentHubId()).isEqualTo(100L);assertThat(premature.getStatus()).isEqualTo("ASSIGNED");f.tasks.remove(premature);
        f.clock.advanceDays(1);LastMileTestFixture.login(f.coordinator);assertThat(f.dispatch.candidates()).hasSize(1);
        f.dispatch.assign(new DeliveryAssignmentRequest(10L,2L));LastMileTestFixture.login(f.shipper);f.service.scanForDelivery(new DeliveryScanRequest(10L,100L));
        assertThat(f.shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.OUT_FOR_DELIVERY);assertThat(f.shipment.getCurrentHubId()).isNull();
    }
}
