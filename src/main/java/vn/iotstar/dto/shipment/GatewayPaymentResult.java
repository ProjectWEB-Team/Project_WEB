package vn.iotstar.dto.shipment;

/** Outcome of applying a payment-gateway confirmation to a shipment. */
public enum GatewayPaymentResult { CONFIRMED, ALREADY_CONFIRMED, NOT_FOUND, AMOUNT_MISMATCH, NOT_PAYABLE }
