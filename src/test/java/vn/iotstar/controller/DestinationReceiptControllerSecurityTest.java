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
import vn.iotstar.service.DestinationReceiptService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real session filter-chain proof for the protected destination receipt browser surface. */
@WebMvcTest(InboundOperationsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DestinationReceiptControllerSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private vn.iotstar.repository.DestinationReceiptRepository destinationReceiptRepository;
    @MockBean private DestinationReceiptService destinationReceiptService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    @Test void anonymousInboundWorkspaceRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/hub/inbound")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
    }
    @Test @WithMockUser(roles = "HUB_STAFF") void staffCanOpenInboundWorkspace() throws Exception {
        mockMvc.perform(get("/hub/inbound")).andExpect(status().isOk()).andExpect(view().name("hub/inbound-queue"));
    }
    @Test @WithMockUser(roles = "HUB_STAFF") void receiptPostCannotBypassCsrf() throws Exception {
        mockMvc.perform(post("/hub/inbound/receive").param("handoverId", "1").param("containerId", "2").param("shipmentId", "3"))
                .andExpect(status().isForbidden());
        verify(destinationReceiptService, never()).receiveItem(any());
    }
    @Test @WithMockUser(roles = "HUB_STAFF") void receiptPostWithCsrfDelegatesToService() throws Exception {
        mockMvc.perform(post("/hub/inbound/receive").with(csrf()).param("handoverId", "1").param("containerId", "2").param("shipmentId", "3"))
                .andExpect(redirectedUrl("/hub/inbound"));
        verify(destinationReceiptService).receiveItem(any());
    }
    @Test @WithMockUser(roles = "HUB_STAFF") void unpackingBindsActualScansAndEmptyReceiptRequiresExplicitConfirmation() throws Exception {
        mockMvc.perform(post("/hub/inbound/reconcile").with(csrf()).param("handoverId","20").param("containerId","30").param("receivedShipmentIds","40,41"))
                .andExpect(redirectedUrl("/hub/inbound"));
        verify(destinationReceiptService).reconcileContainer(new vn.iotstar.dto.hub.DestinationReconciliationCommand(20L,30L,java.util.List.of(40L,41L)));
        mockMvc.perform(post("/hub/inbound/reconcile-empty").with(csrf()).param("handoverId","20").param("containerId","30"))
                .andExpect(redirectedUrl("/hub/inbound"));
        verify(destinationReceiptService,never()).reconcileContainer(new vn.iotstar.dto.hub.DestinationReconciliationCommand(20L,30L,java.util.List.of()));
        mockMvc.perform(post("/hub/inbound/reconcile-empty").with(csrf()).param("handoverId","20").param("containerId","30").param("confirmAllMissing","true"))
                .andExpect(redirectedUrl("/hub/inbound"));
        verify(destinationReceiptService).reconcileContainer(new vn.iotstar.dto.hub.DestinationReconciliationCommand(20L,30L,java.util.List.of()));
    }
}
