package vn.iotstar.dto.returnflow;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

public class ReturnHandoverRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Shipment ID is required")
    private Long shipmentId;

    private String handoverType; // SELF_PICKUP, RETURN_DELIVERY

    private String identityProof;

    private BigDecimal settledReturnFee;

    private String paymentMethod; // CASH, VNPAY, E_WALLET

    private String note;

    public ReturnHandoverRequest() {
    }

    public ReturnHandoverRequest(Long shipmentId, String handoverType, String identityProof, BigDecimal settledReturnFee, String note) {
        this.shipmentId = shipmentId;
        this.handoverType = handoverType;
        this.identityProof = identityProof;
        this.settledReturnFee = settledReturnFee;
        this.note = note;
    }

    public Long getShipmentId() { return shipmentId; }
    public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }
    public String getHandoverType() { return handoverType; }
    public void setHandoverType(String handoverType) { this.handoverType = handoverType; }
    public String getIdentityProof() { return identityProof; }
    public void setIdentityProof(String identityProof) { this.identityProof = identityProof; }
    public BigDecimal getSettledReturnFee() { return settledReturnFee; }
    public void setSettledReturnFee(BigDecimal settledReturnFee) { this.settledReturnFee = settledReturnFee; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}

