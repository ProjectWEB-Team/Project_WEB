package vn.iotstar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/** Accountable container-level transfer from an outbound employee to an assigned driver. */
@Entity
@Table(name = "handovers")
public class Handover {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "from_hub_id", nullable = false) private Hub fromHub;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "to_hub_id", nullable = false) private Hub toHub;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sender_employee_id", nullable = false) private EmployeeProfile senderEmployee;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "receiver_driver_id", nullable = false) private User receiverDriver;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "transport_trip_id") private TransportTrip transportTrip;
    @Column(name = "expected_containers", nullable = false) private int expectedContainers;
    @Column(name = "accepted_containers", nullable = false) private int acceptedContainers;
    @Column(nullable = false, length = 30, columnDefinition = "NVARCHAR(30)") private String status;
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME2(0)") private LocalDateTime createdAt;
    @Column(name = "accepted_at", columnDefinition = "DATETIME2(0)") private LocalDateTime acceptedAt;
    @Column(name = "completed_at", columnDefinition = "DATETIME2(0)") private LocalDateTime completedAt;
    @Column(name = "cancelled_at", columnDefinition = "DATETIME2(0)") private LocalDateTime cancelledAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "replaces_handover_id") private Handover replacesHandover;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "replacement_handover_id") private Handover replacementHandover;
    @Version @Column(nullable = false) private Long version;
    protected Handover() { }
    public static Handover pending(Hub from, Hub to, EmployeeProfile sender, User driver, int expected, Handover replaced) {
        Handover handover = new Handover(); handover.fromHub = from; handover.toHub = to; handover.senderEmployee = sender;
        handover.receiverDriver = driver; handover.expectedContainers = expected; handover.acceptedContainers = 0;
        handover.status = "PENDING_ACCEPTANCE"; handover.createdAt = LocalDateTime.now(); handover.replacesHandover = replaced; return handover;
    }
    public boolean isActive() { return "PENDING_ACCEPTANCE".equals(status) || "ACCEPTED".equals(status) || "IN_TRANSIT".equals(status); }
    public boolean isPending() { return "PENDING_ACCEPTANCE".equals(status); }
    public boolean acceptsDriver(Long userId) { return receiverDriver.getId().equals(userId); }
    public void recordContainerScan() {
        if (!isPending()) throw new IllegalStateException("Handover is not awaiting acceptance");
        acceptedContainers++;
        if (acceptedContainers == expectedContainers) { status = "ACCEPTED"; acceptedAt = LocalDateTime.now(); }
    }
    public void cancelForCorrection() { if (!isActive() || transportTrip != null) throw new IllegalStateException("Only custody without a trip can be corrected"); status = "CANCELLED"; cancelledAt = LocalDateTime.now(); }
    public void depart() {
        if (!"ACCEPTED".equals(status)) throw new IllegalStateException("Custody has not been accepted");
        status = "IN_TRANSIT";
    }
    /** Destination physical reconciliation, never driver arrival, settles the custody outcome. */
    public void completeDestinationReceipt(boolean discrepancy) {
        if (!"IN_TRANSIT".equals(status)) throw new IllegalStateException("Only departed custody can be received at destination");
        status = discrepancy ? "COMPLETED_WITH_DISCREPANCY" : "COMPLETED";
        completedAt = LocalDateTime.now();
    }
    public void linkReplacement(Handover replacement) { this.replacementHandover = replacement; }
    public void linkTransportTrip(TransportTrip trip) { this.transportTrip = trip; }
    public Long getId() { return id; }
    public Hub getFromHub() { return fromHub; }
    public Hub getToHub() { return toHub; }
    public User getReceiverDriver() { return receiverDriver; }
    public int getExpectedContainers() { return expectedContainers; }
    public int getAcceptedContainers() { return acceptedContainers; }
    public String getStatus() { return status; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public Handover getReplacesHandover() { return replacesHandover; }
    public Handover getReplacementHandover() { return replacementHandover; }
    public TransportTrip getTransportTrip() { return transportTrip; }
    public EmployeeProfile getSenderEmployee() { return senderEmployee; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
