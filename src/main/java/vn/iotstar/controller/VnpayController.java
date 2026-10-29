package vn.iotstar.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import vn.iotstar.dto.shipment.GatewayPaymentResult;
import vn.iotstar.service.ShipmentService;
import vn.iotstar.service.VnpayService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** VNPay sandbox: the sender is sent to VNPay to pay; the signed result (browser return and server IPN) confirms the shipment. */
@Controller
public class VnpayController {
    private final ShipmentService shipments;
    private final VnpayService vnpay;

    public VnpayController(ShipmentService shipments, VnpayService vnpay) {
        this.shipments = shipments;
        this.vnpay = vnpay;
    }

    @PostMapping("/user/shipments/{shipmentId}/vnpay")
    public String pay(@PathVariable Long shipmentId, HttpServletRequest request, RedirectAttributes flash) {
        String back = "redirect:/user/shipments/" + shipmentId;
        if (!vnpay.enabled()) {
            flash.addFlashAttribute("error", "Cổng thanh toán VNPay chưa được cấu hình.");
            return back;
        }
        try {
            BigDecimal amount = shipments.onlinePaymentAmount(shipmentId);
            String fallback = ServletUriComponentsBuilder.fromContextPath(request).path("/payment/vnpay/return").build().toUriString();
            return "redirect:" + vnpay.paymentUrl(shipmentId, amount, clientIp(request), fallback);
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("error", FlashMessages.text(exception));
            return back;
        }
    }

    /** Where VNPay sends the customer's browser after paying. The signature, not the session, is what is trusted. */
    @GetMapping("/payment/vnpay/return")
    public String back(HttpServletRequest request, RedirectAttributes flash) {
        Optional<Map<String, String>> params = vnpay.verify(flat(request));
        if (params.isEmpty()) {
            flash.addFlashAttribute("error", "Không xác minh được kết quả thanh toán từ VNPay.");
            return "redirect:/user/shipments";
        }
        long shipmentId = VnpayService.shipmentIdOf(params.get().get("vnp_TxnRef")).orElse(-1);
        GatewayPaymentResult result = settle(params.get());
        if (result == GatewayPaymentResult.CONFIRMED || result == GatewayPaymentResult.ALREADY_CONFIRMED) {
            flash.addFlashAttribute("notice", "Thanh toán VNPay thành công. Đơn hàng đã chuyển sang chờ bàn giao.");
        } else {
            flash.addFlashAttribute("error", "Thanh toán VNPay chưa thành công (mã " + params.get().getOrDefault("vnp_ResponseCode", "?") + "). Bạn có thể thử lại.");
        }
        return shipmentId > 0 ? "redirect:/user/shipments/" + shipmentId : "redirect:/user/shipments";
    }

    /** Server-to-server notification; the reply codes are the ones VNPay documents. */
    @GetMapping(value = "/payment/vnpay/ipn", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, String> ipn(HttpServletRequest request) {
        Optional<Map<String, String>> params = vnpay.verify(flat(request));
        if (params.isEmpty()) return reply("97", "Invalid signature");
        GatewayPaymentResult result = settle(params.get());
        if (result == null) return reply("00", "Confirm Success"); // a failed payment is acknowledged, nothing to confirm
        return switch (result) {
            case CONFIRMED -> reply("00", "Confirm Success");
            case ALREADY_CONFIRMED, NOT_PAYABLE -> reply("02", "Order already confirmed");
            case AMOUNT_MISMATCH -> reply("04", "Invalid amount");
            case NOT_FOUND -> reply("01", "Order not found");
        };
    }

    /** Confirms the shipment only for a successful, matching payment; null when VNPay reports the payment did not succeed. */
    private GatewayPaymentResult settle(Map<String, String> p) {
        var shipmentId = VnpayService.shipmentIdOf(p.get("vnp_TxnRef"));
        if (shipmentId.isEmpty()) return GatewayPaymentResult.NOT_FOUND;
        if (!"00".equals(p.get("vnp_ResponseCode")) || !"00".equals(p.getOrDefault("vnp_TransactionStatus", "00"))) return null;
        BigDecimal paid;
        try {
            paid = new BigDecimal(p.getOrDefault("vnp_Amount", "")).movePointLeft(2);
        } catch (NumberFormatException exception) {
            return GatewayPaymentResult.AMOUNT_MISMATCH;
        }
        return shipments.confirmGatewayPayment(shipmentId.getAsLong(), p.get("vnp_TransactionNo"), paid);
    }

    private static Map<String, String> reply(String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }

    private static Map<String, String> flat(HttpServletRequest request) {
        Map<String, String> flat = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> flat.put(k, v.length > 0 ? v[0] : ""));
        return flat;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
