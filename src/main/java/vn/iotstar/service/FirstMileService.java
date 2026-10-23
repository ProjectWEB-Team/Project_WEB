package vn.iotstar.service;

import vn.iotstar.dto.firstmile.PendingPickupResponse;
import vn.iotstar.dto.firstmile.PickupTaskResponse;
import vn.iotstar.dto.firstmile.ShipperOptionResponse;

import java.util.List;

/** HOME_PICKUP first mile: coordinator assignment, shipper attempts/completion and verified service failure. */
public interface FirstMileService {
    List<PendingPickupResponse> pendingPickups();
    List<ShipperOptionResponse> shippersOfMyHub();
    void assignPickup(Long shipmentId, Long shipperUserId);
    void reportServiceFailure(Long shipmentId, String note);
    List<PickupTaskResponse> myPickupTasks();
    void recordFailedAttempt(Long assignmentId, String result, String note);
    void completePickup(Long assignmentId, String scannedTrackingNumber);
}
