package vn.iotstar.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Admin-managed price row (pricing_policies); quotes read only ACTIVE rows. */
@Entity
@Table(name = "pricing_policies")
public class PricingPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "service_code", nullable = false) private String serviceCode;
    @Column(name = "from_region", nullable = false) private String fromRegion;
    @Column(name = "to_region", nullable = false) private String toRegion;
    @Column(name = "base_weight_g", nullable = false) private int baseWeightG;
    @Column(name = "base_price", nullable = false) private BigDecimal basePrice;
    @Column(name = "extra_weight_step_g", nullable = false) private int extraWeightStepG;
    @Column(name = "extra_weight_price", nullable = false) private BigDecimal extraWeightPrice;
    @Column(name = "insurance_rate_percent") private BigDecimal insuranceRatePercent;
    @Column(name = "cod_fee_percent") private BigDecimal codFeePercent;
    @Column(nullable = false) private String status;

    protected PricingPolicy() { }

    public Long getId() { return id; }
    public int getBaseWeightG() { return baseWeightG; }
    public BigDecimal getBasePrice() { return basePrice; }
    public int getExtraWeightStepG() { return extraWeightStepG; }
    public BigDecimal getExtraWeightPrice() { return extraWeightPrice; }
    public BigDecimal getInsuranceRatePercent() { return insuranceRatePercent; }
    public BigDecimal getCodFeePercent() { return codFeePercent; }
    public String getServiceCode() { return serviceCode; }
    public String getFromRegion() { return fromRegion; }
    public String getToRegion() { return toRegion; }
    public String getStatus() { return status; }

    public void reprice(BigDecimal basePrice, BigDecimal extraWeightPrice, BigDecimal insuranceRatePercent,
                        BigDecimal codFeePercent, boolean active) {
        this.basePrice = basePrice; this.extraWeightPrice = extraWeightPrice;
        this.insuranceRatePercent = insuranceRatePercent; this.codFeePercent = codFeePercent;
        this.status = active ? "ACTIVE" : "INACTIVE";
    }
}
