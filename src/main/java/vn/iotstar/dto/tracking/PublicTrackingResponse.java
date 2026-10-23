package vn.iotstar.dto.tracking;

import vn.iotstar.entity.ShipmentStatus;

import java.util.List;

/** Guest tracking projection (Workflow whitelist): no contact, address, staff, finance or audit data. ETA is not shown until the team defines its source. */
public record PublicTrackingResponse(String trackingNumber, ShipmentStatus currentStatus, String currentHubName,
                                     List<TrackingEventResponse> events) {
    public String getTrackingNumber() { return trackingNumber; }
    public ShipmentStatus getCurrentStatus() { return currentStatus; }
    public String getCurrentHubName() { return currentHubName; }
    public List<TrackingEventResponse> getEvents() { return events; }
}
