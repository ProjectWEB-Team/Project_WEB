package vn.iotstar.dto.realtime;

import vn.iotstar.entity.ShipmentStatus;

import java.time.LocalDateTime;

/** Safe, private realtime payload for shipment lifecycle changes: no contact, address, money or staff data. */
public record ShipmentCreatedEventResponse(String trackingNumber, ShipmentStatus currentStatus,
                                           String eventType, LocalDateTime occurredAt) {
}
