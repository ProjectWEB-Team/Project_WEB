package vn.iotstar.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.dto.auth.PhoneLoginRequest;
import vn.iotstar.dto.auth.RegisterUserRequest;
import vn.iotstar.dto.auth.RegistrationChallengeResponse;
import vn.iotstar.dto.auth.VerifyPhoneOtpRequest;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.service.PhoneSessionService;
import vn.iotstar.service.RegistrationService;
import vn.iotstar.service.auth.VietnamPhoneNormalizer;

import java.util.Optional;
import java.util.UUID;

/** Session-bound HTTP adapter; verification identity always comes from the server session. */
@Controller
public class RegistrationController {
    static final String CHALLENGE_ID = "phoneVerificationChallengeId";
    static final String CHALLENGE_PHONE = "phoneVerificationMaskedPhone";
    static final String CHALLENGE_PURPOSE = "phoneVerificationPurpose";
    private static final String REGISTRATION = "REGISTRATION";
    private static final String LOGIN = "LOGIN";
    static final String DUPLICATE_ERROR = "Số điện thoại, email hoặc tên đăng nhập đã được sử dụng. Nếu bạn đã có tài khoản, hãy đăng nhập.";
    static final String CLOSED_NOTICE = "Đăng ký và đăng nhập bằng mã điện thoại đang tạm đóng. Vui lòng dùng tài khoản đã có.";
    private static final String VERIFY_ERROR = "Mã không hợp lệ hoặc đã hết hạn. Vui lòng kiểm tra hoặc yêu cầu mã mới.";

    private final RegistrationService registrationService;
    private final PhoneSessionService phoneSessionService;
    private final boolean otpAvailable;

    public RegistrationController(RegistrationService registrationService, PhoneSessionService phoneSessionService,
                                  @Value("${uteexpress.otp.delivery-mode:disabled}") String otpDeliveryMode) {
        this.registrationService = registrationService;
        this.phoneSessionService = phoneSessionService;
        this.otpAvailable = !"disabled".equals(otpDeliveryMode);
    }

    @ModelAttribute("otpClosedNotice")
    String otpClosedNotice() {
        return otpAvailable ? null : CLOSED_NOTICE;
    }

    @InitBinder("registerUserRequest")
    void registrationFields(WebDataBinder binder) {
        binder.setAllowedFields("username", "password", "fullName", "phone", "email");
    }

    @InitBinder("phoneLoginRequest")
    void phoneLoginFields(WebDataBinder binder) {
        binder.setAllowedFields("phone");
    }

