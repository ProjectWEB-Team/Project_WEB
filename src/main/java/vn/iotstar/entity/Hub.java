package vn.iotstar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Canonical physical hub/SOC hierarchy used for server-side operational scope. */
@Entity
@Table(name = "hubs")
public class Hub {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50, columnDefinition = "NVARCHAR(50)")
    private String code;
    @Column(nullable = false, length = 150, columnDefinition = "NVARCHAR(150)")
    private String name;
    @Column(nullable = false)
    private Integer level;
    @Column(nullable = false, length = 30, columnDefinition = "NVARCHAR(30)")
    private String type;
    @ManyToOne
    @JoinColumn(name = "parent_hub_id")
    private Hub parentHub;
    @Column(nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String region;
    @Column(nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String province;
    @Column(length = 100, columnDefinition = "NVARCHAR(100)")
    private String district;
    @Column(length = 255, columnDefinition = "NVARCHAR(255)")
    private String address;
    @Column(nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private String status;

    protected Hub() { }

    public static Hub active(String code, String name, int level, String type, String region, String province) {
        Hub hub = new Hub();
        hub.code = code;
        hub.name = name;
        hub.level = level;
        hub.type = type;
        hub.region = region;
        hub.province = province;
        hub.status = "ACTIVE";
        return hub;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Integer getLevel() { return level; }
    public String getType() { return type; }
    public Hub getParentHub() { return parentHub; }
    public String getRegion() { return region; }
    public String getProvince() { return province; }
    public String getStatus() { return status; }
}
