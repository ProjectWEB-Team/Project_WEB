package vn.iotstar.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.context.ApplicationEventPublisher;
import vn.iotstar.service.auth.AccountEmailNotification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.iotstar.dto.realtime.ShipmentCreatedEventResponse;

/** Sends only DTO events to the current actor's private STOMP destination after commit. */
@Component
public class StompShipmentEventPublisher implements ShipmentEventPublisher {
    public static final String USER_SHIPMENT_DESTINATION = "/queue/shipments";

    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher events;

    public StompShipmentEventPublisher(SimpMessagingTemplate messagingTemplate, ApplicationEventPublisher events) {
        this.messagingTemplate = messagingTemplate;
        this.events = events;
    }

    @Override
    public void publishToUserAfterCommit(String username, ShipmentCreatedEventResponse event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Shipment realtime publication requires an active transaction");
        }
        events.publishEvent(new AccountEmailNotification(username, "UTEExpress - Cập nhật đơn hàng",
                "Đơn hàng " + event.trackingNumber() + " có cập nhật mới."));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSendToUser(username, USER_SHIPMENT_DESTINATION, event);
            }
        });
    }
}
