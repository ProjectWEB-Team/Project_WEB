package vn.iotstar.dto.finance;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CompensationRequest(@NotNull @Positive Long shipmentId, @NotNull @Positive Long incidentId,
                                  @NotBlank @Size(max=255) String reason) { }
