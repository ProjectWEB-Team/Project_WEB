package vn.iotstar.realtime;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.dto.realtime.OperationalActionNotification;

import static org.mockito.Mockito.*;

class OperationalRealtimePublisherTest {
    @Test void ignoresAnyNotificationOutsideTheFiveD13Types() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        OperationalRealtimePublisher publisher = new OperationalRealtimePublisher(template);
        ReflectionTestUtils.setField(publisher, "entityManager", mock(EntityManager.class));
        publisher.publishAfterCommit(new OperationalActionNotification("CONTAINER_SCANNED", 1L, "container:1"));
        verifyNoInteractions(template);
    }
}
