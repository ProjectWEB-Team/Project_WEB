package vn.iotstar.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PhoneLoginRequest {
    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Size(max = 30, message = "Số điện thoại không được vượt quá 30 ký tự.")
    private String phone;
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value; }
}
