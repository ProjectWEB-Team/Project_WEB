package vn.iotstar.entity;

import jakarta.persistence.*;

/** Canonical fleet inventory. Reservation is serialized by the vehicle row lock. */
@Entity
@Table(name = "vehicles")
public class Vehicle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "plate_number", nullable = false, unique = true, length = 20) private String plateNumber;
    @Column(name = "vehicle_type", nullable = false, length = 50) private String vehicleType;
    private Integer capacity;
    @Column(nullable = false, length = 50) private String status;
    protected Vehicle() { }
    public static Vehicle available(String plate, String type, Integer capacity) {
        Vehicle vehicle = new Vehicle(); vehicle.plateNumber = plate; vehicle.vehicleType = type;
        vehicle.capacity = capacity; vehicle.status = "AVAILABLE"; return vehicle;
    }
    public void reserve() {
        if (!"AVAILABLE".equals(status)) throw new IllegalStateException("Vehicle is not available");
        status = "IN_USE";
    }
    public void release() {
        if (!"IN_USE".equals(status)) throw new IllegalStateException("Vehicle is not reserved");
        status = "AVAILABLE";
    }
    public Long getId() { return id; }
    public String getPlateNumber() { return plateNumber; }
    public String getVehicleType() { return vehicleType; }
    public Integer getCapacity() { return capacity; }
    public String getStatus() { return status; }
}
