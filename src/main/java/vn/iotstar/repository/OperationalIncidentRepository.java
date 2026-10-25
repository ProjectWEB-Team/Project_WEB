package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.iotstar.entity.Incident;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;
import java.util.Collection;

public interface OperationalIncidentRepository extends JpaRepository<Incident, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Incident i where i.id = :id")
    Optional<Incident> findByIdForUpdate(@Param("id") Long id);
    boolean existsByShipmentIdAndStatusInAndIdNot(Long shipmentId, Collection<String> statuses, Long id);
    List<Incident> findTop25ByTypeAndStatusAndHubIdInOrderByIdAsc(String type, String status, Collection<Long> hubIds);
}
