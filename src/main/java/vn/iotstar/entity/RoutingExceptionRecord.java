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

import java.io.Serializable;
import java.time.LocalDateTime;

/** Durable, deduplicated operational exception for an unavailable static next-hop rule. */
@Entity
@Table(name = "routing_exceptions")
public class RoutingExceptionRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hub_id", nullable = false)
    private Hub hub;

    @Column(name = "exception_type", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String exceptionType;

    @Column(nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private String status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opened_by_user_id", nullable = false)
    private User openedBy;

    @Column(name = "opened_at", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime openedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private User resolvedBy;

    @Column(name = "resolved_at", columnDefinition = "DATETIME2(0)")
    private LocalDateTime resolvedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected RoutingExceptionRecord() {
    }

    public static RoutingExceptionRecord openRouteLookupBlocked(Shipment shipment, Hub hub, User openedBy) {
        RoutingExceptionRecord record = new RoutingExceptionRecord();
        record.shipment = shipment;
        record.hub = hub;
        record.exceptionType = "ROUTE_LOOKUP_BLOCKED";
        record.status = "OPEN";
        record.openedBy = openedBy;
        record.openedAt = LocalDateTime.now();
        return record;
    }

    public void resolve(User actor) {
        if (!"OPEN".equals(status)) {
            return;
        }
        status = "RESOLVED";
        resolvedBy = actor;
        resolvedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Shipment getShipment() { return shipment; }
    public Hub getHub() { return hub; }
    public String getExceptionType() { return exceptionType; }
    public String getStatus() { return status; }
    public User getOpenedBy() { return openedBy; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public User getResolvedBy() { return resolvedBy; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
}
