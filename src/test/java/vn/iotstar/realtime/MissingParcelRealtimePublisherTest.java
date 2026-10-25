package vn.iotstar.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.iotstar.dto.realtime.OperationalActionEventResponse;
import vn.iotstar.dto.realtime.OperationalActionNotification;
import vn.iotstar.entity.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MissingParcelRealtimePublisherTest {
    final SimpMessagingTemplate messages=mock(SimpMessagingTemplate.class);
    final EntityManager entities=mock(EntityManager.class);
    final OperationalRealtimePublisher publisher=new OperationalRealtimePublisher(messages);
    final OperationalActionNotification notice=new OperationalActionNotification("NEW_INCIDENT",12L,"incident:77");
    final OperationalActionEventResponse payload=new OperationalActionEventResponse("NEW_INCIDENT","incident:77");
    final List<Set<Long>> queriedScopes=new ArrayList<>();
    final TransportTrip trip=mock(TransportTrip.class);

    @AfterEach void clearSynchronization(){TransactionSynchronizationManager.clear();}

    @SuppressWarnings("unchecked")
    void missingFixture() {
        ReflectionTestUtils.setField(publisher,"entityManager",entities);
        Hub root=hub(1L,"ROOT",null),origin=hub(11L,"ORIGIN",root),destination=hub(12L,"DESTINATION",root);
        when(entities.find(Hub.class,11L)).thenReturn(origin);
        when(entities.find(Hub.class,12L)).thenReturn(destination);
        Incident incident=Incident.create(101L,55L,12L,"MISSING_PARCEL","private camera evidence, phone and amounts",6L);
        incident.setId(77L);
        when(entities.find(Incident.class,77L)).thenReturn(incident);
        when(entities.find(TransportTrip.class,55L)).thenReturn(trip);
        when(trip.getOriginHub()).thenReturn(origin);when(trip.getDestinationHub()).thenReturn(destination);
        when(trip.getDriver()).thenReturn(user(21L,"assigned-driver",UserRole.ROLE_LINEHAUL_DRIVER,"ACTIVE"));
        TypedQuery<User> staff=mock(TypedQuery.class);
        when(entities.createQuery(contains("from EmployeeProfile"),eq(User.class))).thenReturn(staff);
        when(staff.setParameter(anyString(),any())).thenReturn(staff);
        when(staff.getResultList()).thenReturn(List.of(user(22L,"destination-inbound",UserRole.ROLE_HUB_STAFF,"ACTIVE")));
        when(entities.createQuery(contains("from User u"),eq(User.class))).thenAnswer(call->{
            String query=call.getArgument(0);
            assertThat(query).contains("p.status = 'ACTIVE'","p.hub.status = 'ACTIVE'","u.status = 'ACTIVE'","u.role = :manager","p.hub.id in :hubIds");
            TypedQuery<User> supervisors=mock(TypedQuery.class);
            List<User> results=new ArrayList<>();
            when(supervisors.setParameter(anyString(),any())).thenAnswer(parameter->{
                if("hubIds".equals(parameter.getArgument(0))){
                    Set<Long> scope=new LinkedHashSet<>((Set<Long>)parameter.getArgument(1));queriedScopes.add(scope);
                    if(scope.contains(11L))results.add(user(7L,"origin-manager",UserRole.ROLE_MANAGER,"ACTIVE"));
                    if(scope.contains(12L))results.add(user(8L,"destination-manager",UserRole.ROLE_MANAGER,"ACTIVE"));
                    if(scope.contains(13L))results.add(user(99L,"unrelated-manager",UserRole.ROLE_MANAGER,"ACTIVE"));
                    if(scope.contains(1L))results.add(user(9L,"root-manager",UserRole.ROLE_MANAGER,"ACTIVE"));
                    // Distinct entity instances for the same principal still send only once.
                    results.add(user(10L,"admin",UserRole.ROLE_ADMIN,"ACTIVE"));
                    results.add(user(14L,"inactive-manager",UserRole.ROLE_MANAGER,"LOCKED"));
                }
                return supervisors;
            });
            when(supervisors.getResultList()).thenReturn(results);return supervisors;
        });
    }

    @Test void missingReceiptNotifiesSiblingHubSupervisorsAndAssignedDriverOncePrivately() throws Exception {
        missingFixture();publisher.publishAfterCommit(notice);
        for(String username:List.of("origin-manager","destination-manager","root-manager","admin","assigned-driver","destination-inbound"))
            verify(messages,times(1)).convertAndSendToUser(username,"/queue/operations",payload);
        verifyNoMoreInteractions(messages);
        assertThat(queriedScopes).containsExactlyInAnyOrder(Set.of(11L,1L),Set.of(12L,1L));
        var json=new ObjectMapper().valueToTree(payload);
        assertThat(json.size()).isEqualTo(2);
        assertThat(json.toString()).doesNotContain("private","camera","phone","amounts","101","55");
    }

    @Test void mismatchedIncidentHubCannotSelectRecipientsFromSubmittedHub() {
        missingFixture();publisher.publishAfterCommit(new OperationalActionNotification("NEW_INCIDENT",13L,"incident:77"));
        verifyNoInteractions(messages);
    }

    @Test void mismatchedBoundTripDestinationFailsClosed() {
        missingFixture();when(trip.getDestinationHub()).thenReturn(hub(13L,"UNRELATED",null));
        publisher.publishAfterCommit(notice);verifyNoInteractions(messages);
    }

    @Test void inactiveOrWrongRoleAssignedUserIsNeverNotifiedAsDriver() {
        missingFixture();when(trip.getDriver()).thenReturn(user(21L,"assigned-driver",UserRole.ROLE_LINEHAUL_DRIVER,"LOCKED"));
        publisher.publishAfterCommit(notice);
        verify(messages,never()).convertAndSendToUser(eq("assigned-driver"),anyString(),any());
        clearInvocations(messages);
        when(trip.getDriver()).thenReturn(user(21L,"assigned-driver",UserRole.ROLE_USER,"ACTIVE"));
        publisher.publishAfterCommit(notice);
        verify(messages,never()).convertAndSendToUser(eq("assigned-driver"),anyString(),any());
    }

    @Test void otherPersistedIncidentTypesDoNotExpandToOriginOrDriver() {
        missingFixture();
        when(entities.find(Incident.class,77L)).thenReturn(Incident.create(null,55L,12L,"TRIP_DELAY","private delay",21L));
        publisher.publishAfterCommit(notice);
        assertThat(queriedScopes).containsExactly(Set.of(12L,1L));
        verify(messages,never()).convertAndSendToUser(eq("assigned-driver"),anyString(),any());
        verify(messages,never()).convertAndSendToUser(eq("origin-manager"),anyString(),any());
    }

    @Test void missingIncidentOrTripNeverBroadcastsToSubmittedHub() {
        missingFixture();when(entities.find(TransportTrip.class,55L)).thenReturn(null);
        publisher.publishAfterCommit(notice);verifyNoInteractions(messages);
        when(entities.find(Incident.class,77L)).thenReturn(null);
        publisher.publishAfterCommit(notice);verifyNoInteractions(messages);
        publisher.publishAfterCommit(new OperationalActionNotification("NEW_INCIDENT",12L,"incident:invalid"));
        verifyNoInteractions(messages);
    }

    @Test void actualSpringListenerDefersUntilCommitAndSuppressesRollbackAndNoTransaction() {
        missingFixture();
        try(var context=new AnnotationConfigApplicationContext()){
            context.registerBean(TransactionalEventListenerFactory.class);
            // Preconfigured test singleton: retain the mock EntityManager rather than starting JPA.
            context.getBeanFactory().registerSingleton("operationalRealtimePublisher",publisher);context.refresh();
            context.publishEvent(notice);verifyNoInteractions(messages);
            TransactionSynchronizationManager.initSynchronization();TransactionSynchronizationManager.setActualTransactionActive(true);
            context.publishEvent(notice);verifyNoInteractions(messages);
            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
            TransactionSynchronizationManager.getSynchronizations().forEach(s->s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            verifyNoInteractions(messages);TransactionSynchronizationManager.clear();
            TransactionSynchronizationManager.initSynchronization();TransactionSynchronizationManager.setActualTransactionActive(true);
            context.publishEvent(notice);verifyNoInteractions(messages);
            TransactionSynchronizationManager.getSynchronizations().forEach(s->s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
            verify(messages).convertAndSendToUser("assigned-driver","/queue/operations",payload);
        }
    }

    static User user(Long id,String name,UserRole role,String status){return new User(id,name,"hash",role,status);}
    static Hub hub(Long id,String name,Hub parent){Hub hub=Hub.active(name,name,2,"HUB","R","P");ReflectionTestUtils.setField(hub,"id",id);ReflectionTestUtils.setField(hub,"parentHub",parent);return hub;}
}
