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

/** An immutable operational/tracking event, distinct from shipment state (ADR-003). */
@Entity
@Table(name = "shipment_events")
public class ShipmentEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private User actor;

    @Column(name = "event_type", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String eventType;

    @Column(nullable = false, length = 255, columnDefinition = "NVARCHAR(255)")
    private String description;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;

    protected ShipmentEvent() {
    }

    public static ShipmentEvent shipmentCreated(Shipment shipment, User actor) {
        ShipmentEvent event = new ShipmentEvent();
        event.shipment = shipment;
        event.actor = actor;
        event.eventType = "SHIPMENT_CREATED";
        event.description = "Shipment created";
        return event;
    }

    @PrePersist
    void assignCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Shipment getShipment() { return shipment; }
    public User getActor() { return actor; }
    public String getEventType() { return eventType; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
