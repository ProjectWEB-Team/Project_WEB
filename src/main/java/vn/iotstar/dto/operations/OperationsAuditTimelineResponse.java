package vn.iotstar.dto.operations;

import java.util.List;

/** Read-only, scope-authorized event timeline for an existing operational shipment. */
public record OperationsAuditTimelineResponse(Long shipmentId, List<OperationsAuditTimelineEntry> entries) {
}
