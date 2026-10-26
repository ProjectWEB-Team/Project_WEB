package vn.iotstar.dto.shipment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Untrusted intake input; it carries no actor, fee, status or hub, which the server derives. */
public class CreateShipmentRequest {
    @NotBlank(message = "Vui lòng nhập tên người gửi.") @Size(max = 100)
    private String senderName;
    @NotBlank(message = "Vui lòng nhập số điện thoại người gửi.") @Size(max = 20)
    private String senderPhone;
    @NotBlank(message = "Vui lòng nhập địa chỉ người gửi.") @Size(max = 255)
    private String senderAddress;
    @NotBlank(message = "Vui lòng nhập tên người nhận.") @Size(max = 100)
    private String receiverName;
    @NotBlank(message = "Vui lòng nhập số điện thoại người nhận.") @Size(max = 20)
    private String receiverPhone;
    @NotBlank(message = "Vui lòng nhập địa chỉ người nhận.") @Size(max = 255)
    private String receiverAddress;
    @Size(max = 100)
    private String receiverProvince;
    @Size(max = 100)
    private String receiverDistrict;

    @NotBlank(message = "Chọn tỉnh/thành người gửi.") @Pattern(regexp = "[0-9]{2}")
    private String senderProvinceCode;
    @NotBlank(message = "Chọn phường/xã người gửi.") @Pattern(regexp = "[0-9]{5}")
    private String senderWardCode;
    @NotBlank(message = "Chọn tỉnh/thành người nhận.") @Pattern(regexp = "[0-9]{2}")
    private String receiverProvinceCode;
    @NotBlank(message = "Chọn phường/xã người nhận.") @Pattern(regexp = "[0-9]{5}")
    private String receiverWardCode;

    @NotBlank(message = "Vui lòng chọn dịch vụ.")
    @Pattern(regexp = "STANDARD|FAST|EXPRESS", message = "Dịch vụ không hợp lệ.")
    private String serviceCode = "STANDARD";
    @Pattern(regexp = "NORTH|CENTRAL|SOUTH", message = "Vui lòng chọn vùng gửi.")
    private String fromRegion;
    @Pattern(regexp = "NORTH|CENTRAL|SOUTH", message = "Vui lòng chọn vùng nhận.")
    private String toRegion;

    @NotNull(message = "Vui lòng nhập khối lượng (gram).") @Min(value = 1, message = "Khối lượng tối thiểu 1 g.") @Max(1_000_000)
    private Integer weightGrams;
    @NotNull(message = "Nhập chiều dài (cm).") @Min(1) @Max(1000)
    private Integer lengthCm;
    @NotNull(message = "Nhập chiều rộng (cm).") @Min(1) @Max(1000)
    private Integer widthCm;
    @NotNull(message = "Nhập chiều cao (cm).") @Min(1) @Max(1000)
    private Integer heightCm;
    @NotNull @DecimalMin(value = "0", message = "Giá trị khai báo không được âm.")
    @Digits(integer = 13, fraction = 2, message = "Giá trị khai báo tối đa 13 chữ số và 2 số thập phân.")
    private BigDecimal declaredValue = BigDecimal.ZERO;
    @NotNull @DecimalMin(value = "0", message = "Tiền thu hộ không được âm.")
    @Digits(integer = 13, fraction = 2, message = "Tiền thu hộ tối đa 13 chữ số và 2 số thập phân.")
    private BigDecimal codAmount = BigDecimal.ZERO;

    @NotBlank(message = "Vui lòng chọn người trả cước.")
    @Pattern(regexp = "SENDER|RECEIVER", message = "Người trả cước không hợp lệ.")
    private String feePayer = "SENDER";
    @NotBlank(message = "Vui lòng chọn phương thức thanh toán.")
    @Pattern(regexp = "VNPAY|CASH", message = "Phương thức thanh toán không hợp lệ.")
    private String paymentMethod = "VNPAY";
    @NotBlank(message = "Vui lòng chọn hình thức gửi hàng.")
    @Pattern(regexp = "HOME_PICKUP|SELF_DROPOFF", message = "Hình thức gửi hàng không hợp lệ.")
    private String pickupMethod = "SELF_DROPOFF";
    @NotBlank(message = "Vui lòng chọn chính sách kiểm hàng.")
    @Pattern(regexp = "INSPECTION_ONLY|TRIAL_ALLOWED|NO_INSPECTION", message = "Chính sách kiểm hàng không hợp lệ.")
    private String inspectionPolicy = "NO_INSPECTION";

