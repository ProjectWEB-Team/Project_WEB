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
class LastMileReturnControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LastMileDeliveryService lastMileDeliveryService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("Anonymous user accessing /shipper/return-tasks is redirected to /login")
    void anonymousUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/shipper/return-tasks"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "shipper_bob", roles = {"SHIPPER"})
    @DisplayName("Shipper accessing /shipper/return-tasks succeeds (200)")
    void shipperReturnTasksSucceeds() throws Exception {
        mockMvc.perform(get("/shipper/return-tasks"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "shipper_bob", roles = {"SHIPPER"})
    @DisplayName("POST /shipper/delivery-tasks/failure without CSRF is rejected (403)")
    void failureWithoutCsrfRejected() throws Exception {
        mockMvc.perform(post("/shipper/delivery-tasks/failure")
                        .param("shipmentId", "1")
                        .param("reason", "NO_ANSWER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "shipper_bob", roles = {"SHIPPER"})
    @DisplayName("POST /shipper/delivery-tasks/failure with CSRF succeeds (302 PRG)")
    void failureWithCsrfSucceeds() throws Exception {
        mockMvc.perform(post("/shipper/delivery-tasks/failure")
                        .with(csrf())
                        .param("shipmentId", "1")
                        .param("reason", "NO_ANSWER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shipper/delivery-tasks"));
    }

    @Test
    @WithMockUser(username = "shipper_bob", roles = {"SHIPPER"})
    @DisplayName("POST /shipper/return-tasks/confirm with CSRF succeeds (302 PRG)")
    void confirmReturnWithCsrfSucceeds() throws Exception {
        mockMvc.perform(post("/shipper/return-tasks/confirm")
                        .with(csrf())
                        .param("shipmentId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shipper/return-tasks"));
    }

    @Test
    @WithMockUser(username = "user_sender", roles = {"USER"})
    @DisplayName("User POST /user/return-collection with CSRF succeeds (302 PRG)")
    void userReturnCollectionWithCsrfSucceeds() throws Exception {
        mockMvc.perform(post("/user/return-collection")
                        .with(csrf())
                        .param("shipmentId", "1")
                        .param("handoverType", "SELF_PICKUP")
                        .param("identityProof", "CITIZEN_123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/return-collection"));
    }
}

