package vn.iotstar.dto.firstmile;

import java.time.LocalDateTime;

/** Coordinator view of an unassigned HOME_PICKUP shipment. */
public record PendingPickupResponse(Long shipmentId, String trackingNumber, String senderName, String senderPhone,
                                    String senderAddress, LocalDateTime waitingSince) {
    public Long getShipmentId() { return shipmentId; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getSenderName() { return senderName; }
    public String getSenderPhone() { return senderPhone; }
    public String getSenderAddress() { return senderAddress; }
    public LocalDateTime getWaitingSince() { return waitingSince; }
}
