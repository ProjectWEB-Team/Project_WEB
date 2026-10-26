package vn.iotstar.controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.service.impl.ShipmentReturnServiceImpl;
@Controller
@RequestMapping({"/manager/overdue-returns","/admin/overdue-returns"})
public class ReturnDisposalController {
    private final ShipmentReturnServiceImpl service;
    public ReturnDisposalController(ShipmentReturnServiceImpl service){this.service=service;}
    @GetMapping public String page(Model model){model.addAttribute("returns",service.disposalCandidates());return "manager/overdue-returns";}
    @PostMapping public String dispose(@RequestParam Long shipmentId,@RequestParam String reason){service.disposeOverdueReturn(shipmentId,reason);return "redirect:/";}
}
