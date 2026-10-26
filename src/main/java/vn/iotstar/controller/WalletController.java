package vn.iotstar.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.dto.wallet.WalletResponse;
import vn.iotstar.dto.wallet.WithdrawalRequestDto;
import vn.iotstar.entity.WalletTransaction;
import vn.iotstar.entity.WithdrawalRequest;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.WalletLedgerService;
import vn.iotstar.service.WithdrawalService;

import java.util.List;

@Controller
public class WalletController {

    private final WalletLedgerService walletLedgerService;
    private final WithdrawalService withdrawalService;

    public WalletController(WalletLedgerService walletLedgerService,
                            WithdrawalService withdrawalService) {
        this.walletLedgerService = walletLedgerService;
        this.withdrawalService = withdrawalService;
    }

    @GetMapping("/user/wallet")
    public String showWallet(Model model) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        WalletResponse wallet = walletLedgerService.getWalletResponse(actor.userId());
        List<WalletTransaction> transactions = walletLedgerService.getTransactions(actor.userId());
        List<WithdrawalRequest> withdrawals = withdrawalService.getUserWithdrawals(actor.userId());

        model.addAttribute("wallet", wallet);
        model.addAttribute("transactions", transactions);
        model.addAttribute("withdrawals", withdrawals);
        return "user/wallet";
    }

    @PostMapping("/user/wallet/withdraw")
    public String requestWithdrawal(@Valid @ModelAttribute("withdrawalRequest") WithdrawalRequestDto request,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation error: invalid withdrawal request");
            return "redirect:/user/wallet";
        }
        try {
            WithdrawalRequest created = withdrawalService.requestWithdrawal(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Withdrawal request #" + created.getId() + " submitted for Administrator review");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", FlashMessages.of(exception));
        }
        return "redirect:/user/wallet";
    }
}

