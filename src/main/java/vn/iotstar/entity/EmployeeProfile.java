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

/** Active staff position and base hub; role alone never grants an operations action. */
@Entity
@Table(name = "employee_profiles")
public class EmployeeProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "employee_code", nullable = false, unique = true, length = 50, columnDefinition = "NVARCHAR(50)")
    private String employeeCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hub_id", nullable = false)
    private Hub hub;

    /** Read-only scalar view retained for Phase-02 lookup code; {@link #hub} owns persistence. */
    @Column(name = "hub_id", insertable = false, updatable = false)
    private Long hubId;

    @Column(length = 30, columnDefinition = "NVARCHAR(30)")
    private String position;

    @Column(nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private String status;

    protected EmployeeProfile() {
    }

    /** Fixture/legacy compatibility constructor; production persistence uses the Hub relationship. */
    public EmployeeProfile(Long id, User user, Long hubId, String position, String status) {
        this.id = id;
        this.user = user;
        this.hubId = hubId;
        this.employeeCode = "PROFILE-" + id;
        this.position = position;
        this.status = status;
    }

    public static EmployeeProfile active(User user, Hub hub, String position) {
        EmployeeProfile profile = new EmployeeProfile();
        profile.user = user;
        profile.hub = hub;
        profile.hubId = hub == null ? null : hub.getId();
        profile.position = position;
        profile.status = "ACTIVE";
        profile.employeeCode = "AUTO-" + user.getId();
        return profile;
    }

    public void reassign(Hub newHub, String newPosition) {
        hub = newHub; hubId = newHub.getId(); position = newPosition; status = "ACTIVE";
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Hub getHub() { return hub; }
    public Long getHubId() { return hub == null ? hubId : hub.getId(); }
    public String getPosition() { return position; }
    public String getStatus() { return status; }
    public boolean isActive() { return "ACTIVE".equals(status); }
}
