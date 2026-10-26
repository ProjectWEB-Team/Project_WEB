package vn.iotstar.service.impl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.iotstar.repository.RefundSettlementOutboxRepository;
import vn.iotstar.service.ShipmentFinanceService;

/** Each obligation uses a separate transaction; a failed item stays pending for scoped recovery. */
@Component
public class RefundSettlementScheduler {
    private static final Logger LOG=LoggerFactory.getLogger(RefundSettlementScheduler.class);
    private final RefundSettlementOutboxRepository outbox;
    private final ShipmentFinanceService finance;
    public RefundSettlementScheduler(RefundSettlementOutboxRepository outbox, ShipmentFinanceService finance) {
        this.outbox=outbox; this.finance=finance;
    }
    @Scheduled(fixedDelayString="${uteexpress.finance.refund-scan-interval-ms:60000}")
    public void settlePending() {
        for (var obligation: outbox.findTop100ByStatusOrderByIdAsc("PENDING_WALLET_CREDIT")) {
            try { finance.settlePendingRefund(obligation.getPaymentId()); }
            catch (RuntimeException failure) { LOG.warn("Refund obligation {} remains pending; review required", obligation.getId()); }
        }
    }
}
