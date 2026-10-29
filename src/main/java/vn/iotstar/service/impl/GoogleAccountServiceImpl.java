package vn.iotstar.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.GoogleAccountException;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.VerifiedGoogleIdentity;
import vn.iotstar.service.GoogleAccountService;

import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class GoogleAccountServiceImpl implements GoogleAccountService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public GoogleAccountServiceImpl(UserRepository users, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findActiveAccount(VerifiedGoogleIdentity identity) {
        validateIdentity(identity);
        Optional<User> account = users.findByGoogleSubject(identity.subject());
        account.ifPresent(user -> {
            if (!user.isEnabled() || !user.isAccountNonLocked() || user.getRole() != UserRole.ROLE_USER) {
                throw new GoogleAccountException();
            }
        });
        return account;
    }

    @Override
    @Transactional
    public User register(VerifiedGoogleIdentity identity) {
        validateIdentity(identity);
        String email = identity.email().trim().toLowerCase(Locale.ROOT);
        // A matching email is not permission to take over or merge an existing account.
        if (users.findByGoogleSubject(identity.subject()).isPresent()) throw new GoogleAccountException();
        if (users.existsByEmail(email)) throw RegistrationException.emailTaken();
        String name = identity.fullName() == null || identity.fullName().isBlank() ? email : identity.fullName().trim();
        String username = "g_" + UUID.randomUUID().toString().replace("-", "");
        String unknownPassword = UUID.randomUUID().toString() + UUID.randomUUID();
        // The phone is linked later from the profile page, never at sign-in.
        return users.saveAndFlush(User.googleAccount(username, passwordEncoder.encode(unknownPassword),
                name.substring(0, Math.min(name.length(), 100)), null, email, identity.subject(), clock.instant()));
    }

    private void validateIdentity(VerifiedGoogleIdentity identity) {
        if (identity == null || !identity.isCurrent(clock.instant())
                || identity.subject() == null || identity.subject().isBlank() || identity.subject().length() > 255
                || identity.email() == null || identity.email().isBlank() || identity.email().length() > 100
                || !identity.email().contains("@")) {
            throw new GoogleAccountException();
        }
    }
}
