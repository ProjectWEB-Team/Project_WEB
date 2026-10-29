package vn.iotstar.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.entity.User;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.UserProfileService;

import java.util.UUID;

@Controller
@PreAuthorize("isAuthenticated()")
public class UserProfileController {
    static final String PHONE_CHALLENGE_ID = "profilePhoneChallengeId";
    static final String PHONE_TARGET = "profilePhoneTarget";
    static final String EMAIL_CHALLENGE_ID = "profileEmailChallengeId";
    static final String EMAIL_TARGET = "profileEmailTarget";

    private final UserProfileService profileService;

    public UserProfileController(UserProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/user/profile")
    public String viewProfile(HttpSession session, Model model) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        User user = profileService.getUser(actor.userId());
        model.addAttribute("profileUser", user);
        model.addAttribute("pageTitle", "Hồ sơ cá nhân | UTEExpress");

        if (session != null) {
            model.addAttribute("phoneChallengeId", session.getAttribute(PHONE_CHALLENGE_ID));
            model.addAttribute("phoneTarget", session.getAttribute(PHONE_TARGET));
            model.addAttribute("emailChallengeId", session.getAttribute(EMAIL_CHALLENGE_ID));
            model.addAttribute("emailTarget", session.getAttribute(EMAIL_TARGET));
        }

        return "user/profile";
    }

    @PostMapping("/user/profile/update-info")
    public String updateInfo(@RequestParam(defaultValue = "") String fullName,
                             HttpServletRequest request,
                             RedirectAttributes attributes) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        try {
            profileService.updateFullName(actor.userId(), fullName);
            User updatedUser = profileService.getUser(actor.userId());
            refreshAuthentication(updatedUser);
            attributes.addFlashAttribute("successMessage", "Cập nhật họ và tên thành công!");
        } catch (RegistrationException ex) {
            attributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/user/profile";
    }

