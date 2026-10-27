package vn.iotstar.service.impl;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.iotstar.entity.*;
import vn.iotstar.repository.*;
import vn.iotstar.realtime.*;
import vn.iotstar.service.WalletLedgerService;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

/** Real producer/consumer services with in-memory repository doubles; not SQL transaction evidence. */
class RefundProducerConsumerTest {
    @ParameterizedTest @ValueSource(booleans={false,true})
    void voluntaryAndExpiredCancellationSettleEveryPaidSourceExactlyOnce(boolean expired) {
        var shipments=mock(ShipmentRepository.class);var assignments=mock(ShipmentAssignmentRepository.class);
        var payments=mock(PaymentRepository.class);var outbox=mock(RefundSettlementOutboxRepository.class);var events=mock(ShipmentEventRepository.class);
        var ledger=mock(WalletLedgerService.class);var transactions=mock(WalletTransactionRepository.class);
        var notifications=mock(NotificationEventPublisher.class);var sender=new User(1L,"owner","hash",UserRole.ROLE_USER,"ACTIVE");
        var clock=Clock.fixed(Instant.parse("2026-10-25T10:00:00Z"),ZoneId.of("Asia/Ho_Chi_Minh"));
        var s=Shipment.create("REFUND-1",sender,"S","01","A","R","02","B");s.setId(1L);
        s.applyIntake(null,"HCM","Q1","SELF_DROPOFF","SENDER","NO_INSPECTION",500,1,1,1,BigDecimal.ZERO,BigDecimal.ZERO,new BigDecimal("25000"),"PAID",ShipmentStatus.WAITING_FOR_HANDOVER,LocalDateTime.now(clock).minusDays(3));
        var fee=Payment.create(s,"SENDER","SHIPPING_FEE",new BigDecimal("25000"),"E_WALLET","PENDING");fee.setId(2L);fee.collect("E_WALLET",1L,"fee");
        var extra=Payment.create(s,"SENDER","WEIGHT_SURCHARGE",new BigDecimal("5000"),"E_WALLET","PENDING");extra.setId(3L);extra.collect("E_WALLET",1L,"extra");
        when(shipments.findByIdForUpdate(1L)).thenReturn(Optional.of(s));when(payments.findByShipmentIdForUpdate(1L)).thenReturn(List.of(fee,extra));
        Map<Long,RefundSettlementOutbox> pending=new LinkedHashMap<>();Map<Long,WalletTransaction> credits=new HashMap<>();
        when(outbox.save(any())).thenAnswer(a->{RefundSettlementOutbox o=a.getArgument(0);pending.put(o.getPaymentId(),o);return o;});
        when(outbox.existsByPaymentId(anyLong())).thenAnswer(a->pending.containsKey(a.getArgument(0)));
        when(outbox.findByPaymentIdForUpdate(anyLong())).thenAnswer(a->Optional.ofNullable(pending.get(a.getArgument(0))));
        when(outbox.findTop100ByStatusOrderByIdAsc("PENDING_WALLET_CREDIT")).thenAnswer(a->pending.values().stream().filter(o->"PENDING_WALLET_CREDIT".equals(o.getStatus())).toList());
        for(var p:List.of(fee,extra)){when(payments.findById(p.getId())).thenReturn(Optional.of(p));when(payments.findByIdForUpdate(p.getId())).thenReturn(Optional.of(p));}
        when(ledger.creditRefund(anyLong(),any(),anyLong(),anyLong(),anyString())).thenAnswer(a->{Long id=a.getArgument(2);BigDecimal amount=a.getArgument(1);var tx=WalletTransaction.create(1L,"REFUND",amount,BigDecimal.ZERO,amount,id,"PAYMENT","refund");credits.put(id,tx);return tx;});
        when(transactions.findFirstByTransactionTypeAndReferenceTypeAndReferenceId(eq("REFUND"),eq("PAYMENT"),anyLong())).thenAnswer(a->Optional.ofNullable(credits.get(a.getArgument(2))));
        var cancellation=new ShipmentCancellation(assignments,payments,outbox,events,mock(ShipmentEventPublisher.class),clock);
        if(expired){when(shipments.findExpiredDropoffIds(eq(ShipmentStatus.WAITING_FOR_HANDOVER),any())).thenReturn(List.of(1L));assertThat(new IntakeTimeoutServiceImpl(shipments,assignments,cancellation,clock,48,0,48,0,24,0).cancelExpired()).isEqualTo(1);}
        else cancellation.cancel(s,"SENDER_REQUEST",sender);
        assertThat(pending).hasSize(2);
        var finance=new ShipmentFinanceServiceImpl(payments,shipments,mock(IncidentRepository.class),events,mock(UserRepository.class),ledger,notifications,outbox,transactions,mock(FinanceAccess.class),clock);
        SecurityContextHolder.clearContext();var scheduler=new RefundSettlementScheduler(outbox,finance);scheduler.settlePending();scheduler.settlePending();
        finance.settlePendingRefund(2L);finance.settlePendingRefund(3L);
        assertThat(pending.values()).allSatisfy(o->assertThat(o.getStatus()).isEqualTo("SETTLED"));assertThat(credits.values().stream().map(WalletTransaction::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add)).isEqualByComparingTo("30000");
        verify(ledger,times(2)).creditRefund(anyLong(),any(),anyLong(),anyLong(),anyString());verify(notifications,times(2)).publishToUserAfterCommit(eq("owner"),any());
    }
}
