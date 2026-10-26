package vn.iotstar.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.config.LogisticsProperties;
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
import vn.iotstar.entity.Payment;
import vn.iotstar.exception.HubOperationAccessDeniedException;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.exception.RouteConfigurationException;
import vn.iotstar.repository.HubOperationsRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.RoutingExceptionRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.repository.PaymentRepository;
import vn.iotstar.repository.PricingPolicyRepository;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.HubOperationsService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.math.BigDecimal;

/** Transaction boundary for scoped hub receipt and static next-hop decisions. */
@Service
public class HubOperationsServiceImpl implements HubOperationsService {
    private final HubOperationsRepository hubOperationsRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final RoutingExceptionRepository routingExceptionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PaymentRepository paymentRepository;
    private final PricingPolicyRepository pricingPolicyRepository;
    private final ShipmentCancellation cancellation;
    private final vn.iotstar.realtime.ShipmentEventPublisher shipmentPublisher;
    private final LogisticsProperties logisticsProperties;

    public HubOperationsServiceImpl(HubOperationsRepository hubOperationsRepository,
                                    ShipmentEventRepository shipmentEventRepository,
                                    RoutingExceptionRepository routingExceptionRepository,
                                    UserRepository userRepository,
                                    ApplicationEventPublisher applicationEventPublisher,
                                    PaymentRepository paymentRepository, PricingPolicyRepository pricingPolicyRepository,
                                    ShipmentCancellation cancellation, vn.iotstar.realtime.ShipmentEventPublisher shipmentPublisher,
                                    LogisticsProperties logisticsProperties) {
        this.logisticsProperties = logisticsProperties;
        this.hubOperationsRepository = hubOperationsRepository;
        this.shipmentEventRepository = shipmentEventRepository;
        this.routingExceptionRepository = routingExceptionRepository;
        this.userRepository = userRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.paymentRepository = paymentRepository;
        this.pricingPolicyRepository = pricingPolicyRepository;
        this.cancellation = cancellation;
        this.shipmentPublisher = shipmentPublisher;
    }

    @Override
    @Transactional
    public void receiveAtHub(InboundShipmentCommand command) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        EmployeeProfile profile = requireActiveOperatorProfile(actor, "INBOUND");
        Shipment shipment = requireLockedShipment(command.shipmentId());
        if (shipment.isOnHold()) throw new HubOperationAccessDeniedException();
        if ("SELF_DROPOFF".equals(shipment.getPickupMethod())) {
            receiveSelfDropoff(actor, profile, shipment, command);
            return;
        }
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
        if (shipment.isOnHold() || !(shipment.getCurrentStatus() == ShipmentStatus.AT_HUB && "FORWARD".equals(shipment.getDirection())
                || shipment.getCurrentStatus() == ShipmentStatus.RETURN_TO_SENDER && "RETURN".equals(shipment.getDirection())) || currentHub == null
                || !Objects.equals(currentHub.getId(), profile.getHub().getId())) {
            throw new HubOperationAccessDeniedException();
        }

        if (Objects.equals("RETURN".equals(shipment.getDirection()) ? shipment.getOriginHubId() : shipment.getDestinationHubId(), currentHub.getId())) {
            return new SortingDecisionResponse(currentHub.getCode(),
                    currentHub.getRegion() + " / " + currentHub.getProvince(), false, true);
        }

