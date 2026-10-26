package vn.iotstar.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.dto.driver.TripActionResponse;
import vn.iotstar.dto.driver.TripDelayCommand;
import vn.iotstar.dto.driver.TripDelayRequest;
import vn.iotstar.service.LinehaulTripService;

/** MVC/API adapter only; service derives profile, assignment, hub and transition scope. */
@Controller
public class DriverOperationsController {
    private final LinehaulTripService linehaulTripService;

    public DriverOperationsController(LinehaulTripService linehaulTripService) { this.linehaulTripService = linehaulTripService; }

    @GetMapping("/driver")
    public String trips(Model model) {
        model.addAttribute("vehicles", linehaulTripService.availableVehicles());
        if (!model.containsAttribute("tripDelayCommand")) model.addAttribute("tripDelayCommand", new TripDelayCommand(null, null, null));
        return "driver/trips";
    }

    @PostMapping("/driver/handovers/accept")
    public String accept(@RequestParam Long handoverId, @RequestParam Long vehicleId, RedirectAttributes redirect) {
        linehaulTripService.acceptHandover(handoverId, vehicleId);
        redirect.addFlashAttribute("operationMessage", "Assigned handover accepted for linehaul.");
        return "redirect:/driver";
    }

    @PostMapping("/driver/trips/start")
    public String start(@RequestParam Long tripId, RedirectAttributes redirect) {
        linehaulTripService.startTrip(tripId);
        redirect.addFlashAttribute("operationMessage", "Trip start recorded.");
        return "redirect:/driver";
    }

    @PostMapping("/driver/trips/arrive")
    public String arrive(@RequestParam Long tripId, RedirectAttributes redirect) {
        linehaulTripService.arriveTrip(tripId);
        redirect.addFlashAttribute("operationMessage", "Destination arrival recorded; inbound receipt remains required.");
        return "redirect:/driver";
    }

    @PostMapping("/driver/trips/delay")
    public String delay(@Valid @ModelAttribute TripDelayCommand command, BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) { redirect.addFlashAttribute("operationMessage", "Enter a bounded trip-delay note."); return "redirect:/driver"; }
        linehaulTripService.reportTripDelay(command);
        redirect.addFlashAttribute("operationMessage", "Trip-delay evidence recorded.");
        return "redirect:/driver";
    }

    @PostMapping("/api/driver/trips/{tripId}/delay")
    @ResponseBody
    public TripActionResponse delayApi(@PathVariable Long tripId, @Valid @RequestBody TripDelayRequest request) {
        linehaulTripService.reportTripDelay(new TripDelayCommand(tripId, request.note(), request.evidenceReference()));
        return new TripActionResponse(tripId, "TRIP_DELAY_RECORDED", true);
    }
}
