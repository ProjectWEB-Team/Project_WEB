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

import java.time.LocalDateTime;

/** One materialized current next-hop decision, not a precomputed future route. */
@Entity
@Table(name = "shipment_routes")
public class ShipmentRoute {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hub_id", nullable = false)
    private Hub hub;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_hub_id", nullable = false)
    private Hub currentHub;
    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;
    @Column(nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private String status;
    @Column(name = "route_decision_key", nullable = false, unique = true, length = 120, columnDefinition = "NVARCHAR(120)")
    private String routeDecisionKey;
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;

    protected ShipmentRoute() { }

    public static ShipmentRoute decided(Shipment shipment, Hub currentHub, Hub nextHub, String decisionKey) {
        ShipmentRoute route = new ShipmentRoute();
        route.shipment = shipment;
        route.currentHub = currentHub;
        route.hub = nextHub;
        route.sequenceNumber = 1;
        route.status = "PENDING";
        route.routeDecisionKey = decisionKey;
        route.createdAt = LocalDateTime.now();
        return route;
    }
    public Long getId() { return id; }
    public Shipment getShipment() { return shipment; }
    public Hub getHub() { return hub; }
    public Hub getCurrentHub() { return currentHub; }
    public String getRouteDecisionKey() { return routeDecisionKey; }
}
