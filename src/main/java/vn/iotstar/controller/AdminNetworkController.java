package vn.iotstar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.service.impl.AdminNetworkService;

/** Administrator hub tree and routing rules; every rule is rechecked in AdminNetworkService. */
@Controller
@RequestMapping("/admin/network")
public class AdminNetworkController {
    private final AdminNetworkService service;

    public AdminNetworkController(AdminNetworkService service) {
        this.service = service;
    }

    @GetMapping
    public String network(Model model) {
        model.addAttribute("hubs", service.hubs());
        model.addAttribute("routes", service.routes());
        return "admin/network";
    }

    @PostMapping("/hubs")
    public String createHub(@RequestParam String code, @RequestParam String name, @RequestParam String type,
                            @RequestParam String region, @RequestParam String province,
                            @RequestParam(required = false) String district, @RequestParam(required = false) String address,
                            @RequestParam(required = false) Long parentId, RedirectAttributes redirect) {
        return run(redirect, "Đã tạo bưu cục " + code + ".",
                () -> service.createHub(code, name, type, region, province, district, address, parentId));
    }

    @PostMapping("/hubs/{hubId}")
    public String editHub(@PathVariable Long hubId, @RequestParam String name, @RequestParam String province,
                          @RequestParam(required = false) String district, @RequestParam(required = false) String address,
                          @RequestParam(required = false) Long parentId, RedirectAttributes redirect) {
        return run(redirect, "Đã cập nhật bưu cục.", () -> service.editHub(hubId, name, province, district, address, parentId));
    }

    @PostMapping("/hubs/{hubId}/status")
    public String hubStatus(@PathVariable Long hubId, @RequestParam boolean active, RedirectAttributes redirect) {
        return run(redirect, active ? "Đã bật lại bưu cục." : "Đã ngừng bưu cục.", () -> service.setHubActive(hubId, active));
    }

    @PostMapping("/rules")
    public String addRule(@RequestParam Long currentHubId, @RequestParam String region,
                          @RequestParam(required = false) String province, @RequestParam Long nextHubId,
                          RedirectAttributes redirect) {
        return run(redirect, "Đã thêm quy tắc định tuyến.", () -> service.addRule(currentHubId, region, province, nextHubId));
    }

    @PostMapping("/rules/{ruleId}/deactivate")
    public String deactivateRule(@PathVariable Long ruleId, RedirectAttributes redirect) {
        return run(redirect, "Đã ngừng quy tắc định tuyến.", () -> service.deactivateRule(ruleId));
    }

    private static String run(RedirectAttributes redirect, String success, Runnable action) {
        try {
            action.run();
            redirect.addFlashAttribute("successMessage", success);
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("errorMessage", FlashMessages.of(exception));
        }
        return "redirect:/admin/network";
    }
}
