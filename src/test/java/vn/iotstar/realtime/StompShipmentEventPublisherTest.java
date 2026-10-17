package vn.iotstar.realtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.iotstar.dto.realtime.ShipmentCreatedEventResponse;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.security.AuthenticatedActor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StompShipmentEventPublisherTest {
    @Mock private SimpMessagingTemplate messagingTemplate;
    private StompShipmentEventPublisher publisher;

    @BeforeEach
    void startTransactionSynchronization() {
        publisher = new StompShipmentEventPublisher(messagingTemplate);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void sendsOnlyTheSafeDtoToTheCurrentActorsPrivateDestinationAfterCommit() {
        AuthenticatedActor actor = new AuthenticatedActor(7L, "owner", java.util.Set.of());
        ShipmentCreatedEventResponse event = new ShipmentCreatedEventResponse("UTX-001", ShipmentStatus.CREATED,
                "SHIPMENT_CREATED", LocalDateTime.of(2026, 10, 1, 12, 0));

        publisher.publishAfterCommit(actor, event);

        verifyNoInteractions(messagingTemplate);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSendToUser(eq("owner"), eq(StompShipmentEventPublisher.USER_SHIPMENT_DESTINATION),
                payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).isSameAs(event).isInstanceOf(ShipmentCreatedEventResponse.class);
        assertThat(ShipmentCreatedEventResponse.class.getRecordComponents())
                .extracting(component -> component.getName())
                .containsExactly("trackingNumber", "currentStatus", "eventType", "occurredAt");
    }

    @Test
    void rejectsImmediatePublishingOutsideAnActiveTransaction() {
        TransactionSynchronizationManager.clearSynchronization();
        AuthenticatedActor actor = new AuthenticatedActor(7L, "owner", java.util.Set.of());
        ShipmentCreatedEventResponse event = new ShipmentCreatedEventResponse("UTX-001", ShipmentStatus.CREATED,
                "SHIPMENT_CREATED", LocalDateTime.now());

        assertThatThrownBy(() -> publisher.publishAfterCommit(actor, event)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(messagingTemplate);
    }
}
