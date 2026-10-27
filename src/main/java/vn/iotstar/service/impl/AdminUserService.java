package vn.iotstar.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.repository.EmployeeProfileRepository;
import vn.iotstar.repository.HubRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.auth.VietnamPhoneNormalizer;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Workflow §23: the Administrator manages accounts, roles, hub assignment and positions. */
@Service
@Transactional
public class AdminUserService {
    public static final List<String> POSITIONS = List.of("GENERAL_STAFF", "COORDINATOR", "INBOUND", "SORTER", "OUTBOUND",
            "PICKUP_SHIPPER", "DELIVERY_SHIPPER", "LINEHAUL_DRIVER", "MANAGER");
    private static final Set<UserRole> HUB_ROLES = Set.of(UserRole.ROLE_HUB_STAFF, UserRole.ROLE_SHIPPER,
            UserRole.ROLE_LINEHAUL_DRIVER, UserRole.ROLE_MANAGER);
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final UserRepository users;
    private final EmployeeProfileRepository profiles;
    private final HubRepository hubs;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public AdminUserService(UserRepository users, EmployeeProfileRepository profiles, HubRepository hubs,
                            PasswordEncoder encoder, Clock clock) {
        this.users = users; this.profiles = profiles; this.hubs = hubs; this.encoder = encoder; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<User> search(String q, int page) {
        requireAdmin();
        return users.search(q == null ? "" : q.strip(), PageRequest.of(Math.max(page, 0), 20));
    }

    @Transactional(readOnly = true)
    public Optional<EmployeeProfile> profileOf(Long userId) {
        return profiles.findFirstByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Hub> hubs() {
        requireAdmin();
        return hubs.findAll(org.springframework.data.domain.Sort.by("code"));
    }

    /** Creates an ACTIVE account and returns its one-time generated password; only its hash is stored. */
    public String createStaff(String username, String fullName, String phone, String email, UserRole role,
                              Long hubId, String position) {
        requireAdmin();
        if (username == null || !username.matches("[A-Za-z0-9_.-]{3,50}") || fullName == null || fullName.isBlank()
                || email == null || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || role == null)
            throw new IllegalArgumentException("Thông tin tài khoản chưa hợp lệ.");
        String normalizedPhone;
        try {
            normalizedPhone = VietnamPhoneNormalizer.normalize(phone);
        } catch (RegistrationException exception) {
            throw new IllegalArgumentException(exception.getMessage());
        }
        if (users.existsByUsername(username) || users.existsByPhone(normalizedPhone) || users.existsByEmail(email.strip().toLowerCase()))
            throw new IllegalArgumentException("Tên đăng nhập, số điện thoại hoặc email đã được sử dụng.");
        Hub hub = hubFor(role, hubId, position);
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) password.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        User user = users.saveAndFlush(User.staff(username, encoder.encode(password), fullName.strip(), normalizedPhone,
                email.strip().toLowerCase(), role, clock.instant()));
        if (hub != null) profiles.save(EmployeeProfile.active(user, hub, position));
        return password.toString();
    }

    public void setLocked(Long userId, boolean locked) {
        User user = otherUser(userId);
        user.changeStatus(locked ? "LOCKED" : "ACTIVE");
    }

    public void assign(Long userId, UserRole role, Long hubId, String position) {
        User user = otherUser(userId);
        if (role == null) throw new IllegalArgumentException("Chọn vai trò.");
        Hub hub = hubFor(role, hubId, position);
        user.changeRole(role);
        if (hub == null) return;
        EmployeeProfile profile = profiles.findFirstByUserId(userId).orElse(null);
        if (profile == null) profiles.save(EmployeeProfile.active(user, hub, position));
        else profile.reassign(hub, position);
    }

    private Hub hubFor(UserRole role, Long hubId, String position) {
        if (!HUB_ROLES.contains(role)) return null;
        if (hubId == null || position == null || !POSITIONS.contains(position))
            throw new IllegalArgumentException("Nhân viên cần có bưu cục và vị trí làm việc.");
        return hubs.findById(hubId).orElseThrow(() -> new IllegalArgumentException("Bưu cục không tồn tại."));
    }

    private User otherUser(Long userId) {
        AuthenticatedActor actor = requireAdmin();
        if (actor.userId().equals(userId))
            throw new IllegalArgumentException("Không thể tự khoá hoặc đổi vai trò của chính mình.");
        return users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
    }

    private AuthenticatedActor requireAdmin() {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (!actor.hasRole(UserRole.ROLE_ADMIN)) throw new AccessDeniedException("ROLE_ADMIN required");
        return actor;
    }
}
