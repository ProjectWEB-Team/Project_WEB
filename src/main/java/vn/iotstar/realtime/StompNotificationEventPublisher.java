package vn.iotstar.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.iotstar.dto.realtime.NotificationEventResponse;
import vn.iotstar.security.AuthenticatedActor;

/** Sends only DTO notifications to the target user's private STOMP destination after commit. */
@Component
public class StompNotificationEventPublisher implements NotificationEventPublisher {

    public static final String USER_NOTIFICATION_DESTINATION = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public StompNotificationEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publishAfterCommit(AuthenticatedActor actor, NotificationEventResponse event) {
        publishToUserAfterCommit(actor.username(), event);
    }

    @Override
    public void publishToUserAfterCommit(String username, NotificationEventResponse event) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Notification recipient required");
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Notification publication requires an active transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSendToUser(username, USER_NOTIFICATION_DESTINATION, event);
            }
        });
    }
}
