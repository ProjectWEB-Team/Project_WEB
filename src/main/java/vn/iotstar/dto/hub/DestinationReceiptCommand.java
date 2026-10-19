package vn.iotstar.dto.hub;

import jakarta.validation.constraints.NotNull;

/** A physical destination-inbound scan; hub and actor are always derived server-side. */
public record DestinationReceiptCommand(
        @NotNull Long handoverId,
        @NotNull Long containerId,
        @NotNull Long shipmentId) {
}
