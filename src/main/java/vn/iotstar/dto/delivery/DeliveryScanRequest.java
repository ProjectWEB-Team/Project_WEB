package vn.iotstar.dto.delivery;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

public class DeliveryScanRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Shipment ID is required")
    private Long shipmentId;

    @NotNull(message = "Hub ID is required")
    private Long hubId;

    public DeliveryScanRequest() {
    }

    public DeliveryScanRequest(Long shipmentId, Long hubId) {
        this.shipmentId = shipmentId;
        this.hubId = hubId;
    }

    public Long getShipmentId() { return shipmentId; }
    public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }
    public Long getHubId() { return hubId; }
    public void setHubId(Long hubId) { this.hubId = hubId; }
}

