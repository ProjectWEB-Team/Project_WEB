package vn.iotstar.dto.tracking;

import java.time.LocalDateTime;

/** Allowlisted timeline entry: a fixed public label, never the internal event description. */
public record TrackingEventResponse(String label, LocalDateTime occurredAt) {
    public String getLabel() { return label; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
