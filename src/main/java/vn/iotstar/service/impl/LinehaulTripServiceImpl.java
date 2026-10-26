package vn.iotstar.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.config.LogisticsProperties;
import vn.iotstar.dto.driver.TripActionResponse;
import vn.iotstar.dto.driver.TripDelayCommand;
import vn.iotstar.dto.realtime.OperationalActionNotification;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Handover;
import vn.iotstar.entity.Incident;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.TransportTrip;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.entity.Vehicle;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.ContainerItem;
import vn.iotstar.dto.driver.VehicleOptionResponse;
import vn.iotstar.exception.HubOperationAccessDeniedException;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.repository.HubOperationsRepository;
import vn.iotstar.repository.LinehaulOperationsRepository;
import vn.iotstar.repository.OperationalIncidentRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.LinehaulTripService;

import java.util.Objects;
import java.util.List;

/** Transaction, sequencing and contextual-scope boundary for the driver workspace. */
@Service
public class LinehaulTripServiceImpl implements LinehaulTripService {
    private final LinehaulOperationsRepository operationsRepository;
    private final HubOperationsRepository hubOperationsRepository;
    private final OperationalIncidentRepository incidentRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final LogisticsProperties logisticsProperties;

    public LinehaulTripServiceImpl(LinehaulOperationsRepository operationsRepository,
                                   HubOperationsRepository hubOperationsRepository,
                                   OperationalIncidentRepository incidentRepository,
                                   ShipmentEventRepository shipmentEventRepository,
                                   UserRepository userRepository,
                                   ApplicationEventPublisher applicationEventPublisher,
                                   LogisticsProperties logisticsProperties) {
        this.logisticsProperties = logisticsProperties;
        this.operationsRepository = operationsRepository;
        this.hubOperationsRepository = hubOperationsRepository;
        this.incidentRepository = incidentRepository;
        this.shipmentEventRepository = shipmentEventRepository;
        this.userRepository = userRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    @Transactional
    public TripActionResponse acceptHandover(Long handoverId) {
        return acceptHandover(handoverId, null);
    }

    @Override @Transactional(readOnly = true)
    public List<VehicleOptionResponse> availableVehicles() {
        requireActiveDriver(AuthenticatedActor.requireCurrent());
        return operationsRepository.findAvailableVehicles().stream()
                .map(v -> new VehicleOptionResponse(v.getId(), v.getPlateNumber(), v.getVehicleType())).toList();
    }

    @Override @Transactional
    public TripActionResponse acceptHandover(Long handoverId, Long vehicleId) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        requireActiveDriver(actor);
        Handover handover = operationsRepository.findHandoverForUpdate(handoverId)
                .orElseThrow(() -> new ResourceNotFoundException("Handover"));
        requireAssigned(actor, handover);
        TransportTrip existing = handover.getTransportTrip();
        if (existing != null) {
            if (existing.getVehicle() == null || (vehicleId != null && !vehicleId.equals(existing.getVehicle().getId())))
                throw new HubOperationAccessDeniedException();
            return response(existing);
        }
        if (!"ACCEPTED".equals(handover.getStatus())) throw new HubOperationAccessDeniedException();
        if (vehicleId == null) throw new IllegalArgumentException("Select an available vehicle for the trip");
        Vehicle vehicle = operationsRepository.findVehicleForUpdate(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle"));
        if (!"AVAILABLE".equals(vehicle.getStatus())) throw new HubOperationAccessDeniedException();
        User driver = userRepository.getReferenceById(actor.userId());
        vehicle.reserve();
        TransportTrip trip = TransportTrip.accepted(handover, driver, vehicle);
        handover.linkTransportTrip(trip);
        operationsRepository.persist(trip);
        appendShipmentEvents(handover, driver, "ACCEPT");
        return response(trip);
    }

    @Override
    @Transactional
    public TripActionResponse startTrip(Long tripId) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        requireActiveDriver(actor);
        TransportTrip trip = requiredAssignedTrip(tripId, actor);
        requireAcceptedRoute(trip);
        if ("IN_TRANSIT".equals(trip.getStatus()) || "ARRIVED".equals(trip.getStatus()) || trip.isCompleted()) return response(trip);
        if (!"SCHEDULED".equals(trip.getStatus()) || trip.getVehicle() == null || !"IN_USE".equals(trip.getVehicle().getStatus()))
            throw new HubOperationAccessDeniedException();
        var containers = operationsRepository.findContainersForUpdate(trip.getHandover().getId());
        var items = operationsRepository.findManifestItemsForUpdate(trip.getHandover().getId());
        if (containers.size() != trip.getHandover().getExpectedContainers() || containers.isEmpty() || items.isEmpty()
                || containers.stream().anyMatch(hc -> !hc.isScanned() || !"SEALED".equals(hc.getContainer().getStatus())
                || !sameHub(hc.getContainer().getOriginHub(), trip.getOriginHub()) || !sameHub(hc.getContainer().getDestinationHub(), trip.getDestinationHub()))
                || items.stream().map(i -> i.getShipment().getId()).distinct().count() != items.size()
                || containers.stream().anyMatch(hc -> items.stream().noneMatch(i -> Objects.equals(i.getContainer().getId(), hc.getContainer().getId()))))
            throw new HubOperationAccessDeniedException();
        for (ContainerItem item : items) {
            Shipment shipment = item.getShipment();
            boolean forward = "FORWARD".equals(shipment.getDirection()) && shipment.getCurrentStatus() == ShipmentStatus.AT_HUB;
            boolean returning = "RETURN".equals(shipment.getDirection()) && shipment.getCurrentStatus() == ShipmentStatus.RETURN_TO_SENDER;
            if (!"IN_CONTAINER".equals(item.getStatus()) || shipment.isOnHold() || !(forward || returning)
                    || !Objects.equals(shipment.getCurrentHubId(), trip.getOriginHub().getId())) throw new HubOperationAccessDeniedException();
        }
        User driver = userRepository.getReferenceById(actor.userId());
        // Validate the complete locked manifest before any mutation.
        trip.start(driver);
        trip.getHandover().depart();
        containers.forEach(hc -> hc.getContainer().loadForDeparture());
        for (ContainerItem item : items) {
            Shipment shipment = item.getShipment();
            if ("FORWARD".equals(shipment.getDirection())) shipment.transitionTo(ShipmentStatus.IN_TRANSIT);
            shipment.setCurrentHubId(null);
            shipmentEventRepository.save(ShipmentEvent.linehaulTripStarted(shipment, driver, trip.getOriginHub()));
        }
        // Demo only (Workflow section 101): reuse the normal arrival instead of a parallel state path.
        if (logisticsProperties.getDemo().isAutoLinehaulTransitSimulation()) return arriveTrip(tripId);
        return response(trip);
    }

