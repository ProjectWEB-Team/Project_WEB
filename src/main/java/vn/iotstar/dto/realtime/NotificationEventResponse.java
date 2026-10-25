package vn.iotstar.dto.realtime;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Private action hint. Financial details are fetched again through scoped services. */
public class NotificationEventResponse implements Serializable {
    private final String eventType;
    private final String reference;
    private final String message;
    public NotificationEventResponse(String eventType, String reference, String message) {
        this.eventType=eventType; this.reference=reference; this.message=message;
    }
    /** Compatibility for operational producers; sensitive arguments never enter the payload. */
    public NotificationEventResponse(String eventType, String trackingNumber, String shipmentStatus,
                                     String amount, String reference, String message, LocalDateTime occurredAt) {
        this(eventType, reference, message);
    }
    public String getEventType() { return eventType; }
    public String getReference() { return reference; }
    public String getMessage() { return message; }
}
