package vn.iotstar.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.config.JwtProperties;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.controller.api.AuthApiController;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.service.AuthService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthApiController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class SecurityConfigTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private AuthService authService;
    @MockBean private JwtService ignoredJwtService;
    @MockBean private UserDetailsService userDetailsService;

    @Test
    void tokenRoundTripUsesCanonicalUserIdentity() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-only-uteexpress-jwt-secret-with-at-least-thirty-two-characters");
        JwtService jwtService = new JwtService(properties);
        User user = new User(1L, "linehaul", "hash", UserRole.ROLE_LINEHAUL_DRIVER, "ACTIVE");
        String token = jwtService.issueToken(user);
        assertEquals("linehaul", jwtService.extractUsername(token));
        assertTrue(jwtService.isValid(token, "linehaul"));
        assertFalse(jwtService.isValid(token, "another-user"));
    }

    @Test
    void apiProtectedPathRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/security/actor")).andExpect(status().isUnauthorized());
    }

    @Test
    void webStateChangeRetainsCsrfProtection() throws Exception {
        mockMvc.perform(post("/user/profile")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LINEHAUL_DRIVER")
    void canonicalLinehaulRolePassesWebAuthorizationBeforeRouting() throws Exception {
        mockMvc.perform(get("/driver/route")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void legacyDriverRoleIsDenied() throws Exception {
        mockMvc.perform(get("/driver/route")).andExpect(status().isForbidden());
    }
}
