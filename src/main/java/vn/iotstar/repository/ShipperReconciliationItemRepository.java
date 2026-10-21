package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.ShipperReconciliationItem;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipperReconciliationItemRepository extends JpaRepository<ShipperReconciliationItem, Long> {

    List<ShipperReconciliationItem> findByReconciliationId(Long reconciliationId);

    Optional<ShipperReconciliationItem> findBySourceTypeAndSourceId(String sourceType, Long sourceId);
}

