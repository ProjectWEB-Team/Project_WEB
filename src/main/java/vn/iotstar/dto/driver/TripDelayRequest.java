package vn.iotstar.dto.driver;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** API body deliberately excludes the trip owner/actor identity. */
public record TripDelayRequest(@NotBlank @Size(max = 500) String note,
                               @Size(max = 120) String evidenceReference) { }
