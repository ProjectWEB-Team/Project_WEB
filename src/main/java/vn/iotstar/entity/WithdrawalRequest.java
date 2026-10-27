package vn.iotstar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawal_requests")
public class WithdrawalRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version @Column(nullable = false) private Long version;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "bank_name", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String bankName;

    @Column(name = "account_number", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String accountNumber;

    @Column(name = "account_holder_name", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String accountHolderName;

    @Column(name = "status", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, COMPLETED

    @Column(name = "rejection_reason", length = 255, columnDefinition = "NVARCHAR(255)")
    private String rejectionReason;

    @Column(name = "request_key", length = 100, columnDefinition = "NVARCHAR(100)")
    private String requestKey;

    @Column(name = "requested_at", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime requestedAt;

    @Column(name = "approved_at", columnDefinition = "DATETIME2(0)")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at", columnDefinition = "DATETIME2(0)")
    private LocalDateTime rejectedAt;

    @Column(name = "completed_at", columnDefinition = "DATETIME2(0)")
    private LocalDateTime completedAt;

    protected WithdrawalRequest() {
    }

    public static WithdrawalRequest create(Long userId, BigDecimal amount, String bankName,
                                           String accountNumber, String accountHolderName,
                                           String requestKey) {
        WithdrawalRequest request = new WithdrawalRequest();
        request.userId = userId;
        request.amount = amount;
        request.bankName = bankName;
        request.accountNumber = accountNumber;
        request.accountHolderName = accountHolderName;
        request.requestKey = requestKey;
        request.status = "PENDING";
        request.requestedAt = LocalDateTime.now();
        return request;
    }

    @PrePersist
    void onPrePersist() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public String getBankName() { return bankName; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolderName() { return accountHolderName; }
    public String getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public String getRequestKey() { return requestKey; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }

    public void setId(Long id) { this.id = id; }
    public void setStatus(String status) { this.status = status; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}

