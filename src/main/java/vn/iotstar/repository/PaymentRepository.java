package vn.iotstar.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.Payment;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);
    List<Payment> findByShipmentId(Long shipmentId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.shipment.id = :shipmentId order by p.id")
    List<Payment> findByShipmentIdForUpdate(@Param("shipmentId") Long shipmentId);

    @Query("select p from Payment p where p.collectedByUserId = :shipperId and p.status = 'PAID' and p.method = 'CASH' " +
            "and p.paidAt >= :start and p.paidAt < :end order by p.id")
    List<Payment> findCashShiftPayments(@Param("shipperId") Long shipperId,
            @Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
    List<Payment> findByStatusAndPaymentType(String status, String paymentType);
    List<Payment> findByStatusAndPaymentTypeAndPayerType(String status, String paymentType, String payerType);
    Optional<Payment> findFirstByShipmentIdAndPaymentType(Long shipmentId, String paymentType);
    Optional<Payment> findFirstByShipmentIdAndPaymentTypeAndStatus(Long shipmentId, String paymentType, String status);
    Optional<Payment> findFirstByShipmentIdAndPayerTypeAndPaymentType(Long shipmentId, String payerType, String paymentType);
}
