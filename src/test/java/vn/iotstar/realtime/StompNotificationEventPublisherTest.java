package vn.iotstar.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.iotstar.dto.realtime.NotificationEventResponse;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class StompNotificationEventPublisherTest {
    SimpMessagingTemplate template=mock(SimpMessagingTemplate.class);
    StompNotificationEventPublisher publisher=new StompNotificationEventPublisher(template);
    NotificationEventResponse event=new NotificationEventResponse("REFUND_COMPLETED","PAYMENT#1","Refund completed");
    @AfterEach void clear(){if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.clearSynchronization();}
    @Test void targetsOwnerOnlyAfterCommit(){
        TransactionSynchronizationManager.initSynchronization();publisher.publishToUserAfterCommit("owner",event);verifyNoInteractions(template);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(template).convertAndSendToUser("owner",StompNotificationEventPublisher.USER_NOTIFICATION_DESTINATION,event);verifyNoMoreInteractions(template);
    }
    @Test void rollbackNeverSends(){
        TransactionSynchronizationManager.initSynchronization();publisher.publishToUserAfterCommit("owner",event);
        TransactionSynchronizationManager.getSynchronizations().forEach(s->s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));verifyNoInteractions(template);
    }
    @Test void rejectsPublicationWithoutTransaction(){assertThatThrownBy(()->publisher.publishToUserAfterCommit("owner",event)).isInstanceOf(IllegalStateException.class);verifyNoInteractions(template);}
    @Test void payloadHasOnlySafeThreeFields()throws Exception{
        var json=new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(new NotificationEventResponse("REFUND", "tracking", "status", "900000", "PAYMENT#1", "Updated",null)));
        assertThat(json.size()).isEqualTo(3);assertThat(json.has("eventType")).isTrue();assertThat(json.has("reference")).isTrue();assertThat(json.has("message")).isTrue();
        assertThat(json.toString()).doesNotContain("900000","tracking","status");
    }
}
