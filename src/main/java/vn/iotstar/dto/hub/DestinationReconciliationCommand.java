package vn.iotstar.dto.hub;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Actual physical item scans for one arrived destination container. */
public record DestinationReconciliationCommand(
        @NotNull Long handoverId,
        @NotNull Long containerId,
        @NotNull @Size(max = 10000) List<@NotNull Long> receivedShipmentIds) {
}
