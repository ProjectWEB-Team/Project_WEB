package vn.iotstar.dto.shipment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Untrusted Phase-01 snapshot input; it intentionally carries no actor or status fields. */
public class CreateShipmentRequest {
    @NotBlank(message = "Sender name is required")
    @Size(max = 100)
    private String senderName;
    @NotBlank(message = "Sender phone is required")
    @Size(max = 20)
    private String senderPhone;
    @NotBlank(message = "Sender address is required")
    @Size(max = 255)
    private String senderAddress;
    @NotBlank(message = "Receiver name is required")
    @Size(max = 100)
    private String receiverName;
    @NotBlank(message = "Receiver phone is required")
    @Size(max = 20)
    private String receiverPhone;
    @NotBlank(message = "Receiver address is required")
    @Size(max = 255)
    private String receiverAddress;

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSenderPhone() { return senderPhone; }
    public void setSenderPhone(String senderPhone) { this.senderPhone = senderPhone; }
    public String getSenderAddress() { return senderAddress; }
    public void setSenderAddress(String senderAddress) { this.senderAddress = senderAddress; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getReceiverAddress() { return receiverAddress; }
    public void setReceiverAddress(String receiverAddress) { this.receiverAddress = receiverAddress; }
}
