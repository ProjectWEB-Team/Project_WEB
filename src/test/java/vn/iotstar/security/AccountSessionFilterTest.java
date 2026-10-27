package vn.iotstar.security;

import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import vn.iotstar.entity.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountSessionFilterTest {
    UserDetailsService users = mock(UserDetailsService.class);
    AccountSessionFilter filter = new AccountSessionFilter(users);
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    private User user() { return new User(1L, "owner", "hash", UserRole.ROLE_USER, "ACTIVE"); }
    @Test void passwordResetInvalidatesAnOldSession() throws Exception {
        User snapshot = user(), current = user(); current.changePassword("replacement-hash");
        when(users.loadUserByUsername("owner")).thenReturn(current);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(snapshot, null, snapshot.getAuthorities()));
        MockHttpServletRequest request = new MockHttpServletRequest(); MockHttpSession session = (MockHttpSession) request.getSession();
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull(); assertThat(session.isInvalid()).isTrue();
    }
    @Test void unlockingDoesNotRestoreARevokedSessionAndCurrentSessionsStillWork() {
        User snapshot = user(), current = user(); assertThat(AccountSessionFilter.matches(snapshot, current)).isTrue();
        current.changeStatus("LOCKED"); current.changeStatus("ACTIVE");
        assertThat(AccountSessionFilter.matches(snapshot, current)).isFalse();
        assertThat(AccountSessionFilter.matches(current, current)).isTrue();
    }
}
