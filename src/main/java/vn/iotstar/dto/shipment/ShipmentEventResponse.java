package vn.iotstar.dto.shipment;

import java.time.LocalDateTime;

/** Safe tracking-event read model. */
public record ShipmentEventResponse(String eventType, String description, LocalDateTime occurredAt) {
}
