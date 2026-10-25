package vn.iotstar.controller;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.dto.delivery.DeliveryAssignmentRequest;
import vn.iotstar.service.impl.LastMileAssignmentService;
@Controller
@RequestMapping("/hub/delivery-assignments")
public class LastMileAssignmentController {
    private final LastMileAssignmentService service;
    public LastMileAssignmentController(LastMileAssignmentService service) { this.service=service; }
    @GetMapping public String page(Model model) {
        model.addAttribute("candidates",service.candidates()); model.addAttribute("shippers",service.shippers());
        return "hub/delivery-assignments";
    }
    @PostMapping public String assign(@Valid @ModelAttribute DeliveryAssignmentRequest request, BindingResult result) {
        if(result.hasErrors()) throw new IllegalArgumentException("Invalid assignment");
        service.assign(request); return "redirect:/hub/delivery-assignments";
    }
}
