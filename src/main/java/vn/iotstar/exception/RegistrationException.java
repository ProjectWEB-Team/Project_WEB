package vn.iotstar.exception;

public class RegistrationException extends RuntimeException {
    private final String field;

    public RegistrationException(String safeMessage) { this(safeMessage, null); }
    /** @param field the form field the message belongs under, or null for a general message */
    public RegistrationException(String safeMessage, String field) { super(safeMessage); this.field = field; }
    public String getField() { return field; }

    public static RegistrationException usernameTaken() { return new RegistrationException("Tên đăng nhập này đã được sử dụng. Vui lòng chọn tên khác.", "username"); }
    public static RegistrationException emailTaken() { return new RegistrationException("Email này đã được đăng ký. Hãy đăng nhập hoặc dùng email khác.", "email"); }
    public static RegistrationException phoneTaken() { return new RegistrationException("Số điện thoại này đã được đăng ký. Hãy đăng nhập hoặc dùng số khác.", "phone"); }
    public static RegistrationException weakPassword() { return new RegistrationException("Mật khẩu cần ít nhất 8 ký tự và tối đa 72 byte.", "password"); }
}
