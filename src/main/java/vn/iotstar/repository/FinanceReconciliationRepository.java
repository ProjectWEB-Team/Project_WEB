package vn.iotstar.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.ShipperReconciliation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinanceReconciliationRepository extends JpaRepository<ShipperReconciliation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sr FROM ShipperReconciliation sr WHERE sr.id = :id")
    Optional<ShipperReconciliation> findByIdForUpdate(@Param("id") Long id);

    List<ShipperReconciliation> findByShipperId(Long shipperId);
    List<ShipperReconciliation> findTop100ByShipperIdOrderByIdDesc(Long shipperId);

    List<ShipperReconciliation> findByManagerId(Long managerId);

    List<ShipperReconciliation> findByReconciliationDate(LocalDate date);
}

