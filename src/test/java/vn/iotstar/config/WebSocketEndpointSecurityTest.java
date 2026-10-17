package vn.iotstar.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.controller.HomeController;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Confirms the HTTP handshake endpoint follows the session security chain before STOMP processing. */
@WebMvcTest(HomeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, WebSocketConfig.class})
class WebSocketEndpointSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    @Test
    void unauthenticatedWebSocketHandshakeIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get(WebSocketConfig.STOMP_ENDPOINT))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void authenticatedHandshakeReachesTheWebSocketEndpoint() throws Exception {
        mockMvc.perform(get(WebSocketConfig.STOMP_ENDPOINT))
                .andExpect(status().isBadRequest());
    }
}
