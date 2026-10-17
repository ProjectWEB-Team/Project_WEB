package vn.iotstar.dto.shipment;

import java.time.LocalDateTime;

/** Safe tracking-event read model. */
public record ShipmentEventResponse(String eventType, String description, LocalDateTime occurredAt) {
    public String getEventType() { return eventType; }
    public String getDescription() { return description; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
