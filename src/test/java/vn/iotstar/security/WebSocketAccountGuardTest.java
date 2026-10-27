package vn.iotstar.security;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import vn.iotstar.entity.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketAccountGuardTest {
    @Test void previouslySubscribedSocketStopsReceivingAfterPasswordReset() {
        var users = mock(UserDetailsService.class); var guard = new WebSocketAccountGuard(users);
        var snapshot = new User(1L, "owner", "hash", UserRole.ROLE_USER, "ACTIVE");
        var current = new User(1L, "owner", "hash", UserRole.ROLE_USER, "ACTIVE");
        when(users.loadUserByUsername("owner")).thenReturn(current);
        var connect = StompHeaderAccessor.create(StompCommand.CONNECT); connect.setSessionId("session");
        connect.setUser(UsernamePasswordAuthenticationToken.authenticated(snapshot, null, snapshot.getAuthorities()));
        guard.preSend(MessageBuilder.createMessage(new byte[0], connect.getMessageHeaders()), null);
        var headers = StompHeaderAccessor.create(StompCommand.MESSAGE); headers.setSessionId("session");
        var message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
        assertThat(guard.outbound().preSend(message, null)).isNotNull();
        current.changePassword("changed"); assertThat(guard.outbound().preSend(message, null)).isNull();
    }
}
