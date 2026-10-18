package vn.iotstar.dto.hub;

import jakarta.validation.constraints.NotNull;

public record ScanHandoverContainerCommand(@NotNull Long handoverId, @NotNull Long containerId) { }
