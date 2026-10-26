package vn.iotstar.dto.finance;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * One money item of a shipper shift that the manager reconciles against the
 * cash actually handed in. The expected amount is always derived from the
 * system record, never from client input.
 */
public class ShiftExpectedItemResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long shipmentId;
    private String trackingNumber;
    private String sourceType;
    private Long sourceId;
    private BigDecimal expectedAmount;
    private String sourceStatus;
    private String description;

    public ShiftExpectedItemResponse() {
    }

    public ShiftExpectedItemResponse(Long shipmentId, String trackingNumber, String sourceType, Long sourceId,
                                      BigDecimal expectedAmount, String sourceStatus, String description) {
        this.shipmentId = shipmentId;
        this.trackingNumber = trackingNumber;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.expectedAmount = expectedAmount;
        this.sourceStatus = sourceStatus;
        this.description = description;
    }

    public Long getShipmentId() { return shipmentId; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getSourceType() { return sourceType; }
    public Long getSourceId() { return sourceId; }
    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public String getSourceStatus() { return sourceStatus; }
    public String getDescription() { return description; }
}