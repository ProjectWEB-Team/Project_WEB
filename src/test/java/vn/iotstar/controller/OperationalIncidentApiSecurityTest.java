package vn.iotstar.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.LinehaulTripService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** API chain proof: delay evidence is a JWT driver capability, never a public incident endpoint. */
@WebMvcTest(DriverOperationsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class OperationalIncidentApiSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private LinehaulTripService linehaulTripService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @Test void missingBearerTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/driver/trips/40/delay").contentType("application/json").content("{\"note\":\"delay\"}"))
                .andExpect(status().isUnauthorized());
    }
    @Test @WithMockUser(roles = "USER") void nonDriverCannotCreateDelayEvidence() throws Exception {
        mockMvc.perform(post("/api/driver/trips/40/delay").contentType("application/json").content("{\"note\":\"delay\"}"))
                .andExpect(status().isForbidden());
    }
}