    public String getSenderName() { return senderName; }
    public String getSenderProvinceCode() { return senderProvinceCode; }
    public void setSenderProvinceCode(String value) { senderProvinceCode = value; }
    public String getSenderWardCode() { return senderWardCode; }
    public void setSenderWardCode(String value) { senderWardCode = value; }
    public String getReceiverProvinceCode() { return receiverProvinceCode; }
    public void setReceiverProvinceCode(String value) { receiverProvinceCode = value; }
    public String getReceiverWardCode() { return receiverWardCode; }
    public void setReceiverWardCode(String value) { receiverWardCode = value; }
    public boolean usesModernAddress() { return senderProvinceCode != null || senderWardCode != null || receiverProvinceCode != null || receiverWardCode != null; }
    public void setSenderName(String v) { this.senderName = v; }
    public String getSenderPhone() { return senderPhone; }
    public void setSenderPhone(String v) { this.senderPhone = v; }
    public String getSenderAddress() { return senderAddress; }
    public void setSenderAddress(String v) { this.senderAddress = v; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String v) { this.receiverName = v; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String v) { this.receiverPhone = v; }
    public String getReceiverAddress() { return receiverAddress; }
    public void setReceiverAddress(String v) { this.receiverAddress = v; }
    public String getReceiverProvince() { return receiverProvince; }
    public void setReceiverProvince(String v) { this.receiverProvince = v; }
    public String getReceiverDistrict() { return receiverDistrict; }
    public void setReceiverDistrict(String v) { this.receiverDistrict = v; }
    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String v) { this.serviceCode = v; }
    public String getFromRegion() { return fromRegion; }
    public void setFromRegion(String v) { this.fromRegion = v; }
    public String getToRegion() { return toRegion; }
    public void setToRegion(String v) { this.toRegion = v; }
    public Integer getWeightGrams() { return weightGrams; }
    public void setWeightGrams(Integer v) { this.weightGrams = v; }
    public Integer getLengthCm() { return lengthCm; }
    public void setLengthCm(Integer v) { this.lengthCm = v; }
    public Integer getWidthCm() { return widthCm; }
    public void setWidthCm(Integer v) { this.widthCm = v; }
    public Integer getHeightCm() { return heightCm; }
    public void setHeightCm(Integer v) { this.heightCm = v; }
    public BigDecimal getDeclaredValue() { return declaredValue; }
    public void setDeclaredValue(BigDecimal v) { this.declaredValue = v; }
    public BigDecimal getCodAmount() { return codAmount; }
    public void setCodAmount(BigDecimal v) { this.codAmount = v; }
    public String getFeePayer() { return feePayer; }
    public void setFeePayer(String v) { this.feePayer = v; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String v) { this.paymentMethod = v; }
    public String getPickupMethod() { return pickupMethod; }
    public void setPickupMethod(String v) { this.pickupMethod = v; }
    public String getInspectionPolicy() { return inspectionPolicy; }
    public void setInspectionPolicy(String v) { this.inspectionPolicy = v; }

    /** Snapshot after validation; pickup is chosen last and does not change the quote. */
    public List<Object> quoteInputs() {
        return List.of(senderName, senderPhone, senderAddress, receiverName, receiverPhone, receiverAddress,
                java.util.Objects.toString(receiverProvince, ""), java.util.Objects.toString(receiverDistrict, ""), serviceCode, fromRegion, toRegion,
                weightGrams, lengthCm, widthCm, heightCm, declaredValue.stripTrailingZeros(),
                codAmount.stripTrailingZeros(), feePayer, paymentMethod, inspectionPolicy,
                java.util.Objects.toString(senderProvinceCode, ""), java.util.Objects.toString(senderWardCode, ""),
                java.util.Objects.toString(receiverProvinceCode, ""), java.util.Objects.toString(receiverWardCode, ""));
    }
}
