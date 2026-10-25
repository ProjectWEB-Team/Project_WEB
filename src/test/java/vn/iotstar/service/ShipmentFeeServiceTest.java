package vn.iotstar.service;

import org.junit.jupiter.api.*;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.iotstar.entity.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShipmentFeeServiceTest {
    LastMileTestFixture f;
    @BeforeEach void setup(){f=new LastMileTestFixture();}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    void surcharge(){
        org.springframework.test.util.ReflectionTestUtils.setField(f.shipment,"pickupMethod","SELF_DROPOFF");f.shipment.transitionTo(ShipmentStatus.WAITING_FOR_HANDOVER);f.shipment.setCurrentHubId(null);
        f.money.put("WEIGHT_SURCHARGE",Payment.create(f.shipment,"SENDER","WEIGHT_SURCHARGE",new BigDecimal("5000"),"VNPAY","PENDING"));
        LastMileTestFixture.login(f.sender);
    }
    @Test void walletSettlesAssessedSurchargeOnceWithSenderEvidence(){
        surcharge();f.fees.paySurcharge(10L,"E_WALLET",false);f.fees.paySurcharge(10L,"E_WALLET",false);
        Payment p=f.money.get("WEIGHT_SURCHARGE");assertThat(p.getStatus()).isEqualTo("PAID");assertThat(p.getCollectedByUserId()).isEqualTo(1L);
        assertThat(p.getPaidAt()).isNotNull();assertThat(p.getTransactionRef()).isNotBlank();
        verify(f.wallet,times(1)).recordFeeDebit(1L,new BigDecimal("5000"),"WEIGHT_SURCHARGE_PAYMENT",10L,"Sender shipment fee");
        assertThat(f.shipment.getCurrentHubId()).isNull();
    }
    @Test void demoVnpayRequiresExplicitConfirmationAndNeverDebitsWallet(){
        surcharge();assertThatThrownBy(()->f.fees.paySurcharge(10L,"VNPAY",false)).isInstanceOf(IllegalArgumentException.class);
        f.fees.paySurcharge(10L,"VNPAY",true);assertThat(f.money.get("WEIGHT_SURCHARGE").getTransactionRef()).startsWith("DEMO-VNPAY-");verifyNoInteractions(f.wallet);
    }
    @Test void ownerCannotClaimCashOrUnsupportedMethod(){
        surcharge();for(String method:java.util.List.of("CASH","BANK","WALLET")) assertThatThrownBy(()->f.fees.paySurcharge(10L,method,true)).isInstanceOf(IllegalArgumentException.class);
        assertThat(f.money.get("WEIGHT_SURCHARGE").getStatus()).isEqualTo("PENDING");verifyNoInteractions(f.wallet);
    }
    @Test void anotherUserCannotSettleOrReadSurcharge(){
        surcharge();LastMileTestFixture.login(new User(99L,"other","hash",UserRole.ROLE_USER,"ACTIVE"));
        assertThatThrownBy(()->f.fees.paySurcharge(10L,"E_WALLET",false)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->f.fees.view(10L)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);verifyNoInteractions(f.wallet);
    }
    @Test void paidReplayCannotChangeMethodOrCollector(){
        surcharge();f.fees.paySurcharge(10L,"E_WALLET",false);
        assertThatThrownBy(()->f.fees.paySurcharge(10L,"VNPAY",true)).isInstanceOf(IllegalStateException.class);
        assertThat(f.money.get("WEIGHT_SURCHARGE").getMethod()).isEqualTo("E_WALLET");
    }
    @Test void wrongPayerAndMissingPaidEvidenceAreRejected(){
        surcharge();f.money.put("WEIGHT_SURCHARGE",Payment.create(f.shipment,"RECEIVER","WEIGHT_SURCHARGE",new BigDecimal("5000"),"VNPAY","PENDING"));
        assertThatThrownBy(()->f.fees.paySurcharge(10L,"E_WALLET",false)).isInstanceOf(IllegalStateException.class);
        f.money.put("WEIGHT_SURCHARGE",Payment.create(f.shipment,"SENDER","WEIGHT_SURCHARGE",new BigDecimal("5000"),"VNPAY","PAID"));
        assertThatThrownBy(()->f.fees.paySurcharge(10L,"VNPAY",true)).isInstanceOf(IllegalStateException.class);verifyNoInteractions(f.wallet);
    }
    @Test void invalidSecondLiabilityPreventsFirstWalletDebit(){
        f.returnAtOrigin();f.shipment.setFeePayer("RECEIVER");f.money.put("SHIPPING_FEE",Payment.create(f.shipment,"RECEIVER","SHIPPING_FEE",new BigDecimal("35000"),"CASH","UNPAID"));
        f.money.put("RETURN_FEE",Payment.create(f.shipment,"SENDER","RETURN_FEE",BigDecimal.ONE,"CASH","UNPAID"));LastMileTestFixture.login(f.sender);
        assertThatThrownBy(()->f.fees.payReturn(10L,"E_WALLET",false)).isInstanceOf(IllegalStateException.class);verifyNoInteractions(f.wallet);
        assertThat(f.money).doesNotContainKey("FORWARD_FEE_ON_RETURN");
    }
}
