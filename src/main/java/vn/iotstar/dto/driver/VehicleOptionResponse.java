package vn.iotstar.dto.driver;

public record VehicleOptionResponse(Long id, String plateNumber, String vehicleType) {
    // JavaBean accessors for the configured Jasper/EL 5 runtime.
    public Long getId() { return id; }
    public String getPlateNumber() { return plateNumber; }
    public String getVehicleType() { return vehicleType; }
}
