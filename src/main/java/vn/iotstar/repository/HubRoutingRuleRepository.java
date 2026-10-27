package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.iotstar.entity.HubRoutingRule;

import java.util.List;

public interface HubRoutingRuleRepository extends JpaRepository<HubRoutingRule, Long> {
    @Query("select r from HubRoutingRule r join fetch r.currentHub join fetch r.nextHub order by r.currentHub.code, r.destinationRegion, r.destinationProvince")
    List<HubRoutingRule> findAllWithHubs();

    @Query("select count(r) from HubRoutingRule r where r.status = 'ACTIVE' and r.currentHub.id = :currentHubId"
            + " and r.destinationRegion = :region and coalesce(r.destinationProvince, '') = coalesce(:province, '')")
    long countActiveSameKey(@org.springframework.data.repository.query.Param("currentHubId") Long currentHubId,
                            @org.springframework.data.repository.query.Param("region") String region,
                            @org.springframework.data.repository.query.Param("province") String province);

    /** Everything that still depends on a hub; any non-zero entry blocks deactivating it. */
    @Query("""
            select (select count(h) from Hub h where h.parentHub.id = :hubId and h.status = 'ACTIVE'),
                   (select count(p) from EmployeeProfile p where p.hub.id = :hubId and p.status = 'ACTIVE'),
                   (select count(r) from HubRoutingRule r where r.status = 'ACTIVE' and (r.currentHub.id = :hubId or r.nextHub.id = :hubId)),
                   (select count(s) from Shipment s where s.currentHubId = :hubId and s.currentStatus not in
                        (vn.iotstar.entity.ShipmentStatus.DELIVERED, vn.iotstar.entity.ShipmentStatus.CANCELLED,
                         vn.iotstar.entity.ShipmentStatus.RETURNED_TO_SENDER, vn.iotstar.entity.ShipmentStatus.CLOSED_EXCEPTION)),
                   (select count(c) from ParcelContainer c where c.status in ('OPEN', 'SEALED', 'LOADED')
                        and (c.originHub.id = :hubId or c.destinationHub.id = :hubId)),
                   (select count(t) from TransportTrip t where t.status in ('SCHEDULED', 'IN_TRANSIT', 'ARRIVED')
                        and (t.originHub.id = :hubId or t.destinationHub.id = :hubId)),
                   (select count(o) from Handover o where o.status in ('PENDING_ACCEPTANCE', 'ACCEPTED', 'IN_TRANSIT')
                        and (o.fromHub.id = :hubId or o.toHub.id = :hubId))
            from Hub x where x.id = :hubId""")
    List<Object[]> hubDependencies(@org.springframework.data.repository.query.Param("hubId") Long hubId);
}
