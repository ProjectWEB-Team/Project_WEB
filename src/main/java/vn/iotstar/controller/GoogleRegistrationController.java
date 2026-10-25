package vn.iotstar.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import vn.iotstar.dto.auth.GoogleRegistrationRequest;
import vn.iotstar.entity.User;
import vn.iotstar.exception.GoogleAccountException;
import vn.iotstar.security.AccountSessionAuthenticator;
import vn.iotstar.security.VerifiedGoogleIdentity;
import vn.iotstar.service.GoogleAccountService;

import java.time.Clock;

@Controller
public class GoogleRegistrationController {
    private final GoogleAccountService accounts;
    private final AccountSessionAuthenticator sessionAuthenticator;
    private final Clock clock;

    public GoogleRegistrationController(GoogleAccountService accounts,
                                        AccountSessionAuthenticator sessionAuthenticator, Clock clock) {
        this.accounts = accounts;
        this.sessionAuthenticator = sessionAuthenticator;
        this.clock = clock;
    }

    @GetMapping("/auth/google/complete")
    public String show(HttpServletRequest request, Model model) {
        VerifiedGoogleIdentity identity = currentIdentity(request.getSession(false));
        if (identity == null) {
            return "redirect:/login?googleError=true";
        }
        GoogleRegistrationRequest form = new GoogleRegistrationRequest();
        form.setFullName(identity.fullName());
        model.addAttribute("form", form);
        model.addAttribute("googleEmail", identity.email());
        return "auth/google-complete";
    }

    @PostMapping("/auth/google/complete")
    public String complete(@Valid @ModelAttribute("form") GoogleRegistrationRequest form,
                           BindingResult errors, HttpServletRequest request,
                           HttpServletResponse response, Model model) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return "redirect:/login?googleError=true";
        }
        synchronized (session) {
            VerifiedGoogleIdentity identity = currentIdentity(session);
            if (identity == null) {
                return "redirect:/login?googleError=true";
            }
            model.addAttribute("googleEmail", identity.email());
            if (errors.hasErrors()) {
                return "auth/google-complete";
            }
            try {
                User account = accounts.register(identity, form);
                session.removeAttribute(VerifiedGoogleIdentity.SESSION_ATTRIBUTE);
                sessionAuthenticator.signIn(account, request, response);
                return "redirect:/user/shipments/create";
            } catch (GoogleAccountException | DataIntegrityViolationException exception) {
                model.addAttribute("error", new GoogleAccountException().getMessage());
                return "auth/google-complete";
            }
        }
    }

    private VerifiedGoogleIdentity currentIdentity(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(VerifiedGoogleIdentity.SESSION_ATTRIBUTE);
        if (value instanceof VerifiedGoogleIdentity identity && identity.isCurrent(clock.instant())) {
            return identity;
        }
        session.removeAttribute(VerifiedGoogleIdentity.SESSION_ATTRIBUTE);
        return null;
    }
}
