package vn.iotstar.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.dto.hub.MissingParcelResolutionCommand;
import vn.iotstar.service.DestinationReceiptService;

/** Investigation decisions use the incident's scoped custody evidence, not current parcel location. */
@Controller
@RequestMapping("/manager/incidents/missing")
public class MissingParcelController {
    private final DestinationReceiptService receiptService;
    public MissingParcelController(DestinationReceiptService receiptService) { this.receiptService = receiptService; }
    @GetMapping
    public String cases(Model model) {
        model.addAttribute("cases", receiptService.missingCases());
        return "manager/missing-parcels";
    }
    @GetMapping("/{incidentId}")
    public String detail(@PathVariable Long incidentId, Model model) {
        model.addAttribute("selectedCase", receiptService.missingCase(incidentId));
        return "manager/missing-parcels";
    }
    @PostMapping("/resolve")
    public String resolve(@Valid @ModelAttribute MissingParcelResolutionCommand command, BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            redirect.addFlashAttribute("operationMessage", "Nhập đủ tham chiếu bàn giao và bằng chứng điều tra.");
        } else {
            receiptService.resolveMissing(command);
            redirect.addFlashAttribute("operationMessage", "Đã ghi nhận kết quả điều tra. Trường hợp thất lạc sẽ chờ xử lý tài chính.");
        }
        return "redirect:/manager/incidents/missing";
    }
}
