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

    /** Null only for server-owned automated events such as timeout cancellation. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_id")
    private Hub hub;

    @Column(name = "event_type", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String eventType;

    @Column(nullable = false, length = 255, columnDefinition = "NVARCHAR(255)")
    private String description;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;

    protected ShipmentEvent() {
    }

    public static ShipmentEvent shipmentCreated(Shipment shipment, User actor) {
        return of(shipment, actor, "SHIPMENT_CREATED", "Shipment created");
    }

    public static ShipmentEvent of(Shipment shipment, User actor, String eventType, String description) {
        ShipmentEvent event = new ShipmentEvent();
        event.shipment = shipment;
        event.actor = actor;
        event.eventType = eventType;
        event.description = description;
        event.createdAt = LocalDateTime.now();
        return event;
    }

    public static ShipmentEvent arrivedAtHub(Shipment shipment, User actor, Hub hub) {
        return operational(shipment, actor, hub, "ARRIVED_AT_HUB", "Shipment received at hub");
    }

    public static ShipmentEvent sortingCompleted(Shipment shipment, User actor, Hub hub) {
        return operational(shipment, actor, hub, "SORTING_COMPLETED", "Static next-hop routing completed");
    }

    public static ShipmentEvent routeLookupBlocked(Shipment shipment, User actor, Hub hub) {
        return operational(shipment, actor, hub, "ROUTE_LOOKUP_BLOCKED", "Static next-hop routing requires manager correction");
    }

    public static ShipmentEvent bagCreated(Shipment shipment, User actor, Hub hub) {
        return operational(shipment, actor, hub, "BAG_CREATED", "Shipment added to an outbound container");
    }

    public static ShipmentEvent outboundScanned(Shipment shipment, User actor, Hub hub) {
        return operational(shipment, actor, hub, "OUTBOUND_SCANNED", "Shipment scanned for outbound custody");
    }

    public static ShipmentEvent linehaulHandoverAccepted(Shipment shipment, User actor, Hub hub) {
        return operational(shipment, actor, hub, "LINEHAUL_HANDOVER_ACCEPTED", "Assigned linehaul driver accepted custody");
    }

    public static ShipmentEvent linehaulTripStarted(Shipment shipment, User actor, Hub hub) {
        return operational(shipment, actor, hub, "LINEHAUL_TRIP_STARTED", "Assigned linehaul trip started");
    }

    public static ShipmentEvent linehaulArrived(Shipment shipment, User actor, Hub hub) {
        return operational(shipment, actor, hub, "LINEHAUL_ARRIVED", "Linehaul arrived; destination receipt is still required");
    }
    public static ShipmentEvent missingParcel(Shipment shipment, User actor, Hub hub) {
        return operational(shipment, actor, hub, "MISSING_PARCEL", "Destination receipt recorded a missing container item");
    }

    private static ShipmentEvent operational(Shipment shipment, User actor, Hub hub, String eventType, String description) {
        ShipmentEvent event = new ShipmentEvent();
        event.shipment = shipment;
        event.actor = actor;
        event.hub = hub;
        event.eventType = eventType;
        event.description = description;
        event.createdAt = LocalDateTime.now();
        return event;
    }

    public static ShipmentEvent create(Shipment shipment, User actor, String eventType, String description) {
        ShipmentEvent event = new ShipmentEvent();
        event.shipment = shipment;
        event.actor = actor;
        event.eventType = eventType;
        event.description = description;
        event.createdAt = LocalDateTime.now();
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
    public Hub getHub() { return hub; }
    public String getEventType() { return eventType; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
