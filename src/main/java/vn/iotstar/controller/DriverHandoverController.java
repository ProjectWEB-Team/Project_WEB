package vn.iotstar.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.dto.hub.ScanHandoverContainerCommand;
import vn.iotstar.service.ContainerHandoverService;

/** Driver-only MVC adapter; assignment and custody checks remain in the service. */
@Controller
@RequestMapping("/driver/handovers")
public class DriverHandoverController {
    private final ContainerHandoverService containerHandoverService;
    public DriverHandoverController(ContainerHandoverService containerHandoverService) { this.containerHandoverService = containerHandoverService; }
    @PostMapping("/scan")
    public String scan(@Valid @ModelAttribute ScanHandoverContainerCommand command, BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) { redirect.addFlashAttribute("operationMessage", "Enter valid handover and container identifiers."); return "redirect:/driver"; }
        containerHandoverService.scanAndAccept(command);
        redirect.addFlashAttribute("operationMessage", "Container scan recorded.");
        return "redirect:/driver";
    }
}
