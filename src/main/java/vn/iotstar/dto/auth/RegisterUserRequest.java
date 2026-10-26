package vn.iotstar.dto.auth;

import jakarta.validation.constraints.*;

public class RegisterUserRequest {
    @NotBlank(message = "Vui lòng nhập tên đăng nhập.")
    @Pattern(regexp = "[A-Za-z0-9_.-]{3,50}", message = "Tên đăng nhập gồm 3–50 chữ cái, chữ số hoặc dấu _ . -.")
    private String username;
    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    @Size(min = 8, max = 72, message = "Mật khẩu gồm 8–72 ký tự; tối đa 72 byte UTF-8.")
    private String password;
    @NotBlank(message = "Vui lòng nhập họ tên.")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự.")
    private String fullName;
    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Size(max = 30, message = "Số điện thoại không hợp lệ.")
    private String phone;
    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Email không hợp lệ.")
    @Size(max = 100, message = "Email tối đa 100 ký tự.")
    private String email;
    public String getUsername() { return username; }
    public void setUsername(String v) { username = v; }
    public String getPassword() { return password; }
    public void setPassword(String v) { password = v; }
    public String getFullName() { return fullName; }
    public void setFullName(String v) { fullName = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
}
