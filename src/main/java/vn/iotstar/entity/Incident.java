package vn.iotstar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Canonical shared incident record from the ERD/Workflow.
 *
 * <p>Operational incident provenance remains owned by Phase 03: drivers may
 * record only {@code TRIP_DELAY}; {@code MISSING_PARCEL} comes only from a
 * destination receipt. Financial incident types belong to Phase 04, but use
 * this same table rather than a parallel operational schema.</p>
 */
@Entity
@Table(name = "incidents")
public class Incident implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shipment_id")
    private Long shipmentId;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "hub_id")
    private Long hubId;

    @Column(name = "type", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String type;

    @Column(name = "description", length = 500, columnDefinition = "NVARCHAR(500)")
    private String description;

    @Column(name = "reported_by", nullable = false)
    private Long reportedBy;

    @Column(name = "status", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String status;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;

    @Column(name = "resolved_at", columnDefinition = "DATETIME2(0)")
    private LocalDateTime resolvedAt;

    protected Incident() {
    }

    /** Driver-only, bounded delay evidence for the driver's currently assigned trip. */
    public static Incident tripDelay(TransportTrip trip, User actor, String note, String evidenceReference) {
        Objects.requireNonNull(trip, "trip");
        Objects.requireNonNull(actor, "actor");
        return create(null, trip.getId(), trip.getOriginHub().getId(), "TRIP_DELAY",
                appendEvidence(note, evidenceReference), actor.getId(), "OPEN");
    }

    /** Destination-receipt-only evidence for one affected shipment/container item. */
    public static Incident missingParcel(Shipment shipment, TransportTrip trip, User actor, Hub hub,
                                        String receiptReference) {
        Objects.requireNonNull(shipment, "shipment");
        Objects.requireNonNull(trip, "trip");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(hub, "hub");
        return create(shipment.getId(), trip.getId(), hub.getId(), "MISSING_PARCEL",
                appendEvidence("Destination receipt discrepancy", receiptReference), actor.getId(), "INVESTIGATING");
    }

    private static Incident create(Long shipmentId, Long tripId, Long hubId, String type, String description,
                                   Long reportedBy, String status) {
        Incident incident = new Incident();
        incident.shipmentId = shipmentId;
        incident.tripId = tripId;
        incident.hubId = hubId;
        incident.type = type;
        incident.description = description;
        incident.reportedBy = reportedBy;
        incident.status = status;
        incident.createdAt = LocalDateTime.now();
        return incident;
    }

    private static String appendEvidence(String description, String evidenceReference) {
        if (evidenceReference == null || evidenceReference.isBlank()) {
            return description;
        }
        return description + " [evidence:" + evidenceReference.trim() + "]";
    }

    @PrePersist
    void assignCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Long getShipmentId() { return shipmentId; }
    public Long getTripId() { return tripId; }
    public Long getHubId() { return hubId; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public Long getReportedBy() { return reportedBy; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
}
