package vn.iotstar.dto.hub;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Actual physical item scans for one arrived destination container. */
public record DestinationReconciliationCommand(
        @NotNull Long handoverId,
        @NotNull Long containerId,
        @NotEmpty List<@NotNull Long> receivedShipmentIds) {
}
