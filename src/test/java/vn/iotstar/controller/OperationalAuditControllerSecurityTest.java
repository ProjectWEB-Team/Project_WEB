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
import vn.iotstar.dto.operations.OperationsAuditTimelineResponse;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.OperationsAuditService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OperationsAuditController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class OperationalAuditControllerSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private OperationsAuditService operationsAuditService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    @Test void anonymousAuditRouteRedirectsToLogin() throws Exception { mockMvc.perform(get("/hub/audit/shipments/44")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login")); }
    @Test @WithMockUser(roles = "HUB_STAFF") void scopedHubStaffCanReachAuditAdapter() throws Exception {
        when(operationsAuditService.getTimeline(44L)).thenReturn(new OperationsAuditTimelineResponse(44L, List.of()));
        mockMvc.perform(get("/hub/audit/shipments/44")).andExpect(status().isOk()).andExpect(view().name("hub/audit-timeline"));
        verify(operationsAuditService).getTimeline(44L);
    }
    @Test @WithMockUser(roles = "MANAGER") void managerUsesSeparateSupervisorRoute() throws Exception {
        when(operationsAuditService.getTimeline(44L)).thenReturn(new OperationsAuditTimelineResponse(44L, List.of()));
        mockMvc.perform(get("/manager/audit/shipments/44")).andExpect(status().isOk());
    }
    @Test @WithMockUser(roles = "USER") void ordinaryUserCannotProbeAuditRoute() throws Exception { mockMvc.perform(get("/hub/audit/shipments/44")).andExpect(status().isForbidden()); verifyNoInteractions(operationsAuditService); }
}
