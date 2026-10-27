package vn.iotstar.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.iotstar.dto.realtime.NotificationEventResponse;
import vn.iotstar.dto.realtime.OperationalActionEventResponse;
import vn.iotstar.dto.realtime.OperationalActionNotification;
import vn.iotstar.dto.realtime.ShipmentCreatedEventResponse;
import vn.iotstar.entity.Handover;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.TransportTrip;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.realtime.OperationalRealtimePublisher;
import vn.iotstar.realtime.StompNotificationEventPublisher;
import vn.iotstar.realtime.StompShipmentEventPublisher;

import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * HARD-02 / D-04: multi-session STOMP delivery in Maven. Sessions are distinct principals ("owner", "stranger",
 * hub staff); the per-user SimpMessagingTemplate call is the delivery boundary (D-07: no client or broker).
 */
class StompMultiSessionDeliveryTest {
    private final SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
    private final org.springframework.context.ApplicationEventPublisher emails = mock(org.springframework.context.ApplicationEventPublisher.class);
    private final WebSocketConfig.AuthenticatedShipmentStompInterceptor interceptor = new WebSocketConfig.AuthenticatedShipmentStompInterceptor();

    @BeforeEach void start() { TransactionSynchronizationManager.initSynchronization(); }
    @AfterEach void clear() { TransactionSynchronizationManager.clear(); }

    // --- Shipment events: one owner, one stranger ---

    private final ShipmentCreatedEventResponse shipmentEvent =
            new ShipmentCreatedEventResponse("UTX-1", ShipmentStatus.CREATED, "SHIPMENT_CREATED", LocalDateTime.of(2026, 10, 26, 9, 0));

    @Test void shipmentEventReachesOnlyTheOwnerAndOnlyAfterCommit() {
        new StompShipmentEventPublisher(template, emails).publishToUserAfterCommit("owner", shipmentEvent);
        verifyNoInteractions(template);

        complete(TransactionSynchronization.STATUS_COMMITTED);

        verify(template, times(1)).convertAndSendToUser("owner", StompShipmentEventPublisher.USER_SHIPMENT_DESTINATION, shipmentEvent);
        verify(template, never()).convertAndSendToUser(eq("stranger"), anyString(), any());
        verifyNoMoreInteractions(template);
    }

    @Test void shipmentEventIsNeverSentOnRollback() {
        new StompShipmentEventPublisher(template, emails).publishToUserAfterCommit("owner", shipmentEvent);
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verifyNoInteractions(template);
    }

    @Test void shipmentPayloadCarriesOnlyTheFourPublicComponents() {
        assertThat(components(ShipmentCreatedEventResponse.class)).containsExactly("trackingNumber", "currentStatus", "eventType", "occurredAt");
    }

