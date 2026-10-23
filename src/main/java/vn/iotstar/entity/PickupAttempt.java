package vn.iotstar.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pickup_attempts")
public class PickupAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @Column(name = "attempt_number", nullable = false) private int attemptNumber;
    @Column(name = "attempt_time", nullable = false, columnDefinition = "DATETIME2(0)") private LocalDateTime attemptTime;
    @Column(nullable = false) private String result;
    @Column(length = 255, columnDefinition = "NVARCHAR(255)") private String note;

    protected PickupAttempt() { }

    public static PickupAttempt of(ShipmentAssignment assignment, int attemptNumber, String result, String note, LocalDateTime at) {
        PickupAttempt p = new PickupAttempt();
        p.assignment = assignment; p.shipment = assignment.getShipment(); p.shipper = assignment.getAssignee();
        p.attemptNumber = attemptNumber; p.result = result; p.note = note; p.attemptTime = at;
        return p;
    }

    public int getAttemptNumber() { return attemptNumber; }
    public String getResult() { return result; }
}
