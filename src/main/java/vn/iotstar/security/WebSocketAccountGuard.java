package vn.iotstar.security;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import vn.iotstar.entity.User;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Revoked sockets cannot keep receiving private messages after the HTTP session is invalidated. */
public class WebSocketAccountGuard implements ChannelInterceptor {
    private final UserDetailsService users;
    private final Map<String, User> sessions = new ConcurrentHashMap<>();
    public WebSocketAccountGuard(UserDetailsService users) { this.users = users; }
    @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var headers = StompHeaderAccessor.wrap(message);
        if (headers.getCommand() == StompCommand.DISCONNECT) {
            if (headers.getSessionId() != null) sessions.remove(headers.getSessionId());
            return message;
        }
        if (headers.getCommand() == StompCommand.CONNECT || headers.getCommand() == StompCommand.SUBSCRIBE) {
            if (!(headers.getUser() instanceof Authentication authentication)
                    || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User user)
                    || headers.getSessionId() == null || !valid(user)) throw new AccessDeniedException("Account session expired");
            if (headers.getCommand() == StompCommand.CONNECT) sessions.put(headers.getSessionId(), user);
        }
        return message;
    }
    public ChannelInterceptor outbound() {
        return new ChannelInterceptor() {
            @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
                var headers = StompHeaderAccessor.wrap(message);
                if (headers.getMessageType() != SimpMessageType.MESSAGE) return message;
                User user = headers.getSessionId() == null ? null : sessions.get(headers.getSessionId());
                return user != null && valid(user) ? message : null;
            }
        };
    }
    @EventListener public void disconnected(SessionDisconnectEvent event) { sessions.remove(event.getSessionId()); }
    private boolean valid(User snapshot) {
        try { return AccountSessionFilter.matches(snapshot, users.loadUserByUsername(snapshot.getUsername())); }
        catch (AuthenticationException exception) { return false; }
    }
}
