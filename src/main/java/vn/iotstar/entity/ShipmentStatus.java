package vn.iotstar.entity;

/**
 * Canonical shipment lifecycle states from ADR-014. Operational activity is
 * recorded separately in {@link ShipmentEvent} and must not extend this enum.
 */
public enum ShipmentStatus {
    CREATED,
    WAITING_FOR_PAYMENT,
    WAITING_FOR_HANDOVER,
    PICKED_UP,
    AT_HUB,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    FAILED,
    DELIVERED,
    RETURN_TO_SENDER,
    RETURNED_TO_SENDER,
    CANCELLED,
    CLOSED_EXCEPTION
}
