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

    private final vn.iotstar.repository.HubRepository hubs;

    public LastMileDeliveryController(LastMileDeliveryService lastMileDeliveryService, vn.iotstar.repository.HubRepository hubs) {
        this.lastMileDeliveryService = lastMileDeliveryService;
        this.hubs = hubs;
    }

    @GetMapping("/shipper/delivery-tasks")
    public String listDeliveryTasks(Model model) {
        List<DeliveryTaskResponse> tasks = lastMileDeliveryService.getAssignedDeliveryTasks();
        model.addAttribute("tasks", tasks);
        model.addAttribute("hubs", hubs.findByStatusOrderByCode("ACTIVE"));
        return "shipper/delivery-tasks";
    }

    @PostMapping("/shipper/delivery-tasks/scan")
    public String scanForDelivery(@Valid @ModelAttribute("scanRequest") DeliveryScanRequest request,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thông tin quét chưa hợp lệ.");
            return "redirect:/shipper/delivery-tasks";
        }
        try {
            lastMileDeliveryService.scanForDelivery(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã quét, đơn chuyển sang đang giao.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", FlashMessages.of(e));
        }
        return "redirect:/shipper/delivery-tasks";
    }

    @PostMapping("/shipper/delivery-tasks/collect")
    public String recordCollection(@Valid @ModelAttribute("collectionRequest") DeliveryCollectionRequest request,
                                   BindingResult bindingResult,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thông tin thu tiền chưa hợp lệ.");
            return "redirect:/shipper/delivery-tasks";
        }
        try {
            lastMileDeliveryService.recordCollection(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã ghi nhận thu tiền.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", FlashMessages.of(e));
        }
        return "redirect:/shipper/delivery-tasks";
    }

    /** One tap for the shipper: record what was collected, then confirm the delivery. */
    @PostMapping("/shipper/delivery-tasks/deliver")
    public String deliver(@Valid @ModelAttribute("collectionRequest") DeliveryCollectionRequest request,
                          BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thông tin thu tiền chưa hợp lệ.");
            return "redirect:/shipper/delivery-tasks";
        }
        try {
            lastMileDeliveryService.recordCollection(request);
            lastMileDeliveryService.confirmDelivery(request.getShipmentId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã thu tiền và xác nhận giao thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", FlashMessages.of(e));
        }
        return "redirect:/shipper/delivery-tasks";
    }

    @PostMapping("/shipper/delivery-tasks/confirm")
    public String confirmDelivery(@RequestParam("shipmentId") Long shipmentId,
                                  RedirectAttributes redirectAttributes) {
        try {
            lastMileDeliveryService.confirmDelivery(shipmentId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận giao thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", FlashMessages.of(e));
        }
        return "redirect:/shipper/delivery-tasks";
    }

    @PostMapping("/shipper/delivery-tasks/failure")
    public String recordFailure(@Valid @ModelAttribute("failureRequest") DeliveryFailureRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thông tin giao không thành công chưa hợp lệ.");
            return "redirect:/shipper/delivery-tasks";
        }
        try {
            lastMileDeliveryService.recordFailure(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã ghi nhận giao không thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", FlashMessages.of(e));
        }
        return "redirect:/shipper/delivery-tasks";
    }

    @GetMapping("/shipper/return-tasks")
    public String listReturnTasks(Model model) {
        List<ReturnTaskResponse> tasks = lastMileDeliveryService.getAssignedReturnTasks();
        model.addAttribute("tasks", tasks);
        return "shipper/return-tasks";
    }

    @PostMapping("/shipper/return-tasks/scan")
    public String scanReturn(@RequestParam Long shipmentId) {
        lastMileDeliveryService.scanReturnDelivery(shipmentId);return "redirect:/shipper/return-tasks";
    }
    @PostMapping("/hub/delivery-returns/receive")
    public String receiveFailed(@RequestParam Long shipmentId,@RequestParam String trackingNumber) {
        lastMileDeliveryService.receiveFailedDelivery(shipmentId,trackingNumber);return "redirect:/hub/inbound";
    }
    @PostMapping("/shipper/return-tasks/confirm")
    public String confirmReturnDelivery(@RequestParam("shipmentId") Long shipmentId,
                                        RedirectAttributes redirectAttributes) {
        try {
            lastMileDeliveryService.confirmReturnDelivery(shipmentId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận trả hàng hoàn.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", FlashMessages.of(e));
        }
        return "redirect:/shipper/return-tasks";
    }

    @PostMapping("/hub/return-collection")
    public String processReturnHandover(@Valid @ModelAttribute("returnRequest") ReturnHandoverRequest request,
                                        BindingResult bindingResult,
                                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thông tin nhận hàng hoàn chưa hợp lệ.");
            return "redirect:/hub/return-collection";
        }
        try {
            lastMileDeliveryService.processReturnHandover(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã trả hàng hoàn cho người gửi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", FlashMessages.of(e));
        }
        return "redirect:/hub/return-collection";
    }

    @GetMapping("/hub/return-collection")
    public String returnCollectionPage(Model model) {
        return "user/return-collection";
    }
}

