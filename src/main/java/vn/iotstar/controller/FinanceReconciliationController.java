package vn.iotstar.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.dto.finance.ReconciliationSubmissionRequest;
import vn.iotstar.dto.finance.ShiftExpectedItemResponse;
import vn.iotstar.entity.ShipperReconciliation;
import vn.iotstar.service.FinanceReconciliationService;

import java.time.LocalDate;
import java.util.List;

@Controller
public class FinanceReconciliationController {

    private final FinanceReconciliationService reconciliationService;

    public FinanceReconciliationController(FinanceReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/manager/reconciliation")
    public String showReconciliationForm(@RequestParam(value = "shipperId", required = false) Long shipperId,
                                         @RequestParam(value = "date", required = false) LocalDate date,
                                         Model model) {
        if (shipperId != null) {
            List<ShipperReconciliation> history = reconciliationService.getReconciliationsByShipper(shipperId);
            model.addAttribute("history", history);

            List<ShiftExpectedItemResponse> expectedItems =
                    reconciliationService.getShiftExpectedItems(shipperId, date);
            model.addAttribute("expectedItems", expectedItems);
        }
        return "manager/reconciliation";
    }

    @PostMapping("/manager/reconciliation/submit")
    public String submitReconciliation(@Valid @ModelAttribute("submissionRequest") ReconciliationSubmissionRequest request,
                                       BindingResult bindingResult,
                                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation error: invalid reconciliation request");
            return "redirect:/manager/reconciliation";
        }
        try {
            ShipperReconciliation rec = reconciliationService.reconcile(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Reconciliation #" + rec.getId() + " completed with status: " + rec.getStatus());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/reconciliation";
    }

    @PostMapping("/manager/reconciliation/payout")
    public String payoutReadyCod(@RequestParam("codTransactionId") Long codTransactionId,
                                  RedirectAttributes redirectAttributes) {
        try {
            reconciliationService.payoutReadyCod(codTransactionId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "COD transaction #" + codTransactionId + " was paid out to the sender wallet");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/reconciliation";
    }
}

