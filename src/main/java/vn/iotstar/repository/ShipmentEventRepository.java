package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.iotstar.entity.ShipmentEvent;

import java.util.List;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, Long> {
    List<ShipmentEvent> findByShipmentIdOrderByCreatedAtAsc(Long shipmentId);
}
