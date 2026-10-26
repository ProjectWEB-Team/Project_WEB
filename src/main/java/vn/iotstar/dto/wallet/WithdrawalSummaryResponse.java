package vn.iotstar.dto.wallet;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Read model for the Administrator withdrawal review workspace. */
public class WithdrawalSummaryResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String username;
    private BigDecimal amount;
    private String bankName;
    private String accountNumber;
    private String accountHolderName;
    private String status;
    private String rejectionReason;
    private LocalDateTime requestedAt;

    public WithdrawalSummaryResponse() {
    }

    public WithdrawalSummaryResponse(Long id, Long userId, String username, BigDecimal amount, String bankName,
                                      String accountNumber, String accountHolderName, String status,
                                      String rejectionReason, LocalDateTime requestedAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.amount = amount;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.requestedAt = requestedAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public BigDecimal getAmount() { return amount; }
    public String getBankName() { return bankName; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolderName() { return accountHolderName; }
    public String getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
}