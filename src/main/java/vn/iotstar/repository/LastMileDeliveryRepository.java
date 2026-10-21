package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.DeliveryAttempt;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LastMileDeliveryRepository extends JpaRepository<DeliveryAttempt, Long> {

    List<DeliveryAttempt> findByShipmentIdOrderByAttemptNumberAsc(Long shipmentId);

    Optional<DeliveryAttempt> findTopByShipmentIdOrderByAttemptNumberDesc(Long shipmentId);

    long countByShipmentId(Long shipmentId);

    @Query("SELECT COUNT(da) FROM DeliveryAttempt da WHERE da.shipment.id = :shipmentId AND da.attemptDate = :attemptDate")
    long countByShipmentIdAndAttemptDate(@Param("shipmentId") Long shipmentId, @Param("attemptDate") LocalDate attemptDate);
}

