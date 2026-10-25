package vn.iotstar.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.firstmile.PendingPickupResponse;
import vn.iotstar.dto.firstmile.PickupTaskResponse;
import vn.iotstar.dto.firstmile.ShipperOptionResponse;
import vn.iotstar.dto.realtime.ShipmentCreatedEventResponse;
import vn.iotstar.entity.EmployeeProfile;
import vn.iotstar.entity.Payment;
import vn.iotstar.entity.PickupAttempt;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentAssignment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.exception.ShipmentAccessDeniedException;
import vn.iotstar.realtime.ShipmentEventPublisher;
import vn.iotstar.repository.EmployeeProfileRepository;
import vn.iotstar.repository.PaymentRepository;
import vn.iotstar.repository.PickupAttemptRepository;
import vn.iotstar.repository.ShipmentAssignmentRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.FirstMileService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Same-hub pickup assignment and completion. Every mutation locks the shipment row first, so a concurrent
 * cancellation and pickup completion have exactly one winner.
 */
@Service
public class FirstMileServiceImpl implements FirstMileService {
    private static final Set<String> COORDINATOR_POSITIONS = Set.of("COORDINATOR", "GENERAL_STAFF");
    private static final Set<String> FAILED_RESULTS = Set.of("FAILED_NO_ANSWER", "FAILED_SENDER_REFUSED", "FAILED_OTHER");

    private final ShipmentRepository shipmentRepository;
    private final ShipmentAssignmentRepository assignmentRepository;
    private final PickupAttemptRepository attemptRepository;
    private final EmployeeProfileRepository profileRepository;
    private final PaymentRepository paymentRepository;
    private final ShipmentEventRepository eventRepository;
    private final ShipmentEventPublisher publisher;
    private final ShipmentCancellation cancellation;
    private final Clock clock;

