package vn.iotstar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** Minimal Phase-01 shipment record; extended for Phase-04 operational and financial fields. */
@Entity
@Table(name = "shipments")
public class Shipment implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_number", nullable = false, unique = true, length = 50, columnDefinition = "NVARCHAR(50)")
    private String trackingNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User sender;

    @Column(name = "sender_name", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String senderName;

    @Column(name = "sender_phone", nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private String senderPhone;

    @Column(name = "sender_address", nullable = false, length = 255, columnDefinition = "NVARCHAR(255)")
    private String senderAddress;

    @Column(name = "receiver_name", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private String receiverPhone;

    @Column(name = "receiver_address", nullable = false, length = 255, columnDefinition = "NVARCHAR(255)")
    private String receiverAddress;

    /* Required schema fields held to fixed, non-workflow-driving Phase-01 defaults. */
    @Column(name = "pickup_method", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String pickupMethod;

    @Column(name = "fee_payer", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String feePayer;

    @Column(name = "inspection_policy", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String inspectionPolicy;

    @Column(name = "payment_status", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private ShipmentStatus currentStatus;

    @Column(name = "current_hub_id")
    private Long currentHubId;

    @Column(name = "origin_hub_id")
    private Long originHubId;

    @Column(name = "destination_hub_id")
    private Long destinationHubId;

    @Column(name = "direction", length = 20, columnDefinition = "NVARCHAR(20)")
    private String direction = "FORWARD";

    @Column(name = "shipping_fee", precision = 15, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "cod_amount", precision = 15, scale = 2)
    private BigDecimal codAmount;

    @Column(name = "return_fee", precision = 15, scale = 2)
    private BigDecimal returnFee;

    @Column(name = "declared_value", precision = 15, scale = 2)
    private BigDecimal declaredValue;

    @Column(name = "fail_reason", length = 50, columnDefinition = "NVARCHAR(50)")
    private String failReason;

    @Column(name = "cancel_reason", length = 255, columnDefinition = "NVARCHAR(255)")
    private String cancelReason;

    @Column(name = "closure_reason", length = 50, columnDefinition = "NVARCHAR(50)")
    private String closureReason;

    @Column(name = "on_hold", nullable = false)
    private boolean onHold = false;

    @Column(name = "delivered_at", columnDefinition = "DATETIME2(0)")
    private LocalDateTime deliveredAt;

    @Column(name = "returned_at", columnDefinition = "DATETIME2(0)")
    private LocalDateTime returnedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;

    protected Shipment() {
    }

    public static Shipment create(String trackingNumber, User sender, String senderName, String senderPhone,
                                  String senderAddress, String receiverName, String receiverPhone,
                                  String receiverAddress) {
        Shipment shipment = new Shipment();
        shipment.trackingNumber = trackingNumber;
        shipment.sender = sender;
        shipment.senderName = senderName;
        shipment.senderPhone = senderPhone;
        shipment.senderAddress = senderAddress;
        shipment.receiverName = receiverName;
        shipment.receiverPhone = receiverPhone;
        shipment.receiverAddress = receiverAddress;
        shipment.pickupMethod = "SELF_DROPOFF";
        shipment.feePayer = "SENDER";
        shipment.inspectionPolicy = "NO_INSPECTION";
        shipment.paymentStatus = "UNPAID";
        shipment.currentStatus = ShipmentStatus.CREATED;
        return shipment;
    }

    @PrePersist
    void assignCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getTrackingNumber() { return trackingNumber; }
    public User getSender() { return sender; }
    public String getSenderName() { return senderName; }
    public String getSenderPhone() { return senderPhone; }
    public String getSenderAddress() { return senderAddress; }
    public String getReceiverName() { return receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public String getReceiverAddress() { return receiverAddress; }
    public String getPickupMethod() { return pickupMethod; }
    public String getFeePayer() { return feePayer; }
    public String getInspectionPolicy() { return inspectionPolicy; }
    public String getPaymentStatus() { return paymentStatus; }
    public ShipmentStatus getCurrentStatus() { return currentStatus; }
    public Long getCurrentHubId() { return currentHubId; }
    public Long getOriginHubId() { return originHubId; }
    public Long getDestinationHubId() { return destinationHubId; }
    public String getDirection() { return direction; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public BigDecimal getCodAmount() { return codAmount; }
    public BigDecimal getReturnFee() { return returnFee; }
    public BigDecimal getDeclaredValue() { return declaredValue; }
    public String getFailReason() { return failReason; }
    public String getCancelReason() { return cancelReason; }
    public String getClosureReason() { return closureReason; }
    public boolean isOnHold() { return onHold; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void transitionTo(ShipmentStatus nextStatus) { this.currentStatus = nextStatus; }
    public void setCurrentHubId(Long currentHubId) { this.currentHubId = currentHubId; }
    public void setOriginHubId(Long originHubId) { this.originHubId = originHubId; }
    public void setDestinationHubId(Long destinationHubId) { this.destinationHubId = destinationHubId; }
    public void setDirection(String direction) { this.direction = direction; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }
    public void setCodAmount(BigDecimal codAmount) { this.codAmount = codAmount; }
    public void setReturnFee(BigDecimal returnFee) { this.returnFee = returnFee; }
    public void setDeclaredValue(BigDecimal declaredValue) { this.declaredValue = declaredValue; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public void setClosureReason(String closureReason) { this.closureReason = closureReason; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }
    public void setOnHold(boolean onHold) { this.onHold = onHold; }
    public void setFeePayer(String feePayer) { this.feePayer = feePayer; }
}
