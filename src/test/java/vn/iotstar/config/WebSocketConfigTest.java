package vn.iotstar.config;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebSocketConfigTest {
    private final WebSocketConfig.AuthenticatedShipmentStompInterceptor interceptor =
            new WebSocketConfig.AuthenticatedShipmentStompInterceptor();

    @Test
    void rejectsUnauthenticatedStompConnections() {
        assertThatThrownBy(() -> interceptor.preSend(unauthenticatedMessage(StompCommand.CONNECT), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void permitsOnlyThePrivateShipmentSubscriptionForAuthenticatedClients() {
        Message<?> allowed = message(StompCommand.SUBSCRIBE, WebSocketConfig.USER_SHIPMENT_DESTINATION);
        assertThat(interceptor.preSend(allowed, null)).isSameAs(allowed);

        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.SUBSCRIBE, "/user/queue/other"), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deniesClientInitiatedStompSendsInTheWalkingSkeleton() {
        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.SEND, "/app/shipments"), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Message<byte[]> message(StompCommand command, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setUser(new UsernamePasswordAuthenticationToken("owner", null));
        if (destination != null) {
            accessor.setDestination(destination);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> unauthenticatedMessage(StompCommand command) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
