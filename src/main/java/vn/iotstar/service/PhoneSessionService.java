package vn.iotstar.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
public interface PhoneSessionService {
    /** Called only with the user ID returned by successful one-time proof consumption. */
    void signIn(Long verifiedUserId, HttpServletRequest request, HttpServletResponse response);
}
