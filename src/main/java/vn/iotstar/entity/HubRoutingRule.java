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

/** Static canonical next-hop matrix; it is not a dynamic routing algorithm. */
@Entity
@Table(name = "hub_routing_rules")
public class HubRoutingRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_hub_id", nullable = false)
    private Hub currentHub;
    @Column(name = "destination_region", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String destinationRegion;
    @Column(name = "destination_province", length = 100, columnDefinition = "NVARCHAR(100)")
    private String destinationProvince;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "next_hub_id", nullable = false)
    private Hub nextHub;
    @Column(nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private String status;
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime updatedAt;

    protected HubRoutingRule() { }

    public static HubRoutingRule active(Hub currentHub, String region, String province, Hub nextHub) {
        HubRoutingRule rule = new HubRoutingRule();
        rule.currentHub = currentHub;
        rule.destinationRegion = region;
        rule.destinationProvince = province;
        rule.nextHub = nextHub;
        rule.status = "ACTIVE";
        rule.createdAt = LocalDateTime.now();
        rule.updatedAt = rule.createdAt;
        return rule;
    }
    public void deactivate() { status = "INACTIVE"; updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Hub getCurrentHub() { return currentHub; }
    public String getDestinationRegion() { return destinationRegion; }
    public String getDestinationProvince() { return destinationProvince; }
    public Hub getNextHub() { return nextHub; }
    public String getStatus() { return status; }
}
