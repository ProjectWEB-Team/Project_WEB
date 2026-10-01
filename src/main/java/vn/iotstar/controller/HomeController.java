package vn.iotstar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller điều hướng trang chủ và master layout UTEExpress Logistics.
 * Tuân thủ quy chuẩn 3 tầng Clean Architecture môn Lập trình Web - HCMUTE:
 * Controller chỉ xử lý HTTP request/response và điều hướng View.
 */
@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String index(Model model) {
        model.addAttribute("pageTitle", "UTEExpress - Hệ thống Quản lý Chuỗi Giao nhận Logistics HCMUTE");
        model.addAttribute("systemName", "UTEExpress Logistics");
        model.addAttribute("activeMenu", "home");
        return "index";
    }

    /** Renders the public form-login view; Spring Security owns POST /login authentication. */
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }
}
