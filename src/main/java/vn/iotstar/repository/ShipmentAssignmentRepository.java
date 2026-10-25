package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.iotstar.entity.ShipmentAssignment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShipmentAssignmentRepository extends JpaRepository<ShipmentAssignment, Long> {
    Optional<ShipmentAssignment> findFirstByShipmentIdAndAssignmentTypeAndStatus(Long shipmentId, String type, String status);
    List<ShipmentAssignment> findByAssigneeIdAndAssignmentTypeAndStatusOrderByAssignedAtAsc(Long assigneeId, String type, String status);

    /** Active pickups whose 48-hour window from assigned_at has elapsed (boundary inclusive). */
    @Query("select a.shipment.id from ShipmentAssignment a where a.assignmentType = 'PICKUP' and a.status = 'ASSIGNED'"
            + " and a.assignedAt <= :cutoff")
    List<Long> findExpiredPickupShipmentIds(@Param("cutoff") LocalDateTime cutoff);
}
