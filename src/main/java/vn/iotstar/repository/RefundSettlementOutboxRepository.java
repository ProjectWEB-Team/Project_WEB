package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import vn.iotstar.entity.RefundSettlementOutbox;

public interface RefundSettlementOutboxRepository extends JpaRepository<RefundSettlementOutbox, Long> {
    boolean existsByPaymentId(Long paymentId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from RefundSettlementOutbox o where o.paymentId = :paymentId")
    Optional<RefundSettlementOutbox> findByPaymentIdForUpdate(@Param("paymentId") Long paymentId);
    List<RefundSettlementOutbox> findTop100ByStatusOrderByIdAsc(String status);
}
