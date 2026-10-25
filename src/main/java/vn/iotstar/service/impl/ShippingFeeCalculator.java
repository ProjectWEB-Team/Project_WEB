package vn.iotstar.service.impl;

import vn.iotstar.entity.PricingPolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** The same actual-weight tariff is used for quotes and physical intake. */
final class ShippingFeeCalculator {
    private ShippingFeeCalculator() { }

    static BigDecimal calculate(PricingPolicy policy, int weight, BigDecimal declaredValue, BigDecimal cod) {
        BigDecimal fee = policy.getBasePrice();
        int extra = weight - policy.getBaseWeightG();
        if (extra > 0) {
            if (policy.getExtraWeightStepG() <= 0) throw new IllegalStateException("Invalid weight pricing policy");
            long steps = ((long) extra + policy.getExtraWeightStepG() - 1) / policy.getExtraWeightStepG();
            fee = fee.add(policy.getExtraWeightPrice().multiply(BigDecimal.valueOf(steps)));
        }
        return fee.add(percent(declaredValue, policy.getInsuranceRatePercent()))
                .add(percent(cod, policy.getCodFeePercent())).setScale(0, RoundingMode.HALF_UP);
    }

    private static BigDecimal percent(BigDecimal amount, BigDecimal rate) {
        return amount == null || rate == null ? BigDecimal.ZERO : amount.multiply(rate).divide(BigDecimal.valueOf(100));
    }
}
