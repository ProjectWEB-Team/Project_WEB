package vn.iotstar.service;

import vn.iotstar.entity.User;
import java.util.UUID;

public interface UserProfileService {
    User getUser(Long userId);
    void updateFullName(Long userId, String fullName);
    void changePassword(Long userId, String currentPassword, String newPassword);

    UUID requestPhoneVerification(Long userId, String targetPhone);
    boolean verifyPhoneOtp(Long userId, UUID challengeId, String code);
    UUID resendPhoneOtp(Long userId, UUID challengeId);

    UUID requestEmailVerification(Long userId, String targetEmail);
    boolean verifyEmailOtp(Long userId, UUID challengeId, String code);
    UUID resendEmailOtp(Long userId, UUID challengeId);
}

