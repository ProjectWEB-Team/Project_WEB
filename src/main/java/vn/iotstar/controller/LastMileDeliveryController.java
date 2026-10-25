package vn.iotstar.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.dto.delivery.DeliveryCollectionRequest;
import vn.iotstar.dto.delivery.DeliveryFailureRequest;
import vn.iotstar.dto.delivery.DeliveryScanRequest;
import vn.iotstar.dto.delivery.DeliveryTaskResponse;
import vn.iotstar.dto.returnflow.ReturnHandoverRequest;
import vn.iotstar.dto.returnflow.ReturnTaskResponse;
import vn.iotstar.service.LastMileDeliveryService;

import java.util.List;

@Controller
public class LastMileDeliveryController {

    private final LastMileDeliveryService lastMileDeliveryService;

    public LastMileDeliveryController(LastMileDeliveryService lastMileDeliveryService) {
        this.lastMileDeliveryService = lastMileDeliveryService;
    }

    @GetMapping("/shipper/delivery-tasks")
    public String listDeliveryTasks(Model model) {
        List<DeliveryTaskResponse> tasks = lastMileDeliveryService.getAssignedDeliveryTasks();
        model.addAttribute("tasks", tasks);
        return "shipper/delivery-tasks";
    }

    @PostMapping("/shipper/delivery-tasks/scan")
    public String scanForDelivery(@Valid @ModelAttribute("scanRequest") DeliveryScanRequest request,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation error: invalid scan parameters");
            return "redirect:/shipper/delivery-tasks";
        }
        try {
            lastMileDeliveryService.scanForDelivery(request);
            redirectAttributes.addFlashAttribute("successMessage", "Shipment scanned successfully into OUT_FOR_DELIVERY");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/shipper/delivery-tasks";
    }

    @PostMapping("/shipper/delivery-tasks/collect")
    public String recordCollection(@Valid @ModelAttribute("collectionRequest") DeliveryCollectionRequest request,
                                   BindingResult bindingResult,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation error: invalid collection parameters");
            return "redirect:/shipper/delivery-tasks";
        }
        try {
            lastMileDeliveryService.recordCollection(request);
            redirectAttributes.addFlashAttribute("successMessage", "Payment collection recorded successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/shipper/delivery-tasks";
    }

    @PostMapping("/shipper/delivery-tasks/confirm")
    public String confirmDelivery(@RequestParam("shipmentId") Long shipmentId,
                                  RedirectAttributes redirectAttributes) {
        try {
            lastMileDeliveryService.confirmDelivery(shipmentId);
            redirectAttributes.addFlashAttribute("successMessage", "Shipment confirmed DELIVERED");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/shipper/delivery-tasks";
    }

    @PostMapping("/shipper/delivery-tasks/failure")
    public String recordFailure(@Valid @ModelAttribute("failureRequest") DeliveryFailureRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation error: invalid failure parameters");
            return "redirect:/shipper/delivery-tasks";
        }
        try {
            lastMileDeliveryService.recordFailure(request);
            redirectAttributes.addFlashAttribute("successMessage", "Delivery failure recorded");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/shipper/delivery-tasks";
    }

    @GetMapping("/shipper/return-tasks")
    public String listReturnTasks(Model model) {
        List<ReturnTaskResponse> tasks = lastMileDeliveryService.getAssignedReturnTasks();
        model.addAttribute("tasks", tasks);
        return "shipper/return-tasks";
    }

    @PostMapping("/shipper/return-tasks/confirm")
    public String confirmReturnDelivery(@RequestParam("shipmentId") Long shipmentId,
                                        RedirectAttributes redirectAttributes) {
        try {
            lastMileDeliveryService.confirmReturnDelivery(shipmentId);
            redirectAttributes.addFlashAttribute("successMessage", "Return delivery confirmed completed");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/shipper/return-tasks";
    }

    @PostMapping("/user/return-collection")
    public String processReturnHandover(@Valid @ModelAttribute("returnRequest") ReturnHandoverRequest request,
                                        BindingResult bindingResult,
                                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation error on return pickup");
            return "redirect:/user/return-collection";
        }
        try {
            lastMileDeliveryService.processReturnHandover(request);
            redirectAttributes.addFlashAttribute("successMessage", "Return parcel collected successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/user/return-collection";
    }

    @GetMapping("/user/return-collection")
    public String returnCollectionPage(Model model) {
        return "user/return-collection";
    }
}

