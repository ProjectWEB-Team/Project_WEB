package vn.iotstar.dto.shipment;

import vn.iotstar.entity.ShipmentStatus;

import java.time.LocalDateTime;
import java.util.List;

/** Safe shipment detail/tracking read model; it deliberately excludes owner credentials. */
public record ShipmentDetailResponse(Long id, String trackingNumber, ShipmentStatus currentStatus,
                                     String senderName, String senderPhone, String senderAddress,
                                     String receiverName, String receiverPhone, String receiverAddress,
                                     LocalDateTime createdAt, List<ShipmentEventResponse> events) {
    public Long getId() { return id; }
    public String getTrackingNumber() { return trackingNumber; }
    public ShipmentStatus getCurrentStatus() { return currentStatus; }
    public String getSenderName() { return senderName; }
    public String getSenderPhone() { return senderPhone; }
    public String getSenderAddress() { return senderAddress; }
    public String getReceiverName() { return receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public String getReceiverAddress() { return receiverAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<ShipmentEventResponse> getEvents() { return events; }
}
