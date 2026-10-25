package vn.iotstar.dto.finance;
import java.math.BigDecimal;
public record ReconciliationItemResponse(Long shipmentId, String trackingNumber, String sourceType, Long sourceId,
        BigDecimal expectedAmount, BigDecimal actualAmount, BigDecimal differenceAmount, String status) { }
