package vn.iotstar.realtime;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.iotstar.dto.realtime.OperationalActionEventResponse;
import vn.iotstar.dto.realtime.OperationalActionNotification;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;

import java.lang.reflect.Method;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class OperationalRealtimePublisherTest {
    @Test void ignoresAnyNotificationOutsideTheFiveD13Types() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        OperationalRealtimePublisher publisher = new OperationalRealtimePublisher(template);
        ReflectionTestUtils.setField(publisher, "entityManager", mock(EntityManager.class));
        publisher.publishAfterCommit(new OperationalActionNotification("CONTAINER_SCANNED", 1L, "container:1"));
        verifyNoInteractions(template);
    }

    @Test
    void blockedRouteIsSentOnlyToServerResolvedSupervisorsAfterCommit() throws Exception {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        EntityManager entityManager = mock(EntityManager.class);
        @SuppressWarnings("unchecked")
        TypedQuery<User> supervisors = mock(TypedQuery.class);
        User manager = new User(7L, "manager", "hash", UserRole.ROLE_MANAGER, "ACTIVE");
        User admin = new User(8L, "admin", "hash", UserRole.ROLE_ADMIN, "ACTIVE");
        var root=vn.iotstar.entity.Hub.active("ROOT","Root",1,"SOC","R","P");ReflectionTestUtils.setField(root,"id",1L);
        var leaf=vn.iotstar.entity.Hub.active("LEAF","Leaf",2,"HUB","R","P");ReflectionTestUtils.setField(leaf,"id",11L);ReflectionTestUtils.setField(leaf,"parentHub",root);
        when(entityManager.find(vn.iotstar.entity.Hub.class,11L)).thenReturn(leaf);
        when(entityManager.createQuery(contains("from User u"), eq(User.class))).thenReturn(supervisors);
        when(supervisors.setParameter(anyString(), any())).thenReturn(supervisors);
        when(supervisors.getResultList()).thenReturn(List.of(manager, admin));
        OperationalRealtimePublisher publisher = new OperationalRealtimePublisher(template);
        ReflectionTestUtils.setField(publisher, "entityManager", entityManager);

        publisher.publishAfterCommit(new OperationalActionNotification("BLOCKED_ROUTE", 11L, "routing-exception:77"));

        verify(template).convertAndSendToUser("manager", OperationalRealtimePublisher.USER_OPERATION_DESTINATION,
                new OperationalActionEventResponse("BLOCKED_ROUTE", "routing-exception:77"));
        verify(template).convertAndSendToUser("admin", OperationalRealtimePublisher.USER_OPERATION_DESTINATION,
                new OperationalActionEventResponse("BLOCKED_ROUTE", "routing-exception:77"));
        Method listener = OperationalRealtimePublisher.class.getDeclaredMethod("publishAfterCommit", OperationalActionNotification.class);
        assertThat(listener.getAnnotation(TransactionalEventListener.class).phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        verify(supervisors).setParameter("hubIds",new java.util.LinkedHashSet<>(List.of(11L,1L)));
        verify(entityManager).createQuery(contains("u.status = 'ACTIVE'"),eq(User.class));
    }
}
