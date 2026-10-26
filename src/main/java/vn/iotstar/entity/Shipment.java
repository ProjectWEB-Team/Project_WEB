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
import jakarta.persistence.Version;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Canonical shipment aggregate shared by intake, operations, last-mile, and finance. */
@Entity
@Table(name = "shipments")
public class Shipment implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tracking_number", nullable = false, unique = true, length = 50, columnDefinition = "NVARCHAR(50)") private String trackingNumber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sender_user_id", nullable = false) private User sender;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "receiver_user_id") private User receiver;
    @Column(name = "sender_name", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)") private String senderName;
    @Column(name = "sender_phone", nullable = false, length = 20, columnDefinition = "NVARCHAR(20)") private String senderPhone;
    @Column(name = "sender_address", nullable = false, length = 255, columnDefinition = "NVARCHAR(255)") private String senderAddress;
    @Column(name = "receiver_name", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)") private String receiverName;
    @Column(name = "receiver_phone", nullable = false, length = 20, columnDefinition = "NVARCHAR(20)") private String receiverPhone;
    @Column(name = "receiver_address", nullable = false, length = 255, columnDefinition = "NVARCHAR(255)") private String receiverAddress;
    @Column(name = "receiver_province", length = 100, columnDefinition = "NVARCHAR(100)") private String receiverProvince;
    @Column(name = "receiver_district", length = 100, columnDefinition = "NVARCHAR(100)") private String receiverDistrict;
    @Column(name = "service_code", length = 50, columnDefinition = "NVARCHAR(50)") private String serviceCode;
    @Column(name = "weight") private Integer weight;
    @Column(name = "length") private Integer length;
    @Column(name = "width") private Integer width;
    @Column(name = "height") private Integer height;
    @Column(name = "declared_value", precision = 15, scale = 2) private BigDecimal declaredValue;
    @Column(name = "shipping_fee", precision = 15, scale = 2) private BigDecimal shippingFee;
    @Column(name = "cod_amount", precision = 15, scale = 2) private BigDecimal codAmount;
    @Column(name = "return_fee", precision = 15, scale = 2) private BigDecimal returnFee;
    @Column(name = "pickup_method", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)") private String pickupMethod;
    @Column(name = "fee_payer", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)") private String feePayer;
    @Column(name = "inspection_policy", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)") private String inspectionPolicy;
    @Column(name = "payment_status", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)") private String paymentStatus;
    @Enumerated(EnumType.STRING) @Column(name = "current_status", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)") private ShipmentStatus currentStatus;

    /** The scalar owns writes; the relationship is a read-only convenience for Phase-03 scope checks. */
    @Column(name = "current_hub_id") private Long currentHubId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "current_hub_id", insertable = false, updatable = false) private Hub currentHub;
    @Column(name = "origin_hub_id") private Long originHubId;
    @Column(name = "destination_hub_id") private Long destinationHubId;
    @Column(name = "destination_region", length = 100, columnDefinition = "NVARCHAR(100)") private String destinationRegion;
    @Column(name = "destination_province", length = 100, columnDefinition = "NVARCHAR(100)") private String destinationProvince;
    @Column(name = "direction", length = 20, columnDefinition = "NVARCHAR(20)") private String direction = "FORWARD";
    @Column(name = "fail_reason", length = 50, columnDefinition = "NVARCHAR(50)") private String failReason;
    @Column(name = "cancel_reason", length = 255, columnDefinition = "NVARCHAR(255)") private String cancelReason;
    @Column(name = "closure_reason", length = 50, columnDefinition = "NVARCHAR(50)") private String closureReason;
    @Column(name = "on_hold", nullable = false) private boolean onHold;
    @Column(name = "delivered_at", columnDefinition = "DATETIME2(0)") private LocalDateTime deliveredAt;
    @Column(name = "returned_at", columnDefinition = "DATETIME2(0)") private LocalDateTime returnedAt;
    @Column(name = "return_arrived_at_origin_at", columnDefinition = "DATETIME2(0)") private LocalDateTime returnArrivedAtOriginAt;
    @Version @Column(name = "version", nullable = false) private Long version;
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME2(0)") private LocalDateTime createdAt;
    @Column(name = "waiting_for_handover_at", columnDefinition = "DATETIME2(0)") private LocalDateTime waitingForHandoverAt;
    @Column(name = "picked_up_at", columnDefinition = "DATETIME2(0)") private LocalDateTime pickedUpAt;

    protected Shipment() {
    }

    public static Shipment create(String trackingNumber, User sender, String senderName, String senderPhone,
                                  String senderAddress, String receiverName, String receiverPhone, String receiverAddress) {
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

    public void applyIntake(User receiver, String province, String district, String pickupMethod, String feePayer,
                            String inspectionPolicy, int weightG, int lengthCm, int widthCm, int heightCm,
                            BigDecimal declaredValue, BigDecimal codAmount, BigDecimal shippingFee,
                            String paymentStatus, ShipmentStatus status, LocalDateTime createdAt) {
        this.receiver = receiver; this.receiverProvince = province; this.receiverDistrict = district;
        this.pickupMethod = pickupMethod; this.feePayer = feePayer; this.inspectionPolicy = inspectionPolicy;
        this.weight = weightG; this.length = lengthCm; this.width = widthCm; this.height = heightCm;
        this.declaredValue = declaredValue; this.codAmount = codAmount; this.shippingFee = shippingFee;
        this.paymentStatus = paymentStatus; this.currentStatus = status; this.createdAt = createdAt;
        this.waitingForHandoverAt = status == ShipmentStatus.WAITING_FOR_HANDOVER ? createdAt : null;
    }

    public void confirmOnlinePayment(LocalDateTime confirmedAt) {
        paymentStatus = "PAID"; currentStatus = ShipmentStatus.WAITING_FOR_HANDOVER;
        if (waitingForHandoverAt == null) waitingForHandoverAt = confirmedAt;
    }
    public void cancel(String reason) { currentStatus = ShipmentStatus.CANCELLED; cancelReason = reason; }
    public void assignOriginHub(Long hubId) { if (originHubId == null) originHubId = hubId; }
    public void markPickedUp(LocalDateTime at, Hub hub, boolean cashCollected) {
        currentStatus = ShipmentStatus.PICKED_UP; pickedUpAt = at; currentHub = hub; currentHubId = hub == null ? null : hub.getId();
        if (cashCollected) paymentStatus = "PAID";
    }
    /** Compatibility for isolated Phase-02 fixtures; production flow supplies a Hub relationship. */
    public void markPickedUp(LocalDateTime at, Long hubId, boolean cashCollected) {
        currentStatus = ShipmentStatus.PICKED_UP; pickedUpAt = at; currentHubId = hubId;
        if (cashCollected) paymentStatus = "PAID";
    }
    public void setDestinationGrouping(String destinationRegion, String destinationProvince) {
        this.destinationRegion = destinationRegion; this.destinationProvince = destinationProvince;
    }
    public void receiveAt(Hub hub) {
        currentHub = hub; currentHubId = hub == null ? null : hub.getId();
        currentStatus = "RETURN".equals(direction) ? ShipmentStatus.RETURN_TO_SENDER : ShipmentStatus.AT_HUB;
        recordReturnOriginArrival(LocalDateTime.now());
    }
    public void recordReturnOriginArrival(LocalDateTime at) {
        if (returnArrivedAtOriginAt == null && "RETURN".equals(direction)
                && currentStatus == ShipmentStatus.RETURN_TO_SENDER && originHubId != null && originHubId.equals(currentHubId))
            returnArrivedAtOriginAt = at;
    }
    public LocalDateTime getReturnArrivedAtOriginAt() { return returnArrivedAtOriginAt; }
    public String routeDecisionKey(Long hubId) { return "shipment:" + id + ":hub:" + hubId + ":" + direction; }
    @PrePersist void assignCreatedAt() { if (createdAt == null) createdAt = LocalDateTime.now(); }

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
    public String getPickupMethod() { return pickupMethod; }
    public String getFeePayer() { return feePayer; }
    public String getInspectionPolicy() { return inspectionPolicy; }
    public String getPaymentStatus() { return paymentStatus; }
    public ShipmentStatus getCurrentStatus() { return currentStatus; }
    public Long getCurrentHubId() { return currentHubId; }
    public Hub getCurrentHub() { return currentHub; }
    public Long getOriginHubId() { return originHubId; }
    public Long getDestinationHubId() { return destinationHubId; }
    public String getDestinationRegion() { return destinationRegion; }
    public String getDestinationProvince() { return destinationProvince; }
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
    public LocalDateTime getWaitingForHandoverAt() { return waitingForHandoverAt; }
    public String getReceiverProvince() { return receiverProvince; }
    public String getReceiverDistrict() { return receiverDistrict; }
    public LocalDateTime getPickedUpAt() { return pickedUpAt; }
    public void setId(Long id) { this.id = id; }
    public void transitionTo(ShipmentStatus nextStatus) { this.currentStatus = nextStatus; }
    public void setCurrentHubId(Long currentHubId) { this.currentHubId = currentHubId; this.currentHub = null; }
    public void setOriginHubId(Long originHubId) { this.originHubId = originHubId; }
    public void setDestinationHubId(Long destinationHubId) { this.destinationHubId = destinationHubId; }
    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
    public Integer getWeight() { return weight; }
    public Integer getLength() { return length; }
    public Integer getWidth() { return width; }
    public Integer getHeight() { return height; }
    public void recordMeasurements(int weight, int length, int width, int height) {
        this.weight = weight; this.length = length; this.width = width; this.height = height;
    }
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
