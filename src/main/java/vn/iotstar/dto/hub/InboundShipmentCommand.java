package vn.iotstar.dto.hub;

import jakarta.validation.constraints.NotNull;

/** Browser command intentionally contains no caller-controlled hub or actor identity. */
public record InboundShipmentCommand(@NotNull Long shipmentId) { }
