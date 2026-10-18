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
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "driver_user_id") private User driver;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "handover_id", unique = true) private Handover handover;
    @Column(nullable = false, length = 30, columnDefinition = "NVARCHAR(30)") private String status;
    @Column(name = "accepted_at", nullable = false, columnDefinition = "DATETIME2(0)") private LocalDateTime acceptedAt;
    @Column(name = "started_at", columnDefinition = "DATETIME2(0)") private LocalDateTime startedAt;
    @Column(name = "arrived_at", columnDefinition = "DATETIME2(0)") private LocalDateTime arrivedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "accepted_by_user_id", nullable = false) private User acceptedBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "started_by_user_id") private User startedBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "arrived_by_user_id") private User arrivedBy;
    @Version @Column(nullable = false) private Long version;
    protected TransportTrip() { }

    public static TransportTrip accepted(Handover handover, User driver) {
        TransportTrip trip = new TransportTrip();
        trip.tripCode = "TRIP-" + handover.getId();
        trip.originHub = handover.getFromHub();
        trip.destinationHub = handover.getToHub();
        trip.driver = driver;
        trip.handover = handover;
        trip.status = "ACCEPTED";
        trip.acceptedBy = driver;
        trip.acceptedAt = LocalDateTime.now();
        return trip;
    }
    public boolean isAssignedTo(Long userId) { return driver != null && driver.getId().equals(userId); }
    public void start(User actor) {
        if ("STARTED".equals(status)) return;
        if (!"ACCEPTED".equals(status)) throw new IllegalStateException("Trip is not ready to start");
        status = "STARTED"; startedBy = actor; startedAt = LocalDateTime.now();
    }
    public void arrive(User actor) {
        if ("ARRIVED".equals(status)) return;
        if (!"STARTED".equals(status)) throw new IllegalStateException("Trip is not in transit");
        status = "ARRIVED"; arrivedBy = actor; arrivedAt = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public String getTripCode() { return tripCode; }
    public Hub getOriginHub() { return originHub; }
    public Hub getDestinationHub() { return destinationHub; }
    public User getDriver() { return driver; }
    public Handover getHandover() { return handover; }
    public String getStatus() { return status; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getArrivedAt() { return arrivedAt; }
}
