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
@Table(name = "wallets")
public class Wallet implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version @Column(nullable = false) private Long version;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "locked_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;

    protected Wallet() {
    }

    public static Wallet createForUser(Long userId) {
        Wallet wallet = new Wallet();
        wallet.userId = userId;
        wallet.balance = BigDecimal.ZERO;
        wallet.lockedBalance = BigDecimal.ZERO;
        wallet.status = "ACTIVE";
        wallet.createdAt = LocalDateTime.now();
        return wallet;
    }

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        if (lockedBalance == null) {
            lockedBalance = BigDecimal.ZERO;
        }
    }

    public void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance to pay from wallet");
        }
        this.balance = this.balance.subtract(amount);
    }

    public void lockFunds(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Lock amount must be positive");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance to lock funds");
        }
        this.balance = this.balance.subtract(amount);
        this.lockedBalance = this.lockedBalance.add(amount);
    }

    public void deductLocked(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deduct amount must be positive");
        }
        if (this.lockedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient locked balance to deduct");
        }
        this.lockedBalance = this.lockedBalance.subtract(amount);
    }

    public void refundLocked(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }
        if (this.lockedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient locked balance to refund");
        }
        this.lockedBalance = this.lockedBalance.subtract(amount);
        this.balance = this.balance.add(amount);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getLockedBalance() { return lockedBalance; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setStatus(String status) { this.status = status; }
}

