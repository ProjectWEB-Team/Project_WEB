package vn.iotstar.dto.operations;

/** Internal immutable reference used to correlate a shipment event with existing custody evidence. */
public record AuditCustodyReference(Long containerId, Long handoverId, Long tripId, Long assignedDriverUserId) {
}
