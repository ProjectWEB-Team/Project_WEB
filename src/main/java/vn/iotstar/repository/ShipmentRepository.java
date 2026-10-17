package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.iotstar.entity.Shipment;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
}
