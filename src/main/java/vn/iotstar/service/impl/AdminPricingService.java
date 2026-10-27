package vn.iotstar.service.impl;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.PricingPolicy;
import vn.iotstar.entity.UserRole;
import vn.iotstar.repository.PricingPolicyRepository;
import vn.iotstar.security.AuthenticatedActor;

import java.math.BigDecimal;
import java.util.List;

/** Workflow §23: the Administrator maintains the price table. A changed price makes open quotes re-quote. */
@Service
@Transactional
public class AdminPricingService {
    private static final BigDecimal MAX_PERCENT = new BigDecimal("100");
    private final PricingPolicyRepository policies;

    public AdminPricingService(PricingPolicyRepository policies) {
        this.policies = policies;
    }

    @Transactional(readOnly = true)
    public List<PricingPolicy> all() {
        requireAdmin();
        return policies.findAll(Sort.by("serviceCode", "fromRegion", "toRegion", "id"));
    }

    public void update(Long id, BigDecimal basePrice, BigDecimal extraWeightPrice, BigDecimal insurancePercent,
                       BigDecimal codPercent, boolean active) {
        requireAdmin();
        if (!nonNegative(basePrice) || !nonNegative(extraWeightPrice) || !percent(insurancePercent) || !percent(codPercent))
            throw new IllegalArgumentException("Giá phải từ 0 trở lên, tỷ lệ phần trăm từ 0 đến 100.");
        policies.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dòng bảng giá."))
                .reprice(basePrice, extraWeightPrice, insurancePercent, codPercent, active);
    }

    private static boolean nonNegative(BigDecimal value) { return value != null && value.signum() >= 0; }
    private static boolean percent(BigDecimal value) { return nonNegative(value) && value.compareTo(MAX_PERCENT) <= 0; }

    private static void requireAdmin() {
        if (!AuthenticatedActor.requireCurrent().hasRole(UserRole.ROLE_ADMIN)) throw new AccessDeniedException("ROLE_ADMIN required");
    }
}
