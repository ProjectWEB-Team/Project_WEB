package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.iotstar.entity.ShipmentEvent;

import java.util.List;
import java.util.Optional;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, Long> {
    List<ShipmentEvent> findByShipmentIdOrderByCreatedAtAsc(Long shipmentId);

    Optional<ShipmentEvent> findFirstByShipmentIdAndEventTypeOrderByCreatedAtDesc(Long shipmentId, String eventType);
}
