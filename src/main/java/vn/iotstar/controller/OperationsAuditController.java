package vn.iotstar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import vn.iotstar.service.OperationsAuditService;

/** Read-only MVC adapter; the service owns all shipment/resource/scope authorization. */
@Controller
public class OperationsAuditController {
    private final OperationsAuditService operationsAuditService;

    public OperationsAuditController(OperationsAuditService operationsAuditService) {
        this.operationsAuditService = operationsAuditService;
    }

    @GetMapping({"/hub/audit/shipments/{shipmentId}", "/manager/audit/shipments/{shipmentId}", "/driver/audit/shipments/{shipmentId}"})
    public String timeline(@PathVariable Long shipmentId, Model model) {
        model.addAttribute("timeline", operationsAuditService.getTimeline(shipmentId));
        return "hub/audit-timeline";
    }
}
