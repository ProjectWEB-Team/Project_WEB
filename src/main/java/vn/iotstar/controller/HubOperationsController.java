package vn.iotstar.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.dto.hub.InboundShipmentCommand;
import vn.iotstar.dto.hub.SortShipmentCommand;
import vn.iotstar.exception.RouteConfigurationException;
import vn.iotstar.service.HubOperationsService;

/** MVC adapter for hub staff; authorization and all business decisions stay in the service. */
@Controller
@RequestMapping("/hub")
public class HubOperationsController {
    private final HubOperationsService hubOperationsService;

    public HubOperationsController(HubOperationsService hubOperationsService) {
        this.hubOperationsService = hubOperationsService;
    }

    @GetMapping("/sorter")
    public String sorterQueue(Model model) {
        if (!model.containsAttribute("sortShipmentCommand")) {
            model.addAttribute("sortShipmentCommand", new SortShipmentCommand(null));
        }
        if (!model.containsAttribute("inboundShipmentCommand")) {
            model.addAttribute("inboundShipmentCommand", InboundShipmentCommand.pickupReceipt(null));
        }
        return "hub/sorter-queue";
    }

    @PostMapping("/inbound")
    public String receive(@Valid @ModelAttribute InboundShipmentCommand command, BindingResult errors,
                          RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            redirectAttributes.addFlashAttribute("operationMessage", "Nhập ID đơn hàng.");
            return "redirect:/hub/sorter";
        }
        try {
            hubOperationsService.receiveAtHub(command);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("operationMessage", FlashMessages.text(exception));
            return "redirect:/hub/sorter";
        }
        redirectAttributes.addFlashAttribute("operationMessage", "Đã nhận hàng vào hub.");
        return "redirect:/hub/sorter";
    }

    @PostMapping("/sorter")
    public String sort(@Valid @ModelAttribute SortShipmentCommand command, BindingResult errors,
                       RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            redirectAttributes.addFlashAttribute("operationMessage", "Nhập ID đơn hàng.");
            return "redirect:/hub/sorter";
        }
        try {
            redirectAttributes.addFlashAttribute("sortingDecision", hubOperationsService.sortAtHub(command));
            redirectAttributes.addFlashAttribute("operationMessage", "Đã xác định hub kế tiếp.");
        } catch (RouteConfigurationException exception) {
            redirectAttributes.addFlashAttribute("operationMessage", "Tạm thời chưa phân loại được.");
        }
        return "redirect:/hub/sorter";
    }
}
