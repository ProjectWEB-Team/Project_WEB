package vn.iotstar.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.dto.hub.DestinationReceiptCommand;
import vn.iotstar.dto.hub.DestinationReconciliationCommand;
import vn.iotstar.service.DestinationReceiptService;

/** MVC adapter only; destination identity, scope and custody rules stay in the service. */
@Controller
@RequestMapping("/hub/inbound")
public class InboundOperationsController {
    private final DestinationReceiptService destinationReceiptService;
    public InboundOperationsController(DestinationReceiptService destinationReceiptService) { this.destinationReceiptService = destinationReceiptService; }

    @GetMapping
    public String queue(Model model) {
        if (!model.containsAttribute("destinationReceiptCommand")) model.addAttribute("destinationReceiptCommand", new DestinationReceiptCommand(null, null, null));
        return "hub/inbound-queue";
    }

    @PostMapping("/receive")
    public String receive(@Valid @ModelAttribute DestinationReceiptCommand command, BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) { redirect.addFlashAttribute("operationMessage", "Enter valid arrival, container and shipment identifiers."); return "redirect:/hub/inbound"; }
        destinationReceiptService.receiveItem(command);
        redirect.addFlashAttribute("operationMessage", "Destination receipt recorded.");
        return "redirect:/hub/inbound";
    }

    @PostMapping("/reconcile")
    public String reconcile(@Valid @ModelAttribute DestinationReconciliationCommand command, BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) { redirect.addFlashAttribute("operationMessage", "Enter valid reconciliation scans."); return "redirect:/hub/inbound"; }
        destinationReceiptService.reconcileContainer(command);
        redirect.addFlashAttribute("operationMessage", "Container reconciliation recorded.");
        return "redirect:/hub/inbound";
    }

    @PostMapping("/reconcile-empty")
    public String reconcileEmpty(@RequestParam Long handoverId, @RequestParam Long containerId,
                                 @RequestParam(defaultValue = "false") boolean confirmAllMissing, RedirectAttributes redirect) {
        if (!confirmAllMissing) {
            redirect.addFlashAttribute("operationMessage", "Confirm that no expected parcel was physically received.");
        } else {
            destinationReceiptService.reconcileContainer(new DestinationReconciliationCommand(handoverId, containerId, java.util.List.of()));
            redirect.addFlashAttribute("operationMessage", "Missing container contents recorded for investigation.");
        }
        return "redirect:/hub/inbound";
    }
}
