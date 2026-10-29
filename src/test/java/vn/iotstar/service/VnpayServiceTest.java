package vn.iotstar.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VnpayServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC);
    private final VnpayService vnpay = new VnpayService("TESTCODE", "TESTSECRET", "https://sandbox.example/pay", "https://shop.example/return", CLOCK);

    private static Map<String, String> query(String url) {
        Map<String, String> map = new HashMap<>();
        for (String pair : URI.create(url).getRawQuery().split("&")) {
            int i = pair.indexOf('=');
            map.put(pair.substring(0, i), URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
        }
        return map;
    }

    @Test
    void paymentUrlCarriesTheAmountInHundredthsAndAValidSignature() {
        String url = vnpay.paymentUrl(42L, new BigDecimal("45000.00"), "10.0.0.5", "http://fallback");
        Map<String, String> p = query(url);

        assertThat(url).startsWith("https://sandbox.example/pay?");
        assertThat(p).containsEntry("vnp_TmnCode", "TESTCODE").containsEntry("vnp_Amount", "4500000")
                .containsEntry("vnp_CurrCode", "VND").containsEntry("vnp_ReturnUrl", "https://shop.example/return")
                .containsEntry("vnp_CreateDate", "20261029170000"); // Vietnam time, UTC+7
        assertThat(p.get("vnp_TxnRef")).matches("S42-\\d+");
        assertThat(vnpay.verify(p)).isPresent();
    }

    @Test
    void tamperedAmountOrWrongKeyIsRejected() {
        Map<String, String> p = query(vnpay.paymentUrl(42L, new BigDecimal("45000"), "10.0.0.5", "http://fallback"));
        Map<String, String> cheaper = new HashMap<>(p);
        cheaper.put("vnp_Amount", "100");
        VnpayService other = new VnpayService("TESTCODE", "OTHERSECRET", "https://sandbox.example/pay", "", CLOCK);

        assertThat(vnpay.verify(cheaper)).isEmpty();
        assertThat(other.verify(p)).isEmpty();
        p.remove("vnp_SecureHash");
        assertThat(vnpay.verify(p)).isEmpty();
    }

    @Test
    void withoutKeysTheGatewayIsOffAndNothingVerifies() {
        VnpayService off = new VnpayService("", "", "https://sandbox.example/pay", "", CLOCK);
        assertThat(off.enabled()).isFalse();
        assertThat(off.verify(Map.of("vnp_SecureHash", "abc", "vnp_Amount", "1"))).isEmpty();
    }

    @Test
    void fallbackReturnUrlIsUsedOnlyWhenNoneIsConfigured() {
        VnpayService local = new VnpayService("T", "S", "https://sandbox.example/pay", "", CLOCK);
        assertThat(query(local.paymentUrl(1L, BigDecimal.TEN, "1.1.1.1", "http://localhost:8080/payment/vnpay/return")))
                .containsEntry("vnp_ReturnUrl", "http://localhost:8080/payment/vnpay/return");
    }

    @Test
    void shipmentIdIsReadBackFromTheTransactionReference() {
        assertThat(VnpayService.shipmentIdOf("S42-1761732000")).hasValue(42);
        assertThat(VnpayService.shipmentIdOf("42")).isEmpty();
        assertThat(VnpayService.shipmentIdOf(null)).isEmpty();
    }
}
