package vn.iotstar.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.WalletLedgerService;
import vn.iotstar.service.WithdrawalService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({WalletController.class, AdminWithdrawalController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class WalletAndWithdrawalControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletLedgerService walletLedgerService;

    @MockBean
    private WithdrawalService withdrawalService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("Anonymous user accessing /user/wallet is redirected to /login")
    void anonymousUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/user/wallet"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "normal_user", roles = {"USER"})
    @DisplayName("Authenticated user accessing /user/wallet succeeds (200)")
    void authenticatedUserReachesWallet() throws Exception {
        mockMvc.perform(get("/user/wallet").with(authenticatedAs(UserRole.ROLE_USER)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "normal_user", roles = {"USER"})
    @DisplayName("POST /user/wallet/withdraw without CSRF is rejected (403)")
    void withdrawWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/user/wallet/withdraw")
                        .with(authenticatedAs(UserRole.ROLE_USER))
                        .param("amount", "50000")
                        .param("bankName", "Vietcombank")
                        .param("accountNumber", "0123456789")
                        .param("accountHolderName", "Nguyen Van A"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "normal_user", roles = {"USER"})
    @DisplayName("POST /user/wallet/withdraw with CSRF redirects back to the wallet (302 PRG)")
    void withdrawWithCsrfRedirects() throws Exception {
        mockMvc.perform(post("/user/wallet/withdraw")
                        .with(csrf())
                        .with(authenticatedAs(UserRole.ROLE_USER))
                        .param("amount", "50000")
                        .param("bankName", "Vietcombank")
                        .param("accountNumber", "0123456789")
                        .param("accountHolderName", "Nguyen Van A"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/wallet"));
    }

    @Test
    @DisplayName("User with ROLE_USER is denied access to /admin/withdrawals (403)")
    void userCannotReachAdminWithdrawals() throws Exception {
        mockMvc.perform(get("/admin/withdrawals")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("normal_user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_bob", roles = {"ADMIN"})
    @DisplayName("Administrator reaches /admin/withdrawals (200)")
    void adminReachesWithdrawals() throws Exception {
        mockMvc.perform(get("/admin/withdrawals"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin_bob", roles = {"ADMIN"})
    @DisplayName("POST /admin/withdrawals/{id}/approve without CSRF is rejected (403)")
    void approveWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/admin/withdrawals/7/approve"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_bob", roles = {"ADMIN"})
    @DisplayName("POST /admin/withdrawals/{id}/approve with CSRF redirects back (302 PRG)")
    void approveWithCsrfRedirects() throws Exception {
        mockMvc.perform(post("/admin/withdrawals/7/approve").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/withdrawals"));
    }

    /**
     * The server derives the identity from the security context, so the test
     * principal has to be the UTEExpress user entity and not a plain Spring
     * user details object.
     */
    private RequestPostProcessor authenticatedAs(UserRole role) {
        User principal = new User(42L, "test_" + role.name().toLowerCase(), "hash", role, "ACTIVE");
        org.mockito.Mockito.when(userDetailsService.loadUserByUsername(principal.getUsername())).thenReturn(principal);
        Authentication authentication =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return request;
        };
    }
}
