package vn.iotstar.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.ContainerHandoverService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.security.test.context.support.WithMockUser;

/** Filter-enabled browser route proof; CSRF is enforced before custody commands reach the service. */
@WebMvcTest({OutboundOperationsController.class, DriverHandoverController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class HandoverControllerSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private ContainerHandoverService containerHandoverService;
    @MockBean private vn.iotstar.repository.HubRepository hubRepository;
    @MockBean private vn.iotstar.repository.UserRepository userRepository;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    @Test void anonymousCannotOpenOutboundWorkspace() throws Exception {
        mockMvc.perform(get("/hub/outbound")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
    }
    @Test @WithMockUser(roles = "HUB_STAFF") void hubStaffCanOpenOutboundWorkspace() throws Exception {
        mockMvc.perform(get("/hub/outbound")).andExpect(status().isOk()).andExpect(view().name("hub/outbound-queue"));
    }
    @Test @WithMockUser(roles = "HUB_STAFF") void custodyPostWithoutCsrfIsRejectedBeforeService() throws Exception {
        mockMvc.perform(post("/hub/outbound/load").param("containerId", "201").param("shipmentId", "101")).andExpect(status().isForbidden());
        verify(containerHandoverService, never()).loadShipment(any());
    }
    @Test @WithMockUser(roles = "HUB_STAFF") void custodyPostWithCsrfUsesPrgAndReachesService() throws Exception {
        mockMvc.perform(post("/hub/outbound/load").with(csrf()).param("containerId", "201").param("shipmentId", "101"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/hub/outbound"));
        verify(containerHandoverService).loadShipment(any());
    }
    @Test @WithMockUser(roles = "HUB_STAFF") void correctionRemainsCsrfProtectedAndDelegatesToService() throws Exception {
        mockMvc.perform(post("/hub/outbound/handovers/correct").with(csrf()).param("handoverId", "301").param("replacementContainerIds", "201"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/hub/outbound"));
        verify(containerHandoverService).correctByCancellationAndRecreation(any());
    }
    @Test @WithMockUser(roles = "LINEHAUL_DRIVER") void assignedDriverRouteEnforcesCsrfAndDelegates() throws Exception {
        mockMvc.perform(post("/driver/handovers/scan").with(csrf()).param("handoverId", "301").param("containerId", "201"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/driver"));
        verify(containerHandoverService).scanAndAccept(any());
    }
}
