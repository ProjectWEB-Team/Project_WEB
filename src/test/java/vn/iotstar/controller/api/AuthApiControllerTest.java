package vn.iotstar.controller.api;

import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.dto.auth.ApiLoginResponse;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.AuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthApiController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthApiControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private AuthService authService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    @Test
    void validApiCredentialsReceiveABearerToken() throws Exception {
        when(authService.login(any())).thenReturn(new ApiLoginResponse("signed-token", "Bearer", 1800));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"api-user\",\"password\":\"correct-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void invalidCredentialsReceiveGenericUnauthorizedResponse() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("secret detail"));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"api-user\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void malformedBearerTokenCannotReachProtectedEndpoint() throws Exception {
        when(jwtService.extractUsername("bad-token")).thenThrow(new MalformedJwtException("bad"));
        mockMvc.perform(get("/api/security/actor").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void validBearerUsesServerPrincipalRatherThanRequestProvidedIdentity() throws Exception {
        User persistedUser = new User(7L, "api-user", "hash", UserRole.ROLE_USER, "ACTIVE");
        when(jwtService.extractUsername("valid-token")).thenReturn("api-user");
        when(userDetailsService.loadUserByUsername("api-user")).thenReturn(persistedUser);
        when(jwtService.isValid("valid-token", "api-user")).thenReturn(true);

        mockMvc.perform(get("/api/security/actor").param("userId", "999")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.username").value("api-user"));
    }
}
