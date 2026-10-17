package vn.iotstar.controller;

import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.ShipmentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Exercises the real session/CSRF security chain for the Phase-01 browser login surface. */
@WebMvcTest({HomeController.class, ShipmentController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class LoginRouteSecurityTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private PasswordEncoder passwordEncoder;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private JwtService jwtService;
    @MockBean private ShipmentService shipmentService;

    @Test
    void publicLoginRouteRendersTheLoginViewForNormalAndGenericFailureStates() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));

        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));

        mockMvc.perform(get("/login").param("logout", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void internalLoginJspForwardIsPermittedByTheRealSecurityFilterChain() throws Exception {
        mockMvc.perform(get("/WEB-INF/views/auth/login.jsp").with(request -> {
                    request.setDispatcherType(DispatcherType.FORWARD);
                    return request;
                }))
                // MockMvc has no JSP servlet, so 404 proves security allowed the forward instead of redirecting to /login.
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedShipmentRouteRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/user/shipments/create"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void validRoleUserFormLoginCreatesAnAuthenticatedSessionForShipmentRoutes() throws Exception {
        when(userDetailsService.loadUserByUsername("phase01-user"))
                .thenReturn(activeUser("phase01-user", "local-uat-password"));

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "phase01-user")
                        .param("password", "local-uat-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/shipments/create"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/user/shipments/create").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("shipment/create"));
    }

    @Test
    void nonexistentAndWrongPasswordHaveTheSameGenericFailureRedirect() throws Exception {
        when(userDetailsService.loadUserByUsername("missing"))
                .thenThrow(new UsernameNotFoundException("missing"));
        when(userDetailsService.loadUserByUsername("known"))
                .thenReturn(activeUser("known", "correct-password"));

        mockMvc.perform(post("/login").with(csrf()).param("username", "missing").param("password", "any-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));

        mockMvc.perform(post("/login").with(csrf()).param("username", "known").param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void csrfRemainsEnforcedForProtectedShipmentPostsAndLogoutRetainsConfiguredRedirect() throws Exception {
        mockMvc.perform(post("/user/shipments").with(user("phase01-user").roles("USER"))
                        .param("senderName", "Owner")
                        .param("senderPhone", "0900000001")
                        .param("senderAddress", "1 Owner Street")
                        .param("receiverName", "Receiver")
                        .param("receiverPhone", "0900000002")
                        .param("receiverAddress", "2 Receiver Street"))
                .andExpect(status().isForbidden());
        verify(shipmentService, never()).create(any());

        mockMvc.perform(post("/logout").with(user("phase01-user").roles("USER")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }

    private User activeUser(String username, String rawPassword) {
        return new User(1L, username, passwordEncoder.encode(rawPassword), UserRole.ROLE_USER, "ACTIVE");
    }
}
