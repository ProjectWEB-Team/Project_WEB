package vn.iotstar.service.impl;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.config.LogisticsProperties;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.ShipmentReturnService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Return line of the Workflow: a parcel travels back to the origin hub, waits
 * for the sender during a grace period, and is disposed as an exception when
 * the sender neither collects it nor pays the return fee.
 */
@Service
@Transactional
public class ShipmentReturnServiceImpl implements ShipmentReturnService {

    /** Event that marks the arrival of a returned parcel at the origin hub. */
    public static final String EVENT_RETURNED_AT_ORIGIN_HUB = "RETURNED_AT_ORIGIN_HUB";

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final UserRepository userRepository;
    private final LogisticsProperties logisticsProperties;

    public ShipmentReturnServiceImpl(ShipmentRepository shipmentRepository,
                                      ShipmentEventRepository shipmentEventRepository,
                                      UserRepository userRepository,
                                      LogisticsProperties logisticsProperties) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentEventRepository = shipmentEventRepository;
        this.userRepository = userRepository;
        this.logisticsProperties = logisticsProperties;
    }

    @Override
    public Shipment markReturnArrivedAtOriginHub(Long shipmentId) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (!actor.hasRole(UserRole.ROLE_HUB_STAFF) && !actor.hasRole(UserRole.ROLE_MANAGER)
                && !actor.hasRole(UserRole.ROLE_ADMIN)) {
            throw new AccessDeniedException("Hub staff, manager or administrator required to receive a return");
        }

        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));

        if (!ShipmentStatus.RETURN_TO_SENDER.equals(shipment.getCurrentStatus())) {
            throw new IllegalStateException("Shipment must be RETURN_TO_SENDER to arrive at the origin hub. Current: "
                    + shipment.getCurrentStatus());
        }

        User currentUser = userRepository.findById(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + actor.userId()));

        shipmentEventRepository.save(ShipmentEvent.create(
                shipment, currentUser, EVENT_RETURNED_AT_ORIGIN_HUB,
                "Returned parcel arrived at the origin hub and waits for the sender"));

        return shipment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> findOverdueReturnedShipments(LocalDateTime referenceTime) {
        LocalDateTime deadline = referenceTime.minus(gracePeriod());
        return shipmentRepository.findByCurrentStatus(ShipmentStatus.RETURN_TO_SENDER).stream()
                .filter(shipment -> gracePeriodStart(shipment).isBefore(deadline))
                .toList();
    }

    @Override
    public Shipment disposeOverdueReturn(Long shipmentId, String reason) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (!actor.hasRole(UserRole.ROLE_MANAGER) && !actor.hasRole(UserRole.ROLE_ADMIN)) {
            throw new AccessDeniedException("ROLE_MANAGER or ROLE_ADMIN required to dispose an overdue return");
        }

        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));

        if (ShipmentStatus.CLOSED_EXCEPTION.equals(shipment.getCurrentStatus())) {
            return shipment;
        }
        if (!ShipmentStatus.RETURN_TO_SENDER.equals(shipment.getCurrentStatus())) {
            throw new IllegalStateException("Only a parcel waiting for the sender can be disposed. Current: "
                    + shipment.getCurrentStatus());
        }

        User manager = userRepository.findById(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found: " + actor.userId()));

        shipment.transitionTo(ShipmentStatus.CLOSED_EXCEPTION);
        shipment.setClosureReason("DISPOSED");
        shipment = shipmentRepository.save(shipment);

        shipmentEventRepository.save(ShipmentEvent.create(
                shipment, manager, "RETURN_DISPOSED",
                "Returned parcel disposed after the grace period: " + (reason != null ? reason : "no reason given")));

        return shipment;
    }

    /**
     * The grace period starts when the parcel reaches the origin hub. The
     * arrival is read from the event history so no extra column is needed.
     */
    private LocalDateTime gracePeriodStart(Shipment shipment) {
        Optional<ShipmentEvent> arrival = shipmentEventRepository
                .findFirstByShipmentIdAndEventTypeOrderByCreatedAtDesc(shipment.getId(), EVENT_RETURNED_AT_ORIGIN_HUB);
        if (arrival.isPresent()) {
            return arrival.get().getCreatedAt();
        }
        return shipment.getCreatedAt() != null ? shipment.getCreatedAt() : LocalDateTime.now();
    }

    private Duration gracePeriod() {
        int minutes = logisticsProperties.getTimeout().getDisposedGraceMinutes();
        if (minutes > 0) {
            return Duration.ofMinutes(minutes);
        }
        return Duration.ofDays(logisticsProperties.getTimeout().getDisposedGraceDays());
    }
}
