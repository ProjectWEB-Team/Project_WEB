package vn.iotstar.dto.hub;

import jakarta.validation.constraints.NotNull;

public record LoadContainerCommand(@NotNull Long containerId, @NotNull Long shipmentId) { }
