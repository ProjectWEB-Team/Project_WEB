package vn.iotstar.dto.hub;

import jakarta.validation.constraints.NotNull;

/** Browser command intentionally contains no caller-controlled hub or actor identity. */
public record InboundShipmentCommand(@NotNull Long shipmentId, String trackingNumber,
        Integer actualWeightGrams, Integer actualLengthCm, Integer actualWidthCm, Integer actualHeightCm,
        java.math.BigDecimal cashReceived, String surchargeDecision) {
    /** Pickup receipts retain their existing command; self-dropoff requires physical evidence. */
    public static InboundShipmentCommand pickupReceipt(Long shipmentId) {
        return new InboundShipmentCommand(shipmentId, null, null, null, null, null, null, null);
    }
}
