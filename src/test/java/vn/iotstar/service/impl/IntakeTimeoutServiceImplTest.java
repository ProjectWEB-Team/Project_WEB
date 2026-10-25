package vn.iotstar.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.repository.ShipmentAssignmentRepository;
import vn.iotstar.repository.ShipmentRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntakeTimeoutServiceImplTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 23, 12, 0, 0);
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private ShipmentAssignmentRepository assignmentRepository;
    @Mock private ShipmentCancellation cancellation;

    @Test
    void cutoffIsExactlyFortyEightHoursSoTheBoundaryIsInclusive() {
        // 48:00:00 old matches (<= cutoff); 47:59:59 old does not.
        service(48, 0).cancelExpired();
        verify(assignmentRepository).findExpiredPickupShipmentIds(NOW.minusHours(48));
        verify(shipmentRepository).findExpiredDropoffIds(ShipmentStatus.WAITING_FOR_HANDOVER, NOW.minusHours(48));
        assertThat(NOW.minusHours(48).plusSeconds(1).isAfter(NOW.minusHours(48))).isTrue();
    }

    @Test
    void demoMinutesShortenTheWindow() {
        service(0, 2).cancelExpired();
        verify(assignmentRepository).findExpiredPickupShipmentIds(NOW.minusMinutes(2));
    }

    @Test
    void cancelsStillWaitingShipmentsAndSkipsOnesPickedUpMeanwhile() {
        Shipment waiting = shipment(1L, "SELF_DROPOFF", ShipmentStatus.WAITING_FOR_HANDOVER);
        Shipment pickedUp = shipment(2L, "HOME_PICKUP", ShipmentStatus.PICKED_UP);
        when(assignmentRepository.findExpiredPickupShipmentIds(any())).thenReturn(List.of(2L));
        when(shipmentRepository.findExpiredDropoffIds(any(), any())).thenReturn(List.of(1L));
        when(shipmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(waiting));
        when(shipmentRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(pickedUp));

        assertThat(service(48, 0).cancelExpired()).isEqualTo(1);
        verify(cancellation).cancel(waiting, "DROPOFF_TIMEOUT", null);
        verify(cancellation, never()).cancel(eq(pickedUp), any(), any());
    }

    private IntakeTimeoutServiceImpl service(long hours, long minutes) {
        Clock clock = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        return new IntakeTimeoutServiceImpl(shipmentRepository, assignmentRepository, cancellation, clock,
                hours, minutes, hours, minutes);
    }

    private static Shipment shipment(Long id, String method, ShipmentStatus status) {
        User sender = new User(7L, "sender", "hash", UserRole.ROLE_USER, "ACTIVE");
        Shipment s = Shipment.create("UTX-" + id, sender, "S", "0900000001", "a", "R", "+84900000002", "b");
        s.applyIntake(null, "HCM", "Q1", method, "SENDER", "NO_INSPECTION", 500, 1, 1, 1, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.TEN, "UNPAID", status, NOW.minusDays(3));
        ReflectionTestUtils.setField(s, "id", id);
        return s;
    }
}
