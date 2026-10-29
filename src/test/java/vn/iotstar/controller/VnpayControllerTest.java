package vn.iotstar.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.dto.shipment.GatewayPaymentResult;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.ShipmentService;
import vn.iotstar.service.VnpayService;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VnpayController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, VnpayControllerTest.Config.class})
@TestPropertySource(properties = {"uteexpress.vnpay.tmn-code=TESTCODE", "uteexpress.vnpay.hash-secret=TESTSECRET"})
class VnpayControllerTest {
    @TestConfiguration
    static class Config {
        @Bean Clock clock() { return Clock.systemUTC(); }
        @Bean VnpayService vnpay(Clock clock) { return new VnpayService("TESTCODE", "TESTSECRET", "https://sandbox.example/pay", "", clock); }
    }

    @Autowired MockMvc mvc;
    @Autowired VnpayService vnpay;
    @MockBean ShipmentService shipments;
    @MockBean UserDetailsService userDetailsService;
    @MockBean JwtService jwt;

    /** What VNPay would send back: the parameters of a pay URL we signed, with a result code added and re-signed. */
    private Map<String, String> signedResult(long shipmentId, String amountVnd, String responseCode) throws Exception {
        VnpayService signer = new VnpayService("TESTCODE", "TESTSECRET", "https://sandbox.example/pay", "http://x", Clock.systemUTC());
        Map<String, String> p = new HashMap<>();
        p.put("vnp_TmnCode", "TESTCODE");
        p.put("vnp_Amount", new BigDecimal(amountVnd).movePointRight(2).toPlainString());
        p.put("vnp_TxnRef", "S" + shipmentId + "-1761732000");
        p.put("vnp_TransactionNo", "14567890");
        p.put("vnp_ResponseCode", responseCode);
        p.put("vnp_TransactionStatus", responseCode);
        // sign the way the gateway does: sorted, URL-encoded, HMAC-SHA512 with the shared secret
        String url = signer.paymentUrl(shipmentId, BigDecimal.ONE, "1.1.1.1", "http://x");
        Map<String, String> sample = new HashMap<>();
        for (String pair : URI.create(url).getRawQuery().split("&")) {
            int i = pair.indexOf('=');
            sample.put(pair.substring(0, i), URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
        }
        p.putAll(resign(p));
        return p;
    }

    private Map<String, String> resign(Map<String, String> p) throws Exception {
        java.util.TreeMap<String, String> sorted = new java.util.TreeMap<>(p);
        String data = sorted.entrySet().stream()
                .map(e -> e.getKey() + "=" + java.net.URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII))
                .collect(java.util.stream.Collectors.joining("&"));
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
        mac.init(new javax.crypto.spec.SecretKeySpec("TESTSECRET".getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        return Map.of("vnp_SecureHash", java.util.HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8))));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder call(String path, Map<String, String> p) {
        var req = get(path);
        p.forEach(req::param);
        return req;
    }

    @Test
    void ipnConfirmsAGenuineSuccessfulPaymentWithoutALogin() throws Exception {
        when(shipments.confirmGatewayPayment(eq(42L), eq("14567890"), any())).thenReturn(GatewayPaymentResult.CONFIRMED);

        mvc.perform(call("/payment/vnpay/ipn", signedResult(42L, "45000", "00")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.RspCode").value("00"));
        verify(shipments).confirmGatewayPayment(42L, "14567890", new BigDecimal("45000.00"));
    }

    @Test
    void ipnRefusesAForgedSignatureAndConfirmsNothing() throws Exception {
        Map<String, String> forged = signedResult(42L, "45000", "00");
        forged.put("vnp_Amount", "100");

        mvc.perform(call("/payment/vnpay/ipn", forged)).andExpect(jsonPath("$.RspCode").value("97"));
        verify(shipments, never()).confirmGatewayPayment(any(), any(), any());
    }

    @Test
    void ipnAcknowledgesAFailedPaymentWithoutConfirmingIt() throws Exception {
        mvc.perform(call("/payment/vnpay/ipn", signedResult(42L, "45000", "24")))
                .andExpect(jsonPath("$.RspCode").value("00"));
        verify(shipments, never()).confirmGatewayPayment(any(), any(), any());
    }

    @Test
    void ipnReportsAMismatchedAmountWithVnpayCode04() throws Exception {
        when(shipments.confirmGatewayPayment(any(), any(), any())).thenReturn(GatewayPaymentResult.AMOUNT_MISMATCH);
        mvc.perform(call("/payment/vnpay/ipn", signedResult(42L, "45000", "00")))
                .andExpect(jsonPath("$.RspCode").value("04"));
    }

    @Test
    void returnPageSendsTheCustomerBackToTheShipment() throws Exception {
        when(shipments.confirmGatewayPayment(any(), any(), any())).thenReturn(GatewayPaymentResult.CONFIRMED);
        mvc.perform(call("/payment/vnpay/return", signedResult(42L, "45000", "00")))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/user/shipments/42"));
    }

    @Test
    void returnPageWithAForgedSignatureGoesToTheListAndConfirmsNothing() throws Exception {
        Map<String, String> forged = signedResult(42L, "45000", "00");
        forged.put("vnp_ResponseCode", "00x");
        mvc.perform(call("/payment/vnpay/return", forged)).andExpect(redirectedUrl("/user/shipments"));
        verify(shipments, never()).confirmGatewayPayment(any(), any(), any());
    }
}
