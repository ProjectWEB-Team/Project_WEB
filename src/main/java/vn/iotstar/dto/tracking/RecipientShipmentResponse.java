package vn.iotstar.dto.tracking;

import vn.iotstar.entity.ShipmentStatus;

import java.math.BigDecimal;
import java.util.List;

/** Recipient view: masked until unlocked with the full tracking number; never staff, audit or sender finance data. */
public record RecipientShipmentResponse(Long id, boolean unlocked, String trackingNumber, ShipmentStatus currentStatus,
                                        String senderName, String receiverName, String receiverAddress,
                                        BigDecimal amountToPay, List<TrackingEventResponse> events) {
    public Long getId() { return id; }
    public boolean isUnlocked() { return unlocked; }
    public String getTrackingNumber() { return trackingNumber; }
    public ShipmentStatus getCurrentStatus() { return currentStatus; }
    public String getSenderName() { return senderName; }
    public String getReceiverName() { return receiverName; }
    public String getReceiverAddress() { return receiverAddress; }
    public BigDecimal getAmountToPay() { return amountToPay; }
    public List<TrackingEventResponse> getEvents() { return events; }
}
