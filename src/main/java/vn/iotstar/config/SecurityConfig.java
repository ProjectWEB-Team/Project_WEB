package vn.iotstar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình Dual SecurityFilterChain cho UTEExpress Logistics:
 * 1. Chain @Order(1): Dành riêng cho REST API (/api/**) - Stateless, CSRF disabled.
 * 2. Chain @Order(2): Dành cho Web SSR JSP (/**) - Stateful Session Cookie, CSRF enabled, Form Login.
 * Tuân thủ nghiêm ngặt LECTURE_STANDARDS.md & Clean Architecture.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Bean mã hóa mật khẩu một chiều BCrypt chuẩn bảo mật bắt buộc.
     * Tuyệt đối cấm lưu Plaintext hoặc thuật toán yếu (MD5, SHA-1).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Filter Chain 1: REST API (/api/**)
     * Kiến trúc: Stateless Token / Direct JSON API, không duy trì HTTP Session.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public API: Tra cứu vận đơn & Ước tính cước phí không cần đăng nhập
                .requestMatchers("/api/tracking/**", "/api/calculator/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                // Các API tác nghiệp yêu cầu xác thực
                .requestMatchers("/api/shipper/**").hasAnyRole("SHIPPER", "ADMIN")
                .requestMatchers("/api/hub/**").hasAnyRole("HUB_STAFF", "ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    /**
     * Filter Chain 2: Web SSR JSP (/**)
     * Kiến trúc: Stateful Session Cookie, Form Login, bảo vệ CSRF chống giả mạo request.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .csrf(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                // Tài nguyên công khai (Static assets, layout, trang chủ, tra cứu, tính cước)
                .requestMatchers(
                    "/",
                    "/home",
                    "/tracking/**",
                    "/calculator/**",
                    "/auth/**",
                    "/login",
                    "/register",
                    "/assets/**",
                    "/webjars/**",
                    "/ws/**",
                    "/error"
                ).permitAll()
                // Phân quyền theo 6 Roles nghiệp vụ
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/manager/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/hub/**").hasAnyRole("ADMIN", "HUB_STAFF")
                .requestMatchers("/driver/**").hasAnyRole("ADMIN", "DRIVER")
                .requestMatchers("/shipper/**").hasAnyRole("ADMIN", "SHIPPER")
                .requestMatchers("/user/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", false)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .deleteCookies("UTEEXPRESS_SESSION", "JSESSIONID")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            );

        return http.build();
    }
}