        String decisionKey = shipment.routeDecisionKey(currentHub.getId());
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
        List<HubRoutingRule> matches = blank(targetRegion(shipment)) || blank(targetProvince(shipment))
                ? List.of() : hubOperationsRepository.findActiveRules(currentHub.getId(), targetRegion(shipment), targetProvince(shipment));
        String code = matches.isEmpty() ? "NO_ACTIVE_STATIC_RULE" : "AMBIGUOUS_STATIC_RULE";
        return new RouteDiagnosticResponse(currentHub.getCode(), targetRegion(shipment) + " / " + targetProvince(shipment), code);
    }

    private SortingDecisionResponse createDecision(AuthenticatedActor actor, EmployeeProfile profile, Shipment shipment,
                                                   Hub currentHub, String decisionKey) {
        Hub nextHub = singleStaticRule(shipment, currentHub).map(HubRoutingRule::getNextHub)
                .or(() -> logisticsProperties.getDemo().isFastTrackRouteBypass()
                        ? fastTrackTarget(shipment, currentHub) : Optional.empty())
                .orElseGet(() -> blocked(shipment, currentHub));
        ShipmentRoute saved = hubOperationsRepository.saveRoute(
                ShipmentRoute.decided(shipment, currentHub, nextHub, decisionKey));
        User user = userRepository.getReferenceById(actor.userId());
        routingExceptionRepository.findOpenRouteLookupBlockedForUpdate(shipment.getId(), currentHub.getId())
                .ifPresent(exception -> exception.resolve(user));
        shipmentEventRepository.save(ShipmentEvent.sortingCompleted(shipment, user, profile.getHub()));
        return response(saved, false);
    }

    private Optional<HubRoutingRule> singleStaticRule(Shipment shipment, Hub currentHub) {
        if (blank(targetRegion(shipment)) || blank(targetProvince(shipment))) {
            return Optional.empty();
        }
        List<HubRoutingRule> matches = hubOperationsRepository.findActiveRules(currentHub.getId(),
                targetRegion(shipment), targetProvince(shipment));
        List<HubRoutingRule> exact = matches.stream()
                .filter(rule -> targetProvince(shipment).equals(rule.getDestinationProvince())).toList();
        List<HubRoutingRule> selected = exact.isEmpty()
                ? matches.stream().filter(rule -> rule.getDestinationProvince() == null).toList()
                : exact;
        return selected.size() == 1 ? Optional.of(selected.getFirst()) : Optional.empty();
    }

    /** Demo only: the server-persisted target hub, never a request value (ADR-016). */
    private Optional<Hub> fastTrackTarget(Shipment shipment, Hub currentHub) {
        Long targetId = "RETURN".equals(shipment.getDirection()) ? shipment.getOriginHubId() : shipment.getDestinationHubId();
        if (targetId == null || targetId.equals(currentHub.getId())) return Optional.empty();
        return hubOperationsRepository.findActiveHub(targetId);
    }

    private Hub returnTarget(Shipment s) {
        if (s.getOriginHubId() == null) throw new RouteConfigurationException();
        return hubOperationsRepository.findActiveHub(s.getOriginHubId()).orElseThrow(RouteConfigurationException::new);
    }
    private String targetRegion(Shipment s) { return "RETURN".equals(s.getDirection()) ? returnTarget(s).getRegion() : s.getDestinationRegion(); }
    private String targetProvince(Shipment s) { return "RETURN".equals(s.getDirection()) ? returnTarget(s).getProvince() : s.getDestinationProvince(); }

    private <T> T blocked(Shipment shipment, Hub currentHub) {
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
        return "HOME_PICKUP".equals(shipment.getPickupMethod())
                && shipment.getCurrentStatus() == ShipmentStatus.PICKED_UP
                && sameHub(shipment.getCurrentHub(), inboundHub);
    }

    private void receiveSelfDropoff(AuthenticatedActor actor, EmployeeProfile profile, Shipment shipment,
                                    InboundShipmentCommand command) {
        Hub hub = profile.getHub();
        if (!"FORWARD".equals(shipment.getDirection()) || !"ACTIVE".equals(hub.getStatus()) || !"DELIVERY_HUB".equals(hub.getType())
                || !Integer.valueOf(1).equals(hub.getLevel())
                || (shipment.getOriginHubId() != null && !shipment.getOriginHubId().equals(hub.getId()))
                || (shipment.getCurrentHubId() != null && !shipment.getCurrentHubId().equals(hub.getId()))) {
            throw new HubOperationAccessDeniedException();
        }
        if (!shipment.getTrackingNumber().equals(command.trackingNumber())) {
            throw new IllegalArgumentException("Scan the parcel tracking number before receipt");
        }
        // The row lock serializes repeat scans, and the persisted event distinguishes a real prior receipt.
        if (shipment.getCurrentStatus() == ShipmentStatus.AT_HUB
                && Objects.equals(shipment.getOriginHubId(), hub.getId())
                && shipmentEventRepository.existsByShipmentIdAndEventType(shipment.getId(), "ARRIVED_AT_HUB")) return;
        if (shipment.getCurrentStatus() != ShipmentStatus.WAITING_FOR_HANDOVER
                || !positive(command.actualWeightGrams()) || !positive(command.actualLengthCm())
                || !positive(command.actualWidthCm()) || !positive(command.actualHeightCm())) {
            throw new IllegalArgumentException("Parcel must be ready for handover with positive measured weight and dimensions");
        }
        Payment payment = paymentRepository.findFirstByShipmentIdAndPaymentType(shipment.getId(), "SHIPPING_FEE")
                .orElseThrow(() -> new IllegalStateException("Shipping payment is missing"));
        if (!Objects.equals(payment.getPayerType(), shipment.getFeePayer()) || payment.getAmount() == null
                || shipment.getShippingFee() == null || payment.getAmount().compareTo(shipment.getShippingFee()) != 0) {
            throw new IllegalStateException("Shipping obligation does not match intake");
        }
        boolean senderPays = "SENDER".equals(payment.getPayerType());
        boolean unpaidCash = senderPays && "CASH".equals(payment.getMethod()) && "UNPAID".equals(payment.getStatus());
        boolean prepaid = senderPays && "VNPAY".equals(payment.getMethod()) && "PAID".equals(payment.getStatus());
        boolean receiverPays = "RECEIVER".equals(payment.getPayerType()) && "CASH".equals(payment.getMethod())
                && "UNPAID".equals(payment.getStatus());
        if (!unpaidCash && !prepaid && !receiverPays) throw new IllegalStateException("Payment is not eligible for receipt");

        BigDecimal surcharge = BigDecimal.ZERO;
        if (shipment.getWeight() == null) throw new IllegalStateException("Declared weight is missing");
        if (command.actualWeightGrams() > shipment.getWeight()) {
            if (blank(shipment.getServiceCode()) || blank(targetRegion(shipment))) {
                throw new IllegalStateException("The shipment pricing service must be resolved before receipt");
            }
            var policy = pricingPolicyRepository.findFirstByServiceCodeAndFromRegionAndToRegionAndStatusOrderByIdDesc(
                    shipment.getServiceCode(), hub.getRegion(), targetRegion(shipment), "ACTIVE")
                    .orElseThrow(() -> new IllegalStateException("No active pricing policy for physical intake"));
            BigDecimal actualFee = ShippingFeeCalculator.calculate(policy, command.actualWeightGrams(),
                    shipment.getDeclaredValue(), shipment.getCodAmount());
            surcharge = actualFee.subtract(shipment.getShippingFee()).max(BigDecimal.ZERO);
        }
        User user = userRepository.getReferenceById(actor.userId());
        Payment surchargePayment = paymentRepository.findFirstByShipmentIdAndPaymentType(shipment.getId(), "WEIGHT_SURCHARGE").orElse(null);
        if(surchargePayment!=null && (!senderPays || surchargePayment.getAmount()==null || surcharge.signum()<=0
                || surchargePayment.getAmount().compareTo(surcharge)!=0 || !"SENDER".equals(surchargePayment.getPayerType())))
            throw new IllegalStateException("Existing surcharge must match physical assessment");
        boolean surchargePaid = false;
        if (senderPays && surcharge.signum() > 0) {
            if (surchargePayment != null) {
                if (!"SENDER".equals(surchargePayment.getPayerType()) || surchargePayment.getAmount().compareTo(surcharge) != 0)
                    throw new IllegalStateException("Assessed surcharge differs from measured obligation");
                if ("PAID".equals(surchargePayment.getStatus())) { ShipmentFeeService.requirePaidEvidence(surchargePayment); surchargePaid = true; }
                else if (!"UNPAID".equals(surchargePayment.getStatus()) && !"PENDING".equals(surchargePayment.getStatus()))
                    throw new IllegalStateException("Surcharge cannot be received");
            }
            if ("REJECT".equals(command.surchargeDecision())) {
                if (command.cashReceived() != null && command.cashReceived().signum() != 0)
                    throw new IllegalArgumentException("Cannot cancel while recording collected cash");
                cancellation.cancel(shipment, "WEIGHT_DISCREPANCY", user);
                return;
            }
            if ("PAY_ONLINE".equals(command.surchargeDecision()) && !surchargePaid) {
                if (command.cashReceived() != null && command.cashReceived().signum()!=0) throw new IllegalArgumentException("Do not collect cash while requesting online surcharge payment");
                if (surchargePayment==null) paymentRepository.save(Payment.create(shipment,"SENDER","WEIGHT_SURCHARGE",surcharge,"VNPAY","PENDING"));
                // No custody is accepted yet. The sender pays this server-assessed obligation on their own fee page.
                return;
            }
            if (!"ACCEPT".equals(command.surchargeDecision()) && !surchargePaid) throw new IllegalArgumentException("Sender must accept the weight surcharge");
        }
        BigDecimal cashDue = (unpaidCash ? payment.getAmount() : BigDecimal.ZERO)
                .add(senderPays && !surchargePaid ? surcharge : BigDecimal.ZERO);
        BigDecimal received = command.cashReceived() == null ? BigDecimal.ZERO : command.cashReceived();
        if (received.compareTo(cashDue) != 0) throw new IllegalArgumentException("Collected cash must equal the outstanding intake amount");

        // All state and amount checks precede the first mutation. The surrounding transaction owns all records.
        if (unpaidCash) {
            payment.collect("CASH", actor.userId(), "HUB-" + hub.getId() + "-" + shipment.getId());
            shipment.setPaymentStatus("PAID");
        }
        if (surcharge.signum() > 0 && senderPays) {
            if (!surchargePaid) {
                Payment extra = surchargePayment != null ? surchargePayment : Payment.create(shipment, "SENDER", "WEIGHT_SURCHARGE", surcharge, "CASH", "UNPAID");
                extra.collect("CASH", actor.userId(), "HUB-SURCHARGE-" + shipment.getId());
                paymentRepository.save(extra);
            }
        } else if (surcharge.signum() > 0) {
            shipment.setShippingFee(shipment.getShippingFee().add(surcharge));
            payment.reviseUnpaidAmount(shipment.getShippingFee());
            shipmentEventRepository.save(ShipmentEvent.of(shipment, user, "SHIPPING_FEE_UPDATED", "Shipping fee updated after physical measurement"));
            if (shipment.getReceiver() != null) shipmentPublisher.publishToUserAfterCommit(shipment.getReceiver().getUsername(),
                    new vn.iotstar.dto.realtime.ShipmentCreatedEventResponse(shipment.getTrackingNumber(), shipment.getCurrentStatus(),
                            "SHIPPING_FEE_UPDATED", java.time.LocalDateTime.now()));
        }
        shipment.recordMeasurements(command.actualWeightGrams(), command.actualLengthCm(), command.actualWidthCm(), command.actualHeightCm());
        shipment.assignOriginHub(hub.getId());
        shipment.receiveAt(hub);
        shipmentEventRepository.save(ShipmentEvent.arrivedAtHub(shipment, user, hub));
    }

    private boolean positive(Integer value) { return value != null && value > 0; }

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
