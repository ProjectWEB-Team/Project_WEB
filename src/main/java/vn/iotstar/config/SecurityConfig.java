package vn.iotstar.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.GoogleLoginSuccessHandler;

/** Preserves the canonical session/JSP and stateless JWT/API security split. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, LogisticsProperties.class})
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
                                                      AuthenticationProvider authenticationProvider) throws Exception {
        http.securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(apiAuthenticationEntryPoint())
                        .accessDeniedHandler(apiAccessDeniedHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/tracking/**", "/api/calculator/**", "/api/auth/**").permitAll()
                        .requestMatchers("/api/shipper/**").hasAnyRole("SHIPPER", "ADMIN")
                        .requestMatchers("/api/driver/**").hasAnyRole("LINEHAUL_DRIVER", "ADMIN")
                        .requestMatchers("/api/hub/**").hasAnyRole("HUB_STAFF", "ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http, AuthenticationProvider authenticationProvider,
            ObjectProvider<ClientRegistrationRepository> clientRegistrations,
            ObjectProvider<GoogleLoginSuccessHandler> googleHandlers)
            throws Exception {
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .csrf(Customizer.withDefaults())
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/", "/home", "/tracking/**", "/calculator/**", "/auth/**", "/login",
                                "/register", "/oauth2/authorization/google", "/login/oauth2/code/google",
                                "/assets/**", "/webjars/**", "/error").permitAll()
                        .requestMatchers("/ws", "/ws/**").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/manager/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/hub/**").hasAnyRole("ADMIN", "HUB_STAFF")
                        .requestMatchers("/driver/**").hasAnyRole("ADMIN", "LINEHAUL_DRIVER")
                        .requestMatchers("/shipper/**").hasAnyRole("ADMIN", "SHIPPER")
                        .requestMatchers("/user/**").authenticated().anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login")
                        .defaultSuccessUrl("/user/shipments/create", true).failureUrl("/login?error=true").permitAll())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout=true")
                        .deleteCookies("UTEEXPRESS_SESSION", "JSESSIONID").invalidateHttpSession(true)
                        .clearAuthentication(true).permitAll());
        ClientRegistrationRepository registrations = clientRegistrations.getIfAvailable();
        if (registrations != null && registrations.findByRegistrationId("google") != null) {
            GoogleLoginSuccessHandler handler = googleHandlers.getObject();
            http.oauth2Login(oauth -> oauth.loginPage("/login")
                    .successHandler(handler)
                    .failureHandler((request, response, exception) -> handler.reject(request, response)));
        }
        return http.build();
    }

    private AuthenticationEntryPoint apiAuthenticationEntryPoint() {
        return (request, response, exception) -> writeApiError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
    }
    private AccessDeniedHandler apiAccessDeniedHandler() {
        return (request, response, exception) -> writeApiError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied");
    }
    private void writeApiError(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
