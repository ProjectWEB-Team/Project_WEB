package vn.iotstar.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.PricingPolicy;
import vn.iotstar.repository.PricingPolicyRepository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Read-only view of the active tariff for the public price page; quotes use the same calculator as intake. */
@Service
public class PublicPricingService {
    private final PricingPolicyRepository policies;

    public PublicPricingService(PricingPolicyRepository policies) { this.policies = policies; }

    @Transactional(readOnly = true)
    public List<PricingPolicy> active() {
        return policies.findAll().stream().filter(p -> "ACTIVE".equals(p.getStatus()))
                .sorted(Comparator.comparing(PricingPolicy::getServiceCode).thenComparing(PricingPolicy::getFromRegion)
                        .thenComparing(PricingPolicy::getToRegion)).toList();
    }

    @Transactional(readOnly = true)
    public Optional<BigDecimal> estimate(String service, String from, String to, int weightGrams, BigDecimal declared, BigDecimal cod) {
        return policies.findFirstByServiceCodeAndFromRegionAndToRegionAndStatusOrderByIdDesc(service, from, to, "ACTIVE")
                .map(p -> ShippingFeeCalculator.calculate(p, weightGrams, declared, cod));
    }
}
