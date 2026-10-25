package vn.iotstar.dto.returnflow;

import java.io.Serializable;
import java.math.BigDecimal;

public class ReturnTaskResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long shipmentId;
    private String trackingNumber;
    private String currentStatus;
    private String direction;
    private Long originHubId;
    private String senderName;
    private String senderPhone;
    private BigDecimal returnFee;
    private BigDecimal outstandingFee;
    private boolean feeSettled;
    private boolean canCollect;

    public ReturnTaskResponse() {
    }

    public ReturnTaskResponse(Long shipmentId, String trackingNumber, String currentStatus,
                              String direction, Long originHubId, String senderName,
                              String senderPhone, BigDecimal returnFee, BigDecimal outstandingFee,
                              boolean feeSettled, boolean canCollect) {
        this.shipmentId = shipmentId;
        this.trackingNumber = trackingNumber;
        this.currentStatus = currentStatus;
        this.direction = direction;
        this.originHubId = originHubId;
        this.senderName = senderName;
        this.senderPhone = senderPhone;
        this.returnFee = returnFee;
        this.outstandingFee = outstandingFee;
        this.feeSettled = feeSettled;
        this.canCollect = canCollect;
    }

    public Long getShipmentId() { return shipmentId; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getCurrentStatus() { return currentStatus; }
    public String getDirection() { return direction; }
    public Long getOriginHubId() { return originHubId; }
    public String getSenderName() { return senderName; }
    public String getSenderPhone() { return senderPhone; }
    public BigDecimal getReturnFee() { return returnFee; }
    public BigDecimal getOutstandingFee() { return outstandingFee; }
    public boolean isFeeSettled() { return feeSettled; }
    public boolean isCanCollect() { return canCollect; }
}

