package vn.iotstar.entity;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "users")
public class User implements UserDetails {
    private static final long serialVersionUID = 1L;
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50, columnDefinition = "NVARCHAR(50)")
    private String username;
    @Column(name = "password", nullable = false, length = 255, columnDefinition = "NVARCHAR(255)")
    private String passwordHash;
    @Column(name = "full_name", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String fullName;
    @Column(nullable = false, unique = true, length = 20, columnDefinition = "NVARCHAR(20)")
    private String phone;
    @Column(nullable = false, unique = true, length = 100, columnDefinition = "NVARCHAR(100)")
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;
    @Column(nullable = false)
    private String status = "INACTIVE";
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;
    @Column(name = "phone_verified_at", columnDefinition = "DATETIME2(0)")
    private LocalDateTime phoneVerifiedAt;
    @Column(name = "email_verified_at", columnDefinition = "DATETIME2(0)")
    private LocalDateTime emailVerifiedAt;
    @Column(name = "google_subject", length = 255, columnDefinition = "NVARCHAR(255)")
    private String googleSubject;

    protected User() { }

    public User(Long id, String username, String passwordHash, UserRole role, String status) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getGoogleSubject() { return googleSubject; }
    public boolean isPhoneVerified() { return phoneVerifiedAt != null; }
    public boolean isEmailVerified() { return emailVerifiedAt != null; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public static User registerPending(String username, String passwordHash, String fullName,
                                       String phone, String email, Instant now) {
        User user = new User(null, username, passwordHash, UserRole.ROLE_USER, "INACTIVE");
        user.fullName = fullName; user.phone = phone; user.email = email;
        user.createdAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        return user;
    }
    public static User googleAccount(String username, String passwordHash, String fullName,
                                     String phone, String email, String googleSubject, Instant now) {
        User user = registerPending(username, passwordHash, fullName, phone, email, now);
        user.googleSubject = googleSubject;
        user.emailVerifiedAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        user.status = "ACTIVE";
        return user;
    }
    public void verifyPhone(Instant now) {
        if ("LOCKED".equals(status)) throw new IllegalStateException("Account is locked");
        phoneVerifiedAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        status = "ACTIVE";
    }
    public UserRole getRole() { return role; }
    public String getStatus() { return status; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority(role.name())); }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return username; }
    @Override public boolean isEnabled() { return "ACTIVE".equals(status); }
    @Override public boolean isAccountNonLocked() { return !"LOCKED".equals(status); }
}
