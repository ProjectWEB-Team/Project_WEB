package vn.iotstar.controller.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.iotstar.dto.tracking.PublicTrackingResponse;
import vn.iotstar.service.TrackingService;

/** Public JSON tracking; same whitelist as the guest page. */
@RestController
@RequestMapping("/api/tracking")
public class TrackingApiController {
    private final TrackingService trackingService;

    public TrackingApiController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/{trackingNumber}")
    public PublicTrackingResponse track(@PathVariable String trackingNumber) {
        return trackingService.track(trackingNumber);
    }
}
