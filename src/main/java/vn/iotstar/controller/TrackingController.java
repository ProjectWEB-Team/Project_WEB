package vn.iotstar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.service.TrackingService;

/** Public guest tracking search; shows only the whitelisted projection. */
@Controller
public class TrackingController {
    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/tracking")
    public String search(@RequestParam(required = false) String code, Model model) {
        if (code != null && !code.isBlank()) {
            model.addAttribute("code", code);
            try {
                model.addAttribute("tracking", trackingService.track(code));
            } catch (ResourceNotFoundException exception) {
                model.addAttribute("notFound", true);
            }
        }
        return "tracking/search";
    }
}
