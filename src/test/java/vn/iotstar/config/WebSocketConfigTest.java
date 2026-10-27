package vn.iotstar.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

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

        Message<?> operations = message(StompCommand.SUBSCRIBE, WebSocketConfig.USER_OPERATION_DESTINATION);
        assertThat(interceptor.preSend(operations, null)).isSameAs(operations);
    }

    @Test
    void deniesClientInitiatedStompSendsInTheWalkingSkeleton() {
        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.SEND, "/app/shipments"), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- HARD-01 interceptor matrix: command x destination x principal (plan 06-03) ---

    private static final List<String> ALLOWED = List.of(WebSocketConfig.USER_SHIPMENT_DESTINATION,
            WebSocketConfig.USER_OPERATION_DESTINATION, WebSocketConfig.USER_NOTIFICATION_DESTINATION);

    @Test
    void authenticatedConnectIsAllowed() {
        Message<?> connect = message(StompCommand.CONNECT, null);
        assertThat(interceptor.preSend(connect, null)).isSameAs(connect);
    }

    @ParameterizedTest
    @MethodSource("allowedDestinations")
    void eachPrivateUserQueueIsSubscribableOnlyWhenAuthenticated(String destination) {
        Message<?> subscribe = message(StompCommand.SUBSCRIBE, destination);
        assertThat(interceptor.preSend(subscribe, null)).isSameAs(subscribe);
        assertThatThrownBy(() -> interceptor.preSend(unauthenticated(StompCommand.SUBSCRIBE, destination), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    static List<String> allowedDestinations() { return ALLOWED; }

    @ParameterizedTest
    @ValueSource(strings = {"/user/queue/other", "/queue/shipments", "/queue/notifications", "/topic/shipments",
            "/app/shipments", "/user/other-user/queue/shipments", "/user/queue/shipments/extra"})
    void authenticatedSubscriptionOutsideTheAllowListIsDenied(String destination) {
        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.SUBSCRIBE, destination), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void subscriptionWithoutADestinationIsDenied() {
        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.SUBSCRIBE, null), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/app/shipments", "/user/queue/shipments", "/queue/notifications"})
    void everyClientSendIsDeniedWithOrWithoutAuthentication(String destination) {
        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.SEND, destination), null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> interceptor.preSend(unauthenticated(StompCommand.SEND, destination), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void disconnectAndUnsubscribePassThroughWithoutData() {
        for (StompCommand command : List.of(StompCommand.DISCONNECT, StompCommand.UNSUBSCRIBE)) {
            Message<?> frame = unauthenticated(command, null);
            assertThat(interceptor.preSend(frame, null)).isSameAs(frame);
        }
    }

    // --- Wiring ---

    @Test
    void accountRevocationAndDestinationScopeAreBothEnforced() {
        ChannelRegistration registration = mock(ChannelRegistration.class);
        new WebSocketConfig(mock(org.springframework.security.core.userdetails.UserDetailsService.class)).configureClientInboundChannel(registration);
        ArgumentCaptor<ChannelInterceptor[]> interceptors = ArgumentCaptor.forClass(ChannelInterceptor[].class);
        verify(registration).interceptors(interceptors.capture());
        assertThat(interceptors.getValue()).hasSize(2);
        assertThat(interceptors.getValue()[0]).isInstanceOf(vn.iotstar.security.WebSocketAccountGuard.class);
        assertThat(interceptors.getValue()[1]).isInstanceOf(WebSocketConfig.AuthenticatedShipmentStompInterceptor.class);
    }

    @Test
    void onlyTheSameOriginWsEndpointIsRegistered() {
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration endpoint = mock(StompWebSocketEndpointRegistration.class);
        when(registry.addEndpoint(any(String[].class))).thenReturn(endpoint);

        new WebSocketConfig(mock(org.springframework.security.core.userdetails.UserDetailsService.class)).registerStompEndpoints(registry);

        verify(registry).addEndpoint(WebSocketConfig.STOMP_ENDPOINT);
        verifyNoMoreInteractions(registry);
        verifyNoInteractions(endpoint);
        assertThat(WebSocketConfig.STOMP_ENDPOINT).isEqualTo("/ws");
    }

    @Test
    void brokerUsesOnlyQueueAppAndUserPrefixes() {
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);
        new WebSocketConfig(mock(org.springframework.security.core.userdetails.UserDetailsService.class)).configureMessageBroker(registry);
        verify(registry).enableSimpleBroker("/queue");
        verify(registry).setApplicationDestinationPrefixes("/app");
        verify(registry).setUserDestinationPrefix("/user");
        verifyNoMoreInteractions(registry);
    }

    private Message<byte[]> unauthenticated(StompCommand command, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) accessor.setDestination(destination);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
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
