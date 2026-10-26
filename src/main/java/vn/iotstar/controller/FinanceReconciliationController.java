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
import vn.iotstar.service.ShipmentFinanceService;
import vn.iotstar.dto.finance.CompensationRequest;

import java.time.LocalDate;
import java.util.List;

@Controller
public class FinanceReconciliationController {
    @org.springframework.web.bind.annotation.ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN)
    public String forbidden() { return "error/403"; }

    private final FinanceReconciliationService reconciliationService;
    private final ShipmentFinanceService shipmentFinance;

    public FinanceReconciliationController(FinanceReconciliationService reconciliationService, ShipmentFinanceService shipmentFinance) {
        this.reconciliationService = reconciliationService;
        this.shipmentFinance = shipmentFinance;
    }

    @GetMapping("/manager/reconciliation")
    public String showReconciliationForm(@RequestParam(value = "shipperId", required = false) Long shipperId,
                                         @RequestParam(value = "date", required = false) LocalDate date,
                                         @RequestParam(value = "method", defaultValue = "CASH") String method,
                                         Model model) {
        if (shipperId != null) {
            model.addAttribute("history", reconciliationService.getReconciliationsByShipper(shipperId));
            model.addAttribute("readyForPayout", reconciliationService.getReadyPayouts(shipperId));

            List<ShiftExpectedItemResponse> expectedItems =
                    reconciliationService.getShiftExpectedItems(shipperId, date, method);
            model.addAttribute("expectedItems", expectedItems);
        }
        model.addAttribute("shipperId", shipperId);
        model.addAttribute("selectedDate", date);
        model.addAttribute("selectedMethod", method);
        model.addAttribute("pendingRefunds", shipmentFinance.listPendingRefundCandidates());
        model.addAttribute("confirmedLosses", shipmentFinance.listConfirmedLossCandidates());
        return "manager/reconciliation";
    }

    @GetMapping("/manager/reconciliation/detail")
    public String detail(@RequestParam Long reconciliationId, Model model) {
        model.addAttribute("reconciliationItems", reconciliationService.getReconciliationItems(reconciliationId));
        return "manager/reconciliation";
    }

    @PostMapping("/manager/reconciliation/refund")
    public String refund(@RequestParam Long paymentId, RedirectAttributes redirectAttributes) {
        shipmentFinance.refundShipmentPayment(paymentId, "Pending cancellation obligation");
        redirectAttributes.addFlashAttribute("successMessage", "Refund completed");
        return "redirect:/manager/reconciliation";
    }

    @PostMapping("/manager/reconciliation/compensate")
    public String compensate(@Valid @ModelAttribute CompensationRequest request, BindingResult errors,
                             RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid compensation request");
        } else {
            shipmentFinance.compensateLostShipment(request.shipmentId(), request.incidentId(), request.reason());
            redirectAttributes.addFlashAttribute("successMessage", "Compensation completed");
        }
        return "redirect:/manager/reconciliation";
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
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "The reconciliation could not be completed. Refresh the recorded sources and try again.");
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
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "The COD source is not eligible for payout.");
        }
        return "redirect:/manager/reconciliation";
    }
}

