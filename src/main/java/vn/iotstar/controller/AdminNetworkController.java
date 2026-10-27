package vn.iotstar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.iotstar.service.impl.AdminNetworkService;

@Controller
public class AdminNetworkController {
    private final AdminNetworkService service;

    public AdminNetworkController(AdminNetworkService service) {
        this.service = service;
    }

    @GetMapping("/admin/network")
    public String network(Model model) {
        model.addAttribute("hubs", service.hubs());
        model.addAttribute("routes", service.routes());
        return "admin/network";
    }
}
