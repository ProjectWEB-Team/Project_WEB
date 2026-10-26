package vn.iotstar.dto.operations;

import java.time.LocalDateTime;

/** D-15 minimal operational disclosure; names, addresses, notes and custody contents never cross this boundary. */
public record OperationsAuditTimelineEntry(
        String eventType,
        LocalDateTime occurredAt,
        String hubCode,
        String actorRole,
        Long containerId,
        Long handoverId,
        Long tripId) {

    // JavaBean accessors for the configured Jasper/EL 5 runtime.
    public String getEventType() { return eventType; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getHubCode() { return hubCode; }
    public String getActorRole() { return actorRole; }
    public Long getContainerId() { return containerId; }
    public Long getHandoverId() { return handoverId; }
    public Long getTripId() { return tripId; }
}
