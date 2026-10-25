package vn.iotstar.dto.delivery;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.time.LocalDate;

public class DeliveryFailureRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Shipment ID is required")
    private Long shipmentId;

    @NotBlank(message = "Failure reason is required")
    private String reason; // NO_ANSWER, PAYMENT_NOT_AVAILABLE, RESCHEDULED, OTHER, RECEIVER_REFUSED, WRONG_ADDRESS

    private String note;

    private String evidenceUrl;

    /** Optional photo evidence of the failed delivery attempt. */
    private MultipartFile evidence;

    private LocalDate rescheduledDate;

    public DeliveryFailureRequest() {
    }

    public DeliveryFailureRequest(Long shipmentId, String reason, String note, String evidenceUrl, LocalDate rescheduledDate) {
        this.shipmentId = shipmentId;
        this.reason = reason;
        this.note = note;
        this.evidenceUrl = evidenceUrl;
        this.rescheduledDate = rescheduledDate;
    }

    public Long getShipmentId() { return shipmentId; }
    public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }
    public MultipartFile getEvidence() { return evidence; }
    public void setEvidence(MultipartFile evidence) { this.evidence = evidence; }
    public LocalDate getRescheduledDate() { return rescheduledDate; }
    public void setRescheduledDate(LocalDate rescheduledDate) { this.rescheduledDate = rescheduledDate; }
}

