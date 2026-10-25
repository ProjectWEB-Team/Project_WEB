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
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MissingParcelController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class MissingParcelControllerSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean DestinationReceiptService service;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;
    @Test @WithMockUser(roles="HUB_STAFF") void staffCannotMakeInvestigationDecision() throws Exception {
        mvc.perform(post("/manager/incidents/missing/resolve").with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test @WithMockUser(roles="MANAGER") void managerDetailUsesIncidentScopedService() throws Exception {
        mvc.perform(get("/manager/incidents/missing/77")).andExpect(view().name("manager/missing-parcels"));
        verify(service).missingCase(77L);
    }
    @Test @WithMockUser(roles="MANAGER") void resolutionRequiresCsrfAndValidatedEvidence() throws Exception {
        mvc.perform(post("/manager/incidents/missing/resolve")).andExpect(status().isForbidden());
        mvc.perform(post("/manager/incidents/missing/resolve").with(csrf()).param("outcome","BYPASS"))
                .andExpect(redirectedUrl("/manager/incidents/missing"));
        verify(service,never()).resolveMissing(any());
        mvc.perform(post("/manager/incidents/missing/resolve").with(csrf())
                .param("incidentId","77").param("handoverId","20").param("containerId","30")
                .param("shipmentId","41").param("foundHubId","2").param("outcome","FOUND").param("evidence","Camera and physical scan checked"))
                .andExpect(redirectedUrl("/manager/incidents/missing"));
        verify(service).resolveMissing(any());
    }
}
