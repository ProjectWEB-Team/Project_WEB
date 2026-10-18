package vn.iotstar.dto.driver;

/** Safe driver operation projection; it carries no cross-driver/hub details. */
public record TripActionResponse(Long tripId, String status, boolean destinationReceiptRequired) { }
