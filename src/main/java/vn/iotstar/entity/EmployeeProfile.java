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
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "employee_code", nullable = false, unique = true, length = 50, columnDefinition = "NVARCHAR(50)")
    private String employeeCode;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hub_id", nullable = false)
    private Hub hub;
    @Column(length = 30, columnDefinition = "NVARCHAR(30)")
    private String position;
    @Column(nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private String status;

    protected EmployeeProfile() { }

    public static EmployeeProfile active(User user, Hub hub, String position) {
        EmployeeProfile profile = new EmployeeProfile();
        profile.user = user;
        profile.hub = hub;
        profile.position = position;
        profile.status = "ACTIVE";
        profile.employeeCode = "AUTO-" + user.getId();
        return profile;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Hub getHub() { return hub; }
    public String getPosition() { return position; }
    public String getStatus() { return status; }
}
