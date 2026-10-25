package vn.iotstar.dto.operations;

/** Internal immutable reference used to correlate a shipment event with existing custody evidence. */
public record AuditCustodyReference(Long containerId, Long handoverId, Long tripId, Long assignedDriverUserId) {

    // JavaBean accessors for the configured Jasper/EL 5 runtime.
    public Long getContainerId() { return containerId; }
    public Long getHandoverId() { return handoverId; }
    public Long getTripId() { return tripId; }
    public Long getAssignedDriverUserId() { return assignedDriverUserId; }
}
