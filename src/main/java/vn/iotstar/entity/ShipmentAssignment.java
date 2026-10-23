package vn.iotstar.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** A pickup task; only one ASSIGNED pickup exists per shipment at a time. */
@Entity
@Table(name = "shipment_assignments")
public class ShipmentAssignment {
    public static final String PICKUP = "PICKUP";
    public static final String ASSIGNED = "ASSIGNED";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignee_user_id", nullable = false)
    private User assignee;
    @Column(name = "assignment_type", nullable = false) private String assignmentType;
    @Column(nullable = false) private String status;
    @Column(name = "assigned_at", nullable = false, columnDefinition = "DATETIME2(0)") private LocalDateTime assignedAt;
    @Column(name = "completed_at", columnDefinition = "DATETIME2(0)") private LocalDateTime completedAt;

    protected ShipmentAssignment() { }

    public static ShipmentAssignment pickup(Shipment shipment, User shipper, LocalDateTime at) {
        ShipmentAssignment a = new ShipmentAssignment();
        a.shipment = shipment; a.assignee = shipper; a.assignmentType = PICKUP; a.status = ASSIGNED; a.assignedAt = at;
        return a;
    }

    public void complete(LocalDateTime at) { status = "COMPLETED"; completedAt = at; }
    public void cancel() { status = "CANCELLED"; }

    public Long getId() { return id; }
    public Shipment getShipment() { return shipment; }
    public User getAssignee() { return assignee; }
    public String getStatus() { return status; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public boolean isActive() { return ASSIGNED.equals(status); }
}
