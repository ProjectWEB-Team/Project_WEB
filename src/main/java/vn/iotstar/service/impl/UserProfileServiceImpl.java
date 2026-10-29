package vn.iotstar.service.impl;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.config.OtpProperties;
import vn.iotstar.entity.EmailOtpChallenge;
import vn.iotstar.entity.PhoneOtpChallenge;
import vn.iotstar.entity.User;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.repository.EmailOtpChallengeRepository;
import vn.iotstar.repository.PhoneOtpChallengeRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.ReceiverLinkService;
import vn.iotstar.service.UserProfileService;
import vn.iotstar.service.auth.AccountEmailSender;
import vn.iotstar.service.auth.OtpDelivery;
import vn.iotstar.service.auth.VietnamPhoneNormalizer;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class UserProfileServiceImpl implements UserProfileService {
    private final UserRepository users;
    private final PhoneOtpChallengeRepository phoneChallenges;
    private final EmailOtpChallengeRepository emailChallenges;
    private final PasswordEncoder encoder;
    private final OtpDelivery delivery;
    private final ObjectProvider<AccountEmailSender> mailSenderProvider;
    private final Clock clock;
    private final OtpProperties policy;
    private final ReceiverLinkService receiverLinks;
    private final SecureRandom random = new SecureRandom();

    public UserProfileServiceImpl(UserRepository users,
                                  PhoneOtpChallengeRepository phoneChallenges,
                                  EmailOtpChallengeRepository emailChallenges,
                                  PasswordEncoder encoder,
                                  OtpDelivery delivery,
                                  ObjectProvider<AccountEmailSender> mailSenderProvider,
                                  Clock clock,
                                  OtpProperties policy,
                                  ReceiverLinkService receiverLinks) {
        this.users = users;
        this.phoneChallenges = phoneChallenges;
        this.emailChallenges = emailChallenges;
        this.encoder = encoder;
        this.delivery = delivery;
        this.mailSenderProvider = mailSenderProvider;
        this.clock = clock;
        this.policy = policy;
        this.receiverLinks = receiverLinks;
        policy.validate();
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return users.findById(userId).orElseThrow(() -> new RegistrationException("Không tìm thấy thông tin tài khoản."));
    }

    @Override
    public void updateFullName(Long userId, String fullName) {
        if (fullName == null || fullName.trim().length() < 2 || fullName.trim().length() > 100) {
            throw new RegistrationException("Họ và tên phải từ 2 đến 100 ký tự.");
        }
        User user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new RegistrationException("Không tìm thấy thông tin tài khoản."));
        user.updateFullName(fullName.trim());
        users.save(user);
    }

    @Override
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new RegistrationException("Không tìm thấy thông tin tài khoản."));
        if (!encoder.matches(currentPassword, user.getPassword())) {
            throw new RegistrationException("Mật khẩu hiện tại không chính xác.");
        }
        if (newPassword == null || newPassword.length() < 8 || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RegistrationException("Mật khẩu mới phải từ 8 ký tự và tối đa 72 byte.");
        }
        user.changePassword(encoder.encode(newPassword));
        users.save(user);
    }

    @Override
    public UUID requestPhoneVerification(Long userId, String targetPhone) {
        User user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new RegistrationException("Không tìm thấy tài khoản."));
        if (!user.isAccountNonLocked()) {
            throw new RegistrationException("Tài khoản hiện đang bị khóa.");
        }

        String phoneToSend;
        if (targetPhone != null && !targetPhone.isBlank()) {
            phoneToSend = VietnamPhoneNormalizer.normalize(targetPhone);
            if (phoneToSend.equals(user.getPhone())) {
                throw new RegistrationException("Số điện thoại mới trùng với số điện thoại hiện tại.");
            }
            if (users.existsByPhone(phoneToSend)) {
                throw new RegistrationException("Số điện thoại này đã được sử dụng bởi tài khoản khác.");
            }
        } else {
            phoneToSend = user.getPhone();
            if (user.isPhoneVerified()) {
                throw new RegistrationException("Số điện thoại hiện tại đã được xác thực.");
            }
        }

        Instant now = clock.instant();
        String code = generateCode();
        PhoneOtpChallenge challenge = phoneChallenges.findByUserIdForUpdate(userId).orElse(null);
        if (challenge == null) {
            challenge = PhoneOtpChallenge.create(user, "VERIFY_PHONE", phoneToSend, encoder.encode(code), now, policy);
            phoneChallenges.saveAndFlush(challenge);
        } else {
            challenge.issue("VERIFY_PHONE", phoneToSend, encoder.encode(code), now, policy);
            phoneChallenges.flush();
        }

        delivery.send(challenge.getId(), phoneToSend, code, challenge.getExpiresAt());
        return challenge.getId();
    }

    @Override
    public boolean verifyPhoneOtp(Long userId, UUID challengeId, String code) {
        if (challengeId == null || code == null || !code.matches("[0-9]{6}")) {
            return false;
        }
        PhoneOtpChallenge challenge = phoneChallenges.findByIdForUpdate(challengeId).orElse(null);
        Instant now = clock.instant();
        if (challenge == null || !challenge.getUser().getId().equals(userId)
                || !challenge.usable("VERIFY_PHONE", now, policy.getMaxAttempts())) {
            return false;
        }

        if (!encoder.matches(code, challenge.getCodeHash())) {
            challenge.reject();
            return false;
        }

        challenge.consume(now);
        User user = challenge.getUser();
        String newPhone = challenge.getTargetPhone();
        if (newPhone != null && !newPhone.isBlank() && !newPhone.equals(user.getPhone())) {
            user.updatePhone(newPhone, now);
        } else {
            user.verifyPhone(now);
        }
        receiverLinks.linkVerifiedReceiver(user);
        phoneChallenges.delete(challenge);
        users.save(user);
        return true;
    }

    @Override
    public UUID resendPhoneOtp(Long userId, UUID challengeId) {
        PhoneOtpChallenge challenge = phoneChallenges.findByIdForUpdate(challengeId)
                .orElseThrow(() -> new RegistrationException("Phiên xác thực không tồn tại."));
        if (!challenge.getUser().getId().equals(userId) || !"VERIFY_PHONE".equals(challenge.getPurpose())) {
            throw new RegistrationException("Yêu cầu không hợp lệ.");
        }
        Instant now = clock.instant();
        String code = generateCode();
        String targetPhone = challenge.getTargetPhone();
        challenge.issue("VERIFY_PHONE", targetPhone, encoder.encode(code), now, policy);
        phoneChallenges.flush();
        delivery.send(challenge.getId(), targetPhone, code, challenge.getExpiresAt());
        return challenge.getId();
    }

    @Override
    public UUID requestEmailVerification(Long userId, String targetEmail) {
        AccountEmailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new RegistrationException("Dịch vụ gửi Email OTP chưa được kích hoạt trên hệ thống.");
        }

        User user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new RegistrationException("Không tìm thấy tài khoản."));
        if (!user.isAccountNonLocked()) {
            throw new RegistrationException("Tài khoản hiện đang bị khóa.");
        }

        String emailToSend;
        if (targetEmail != null && !targetEmail.isBlank()) {
            emailToSend = targetEmail.trim().toLowerCase(Locale.ROOT);
            if (!emailToSend.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || emailToSend.length() > 100) {
                throw new RegistrationException("Địa chỉ email không đúng định dạng.");
            }
            if (emailToSend.equalsIgnoreCase(user.getEmail())) {
                throw new RegistrationException("Email mới trùng với email hiện tại.");
            }
            if (users.existsByEmail(emailToSend)) {
                throw new RegistrationException("Email này đã được sử dụng bởi tài khoản khác.");
            }
        } else {
            emailToSend = user.getEmail();
            if (user.isEmailVerified()) {
                throw new RegistrationException("Email hiện tại đã được xác thực.");
            }
        }

        Instant now = clock.instant();
        String code = generateCode();
        EmailOtpChallenge challenge = emailChallenges.findByUserIdForUpdate(userId).orElse(null);
        if (challenge == null) {
            challenge = EmailOtpChallenge.create(user, "VERIFY_EMAIL", emailToSend, encoder.encode(code), now, policy);
            emailChallenges.saveAndFlush(challenge);
        } else {
            challenge.issue("VERIFY_EMAIL", emailToSend, encoder.encode(code), now, policy);
            emailChallenges.flush();
        }

        mailSender.sendOtp(emailToSend, code, challenge.getExpiresAt());
        return challenge.getId();
    }

    @Override
    public boolean verifyEmailOtp(Long userId, UUID challengeId, String code) {
        if (challengeId == null || code == null || !code.matches("[0-9]{6}")) {
            return false;
        }
        EmailOtpChallenge challenge = emailChallenges.findByIdForUpdate(challengeId).orElse(null);
        Instant now = clock.instant();
        if (challenge == null || !challenge.getUser().getId().equals(userId)
                || !challenge.usable("VERIFY_EMAIL", now, policy.getMaxAttempts())) {
            return false;
        }

        if (!encoder.matches(code, challenge.getCodeHash())) {
            challenge.reject();
            return false;
        }

        challenge.consume(now);
        User user = challenge.getUser();
        String targetEmail = challenge.getEmail();
        if (targetEmail != null && !targetEmail.isBlank() && !targetEmail.equalsIgnoreCase(user.getEmail())) {
            user.updateEmail(targetEmail, now);
        } else {
            user.verifyEmail(now);
        }
        emailChallenges.delete(challenge);
        users.save(user);
        return true;
    }

    @Override
    public UUID resendEmailOtp(Long userId, UUID challengeId) {
        AccountEmailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new RegistrationException("Dịch vụ gửi Email OTP chưa được kích hoạt trên hệ thống.");
        }
        EmailOtpChallenge challenge = emailChallenges.findByIdForUpdate(challengeId)
                .orElseThrow(() -> new RegistrationException("Phiên xác thực không tồn tại."));
        if (!challenge.getUser().getId().equals(userId) || !"VERIFY_EMAIL".equals(challenge.getPurpose())) {
            throw new RegistrationException("Yêu cầu không hợp lệ.");
        }
        Instant now = clock.instant();
        String code = generateCode();
        String targetEmail = challenge.getEmail();
        challenge.issue("VERIFY_EMAIL", targetEmail, encoder.encode(code), now, policy);
        emailChallenges.flush();
        mailSender.sendOtp(targetEmail, code, challenge.getExpiresAt());
        return challenge.getId();
    }

    private String generateCode() {
        return String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
    }
}

