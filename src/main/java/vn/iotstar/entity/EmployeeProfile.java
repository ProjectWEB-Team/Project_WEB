package vn.iotstar.entity;

import jakarta.persistence.*;

/** Read-only staff profile: the hub and position that scope coordinator and shipper actions. */
@Entity
@Table(name = "employee_profiles")
public class EmployeeProfile {
    @Id private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "hub_id") private Long hubId;
    @Column private String position;
    @Column(nullable = false) private String status;

    protected EmployeeProfile() { }

    public EmployeeProfile(Long id, User user, Long hubId, String position, String status) {
        this.id = id; this.user = user; this.hubId = hubId; this.position = position; this.status = status;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Long getHubId() { return hubId; }
    public String getPosition() { return position; }
    public boolean isActive() { return "ACTIVE".equals(status); }
}
