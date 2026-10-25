package vn.iotstar.dto.finance;
import java.math.BigDecimal;
import java.time.LocalDate;
public record ReconciliationSummaryResponse(Long id, LocalDate date, BigDecimal expectedAmount,
        BigDecimal actualAmount, BigDecimal differenceAmount, String status, String collectionMethod) { }
