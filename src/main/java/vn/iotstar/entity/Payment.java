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
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "payer_type", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String payerType; // SENDER, RECEIVER

    @Column(name = "payment_type", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String paymentType; // SHIPPING_FEE, RETURN_FEE, WEIGHT_SURCHARGE

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "method", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String method; // CASH, VNPAY, E_WALLET

    @Column(name = "status", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String status; // UNPAID, PENDING, PAID, FAILED, REFUNDED

    @Column(name = "transaction_ref", length = 100, columnDefinition = "NVARCHAR(100)")
    private String transactionRef;

    @Column(name = "paid_at", columnDefinition = "DATETIME2(0)")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at", columnDefinition = "DATETIME2(0)")
    private LocalDateTime refundedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;

    protected Payment() {
    }

    public static Payment create(Shipment shipment, String payerType, String paymentType,
                                 BigDecimal amount, String method, String status) {
        Payment payment = new Payment();
        payment.shipment = shipment;
        payment.payerType = payerType;
        payment.paymentType = paymentType;
        payment.amount = amount;
        payment.method = method;
        payment.status = status;
        payment.createdAt = LocalDateTime.now();
        if ("PAID".equals(status)) {
            payment.paidAt = LocalDateTime.now();
        }
        return payment;
    }

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Shipment getShipment() { return shipment; }
    public String getPayerType() { return payerType; }
    public String getPaymentType() { return paymentType; }
    public BigDecimal getAmount() { return amount; }
    public String getMethod() { return method; }
    public String getStatus() { return status; }
    public String getTransactionRef() { return transactionRef; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setStatus(String status) { this.status = status; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
}

