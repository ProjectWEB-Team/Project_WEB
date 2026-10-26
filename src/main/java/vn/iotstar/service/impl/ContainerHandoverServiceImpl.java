package vn.iotstar.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.hub.CorrectHandoverCommand;
import vn.iotstar.dto.hub.CreateContainerCommand;
import vn.iotstar.dto.hub.CreateHandoverCommand;
import vn.iotstar.dto.hub.HandoverAcceptanceResponse;
import vn.iotstar.dto.hub.LoadContainerCommand;
import vn.iotstar.dto.hub.ScanHandoverContainerCommand;
import vn.iotstar.dto.realtime.OperationalActionNotification;
import vn.iotstar.entity.ContainerItem;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Handover;
import vn.iotstar.entity.HandoverContainer;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.ParcelContainer;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.ShipmentRoute;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.HubOperationAccessDeniedException;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.repository.ContainerHandoverRepository;
import vn.iotstar.repository.HubOperationsRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.ContainerHandoverService;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Transaction and authorization boundary for immutable container custody. */
@Service
public class ContainerHandoverServiceImpl implements ContainerHandoverService {
    private final ContainerHandoverRepository custodyRepository;
    private final HubOperationsRepository hubOperationsRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ContainerHandoverServiceImpl(ContainerHandoverRepository custodyRepository,
                                        HubOperationsRepository hubOperationsRepository,
                                        ShipmentEventRepository shipmentEventRepository,
                                        UserRepository userRepository,
                                        ApplicationEventPublisher applicationEventPublisher) {
        this.custodyRepository = custodyRepository;
        this.hubOperationsRepository = hubOperationsRepository;
        this.shipmentEventRepository = shipmentEventRepository;
        this.userRepository = userRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override @Transactional
    public Long createContainer(CreateContainerCommand command) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        EmployeeProfile profile = requireOutboundProfile(actor);
        Hub destination = custodyRepository.findHubById(command.destinationHubId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination hub"));
        User creator = userRepository.getReferenceById(actor.userId());
        ParcelContainer container = ParcelContainer.open(command.containerCode().trim(), profile.getHub(), destination, creator);
        custodyRepository.persist(container);
        return container.getId();
    }

    @Override @Transactional
    public void loadShipment(LoadContainerCommand command) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        EmployeeProfile profile = requireOutboundProfile(actor);
        ParcelContainer container = lockedContainer(command.containerId());
        Shipment shipment = custodyRepository.findShipmentForUpdate(command.shipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment"));
        if (!"OPEN".equals(container.getStatus()) || shipment.isOnHold()
                || ("RETURN".equals(shipment.getDirection()) && Objects.equals(shipment.getOriginHubId(), profile.getHub().getId()))
                || !("FORWARD".equals(shipment.getDirection()) && shipment.getCurrentStatus() == ShipmentStatus.AT_HUB
                     || "RETURN".equals(shipment.getDirection()) && shipment.getCurrentStatus() == ShipmentStatus.RETURN_TO_SENDER)
                || !sameHub(profile.getHub(), container.getOriginHub()) || !sameHub(profile.getHub(), shipment.getCurrentHub())
                || custodyRepository.containerAlreadyContains(container.getId(), shipment.getId())
                || custodyRepository.hasOtherActiveContainerCustody(container.getId(), shipment.getId())) {
            throw new HubOperationAccessDeniedException();
        }
        ShipmentRoute route = custodyRepository.findPendingRouteForShipmentAtHub(shipment.getId(), profile.getHub().getId())
                .orElseThrow(HubOperationAccessDeniedException::new);
        if (!shipment.routeDecisionKey(profile.getHub().getId()).equals(route.getRouteDecisionKey())
                || !sameHub(route.getHub(), container.getDestinationHub())) throw new HubOperationAccessDeniedException();
        custodyRepository.persist(ContainerItem.scanned(container, shipment));
        route.markOutboundLoaded();
        User user = userRepository.getReferenceById(actor.userId());
        shipmentEventRepository.save(ShipmentEvent.bagCreated(shipment, user, profile.getHub()));
        shipmentEventRepository.save(ShipmentEvent.outboundScanned(shipment, user, profile.getHub()));
    }

    @Override @Transactional
    public Long createHandover(CreateHandoverCommand command) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        EmployeeProfile profile = requireOutboundProfile(actor);
        Hub destination = custodyRepository.findHubById(command.destinationHubId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination hub"));
        User driver = userRepository.findById(command.receiverDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver"));
        if (driver.getRole() != UserRole.ROLE_LINEHAUL_DRIVER || hubOperationsRepository.findActiveProfileByUserId(driver.getId())
                .filter(driverProfile -> "LINEHAUL_DRIVER".equals(driverProfile.getPosition())).isEmpty()) throw new HubOperationAccessDeniedException();
        return createHandover(profile, destination, driver, command.containerIds(), null);
    }

    @Override @Transactional
    public HandoverAcceptanceResponse scanAndAccept(ScanHandoverContainerCommand command) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (!actor.hasRole(UserRole.ROLE_LINEHAUL_DRIVER)) throw new HubOperationAccessDeniedException();
        Handover handover = custodyRepository.findHandoverForUpdate(command.handoverId())
                .orElseThrow(() -> new ResourceNotFoundException("Handover"));
        if (!handover.acceptsDriver(actor.userId())) throw new HubOperationAccessDeniedException();
        HandoverContainer handoverContainer = custodyRepository.findHandoverContainer(handover.getId(), command.containerId())
                .orElseThrow(HubOperationAccessDeniedException::new);
        boolean wasAccepted = "ACCEPTED".equals(handover.getStatus());
        if (!handoverContainer.isScanned()) {
            handoverContainer.markScanned();
            handover.recordContainerScan();
        }
        boolean accepted = "ACCEPTED".equals(handover.getStatus());
        if (accepted && !wasAccepted) {
            applicationEventPublisher.publishEvent(new OperationalActionNotification("HANDOVER_ACCEPTED", handover.getFromHub().getId(), "handover:" + handover.getId()));
        }
        return new HandoverAcceptanceResponse(handover.getId(), handover.getExpectedContainers(), handover.getAcceptedContainers(), accepted);
    }

    @Override @Transactional
    public Long correctByCancellationAndRecreation(CorrectHandoverCommand command) {
        EmployeeProfile profile = requireOutboundProfile(AuthenticatedActor.requireCurrent());
        Handover original = custodyRepository.findHandoverForUpdate(command.handoverId())
                .orElseThrow(() -> new ResourceNotFoundException("Handover"));
        if (!sameHub(profile.getHub(), original.getFromHub())) throw new HubOperationAccessDeniedException();
        original.cancelForCorrection();
        Handover replacement = Handover.pending(original.getFromHub(), original.getToHub(), profile, original.getReceiverDriver(), command.replacementContainerIds().size(), original);
        original.linkReplacement(replacement);
        custodyRepository.persist(replacement);
        for (Long id : distinctIds(command.replacementContainerIds())) {
            ParcelContainer container = lockedContainer(id);
            ensureCompatibleForHandover(container, profile.getHub(), original.getToHub());
            container.seal();
            custodyRepository.persist(HandoverContainer.expected(replacement, container));
        }
        return replacement.getId();
    }

    private Long createHandover(EmployeeProfile profile, Hub destination, User driver, List<Long> inputIds, Handover replaced) {
        List<Long> containerIds = distinctIds(inputIds);
        Handover handover = Handover.pending(profile.getHub(), destination, profile, driver, containerIds.size(), replaced);
        custodyRepository.persist(handover);
        for (Long id : containerIds) {
            ParcelContainer container = lockedContainer(id);
            ensureCompatibleForHandover(container, profile.getHub(), destination);
            container.seal();
            custodyRepository.persist(HandoverContainer.expected(handover, container));
        }
        return handover.getId();
    }

    private EmployeeProfile requireOutboundProfile(AuthenticatedActor actor) {
        if (!actor.hasRole(UserRole.ROLE_HUB_STAFF)) throw new HubOperationAccessDeniedException();
        EmployeeProfile profile = hubOperationsRepository.findActiveProfileByUserId(actor.userId())
                .orElseThrow(HubOperationAccessDeniedException::new);
        if (!"OUTBOUND".equals(profile.getPosition()) && !"GENERAL_STAFF".equals(profile.getPosition())) throw new HubOperationAccessDeniedException();
        return profile;
    }
    private ParcelContainer lockedContainer(Long id) { return custodyRepository.findContainerForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Container")); }
    private void ensureCompatibleForHandover(ParcelContainer container, Hub origin, Hub destination) {
        if (!sameHub(container.getOriginHub(), origin) || !sameHub(container.getDestinationHub(), destination)
                || !custodyRepository.findActiveCustodyForContainer(container.getId()).isEmpty()) throw new HubOperationAccessDeniedException();
    }
    private List<Long> distinctIds(List<Long> ids) {
        if (ids == null || ids.isEmpty() || new HashSet<>(ids).size() != ids.size()) throw new HubOperationAccessDeniedException();
        return ids;
    }
    private boolean sameHub(Hub first, Hub second) { return first != null && second != null && Objects.equals(first.getId(), second.getId()); }
}
