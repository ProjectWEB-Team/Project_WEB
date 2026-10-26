package vn.iotstar.dto.returnflow;
import java.math.BigDecimal;
public record ShipmentFeeResponse(Long shipmentId, String trackingNumber, BigDecimal forwardDue,
        BigDecimal returnDue, BigDecimal surchargeDue) {
    public Long getShipmentId(){return shipmentId;}
    public String getTrackingNumber(){return trackingNumber;}
    public BigDecimal getForwardDue(){return forwardDue;}
    public BigDecimal getReturnDue(){return returnDue;}
    public BigDecimal getSurchargeDue(){return surchargeDue;}
}
