package vn.iotstar.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.controller.api.TrackingApiController;
import vn.iotstar.dto.tracking.PublicTrackingResponse;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.FirstMileService;
import vn.iotstar.service.TrackingService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Route-level access for tracking, recipient, coordinator and shipper screens. */
@WebMvcTest({TrackingController.class, TrackingApiController.class, IncomingShipmentController.class,
        HubPickupController.class, ShipperTaskController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class IntakeRouteSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TrackingService trackingService;
    @MockBean private FirstMileService firstMileService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    @Test
    void guestCanSearchTrackingAndUnknownCodeIsAFriendlyMessage() throws Exception {
        when(trackingService.track("UTX-NONE")).thenThrow(new ResourceNotFoundException("Shipment"));
        mockMvc.perform(get("/tracking").param("code", "UTX-NONE"))
                .andExpect(status().isOk()).andExpect(view().name("tracking/search"))
                .andExpect(model().attribute("notFound", true));
    }

    @Test
    void publicTrackingApiNeedsNoTokenAndReturns404ForUnknownCode() throws Exception {
        when(trackingService.track("UTX-1")).thenReturn(new PublicTrackingResponse("UTX-1", ShipmentStatus.PICKED_UP, null, List.of()));
        mockMvc.perform(get("/api/tracking/UTX-1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus").value("PICKED_UP"));
        when(trackingService.track("UTX-2")).thenThrow(new ResourceNotFoundException("Shipment"));
        mockMvc.perform(get("/api/tracking/UTX-2")).andExpect(status().isNotFound());
    }

    @Test
    void incomingRequiresLoginAndUnlockRequiresCsrf() throws Exception {
        mockMvc.perform(get("/user/incoming")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(post("/user/incoming/42/unlock").param("trackingNumber", "UTX-1").with(user("receiver").roles("USER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(trackingService);
    }

    @Test
    void coordinatorScreenIsForHubStaffOnly() throws Exception {
        mockMvc.perform(get("/hub/pickups").with(user("customer").roles("USER"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/hub/pickups").with(user("coord").roles("HUB_STAFF")))
                .andExpect(status().isOk()).andExpect(view().name("hub/pickups"));
        mockMvc.perform(post("/hub/pickups/42/assign").param("shipperId", "30").with(user("coord").roles("HUB_STAFF")).with(csrf()))
                .andExpect(redirectedUrl("/hub/pickups"));
        verify(firstMileService).assignPickup(42L, 30L);
    }

    @Test
    void shipperScreenIsForShippersOnlyAndUsesPrg() throws Exception {
        mockMvc.perform(get("/shipper/tasks").with(user("coord").roles("HUB_STAFF"))).andExpect(status().isForbidden());
        mockMvc.perform(post("/shipper/tasks/5/complete").param("trackingNumber", "UTX-1")
                        .with(user("shipper").roles("SHIPPER")).with(csrf()))
                .andExpect(redirectedUrl("/shipper/tasks"));
        verify(firstMileService).completePickup(5L, "UTX-1");
    }
}
