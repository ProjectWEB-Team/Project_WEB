package vn.iotstar.dto.realtime;

import vn.iotstar.entity.ShipmentStatus;

import java.time.LocalDateTime;

/** Safe, private realtime payload for the Phase-01 shipment creation tracer. */
public record ShipmentCreatedEventResponse(String trackingNumber, ShipmentStatus currentStatus,
                                           String eventType, LocalDateTime occurredAt) {
}
