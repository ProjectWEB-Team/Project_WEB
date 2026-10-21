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
import vn.iotstar.entity.User;
import vn.iotstar.entity.WithdrawalRequest;
import vn.iotstar.repository.UserRepository;
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
    private final UserRepository userRepository;

    public AdminWithdrawalController(WithdrawalService withdrawalService, UserRepository userRepository) {
        this.withdrawalService = withdrawalService;
        this.userRepository = userRepository;
    }

    @GetMapping("/admin/withdrawals")
    public String listWithdrawals(Model model) {
        List<WithdrawalSummaryResponse> pending = withdrawalService.getPendingWithdrawals().stream()
                .map(this::toSummary)
                .toList();
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
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/withdrawals";
    }

    @PostMapping("/admin/withdrawals/{requestId}/reject")
    public String reject(@Valid @ModelAttribute("reviewRequest") WithdrawalReviewDto review,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation error: invalid rejection request");
            return "redirect:/admin/withdrawals";
        }
        try {
            WithdrawalRequest rejected = withdrawalService.rejectWithdrawal(review);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Withdrawal #" + rejected.getId() + " rejected and the locked balance was released");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/withdrawals";
    }

    private WithdrawalSummaryResponse toSummary(WithdrawalRequest request) {
        String username = userRepository.findById(request.getUserId())
                .map(User::getUsername)
                .orElse("user#" + request.getUserId());
        return new WithdrawalSummaryResponse(
                request.getId(), request.getUserId(), username, request.getAmount(), request.getBankName(),
                request.getAccountNumber(), request.getAccountHolderName(), request.getStatus(),
                request.getRejectionReason(), request.getRequestedAt());
    }
}