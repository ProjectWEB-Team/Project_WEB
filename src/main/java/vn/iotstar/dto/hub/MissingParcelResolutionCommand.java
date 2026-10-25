package vn.iotstar.dto.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MissingParcelResolutionCommand(
        @NotNull @Positive Long incidentId, @NotNull @Positive Long handoverId,
        @NotNull @Positive Long containerId, @NotNull @Positive Long shipmentId,
        @NotNull @Positive Long foundHubId,
        @NotBlank @Pattern(regexp = "FOUND|CONFIRMED_LOST") String outcome,
        @NotBlank @Size(max = 500) String evidence) { }
