package vn.iotstar.service;

import vn.iotstar.dto.auth.*;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationService {
    RegistrationChallengeResponse register(RegisterUserRequest request);
    boolean verify(VerifyPhoneOtpRequest request);
    RegistrationChallengeResponse resend(UUID challengeId);
    RegistrationChallengeResponse requestLogin(PhoneLoginRequest request);
    Optional<Long> verifyLogin(VerifyPhoneOtpRequest request);
    /** Same anti-enumeration contract as requestLogin: unknown phones get a decoy challenge. */
    RegistrationChallengeResponse requestPasswordReset(PhoneLoginRequest request);
    boolean resetPassword(VerifyPhoneOtpRequest request, String newPassword);
}
