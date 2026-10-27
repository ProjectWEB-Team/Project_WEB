package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.iotstar.entity.HubRoutingRule;

import java.util.List;

public interface HubRoutingRuleRepository extends JpaRepository<HubRoutingRule, Long> {
    @Query("select r from HubRoutingRule r join fetch r.currentHub join fetch r.nextHub order by r.currentHub.code, r.destinationRegion, r.destinationProvince")
    List<HubRoutingRule> findAllWithHubs();
}
