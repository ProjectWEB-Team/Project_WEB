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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/** Each expected physical container must be scanned by the assigned driver. */
@Entity
@Table(name = "handover_containers", uniqueConstraints = @UniqueConstraint(name = "UK_handover_container", columnNames = {"handover_id", "container_id"}))
public class HandoverContainer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "handover_id", nullable = false) private Handover handover;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "container_id", nullable = false) private ParcelContainer container;
    @Column(nullable = false, length = 30, columnDefinition = "NVARCHAR(30)") private String status;
    @Column(name = "scanned_at", columnDefinition = "DATETIME2(0)") private LocalDateTime scannedAt;
    protected HandoverContainer() { }
    public static HandoverContainer expected(Handover handover, ParcelContainer container) {
        HandoverContainer item = new HandoverContainer(); item.handover = handover; item.container = container; item.status = "EXPECTED"; return item;
    }
    public boolean isScanned() { return scannedAt != null; }
    public void markScanned() { if (!isScanned()) { status = "LOADED"; scannedAt = LocalDateTime.now(); } }
    public void completeReceipt(boolean discrepancy) {
        if (!isScanned()) throw new IllegalStateException("Container was not scanned for custody");
        status = discrepancy ? "DISCREPANCY" : "DELIVERED";
    }
    public String getStatus() { return status; }
    public LocalDateTime getScannedAt() { return scannedAt; }
    public Handover getHandover() { return handover; }
    public ParcelContainer getContainer() { return container; }
}
