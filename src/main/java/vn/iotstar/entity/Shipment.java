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

/** Shipment record; columns owned by later phases stay unmapped. */
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

    /** Set only by ReceiverLinkService after the receiver's phone is verified; snapshots never change. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_user_id")
    private User receiver;

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
    @Column(name = "receiver_province", length = 100, columnDefinition = "NVARCHAR(100)")
    private String receiverProvince;

    @Column(name = "receiver_district", length = 100, columnDefinition = "NVARCHAR(100)")
    private String receiverDistrict;

    @Column(name = "weight") private Integer weight;
    @Column(name = "length") private Integer length;
    @Column(name = "width") private Integer width;
    @Column(name = "height") private Integer height;
    @Column(name = "declared_value") private BigDecimal declaredValue;
    @Column(name = "shipping_fee") private BigDecimal shippingFee;
    @Column(name = "cod_amount") private BigDecimal codAmount;

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

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;

    @Column(name = "waiting_for_handover_at", columnDefinition = "DATETIME2(0)")
    private LocalDateTime waitingForHandoverAt;

    /* First-mile: hub ids stay plain columns until the shared Phase-03 Hub mapping is merged. */
    @Column(name = "origin_hub_id") private Long originHubId;
    @Column(name = "current_hub_id") private Long currentHubId;
    @Column(name = "picked_up_at", columnDefinition = "DATETIME2(0)") private LocalDateTime pickedUpAt;
    @Column(name = "cancel_reason", length = 255, columnDefinition = "NVARCHAR(255)") private String cancelReason;

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

    /** Phase-02 intake data; every value is server-derived or validated before this call. */
    public void applyIntake(User receiver, String province, String district, String pickupMethod, String feePayer,
                            String inspectionPolicy, int weightG, int lengthCm, int widthCm, int heightCm,
                            BigDecimal declaredValue, BigDecimal codAmount, BigDecimal shippingFee,
                            String paymentStatus, ShipmentStatus status, LocalDateTime createdAt) {
        this.receiver = receiver; this.receiverProvince = province; this.receiverDistrict = district;
        this.pickupMethod = pickupMethod; this.feePayer = feePayer; this.inspectionPolicy = inspectionPolicy;
        this.weight = weightG; this.length = lengthCm; this.width = widthCm; this.height = heightCm;
        this.declaredValue = declaredValue; this.codAmount = codAmount; this.shippingFee = shippingFee;
        this.paymentStatus = paymentStatus; this.currentStatus = status;
        this.createdAt = createdAt;
        this.waitingForHandoverAt = status == ShipmentStatus.WAITING_FOR_HANDOVER ? createdAt : null;
    }

    public void confirmOnlinePayment(LocalDateTime confirmedAt) {
        paymentStatus = "PAID";
        currentStatus = ShipmentStatus.WAITING_FOR_HANDOVER;
        if (waitingForHandoverAt == null) {
            waitingForHandoverAt = confirmedAt;
        }
    }

    /** Callers hold the row lock and have checked the cancellable state. */
    public void cancel(String reason) {
        currentStatus = ShipmentStatus.CANCELLED;
        cancelReason = reason;
    }

    public void assignOriginHub(Long hubId) {
        if (originHubId == null) originHubId = hubId;
    }

    public void markPickedUp(LocalDateTime at, Long hubId, boolean cashCollected) {
        currentStatus = ShipmentStatus.PICKED_UP;
        pickedUpAt = at;
        currentHubId = hubId;
        if (cashCollected) paymentStatus = "PAID";
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
    public User getReceiver() { return receiver; }
    public String getSenderName() { return senderName; }
    public String getSenderPhone() { return senderPhone; }
    public String getSenderAddress() { return senderAddress; }
    public String getReceiverName() { return receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public String getReceiverAddress() { return receiverAddress; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getPickupMethod() { return pickupMethod; }
    public String getFeePayer() { return feePayer; }
    public ShipmentStatus getCurrentStatus() { return currentStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getWaitingForHandoverAt() { return waitingForHandoverAt; }
    public String getReceiverProvince() { return receiverProvince; }
    public String getReceiverDistrict() { return receiverDistrict; }
    public BigDecimal getCodAmount() { return codAmount; }
    public Long getOriginHubId() { return originHubId; }
    public Long getCurrentHubId() { return currentHubId; }
    public LocalDateTime getPickedUpAt() { return pickedUpAt; }
    public String getCancelReason() { return cancelReason; }
}
