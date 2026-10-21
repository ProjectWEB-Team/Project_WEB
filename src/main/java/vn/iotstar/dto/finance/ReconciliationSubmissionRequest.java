package vn.iotstar.dto.finance;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReconciliationSubmissionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Shipper ID is required")
    private Long shipperId;

    private LocalDate reconciliationDate;

    private List<ItemRequest> items = new ArrayList<>();

    public ReconciliationSubmissionRequest() {
    }

    public ReconciliationSubmissionRequest(Long shipperId, LocalDate reconciliationDate, List<ItemRequest> items) {
        this.shipperId = shipperId;
        this.reconciliationDate = reconciliationDate;
        this.items = items;
    }

    public static class ItemRequest implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long shipmentId;
        private String sourceType; // COD_TRANSACTION, RECEIVER_CASH_SHIPPING_FEE_PAYMENT
        private Long sourceId;
        private BigDecimal expectedAmount;
        private BigDecimal actualAmount;
        private String note;

        public ItemRequest() {
        }

        public ItemRequest(Long shipmentId, String sourceType, Long sourceId,
                           BigDecimal expectedAmount, BigDecimal actualAmount, String note) {
            this.shipmentId = shipmentId;
            this.sourceType = sourceType;
            this.sourceId = sourceId;
            this.expectedAmount = expectedAmount;
            this.actualAmount = actualAmount;
            this.note = note;
        }

        public Long getShipmentId() { return shipmentId; }
        public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }
        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }
        public Long getSourceId() { return sourceId; }
        public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
        public BigDecimal getExpectedAmount() { return expectedAmount; }
        public void setExpectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; }
        public BigDecimal getActualAmount() { return actualAmount; }
        public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    public Long getShipperId() { return shipperId; }
    public void setShipperId(Long shipperId) { this.shipperId = shipperId; }
    public LocalDate getReconciliationDate() { return reconciliationDate; }
    public void setReconciliationDate(LocalDate reconciliationDate) { this.reconciliationDate = reconciliationDate; }
    public List<ItemRequest> getItems() { return items; }
    public void setItems(List<ItemRequest> items) { this.items = items; }
}

