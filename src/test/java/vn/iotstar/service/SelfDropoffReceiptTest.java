package vn.iotstar.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.dto.hub.InboundShipmentCommand;
import vn.iotstar.entity.*;
import vn.iotstar.repository.*;
import vn.iotstar.realtime.ShipmentEventPublisher;
import vn.iotstar.service.impl.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SelfDropoffReceiptTest {
    @Mock HubOperationsRepository operations;
    @Mock ShipmentEventRepository events;
    @Mock RoutingExceptionRepository routes;
    @Mock UserRepository users;
    @Mock ApplicationEventPublisher publisher;
    @Mock PaymentRepository payments;
    @Mock PricingPolicyRepository prices;
    @Mock ShipmentCancellation cancellation;
    @Mock ShipmentEventPublisher shipmentPublisher;
    @InjectMocks HubOperationsServiceImpl service;
    Shipment shipment;
    Hub hub;
    User staff;
    Payment base;

    @BeforeEach void setup() {
        staff = new User(7L, "inbound", "hash", UserRole.ROLE_HUB_STAFF, "ACTIVE");
        hub = Hub.active("HUB-A", "A", 1, "DELIVERY_HUB", "SOUTH", "HCM");
        ReflectionTestUtils.setField(hub, "id", 11L);
        shipment = Shipment.create("UTX-DROP", new User(8L, "sender", "hash", UserRole.ROLE_USER, "ACTIVE"),
                "Sender", "0900000001", "a", "Receiver", "0900000002", "b");
        shipment.setId(101L);
        shipment.applyIntake(null, "HCM", "Q1", "SELF_DROPOFF", "SENDER", "NO_INSPECTION", 500, 10, 10, 10,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20000"), "UNPAID", ShipmentStatus.WAITING_FOR_HANDOVER, LocalDateTime.now());
        shipment.setDestinationGrouping("SOUTH", "HCM");
        shipment.setServiceCode("STANDARD");
        base = Payment.shippingFee(shipment, "SENDER", new BigDecimal("20000"), "CASH", "UNPAID");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(staff, null, staff.getAuthorities()));
        when(operations.findActiveProfileByUserId(7L)).thenReturn(Optional.of(EmployeeProfile.active(staff, hub, "INBOUND")));
        when(operations.findShipmentByIdForUpdate(101L)).thenReturn(Optional.of(shipment));
        lenient().when(users.getReferenceById(7L)).thenReturn(staff);
        lenient().when(payments.findFirstByShipmentIdAndPaymentType(101L, "SHIPPING_FEE")).thenReturn(Optional.of(base));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private InboundShipmentCommand scan(int weight, String cash, String decision) {
        return new InboundShipmentCommand(101L, "UTX-DROP", weight, 10, 10, 10, new BigDecimal(cash), decision);
    }
    private void tariff() {
        PricingPolicy p = mock(PricingPolicy.class);
        when(p.getBasePrice()).thenReturn(new BigDecimal("20000"));
        when(p.getBaseWeightG()).thenReturn(500);
        when(p.getExtraWeightStepG()).thenReturn(500);
        when(p.getExtraWeightPrice()).thenReturn(new BigDecimal("5000"));
        when(prices.findFirstByServiceCodeAndFromRegionAndToRegionAndStatusOrderByIdDesc("STANDARD", "SOUTH", "SOUTH", "ACTIVE"))
                .thenReturn(Optional.of(p));
    }
    @Test void freshCashDropoffSetsOriginLocationPaymentAndOneEventAcrossRepeatScan() {
        service.receiveAtHub(scan(450, "20000", null));
        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.AT_HUB);
        assertThat(shipment.getOriginHubId()).isEqualTo(11L);
        assertThat(shipment.getCurrentHubId()).isEqualTo(11L);
        assertThat(base.getStatus()).isEqualTo("PAID");
        assertThat(shipment.getWeight()).isEqualTo(450);
        when(events.existsByShipmentIdAndEventType(101L, "ARRIVED_AT_HUB")).thenReturn(true);
        service.receiveAtHub(scan(450, "20000", null));
        verify(events, times(1)).save(any());
        verify(payments, never()).save(any());
    }
    @Test void wrongCashDoesNotMutateParcelOrPayment() {
        assertThatThrownBy(() -> service.receiveAtHub(scan(500, "1", null))).isInstanceOf(IllegalArgumentException.class);
        assertThat(shipment.getCurrentHubId()).isNull();
        assertThat(base.getStatus()).isEqualTo("UNPAID");
        verifyNoInteractions(events);
    }
    @Test void wrongOriginAndMissingScanAreDeniedBeforePaymentLookup() {
        shipment.setOriginHubId(22L);
        assertThatThrownBy(() -> service.receiveAtHub(scan(500, "20000", null))).isInstanceOf(vn.iotstar.exception.HubOperationAccessDeniedException.class);
        shipment.setOriginHubId(null);
        assertThatThrownBy(() -> service.receiveAtHub(InboundShipmentCommand.pickupReceipt(101L))).isInstanceOf(IllegalArgumentException.class);
        verify(payments, never()).findFirstByShipmentIdAndPaymentType(anyLong(), anyString());
        verifyNoInteractions(events);
    }
    @Test void pendingOnlinePaymentCannotBeReceived() {
        base = Payment.shippingFee(shipment, "SENDER", new BigDecimal("20000"), "VNPAY", "PENDING");
        when(payments.findFirstByShipmentIdAndPaymentType(101L, "SHIPPING_FEE")).thenReturn(Optional.of(base));
        assertThatThrownBy(() -> service.receiveAtHub(scan(500, "0", null))).isInstanceOf(IllegalStateException.class);
        assertThat(shipment.getCurrentHubId()).isNull();
        verifyNoInteractions(events);
    }
    @Test void prepaidWeightSurchargeUsesSeparatePaidPaymentAndNeverRewritesBase() {
        tariff();
        base = Payment.shippingFee(shipment, "SENDER", new BigDecimal("20000"), "VNPAY", "PAID");
        shipment.setPaymentStatus("PAID");
        when(payments.findFirstByShipmentIdAndPaymentType(101L, "SHIPPING_FEE")).thenReturn(Optional.of(base));
        service.receiveAtHub(scan(1000, "5000", "ACCEPT"));
        ArgumentCaptor<Payment> extra = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(extra.capture());
        assertThat(extra.getValue().getPaymentType()).isEqualTo("WEIGHT_SURCHARGE");
        assertThat(extra.getValue().getAmount()).isEqualByComparingTo("5000");
        assertThat(extra.getValue().getStatus()).isEqualTo("PAID");
        assertThat(base.getAmount()).isEqualByComparingTo("20000");
        assertThat(shipment.getShippingFee()).isEqualByComparingTo("20000");
    }
    @Test void receiverWeightSurchargeIncreasesOutstandingObligationWithoutCollectingCash() {
        tariff();
        ReflectionTestUtils.setField(shipment, "feePayer", "RECEIVER");
        base = Payment.shippingFee(shipment, "RECEIVER", new BigDecimal("20000"), "CASH", "UNPAID");
        when(payments.findFirstByShipmentIdAndPaymentType(101L, "SHIPPING_FEE")).thenReturn(Optional.of(base));
        service.receiveAtHub(scan(1000, "0", null));
        assertThat(base.getAmount()).isEqualByComparingTo("25000");
        assertThat(shipment.getShippingFee()).isEqualByComparingTo("25000");
        assertThat(base.getStatus()).isEqualTo("UNPAID");
    }
    @Test void surchargeRequiresExplicitConsentAndExactTotalCash() {
        tariff();
        assertThatThrownBy(() -> service.receiveAtHub(scan(1000, "25000", null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.receiveAtHub(scan(1000, "20000", "ACCEPT"))).isInstanceOf(IllegalArgumentException.class);
        assertThat(base.getStatus()).isEqualTo("UNPAID");
        assertThat(shipment.getCurrentHubId()).isNull();
        verifyNoInteractions(events);
    }
    @Test void refusalUsesSharedCancellationWithoutReceivingOrCollecting() {
        tariff();
        service.receiveAtHub(scan(1000, "0", "REJECT"));
        verify(cancellation).cancel(shipment, "WEIGHT_DISCREPANCY", staff);
        assertThat(base.getStatus()).isEqualTo("UNPAID");
        assertThat(shipment.getCurrentHubId()).isNull();
        verifyNoInteractions(events);
    }
    @Test void onlineAssessmentCreatesPendingObligationWithoutCustodyOrCash() {
        tariff();service.receiveAtHub(scan(1000,"0","PAY_ONLINE"));
        ArgumentCaptor<Payment> assessed=ArgumentCaptor.forClass(Payment.class);verify(payments).save(assessed.capture());
        assertThat(assessed.getValue().getPaymentType()).isEqualTo("WEIGHT_SURCHARGE");
        assertThat(assessed.getValue().getAmount()).isEqualByComparingTo("5000");
        assertThat(assessed.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(base.getStatus()).isEqualTo("UNPAID");assertThat(shipment.getCurrentHubId()).isNull();verifyNoInteractions(events);
    }
    @Test void senderWalletSettlementIsConsumedByPhysicalReceiptExactlyOnce() {
        tariff();Payment extra=Payment.create(shipment,"SENDER","WEIGHT_SURCHARGE",new BigDecimal("5000"),"VNPAY","PENDING");
        when(payments.findFirstByShipmentIdAndPaymentType(101L,"WEIGHT_SURCHARGE")).thenReturn(Optional.of(extra));
        ShipmentRepository locked=mock(ShipmentRepository.class);when(locked.findByIdForUpdate(101L)).thenReturn(Optional.of(shipment));
        WalletLedgerService wallet=mock(WalletLedgerService.class);
        ShipmentFeeService fees=new ShipmentFeeService(locked,payments,mock(ShipmentAssignmentRepository.class),events,users,wallet,mock(LastMileAccess.class));
        User sender=shipment.getSender();SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(sender,null,sender.getAuthorities()));
        fees.paySurcharge(101L,"E_WALLET",false);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(staff,null,staff.getAuthorities()));
        service.receiveAtHub(scan(1000,"20000","ACCEPT"));
        when(events.existsByShipmentIdAndEventType(101L,"ARRIVED_AT_HUB")).thenReturn(true);
        service.receiveAtHub(scan(1000,"20000","ACCEPT"));
        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.AT_HUB);
        assertThat(extra.getMethod()).isEqualTo("E_WALLET");assertThat(extra.getCollectedByUserId()).isEqualTo(8L);
        assertThat(base.getAmount()).isEqualByComparingTo("20000");assertThat(base.getCollectedByUserId()).isEqualTo(7L);
        verify(wallet,times(1)).recordFeeDebit(8L,new BigDecimal("5000"),"WEIGHT_SURCHARGE_PAYMENT",101L,"Sender shipment fee");
        verify(payments,times(1)).save(extra);
    }
    @Test void forgedOrMismatchedPaidSurchargeCannotAuthorizeReceipt() {
        tariff();Payment extra=Payment.create(shipment,"SENDER","WEIGHT_SURCHARGE",new BigDecimal("5000"),"VNPAY","PAID");
        when(payments.findFirstByShipmentIdAndPaymentType(101L,"WEIGHT_SURCHARGE")).thenReturn(Optional.of(extra));
        assertThatThrownBy(()->service.receiveAtHub(scan(1000,"20000","ACCEPT"))).isInstanceOf(IllegalStateException.class);
        assertThat(base.getStatus()).isEqualTo("UNPAID");assertThat(shipment.getCurrentHubId()).isNull();verifyNoInteractions(events);
    }
    @Test void existingAssessmentCannotBeIgnoredByRescanningLowerWeight() {
        Payment extra=Payment.create(shipment,"SENDER","WEIGHT_SURCHARGE",new BigDecimal("5000"),"CASH","PENDING");
        when(payments.findFirstByShipmentIdAndPaymentType(101L,"WEIGHT_SURCHARGE")).thenReturn(Optional.of(extra));
        assertThatThrownBy(()->service.receiveAtHub(scan(500,"20000","ACCEPT"))).isInstanceOf(IllegalStateException.class);
        assertThat(shipment.getCurrentHubId()).isNull();verifyNoInteractions(events);
    }
}
