package vn.iotstar.dto.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateContainerCommand(@NotBlank String containerCode, @NotNull Long destinationHubId) { }
