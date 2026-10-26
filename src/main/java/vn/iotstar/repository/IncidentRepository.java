package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.Incident;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from Incident i where i.id = :id")
    java.util.Optional<Incident> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    List<Incident> findTop100ByTypeAndStatusAndResolutionOutcomeOrderByIdAsc(String type, String status, String resolutionOutcome);
    @org.springframework.data.jpa.repository.Query("select count(ci) from ContainerItem ci join HandoverContainer hc on hc.container = ci.container " +
            "where ci.shipment.id = :shipmentId and ci.status = 'MISSING' and ci.foundAt is null " +
            "and hc.handover.transportTrip.id = :tripId and hc.handover.toHub.id = :hubId")
    long countMissingCustody(@org.springframework.data.repository.query.Param("shipmentId") Long shipmentId,
            @org.springframework.data.repository.query.Param("tripId") Long tripId,
            @org.springframework.data.repository.query.Param("hubId") Long hubId);

    List<Incident> findByShipmentId(Long shipmentId);

    List<Incident> findByHubId(Long hubId);

    List<Incident> findByStatus(String status);

    List<Incident> findByTypeAndStatus(String type, String status);
}
