package vn.iotstar.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.hub.DestinationReceiptCommand;
import vn.iotstar.dto.hub.DestinationReceiptResponse;
import vn.iotstar.dto.hub.DestinationReconciliationCommand;
import vn.iotstar.dto.realtime.OperationalActionNotification;
import vn.iotstar.entity.ContainerItem;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Handover;
import vn.iotstar.entity.Incident;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.HubOperationAccessDeniedException;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.repository.DestinationReceiptRepository;
import vn.iotstar.repository.HubOperationsRepository;
import vn.iotstar.repository.OperationalIncidentRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.DestinationReceiptService;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Transactional destination-inbound authority for physical receipt, never driver arrival. */
@Service
public class DestinationReceiptServiceImpl implements DestinationReceiptService {
    private final DestinationReceiptRepository receiptRepository;
    private final HubOperationsRepository hubOperationsRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final OperationalIncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public DestinationReceiptServiceImpl(DestinationReceiptRepository receiptRepository, HubOperationsRepository hubOperationsRepository,
                                         ShipmentEventRepository shipmentEventRepository, OperationalIncidentRepository incidentRepository,
                                         UserRepository userRepository, ApplicationEventPublisher applicationEventPublisher) {
        this.receiptRepository = receiptRepository;
        this.hubOperationsRepository = hubOperationsRepository;
        this.shipmentEventRepository = shipmentEventRepository;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override @Transactional
    public DestinationReceiptResponse receiveItem(DestinationReceiptCommand command) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        EmployeeProfile profile = requireInboundProfile(actor);
        Handover handover = requiredArrivedHandover(command.handoverId(), profile);
        receiptRepository.findHandoverContainer(handover.getId(), command.containerId()).orElseThrow(HubOperationAccessDeniedException::new);
        ContainerItem item = receiptRepository.findItemForUpdate(command.containerId(), command.shipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Container item"));
        if (item.isMissing()) throw new HubOperationAccessDeniedException();
        boolean alreadyReceived = item.isReceived();
        if (!alreadyReceived) recordReceipt(item, profile, actor);
        settleIfAllItemsReconciled(handover);
        return new DestinationReceiptResponse(handover.getId(), command.containerId(), command.shipmentId(), alreadyReceived,
                item.getShipment().getCurrentStatus().name());
    }

    @Override @Transactional
    public void reconcileContainer(DestinationReconciliationCommand command) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        EmployeeProfile profile = requireInboundProfile(actor);
        Handover handover = requiredArrivedHandover(command.handoverId(), profile);
        receiptRepository.findHandoverContainer(handover.getId(), command.containerId()).orElseThrow(HubOperationAccessDeniedException::new);
        Set<Long> actual = new HashSet<>(command.receivedShipmentIds());
        if (actual.size() != command.receivedShipmentIds().size()) throw new HubOperationAccessDeniedException();
        User actorUser = userRepository.getReferenceById(actor.userId());
        for (ContainerItem item : receiptRepository.findItemsForContainerForUpdate(command.containerId())) {
            if (actual.remove(item.getShipment().getId())) {
                if (!item.isReceived()) recordReceipt(item, profile, actor);
            } else if (!item.isReceived() && !item.isMissing()) {
                item.markMissing(actorUser);
                shipmentEventRepository.save(ShipmentEvent.missingParcel(item.getShipment(), actorUser, profile.getHub()));
                Incident incident = incidentRepository.save(Incident.missingParcel(handover.getTransportTrip(), actorUser,
                        "receipt:handover:" + handover.getId() + ":container:" + command.containerId() + ":shipment:" + item.getShipment().getId()));
                applicationEventPublisher.publishEvent(new OperationalActionNotification("NEW_INCIDENT", profile.getHub().getId(), "incident:" + incident.getId()));
            }
        }
        if (!actual.isEmpty()) throw new HubOperationAccessDeniedException();
        settleIfAllItemsReconciled(handover);
    }

    private void recordReceipt(ContainerItem item, EmployeeProfile profile, AuthenticatedActor actor) {
        User user = userRepository.getReferenceById(actor.userId());
        item.receive(user);
        item.getShipment().receiveAt(profile.getHub());
        shipmentEventRepository.save(ShipmentEvent.arrivedAtHub(item.getShipment(), user, profile.getHub()));
    }

    private Handover requiredArrivedHandover(Long handoverId, EmployeeProfile profile) {
        Handover handover = receiptRepository.findArrivedHandoverForUpdate(handoverId).orElseThrow(() -> new ResourceNotFoundException("Handover"));
        if (!Objects.equals(profile.getHub().getId(), handover.getToHub().getId()) || handover.getTransportTrip() == null
                || !"ARRIVED".equals(handover.getTransportTrip().getStatus())
                || !("ACCEPTED".equals(handover.getStatus()) || "COMPLETED".equals(handover.getStatus()) || "DISCREPANCY".equals(handover.getStatus()))) {
            throw new HubOperationAccessDeniedException();
        }
        return handover;
    }

    private void settleIfAllItemsReconciled(Handover handover) {
        if (!"ACCEPTED".equals(handover.getStatus())) return;
        var items = receiptRepository.findHandoverContainers(handover.getId()).stream()
                .flatMap(container -> receiptRepository.findItemsForContainerForUpdate(container.getContainer().getId()).stream()).toList();
        if (!items.isEmpty() && items.stream().allMatch(item -> item.isReceived() || item.isMissing())) {
            handover.completeDestinationReceipt(items.stream().anyMatch(ContainerItem::isMissing));
        }
    }

    private EmployeeProfile requireInboundProfile(AuthenticatedActor actor) {
        if (!actor.hasRole(UserRole.ROLE_HUB_STAFF)) throw new HubOperationAccessDeniedException();
        EmployeeProfile profile = hubOperationsRepository.findActiveProfileByUserId(actor.userId()).orElseThrow(HubOperationAccessDeniedException::new);
        if (!"INBOUND".equals(profile.getPosition()) && !"GENERAL_STAFF".equals(profile.getPosition())) throw new HubOperationAccessDeniedException();
        return profile;
    }
}
