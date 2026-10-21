package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.Incident;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByShipmentId(Long shipmentId);

    List<Incident> findByHubId(Long hubId);

    List<Incident> findByStatus(String status);

    List<Incident> findByTypeAndStatus(String type, String status);
}