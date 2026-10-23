package vn.iotstar.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.iotstar.dto.realtime.ShipmentCreatedEventResponse;

/** Sends only DTO events to the current actor's private STOMP destination after commit. */
@Component
public class StompShipmentEventPublisher implements ShipmentEventPublisher {
    public static final String USER_SHIPMENT_DESTINATION = "/queue/shipments";

    private final SimpMessagingTemplate messagingTemplate;

    public StompShipmentEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publishToUserAfterCommit(String username, ShipmentCreatedEventResponse event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Shipment realtime publication requires an active transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSendToUser(username, USER_SHIPMENT_DESTINATION, event);
            }
        });
    }
}
