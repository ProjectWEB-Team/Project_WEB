package vn.iotstar.dto.firstmile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A shipper's own pickup task; cashToCollect is server-computed and null when nothing is collected. */
public record PickupTaskResponse(Long assignmentId, String trackingNumber, String senderName, String senderPhone,
                                 String senderAddress, BigDecimal cashToCollect, LocalDateTime assignedAt, int attempts) {
    public Long getAssignmentId() { return assignmentId; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getSenderName() { return senderName; }
    public String getSenderPhone() { return senderPhone; }
    public String getSenderAddress() { return senderAddress; }
    public BigDecimal getCashToCollect() { return cashToCollect; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public int getAttempts() { return attempts; }
}
