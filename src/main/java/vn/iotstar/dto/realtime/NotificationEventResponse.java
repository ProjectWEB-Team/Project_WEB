package vn.iotstar.dto.realtime;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Private realtime notification for the last-mile and finance slice.
 *
 * <p>The payload is intentionally narrow: it never carries addresses, contact
 * details, payment credentials or internal financial notes.
 */
public class NotificationEventResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventType;
    private String trackingNumber;
    private String shipmentStatus;
    private String amount;
    private String reference;
    private String message;
    private LocalDateTime occurredAt;

    public NotificationEventResponse() {
    }

    public NotificationEventResponse(String eventType, String trackingNumber, String shipmentStatus,
                                     String amount, String reference, String message, LocalDateTime occurredAt) {
        this.eventType = eventType;
        this.trackingNumber = trackingNumber;
        this.shipmentStatus = shipmentStatus;
        this.amount = amount;
        this.reference = reference;
        this.message = message;
        this.occurredAt = occurredAt;
    }

    public String getEventType() { return eventType; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getShipmentStatus() { return shipmentStatus; }
    public String getAmount() { return amount; }
    public String getReference() { return reference; }
    public String getMessage() { return message; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}