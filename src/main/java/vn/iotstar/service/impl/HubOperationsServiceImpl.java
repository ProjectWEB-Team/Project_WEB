package vn.iotstar.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.hub.InboundShipmentCommand;
import vn.iotstar.dto.hub.SortShipmentCommand;
import vn.iotstar.dto.hub.SortingDecisionResponse;
import vn.iotstar.dto.hub.RouteDiagnosticResponse;
import vn.iotstar.dto.realtime.OperationalActionNotification;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.HubRoutingRule;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.RoutingExceptionRecord;
import vn.iotstar.entity.ShipmentRoute;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.HubOperationAccessDeniedException;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.exception.RouteConfigurationException;
import vn.iotstar.repository.HubOperationsRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.RoutingExceptionRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.HubOperationsService;

import java.util.List;
import java.util.Objects;

/** Transaction boundary for scoped hub receipt and static next-hop decisions. */
@Service
public class HubOperationsServiceImpl implements HubOperationsService {
    private final HubOperationsRepository hubOperationsRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final RoutingExceptionRepository routingExceptionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public HubOperationsServiceImpl(HubOperationsRepository hubOperationsRepository,
                                    ShipmentEventRepository shipmentEventRepository,
                                    RoutingExceptionRepository routingExceptionRepository,
                                    UserRepository userRepository,
                                    ApplicationEventPublisher applicationEventPublisher) {
        this.hubOperationsRepository = hubOperationsRepository;
        this.shipmentEventRepository = shipmentEventRepository;
        this.routingExceptionRepository = routingExceptionRepository;
        this.userRepository = userRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    @Transactional
    public void receiveAtHub(InboundShipmentCommand command) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        EmployeeProfile profile = requireActiveOperatorProfile(actor, "INBOUND");
        Shipment shipment = requireLockedShipment(command.shipmentId());
        if (!hasExpectedInboundRelationship(shipment, profile.getHub())) {
            throw new HubOperationAccessDeniedException();
        }
        User user = userRepository.getReferenceById(actor.userId());
        shipment.receiveAt(profile.getHub());
        shipmentEventRepository.save(ShipmentEvent.arrivedAtHub(shipment, user, profile.getHub()));
    }

    @Override
    @Transactional(noRollbackFor = RouteConfigurationException.class)
    public SortingDecisionResponse sortAtHub(SortShipmentCommand command) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        EmployeeProfile profile = requireActiveOperatorProfile(actor, "SORTER");
        Shipment shipment = requireLockedShipment(command.shipmentId());
        Hub currentHub = shipment.getCurrentHub();
        if (shipment.getCurrentStatus() != ShipmentStatus.AT_HUB || currentHub == null
                || !Objects.equals(currentHub.getId(), profile.getHub().getId())) {
            throw new HubOperationAccessDeniedException();
        }

