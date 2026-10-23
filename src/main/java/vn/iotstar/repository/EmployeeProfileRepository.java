package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.UserRole;

import java.util.List;
import java.util.Optional;

public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long> {
    Optional<EmployeeProfile> findFirstByUserId(Long userId);
    List<EmployeeProfile> findByHubIdAndStatusAndUserRole(Long hubId, String status, UserRole role);
}
