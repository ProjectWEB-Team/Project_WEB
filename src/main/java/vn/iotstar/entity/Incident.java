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

/**
 * Single canonical incident record for the shared {@code incidents} table.
 *
 * <p>The column set follows the field-level authority in {@code docs/schema.sql}
 * (ADR-004). Operational incidents (MISSING_PARCEL, DAMAGED_PARCEL, WRONG_ROUTE,
 * TRIP_DELAY) and financial incidents (MONEY_SHORTAGE, MONEY_OVERAGE) share the
 * same table so that ADR-006 stays satisfied without a second table.
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
    private String type; // MISSING_PARCEL, DAMAGED_PARCEL, WRONG_ROUTE, TRIP_DELAY, MONEY_SHORTAGE, MONEY_OVERAGE, OTHER

    @Column(name = "description", length = 500, columnDefinition = "NVARCHAR(500)")
    private String description;

    @Column(name = "reported_by", nullable = false)
    private Long reportedBy;

    @Column(name = "status", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String status = "OPEN"; // OPEN, INVESTIGATING, RESOLVED, CLOSED

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;

    @Column(name = "resolved_at", columnDefinition = "DATETIME2(0)")
    private LocalDateTime resolvedAt;

    protected Incident() {
    }

    public static Incident create(Long shipmentId, Long tripId, Long hubId, String type,
                                  String description, Long reportedBy) {
        Incident incident = new Incident();
        incident.shipmentId = shipmentId;
        incident.tripId = tripId;
        incident.hubId = hubId;
        incident.type = type;
        incident.description = description;
        incident.reportedBy = reportedBy;
        incident.status = "OPEN";
        incident.createdAt = LocalDateTime.now();
        return incident;
    }

    @PrePersist
    void onPrePersist() {
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

    public void setId(Long id) { this.id = id; }
    public void setStatus(String status) { this.status = status; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}