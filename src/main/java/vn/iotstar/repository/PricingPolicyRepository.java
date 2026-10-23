package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.iotstar.entity.PricingPolicy;

import java.util.Optional;

public interface PricingPolicyRepository extends JpaRepository<PricingPolicy, Long> {
    Optional<PricingPolicy> findFirstByServiceCodeAndFromRegionAndToRegionAndStatusOrderByIdDesc(
            String serviceCode, String fromRegion, String toRegion, String status);
}
