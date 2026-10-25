package vn.iotstar.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.CodTransaction;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodTransactionRepository extends JpaRepository<CodTransaction, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ct FROM CodTransaction ct WHERE ct.id = :id")
    Optional<CodTransaction> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ct FROM CodTransaction ct WHERE ct.shipment.id = :shipmentId")
    Optional<CodTransaction> findByShipmentIdForUpdate(@Param("shipmentId") Long shipmentId);

    Optional<CodTransaction> findByShipmentId(Long shipmentId);

    Optional<CodTransaction> findByShipmentIdAndStatus(Long shipmentId, String status);

    List<CodTransaction> findByCollectedByShipperIdAndStatus(Long shipperId, String status);
}

