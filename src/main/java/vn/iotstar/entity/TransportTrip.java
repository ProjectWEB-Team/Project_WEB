package vn.iotstar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/** Accountable assigned-driver transport lifecycle; destination custody remains a separate action. */
@Entity
@Table(name = "transport_trips")
public class TransportTrip {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "trip_code", nullable = false, unique = true, length = 80, columnDefinition = "NVARCHAR(80)") private String tripCode;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "origin_hub_id", nullable = false) private Hub originHub;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "destination_hub_id", nullable = false) private Hub destinationHub;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "driver_user_id", nullable = false) private User driver;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "vehicle_id", nullable = false) private Vehicle vehicle;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "handover_id", unique = true) private Handover handover;
    @Column(nullable = false, length = 30, columnDefinition = "NVARCHAR(30)") private String status;
    @Column(name = "accepted_at", nullable = false, columnDefinition = "DATETIME2(0)") private LocalDateTime acceptedAt;
    @Column(name = "departure_time", columnDefinition = "DATETIME2(0)") private LocalDateTime startedAt;
    @Column(name = "arrival_time", columnDefinition = "DATETIME2(0)") private LocalDateTime arrivedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "accepted_by_user_id", nullable = false) private User acceptedBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "started_by_user_id") private User startedBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "arrived_by_user_id") private User arrivedBy;
    @Version @Column(nullable = false) private Long version;
    protected TransportTrip() { }

    public static TransportTrip accepted(Handover handover, User driver, Vehicle vehicle) {
        TransportTrip trip = new TransportTrip();
        trip.tripCode = "TRIP-" + handover.getId();
        trip.originHub = handover.getFromHub();
        trip.destinationHub = handover.getToHub();
        trip.driver = driver;
        trip.vehicle = java.util.Objects.requireNonNull(vehicle, "Vehicle required");
        trip.handover = handover;
        trip.status = "SCHEDULED";
        trip.acceptedBy = driver;
        trip.acceptedAt = LocalDateTime.now();
        return trip;
    }
    public boolean isAssignedTo(Long userId) { return driver != null && driver.getId().equals(userId); }
    public void start(User actor) {
        if ("IN_TRANSIT".equals(status)) return;
        if (!"SCHEDULED".equals(status)) throw new IllegalStateException("Trip is not ready to start");
        status = "IN_TRANSIT"; startedBy = actor; startedAt = LocalDateTime.now();
    }
    public void arrive(User actor) {
        if ("ARRIVED".equals(status) || isCompleted()) return;
        if (!"IN_TRANSIT".equals(status)) throw new IllegalStateException("Trip is not in transit");
        status = "ARRIVED"; arrivedBy = actor; arrivedAt = LocalDateTime.now();
    }
    public boolean isCompleted() { return "COMPLETED".equals(status) || "COMPLETED_WITH_DISCREPANCY".equals(status); }
    public void completeReceipt(boolean discrepancy) {
        if (isCompleted()) return;
        if (!"ARRIVED".equals(status)) throw new IllegalStateException("Trip has not arrived");
        status = discrepancy ? "COMPLETED_WITH_DISCREPANCY" : "COMPLETED";
        vehicle.release();
    }
    public Long getId() { return id; }
    public String getTripCode() { return tripCode; }
    public Hub getOriginHub() { return originHub; }
    public Hub getDestinationHub() { return destinationHub; }
    public User getDriver() { return driver; }
    public Vehicle getVehicle() { return vehicle; }
    public Handover getHandover() { return handover; }
    public String getStatus() { return status; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getArrivedAt() { return arrivedAt; }
}
