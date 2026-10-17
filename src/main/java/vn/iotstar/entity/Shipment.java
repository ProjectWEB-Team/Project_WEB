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
import java.time.LocalDateTime;

/** Minimal Phase-01 shipment record; later operational and financial fields remain untouched. */
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
    public ShipmentStatus getCurrentStatus() { return currentStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
