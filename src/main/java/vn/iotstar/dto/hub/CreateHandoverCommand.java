package vn.iotstar.dto.hub;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateHandoverCommand(@NotNull Long destinationHubId, @NotNull Long receiverDriverId, @NotEmpty List<Long> containerIds) { }
