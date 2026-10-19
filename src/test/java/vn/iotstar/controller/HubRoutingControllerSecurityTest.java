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
import vn.iotstar.dto.hub.SortingDecisionResponse;
import vn.iotstar.exception.RouteConfigurationException;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.HubOperationsService;

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

/** Filter-enabled contract for the CSRF-protected hub sorter command. */
@WebMvcTest(HubOperationsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class HubRoutingControllerSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private HubOperationsService hubOperationsService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    @Test
    void anonymousUserCannotOpenHubWorkspace() throws Exception {
        mockMvc.perform(get("/hub/sorter"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "HUB_STAFF")
    void hubStaffCanOpenSorterWorkspace() throws Exception {
        mockMvc.perform(get("/hub/sorter"))
                .andExpect(status().isOk())
                .andExpect(view().name("hub/sorter-queue"));
    }

    @Test
    @WithMockUser(roles = "HUB_STAFF")
    void sorterPostWithoutCsrfIsRejectedBeforeService() throws Exception {
        mockMvc.perform(post("/hub/sorter").param("shipmentId", "101"))
                .andExpect(status().isForbidden());
        verify(hubOperationsService, never()).sortAtHub(any());
    }

    @Test
    @WithMockUser(roles = "HUB_STAFF")
    void sorterPostWithCsrfReachesServiceAndUsesPrg() throws Exception {
        when(hubOperationsService.sortAtHub(any()))
                .thenReturn(new SortingDecisionResponse("SOC-LA", "SOUTH / Long An", true, false));

        mockMvc.perform(post("/hub/sorter").with(csrf()).param("shipmentId", "101"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hub/sorter"));
        verify(hubOperationsService).sortAtHub(any());
    }

    @Test
    @WithMockUser(roles = "HUB_STAFF")
    void blockedRouteKeepsTheStaffResponseGenericAndUsesPrg() throws Exception {
        when(hubOperationsService.sortAtHub(any())).thenThrow(new RouteConfigurationException());

        mockMvc.perform(post("/hub/sorter").with(csrf()).param("shipmentId", "101"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hub/sorter"));

        verify(hubOperationsService).sortAtHub(any());
    }
}