    @Test void bothSessionsMaySubscribeToTheirOwnUserQueueButNotToARawOrForeignQueue() {
        assertThat("/user" + StompShipmentEventPublisher.USER_SHIPMENT_DESTINATION).isEqualTo(WebSocketConfig.USER_SHIPMENT_DESTINATION);
        for (String session : List.of("owner", "stranger")) {
            Message<?> own = subscribe(session, WebSocketConfig.USER_SHIPMENT_DESTINATION);
            assertThat(interceptor.preSend(own, null)).isSameAs(own);
        }
        assertThatThrownBy(() -> interceptor.preSend(subscribe("owner", "/queue/shipments"), null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> interceptor.preSend(subscribe("owner", "/user/stranger/queue/shipments"), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- Notifications ---

    @Test void twoNotificationRecipientsEachReceiveOnlyTheirOwnPayload() {
        var publisher = new StompNotificationEventPublisher(template, emails);
        var toOwner = new NotificationEventResponse("REFUND_COMPLETED", "PAYMENT#1", "owner message");
        var toStranger = new NotificationEventResponse("WITHDRAWAL_REJECTED", "WITHDRAWAL#2", "stranger message");
        publisher.publishToUserAfterCommit("owner", toOwner);
        publisher.publishToUserAfterCommit("stranger", toStranger);
        verifyNoInteractions(template);

        complete(TransactionSynchronization.STATUS_COMMITTED);

        verify(template).convertAndSendToUser("owner", StompNotificationEventPublisher.USER_NOTIFICATION_DESTINATION, toOwner);
        verify(template).convertAndSendToUser("stranger", StompNotificationEventPublisher.USER_NOTIFICATION_DESTINATION, toStranger);
        verifyNoMoreInteractions(template);
    }

    @Test void blankNotificationRecipientIsRejectedBeforeAnySynchronization() {
        var publisher = new StompNotificationEventPublisher(template, emails);
        assertThatThrownBy(() -> publisher.publishToUserAfterCommit(" ", new NotificationEventResponse("T", "R", "M")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    @Test void payloadsSerializeToTheMinimalFieldSets() throws Exception {
        assertThat(components(OperationalActionEventResponse.class)).containsExactly("type", "reference");
        var json = new ObjectMapper().valueToTree(new NotificationEventResponse("OUT_FOR_DELIVERY", "0909", "PAID", "500000",
                "SHIPMENT#1", "message", LocalDateTime.now()));
        assertThat(json.fieldNames()).toIterable().containsExactlyInAnyOrder("eventType", "reference", "message");
        assertThat(json.toString()).doesNotContain("0909", "500000", "PAID");
    }

    // --- Operational events: recipients by hub and position ---

    private final EntityManager entities = mock(EntityManager.class);
    private final OperationalRealtimePublisher operations = new OperationalRealtimePublisher(template);
    private final User outbound11 = user(1L, "outbound-11", UserRole.ROLE_HUB_STAFF, "ACTIVE");
    private final User inbound12 = user(2L, "inbound-12", UserRole.ROLE_HUB_STAFF, "ACTIVE");
    private final User staff13 = user(3L, "staff-13", UserRole.ROLE_HUB_STAFF, "ACTIVE");
    private final User locked11 = user(4L, "locked-11", UserRole.ROLE_HUB_STAFF, "LOCKED");
    private final User driver = user(5L, "assigned-driver", UserRole.ROLE_LINEHAUL_DRIVER, "ACTIVE");

    @SuppressWarnings("unchecked")
    private void operationalHarness() {
        ReflectionTestUtils.setField(operations, "entityManager", entities);
        // Staff returned per (hubId, position) so a wrong-hub query would surface the wrong user.
        Map<String, List<User>> staff = Map.of("11:OUTBOUND", List.of(outbound11, locked11), "12:INBOUND", List.of(inbound12),
                "13:OUTBOUND", List.of(staff13), "13:INBOUND", List.of(staff13));
        when(entities.createQuery(contains("from EmployeeProfile"), eq(User.class))).thenAnswer(call -> {
            TypedQuery<User> query = mock(TypedQuery.class);
            Map<String, Object> parameters = new HashMap<>();
            when(query.setParameter(anyString(), any())).thenAnswer(p -> { parameters.put(p.getArgument(0), p.getArgument(1)); return query; });
            when(query.getResultList()).thenAnswer(r -> staff.getOrDefault(parameters.get("hubId") + ":" + parameters.get("position"), List.of()));
            return query;
        });
        Handover handover = mock(Handover.class);
        when(handover.getReceiverDriver()).thenReturn(driver);
        when(entities.find(Handover.class, 30L)).thenReturn(handover);
        TransportTrip trip = mock(TransportTrip.class);
        when(trip.getDriver()).thenReturn(driver);
        when(entities.find(TransportTrip.class, 40L)).thenReturn(trip);
    }

    @Test void handoverAcceptedReachesOnlyOutboundStaffOfThatHubAndTheReceivingDriver() {
        operationalHarness();
        operations.publishAfterCommit(new OperationalActionNotification("HANDOVER_ACCEPTED", 11L, "handover:30"));

        var payload = new OperationalActionEventResponse("HANDOVER_ACCEPTED", "handover:30");
        verify(template, times(1)).convertAndSendToUser("outbound-11", OperationalRealtimePublisher.USER_OPERATION_DESTINATION, payload);
        verify(template, times(1)).convertAndSendToUser("assigned-driver", OperationalRealtimePublisher.USER_OPERATION_DESTINATION, payload);
        verifyNoMoreInteractions(template);
    }

    @Test void destinationArrivalAndInboundReadyReachOnlyInboundStaffOfTheDestinationAndTheTripDriver() {
        operationalHarness();
        for (String type : List.of("DESTINATION_ARRIVAL", "INBOUND_READY_CONTAINER")) {
            clearInvocations(template);
            operations.publishAfterCommit(new OperationalActionNotification(type, 12L, "trip:40"));
            var payload = new OperationalActionEventResponse(type, "trip:40");
            verify(template, times(1)).convertAndSendToUser("inbound-12", OperationalRealtimePublisher.USER_OPERATION_DESTINATION, payload);
            verify(template, times(1)).convertAndSendToUser("assigned-driver", OperationalRealtimePublisher.USER_OPERATION_DESTINATION, payload);
            verifyNoMoreInteractions(template);
        }
    }

    @Test void destinationArrivalThroughTheSpringListenerWaitsForCommitAndIsSilentOnRollback() {
        operationalHarness();
        var notice = new OperationalActionNotification("DESTINATION_ARRIVAL", 12L, "trip:40");
        TransactionSynchronizationManager.clear();
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(TransactionalEventListenerFactory.class);
            context.getBeanFactory().registerSingleton("operationalRealtimePublisher", operations);
            context.refresh();

            TransactionSynchronizationManager.initSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(true);
            context.publishEvent(notice);
            verifyNoInteractions(template);
            complete(TransactionSynchronization.STATUS_ROLLED_BACK);
            verifyNoInteractions(template);
            TransactionSynchronizationManager.clear();

            TransactionSynchronizationManager.initSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(true);
            context.publishEvent(notice);
            complete(TransactionSynchronization.STATUS_COMMITTED);
            verify(template, times(1)).convertAndSendToUser(eq("inbound-12"), eq(OperationalRealtimePublisher.USER_OPERATION_DESTINATION), any());
        }
    }

    @Test void aNonDriverAccountOnTheHandoverOrTripIsNeverNotifiedAsDriver() {
        operationalHarness();
        User notDriver = user(6L, "not-driver", UserRole.ROLE_USER, "ACTIVE");
        when(entities.find(Handover.class, 30L).getReceiverDriver()).thenReturn(notDriver);
        when(entities.find(TransportTrip.class, 40L).getDriver()).thenReturn(notDriver);
        operations.publishAfterCommit(new OperationalActionNotification("HANDOVER_ACCEPTED", 11L, "handover:30"));
        operations.publishAfterCommit(new OperationalActionNotification("DESTINATION_ARRIVAL", 12L, "trip:40"));
        verify(template, never()).convertAndSendToUser(eq("not-driver"), anyString(), any());
    }

    // --- Destination and principal-name invariants ---

    @Test void everyPublisherDestinationIsSubscribableOnlyThroughTheUserPrefix() {
        for (String destination : List.of(StompShipmentEventPublisher.USER_SHIPMENT_DESTINATION,
                OperationalRealtimePublisher.USER_OPERATION_DESTINATION, StompNotificationEventPublisher.USER_NOTIFICATION_DESTINATION)) {
            Message<?> userQueue = subscribe("owner", "/user" + destination);
            assertThat(interceptor.preSend(userQueue, null)).as(destination).isSameAs(userQueue);
            assertThatThrownBy(() -> interceptor.preSend(subscribe("owner", destination), null)).as(destination)
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test void theSessionPrincipalNameIsTheUsernamePublishersAddress() {
        User owner = user(1L, "owner", UserRole.ROLE_USER, "ACTIVE");
        assertThat(new UsernamePasswordAuthenticationToken(owner, null, owner.getAuthorities()).getName()).isEqualTo(owner.getUsername());
    }

    private static void complete(int status) {
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        if (status == TransactionSynchronization.STATUS_COMMITTED) synchronizations.forEach(TransactionSynchronization::afterCommit);
        synchronizations.forEach(s -> s.afterCompletion(status));
    }

    private static Message<byte[]> subscribe(String principal, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null));
        accessor.setDestination(destination);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static List<String> components(Class<? extends Record> type) {
        return Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
    }

    private static User user(Long id, String username, UserRole role, String status) {
        return new User(id, username, "hash", role, status);
    }
}
