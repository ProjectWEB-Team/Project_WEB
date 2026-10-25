package vn.iotstar.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.WebUtils;
import vn.iotstar.dto.shipment.CreateShipmentRequest;
import vn.iotstar.dto.shipment.QuoteResponse;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.service.ShipmentService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** HTTP/view adapter for shipment intake: list, form, server quote, create, detail, demo payment and cancel. */
@Controller
@RequestMapping("/user/shipments")
public class ShipmentController {
    private static final String QUOTED_DRAFT = ShipmentController.class.getName() + ".quotedDraft";
    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) ShipmentStatus status,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("shipments", shipmentService.listMine(status, page));
        model.addAttribute("status", status);
        model.addAttribute("statuses", ShipmentStatus.values());
        return "shipment/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("createShipmentRequest")) {
            model.addAttribute("createShipmentRequest", new CreateShipmentRequest());
        }
        return "shipment/create";
    }

    @PostMapping("/quote")
    public String quote(@Valid @ModelAttribute CreateShipmentRequest request, BindingResult bindingResult,
                        Model model, HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            session.removeAttribute(QUOTED_DRAFT);
            if (!bindingResult.hasErrors()) {
                try {
                    QuoteResponse quote = shipmentService.quote(request);
                    String reference = UUID.randomUUID().toString();
                    session.setAttribute(QUOTED_DRAFT, new QuotedDraft(reference, request.quoteInputs(), quote.shippingFee()));
                    model.addAttribute("quote", quote);
                    model.addAttribute("quoteReference", reference);
                } catch (IllegalArgumentException exception) {
                    bindingResult.reject("quote.rejected", exception.getMessage());
                }
            }
        }
        return "shipment/create";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute CreateShipmentRequest request, BindingResult bindingResult,
                         @RequestParam(required = false) String quoteReference, HttpSession session) {
        if (bindingResult.hasErrors()) {
            return "shipment/create";
        }
        // One reviewed draft per session; serialize final submits and consume after successful commit.
        synchronized (WebUtils.getSessionMutex(session)) {
            QuotedDraft draft = (QuotedDraft) session.getAttribute(QUOTED_DRAFT);
            if (draft == null || !draft.reference().equals(quoteReference) || !draft.inputs().equals(request.quoteInputs())) {
                bindingResult.reject("quote.changed", "Thông tin hoặc báo giá đã thay đổi. Vui lòng xem báo giá lại trước khi tạo đơn.");
                return "shipment/create";
            }
            try {
                Long shipmentId = shipmentService.create(request, draft.fee());
                session.removeAttribute(QUOTED_DRAFT);
                return "redirect:/user/shipments/" + shipmentId;
            } catch (IllegalArgumentException exception) {
                session.removeAttribute(QUOTED_DRAFT);
                bindingResult.reject("create.rejected", exception.getMessage());
                return "shipment/create";
            }
        }
    }

    @GetMapping("/{shipmentId}")
    public String detail(@PathVariable Long shipmentId, Model model) {
        model.addAttribute("shipment", shipmentService.getDetail(shipmentId));
        return "shipment/detail";
    }

    @PostMapping("/{shipmentId}/confirm-payment")
    public String confirmPayment(@PathVariable Long shipmentId, RedirectAttributes redirectAttributes) {
        try {
            shipmentService.confirmPayment(shipmentId);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/user/shipments/" + shipmentId;
    }

    @PostMapping("/{shipmentId}/cancel")
    public String cancel(@PathVariable Long shipmentId, RedirectAttributes redirectAttributes) {
        try {
            shipmentService.cancel(shipmentId);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/user/shipments/" + shipmentId;
    }

    private record QuotedDraft(String reference, List<Object> inputs, BigDecimal fee) implements java.io.Serializable { }
}