    public FirstMileServiceImpl(ShipmentRepository shipmentRepository, ShipmentAssignmentRepository assignmentRepository,
                                PickupAttemptRepository attemptRepository, EmployeeProfileRepository profileRepository,
                                PaymentRepository paymentRepository, ShipmentEventRepository eventRepository,
                                ShipmentEventPublisher publisher, ShipmentCancellation cancellation, Clock clock) {
        this.shipmentRepository = shipmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.attemptRepository = attemptRepository;
        this.profileRepository = profileRepository;
        this.paymentRepository = paymentRepository;
        this.eventRepository = eventRepository;
        this.publisher = publisher;
        this.cancellation = cancellation;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingPickupResponse> pendingPickups() {
        EmployeeProfile coordinator = requireCoordinator();
        return shipmentRepository.findPendingPickups(coordinator.getHubId(), ShipmentStatus.WAITING_FOR_HANDOVER).stream()
                .map(s -> new PendingPickupResponse(s.getId(), s.getTrackingNumber(), s.getSenderName(), s.getSenderPhone(),
                        s.getSenderAddress(), s.getWaitingForHandoverAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipperOptionResponse> shippersOfMyHub() {
        EmployeeProfile coordinator = requireCoordinator();
        return profileRepository.findByHubIdAndStatusAndUserRole(coordinator.getHubId(), "ACTIVE", UserRole.ROLE_SHIPPER).stream()
                .filter(p -> p.getUser().isEnabled())
                .map(p -> new ShipperOptionResponse(p.getUser().getId(), p.getUser().getFullName()))
                .toList();
    }

    @Override
    @Transactional
    public void assignPickup(Long shipmentId, Long shipperUserId) {
        EmployeeProfile coordinator = requireCoordinator();
        Shipment shipment = lockPickupShipment(shipmentId, coordinator.getHubId());
        if (assignmentRepository.findFirstByShipmentIdAndAssignmentTypeAndStatus(shipmentId,
                ShipmentAssignment.PICKUP, ShipmentAssignment.ASSIGNED).isPresent()) {
            throw new IllegalArgumentException("Đơn đã được phân công lấy hàng.");
        }
        EmployeeProfile shipper = profileRepository.findFirstByUserId(shipperUserId)
                .filter(p -> isActiveAt(p, UserRole.ROLE_SHIPPER, coordinator.getHubId()))
                .orElseThrow(() -> new IllegalArgumentException("Shipper không hợp lệ cho bưu cục này."));
        LocalDateTime now = ServerTime.now(clock);
        shipment.assignOriginHub(coordinator.getHubId());
        assignmentRepository.save(ShipmentAssignment.pickup(shipment, shipper.getUser(), now));
        eventRepository.save(ShipmentEvent.of(shipment, coordinator.getUser(), "PICKUP_TASK_ASSIGNED", "Pickup task assigned"));
        notify(shipment.getSender().getUsername(), shipment, "PICKUP_TASK_ASSIGNED", now);
        notify(shipper.getUser().getUsername(), shipment, "PICKUP_TASK_ASSIGNED", now);
    }

    @Override
    @Transactional
    public void reportServiceFailure(Long shipmentId, String note) {
        if (note == null || note.isBlank() || note.length() > 200) {
            throw new IllegalArgumentException("Vui lòng ghi lý do lỗi dịch vụ (tối đa 200 ký tự).");
        }
        EmployeeProfile coordinator = requireCoordinator();
        Shipment shipment = lockPickupShipment(shipmentId, coordinator.getHubId());
        eventRepository.save(ShipmentEvent.of(shipment, coordinator.getUser(), "PICKUP_SERVICE_FAILURE", note.trim()));
        cancellation.cancel(shipment, "PICKUP_SERVICE_FAILURE", coordinator.getUser());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PickupTaskResponse> myPickupTasks() {
        EmployeeProfile shipper = requireShipper();
        return assignmentRepository.findByAssigneeIdAndAssignmentTypeAndStatusOrderByAssignedAtAsc(shipper.getUser().getId(),
                        ShipmentAssignment.PICKUP, ShipmentAssignment.ASSIGNED).stream()
                .map(a -> {
                    Shipment s = a.getShipment();
                    return new PickupTaskResponse(a.getId(), s.getTrackingNumber(), s.getSenderName(), s.getSenderPhone(),
                            s.getSenderAddress(), cashToCollect(s).map(Payment::getAmount).orElse(null), a.getAssignedAt(),
                            attemptRepository.countByAssignmentId(a.getId()));
                })
                .toList();
    }

    @Override
    @Transactional
    public void recordFailedAttempt(Long assignmentId, String result, String note) {
        if (!FAILED_RESULTS.contains(result)) {
            throw new IllegalArgumentException("Kết quả liên hệ không hợp lệ.");
        }
        if (note != null && note.length() > 255) {
            throw new IllegalArgumentException("Ghi chú tối đa 255 ký tự.");
        }
        EmployeeProfile shipper = requireShipper();
        ShipmentAssignment assignment = lockOwnTask(assignmentId, shipper);
        LocalDateTime now = ServerTime.now(clock);
        attemptRepository.save(PickupAttempt.of(assignment, attemptRepository.countByAssignmentId(assignmentId) + 1,
                result, note, now));
        eventRepository.save(ShipmentEvent.of(assignment.getShipment(), shipper.getUser(), "PICKUP_ATTEMPT_FAILED", result));
    }

    @Override
    @Transactional
    public void completePickup(Long assignmentId, String scannedTrackingNumber) {
        EmployeeProfile shipper = requireShipper();
        ShipmentAssignment assignment = lockOwnTask(assignmentId, shipper);
        Shipment shipment = assignment.getShipment();
        if (scannedTrackingNumber == null || !shipment.getTrackingNumber().equalsIgnoreCase(scannedTrackingNumber.trim())) {
            throw new IllegalArgumentException("Mã vận đơn quét không khớp với nhiệm vụ.");
        }
        LocalDateTime now = ServerTime.now(clock);
        // Sender cash is collected exactly once, for the server-computed amount; a retry finds it PAID already.
        boolean cashCollected = cashToCollect(shipment).map(p -> {
            p.collect("CASH", shipper.getUser().getId(), "PICKUP-CASH-" + assignmentId);
            return true;
        }).orElse(false);
        if (shipper.getHub() != null) {
            shipment.markPickedUp(now, shipper.getHub(), cashCollected);
        } else {
            // Compatibility for isolated fixtures that represent the scoped hub by id only.
            shipment.markPickedUp(now, shipper.getHubId(), cashCollected);
        }
        assignment.complete(now);
        attemptRepository.save(PickupAttempt.of(assignment, attemptRepository.countByAssignmentId(assignmentId) + 1,
                "SUCCESS", null, now));
        eventRepository.save(ShipmentEvent.of(shipment, shipper.getUser(), "PICKED_UP", "Parcel picked up by shipper"));
        notify(shipment.getSender().getUsername(), shipment, "PICKED_UP", now);
    }

    private Shipment lockPickupShipment(Long shipmentId, Long hubId) {
        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment"));
        if (shipment.getOriginHubId() != null && !shipment.getOriginHubId().equals(hubId)) {
            throw new ShipmentAccessDeniedException();
        }
        if (!"HOME_PICKUP".equals(shipment.getPickupMethod())
                || shipment.getCurrentStatus() != ShipmentStatus.WAITING_FOR_HANDOVER) {
            throw new IllegalArgumentException("Đơn không còn chờ lấy hàng.");
        }
        return shipment;
    }

    /** Locks the task's shipment, then re-checks the task: the shipment status is the race arbiter. */
    private ShipmentAssignment lockOwnTask(Long assignmentId, EmployeeProfile shipper) {
        ShipmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment"));
        if (!assignment.getAssignee().getId().equals(shipper.getUser().getId())) {
            throw new ShipmentAccessDeniedException();
        }
        Shipment shipment = shipmentRepository.findByIdForUpdate(assignment.getShipment().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment"));
        if (!assignment.isActive() || shipment.getCurrentStatus() != ShipmentStatus.WAITING_FOR_HANDOVER) {
            throw new IllegalArgumentException("Nhiệm vụ lấy hàng không còn hiệu lực.");
        }
        if (!shipper.getHubId().equals(shipment.getOriginHubId())) {
            throw new ShipmentAccessDeniedException();
        }
        return assignment;
    }

    private Optional<Payment> cashToCollect(Shipment shipment) {
        return paymentRepository.findFirstByShipmentIdAndPaymentType(shipment.getId(), "SHIPPING_FEE")
                .filter(p -> "SENDER".equals(p.getPayerType()) && "CASH".equals(p.getMethod()) && !"PAID".equals(p.getStatus()));
    }

    private EmployeeProfile requireCoordinator() {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        return profileRepository.findFirstByUserId(actor.userId())
                .filter(p -> isActiveAt(p, UserRole.ROLE_HUB_STAFF, p.getHubId()) && COORDINATOR_POSITIONS.contains(p.getPosition()))
                .orElseThrow(ShipmentAccessDeniedException::new);
    }

    private EmployeeProfile requireShipper() {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        return profileRepository.findFirstByUserId(actor.userId())
                .filter(p -> isActiveAt(p, UserRole.ROLE_SHIPPER, p.getHubId()))
                .orElseThrow(ShipmentAccessDeniedException::new);
    }

    private static boolean isActiveAt(EmployeeProfile profile, UserRole role, Long hubId) {
        User user = profile.getUser();
        return profile.isActive() && hubId != null && hubId.equals(profile.getHubId())
                && user.getRole() == role && user.isEnabled();
    }

    private void notify(String username, Shipment shipment, String eventType, LocalDateTime now) {
        publisher.publishToUserAfterCommit(username, new ShipmentCreatedEventResponse(shipment.getTrackingNumber(),
                shipment.getCurrentStatus(), eventType, now));
    }
}
