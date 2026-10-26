package vn.iotstar.dto.driver;

/** Safe driver operation projection; it carries no cross-driver/hub details. */
public record TripActionResponse(Long tripId, String status, boolean destinationReceiptRequired) {
    // JavaBean accessors for the configured Jasper/EL 5 runtime.
    public Long getTripId() { return tripId; }
    public String getStatus() { return status; }
    public boolean getDestinationReceiptRequired() { return destinationReceiptRequired; }
}
