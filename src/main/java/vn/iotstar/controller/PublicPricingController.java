package vn.iotstar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.iotstar.service.impl.PublicPricingService;

import java.math.BigDecimal;

/** Public price list plus a no-login fee estimate; the real quote is still taken when a customer creates a shipment. */
@Controller
public class PublicPricingController {
    private final PublicPricingService pricing;

    public PublicPricingController(PublicPricingService pricing) { this.pricing = pricing; }

    @GetMapping("/calculator")
    public String calculatorRedirect() {
        return "redirect:/pricing";
    }

    @GetMapping("/pricing")
    public String prices(@RequestParam(required = false) String serviceCode, @RequestParam(required = false) String fromRegion,
                         @RequestParam(required = false) String toRegion, @RequestParam(required = false) Integer weightGrams,
                         @RequestParam(required = false) BigDecimal declaredValue, @RequestParam(required = false) BigDecimal codAmount,
                         Model model) {
        model.addAttribute("policies", pricing.active());
        if (serviceCode != null && fromRegion != null && toRegion != null && weightGrams != null) {
            model.addAttribute("asked", true);
            boolean valid = weightGrams >= 1 && weightGrams <= 1_000_000
                    && (declaredValue == null || declaredValue.signum() >= 0) && (codAmount == null || codAmount.signum() >= 0);
            if (valid) {
                pricing.estimate(serviceCode, fromRegion, toRegion, weightGrams, declaredValue, codAmount)
                        .ifPresent(fee -> model.addAttribute("estimate", fee));
            } else {
                model.addAttribute("invalid", true);
            }
        }
        return "pricing/public";
    }
}
