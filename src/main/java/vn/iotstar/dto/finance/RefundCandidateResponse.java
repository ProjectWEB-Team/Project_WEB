package vn.iotstar.dto.finance;
import java.math.BigDecimal;
public record RefundCandidateResponse(Long paymentId, Long shipmentId, String trackingNumber, BigDecimal amount) { }
