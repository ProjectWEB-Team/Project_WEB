package vn.iotstar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.service.FirstMileService;

/** Coordinator screen: assign HOME_PICKUP tasks to same-hub shippers and confirm service failures. */
@Controller
@RequestMapping("/hub/pickups")
public class HubPickupController {
    private final FirstMileService firstMileService;

    public HubPickupController(FirstMileService firstMileService) {
        this.firstMileService = firstMileService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pickups", firstMileService.pendingPickups());
        model.addAttribute("shippers", firstMileService.shippersOfMyHub());
        return "hub/pickups";
    }

    @PostMapping("/{shipmentId}/assign")
    public String assign(@PathVariable Long shipmentId, @RequestParam Long shipperId, RedirectAttributes redirectAttributes) {
        return run(() -> firstMileService.assignPickup(shipmentId, shipperId), "Đã phân công lấy hàng.", redirectAttributes);
    }

    @PostMapping("/{shipmentId}/service-failure")
    public String serviceFailure(@PathVariable Long shipmentId, @RequestParam String note, RedirectAttributes redirectAttributes) {
        return run(() -> firstMileService.reportServiceFailure(shipmentId, note), "Đã hủy đơn do lỗi dịch vụ.", redirectAttributes);
    }

    private String run(Runnable action, String success, RedirectAttributes redirectAttributes) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("message", success);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", FlashMessages.text(exception));
        }
        return "redirect:/hub/pickups";
    }
}
