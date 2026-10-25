package vn.iotstar.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.ShipmentAssignment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentAssignmentRepository extends JpaRepository<ShipmentAssignment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sa FROM ShipmentAssignment sa WHERE sa.id = :id")
    Optional<ShipmentAssignment> findByIdForUpdate(@Param("id") Long id);
    Optional<ShipmentAssignment> findFirstByShipmentIdAndAssignmentTypeAndStatus(Long shipmentId, String type, String status);
    Optional<ShipmentAssignment> findFirstByShipmentIdAndAssigneeIdAndStatus(Long shipmentId, Long assigneeId, String status);
    Optional<ShipmentAssignment> findFirstByShipmentIdAndStatus(Long shipmentId, String status);
    List<ShipmentAssignment> findByAssigneeIdAndAssignmentTypeAndStatusOrderByAssignedAtAsc(Long assigneeId, String type, String status);
    List<ShipmentAssignment> findByAssigneeIdAndStatus(Long assigneeId, String status);
    List<ShipmentAssignment> findByAssigneeIdAndStatusIn(Long assigneeId, List<String> statuses);
    List<ShipmentAssignment> findByShipmentIdOrderByAssignedAtDesc(Long shipmentId);
    boolean existsByShipmentIdAndStatusIn(Long shipmentId, List<String> statuses);
    @Query("select a.shipment.id from ShipmentAssignment a where a.assignmentType = 'PICKUP' and a.status = 'ASSIGNED' and a.assignedAt <= :cutoff")
    List<Long> findExpiredPickupShipmentIds(@Param("cutoff") LocalDateTime cutoff);
}
