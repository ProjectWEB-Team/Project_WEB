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
import vn.iotstar.security.*;
import vn.iotstar.service.impl.LastMileAssignmentService;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(LastMileAssignmentController.class)
@Import({SecurityConfig.class,JwtAuthenticationFilter.class})
class LastMileAssignmentControllerSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean LastMileAssignmentService service;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;
    @Test @WithMockUser(roles="HUB_STAFF") void bindsActualRecordAndRedirects() throws Exception {
        mvc.perform(post("/hub/delivery-assignments").with(csrf()).param("shipmentId","10").param("shipperId","2")).andExpect(redirectedUrl("/hub/delivery-assignments"));
        verify(service).assign(argThat(r->r.shipmentId()==10L&&r.shipperId()==2L));
    }
    @Test @WithMockUser(roles="HUB_STAFF") void csrfRequiredBeforeMutation() throws Exception {
        mvc.perform(post("/hub/delivery-assignments").param("shipmentId","10").param("shipperId","2")).andExpect(status().isForbidden());verifyNoInteractions(service);
    }
    @Test @WithMockUser(roles="SHIPPER") void shipperCannotAssign() throws Exception {
        mvc.perform(post("/hub/delivery-assignments").with(csrf()).param("shipmentId","10").param("shipperId","2")).andExpect(status().isForbidden());verifyNoInteractions(service);
    }
    @Test void anonymousRedirected() throws Exception {mvc.perform(get("/hub/delivery-assignments")).andExpect(redirectedUrlPattern("**/login"));}
}
