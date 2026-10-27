package vn.iotstar.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.repository.EmployeeProfileRepository;
import vn.iotstar.repository.HubRepository;
import vn.iotstar.repository.UserRepository;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final EmployeeProfileRepository profiles = mock(EmployeeProfileRepository.class);
    private final HubRepository hubs = mock(HubRepository.class);
    private final AdminUserService service = new AdminUserService(users, profiles, hubs,
            NoOpPasswordEncoder.getInstance(), Clock.systemUTC());

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void nonAdminIsDenied() {
        login(5L, UserRole.ROLE_MANAGER);
        assertThatThrownBy(() -> service.setLocked(9L, true)).isInstanceOf(AccessDeniedException.class);
    }

    @Test void adminCannotLockOrDemoteThemself() {
        login(1L, UserRole.ROLE_ADMIN);
        assertThatThrownBy(() -> service.setLocked(1L, true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.assign(1L, UserRole.ROLE_USER, null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void createsActiveStaffWithProfileAndOneTimePassword() {
        login(1L, UserRole.ROLE_ADMIN);
        Hub hub = mock(Hub.class);
        when(hubs.findById(3L)).thenReturn(Optional.of(hub));
        when(users.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        String password = service.createStaff("ship_new", "Shipper Mới", "0912345678", "ship@example.test",
                UserRole.ROLE_SHIPPER, 3L, "DELIVERY_SHIPPER");
        assertThat(password).hasSize(12);
        verify(users).saveAndFlush(org.mockito.ArgumentMatchers.argThat(u -> u.getRole() == UserRole.ROLE_SHIPPER
                && u.isEnabled() && "+84912345678".equals(u.getPhone()) && password.equals(u.getPassword())));
        verify(profiles).save(any(EmployeeProfile.class));
    }

    @Test void hubRoleNeedsHubAndPositionAndDuplicatesAreRefused() {
        login(1L, UserRole.ROLE_ADMIN);
        assertThatThrownBy(() -> service.createStaff("hub_new", "Hub", "0912345678", "h@example.test",
                UserRole.ROLE_HUB_STAFF, null, null)).isInstanceOf(IllegalArgumentException.class);
        when(users.existsByUsername("taken")).thenReturn(true);
        assertThatThrownBy(() -> service.createStaff("taken", "X", "0912345678", "x@example.test",
                UserRole.ROLE_ADMIN, null, null)).isInstanceOf(IllegalArgumentException.class);
        verify(users, never()).saveAndFlush(any());
    }

    @Test void lockAndReassignAnotherAccount() {
        login(1L, UserRole.ROLE_ADMIN);
        User target = new User(9L, "staff", "x", UserRole.ROLE_HUB_STAFF, "ACTIVE");
        when(users.findById(9L)).thenReturn(Optional.of(target));
        service.setLocked(9L, true);
        assertThat(target.isAccountNonLocked()).isFalse();
        Hub hub = mock(Hub.class);
        when(hubs.findById(4L)).thenReturn(Optional.of(hub));
        EmployeeProfile profile = new EmployeeProfile(7L, target, 2L, "INBOUND", "ACTIVE");
        when(profiles.findFirstByUserId(9L)).thenReturn(Optional.of(profile));
        service.assign(9L, UserRole.ROLE_SHIPPER, 4L, "DELIVERY_SHIPPER");
        assertThat(target.getRole()).isEqualTo(UserRole.ROLE_SHIPPER);
        assertThat(profile.getPosition()).isEqualTo("DELIVERY_SHIPPER");
        assertThat(profile.getHub()).isSameAs(hub);
    }

    private static void login(Long id, UserRole role) {
        User actor = new User(id, "actor", "x", role, "ACTIVE");
        ReflectionTestUtils.setField(actor, "id", id);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, null, actor.getAuthorities()));
    }
}
