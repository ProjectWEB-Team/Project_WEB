package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.iotstar.entity.PickupAttempt;

public interface PickupAttemptRepository extends JpaRepository<PickupAttempt, Long> {
    int countByAssignmentId(Long assignmentId);
}
