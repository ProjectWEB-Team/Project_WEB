package vn.iotstar.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Pending refund obligation handed to Phase 04; Phase 02 never credits wallets or marks payments REFUNDED. */
@Entity
@Table(name = "refund_settlement_outbox")
public class RefundSettlementOutbox {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "shipment_id", nullable = false) private Long shipmentId;
    @Column(name = "payment_id", nullable = false, unique = true) private Long paymentId;
    @Column(name = "sender_user_id", nullable = false) private Long senderUserId;
    @Column(nullable = false) private BigDecimal amount;
    @Column(name = "cancel_reason", nullable = false) private String cancelReason;
    @Column(nullable = false) private String status;
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME2(0)") private LocalDateTime createdAt;
    @Column(name = "settled_at", columnDefinition = "DATETIME2(0)") private LocalDateTime settledAt;

    protected RefundSettlementOutbox() { }

    public static RefundSettlementOutbox pending(Shipment shipment, Payment payment, String reason, LocalDateTime at) {
        RefundSettlementOutbox o = new RefundSettlementOutbox();
        o.shipmentId = shipment.getId(); o.paymentId = payment.getId(); o.senderUserId = shipment.getSender().getId();
        o.amount = payment.getAmount(); o.cancelReason = reason; o.status = "PENDING_WALLET_CREDIT"; o.createdAt = at;
        return o;
    }

    public Long getId() { return id; }
    public Long getShipmentId() { return shipmentId; }
    public Long getSenderUserId() { return senderUserId; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void settle(LocalDateTime at) {
        if ("SETTLED".equals(status)) return;
        if (!"PENDING_WALLET_CREDIT".equals(status)) throw new IllegalStateException("Refund obligation is not pending");
        status = "SETTLED";
        settledAt = at;
    }
    public Long getPaymentId() { return paymentId; }
    public BigDecimal getAmount() { return amount; }
    public String getCancelReason() { return cancelReason; }
    public String getStatus() { return status; }
}
