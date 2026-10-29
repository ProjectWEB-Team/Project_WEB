package vn.iotstar.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.dto.auth.RegisterUserRequest;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.service.impl.EmailAccountService;
import java.util.Set;
import java.util.UUID;

@Controller
@ConditionalOnProperty(name = "uteexpress.mail.enabled", havingValue = "true")
public class EmailAccountController {
    static final String CHALLENGE = "emailOtpChallenge";
    static final String PURPOSE = "emailOtpPurpose";
    static final String MASKED_EMAIL = "emailOtpAddress";
    private final EmailAccountService accounts;
    public EmailAccountController(EmailAccountService accounts) { this.accounts = accounts; }

    @InitBinder("registerUserRequest")
    void registrationFields(WebDataBinder binder) { binder.setAllowedFields("username", "password", "fullName", "phone", "email"); }

    @GetMapping("/auth/email/register")
    public String registerForm(Model model) {
        model.addAttribute("registerUserRequest", new RegisterUserRequest());
        model.addAttribute("emailRegistration", true);
        return "auth/register";
    }
    @PostMapping("/auth/email/register")
    public String register(@Valid @ModelAttribute("registerUserRequest") RegisterUserRequest form,
                           BindingResult errors, HttpSession session, Model model) {
        model.addAttribute("emailRegistration", true);
        try {
            if (errors.hasErrors()) return "auth/register";
            remember(session, accounts.register(form), "REGISTRATION");
            return "redirect:/auth/email/verify";
        } catch (RegistrationException exception) {
            model.addAttribute("error", exception.getMessage());
        } catch (DataIntegrityViolationException exception) {
            model.addAttribute("error", RegistrationController.DUPLICATE_ERROR);
        } finally {
            form.setPassword("");
        }
        return "auth/register";
    }

    @GetMapping("/auth/email/forgot-password")
    public String forgotForm() { return "auth/email-forgot-password"; }
    @PostMapping("/auth/email/forgot-password")
    public String forgot(@RequestParam(defaultValue = "") String email, HttpSession session, Model model) {
        try {
            remember(session, accounts.requestPasswordReset(email), "PASSWORD_RESET");
            return "redirect:/auth/email/verify";
        } catch (RegistrationException exception) {
            model.addAttribute("error", "Vui lòng nhập địa chỉ email hợp lệ.");
            return "auth/email-forgot-password";
        }
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user/email")
    public String emailSettings(Model model) {
        var status = accounts.status(AuthenticatedActor.requireCurrent().userId());
        model.addAttribute("maskedEmail", status.maskedEmail());
        model.addAttribute("emailVerified", status.verified());
        return "auth/email-settings";
    }
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/user/email")
    public String sendVerification(HttpSession session, RedirectAttributes attributes) {
        try {
            remember(session, accounts.requestVerification(AuthenticatedActor.requireCurrent().userId()), "VERIFY_EMAIL");
            return "redirect:/auth/email/verify";
        } catch (RegistrationException exception) {
            attributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/user/email";
        }
    }

    @GetMapping("/auth/email/verify")
    public String verifyForm(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (challengeId(session) == null) return "redirect:/login";
        populate(session, model);
        return "auth/verify-email";
    }
    @PostMapping(value = "/auth/email/check-code", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public java.util.Map<String, Object> checkCode(@RequestParam(defaultValue = "") String code, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        UUID id = challengeId(session);
        if (id == null) return java.util.Map.of("valid", false, "message", "Phiên xác thực đã hết hạn. Vui lòng thử lại từ đầu.");
        String purpose = (String) session.getAttribute(PURPOSE);
        boolean valid = accounts.check(id, code.trim(), purpose, actorId(request, purpose));
        return java.util.Map.of("valid", valid, "message", valid ? "Mã hợp lệ." : "Mã không chính xác hoặc đã hết hạn.");
    }
    @PostMapping("/auth/email/verify")
    public String verify(@RequestParam(defaultValue = "") String code, @RequestParam(defaultValue = "") String newPassword,
                         HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        UUID id = challengeId(session);
        if (id == null) return "redirect:/login";
        String purpose = (String) session.getAttribute(PURPOSE);
        if (accounts.verify(id, code, purpose, actorId(request, purpose), newPassword)) {
            session.removeAttribute(CHALLENGE); session.removeAttribute(PURPOSE); session.removeAttribute(MASKED_EMAIL);
            if ("VERIFY_EMAIL".equals(purpose)) return "redirect:/user/email";
            if ("PASSWORD_RESET".equals(purpose)) {
                SecurityContextHolder.clearContext(); session.invalidate();
                return "redirect:/login?reset=true";
            }
            return "redirect:/login?registered=true";
        }
        populate(session, model);
        model.addAttribute("error", "PASSWORD_RESET".equals(purpose)
                ? "Mã không hợp lệ hoặc đã hết hạn. Mật khẩu mới cần ít nhất 8 ký tự, tối đa 72 byte UTF-8."
                : "Mã không hợp lệ hoặc đã hết hạn. Vui lòng kiểm tra hoặc yêu cầu mã mới.");
        return "auth/verify-email";
    }
    @PostMapping("/auth/email/resend")
    public String resend(HttpServletRequest request, RedirectAttributes attributes) {
        HttpSession session = request.getSession(false);
        UUID id = challengeId(session);
        if (id == null) return "redirect:/login";
        String purpose = (String) session.getAttribute(PURPOSE);
        try {
            remember(session, accounts.resend(id, purpose, actorId(request, purpose)), purpose);
        } catch (RegistrationException exception) {
            // Keep the reset response identical for real and decoy challenges.
        }
        attributes.addFlashAttribute("notice", "Nếu yêu cầu hợp lệ, mã sẽ được gửi. Vui lòng chờ ít nhất 60 giây giữa các lần gửi và kiểm tra thư rác.");
        return "redirect:/auth/email/verify";
    }
    private Long actorId(HttpServletRequest request, String purpose) {
        return "VERIFY_EMAIL".equals(purpose) && request.isUserInRole("USER") ? AuthenticatedActor.requireCurrent().userId() : null;
    }
    private UUID challengeId(HttpSession session) {
        if (session == null || !(session.getAttribute(CHALLENGE) instanceof UUID id)
                || !(session.getAttribute(PURPOSE) instanceof String purpose)
                || !Set.of("REGISTRATION", "PASSWORD_RESET", "VERIFY_EMAIL").contains(purpose)) return null;
        return id;
    }
    private void remember(HttpSession session, EmailAccountService.Challenge challenge, String purpose) {
        session.setAttribute(CHALLENGE, challenge.id()); session.setAttribute(PURPOSE, purpose);
        session.setAttribute(MASKED_EMAIL, challenge.maskedEmail());
    }
    private void populate(HttpSession session, Model model) {
        model.addAttribute("maskedEmail", session.getAttribute(MASKED_EMAIL));
        model.addAttribute("emailPasswordReset", "PASSWORD_RESET".equals(session.getAttribute(PURPOSE)));
    }
}
