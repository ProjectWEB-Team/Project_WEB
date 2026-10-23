package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.iotstar.entity.RefundSettlementOutbox;

public interface RefundSettlementOutboxRepository extends JpaRepository<RefundSettlementOutbox, Long> {
    boolean existsByPaymentId(Long paymentId);
}