    @InitBinder("otpForm")
    void otpFields(WebDataBinder binder) {
        binder.setAllowedFields("code");
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerUserRequest", new RegisterUserRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerUserRequest") RegisterUserRequest form,
                           BindingResult errors, HttpSession session, Model model) {
        if (!errors.hasFieldErrors("phone")) {
            try {
                VietnamPhoneNormalizer.normalize(form.getPhone());
            } catch (RegistrationException exception) {
                errors.rejectValue("phone", "phone.invalid",
                        "Nhập số di động Việt Nam 10 chữ số bắt đầu bằng 03, 05, 07, 08 hoặc 09; hoặc dùng dạng +84 tương ứng.");
            }
        }
        if (errors.hasErrors()) {
            form.setPassword("");
            return "auth/register";
        }
        try {
            rememberChallenge(session, registrationService.register(form), REGISTRATION);
            return "redirect:/auth/verify-phone";
        } catch (RegistrationException exception) {
            // Service messages are written to be safe to show (no secrets, no codes).
            model.addAttribute("error", exception.getMessage());
            return "auth/register";
        } catch (DataIntegrityViolationException exception) {
            model.addAttribute("error", DUPLICATE_ERROR);
            return "auth/register";
        } finally {
            form.setPassword("");
        }
    }

    @GetMapping("/auth/phone-login")
    public String phoneLoginForm(Model model) {
        model.addAttribute("phoneLoginRequest", new PhoneLoginRequest());
        return "auth/phone-login";
    }

    @PostMapping("/auth/phone-login")
    public String phoneLogin(@Valid @ModelAttribute("phoneLoginRequest") PhoneLoginRequest form,
                             BindingResult errors, HttpSession session, Model model) {
        if (errors.hasErrors()) {
            return "auth/phone-login";
        }
        try {
            rememberChallenge(session, registrationService.requestLogin(form), LOGIN);
            return "redirect:/auth/verify-phone";
        } catch (RegistrationException exception) {
            model.addAttribute("error", exception.getMessage());
            return "auth/phone-login";
        }
    }

    @GetMapping("/auth/verify-phone")
    public String verificationForm(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (challengeId(session) == null) {
            return "redirect:/login";
        }
        populateVerificationModel(session, model);
        model.addAttribute("otpForm", new PhoneOtpForm());
        return "auth/verify-phone";
    }

    @PostMapping("/auth/verify-phone")
    public String verify(@Valid @ModelAttribute("otpForm") PhoneOtpForm form, BindingResult errors,
                         HttpServletRequest request, HttpServletResponse response, Model model) {
        HttpSession session = request.getSession(false);
        UUID challengeId = challengeId(session);
        if (challengeId == null) {
            form.setCode("");
            return "redirect:/login";
        }
        populateVerificationModel(session, model);
        if (errors.hasErrors()) {
            form.setCode("");
            return "auth/verify-phone";
        }
        VerifyPhoneOtpRequest verification = new VerifyPhoneOtpRequest();
        verification.setChallengeId(challengeId);
        verification.setCode(form.getCode());
        form.setCode("");
        try {
            if (LOGIN.equals(session.getAttribute(CHALLENGE_PURPOSE))) {
                Optional<Long> userId = registrationService.verifyLogin(verification);
                if (userId.isPresent()) {
                    clearChallenge(session);
                    phoneSessionService.signIn(userId.get(), request, response);
                    return "redirect:/user/shipments/create";
                }
            } else if (REGISTRATION.equals(session.getAttribute(CHALLENGE_PURPOSE))
                    && registrationService.verify(verification)) {
                clearChallenge(session);
                return "redirect:/login?registered=true";
            }
        } catch (AuthenticationException exception) {
            clearChallenge(session);
            return "redirect:/login?phoneError=true";
        } catch (RegistrationException exception) {
            // The public response is deliberately identical for all invalid proofs.
        }
        model.addAttribute("error", VERIFY_ERROR);
        return "auth/verify-phone";
    }

    @PostMapping("/auth/verify-phone/resend")
    public String resend(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        UUID challengeId = challengeId(session);
        if (challengeId == null) {
            return "redirect:/login";
        }
        try {
            String purpose = String.valueOf(session.getAttribute(CHALLENGE_PURPOSE));
            rememberChallenge(session, registrationService.resend(challengeId), purpose);
            redirectAttributes.addFlashAttribute("notice", "Mã mới đã được gửi. Chỉ mã mới nhất có hiệu lực.");
        } catch (RegistrationException exception) {
            redirectAttributes.addFlashAttribute("error", "Chưa thể gửi lại mã. Vui lòng chờ và thử lại.");
        }
        return "redirect:/auth/verify-phone";
    }

    private void rememberChallenge(HttpSession session, RegistrationChallengeResponse challenge, String purpose) {
        session.setAttribute(CHALLENGE_ID, challenge.challengeId());
        session.setAttribute(CHALLENGE_PHONE, challenge.maskedPhone());
        session.setAttribute(CHALLENGE_PURPOSE, purpose);
    }

    private UUID challengeId(HttpSession session) {
        if (session == null || !(session.getAttribute(CHALLENGE_ID) instanceof UUID id)) {
            return null;
        }
        Object purpose = session.getAttribute(CHALLENGE_PURPOSE);
        return LOGIN.equals(purpose) || REGISTRATION.equals(purpose) ? id : null;
    }

    private void populateVerificationModel(HttpSession session, Model model) {
        model.addAttribute("maskedPhone", session.getAttribute(CHALLENGE_PHONE));
        model.addAttribute("phoneLogin", LOGIN.equals(session.getAttribute(CHALLENGE_PURPOSE)));
    }

    private void clearChallenge(HttpSession session) {
        session.removeAttribute(CHALLENGE_ID);
        session.removeAttribute(CHALLENGE_PHONE);
        session.removeAttribute(CHALLENGE_PURPOSE);
    }

    public static final class PhoneOtpForm {
        @NotBlank(message = "Nhập mã gồm 6 chữ số.")
        @Pattern(regexp = "[0-9]{6}", message = "Nhập mã gồm 6 chữ số.")
        private String code = "";

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}
