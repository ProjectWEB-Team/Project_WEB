package vn.iotstar.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.ShipmentAssignment;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentAssignmentRepository extends JpaRepository<ShipmentAssignment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sa FROM ShipmentAssignment sa WHERE sa.id = :id")
    Optional<ShipmentAssignment> findByIdForUpdate(@Param("id") Long id);

    Optional<ShipmentAssignment> findFirstByShipmentIdAndAssigneeIdAndStatus(Long shipmentId, Long assigneeId, String status);

    Optional<ShipmentAssignment> findFirstByShipmentIdAndStatus(Long shipmentId, String status);

    List<ShipmentAssignment> findByAssigneeIdAndStatus(Long assigneeId, String status);

    List<ShipmentAssignment> findByAssigneeIdAndStatusIn(Long assigneeId, List<String> statuses);

    List<ShipmentAssignment> findByShipmentIdOrderByAssignedAtDesc(Long shipmentId);
}

