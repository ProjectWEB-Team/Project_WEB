package vn.iotstar.dto.delivery;
public record DeliveryAssignmentCandidate(Long shipmentId, String trackingNumber, String direction) {
    public Long getShipmentId(){return shipmentId;}
    public String getTrackingNumber(){return trackingNumber;}
    public String getDirection(){return direction;}
}
