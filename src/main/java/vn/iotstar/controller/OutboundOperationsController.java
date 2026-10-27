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
import vn.iotstar.dto.hub.CreateContainerCommand;
import vn.iotstar.dto.hub.CreateHandoverCommand;
import vn.iotstar.dto.hub.CorrectHandoverCommand;
import vn.iotstar.dto.hub.LoadContainerCommand;
import vn.iotstar.service.ContainerHandoverService;

import java.util.List;

/** JSP adapter only: the custody service owns all scope and transition decisions. */
@Controller
@RequestMapping("/hub/outbound")
public class OutboundOperationsController {
    private final ContainerHandoverService containerHandoverService;
    public OutboundOperationsController(ContainerHandoverService containerHandoverService) { this.containerHandoverService = containerHandoverService; }

    @GetMapping
    public String queue(Model model) {
        if (!model.containsAttribute("createContainerCommand")) model.addAttribute("createContainerCommand", new CreateContainerCommand(null, null));
        if (!model.containsAttribute("loadContainerCommand")) model.addAttribute("loadContainerCommand", new LoadContainerCommand(null, null));
        if (!model.containsAttribute("createHandoverCommand")) model.addAttribute("createHandoverCommand", new CreateHandoverCommand(null, null, List.of()));
        if (!model.containsAttribute("correctHandoverCommand")) model.addAttribute("correctHandoverCommand", new CorrectHandoverCommand(null, List.of()));
        return "hub/outbound-queue";
    }

    @PostMapping("/containers")
    public String createContainer(@Valid @ModelAttribute CreateContainerCommand command, BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) return invalid(redirect);
        Long id = containerHandoverService.createContainer(command); redirect.addFlashAttribute("operationMessage", "Đã tạo bao, ID bao: " + id + "."); return "redirect:/hub/outbound";
    }
    @PostMapping("/load")
    public String load(@Valid @ModelAttribute LoadContainerCommand command, BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) return invalid(redirect);
        containerHandoverService.loadShipment(command); redirect.addFlashAttribute("operationMessage", "Đã cho đơn vào bao."); return "redirect:/hub/outbound";
    }
    @PostMapping("/handovers")
    public String createHandover(@Valid @ModelAttribute CreateHandoverCommand command, BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) return invalid(redirect);
        Long id = containerHandoverService.createHandover(command); redirect.addFlashAttribute("operationMessage", "Đã tạo phiếu bàn giao cho tài xế, ID phiếu: " + id + ". Báo ID này cho tài xế."); return "redirect:/hub/outbound";
    }
    @PostMapping("/handovers/correct")
    public String correct(@Valid @ModelAttribute CorrectHandoverCommand command, BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) return invalid(redirect);
        Long id = containerHandoverService.correctByCancellationAndRecreation(command);
        redirect.addFlashAttribute("operationMessage", "Đã tạo phiếu bàn giao thay thế, ID phiếu: " + id + ".");
        return "redirect:/hub/outbound";
    }
    private String invalid(RedirectAttributes redirect) { redirect.addFlashAttribute("operationMessage", "Nhập đúng các ID nghiệp vụ."); return "redirect:/hub/outbound"; }
}
