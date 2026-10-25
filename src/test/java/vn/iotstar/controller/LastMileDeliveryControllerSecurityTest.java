package vn.iotstar.controller;

import org.junit.jupiter.api.DisplayName;
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
import vn.iotstar.service.LastMileDeliveryService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LastMileDeliveryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class LastMileDeliveryControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LastMileDeliveryService lastMileDeliveryService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("Anonymous user accessing /shipper/delivery-tasks is redirected to /login")
    void anonymousUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/shipper/delivery-tasks"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "normal_user", roles = {"USER"})
    @DisplayName("User with ROLE_USER is denied access to /shipper/delivery-tasks (403)")
    void userWithRoleUserIsForbidden() throws Exception {
        mockMvc.perform(get("/shipper/delivery-tasks"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "shipper_bob", roles = {"SHIPPER"})
    @DisplayName("Shipper accessing /shipper/delivery-tasks succeeds (200)")
    void shipperAccessSucceeds() throws Exception {
        mockMvc.perform(get("/shipper/delivery-tasks"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "shipper_bob", roles = {"SHIPPER"})
    @DisplayName("POST /shipper/delivery-tasks/scan without CSRF is rejected (403)")
    void postWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/shipper/delivery-tasks/scan")
                        .param("shipmentId", "1")
                        .param("hubId", "100"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "shipper_bob", roles = {"SHIPPER"})
    @DisplayName("POST /shipper/delivery-tasks/scan with CSRF succeeds and redirects (302 PRG)")
    void postWithCsrfSucceedsAndRedirects() throws Exception {
        mockMvc.perform(post("/shipper/delivery-tasks/scan")
                        .with(csrf())
                        .param("shipmentId", "1")
                        .param("hubId", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shipper/delivery-tasks"));
    }

    @Test
    @WithMockUser(username = "shipper_bob", roles = {"SHIPPER"})
    @DisplayName("POST /shipper/delivery-tasks/confirm with CSRF succeeds and redirects (302 PRG)")
    void confirmWithCsrfSucceeds() throws Exception {
        mockMvc.perform(post("/shipper/delivery-tasks/confirm")
                        .with(csrf())
                        .param("shipmentId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shipper/delivery-tasks"));
    }
}

