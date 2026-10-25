package vn.iotstar.dto.delivery;

import java.io.Serializable;
import java.math.BigDecimal;

public class DeliveryTaskResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long assignmentId;
    private Long shipmentId;
    private String trackingNumber;
    private String assignmentStatus;
    private String currentStatus;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private BigDecimal codAmount;
    private BigDecimal shippingFee;
    private String feePayer;
    private boolean codCollected;
    private boolean feeCollected;
    private boolean eligibleForDelivery;

    public DeliveryTaskResponse() {
    }

    public DeliveryTaskResponse(Long assignmentId, Long shipmentId, String trackingNumber,
                                String assignmentStatus, String currentStatus, String receiverName,
                                String receiverPhone, String receiverAddress, BigDecimal codAmount,
                                BigDecimal shippingFee, String feePayer, boolean codCollected,
                                boolean feeCollected, boolean eligibleForDelivery) {
        this.assignmentId = assignmentId;
        this.shipmentId = shipmentId;
        this.trackingNumber = trackingNumber;
        this.assignmentStatus = assignmentStatus;
        this.currentStatus = currentStatus;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.codAmount = codAmount;
        this.shippingFee = shippingFee;
        this.feePayer = feePayer;
        this.codCollected = codCollected;
        this.feeCollected = feeCollected;
        this.eligibleForDelivery = eligibleForDelivery;
    }

    public Long getAssignmentId() { return assignmentId; }
    public Long getShipmentId() { return shipmentId; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getAssignmentStatus() { return assignmentStatus; }
    public String getCurrentStatus() { return currentStatus; }
    public String getReceiverName() { return receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public String getReceiverAddress() { return receiverAddress; }
    public BigDecimal getCodAmount() { return codAmount; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public String getFeePayer() { return feePayer; }
    public boolean isCodCollected() { return codCollected; }
    public boolean isFeeCollected() { return feeCollected; }
    public boolean isEligibleForDelivery() { return eligibleForDelivery; }
}

