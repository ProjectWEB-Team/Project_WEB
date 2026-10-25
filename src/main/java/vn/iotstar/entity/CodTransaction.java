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
import java.time.LocalDateTime;

@Entity
@Table(name = "cod_transactions")
public class CodTransaction implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collected_by_shipper_id", nullable = false)
    private User collectedByShipper;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "method", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String method;

    @Column(name = "status", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String status;

    @Column(name = "collected_at", columnDefinition = "DATETIME2(0)")
    private LocalDateTime collectedAt;

    @Column(name = "reconciled_at", columnDefinition = "DATETIME2(0)")
    private LocalDateTime reconciledAt;

    @Column(name = "payout_at", columnDefinition = "DATETIME2(0)")
    private LocalDateTime payoutAt;

    protected CodTransaction() {
    }

    public static CodTransaction createCollected(Shipment shipment, User shipper, BigDecimal amount, String method) {
        CodTransaction tx = new CodTransaction();
        tx.shipment = shipment;
        tx.collectedByShipper = shipper;
        tx.amount = amount;
        tx.method = method != null ? method : "CASH";
        tx.status = "COLLECTED";
        tx.collectedAt = LocalDateTime.now();
        return tx;
    }

    public static CodTransaction createWaiting(Shipment shipment, User shipper, BigDecimal amount, String method) {
        CodTransaction tx = new CodTransaction();
        tx.shipment = shipment;
        tx.collectedByShipper = shipper;
        tx.amount = amount;
        tx.method = method != null ? method : "CASH";
        tx.status = "WAITING_COLLECTION";
        return tx;
    }

    public Long getId() { return id; }
    public Shipment getShipment() { return shipment; }
    public User getCollectedByShipper() { return collectedByShipper; }
    public BigDecimal getAmount() { return amount; }
    public String getMethod() { return method; }
    public String getStatus() { return status; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
    public LocalDateTime getReconciledAt() { return reconciledAt; }
    public LocalDateTime getPayoutAt() { return payoutAt; }

    public void setStatus(String status) { this.status = status; }
    public void collect(String method, User collector) {
        if (method == null || !java.util.Set.of("CASH", "VNPAY", "TRANSFER").contains(method)) throw new IllegalArgumentException("Unsupported COD method");
        if (!"WAITING_COLLECTION".equals(status)) throw new IllegalStateException("COD is not awaiting collection");
        this.method = method;
        this.collectedByShipper = collector;
        this.status = "COLLECTED";
        this.collectedAt = LocalDateTime.now();
    }
    public void assignWaitingCollector(User shipper) {
        if (!"WAITING_COLLECTION".equals(status)) throw new IllegalStateException("Collected COD is immutable");
        this.collectedByShipper = shipper;
    }
    public void setCollectedAt(LocalDateTime collectedAt) { this.collectedAt = collectedAt; }
    public void setReconciledAt(LocalDateTime reconciledAt) { this.reconciledAt = reconciledAt; }
    public void setPayoutAt(LocalDateTime payoutAt) { this.payoutAt = payoutAt; }
}

