package vn.iotstar.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;
    @Column(name = "payer_type", nullable = false) private String payerType;
    @Column(name = "payment_type", nullable = false) private String paymentType;
    @Column(nullable = false) private BigDecimal amount;
    @Column(nullable = false) private String method;
    @Column(nullable = false) private String status;
    @Column(name = "transaction_ref") private String transactionRef;
    @Column(name = "paid_at", columnDefinition = "DATETIME2(0)") private LocalDateTime paidAt;
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME2(0)") private LocalDateTime createdAt;

    protected Payment() { }

    public static Payment shippingFee(Shipment shipment, String payerType, BigDecimal amount, String method, String status) {
        Payment p = new Payment();
        p.shipment = shipment; p.payerType = payerType; p.paymentType = "SHIPPING_FEE";
        p.amount = amount; p.method = method; p.status = status;
        return p;
    }

    @PrePersist
    void assignCreatedAt() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    /** Idempotent: a second confirmation keeps the first reference and time. */
    public void markPaid(String reference) {
        if ("PAID".equals(status)) return;
        status = "PAID"; transactionRef = reference; paidAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getPayerType() { return payerType; }
    public BigDecimal getAmount() { return amount; }
    public String getMethod() { return method; }
    public String getStatus() { return status; }
}
