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
import vn.iotstar.dto.operations.ManagerOperationsDashboardResponse;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.ManagerOperationsService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ManagerOperationsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ManagerOperationsControllerSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private ManagerOperationsService managerOperationsService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    @Test void anonymousDashboardRedirectsToLogin() throws Exception { mockMvc.perform(get("/manager/operations")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login")); }
    @Test @WithMockUser(roles = "MANAGER") void managerGetsDedicatedDashboard() throws Exception {
        when(managerOperationsService.getDashboard()).thenReturn(new ManagerOperationsDashboardResponse("SOC-A", false, 1, 2, 3, 4, 5, 6, 7));
        mockMvc.perform(get("/manager/operations")).andExpect(status().isOk()).andExpect(view().name("manager/operations-dashboard")).andExpect(model().attributeExists("dashboard"));
        verify(managerOperationsService).getDashboard();
    }
    @Test @WithMockUser(roles = "HUB_STAFF") void nonManagerCannotOpenDashboard() throws Exception { mockMvc.perform(get("/manager/operations")).andExpect(status().isForbidden()); verifyNoInteractions(managerOperationsService); }
    @Test @WithMockUser(roles = "MANAGER") void workItemDetailsAreLoadedThroughScopedService() throws Exception {
        when(managerOperationsService.getWorkItem("MISSING", 4L)).thenReturn(new vn.iotstar.dto.operations.OperationalWorkItemResponse("MISSING", 4L, 101L, 11L, "Investigation required"));
        mockMvc.perform(get("/manager/operations/work/MISSING/4")).andExpect(status().isOk())
                .andExpect(view().name("manager/operation-work-item")).andExpect(model().attributeExists("item"));
        verify(managerOperationsService).getWorkItem("MISSING", 4L);
    }
    @Test @WithMockUser(roles = "USER") void nonManagerCannotOpenWorkItemDetails() throws Exception {
        mockMvc.perform(get("/manager/operations/work/MISSING/4")).andExpect(status().isForbidden());
        verifyNoInteractions(managerOperationsService);
    }
}
