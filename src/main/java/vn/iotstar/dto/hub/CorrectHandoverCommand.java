package vn.iotstar.dto.hub;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CorrectHandoverCommand(@NotNull Long handoverId, @NotEmpty List<Long> replacementContainerIds) { }
