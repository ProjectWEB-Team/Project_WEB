package vn.iotstar.realtime;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.iotstar.dto.realtime.OperationalActionEventResponse;
import vn.iotstar.dto.realtime.OperationalActionNotification;
import vn.iotstar.entity.Handover;
import vn.iotstar.entity.TransportTrip;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** D-13: an after-commit, server-targeted operational notification boundary. */
@Component
public class OperationalRealtimePublisher {
    public static final String USER_OPERATION_DESTINATION = "/queue/operations";
    private static final Set<String> ALLOWED_TYPES = Set.of("HANDOVER_ACCEPTED", "DESTINATION_ARRIVAL", "INBOUND_READY_CONTAINER", "NEW_INCIDENT", "BLOCKED_ROUTE");
    @PersistenceContext private EntityManager entityManager;
    private final SimpMessagingTemplate messagingTemplate;

    public OperationalRealtimePublisher(SimpMessagingTemplate messagingTemplate) { this.messagingTemplate = messagingTemplate; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(OperationalActionNotification notification) {
        if (!ALLOWED_TYPES.contains(notification.type())) return;
        OperationalActionEventResponse payload = new OperationalActionEventResponse(notification.type(), notification.diagnosticReference());
        recipients(notification).forEach(user -> messagingTemplate.convertAndSendToUser(user.getUsername(), USER_OPERATION_DESTINATION, payload));
    }

    private Set<User> recipients(OperationalActionNotification notification) {
        Set<User> recipients = new LinkedHashSet<>();
        switch (notification.type()) {
            case "HANDOVER_ACCEPTED" -> {
                recipients.addAll(staffAtHub(notification.currentHubId(), "OUTBOUND"));
                handover(notification.diagnosticReference()).ifPresent(handover -> recipients.add(handover.getReceiverDriver()));
            }
            case "DESTINATION_ARRIVAL", "INBOUND_READY_CONTAINER" -> {
                recipients.addAll(staffAtHub(notification.currentHubId(), "INBOUND"));
                trip(notification.diagnosticReference()).ifPresent(trip -> recipients.add(trip.getDriver()));
            }
            case "BLOCKED_ROUTE" -> recipients.addAll(supervisorsAtHub(notification.currentHubId()));
            case "NEW_INCIDENT" -> {
                recipients.addAll(staffAtHub(notification.currentHubId(), "INBOUND"));
                recipients.addAll(supervisorsAtHub(notification.currentHubId()));
            }
            default -> { }
        }
        return recipients;
    }

    private List<User> staffAtHub(Long hubId, String position) {
        return entityManager.createQuery("select p.user from EmployeeProfile p where p.hub.id = :hubId and p.status = 'ACTIVE' and (p.position = :position or p.position = 'GENERAL_STAFF')", User.class)
                .setParameter("hubId", hubId).setParameter("position", position).getResultList();
    }
    private List<User> supervisorsAtHub(Long hubId) {
        return entityManager.createQuery("select u from User u left join EmployeeProfile p on p.user = u and p.status = 'ACTIVE' where u.role = :admin or (u.role = :manager and p.hub.id = :hubId)", User.class)
                .setParameter("admin", UserRole.ROLE_ADMIN).setParameter("manager", UserRole.ROLE_MANAGER).setParameter("hubId", hubId).getResultList();
    }
    private java.util.Optional<Handover> handover(String reference) {
        return numericId(reference, "handover:").map(id -> entityManager.find(Handover.class, id));
    }
    private java.util.Optional<TransportTrip> trip(String reference) {
        return numericId(reference, "trip:").map(id -> entityManager.find(TransportTrip.class, id));
    }
    private java.util.Optional<Long> numericId(String reference, String prefix) {
        if (reference == null || !reference.startsWith(prefix)) return java.util.Optional.empty();
        try { return java.util.Optional.of(Long.parseLong(reference.substring(prefix.length()))); }
        catch (NumberFormatException ignored) { return java.util.Optional.empty(); }
    }
}
