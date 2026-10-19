package vn.iotstar.dto.hub;

/** Safe acknowledgement of one idempotent destination receipt scan. */
public record DestinationReceiptResponse(
        Long handoverId,
        Long containerId,
        Long shipmentId,
        boolean alreadyReceived,
        String shipmentStatus) {
}
