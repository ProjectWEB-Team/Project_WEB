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

/** Shipper screen: own pickup tasks, failed contact attempts and scan-to-complete. */
@Controller
@RequestMapping("/shipper/tasks")
public class ShipperTaskController {
    private final FirstMileService firstMileService;

    public ShipperTaskController(FirstMileService firstMileService) {
        this.firstMileService = firstMileService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("tasks", firstMileService.myPickupTasks());
        return "shipper/tasks";
    }

    @PostMapping("/{assignmentId}/attempt")
    public String failedAttempt(@PathVariable Long assignmentId, @RequestParam String result,
                                @RequestParam(required = false) String note, RedirectAttributes redirectAttributes) {
        return run(() -> firstMileService.recordFailedAttempt(assignmentId, result, note), "Đã ghi lần liên hệ.", redirectAttributes);
    }

    @PostMapping("/{assignmentId}/complete")
    public String complete(@PathVariable Long assignmentId, @RequestParam String trackingNumber,
                           RedirectAttributes redirectAttributes) {
        return run(() -> firstMileService.completePickup(assignmentId, trackingNumber), "Đã lấy hàng thành công.", redirectAttributes);
    }

    private String run(Runnable action, String success, RedirectAttributes redirectAttributes) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("message", success);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", FlashMessages.text(exception));
        }
        return "redirect:/shipper/tasks";
    }
}
