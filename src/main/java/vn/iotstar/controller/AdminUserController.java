package vn.iotstar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.entity.UserRole;
import vn.iotstar.service.impl.AdminUserService;

/** Administrator account management; authorization is enforced again in AdminUserService. */
@Controller
@RequestMapping("/admin/users")
public class AdminUserController {
    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page, Model model) {
        var users = service.search(q, page);
        var hubs = service.hubs();
        var hubCodes = new java.util.HashMap<Long, String>();
        hubs.forEach(h -> hubCodes.put(h.getId(), h.getCode()));
        // Plain text per user: the profile's hub relation is lazy and the view renders outside the transaction.
        var workplaces = new java.util.HashMap<Long, String>();
        users.forEach(u -> service.profileOf(u.getId()).ifPresent(p ->
                workplaces.put(u.getId(), hubCodes.get(p.getHubId()) + " · " + p.getPosition())));
        model.addAttribute("users", users);
        model.addAttribute("workplaces", workplaces);
        model.addAttribute("hubs", hubs);
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("positions", AdminUserService.POSITIONS);
        model.addAttribute("q", q);
        return "admin/users";
    }

    @PostMapping
    public String create(@RequestParam String username, @RequestParam String fullName, @RequestParam String phone,
                         @RequestParam String email, @RequestParam UserRole role,
                         @RequestParam(required = false) Long hubId, @RequestParam(required = false) String position,
                         RedirectAttributes redirect) {
        try {
            String password = service.createStaff(username, fullName, phone, email, role, hubId, blankToNull(position));
            redirect.addFlashAttribute("successMessage", "Đã tạo tài khoản " + username
                    + ". Mật khẩu tạm (chỉ hiện một lần, hãy gửi riêng cho nhân viên): " + password);
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("errorMessage", FlashMessages.of(exception));
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{userId}/lock")
    public String lock(@PathVariable Long userId, @RequestParam boolean locked, RedirectAttributes redirect) {
        try {
            service.setLocked(userId, locked);
            redirect.addFlashAttribute("successMessage", locked ? "Đã khoá tài khoản." : "Đã mở khoá tài khoản.");
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("errorMessage", FlashMessages.of(exception));
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{userId}/assign")
    public String assign(@PathVariable Long userId, @RequestParam UserRole role,
                         @RequestParam(required = false) Long hubId, @RequestParam(required = false) String position,
                         RedirectAttributes redirect) {
        try {
            service.assign(userId, role, hubId, blankToNull(position));
            redirect.addFlashAttribute("successMessage", "Đã cập nhật vai trò và nơi làm việc.");
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("errorMessage", FlashMessages.of(exception));
        }
        return "redirect:/admin/users";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
