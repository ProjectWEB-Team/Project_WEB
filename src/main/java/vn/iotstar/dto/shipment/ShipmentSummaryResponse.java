package vn.iotstar.dto.shipment;

import vn.iotstar.entity.ShipmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One row of the sender's own shipment list. */
public record ShipmentSummaryResponse(Long id, String trackingNumber, String receiverName, ShipmentStatus currentStatus,
                                      BigDecimal shippingFee, String paymentStatus, LocalDateTime createdAt) {
    public Long getId() { return id; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getReceiverName() { return receiverName; }
    public ShipmentStatus getCurrentStatus() { return currentStatus; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public String getPaymentStatus() { return paymentStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
