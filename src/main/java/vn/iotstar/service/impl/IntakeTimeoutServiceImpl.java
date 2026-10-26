package vn.iotstar.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.repository.ShipmentAssignmentRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.service.IntakeTimeoutService;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * HOME_PICKUP: 48h from assigned_at. SELF_DROPOFF: 48h from waiting_for_handover_at (never created_at).
 * The demo profile shortens both through the *-minutes properties.
 */
@Service
public class IntakeTimeoutServiceImpl implements IntakeTimeoutService {
    private final ShipmentRepository shipmentRepository;
    private final ShipmentAssignmentRepository assignmentRepository;
    private final ShipmentCancellation cancellation;
    private final Clock clock;
    private final Duration pickupWindow;
    private final Duration dropoffWindow;

    public IntakeTimeoutServiceImpl(ShipmentRepository shipmentRepository, ShipmentAssignmentRepository assignmentRepository,
                                    ShipmentCancellation cancellation, Clock clock,
                                    @Value("${uteexpress.timeout.pickup-hours:48}") long pickupHours,
                                    @Value("${uteexpress.timeout.pickup-minutes:0}") long pickupMinutes,
                                    @Value("${uteexpress.timeout.dropoff-hours:48}") long dropoffHours,
                                    @Value("${uteexpress.timeout.dropoff-minutes:0}") long dropoffMinutes) {
        this.shipmentRepository = shipmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.cancellation = cancellation;
        this.clock = clock;
        this.pickupWindow = Duration.ofHours(pickupHours).plusMinutes(pickupMinutes);
        this.dropoffWindow = Duration.ofHours(dropoffHours).plusMinutes(dropoffMinutes);
    }

    @Override
    @Scheduled(fixedDelayString = "${uteexpress.timeout.scan-interval-ms:60000}")
    @Transactional
    public int cancelExpired() {
        LocalDateTime now = ServerTime.now(clock);
        int cancelled = 0;
        for (Long id : assignmentRepository.findExpiredPickupShipmentIds(now.minus(pickupWindow))) {
            cancelled += cancelIfStillWaiting(id, "HOME_PICKUP", "PICKUP_TIMEOUT");
        }
        LocalDateTime dropoffCutoff = now.minus(dropoffWindow);
        for (Long id : shipmentRepository.findExpiredDropoffIds(ShipmentStatus.WAITING_FOR_HANDOVER, dropoffCutoff)) {
            cancelled += cancelIfStillWaiting(id, "SELF_DROPOFF", "DROPOFF_TIMEOUT");
        }
        return cancelled;
    }

    /** Re-checks under the row lock: a pickup completed or cancelled meanwhile wins and the job skips it. */
    private int cancelIfStillWaiting(Long shipmentId, String pickupMethod, String reason) {
        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId).orElse(null);
        if (shipment == null || shipment.getCurrentStatus() != ShipmentStatus.WAITING_FOR_HANDOVER
                || !pickupMethod.equals(shipment.getPickupMethod())) {
            return 0;
        }
        cancellation.cancel(shipment, reason, null);
        return 1;
    }
}