    @Override
    @Transactional
    public TripActionResponse arriveTrip(Long tripId) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        requireActiveDriver(actor);
        TransportTrip trip = requiredAssignedTrip(tripId, actor);
        requireAcceptedRoute(trip);
        boolean transition = "IN_TRANSIT".equals(trip.getStatus());
        trip.arrive(userRepository.getReferenceById(actor.userId()));
        if (transition) {
            User driver = userRepository.getReferenceById(actor.userId());
            appendShipmentEvents(trip.getHandover(), driver, "ARRIVE");
            applicationEventPublisher.publishEvent(new OperationalActionNotification("DESTINATION_ARRIVAL", trip.getDestinationHub().getId(), "trip:" + trip.getId()));
            applicationEventPublisher.publishEvent(new OperationalActionNotification("INBOUND_READY_CONTAINER", trip.getDestinationHub().getId(), "trip:" + trip.getId()));
        }
        // Arrival deliberately leaves handover custody open for destination inbound receipt in 03-04.
        return response(trip);
    }

    @Override
    @Transactional
    public Long reportTripDelay(TripDelayCommand command) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        requireActiveDriver(actor);
        TransportTrip trip = requiredAssignedTrip(command.tripId(), actor);
        if (!"IN_TRANSIT".equals(trip.getStatus())) throw new HubOperationAccessDeniedException();
        String note = command.note() == null ? "" : command.note().trim();
        if (note.isBlank() || note.length() > 500 || (command.evidenceReference() != null && command.evidenceReference().trim().length() > 120)) {
            throw new HubOperationAccessDeniedException();
        }
        User driver = userRepository.getReferenceById(actor.userId());
        Incident incident = incidentRepository.save(Incident.tripDelay(trip, driver, note,
                command.evidenceReference() == null ? null : command.evidenceReference().trim()));
        applicationEventPublisher.publishEvent(new OperationalActionNotification("NEW_INCIDENT", trip.getOriginHub().getId(), "incident:" + incident.getId()));
        return incident.getId();
    }

    private TransportTrip requiredAssignedTrip(Long tripId, AuthenticatedActor actor) {
        TransportTrip trip = operationsRepository.findTripForUpdate(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip"));
        if (!trip.isAssignedTo(actor.userId())) throw new HubOperationAccessDeniedException();
        return trip;
    }
    private void requireActiveDriver(AuthenticatedActor actor) {
        if (!actor.hasRole(UserRole.ROLE_LINEHAUL_DRIVER)) throw new HubOperationAccessDeniedException();
        EmployeeProfile profile = hubOperationsRepository.findActiveProfileByUserId(actor.userId())
                .orElseThrow(HubOperationAccessDeniedException::new);
        if (!"LINEHAUL_DRIVER".equals(profile.getPosition())) throw new HubOperationAccessDeniedException();
    }
    private void requireAssigned(AuthenticatedActor actor, Handover handover) {
        if (!handover.acceptsDriver(actor.userId())) throw new HubOperationAccessDeniedException();
    }
    private void requireAcceptedRoute(TransportTrip trip) {
        Handover handover = trip.getHandover();
        if (!("ACCEPTED".equals(handover.getStatus()) || "IN_TRANSIT".equals(handover.getStatus())
                || "COMPLETED".equals(handover.getStatus()) || "COMPLETED_WITH_DISCREPANCY".equals(handover.getStatus()))
                || !sameHub(trip.getOriginHub(), handover.getFromHub())
                || !sameHub(trip.getDestinationHub(), handover.getToHub())) throw new HubOperationAccessDeniedException();
    }
    private void appendShipmentEvents(Handover handover, User actor, String transition) {
        for (Shipment shipment : operationsRepository.findShipmentsForHandover(handover.getId())) {
            ShipmentEvent event = switch (transition) {
                case "ACCEPT" -> ShipmentEvent.linehaulHandoverAccepted(shipment, actor, handover.getFromHub());
                case "START" -> ShipmentEvent.linehaulTripStarted(shipment, actor, handover.getFromHub());
                case "ARRIVE" -> ShipmentEvent.linehaulArrived(shipment, actor, handover.getToHub());
                default -> throw new IllegalArgumentException("Unknown linehaul transition");
            };
            shipmentEventRepository.save(event);
        }
    }
    private TripActionResponse response(TransportTrip trip) { return new TripActionResponse(trip.getId(), trip.getStatus(), true); }
    private boolean sameHub(vn.iotstar.entity.Hub first, vn.iotstar.entity.Hub second) {
        return first != null && second != null && Objects.equals(first.getId(), second.getId());
    }
}
