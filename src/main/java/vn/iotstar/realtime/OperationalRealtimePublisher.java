package vn.iotstar.realtime;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.Incident;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;

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
        recipients(notification).stream().map(User::getUsername).distinct()
                .forEach(username -> messagingTemplate.convertAndSendToUser(username, USER_OPERATION_DESTINATION, payload));
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
                Incident incident=numericId(notification.diagnosticReference(), "incident:")
                        .map(id -> entityManager.find(Incident.class,id)).orElse(null);
                if (incident==null || incident.getHubId()==null
                        || !Objects.equals(incident.getHubId(),notification.currentHubId())) break;
                if ("MISSING_PARCEL".equals(incident.getType())) {
                    TransportTrip boundTrip=incident.getTripId()==null ? null : entityManager.find(TransportTrip.class,incident.getTripId());
                    if (incident.getShipmentId()==null || boundTrip==null || boundTrip.getOriginHub()==null
                            || boundTrip.getDestinationHub()==null
                            || !Objects.equals(incident.getHubId(),boundTrip.getDestinationHub().getId())) break;
                    recipients.addAll(supervisorsAtHub(boundTrip.getOriginHub().getId()));
                    User driver=boundTrip.getDriver();
                    if (driver!=null && driver.getRole()==UserRole.ROLE_LINEHAUL_DRIVER) recipients.add(driver);
                }
                recipients.addAll(staffAtHub(incident.getHubId(), "INBOUND"));
                recipients.addAll(supervisorsAtHub(incident.getHubId()));
            }
            default -> { }
        }
        recipients.removeIf(user -> user == null || !"ACTIVE".equals(user.getStatus()));
        return recipients;
    }

    private List<User> staffAtHub(Long hubId, String position) {
        return entityManager.createQuery("select p.user from EmployeeProfile p where p.hub.id = :hubId and p.hub.status = 'ACTIVE' and p.user.status = 'ACTIVE' and p.status = 'ACTIVE' and (p.position = :position or p.position = 'GENERAL_STAFF')", User.class)
                .setParameter("hubId", hubId).setParameter("position", position).getResultList();
    }
    private List<User> supervisorsAtHub(Long hubId) {
        Set<Long> ancestors=new LinkedHashSet<>();
        Hub hub=hubId==null ? null : entityManager.find(Hub.class,hubId);
        while (hub!=null && "ACTIVE".equals(hub.getStatus()) && ancestors.add(hub.getId())) hub=hub.getParentHub();
        // A nonmatching sentinel keeps JPQL IN portable for an unassigned/invalid hub.
        if (ancestors.isEmpty()) ancestors.add(-1L);
        return entityManager.createQuery("select u from User u left join EmployeeProfile p on p.user = u and p.status = 'ACTIVE' where u.status = 'ACTIVE' and (u.role = :admin or (u.role = :manager and p.hub.status = 'ACTIVE' and p.hub.id in :hubIds))", User.class)
                .setParameter("admin", UserRole.ROLE_ADMIN).setParameter("manager", UserRole.ROLE_MANAGER).setParameter("hubIds", ancestors).getResultList();
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
