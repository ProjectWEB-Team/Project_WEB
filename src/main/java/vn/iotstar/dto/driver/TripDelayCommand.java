package vn.iotstar.dto.driver;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TripDelayCommand(@NotNull Long tripId,
                               @NotBlank @Size(max = 500) String note,
                               @Size(max = 120) String evidenceReference) { }
