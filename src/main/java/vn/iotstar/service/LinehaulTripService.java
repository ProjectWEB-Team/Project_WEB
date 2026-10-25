package vn.iotstar.service;

import vn.iotstar.dto.driver.TripActionResponse;
import vn.iotstar.dto.driver.TripDelayCommand;

/** Service boundary for the assigned-driver accept, start, arrive and delay evidence path. */
public interface LinehaulTripService {
    TripActionResponse acceptHandover(Long handoverId);
    TripActionResponse acceptHandover(Long handoverId, Long vehicleId);
    java.util.List<vn.iotstar.dto.driver.VehicleOptionResponse> availableVehicles();
    TripActionResponse startTrip(Long tripId);
    TripActionResponse arriveTrip(Long tripId);
    Long reportTripDelay(TripDelayCommand command);
}
