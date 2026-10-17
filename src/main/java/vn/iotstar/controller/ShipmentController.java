package vn.iotstar.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.iotstar.dto.shipment.CreateShipmentRequest;
import vn.iotstar.service.ShipmentService;

/** HTTP/view adapter for the minimal authenticated shipment tracer. */
@Controller
@RequestMapping("/user/shipments")
public class ShipmentController {
    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("createShipmentRequest")) {
            model.addAttribute("createShipmentRequest", new CreateShipmentRequest());
        }
        return "shipment/create";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute CreateShipmentRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "shipment/create";
        }
        Long shipmentId = shipmentService.create(request);
        return "redirect:/user/shipments/" + shipmentId;
    }

    @GetMapping("/{shipmentId}")
    public String detail(@PathVariable Long shipmentId, Model model) {
        model.addAttribute("shipment", shipmentService.getDetail(shipmentId));
        return "shipment/detail";
    }
}
