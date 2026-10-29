package vn.iotstar.service.auth;

import vn.iotstar.exception.RegistrationException;

public final class VietnamPhoneNormalizer {
    private VietnamPhoneNormalizer() { }
    public static String normalize(String phone) {
        if (phone == null || phone.length() > 30 || !phone.matches("[+0-9 ()-]+")) throw invalid();
        String value = phone.replaceAll("[ ()-]", "");
        if (value.startsWith("+84")) value = "0" + value.substring(3);
        else if (value.startsWith("84")) value = "0" + value.substring(2);
        if (!value.matches("0[35789][0-9]{8}")) throw invalid();
        return "+84" + value.substring(1);
    }
    private static RegistrationException invalid() {
        return new RegistrationException("Số điện thoại di động Việt Nam chưa hợp lệ.", "phone");
    }
}
