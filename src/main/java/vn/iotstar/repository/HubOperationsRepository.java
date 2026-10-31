package vn.iotstar.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.HubRoutingRule;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentRoute;

import java.util.List;
import java.util.Optional;

/** Repository-only data access for hub-scoped operations; controllers never receive it. */
public interface HubOperationsRepository extends JpaRepository<ShipmentRoute, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shipment s where s.id = :shipmentId")
    Optional<Shipment> findShipmentByIdForUpdate(@Param("shipmentId") Long shipmentId);

    @Query("select s from Shipment s join fetch s.currentHub where s.id = :shipmentId")
    Optional<Shipment> findShipmentByIdWithHub(@Param("shipmentId") Long shipmentId);

    @Query("select p from EmployeeProfile p join fetch p.hub where p.user.id = :userId and p.status = 'ACTIVE'")
    Optional<EmployeeProfile> findActiveProfileByUserId(@Param("userId") Long userId);

    @Query(value = "WITH hub_scope AS (SELECT id FROM hubs WHERE id = :rootHubId " +
            "UNION ALL SELECT h.id FROM hubs h JOIN hub_scope hs ON h.parent_hub_id = hs.id) " +
            "SELECT CASE WHEN EXISTS (SELECT 1 FROM hub_scope WHERE id = :targetHubId) THEN 1 ELSE 0 END",
            nativeQuery = true)
    Integer isHubWithinSubtree(@Param("rootHubId") Long rootHubId, @Param("targetHubId") Long targetHubId);

    @Query("select r from HubRoutingRule r join fetch r.nextHub where r.currentHub.id = :currentHubId " +
            "and r.destinationRegion = :region and r.status = 'ACTIVE' " +
            "and (r.destinationProvince = :province or r.destinationProvince is null)")
    List<HubRoutingRule> findActiveRules(@Param("currentHubId") Long currentHubId,
                                         @Param("region") String region, @Param("province") String province);

    @Query("select s from Shipment s where s.onHold = false and ((s.currentHubId = :hubId and s.direction = 'FORWARD' "
            + "and s.currentStatus = vn.iotstar.entity.ShipmentStatus.AT_HUB) or (s.pickupMethod = 'SELF_DROPOFF' and s.originHubId = :hubId "
            + "and s.currentStatus = vn.iotstar.entity.ShipmentStatus.WAITING_FOR_HANDOVER)) order by s.id")
    List<Shipment> findWaitingAtHub(@Param("hubId") Long hubId, org.springframework.data.domain.Pageable page);

    @Query("select h from Hub h where h.id = :id and h.status = 'ACTIVE'")
    Optional<vn.iotstar.entity.Hub> findActiveHub(@Param("id") Long id);

    Optional<ShipmentRoute> findByRouteDecisionKey(String routeDecisionKey);

    default ShipmentRoute saveRoute(ShipmentRoute route) { return save(route); }
}
