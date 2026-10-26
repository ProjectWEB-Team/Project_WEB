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
    @Column(name = "received_at", columnDefinition = "DATETIME2(0)") private LocalDateTime receivedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "received_by_user_id") private User receivedBy;
    @Column(name = "found_at", columnDefinition = "DATETIME2(0)") private LocalDateTime foundAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "found_by_user_id") private User foundBy;
    protected ContainerItem() { }
    public static ContainerItem scanned(ParcelContainer container, Shipment shipment) {
        ContainerItem item = new ContainerItem(); item.container = container; item.shipment = shipment;
        item.status = "IN_CONTAINER"; item.scannedAt = LocalDateTime.now(); return item;
    }
    public ParcelContainer getContainer() { return container; }
    public Shipment getShipment() { return shipment; }
    public String getStatus() { return status; }
    public boolean isReceived() { return "RECEIVED".equals(status); }
    public boolean isMissing() { return "MISSING".equals(status); }
    public boolean isRecovered() { return foundAt != null; }
    public void recover(User actor) {
        if (!isMissing() || isRecovered()) throw new IllegalStateException("Only an unresolved missing item can be recovered");
        foundAt = LocalDateTime.now(); foundBy = actor;
        // The original MISSING reconciliation evidence remains immutable.
    }
    public void receive(User actor) {
        if (isReceived()) return;
        if (isMissing()) throw new IllegalStateException("A missing item cannot be received without corrective custody evidence");
        status = "RECEIVED"; receivedAt = LocalDateTime.now(); receivedBy = actor;
    }
    public void markMissing(User actor) {
        if (isMissing()) return;
        if (isReceived()) throw new IllegalStateException("A received item cannot be marked missing");
        status = "MISSING"; receivedAt = LocalDateTime.now(); receivedBy = actor;
    }
}
