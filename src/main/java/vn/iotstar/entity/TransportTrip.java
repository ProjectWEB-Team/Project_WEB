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

/** Persistence placeholder for the later assigned-driver lifecycle; no trip actions are exposed in 03-02. */
@Entity
@Table(name = "transport_trips")
public class TransportTrip {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "trip_code", nullable = false, unique = true, length = 80, columnDefinition = "NVARCHAR(80)") private String tripCode;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "origin_hub_id", nullable = false) private Hub originHub;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "destination_hub_id", nullable = false) private Hub destinationHub;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "driver_user_id") private User driver;
    @Column(nullable = false, length = 30, columnDefinition = "NVARCHAR(30)") private String status;
    protected TransportTrip() { }
}
