package vn.iotstar.dto.wallet;

import java.io.Serializable;
import java.math.BigDecimal;

public class WalletResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private BigDecimal balance;
    private BigDecimal lockedBalance;
    private BigDecimal availableBalance;
    private String status;

    public WalletResponse() {
    }

    public WalletResponse(Long userId, BigDecimal balance, BigDecimal lockedBalance, String status) {
        this.userId = userId;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.lockedBalance = lockedBalance != null ? lockedBalance : BigDecimal.ZERO;
        this.availableBalance = this.balance;
        this.status = status;
    }

    public Long getUserId() { return userId; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getLockedBalance() { return lockedBalance; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public String getStatus() { return status; }
}

