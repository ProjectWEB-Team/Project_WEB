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
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/** A physical bag/container. Custody is at this level, never per parcel. */
@Entity
@Table(name = "parcel_containers", uniqueConstraints = @UniqueConstraint(name = "UK_parcel_containers_code", columnNames = "container_code"))
public class ParcelContainer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "container_code", nullable = false, unique = true, length = 80, columnDefinition = "NVARCHAR(80)")
    private String containerCode;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "origin_hub_id", nullable = false)
    private Hub originHub;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "destination_hub_id", nullable = false)
    private Hub destinationHub;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @Column(nullable = false, length = 30, columnDefinition = "NVARCHAR(30)")
    private String status;
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;
    @Version @Column(nullable = false)
    private Long version;

    protected ParcelContainer() { }

    public static ParcelContainer open(String code, Hub originHub, Hub destinationHub, User createdBy) {
        ParcelContainer container = new ParcelContainer();
        container.containerCode = code;
        container.originHub = originHub;
        container.destinationHub = destinationHub;
        container.createdBy = createdBy;
        container.status = "OPEN";
        container.createdAt = LocalDateTime.now();
        return container;
    }

    public void seal() {
        if (!"OPEN".equals(status)) throw new IllegalStateException("Container is not open for custody");
        status = "SEALED";
    }
    public Long getId() { return id; }
    public String getContainerCode() { return containerCode; }
    public Hub getOriginHub() { return originHub; }
    public Hub getDestinationHub() { return destinationHub; }
    public User getCreatedBy() { return createdBy; }
    public String getStatus() { return status; }
}
