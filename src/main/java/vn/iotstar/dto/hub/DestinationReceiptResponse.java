package vn.iotstar.dto.hub;

/** Safe acknowledgement of one idempotent destination receipt scan. */
public record DestinationReceiptResponse(
        Long handoverId,
        Long containerId,
        Long shipmentId,
        boolean alreadyReceived,
        String shipmentStatus) {

    // JavaBean accessors for the configured Jasper/EL 5 runtime.
    public Long getHandoverId() { return handoverId; }
    public Long getContainerId() { return containerId; }
    public Long getShipmentId() { return shipmentId; }
    public boolean getAlreadyReceived() { return alreadyReceived; }
    public String getShipmentStatus() { return shipmentStatus; }
}
