package vn.iotstar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "transaction_type", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String transactionType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "reference_type", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String referenceType;

    @Column(name = "description", length = 255, columnDefinition = "NVARCHAR(255)")
    private String description;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;

    protected WalletTransaction() {
    }

    public static WalletTransaction create(Long userId, String transactionType, BigDecimal amount,
                                           BigDecimal balanceBefore, BigDecimal balanceAfter,
                                           Long referenceId, String referenceType, String description) {
        WalletTransaction tx = new WalletTransaction();
        tx.userId = userId;
        tx.transactionType = transactionType;
        tx.amount = amount;
        tx.balanceBefore = balanceBefore;
        tx.balanceAfter = balanceAfter;
        tx.referenceId = referenceId;
        tx.referenceType = referenceType;
        tx.description = description;
        tx.createdAt = LocalDateTime.now();
        return tx;
    }

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTransactionType() { return transactionType; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public Long getReferenceId() { return referenceId; }
    public String getReferenceType() { return referenceType; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

