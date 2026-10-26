package vn.iotstar.dto.hub;

import jakarta.validation.constraints.NotNull;

/** Browser command intentionally contains no caller-selected next hub. */
public record SortShipmentCommand(@NotNull Long shipmentId) { }
