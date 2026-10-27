package vn.iotstar.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.dto.wallet.WithdrawalReviewDto;
import vn.iotstar.dto.wallet.WithdrawalSummaryResponse;
import vn.iotstar.entity.WithdrawalRequest;
import vn.iotstar.service.WithdrawalService;

import java.util.List;

/**
 * Administrator workspace for the withdrawal review the Workflow reserves for
 * ROLE_ADMIN. The data scope is the Administrator role, enforced again in the
 * service layer.
 */
@Controller
public class AdminWithdrawalController {

    private final WithdrawalService withdrawalService;

    public AdminWithdrawalController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    @GetMapping("/admin/withdrawals")
    public String listWithdrawals(Model model) {
        List<WithdrawalSummaryResponse> pending = withdrawalService.getPendingWithdrawalSummaries();
        model.addAttribute("pendingWithdrawals", pending);
        return "admin/withdrawals";
    }

    @PostMapping("/admin/withdrawals/{requestId}/approve")
    public String approve(@PathVariable Long requestId,
                          RedirectAttributes redirectAttributes) {
        WithdrawalReviewDto review = new WithdrawalReviewDto(requestId, null);
        try {
            WithdrawalRequest approved = withdrawalService.approveWithdrawal(review);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Withdrawal #" + approved.getId() + " approved and released to the sender wallet");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", FlashMessages.of(exception));
        }
        return "redirect:/admin/withdrawals";
    }

    @PostMapping("/admin/withdrawals/{requestId}/reject")
    public String reject(@Valid @ModelAttribute("reviewRequest") WithdrawalReviewDto review,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Yêu cầu từ chối chưa hợp lệ.");
            return "redirect:/admin/withdrawals";
        }
        try {
            WithdrawalRequest rejected = withdrawalService.rejectWithdrawal(review);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Withdrawal #" + rejected.getId() + " rejected and the locked balance was released");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", FlashMessages.of(exception));
        }
        return "redirect:/admin/withdrawals";
    }
}