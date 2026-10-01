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
import vn.iotstar.service.ShipmentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Verifies the real session/CSRF SecurityFilterChain around the JSP shipment routes. */
@WebMvcTest(ShipmentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ShipmentRouteSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private ShipmentService shipmentService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    @Test
    void unauthenticatedUserIsRedirectedToTheJspLoginRoute() throws Exception {
        mockMvc.perform(get("/user/shipments/create"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "shipment-user", roles = "USER")
    void authenticatedUserCanOpenTheCreatePage() throws Exception {
        mockMvc.perform(get("/user/shipments/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("shipment/create"));
    }

    @Test
    @WithMockUser(username = "shipment-user", roles = "USER")
    void shipmentPostWithoutCsrfIsRejectedBeforeTheController() throws Exception {
        mockMvc.perform(post("/user/shipments")
                        .param("senderName", "Owner")
                        .param("senderPhone", "0900000001")
                        .param("senderAddress", "1 Owner Street")
                        .param("receiverName", "Receiver")
                        .param("receiverPhone", "0900000002")
                        .param("receiverAddress", "2 Receiver Street"))
                .andExpect(status().isForbidden());

        verify(shipmentService, never()).create(any());
    }

    @Test
    @WithMockUser(username = "shipment-user", roles = "USER")
    void shipmentPostWithCsrfReachesTheMvcServiceBoundary() throws Exception {
        when(shipmentService.create(any())).thenReturn(42L);

        mockMvc.perform(post("/user/shipments")
                        .with(csrf())
                        .param("senderName", "Owner")
                        .param("senderPhone", "0900000001")
                        .param("senderAddress", "1 Owner Street")
                        .param("receiverName", "Receiver")
                        .param("receiverPhone", "0900000002")
                        .param("receiverAddress", "2 Receiver Street"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/shipments/42"));

        verify(shipmentService).create(any());
    }
}
