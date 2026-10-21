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
import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_assignments")
public class ShipmentAssignment implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignee_user_id", nullable = false)
    private User assignee;

    @Column(name = "assignment_type", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String assignmentType;

    @Column(name = "status", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String status;

    @Column(name = "assigned_at", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime assignedAt;

    @Column(name = "accepted_at", columnDefinition = "DATETIME2(0)")
    private LocalDateTime acceptedAt;

    @Column(name = "completed_at", columnDefinition = "DATETIME2(0)")
    private LocalDateTime completedAt;

    protected ShipmentAssignment() {
    }

    public static ShipmentAssignment create(Shipment shipment, User assignee, String assignmentType) {
        ShipmentAssignment assignment = new ShipmentAssignment();
        assignment.shipment = shipment;
        assignment.assignee = assignee;
        assignment.assignmentType = assignmentType;
        assignment.status = "ASSIGNED";
        assignment.assignedAt = LocalDateTime.now();
        return assignment;
    }

    @PrePersist
    void onPrePersist() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Shipment getShipment() { return shipment; }
    public User getAssignee() { return assignee; }
    public String getAssignmentType() { return assignmentType; }
    public String getStatus() { return status; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }

    public void setStatus(String status) { this.status = status; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}

