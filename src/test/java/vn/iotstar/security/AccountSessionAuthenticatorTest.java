package vn.iotstar.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import vn.iotstar.entity.*;
import static org.assertj.core.api.Assertions.*;

class AccountSessionAuthenticatorTest {
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void establishesNormalUserPrincipalAndRotatesSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String oldId = request.getSession().getId();
        User user = new User(1L,"sender","hash",UserRole.ROLE_USER,"ACTIVE");
        new AccountSessionAuthenticator().signIn(user, request, new MockHttpServletResponse());
        assertThat(request.getSession().getId()).isNotEqualTo(oldId);
        assertThat(AuthenticatedActor.requireCurrent().userId()).isEqualTo(1L);
        assertThat(request.getSession().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNotNull();
    }
    @Test void inactiveOrLockedUserCannotObtainSession() {
        for (String status : new String[]{"INACTIVE","LOCKED"}) {
            assertThatThrownBy(() -> new AccountSessionAuthenticator().signIn(
                    new User(1L,"sender","hash",UserRole.ROLE_USER,status), new MockHttpServletRequest(), new MockHttpServletResponse()))
                    .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
        }
    }
}
