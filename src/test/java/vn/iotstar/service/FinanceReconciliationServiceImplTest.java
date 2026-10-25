package vn.iotstar.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.dto.finance.ReconciliationSubmissionRequest;
import vn.iotstar.entity.*;
import vn.iotstar.repository.*;
import vn.iotstar.realtime.NotificationEventPublisher;
import vn.iotstar.service.impl.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class FinanceReconciliationServiceImplTest {
    FinanceReconciliationRepository recs=mock(FinanceReconciliationRepository.class);
    ShipperReconciliationItemRepository items=mock(ShipperReconciliationItemRepository.class);
    CodTransactionRepository cods=mock(CodTransactionRepository.class);
    PaymentRepository payments=mock(PaymentRepository.class);
    WalletRepository wallets=mock(WalletRepository.class);
    WalletTransactionRepository transactions=mock(WalletTransactionRepository.class);
    IncidentRepository incidents=mock(IncidentRepository.class);
    ShipmentRepository shipments=mock(ShipmentRepository.class);
    UserRepository users=mock(UserRepository.class);
    ShipmentEventRepository events=mock(ShipmentEventRepository.class);
    NotificationEventPublisher notifications=mock(NotificationEventPublisher.class);
    ManagerOperationsRepository managers=mock(ManagerOperationsRepository.class);
    EmployeeProfileRepository employees=mock(EmployeeProfileRepository.class);
    Clock clock=Clock.systemDefaultZone();
    FinanceReconciliationServiceImpl service=new FinanceReconciliationServiceImpl(recs,items,cods,payments,wallets,transactions,incidents,shipments,users,events,notifications,new FinanceAccess(managers,employees),clock);
    User admin=new User(100L,"admin","hash",UserRole.ROLE_ADMIN,"ACTIVE"), shipper=new User(10L,"shipper","hash",UserRole.ROLE_SHIPPER,"ACTIVE"), sender=new User(1L,"sender","hash",UserRole.ROLE_USER,"ACTIVE");
    Shipment shipment; Hub hub; CodTransaction cod;
    List<ShipperReconciliationItem> saved=new ArrayList<>();
    @BeforeEach void setup() {
        login(admin);hub=Hub.active("H","Hub",1,"HUB","R","P");ReflectionTestUtils.setField(hub,"id",5L);
        when(employees.findFirstByUserId(10L)).thenReturn(Optional.of(EmployeeProfile.active(shipper,hub,"SHIPPER")));
        when(users.getReferenceById(100L)).thenReturn(admin);
        shipment=Shipment.create("TRACK",sender,"S","01","A","R","02","B");shipment.setId(1L);shipment.setCodAmount(new BigDecimal("500000"));shipment.transitionTo(ShipmentStatus.DELIVERED);
        when(shipments.findByIdForUpdate(1L)).thenReturn(Optional.of(shipment));
        cod=CodTransaction.createCollected(shipment,shipper,new BigDecimal("500000"),"CASH");ReflectionTestUtils.setField(cod,"id",50L);
        when(cods.findByIdForUpdate(50L)).thenReturn(Optional.of(cod));when(cods.findById(50L)).thenReturn(Optional.of(cod));
        when(recs.save(any())).thenAnswer(a->{ShipperReconciliation r=a.getArgument(0);ReflectionTestUtils.setField(r,"id",7L);return r;});
        when(items.save(any())).thenAnswer(a->{ShipperReconciliationItem i=a.getArgument(0);saved.add(i);return i;});
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    void login(User user){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities()));}
    ReconciliationSubmissionRequest.ItemRequest row(String type,long id,BigDecimal expected,BigDecimal actual){return new ReconciliationSubmissionRequest.ItemRequest(1L,type,id,expected,actual,"note");}
    ReconciliationSubmissionRequest request(ReconciliationSubmissionRequest.ItemRequest... rows){return new ReconciliationSubmissionRequest(10L,LocalDate.now(clock),List.of(rows));}
    ReconciliationSubmissionRequest.ItemRequest codRow(){return row(FinanceReconciliationServiceImpl.COD,50L,null,new BigDecimal("500000"));}
    Payment payment(String payer,String type,long id,BigDecimal amount) {
        var p=Payment.create(shipment,payer,type,amount,"CASH","PENDING");p.setId(id);p.collect("CASH",10L,"collection");when(payments.findByIdForUpdate(id)).thenReturn(Optional.of(p));return p;
    }
    @Test void matchedCodRequiresSeparatePayoutAndReplayProvesLedger() {
        var rec=service.reconcile(request(codRow()));assertThat(rec.getStatus()).isEqualTo("COMPLETED");assertThat(cod.getStatus()).isEqualTo("READY_FOR_PAYOUT");verifyNoInteractions(wallets,transactions);
        when(items.findBySourceTypeAndSourceId(FinanceReconciliationServiceImpl.COD,50L)).thenReturn(Optional.of(saved.getFirst()));
        Wallet wallet=Wallet.createForUser(1L);when(wallets.findByUserIdForUpdate(1L)).thenReturn(Optional.of(wallet));
        service.payoutReadyCod(50L);assertThat(wallet.getBalance()).isEqualByComparingTo("500000");assertThat(cod.getStatus()).isEqualTo("PAID_TO_SENDER");
        verify(notifications).publishToUserAfterCommit(eq("sender"),any());
        when(transactions.findFirstByTransactionTypeAndReferenceTypeAndReferenceId("COD_PAYOUT","SHIPMENT",1L)).thenReturn(Optional.of(WalletTransaction.create(1L,"COD_PAYOUT",new BigDecimal("500000"),BigDecimal.ZERO,new BigDecimal("500000"),1L,"SHIPMENT","payout")));
        service.payoutReadyCod(50L);verify(transactions,times(1)).save(any());assertThat(wallet.getBalance()).isEqualByComparingTo("500000");
    }
    @Test void receiverFeeReconcilesWithoutWalletCredit() {
        payment("RECEIVER","SHIPPING_FEE",60L,new BigDecimal("35000"));service.reconcile(request(row(FinanceReconciliationServiceImpl.RECEIVER_FEE,60L,null,new BigDecimal("35000"))));
        assertThat(saved.getFirst().getExpectedAmount()).isEqualByComparingTo("35000");verifyNoInteractions(wallets,transactions,notifications);
    }
    @Test void senderPickupAndReturnFeeAreIncludedAsCashAccountability() {
        payment("SENDER","SHIPPING_FEE",60L,BigDecimal.TEN);payment("SENDER","RETURN_FEE",61L,BigDecimal.TEN);
        service.reconcile(request(row(FinanceReconciliationServiceImpl.OTHER_FEE,60L,null,BigDecimal.TEN),row(FinanceReconciliationServiceImpl.OTHER_FEE,61L,null,BigDecimal.TEN)));
        assertThat(saved).hasSize(2);verifyNoInteractions(wallets,transactions);
    }
    @Test void shortageAndOverageCannotNetAway() {
        payment("RECEIVER","SHIPPING_FEE",60L,new BigDecimal("35000"));
        var rec=service.reconcile(request(row(FinanceReconciliationServiceImpl.COD,50L,null,new BigDecimal("490000")),row(FinanceReconciliationServiceImpl.RECEIVER_FEE,60L,null,new BigDecimal("45000"))));
        assertThat(rec.getDifferenceAmount()).isEqualByComparingTo("0");assertThat(rec.getStatus()).isEqualTo("COMPLETED_WITH_DISCREPANCY");assertThat(cod.getStatus()).isEqualTo("HOLD_DISPUTE");
        assertThat(saved).extracting(ShipperReconciliationItem::getStatus).containsExactlyInAnyOrder("SHORTAGE","OVERAGE");verify(incidents,times(2)).save(any());verifyNoInteractions(wallets,transactions);
    }
    @ParameterizedTest @ValueSource(strings={"collector","method","date","amount","shipment","status","expected"})
    void forgedCodSourceRejectedBeforePersistence(String invalid) {
        var row=codRow();
        switch(invalid) {
            case "collector" -> ReflectionTestUtils.setField(cod,"collectedByShipper",sender);
            case "method" -> ReflectionTestUtils.setField(cod,"method","VNPAY");
            case "date" -> ReflectionTestUtils.setField(cod,"collectedAt",LocalDateTime.now().minusDays(1));
            case "amount" -> ReflectionTestUtils.setField(cod,"amount",BigDecimal.ONE);
            case "shipment" -> {Shipment wrong=Shipment.create("OTHER",sender,"S","01","A","R","02","B");wrong.setId(2L);wrong.setCodAmount(new BigDecimal("500000"));wrong.transitionTo(ShipmentStatus.DELIVERED);ReflectionTestUtils.setField(cod,"shipment",wrong);}
            case "status" -> cod.setStatus("WAITING_COLLECTION");
            case "expected" -> row=row(FinanceReconciliationServiceImpl.COD,50L,BigDecimal.ONE,new BigDecimal("500000"));
        }
        var request=request(row);assertThatThrownBy(()->service.reconcile(request)).isInstanceOf(IllegalArgumentException.class);verify(recs,never()).save(any());verify(items,never()).save(any());verifyNoInteractions(wallets,transactions,events,notifications);
    }
    @ParameterizedTest @ValueSource(strings={"collector","method","date","payer","type","status","alias"})
    void ineligiblePaymentRejectedBeforePersistence(String invalid) {
        var p=payment("RECEIVER","SHIPPING_FEE",60L,BigDecimal.TEN);String type=FinanceReconciliationServiceImpl.RECEIVER_FEE;
        switch(invalid) {
            case "collector" -> ReflectionTestUtils.setField(p,"collectedByUserId",99L);
            case "method" -> ReflectionTestUtils.setField(p,"method","VNPAY");
            case "date" -> ReflectionTestUtils.setField(p,"paidAt",LocalDateTime.now().minusDays(1));
            case "payer" -> ReflectionTestUtils.setField(p,"payerType","OTHER");
            case "type" -> ReflectionTestUtils.setField(p,"paymentType","OTHER");
            case "status" -> p.setStatus("REFUNDED");
            case "alias" -> type=FinanceReconciliationServiceImpl.OTHER_FEE;
        }
        var request=request(row(type,60L,null,BigDecimal.TEN));assertThatThrownBy(()->service.reconcile(request)).isInstanceOf(IllegalArgumentException.class);verify(recs,never()).save(any());
    }
    @Test void duplicateAndInvalidRowsAreRejectedBeforeReadingSources() {
        assertThatThrownBy(()->service.reconcile(request(codRow(),codRow()))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.reconcile(request(row(null,50L,null,BigDecimal.TEN)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.reconcile(request(row("OTHER",50L,null,BigDecimal.TEN)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.reconcile(request(row(FinanceReconciliationServiceImpl.COD,50L,null,BigDecimal.ONE.negate())))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.reconcile(request(row(FinanceReconciliationServiceImpl.COD,50L,null,null)))).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(cods);verify(recs,never()).save(any());
    }
    @Test void previouslyReconciledSourceCannotBeReused() {
        when(items.findBySourceTypeAndSourceId(FinanceReconciliationServiceImpl.COD,50L)).thenReturn(Optional.of(mock(ShipperReconciliationItem.class)));
        assertThatThrownBy(()->service.reconcile(request(codRow()))).isInstanceOf(IllegalStateException.class);verify(recs,never()).save(any());
    }
    @Test void managerScopeAppliesBeforeListDetailAndReplay() {
        User manager=new User(2L,"manager","hash",UserRole.ROLE_MANAGER,"ACTIVE");Hub other=Hub.active("O","Other",1,"HUB","R","P");ReflectionTestUtils.setField(other,"id",99L);
        when(managers.findActiveProfileByUserId(2L)).thenReturn(Optional.of(EmployeeProfile.active(manager,other,"MANAGER")));when(managers.findHubSubtreeIds(99L)).thenReturn(List.of(99L));
        var rec=service.reconcile(request(codRow()));when(recs.findById(7L)).thenReturn(Optional.of(rec));when(items.findBySourceTypeAndSourceId(FinanceReconciliationServiceImpl.COD,50L)).thenReturn(Optional.of(saved.getFirst()));cod.setStatus("PAID_TO_SENDER");login(manager);
        assertThatThrownBy(()->service.getShiftExpectedItems(10L,null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->service.getReconciliationItems(7L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->service.payoutReadyCod(50L)).isInstanceOf(AccessDeniedException.class);verifyNoInteractions(wallets,transactions);
    }
    @Test void userCannotEnterFinanceServices(){login(sender);assertThatThrownBy(()->service.reconcile(request(codRow()))).isInstanceOf(AccessDeniedException.class);}
    @Test void expectedItemsExcludeAlreadyReconciledAndNoncashSources() {
        when(cods.findByCollectedByShipperIdAndStatus(10L,"COLLECTED")).thenReturn(List.of(cod));
        when(items.findBySourceTypeAndSourceId(FinanceReconciliationServiceImpl.COD,50L)).thenReturn(Optional.of(mock(ShipperReconciliationItem.class)));
        assertThat(service.getShiftExpectedItems(10L,null)).isEmpty();
        ReflectionTestUtils.setField(cod,"method","VNPAY");assertThat(service.getShiftExpectedItems(10L,null)).isEmpty();
    }
    @ParameterizedTest @ValueSource(strings={"VNPAY","TRANSFER"})
    void electronicCodRequiresSeparateVerifiedSettlementBeforePayout(String method) {
        ReflectionTestUtils.setField(cod,"method",method);var request=request(codRow());request.setCollectionMethod(method);
        assertThatThrownBy(()->service.reconcile(request)).isInstanceOf(IllegalArgumentException.class);verify(recs,never()).save(any());assertThat(cod.getStatus()).isEqualTo("COLLECTED");
        request.setSettlementEvidence("Statement reference VERIFIED-101 reviewed by manager");var rec=service.reconcile(request);
        assertThat(rec.getCollectionMethod()).isEqualTo(method);assertThat(rec.getSettlementEvidence()).contains("VERIFIED-101");verifyNoInteractions(wallets,transactions);
        when(items.findBySourceTypeAndSourceId(FinanceReconciliationServiceImpl.COD,50L)).thenReturn(Optional.of(saved.getFirst()));Wallet wallet=Wallet.createForUser(1L);when(wallets.findByUserIdForUpdate(1L)).thenReturn(Optional.of(wallet));
        service.payoutReadyCod(50L);assertThat(wallet.getBalance()).isEqualByComparingTo("500000");
        when(transactions.findFirstByTransactionTypeAndReferenceTypeAndReferenceId("COD_PAYOUT","SHIPMENT",1L)).thenReturn(Optional.of(WalletTransaction.create(1L,"COD_PAYOUT",new BigDecimal("500000"),BigDecimal.ZERO,new BigDecimal("500000"),1L,"SHIPMENT","payout")));
        service.payoutReadyCod(50L);verify(transactions,times(1)).save(any());
        ReflectionTestUtils.setField(rec,"settlementEvidence",null);assertThatThrownBy(()->service.payoutReadyCod(50L)).isInstanceOf(IllegalStateException.class);
    }
    @Test void electronicAndCashSourcesCannotShareOneReconciliation() {
        var request=request(codRow());request.setCollectionMethod("VNPAY");request.setSettlementEvidence("statement");
        assertThatThrownBy(()->service.reconcile(request)).isInstanceOf(IllegalArgumentException.class);verify(recs,never()).save(any());
    }
}
