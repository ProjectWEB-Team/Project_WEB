package vn.iotstar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import vn.iotstar.service.ManagerOperationsService;

/** MVC adapter only; role/profile/hub authorization and aggregate selection stay in the service. */
@Controller
public class ManagerOperationsController {
    private final ManagerOperationsService managerOperationsService;

    public ManagerOperationsController(ManagerOperationsService managerOperationsService) {
        this.managerOperationsService = managerOperationsService;
    }

    @GetMapping("/manager/operations")
    public String dashboard(Model model) {
        model.addAttribute("dashboard", managerOperationsService.getDashboard());
        model.addAttribute("workItems", managerOperationsService.getWorkItems());
        return "manager/operations-dashboard";
    }

    @GetMapping("/manager/operations/work/{kind}/{id}")
    public String workItem(@PathVariable String kind, @PathVariable Long id, Model model) {
        model.addAttribute("item", managerOperationsService.getWorkItem(kind, id));
        return "manager/operation-work-item";
    }
}
