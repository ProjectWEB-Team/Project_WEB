package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.iotstar.entity.ShipmentEvent;

import java.util.List;
import java.util.Optional;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, Long> {
    @org.springframework.data.jpa.repository.Query("select e from ShipmentEvent e where e.shipment.id = :shipmentId order by e.createdAt asc, e.id asc")
    List<ShipmentEvent> findByShipmentIdOrderByCreatedAtAsc(Long shipmentId);
    boolean existsByShipmentIdAndEventType(Long shipmentId, String eventType);

    Optional<ShipmentEvent> findFirstByShipmentIdAndEventTypeOrderByCreatedAtDesc(Long shipmentId, String eventType);
}