        String decisionKey = "shipment:" + shipment.getId() + ":hub:" + currentHub.getId();
        return hubOperationsRepository.findByRouteDecisionKey(decisionKey)
                .map(route -> response(route, true))
                .orElseGet(() -> createDecision(actor, profile, shipment, currentHub, decisionKey));
    }

    @Override
    @Transactional(readOnly = true)
    public RouteDiagnosticResponse getRouteDiagnostic(Long shipmentId) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        Shipment shipment = hubOperationsRepository.findShipmentByIdWithHub(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment"));
        Hub currentHub = shipment.getCurrentHub();
        if (currentHub == null || !mayReadHubDiagnostic(actor, currentHub)) {
            throw new HubOperationAccessDeniedException();
        }
        List<HubRoutingRule> matches = blank(shipment.getDestinationRegion()) || blank(shipment.getDestinationProvince())
                ? List.of() : hubOperationsRepository.findActiveRules(currentHub.getId(), shipment.getDestinationRegion(), shipment.getDestinationProvince());
        String code = matches.isEmpty() ? "NO_ACTIVE_STATIC_RULE" : "AMBIGUOUS_STATIC_RULE";
        return new RouteDiagnosticResponse(currentHub.getCode(), shipment.getDestinationRegion() + " / " + shipment.getDestinationProvince(), code);
    }

    private SortingDecisionResponse createDecision(AuthenticatedActor actor, EmployeeProfile profile, Shipment shipment,
                                                   Hub currentHub, String decisionKey) {
        HubRoutingRule rule = resolveSingleStaticRule(shipment, currentHub);
        ShipmentRoute saved = hubOperationsRepository.saveRoute(
                ShipmentRoute.decided(shipment, currentHub, rule.getNextHub(), decisionKey));
        User user = userRepository.getReferenceById(actor.userId());
        routingExceptionRepository.findOpenRouteLookupBlockedForUpdate(shipment.getId(), currentHub.getId())
                .ifPresent(exception -> exception.resolve(user));
        shipmentEventRepository.save(ShipmentEvent.sortingCompleted(shipment, user, profile.getHub()));
        return response(saved, false);
    }

    private HubRoutingRule resolveSingleStaticRule(Shipment shipment, Hub currentHub) {
        if (blank(shipment.getDestinationRegion()) || blank(shipment.getDestinationProvince())) {
            return blocked(shipment, currentHub);
        }
        List<HubRoutingRule> matches = hubOperationsRepository.findActiveRules(currentHub.getId(),
                shipment.getDestinationRegion(), shipment.getDestinationProvince());
        List<HubRoutingRule> exact = matches.stream()
                .filter(rule -> shipment.getDestinationProvince().equals(rule.getDestinationProvince())).toList();
        List<HubRoutingRule> selected = exact.isEmpty()
                ? matches.stream().filter(rule -> rule.getDestinationProvince() == null).toList()
                : exact;
        if (selected.size() != 1) {
            return blocked(shipment, currentHub);
        }
        return selected.getFirst();
    }

    private HubRoutingRule blocked(Shipment shipment, Hub currentHub) {
        if (routingExceptionRepository.findOpenRouteLookupBlockedForUpdate(shipment.getId(), currentHub.getId()).isEmpty()) {
            User actor = userRepository.getReferenceById(AuthenticatedActor.requireCurrent().userId());
            RoutingExceptionRecord exception = routingExceptionRepository.save(
                    RoutingExceptionRecord.openRouteLookupBlocked(shipment, currentHub, actor));
            shipmentEventRepository.save(ShipmentEvent.routeLookupBlocked(shipment, actor, currentHub));
            applicationEventPublisher.publishEvent(new OperationalActionNotification(
                    "BLOCKED_ROUTE", currentHub.getId(), "routing-exception:" + exception.getId()));
        }
        throw new RouteConfigurationException();
    }

    private EmployeeProfile requireActiveOperatorProfile(AuthenticatedActor actor, String requiredPosition) {
        if (!actor.hasRole(UserRole.ROLE_HUB_STAFF)) {
            throw new HubOperationAccessDeniedException();
        }
        EmployeeProfile profile = hubOperationsRepository.findActiveProfileByUserId(actor.userId())
                .orElseThrow(HubOperationAccessDeniedException::new);
        if (!requiredPosition.equals(profile.getPosition()) && !"GENERAL_STAFF".equals(profile.getPosition())) {
            throw new HubOperationAccessDeniedException();
        }
        return profile;
    }

    private Shipment requireLockedShipment(Long shipmentId) {
        return hubOperationsRepository.findShipmentByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment"));
    }

    private boolean hasExpectedInboundRelationship(Shipment shipment, Hub inboundHub) {
        // Container-custody arrivals, including discrepancy/missing outcomes, are settled only by
        // DestinationReceiptService with handover, container and item-level physical evidence.
        // This generic shipment-id endpoint remains exclusively for the non-container initial intake path.
        return (shipment.getCurrentStatus() == ShipmentStatus.WAITING_FOR_HANDOVER
                || shipment.getCurrentStatus() == ShipmentStatus.PICKED_UP)
                && sameHub(shipment.getCurrentHub(), inboundHub);
    }

    private boolean mayReadHubDiagnostic(AuthenticatedActor actor, Hub currentHub) {
        if (actor.hasRole(UserRole.ROLE_ADMIN)) {
            return true;
        }
        if (!actor.hasRole(UserRole.ROLE_MANAGER)) {
            return false;
        }
        EmployeeProfile profile = hubOperationsRepository.findActiveProfileByUserId(actor.userId())
                .orElseThrow(HubOperationAccessDeniedException::new);
        return Integer.valueOf(1).equals(hubOperationsRepository.isHubWithinSubtree(profile.getHub().getId(), currentHub.getId()));
    }

    private SortingDecisionResponse response(ShipmentRoute route, boolean reused) {
        Hub nextHub = route.getHub();
        return new SortingDecisionResponse(nextHub.getCode(), nextHub.getRegion() + " / " + nextHub.getProvince(),
                true, reused);
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private boolean sameHub(Hub first, Hub second) {
        return first != null && second != null && Objects.equals(first.getId(), second.getId());
    }
}
