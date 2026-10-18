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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DriverOperationsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class LinehaulTripRouteSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private LinehaulTripService linehaulTripService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @Test void anonymousDriverWorkspaceRedirectsToLogin() throws Exception { mockMvc.perform(get("/driver")).andExpect(status().is3xxRedirection()); }
    @Test @WithMockUser(roles = "LINEHAUL_DRIVER") void driverWorkspaceRenders() throws Exception { mockMvc.perform(get("/driver")).andExpect(status().isOk()).andExpect(view().name("driver/trips")); }
    @Test @WithMockUser(roles = "LINEHAUL_DRIVER") void startTripRetainsCsrfBoundary() throws Exception {
        mockMvc.perform(post("/driver/trips/start").param("tripId", "40")).andExpect(status().isForbidden());
        mockMvc.perform(post("/driver/trips/start").with(csrf()).param("tripId", "40")).andExpect(redirectedUrl("/driver"));
        verify(linehaulTripService).startTrip(40L);
    }
    @Test void apiDriverActionDoesNotAllowAnonymousAccess() throws Exception {
        mockMvc.perform(post("/api/driver/trips/40/delay").contentType("application/json").content("{\"note\":\"delay\"}"))
                .andExpect(status().isUnauthorized());
    }
}
