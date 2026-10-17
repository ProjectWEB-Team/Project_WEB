package vn.iotstar.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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

import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthApiController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
class SecurityConfigTest {
    private static final String TEST_SECRET = Base64.getEncoder().encodeToString(
            "01234567890123456789012345678901".getBytes());
    private static final String OTHER_TEST_SECRET = Base64.getEncoder().encodeToString(
            "abcdefghijklmnopqrstuvwxyz012345".getBytes());

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @MockBean private AuthService authService;
    @MockBean private UserDetailsService userDetailsService;

    @Test
    void configuredTestSecretAllowsRealTokenRoundTrip() {
        User user = activeUser();
        String token = jwtService.issueToken(user);
        assertEquals("linehaul", jwtService.extractUsername(token));
        assertTrue(jwtService.isValid(token, "linehaul"));
        assertFalse(jwtService.isValid(token, "another-user"));
    }

    @Test
    void missingOrWeakSigningSecretIsRejectedBeforeUse() {
        JwtProperties missing = new JwtProperties();
        missing.setSecret(" ");
        assertThrows(IllegalStateException.class, missing::validateSecret);

        JwtProperties weak = new JwtProperties();
        weak.setSecret(Base64.getEncoder().encodeToString("too-short".getBytes()));
        assertThrows(IllegalStateException.class, weak::validateSecret);
    }

    @Test
    void realValidTokenAuthenticatesProtectedEndpoint() throws Exception {
        User user = activeUser();
        when(userDetailsService.loadUserByUsername("linehaul")).thenReturn(user);

        mockMvc.perform(get("/api/security/actor").header("Authorization", "Bearer " + jwtService.issueToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("linehaul"));
    }

    @Test
    void expiredRealTokenCannotReachProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/security/actor").header("Authorization", "Bearer " + signedToken(TEST_SECRET,
                        Date.from(Instant.now().minusSeconds(60)))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void differentlySignedRealTokenCannotReachProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/security/actor").header("Authorization", "Bearer " + signedToken(OTHER_TEST_SECRET,
                        Date.from(Instant.now().plusSeconds(60)))) )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void malformedBearerTokenCannotReachProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/security/actor").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
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

    private User activeUser() {
        return new User(1L, "linehaul", "hash", UserRole.ROLE_LINEHAUL_DRIVER, "ACTIVE");
    }

    private String signedToken(String secret, Date expiration) {
        return Jwts.builder().subject("linehaul").issuedAt(new Date()).expiration(expiration)
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))).compact();
    }
}
