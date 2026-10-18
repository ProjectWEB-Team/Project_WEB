package vn.iotstar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Immutable membership after a container becomes part of a handover. */
@Entity
@IdClass(ContainerItemId.class)
@Table(name = "container_items")
public class ContainerItem {
    @Id @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "container_id", nullable = false)
    private ParcelContainer container;
    @Id @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;
    @Column(nullable = false, length = 30, columnDefinition = "NVARCHAR(30)") private String status;
    @Column(name = "scanned_at", nullable = false, columnDefinition = "DATETIME2(0)") private LocalDateTime scannedAt;
    protected ContainerItem() { }
    public static ContainerItem scanned(ParcelContainer container, Shipment shipment) {
        ContainerItem item = new ContainerItem(); item.container = container; item.shipment = shipment;
        item.status = "IN_CONTAINER"; item.scannedAt = LocalDateTime.now(); return item;
    }
    public ParcelContainer getContainer() { return container; }
    public Shipment getShipment() { return shipment; }
    public String getStatus() { return status; }
}
