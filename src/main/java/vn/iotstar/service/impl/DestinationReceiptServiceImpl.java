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
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.Hub;
import vn.iotstar.dto.hub.MissingParcelResolutionCommand;
import vn.iotstar.dto.hub.MissingParcelCaseResponse;
import vn.iotstar.exception.HubOperationAccessDeniedException;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.repository.DestinationReceiptRepository;
import vn.iotstar.repository.HubOperationsRepository;
import vn.iotstar.repository.OperationalIncidentRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.repository.ManagerOperationsRepository;
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
    private final ManagerOperationsRepository managerRepository;

    public DestinationReceiptServiceImpl(DestinationReceiptRepository receiptRepository, HubOperationsRepository hubOperationsRepository,
                                         ShipmentEventRepository shipmentEventRepository, OperationalIncidentRepository incidentRepository,
                                         UserRepository userRepository, ApplicationEventPublisher applicationEventPublisher,
                                         ManagerOperationsRepository managerRepository) {
        this.receiptRepository = receiptRepository;
        this.hubOperationsRepository = hubOperationsRepository;
        this.shipmentEventRepository = shipmentEventRepository;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.managerRepository = managerRepository;
    }

    @Override @Transactional
    public DestinationReceiptResponse receiveItem(DestinationReceiptCommand command) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        EmployeeProfile profile = requireInboundProfile(actor);
        Handover handover = requiredArrivedHandover(command.handoverId(), profile);
        requireScannedContainer(handover, command.containerId());
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
        requireScannedContainer(handover, command.containerId());
        Set<Long> actual = new HashSet<>(command.receivedShipmentIds());
        if (actual.size() != command.receivedShipmentIds().size()) throw new HubOperationAccessDeniedException();
        var items = receiptRepository.findItemsForContainerForUpdate(command.containerId());
        if (items.isEmpty() || !items.stream().map(item -> item.getShipment().getId()).toList().containsAll(actual))
            throw new HubOperationAccessDeniedException();
        for (ContainerItem item : items) {
            if (actual.contains(item.getShipment().getId()) && item.isMissing()) throw new HubOperationAccessDeniedException();
            if (!item.isReceived() && !item.isMissing()) requireTransitShipment(item);
        }
        User actorUser = userRepository.getReferenceById(actor.userId());
        for (ContainerItem item : items) {
            if (actual.remove(item.getShipment().getId())) {
                if (!item.isReceived()) recordReceipt(item, profile, actor);
            } else if (!item.isReceived() && !item.isMissing()) {
                item.markMissing(actorUser);
                item.getShipment().setOnHold(true);
                shipmentEventRepository.save(ShipmentEvent.missingParcel(item.getShipment(), actorUser, profile.getHub()));
                Incident incident = incidentRepository.save(Incident.missingParcel(item.getShipment(), handover.getTransportTrip(), actorUser,
                        profile.getHub(), "receipt:handover:" + handover.getId() + ":container:" + command.containerId() + ":shipment:" + item.getShipment().getId()));
                applicationEventPublisher.publishEvent(new OperationalActionNotification("NEW_INCIDENT", profile.getHub().getId(), "incident:" + incident.getId()));
            }
        }
        if (!actual.isEmpty()) throw new HubOperationAccessDeniedException();
        settleIfAllItemsReconciled(handover);
    }

    private void recordReceipt(ContainerItem item, EmployeeProfile profile, AuthenticatedActor actor) {
        requireTransitShipment(item);
        User user = userRepository.getReferenceById(actor.userId());
        item.receive(user);
        receiveAt(item, profile.getHub());
        shipmentEventRepository.save(ShipmentEvent.arrivedAtHub(item.getShipment(), user, profile.getHub()));
    }

    private Handover requiredArrivedHandover(Long handoverId, EmployeeProfile profile) {
        Handover handover = receiptRepository.findArrivedHandoverForUpdate(handoverId).orElseThrow(() -> new ResourceNotFoundException("Handover"));
        if (!Objects.equals(profile.getHub().getId(), handover.getToHub().getId()) || handover.getTransportTrip() == null
                || !("ARRIVED".equals(handover.getTransportTrip().getStatus()) || handover.getTransportTrip().isCompleted())
                || !("IN_TRANSIT".equals(handover.getStatus()) || "COMPLETED".equals(handover.getStatus()) || "COMPLETED_WITH_DISCREPANCY".equals(handover.getStatus()))) {
            throw new HubOperationAccessDeniedException();
        }
        return handover;
    }

    private void settleIfAllItemsReconciled(Handover handover) {
        if (!"IN_TRANSIT".equals(handover.getStatus())) return;
        var containers = receiptRepository.findHandoverContainers(handover.getId());
        boolean all = !containers.isEmpty() && containers.size() == handover.getExpectedContainers();
        boolean anyDiscrepancy = false;
        for (var membership : containers) {
            var items = receiptRepository.findItemsForContainerForUpdate(membership.getContainer().getId());
            boolean complete = !items.isEmpty() && items.stream().allMatch(item -> item.isReceived() || item.isMissing());
            boolean discrepancy = items.stream().anyMatch(ContainerItem::isMissing);
            if (complete) {
                membership.getContainer().completeUnpacking(discrepancy);
                membership.completeReceipt(discrepancy);
            }
            all &= complete;
            anyDiscrepancy |= discrepancy;
        }
        if (all) {
            handover.completeDestinationReceipt(anyDiscrepancy);
            handover.getTransportTrip().completeReceipt(anyDiscrepancy);
        }
    }

    private void requireScannedContainer(Handover handover, Long containerId) {
        var membership = receiptRepository.findHandoverContainer(handover.getId(), containerId).orElseThrow(HubOperationAccessDeniedException::new);
        if (!membership.isScanned() || !Objects.equals(membership.getContainer().getDestinationHub().getId(), handover.getToHub().getId()))
            throw new HubOperationAccessDeniedException();
    }

    private void requireTransitShipment(ContainerItem item) {
        var shipment = item.getShipment();
        boolean forward = "FORWARD".equals(shipment.getDirection()) && shipment.getCurrentStatus() == ShipmentStatus.IN_TRANSIT;
        boolean returning = "RETURN".equals(shipment.getDirection()) && shipment.getCurrentStatus() == ShipmentStatus.RETURN_TO_SENDER;
        if (shipment.isOnHold() || !(forward || returning) || shipment.getCurrentHubId() != null) throw new HubOperationAccessDeniedException();
    }

    private void receiveAt(ContainerItem item, Hub hub) {
        boolean returning = "RETURN".equals(item.getShipment().getDirection());
        item.getShipment().receiveAt(hub);
        if (returning) {
            item.getShipment().transitionTo(ShipmentStatus.RETURN_TO_SENDER);
            if (item.getShipment().getReturnArrivedAtOriginAt()!=null
                    && !shipmentEventRepository.existsByShipmentIdAndEventType(item.getShipment().getId(), "RETURNED_AT_ORIGIN_HUB"))
                shipmentEventRepository.save(ShipmentEvent.of(item.getShipment(),userRepository.getReferenceById(AuthenticatedActor.requireCurrent().userId()),
                        "RETURNED_AT_ORIGIN_HUB","Return parcel physically received at origin"));
        }
    }

    @Override @Transactional(readOnly = true)
    public java.util.List<MissingParcelCaseResponse> missingCases() {
        var scope = managerScope(AuthenticatedActor.requireCurrent());
        if (scope.isEmpty()) return java.util.List.of();
        return incidentRepository.findTop25ByTypeAndStatusAndHubIdInOrderByIdAsc("MISSING_PARCEL", "INVESTIGATING", scope)
                .stream().map(this::caseResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public MissingParcelCaseResponse missingCase(Long incidentId) {
        var scope = managerScope(AuthenticatedActor.requireCurrent());
        Incident incident = incidentRepository.findById(incidentId).orElseThrow(() -> new ResourceNotFoundException("Incident"));
        if (!"MISSING_PARCEL".equals(incident.getType()) || !scope.contains(incident.getHubId())) throw new HubOperationAccessDeniedException();
        return caseResponse(incident);
    }

    private MissingParcelCaseResponse caseResponse(Incident incident) {
        return new MissingParcelCaseResponse(incident.getId(), incident.getShipmentId(), incident.getTripId(), incident.getHubId(),
                incident.getDescription(), incident.getResolutionOutcome());
    }

    @Override @Transactional
    public void resolveMissing(MissingParcelResolutionCommand command) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        var scope = managerScope(actor);
        Handover handover = receiptRepository.findArrivedHandoverForUpdate(command.handoverId())
                .orElseThrow(() -> new ResourceNotFoundException("Handover"));
        if (handover.getTransportTrip() == null || !handover.getTransportTrip().isCompleted()
                || !scope.contains(handover.getToHub().getId()) || !"ACTIVE".equals(handover.getToHub().getStatus())
                || !Objects.equals(command.foundHubId(), handover.getToHub().getId()))
            throw new HubOperationAccessDeniedException();
        requireScannedContainer(handover, command.containerId());
        ContainerItem item = receiptRepository.findItemForUpdate(command.containerId(), command.shipmentId()).orElseThrow(HubOperationAccessDeniedException::new);
        Incident incident = incidentRepository.findByIdForUpdate(command.incidentId()).orElseThrow(() -> new ResourceNotFoundException("Incident"));
        String reference = "[evidence:receipt:handover:" + handover.getId() + ":container:" + command.containerId() + ":shipment:" + command.shipmentId() + "]";
        if (!"MISSING_PARCEL".equals(incident.getType()) || !Objects.equals(incident.getShipmentId(), command.shipmentId())
                || !Objects.equals(incident.getTripId(), handover.getTransportTrip().getId())
                || !Objects.equals(incident.getHubId(), handover.getToHub().getId()) || incident.getDescription() == null
                || !incident.getDescription().endsWith(reference) || !item.isMissing()) throw new HubOperationAccessDeniedException();
        String evidence = command.evidence() == null ? "" : command.evidence().trim();
        if (!("FOUND".equals(command.outcome()) || "CONFIRMED_LOST".equals(command.outcome())) || evidence.isBlank() || evidence.length() > 500)
            throw new IllegalArgumentException("A bounded investigation outcome and evidence are required");
        if (incident.getResolutionOutcome() != null) {
            if (!incident.getResolutionOutcome().equals(command.outcome()) || !evidence.equals(incident.getResolutionEvidence()))
                throw new IllegalStateException("Recorded investigation outcome cannot be changed");
            return;
        }
        var shipment = item.getShipment();
        boolean transit = "FORWARD".equals(shipment.getDirection()) && shipment.getCurrentStatus() == ShipmentStatus.IN_TRANSIT;
        boolean returning = "RETURN".equals(shipment.getDirection()) && shipment.getCurrentStatus() == ShipmentStatus.RETURN_TO_SENDER;
        if (!shipment.isOnHold() || !(transit || returning) || !"INVESTIGATING".equals(incident.getStatus()) || item.isRecovered())
            throw new HubOperationAccessDeniedException();
        User manager = userRepository.getReferenceById(actor.userId());
        incident.recordInvestigation(command.outcome(), evidence, actor.userId());
        if ("FOUND".equals(command.outcome())) {
            item.recover(manager);
            receiveAt(item, handover.getToHub());
            shipment.setOnHold(incidentRepository.existsByShipmentIdAndStatusInAndIdNot(shipment.getId(),
                    java.util.List.of("OPEN", "INVESTIGATING"), incident.getId()));
            shipmentEventRepository.save(ShipmentEvent.arrivedAtHub(shipment, manager, handover.getToHub()));
        }
        shipmentEventRepository.save(ShipmentEvent.of(shipment, manager,
                "FOUND".equals(command.outcome()) ? "MISSING_PARCEL_FOUND" : "LOSS_CONFIRMED", "Investigation recorded for incident " + incident.getId()));
    }

    private java.util.List<Long> managerScope(AuthenticatedActor actor) {
        if (actor.hasRole(UserRole.ROLE_ADMIN)) return managerRepository.findAllActiveHubIds();
        if (!actor.hasRole(UserRole.ROLE_MANAGER)) throw new HubOperationAccessDeniedException();
        EmployeeProfile profile = managerRepository.findActiveProfileByUserId(actor.userId()).orElseThrow(HubOperationAccessDeniedException::new);
        if (profile.getHub() == null || !"ACTIVE".equals(profile.getHub().getStatus())) throw new HubOperationAccessDeniedException();
        return managerRepository.findHubSubtreeIds(profile.getHub().getId());
    }

    private EmployeeProfile requireInboundProfile(AuthenticatedActor actor) {
        if (!actor.hasRole(UserRole.ROLE_HUB_STAFF)) throw new HubOperationAccessDeniedException();
        EmployeeProfile profile = hubOperationsRepository.findActiveProfileByUserId(actor.userId()).orElseThrow(HubOperationAccessDeniedException::new);
        if (!"ACTIVE".equals(profile.getHub().getStatus()) || (!"INBOUND".equals(profile.getPosition()) && !"GENERAL_STAFF".equals(profile.getPosition()))) throw new HubOperationAccessDeniedException();
        return profile;
    }
}
