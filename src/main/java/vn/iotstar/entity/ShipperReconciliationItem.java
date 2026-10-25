package vn.iotstar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "shipper_reconciliation_items")
public class ShipperReconciliationItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reconciliation_id", nullable = false)
    private ShipperReconciliation reconciliation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "source_type", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String sourceType; // COD_TRANSACTION, RECEIVER_CASH_SHIPPING_FEE_PAYMENT

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "expected_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal expectedAmount = BigDecimal.ZERO;

    @Column(name = "actual_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal actualAmount = BigDecimal.ZERO;

    @Column(name = "difference_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal differenceAmount = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String status; // MATCHED, SHORTAGE, OVERAGE

    @Column(name = "note", length = 255, columnDefinition = "NVARCHAR(255)")
    private String note;

    protected ShipperReconciliationItem() {
    }

    public static ShipperReconciliationItem create(ShipperReconciliation reconciliation, Shipment shipment,
                                                   String sourceType, Long sourceId,
                                                   BigDecimal expectedAmount, BigDecimal actualAmount,
                                                   String note) {
        ShipperReconciliationItem item = new ShipperReconciliationItem();
        item.reconciliation = reconciliation;
        item.shipment = shipment;
        item.sourceType = sourceType;
        item.sourceId = sourceId;
        item.expectedAmount = expectedAmount != null ? expectedAmount : BigDecimal.ZERO;
        item.actualAmount = actualAmount != null ? actualAmount : BigDecimal.ZERO;
        item.differenceAmount = item.actualAmount.subtract(item.expectedAmount);
        if (item.differenceAmount.compareTo(BigDecimal.ZERO) == 0) {
            item.status = "MATCHED";
        } else if (item.differenceAmount.compareTo(BigDecimal.ZERO) < 0) {
            item.status = "SHORTAGE";
        } else {
            item.status = "OVERAGE";
        }
        item.note = note;
        return item;
    }

    public Long getId() { return id; }
    public ShipperReconciliation getReconciliation() { return reconciliation; }
    public Shipment getShipment() { return shipment; }
    public String getSourceType() { return sourceType; }
    public Long getSourceId() { return sourceId; }
    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public BigDecimal getActualAmount() { return actualAmount; }
    public BigDecimal getDifferenceAmount() { return differenceAmount; }
    public String getStatus() { return status; }
    public String getNote() { return note; }
}