    @PostMapping("/user/profile/change-password")
    public String changePassword(@RequestParam(defaultValue = "") String currentPassword,
                                 @RequestParam(defaultValue = "") String newPassword,
                                 @RequestParam(defaultValue = "") String confirmPassword,
                                 HttpServletRequest request,
                                 RedirectAttributes attributes) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        if (!newPassword.equals(confirmPassword)) {
            attributes.addFlashAttribute("passwordError", "Mật khẩu xác nhận không khớp với mật khẩu mới.");
            return "redirect:/user/profile#security-section";
        }
        try {
            profileService.changePassword(actor.userId(), currentPassword, newPassword);
            User updatedUser = profileService.getUser(actor.userId());
            refreshAuthentication(updatedUser);
            attributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        } catch (RegistrationException ex) {
            attributes.addFlashAttribute("passwordError", ex.getMessage());
        }
        return "redirect:/user/profile#security-section";
    }

    @PostMapping("/user/profile/phone/send-otp")
    public String sendPhoneOtp(@RequestParam(defaultValue = "") String phone,
                               HttpSession session,
                               RedirectAttributes attributes) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        try {
            UUID challengeId = profileService.requestPhoneVerification(actor.userId(), phone);
            session.setAttribute(PHONE_CHALLENGE_ID, challengeId);
            session.setAttribute(PHONE_TARGET, phone.isBlank() ? profileService.getUser(actor.userId()).getPhone() : phone);
            attributes.addFlashAttribute("successMessage",
                    "Mã OTP đã được gửi đến số điện thoại của bạn.");
        } catch (RegistrationException ex) {
            attributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/user/profile#phone-section";
    }

    @PostMapping("/user/profile/phone/verify-otp")
    public String verifyPhoneOtp(@RequestParam(defaultValue = "") String code,
                                 HttpSession session,
                                 RedirectAttributes attributes) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        UUID challengeId = session != null ? (UUID) session.getAttribute(PHONE_CHALLENGE_ID) : null;
        if (challengeId == null) {
            attributes.addFlashAttribute("errorMessage", "Phiên xác thực số điện thoại đã hết hạn hoặc chưa được tạo.");
            return "redirect:/user/profile#phone-section";
        }

        try {
            boolean success = profileService.verifyPhoneOtp(actor.userId(), challengeId, code);
            if (success) {
                session.removeAttribute(PHONE_CHALLENGE_ID);
                session.removeAttribute(PHONE_TARGET);
                User updatedUser = profileService.getUser(actor.userId());
                refreshAuthentication(updatedUser);
                attributes.addFlashAttribute("successMessage", "Xác thực và cập nhật số điện thoại thành công!");
            } else {
                attributes.addFlashAttribute("errorMessage", "Mã OTP không chính xác hoặc đã hết hạn.");
            }
        } catch (RegistrationException ex) {
            attributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/user/profile#phone-section";
    }

    @PostMapping("/user/profile/phone/resend-otp")
    public String resendPhoneOtp(HttpSession session, RedirectAttributes attributes) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        UUID challengeId = session != null ? (UUID) session.getAttribute(PHONE_CHALLENGE_ID) : null;
        if (challengeId == null) {
            attributes.addFlashAttribute("errorMessage", "Phiên xác thực số điện thoại đã hết hạn.");
            return "redirect:/user/profile#phone-section";
        }

        try {
            profileService.resendPhoneOtp(actor.userId(), challengeId);
            attributes.addFlashAttribute("successMessage", "Đã gửi lại mã OTP. Vui lòng kiểm tra SMS (hoặc thư mục .local/otp-sms).");
        } catch (RegistrationException ex) {
            attributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/user/profile#phone-section";
    }

    @PostMapping("/user/profile/email/send-otp")
    public String sendEmailOtp(@RequestParam(defaultValue = "") String email,
                               HttpSession session,
                               RedirectAttributes attributes) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        try {
            UUID challengeId = profileService.requestEmailVerification(actor.userId(), email);
            session.setAttribute(EMAIL_CHALLENGE_ID, challengeId);
            session.setAttribute(EMAIL_TARGET, email.isBlank() ? profileService.getUser(actor.userId()).getEmail() : email);
            attributes.addFlashAttribute("successMessage",
                    "Mã OTP đã được gửi đến email. Vui lòng kiểm tra hộp thư đến (và thư mục Spam/Rác).");
        } catch (RegistrationException ex) {
            attributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/user/profile#email-section";
    }

    @PostMapping("/user/profile/email/verify-otp")
    public String verifyEmailOtp(@RequestParam(defaultValue = "") String code,
                                 HttpSession session,
                                 RedirectAttributes attributes) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        UUID challengeId = session != null ? (UUID) session.getAttribute(EMAIL_CHALLENGE_ID) : null;
        if (challengeId == null) {
            attributes.addFlashAttribute("errorMessage", "Phiên xác thực email đã hết hạn hoặc chưa được tạo.");
            return "redirect:/user/profile#email-section";
        }

        try {
            boolean success = profileService.verifyEmailOtp(actor.userId(), challengeId, code);
            if (success) {
                session.removeAttribute(EMAIL_CHALLENGE_ID);
                session.removeAttribute(EMAIL_TARGET);
                User updatedUser = profileService.getUser(actor.userId());
                refreshAuthentication(updatedUser);
                attributes.addFlashAttribute("successMessage", "Xác thực và cập nhật email thành công!");
            } else {
                attributes.addFlashAttribute("errorMessage", "Mã OTP không chính xác hoặc đã hết hạn.");
            }
        } catch (RegistrationException ex) {
            attributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/user/profile#email-section";
    }

    @PostMapping("/user/profile/email/resend-otp")
    public String resendEmailOtp(HttpSession session, RedirectAttributes attributes) {
        AuthenticatedActor actor = AuthenticatedActor.requireCurrent();
        UUID challengeId = session != null ? (UUID) session.getAttribute(EMAIL_CHALLENGE_ID) : null;
        if (challengeId == null) {
            attributes.addFlashAttribute("errorMessage", "Phiên xác thực email đã hết hạn.");
            return "redirect:/user/profile#email-section";
        }

        try {
            profileService.resendEmailOtp(actor.userId(), challengeId);
            attributes.addFlashAttribute("successMessage", "Đã gửi lại mã OTP tới email. Vui lòng kiểm tra hộp thư.");
        } catch (RegistrationException ex) {
            attributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/user/profile#email-section";
    }

    private void refreshAuthentication(User updatedUser) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            var newAuth = new UsernamePasswordAuthenticationToken(
                    updatedUser, auth.getCredentials(), updatedUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        }
    }
}
