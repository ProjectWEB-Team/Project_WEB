package vn.iotstar.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** VNPay payment gateway v2.1.0: signed pay URL out, signed result in. Works against the sandbox with test keys. */
@Service
public class VnpayService {
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final Pattern TXN_REF = Pattern.compile("S(\\d+)-\\d+");
    private final String tmnCode;
    private final String secret;
    private final String payUrl;
    private final String returnUrl;
    private final Clock clock;

    public VnpayService(@Value("${uteexpress.vnpay.tmn-code:}") String tmnCode,
                        @Value("${uteexpress.vnpay.hash-secret:}") String secret,
                        @Value("${uteexpress.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") String payUrl,
                        @Value("${uteexpress.vnpay.return-url:}") String returnUrl, Clock clock) {
        this.tmnCode = tmnCode; this.secret = secret; this.payUrl = payUrl; this.returnUrl = returnUrl; this.clock = clock;
    }

    public boolean enabled() { return !tmnCode.isBlank() && !secret.isBlank(); }

    /** @param fallbackReturnUrl used when no return URL is configured (local testing) */
    public String paymentUrl(Long shipmentId, BigDecimal amountVnd, String ip, String fallbackReturnUrl) {
        Instant now = clock.instant();
        Map<String, String> p = new TreeMap<>();
        p.put("vnp_Version", "2.1.0");
        p.put("vnp_Command", "pay");
        p.put("vnp_TmnCode", tmnCode);
        p.put("vnp_Amount", amountVnd.setScale(0, RoundingMode.HALF_UP).movePointRight(2).toPlainString());
        p.put("vnp_CurrCode", "VND");
        p.put("vnp_TxnRef", "S" + shipmentId + "-" + now.getEpochSecond());
        p.put("vnp_OrderInfo", "Thanh toan cuoc van don " + shipmentId);
        p.put("vnp_OrderType", "other");
        p.put("vnp_Locale", "vn");
        p.put("vnp_ReturnUrl", returnUrl.isBlank() ? fallbackReturnUrl : returnUrl);
        p.put("vnp_IpAddr", ip);
        p.put("vnp_CreateDate", STAMP.format(now));
        p.put("vnp_ExpireDate", STAMP.format(now.plus(15, ChronoUnit.MINUTES)));
        String query = encode(p);
        return payUrl + "?" + query + "&vnp_SecureHash=" + hmac(query);
    }

    /** The vnp_* parameters when the signature is genuine, empty when it is missing or wrong. */
    public Optional<Map<String, String>> verify(Map<String, String> received) {
        Map<String, String> p = new TreeMap<>();
        received.forEach((k, v) -> {
            if (k.startsWith("vnp_") && !k.equals("vnp_SecureHash") && !k.equals("vnp_SecureHashType") && v != null && !v.isEmpty()) p.put(k, v);
        });
        String given = received.get("vnp_SecureHash");
        if (given == null || !enabled()) return Optional.empty();
        boolean same = MessageDigest.isEqual(hmac(encode(p)).getBytes(StandardCharsets.US_ASCII),
                given.toLowerCase().getBytes(StandardCharsets.US_ASCII));
        return same ? Optional.of(p) : Optional.empty();
    }

    public static OptionalLong shipmentIdOf(String txnRef) {
        Matcher m = txnRef == null ? null : TXN_REF.matcher(txnRef);
        return m != null && m.matches() ? OptionalLong.of(Long.parseLong(m.group(1))) : OptionalLong.empty();
    }

    private static String encode(Map<String, String> sorted) {
        return sorted.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII))
                .collect(Collectors.joining("&"));
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA512 unavailable", exception);
        }
    }
}
