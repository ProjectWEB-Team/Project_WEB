package vn.iotstar.dto.operations;

import java.util.List;

/** Read-only, scope-authorized event timeline for an existing operational shipment. */
public record OperationsAuditTimelineResponse(Long shipmentId, List<OperationsAuditTimelineEntry> entries) {

    // JavaBean accessors for the configured Jasper/EL 5 runtime.
    public Long getShipmentId() { return shipmentId; }
    public List<OperationsAuditTimelineEntry> getEntries() { return entries; }
}
