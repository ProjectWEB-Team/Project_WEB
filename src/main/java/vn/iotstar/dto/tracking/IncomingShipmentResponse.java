package vn.iotstar.dto.tracking;

import vn.iotstar.entity.ShipmentStatus;

import java.time.LocalDateTime;

/** Masked row of a linked recipient's incoming list. */
public record IncomingShipmentResponse(Long id, String maskedTrackingNumber, String maskedSenderName,
                                       ShipmentStatus currentStatus, LocalDateTime createdAt) {
    public Long getId() { return id; }
    public String getMaskedTrackingNumber() { return maskedTrackingNumber; }
    public String getMaskedSenderName() { return maskedSenderName; }
    public ShipmentStatus getCurrentStatus() { return currentStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
