package vn.iotstar.entity;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Locks the ADR-defined role and shipment-state contracts against accidental expansion. */
class CanonicalDomainContractTest {
    @Test
    void declaresExactlyTheSixCanonicalRoles() {
        assertThat(Arrays.stream(UserRole.values()).collect(java.util.stream.Collectors.toSet()))
                .isEqualTo(Set.of(UserRole.ROLE_USER, UserRole.ROLE_SHIPPER, UserRole.ROLE_HUB_STAFF,
                        UserRole.ROLE_LINEHAUL_DRIVER, UserRole.ROLE_MANAGER, UserRole.ROLE_ADMIN));
    }

    @Test
    void declaresExactlyTheThirteenCanonicalShipmentStatuses() {
        assertThat(Arrays.stream(ShipmentStatus.values()).collect(java.util.stream.Collectors.toSet()))
                .isEqualTo(Set.of(ShipmentStatus.CREATED, ShipmentStatus.WAITING_FOR_PAYMENT,
                        ShipmentStatus.WAITING_FOR_HANDOVER, ShipmentStatus.PICKED_UP, ShipmentStatus.AT_HUB,
                        ShipmentStatus.IN_TRANSIT, ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.FAILED,
                        ShipmentStatus.DELIVERED, ShipmentStatus.RETURN_TO_SENDER, ShipmentStatus.RETURNED_TO_SENDER,
                        ShipmentStatus.CANCELLED, ShipmentStatus.CLOSED_EXCEPTION));
    }
}
