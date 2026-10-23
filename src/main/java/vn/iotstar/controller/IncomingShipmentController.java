package vn.iotstar.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.service.TrackingService;

import java.util.HashSet;
import java.util.Set;

/** Linked recipient's incoming shipments: masked by default, unlocked per shipment for this session only. */
@Controller
@RequestMapping("/user/incoming")
public class IncomingShipmentController {
    private static final String UNLOCKED = IncomingShipmentController.class.getName() + ".unlocked";
    private final TrackingService trackingService;

    public IncomingShipmentController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("shipments", trackingService.incoming(page));
        return "shipment/incoming";
    }

    @GetMapping("/{shipmentId}")
    public String detail(@PathVariable Long shipmentId, HttpSession session, Model model) {
        model.addAttribute("shipment", trackingService.incomingDetail(shipmentId, unlocked(session).contains(shipmentId)));
        return "shipment/incoming-detail";
    }

    @PostMapping("/{shipmentId}/unlock")
    public String unlock(@PathVariable Long shipmentId, @RequestParam String trackingNumber, HttpSession session,
                         RedirectAttributes redirectAttributes) {
        if (trackingService.unlock(shipmentId, trackingNumber)) {
            Set<Long> ids = new HashSet<>(unlocked(session));
            ids.add(shipmentId);
            session.setAttribute(UNLOCKED, ids);
        } else {
            redirectAttributes.addFlashAttribute("error", "Mã vận đơn không đúng.");
        }
        return "redirect:/user/incoming/" + shipmentId;
    }

    @SuppressWarnings("unchecked")
    private static Set<Long> unlocked(HttpSession session) {
        Object ids = session.getAttribute(UNLOCKED);
        return ids instanceof Set<?> set ? (Set<Long>) set : Set.of();
    }
}
