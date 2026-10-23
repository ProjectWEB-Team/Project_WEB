package vn.iotstar.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.iotstar.dto.auth.GoogleRegistrationRequest;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.GoogleAccountException;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.VerifiedGoogleIdentity;
import vn.iotstar.service.auth.VietnamPhoneNormalizer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAccountServiceImplTest {
    private static final Instant NOW = Instant.parse("2026-10-22T08:00:00Z");
    @Mock private UserRepository users;
    @Mock private PasswordEncoder encoder;
    private GoogleAccountServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GoogleAccountServiceImpl(users, encoder, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void verifiedGoogleAccountActivatesEmailButNeverClaimsPhoneOwnership() {
        when(encoder.encode(anyString())).thenReturn("opaque-password-hash");
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.register(identity(), form());

        assertThat(result.getRole()).isEqualTo(UserRole.ROLE_USER);
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.isEmailVerified()).isTrue();
        assertThat(result.isPhoneVerified()).isFalse();
        assertThat(result.getGoogleSubject()).isEqualTo("google-sub-123");
        assertThat(result.getEmail()).isEqualTo("owner@example.test");
        assertThat(result.getPhone()).isEqualTo(VietnamPhoneNormalizer.normalize("0901234567"));
        assertThat(result.getPassword()).isEqualTo("opaque-password-hash");
        ArgumentCaptor<String> secret = ArgumentCaptor.forClass(String.class);
        verify(encoder).encode(secret.capture());
        assertThat(secret.getValue()).hasSizeGreaterThanOrEqualTo(64);
    }

    @Test
    void matchingEmailDoesNotLinkOrTakeOverExistingLocalAccount() {
        when(users.existsByEmail("owner@example.test")).thenReturn(true);

        assertThatThrownBy(() -> service.register(identity(), form())).isInstanceOf(GoogleAccountException.class);

        verify(users, never()).saveAndFlush(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void matchingPhoneDoesNotLinkExistingRecipient() {
        when(users.existsByPhone(VietnamPhoneNormalizer.normalize("0901234567"))).thenReturn(true);

        assertThatThrownBy(() -> service.register(identity(), form())).isInstanceOf(GoogleAccountException.class);

        verify(users, never()).saveAndFlush(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void existingSubjectSignsIntoOnlyTheAlreadyLinkedAccount() {
        User linked = new User(9L, "google-user", "hash", UserRole.ROLE_USER, "ACTIVE");
        when(users.findByGoogleSubject("google-sub-123")).thenReturn(Optional.of(linked));

        assertThat(service.findActiveAccount(identity())).containsSame(linked);
        verify(users, never()).findByEmail(anyString());
        verify(users, never()).findByPhone(anyString());
    }

    @Test
    void inactiveOrLockedGoogleAccountCannotSignIn() {
        when(users.findByGoogleSubject("google-sub-123"))
                .thenReturn(Optional.of(new User(9L, "google-user", "hash", UserRole.ROLE_USER, "LOCKED")));

        assertThatThrownBy(() -> service.findActiveAccount(identity())).isInstanceOf(GoogleAccountException.class);
    }

    @Test
    void previouslyLinkedAccountPromotedToInternalRoleCannotUseCustomerGoogleLogin() {
        when(users.findByGoogleSubject("google-sub-123"))
                .thenReturn(Optional.of(new User(9L, "google-user", "hash", UserRole.ROLE_ADMIN, "ACTIVE")));

        assertThatThrownBy(() -> service.findActiveAccount(identity())).isInstanceOf(GoogleAccountException.class);
    }

    @Test
    void expiredProofIsRejectedBeforeRepositoryAccess() {
        VerifiedGoogleIdentity expired = new VerifiedGoogleIdentity("google-sub-123", "owner@example.test", "Owner",
                NOW.minus(VerifiedGoogleIdentity.VALIDITY));

        assertThatThrownBy(() -> service.register(expired, form())).isInstanceOf(GoogleAccountException.class);
        verifyNoInteractions(users, encoder);
    }

    @Test
    void secondRegistrationWithSameGoogleSubjectIsRejected() {
        when(users.findByGoogleSubject("google-sub-123"))
                .thenReturn(Optional.of(new User(9L, "google-user", "hash", UserRole.ROLE_USER, "ACTIVE")));

        assertThatThrownBy(() -> service.register(identity(), form())).isInstanceOf(GoogleAccountException.class);
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void invalidPhoneReturnsSafeGoogleRegistrationErrorBeforeDatabaseAccess() {
        GoogleRegistrationRequest form = form();
        form.setPhone("not-a-phone");

        assertThatThrownBy(() -> service.register(identity(), form)).isInstanceOf(GoogleAccountException.class);
        verifyNoInteractions(users, encoder);
    }

    private VerifiedGoogleIdentity identity() {
        return new VerifiedGoogleIdentity("google-sub-123", "Owner@Example.Test", "Owner", NOW);
    }

    private GoogleRegistrationRequest form() {
        GoogleRegistrationRequest form = new GoogleRegistrationRequest();
        form.setFullName("Owner");
        form.setPhone("0901234567");
        return form;
    }
}
