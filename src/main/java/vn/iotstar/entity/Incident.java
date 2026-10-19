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

import java.time.LocalDateTime;

/** Immutable, attributable operational evidence; driver input is limited to TRIP_DELAY. */
@Entity
@Table(name = "incidents")
public class Incident {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "transport_trip_id", nullable = false) private TransportTrip trip;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "hub_id", nullable = false) private Hub hub;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "actor_user_id", nullable = false) private User actor;
    @Column(name = "incident_type", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)") private String incidentType;
    @Column(name = "note", nullable = false, length = 500, columnDefinition = "NVARCHAR(500)") private String note;
    @Column(name = "evidence_reference", length = 120, columnDefinition = "NVARCHAR(120)") private String evidenceReference;
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME2(0)") private LocalDateTime createdAt;
    protected Incident() { }
    public static Incident tripDelay(TransportTrip trip, User actor, String note, String evidenceReference) {
        Incident incident = new Incident();
        incident.trip = trip; incident.hub = trip.getOriginHub(); incident.actor = actor;
        incident.incidentType = "TRIP_DELAY"; incident.note = note; incident.evidenceReference = evidenceReference;
        incident.createdAt = LocalDateTime.now(); return incident;
    }
    /** Receipt-derived exception evidence; never available through a driver route. */
    public static Incident missingParcel(TransportTrip trip, User actor, String receiptReference) {
        Incident incident = new Incident();
        incident.trip = trip; incident.hub = trip.getDestinationHub(); incident.actor = actor;
        incident.incidentType = "MISSING_PARCEL"; incident.note = "Destination receipt discrepancy";
        incident.evidenceReference = receiptReference; incident.createdAt = LocalDateTime.now(); return incident;
    }
    public Long getId() { return id; }
    public TransportTrip getTrip() { return trip; }
    public String getIncidentType() { return incidentType; }
    public String getNote() { return note; }
}
