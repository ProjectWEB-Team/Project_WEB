package vn.iotstar.dto.delivery;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
public record DeliveryAssignmentRequest(@NotNull @Positive Long shipmentId, @NotNull @Positive Long shipperId) { }
