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
import vn.iotstar.service.FinanceReconciliationService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FinanceReconciliationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class FinanceReconciliationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FinanceReconciliationService reconciliationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("Anonymous user accessing /manager/reconciliation is redirected to /login")
    void anonymousUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/manager/reconciliation"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "shipper_bob", roles = {"SHIPPER"})
    @DisplayName("Shipper accessing /manager/reconciliation is forbidden (403)")
    void shipperAccessForbidden() throws Exception {
        mockMvc.perform(get("/manager/reconciliation"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager_alice", roles = {"MANAGER"})
    @DisplayName("Manager accessing /manager/reconciliation succeeds (200)")
    void managerAccessSucceeds() throws Exception {
        mockMvc.perform(get("/manager/reconciliation"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "manager_alice", roles = {"MANAGER"})
    @DisplayName("POST /manager/reconciliation/submit without CSRF is rejected (403)")
    void postWithoutCsrfRejected() throws Exception {
        mockMvc.perform(post("/manager/reconciliation/submit")
                        .param("shipperId", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager_alice", roles = {"MANAGER"})
    @DisplayName("POST /manager/reconciliation/submit with CSRF succeeds and redirects (302 PRG)")
    void postWithCsrfSucceeds() throws Exception {
        mockMvc.perform(post("/manager/reconciliation/submit")
                        .with(csrf())
                        .param("shipperId", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/manager/reconciliation*"));
    }
}

