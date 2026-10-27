package vn.iotstar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.service.impl.AdminPricingService;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/pricing")
public class AdminPricingController {
    private final AdminPricingService service;

    public AdminPricingController(AdminPricingService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("policies", service.all());
        return "admin/pricing";
    }

    @PostMapping("/{policyId}")
    public String update(@PathVariable Long policyId, @RequestParam BigDecimal basePrice,
                         @RequestParam BigDecimal extraWeightPrice, @RequestParam BigDecimal insuranceRatePercent,
                         @RequestParam BigDecimal codFeePercent, @RequestParam(defaultValue = "false") boolean active,
                         RedirectAttributes redirect) {
        try {
            service.update(policyId, basePrice, extraWeightPrice, insuranceRatePercent, codFeePercent, active);
            redirect.addFlashAttribute("successMessage", "Đã cập nhật bảng giá.");
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("errorMessage", FlashMessages.of(exception));
        }
        return "redirect:/admin/pricing";
    }
}
