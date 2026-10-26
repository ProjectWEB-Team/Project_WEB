package vn.iotstar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipper_reconciliations")
public class ShipperReconciliation implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipper_id", nullable = false)
    private User shipper;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    @Column(name = "reconciliation_date", nullable = false)
    private LocalDate reconciliationDate;
    @Column(name="hub_id") private Long hubId;
    @Column(name="collection_method", length=20) private String collectionMethod;
    @Column(name="settlement_evidence", length=255, columnDefinition="NVARCHAR(255)") private String settlementEvidence;

    @Column(name = "expected_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal expectedAmount = BigDecimal.ZERO;

    @Column(name = "actual_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal actualAmount = BigDecimal.ZERO;

    @Column(name = "difference_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal differenceAmount = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String status = "PENDING"; // PENDING, COMPLETED, COMPLETED_WITH_DISCREPANCY

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;

    @Column(name = "completed_at", columnDefinition = "DATETIME2(0)")
    private LocalDateTime completedAt;

    protected ShipperReconciliation() {
    }

    public static ShipperReconciliation create(User shipper, User manager, LocalDate reconciliationDate,
                                               BigDecimal expectedAmount, BigDecimal actualAmount) {
        ShipperReconciliation rec = new ShipperReconciliation();
        rec.shipper = shipper;
        rec.manager = manager;
        rec.reconciliationDate = reconciliationDate;
        rec.expectedAmount = expectedAmount != null ? expectedAmount : BigDecimal.ZERO;
        rec.actualAmount = actualAmount != null ? actualAmount : BigDecimal.ZERO;
        rec.differenceAmount = rec.actualAmount.subtract(rec.expectedAmount);
        // The shift stays PENDING until every item has been judged, because the
        // final status depends on item level differences rather than on totals.
        rec.status = "PENDING";
        rec.createdAt = LocalDateTime.now();
        return rec;
    }

    /**
     * Finalises the shift. The Workflow makes the status item driven: a shift
     * with one shortage and one compensating overage is still a discrepancy.
     */
    public void complete(boolean hasDiscrepancy) {
        this.status = hasDiscrepancy ? "COMPLETED_WITH_DISCREPANCY" : "COMPLETED";
        this.completedAt = LocalDateTime.now();
    }

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Long getHubId() { return hubId; }
    public String getCollectionMethod() { return collectionMethod; }
    public String getSettlementEvidence() { return settlementEvidence; }
    public void recordSettlement(String method, String evidence) {
        if (collectionMethod!=null || method==null || !java.util.Set.of("CASH","VNPAY","TRANSFER").contains(method)
                || (evidence!=null && evidence.length()>255) || (!"CASH".equals(method) && (evidence==null || evidence.isBlank())))
            throw new IllegalArgumentException("Method and verified settlement evidence are required");
        collectionMethod=method;settlementEvidence=evidence==null ? null : evidence.trim();
    }
    public void assignHub(Long hubId) {
        if (this.hubId!=null || hubId==null) throw new IllegalStateException("Reconciliation hub must be set once");
        this.hubId=hubId;
    }
    public User getShipper() { return shipper; }
    public User getManager() { return manager; }
    public LocalDate getReconciliationDate() { return reconciliationDate; }
    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public BigDecimal getActualAmount() { return actualAmount; }
    public BigDecimal getDifferenceAmount() { return differenceAmount; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }

    public void setStatus(String status) { this.status = status; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}

