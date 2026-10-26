package vn.iotstar.service;

import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.entity.*;
import vn.iotstar.repository.*;
import vn.iotstar.realtime.NotificationEventPublisher;
import vn.iotstar.service.impl.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ShipmentFinanceServiceImplTest {
    PaymentRepository payments=mock(PaymentRepository.class);
    ShipmentRepository shipments=mock(ShipmentRepository.class);
    IncidentRepository incidents=mock(IncidentRepository.class);
    ShipmentEventRepository events=mock(ShipmentEventRepository.class);
    UserRepository users=mock(UserRepository.class);
    WalletLedgerService ledger=mock(WalletLedgerService.class);
    NotificationEventPublisher notifications=mock(NotificationEventPublisher.class);
    RefundSettlementOutboxRepository outbox=mock(RefundSettlementOutboxRepository.class);
    WalletTransactionRepository transactions=mock(WalletTransactionRepository.class);
    ManagerOperationsRepository managers=mock(ManagerOperationsRepository.class);
    EmployeeProfileRepository employees=mock(EmployeeProfileRepository.class);
    Clock clock=Clock.fixed(Instant.parse("2026-10-25T10:00:00Z"),ZoneId.of("Asia/Ho_Chi_Minh"));
    ShipmentFinanceServiceImpl service=new ShipmentFinanceServiceImpl(payments,shipments,incidents,events,users,ledger,notifications,outbox,transactions,new FinanceAccess(managers,employees),clock);
    User admin=new User(100L,"admin","hash",UserRole.ROLE_ADMIN,"ACTIVE");
    User sender=new User(1L,"sender","hash",UserRole.ROLE_USER,"ACTIVE");
    Shipment shipment;
    @BeforeEach void setup() {
        login(admin);
        shipment=Shipment.create("TRACK",sender,"S","01","A","R","02","B"); shipment.setId(1L);
        shipment.setDeclaredValue(new BigDecimal("500000")); shipment.setShippingFee(new BigDecimal("35000"));
        when(shipments.findByIdForUpdate(1L)).thenReturn(Optional.of(shipment));
        when(users.getReferenceById(100L)).thenReturn(admin);
    }
    @AfterEach void clear() {SecurityContextHolder.clearContext();}
    void login(User user) {SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities()));}
    Payment paid(String type) {
        Payment p=Payment.create(shipment,"SENDER",type,new BigDecimal("35000"),"CASH","PENDING");p.setId(50L);p.collect("CASH",10L,"cash");return p;
    }
    RefundSettlementOutbox refund(Payment p) {
        shipment.transitionTo(ShipmentStatus.CANCELLED); shipment.setCancelReason("USER_CANCELLED");
        var o=RefundSettlementOutbox.pending(shipment,p,shipment.getCancelReason(),LocalDateTime.now(clock));
        when(payments.findById(50L)).thenReturn(Optional.of(p)); when(payments.findByIdForUpdate(50L)).thenReturn(Optional.of(p));
        when(outbox.findByPaymentIdForUpdate(50L)).thenReturn(Optional.of(o)); return o;
    }
    Incident loss() {
        shipment.transitionTo(ShipmentStatus.IN_TRANSIT);shipment.setOnHold(true);
        var i=Incident.create(1L,7L,5L,"MISSING_PARCEL","missing",100L);i.setId(9L);i.setStatus("INVESTIGATING");i.recordInvestigation("CONFIRMED_LOST","Search exhausted",100L);
        when(incidents.findByIdForUpdate(9L)).thenReturn(Optional.of(i));when(incidents.countMissingCustody(1L,7L,5L)).thenReturn(1L);return i;
    }
    @Test void refundSettlesObligationAndNotifiesOwner() {
        var p=paid("SHIPPING_FEE");var o=refund(p); service.refundShipmentPayment(50L,"cancelled");
        assertThat(p.getStatus()).isEqualTo("REFUNDED");assertThat(o.getStatus()).isEqualTo("SETTLED");assertThat(o.getSettledAt()).isEqualTo(LocalDateTime.now(clock));
        verify(ledger).creditRefund(eq(1L),eq(new BigDecimal("35000")),eq(50L),eq(1L),anyString());
        verify(notifications).publishToUserAfterCommit(eq("sender"),any());verify(notifications,never()).publishAfterCommit(any(),any());
    }
    @Test void schedulerUsesSameSettlementWithoutAnActor() {
        var p=paid("WEIGHT_SURCHARGE");var o=refund(p);SecurityContextHolder.clearContext();service.settlePendingRefund(50L);assertThat(o.getStatus()).isEqualTo("SETTLED");
        verify(ledger).creditRefund(eq(1L),any(),eq(50L),eq(1L),anyString());
    }
    @Test void staleObligationNeedsExactLedgerProof() {
        var p=paid("SHIPPING_FEE");var o=refund(p);p.setStatus("REFUNDED");
        assertThatThrownBy(()->service.refundShipmentPayment(50L,"retry")).isInstanceOf(IllegalStateException.class);
        assertThat(o.getStatus()).isEqualTo("PENDING_WALLET_CREDIT");
        when(transactions.findFirstByTransactionTypeAndReferenceTypeAndReferenceId("REFUND","PAYMENT",50L)).thenReturn(Optional.of(WalletTransaction.create(1L,"REFUND",p.getAmount(),BigDecimal.ZERO,p.getAmount(),50L,"PAYMENT","refund")));
        service.refundShipmentPayment(50L,"retry");assertThat(o.getStatus()).isEqualTo("SETTLED");verifyNoInteractions(ledger,events,notifications);
    }
    @Test void tamperedObligationFailsBeforeWallet() {
        var p=paid("SHIPPING_FEE");var o=refund(p);ReflectionTestUtils.setField(o,"senderUserId",99L);
        assertThatThrownBy(()->service.refundShipmentPayment(50L,"cancel")).isInstanceOf(IllegalStateException.class);verifyNoInteractions(ledger);
    }
    @Test void walletFailureDoesNotMarkSettledOrPublish() {
        var p=paid("SHIPPING_FEE");var o=refund(p);doThrow(new IllegalStateException("wallet unavailable")).when(ledger).creditRefund(any(),any(),any(),any(),any());
        assertThatThrownBy(()->service.refundShipmentPayment(50L,"cancel")).isInstanceOf(IllegalStateException.class);
        assertThat(p.getStatus()).isEqualTo("PAID");assertThat(o.getStatus()).isEqualTo("PENDING_WALLET_CREDIT");verifyNoInteractions(events,notifications);
    }
    @Test void roleAndUnassignedManagerAreDeniedEvenOnReplay() {
        login(sender);assertThatThrownBy(()->service.refundShipmentPayment(50L,"x")).isInstanceOf(AccessDeniedException.class);
        var p=paid("SHIPPING_FEE");refund(p);p.setStatus("REFUNDED");
        User manager=new User(2L,"manager","hash",UserRole.ROLE_MANAGER,"ACTIVE");Hub hub=Hub.active("H","Hub",1,"HUB","R","P");ReflectionTestUtils.setField(hub,"id",5L);
        when(managers.findActiveProfileByUserId(2L)).thenReturn(Optional.of(EmployeeProfile.active(manager,hub,"MANAGER")));when(managers.findHubSubtreeIds(5L)).thenReturn(List.of(5L));login(manager);
        assertThatThrownBy(()->service.refundShipmentPayment(50L,"retry")).isInstanceOf(AccessDeniedException.class);verifyNoInteractions(ledger,transactions);
    }
    @Test void compensationUsesDeclaredAndActualPaidFeesWithSeparateReferences() {
        var i=loss();var p=paid("SHIPPING_FEE");when(payments.findByShipmentIdForUpdate(1L)).thenReturn(List.of(p));
        service.compensateLostShipment(1L,9L,"Confirmed loss");
        verify(ledger).creditCompensation(eq(1L),eq(new BigDecimal("500000")),eq(9L),anyString());verify(ledger).creditRefund(eq(1L),eq(p.getAmount()),eq(50L),eq(1L),anyString());
        assertThat(shipment.getCurrentStatus()).isEqualTo(ShipmentStatus.CLOSED_EXCEPTION);assertThat(i.getCompensatedAt()).isNotNull();assertThat(p.getStatus()).isEqualTo("REFUNDED");
        verify(notifications).publishToUserAfterCommit(eq("sender"),any());
    }
    @Test void quotedUnpaidAndPreviouslyRefundedFeesDoNotInflateCompensation() {
        loss();var unpaid=paid("SHIPPING_FEE");unpaid.setStatus("PENDING");var refunded=paid("WEIGHT_SURCHARGE");refunded.setStatus("REFUNDED");
        when(payments.findByShipmentIdForUpdate(1L)).thenReturn(List.of(unpaid,refunded));service.compensateLostShipment(1L,9L,"confirmed");
        verify(ledger).creditCompensation(eq(1L),eq(new BigDecimal("500000")),eq(9L),anyString());verify(ledger,never()).creditRefund(any(),any(),any(),any(),any());
    }
    @Test void openOrUnrelatedIncidentCannotAuthorizeCompensation() {
        var i=loss();ReflectionTestUtils.setField(i,"shipmentId",2L);
        assertThatThrownBy(()->service.compensateLostShipment(1L,9L,"confirmed")).isInstanceOf(IllegalStateException.class);
        ReflectionTestUtils.setField(i,"shipmentId",1L);ReflectionTestUtils.setField(i,"resolutionOutcome",null);
        assertThatThrownBy(()->service.compensateLostShipment(1L,9L,"confirmed")).isInstanceOf(IllegalStateException.class);verifyNoInteractions(ledger);
    }
    @Test void compensationReplayRequiresExactResolvedIncident() {
        var i=loss();service.compensateLostShipment(1L,9L,"confirmed");
        when(transactions.findFirstByTransactionTypeAndReferenceTypeAndReferenceId("COMPENSATION","INCIDENT",9L)).thenReturn(Optional.of(WalletTransaction.create(1L,"COMPENSATION",new BigDecimal("500000"),BigDecimal.ZERO,new BigDecimal("500000"),9L,"INCIDENT","loss")));
        service.compensateLostShipment(1L,9L,"retry");verify(ledger,times(1)).creditCompensation(any(),any(),any(),any());
        ReflectionTestUtils.setField(i,"compensatedAt",null);assertThatThrownBy(()->service.compensateLostShipment(1L,9L,"wrong case")).isInstanceOf(IllegalStateException.class);
    }
    @Test void pendingListUsesPersistedObligations() {
        var p=paid("SHIPPING_FEE");var o=refund(p);when(outbox.findTop100ByStatusOrderByIdAsc("PENDING_WALLET_CREDIT")).thenReturn(List.of(o));
        assertThat(service.listPendingRefundCandidates()).singleElement().satisfies(c->assertThat(c.paymentId()).isEqualTo(50L));
    }
    @Test void zeroPayableLossStillClosesWithoutInventedCredit() {
        var incident=loss();shipment.setDeclaredValue(BigDecimal.ZERO);shipment.setFeePayer("RECEIVER");
        service.compensateLostShipment(1L,9L,"confirmed");service.compensateLostShipment(1L,9L,"retry");
        assertThat(shipment.getClosureReason()).isEqualTo("LOST");assertThat(incident.getStatus()).isEqualTo("RESOLVED");verifyNoInteractions(ledger);
    }
    @Test void longestAcceptedReasonFitsPersistedEvent() {
        loss();String reason="x".repeat(255);service.compensateLostShipment(1L,9L,reason);
        verify(events).save(argThat(e->e.getDescription().equals(reason) && e.getDescription().length()==255));
    }
}
