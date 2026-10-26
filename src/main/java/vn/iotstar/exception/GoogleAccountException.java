package vn.iotstar.exception;

/** Safe public outcome, without disclosing an existing account's identity. */
public class GoogleAccountException extends RuntimeException {
    public GoogleAccountException() {
        super("Không thể hoàn tất bằng thông tin này. Vui lòng đăng nhập bằng phương thức đã dùng hoặc kiểm tra lại thông tin.");
    }
}
