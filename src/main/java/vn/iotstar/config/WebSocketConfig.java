package vn.iotstar.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import vn.iotstar.realtime.StompNotificationEventPublisher;
import vn.iotstar.realtime.StompShipmentEventPublisher;
import vn.iotstar.realtime.OperationalRealtimePublisher;

/** Minimal authenticated STOMP foundation for the Phase-01 private shipment notification. */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    public static final String STOMP_ENDPOINT = "/ws";
    public static final String USER_SHIPMENT_DESTINATION = "/user" + StompShipmentEventPublisher.USER_SHIPMENT_DESTINATION;
    public static final String USER_OPERATION_DESTINATION = "/user" + OperationalRealtimePublisher.USER_OPERATION_DESTINATION;
    public static final String USER_NOTIFICATION_DESTINATION = "/user" + StompNotificationEventPublisher.USER_NOTIFICATION_DESTINATION;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Same-origin is intentionally retained; no wildcard origin or anonymous endpoint is configured.
        registry.addEndpoint(STOMP_ENDPOINT);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new AuthenticatedShipmentStompInterceptor());
    }

    static final class AuthenticatedShipmentStompInterceptor implements ChannelInterceptor {
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
            StompCommand command = accessor.getCommand();
            if (command != StompCommand.CONNECT && command != StompCommand.SUBSCRIBE && command != StompCommand.SEND) {
                return message;
            }
            if (accessor.getUser() == null) {
                throw new AccessDeniedException("Authenticated STOMP connection required");
            }
            if (command == StompCommand.SEND) {
                throw new AccessDeniedException("Client STOMP sends are not enabled in the walking skeleton");
            }
            if (command == StompCommand.SUBSCRIBE
                    && !USER_SHIPMENT_DESTINATION.equals(accessor.getDestination())
                    && !USER_OPERATION_DESTINATION.equals(accessor.getDestination())
                    && !USER_NOTIFICATION_DESTINATION.equals(accessor.getDestination())) {
                throw new AccessDeniedException("STOMP subscription is outside the current actor scope");
            }
            return message;
        }
    }
}
