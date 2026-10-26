package vn.iotstar.dto.firstmile;

public record ShipperOptionResponse(Long userId, String fullName) {
    public Long getUserId() { return userId; }
    public String getFullName() { return fullName; }
}
