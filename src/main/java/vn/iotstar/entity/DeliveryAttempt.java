package vn.iotstar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_attempts")
public class DeliveryAttempt implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private ShipmentAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipper_id", nullable = false)
    private User shipper;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "attempt_date", nullable = false)
    private LocalDate attemptDate;

    @Column(name = "attempt_time", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime attemptTime;

    @Column(name = "rescheduled_date")
    private LocalDate rescheduledDate;

    @Column(name = "result", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String result;

    @Column(name = "note", length = 255, columnDefinition = "NVARCHAR(255)")
    private String note;

    @Column(name = "evidence_url", length = 255, columnDefinition = "NVARCHAR(255)")
    private String evidenceUrl;

    protected DeliveryAttempt() {
    }

    public static DeliveryAttempt create(ShipmentAssignment assignment, Shipment shipment, User shipper,
                                         int attemptNumber, LocalDate attemptDate, String result,
                                         String note, String evidenceUrl, LocalDate rescheduledDate) {
        DeliveryAttempt attempt = new DeliveryAttempt();
        attempt.assignment = assignment;
        attempt.shipment = shipment;
        attempt.shipper = shipper;
        attempt.attemptNumber = attemptNumber;
        attempt.attemptDate = attemptDate;
        attempt.attemptTime = LocalDateTime.now();
        attempt.result = result;
        attempt.note = note;
        attempt.evidenceUrl = evidenceUrl;
        attempt.rescheduledDate = rescheduledDate;
        return attempt;
    }

    @PrePersist
    void onPrePersist() {
        if (attemptTime == null) {
            attemptTime = LocalDateTime.now();
        }
        if (attemptDate == null) {
            attemptDate = LocalDate.now();
        }
    }

    public Long getId() { return id; }
    public ShipmentAssignment getAssignment() { return assignment; }
    public Shipment getShipment() { return shipment; }
    public User getShipper() { return shipper; }
    public int getAttemptNumber() { return attemptNumber; }
    public LocalDate getAttemptDate() { return attemptDate; }
    public LocalDateTime getAttemptTime() { return attemptTime; }
    public LocalDate getRescheduledDate() { return rescheduledDate; }
    public String getResult() { return result; }
    public String getNote() { return note; }
    public String getEvidenceUrl() { return evidenceUrl; }
}

