package vn.iotstar.dto.delivery;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

public class DeliveryCollectionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Shipment ID is required")
    private Long shipmentId;

    private BigDecimal codAmountCollected;

    private BigDecimal shippingFeeCollected;

    private String paymentMethod = "CASH";

    public DeliveryCollectionRequest() {
    }

    public DeliveryCollectionRequest(Long shipmentId, BigDecimal codAmountCollected, BigDecimal shippingFeeCollected, String paymentMethod) {
        this.shipmentId = shipmentId;
        this.codAmountCollected = codAmountCollected;
        this.shippingFeeCollected = shippingFeeCollected;
        this.paymentMethod = paymentMethod;
    }

    public Long getShipmentId() { return shipmentId; }
    public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }
    public BigDecimal getCodAmountCollected() { return codAmountCollected; }
    public void setCodAmountCollected(BigDecimal codAmountCollected) { this.codAmountCollected = codAmountCollected; }
    public BigDecimal getShippingFeeCollected() { return shippingFeeCollected; }
    public void setShippingFeeCollected(BigDecimal shippingFeeCollected) { this.shippingFeeCollected = shippingFeeCollected; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}

