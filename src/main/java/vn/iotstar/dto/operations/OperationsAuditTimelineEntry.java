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
}
