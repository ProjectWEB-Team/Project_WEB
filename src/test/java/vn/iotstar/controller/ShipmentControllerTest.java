package vn.iotstar.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.dto.shipment.ShipmentDetailResponse;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.ShipmentService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShipmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShipmentControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private ShipmentService shipmentService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    @Test
    void displaysTheMinimalCreateForm() throws Exception {
        mockMvc.perform(get("/user/shipments/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("shipment/create"))
                .andExpect(model().attributeExists("createShipmentRequest"));
    }

    @Test
    void validFormUsesServiceAndRedirectsToDetail() throws Exception {
        when(shipmentService.create(any())).thenReturn(42L);

        mockMvc.perform(post("/user/shipments")
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

    @Test
    void invalidFormReturnsValidationFeedbackWithoutPersistence() throws Exception {
        mockMvc.perform(post("/user/shipments").param("senderName", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("shipment/create"))
                .andExpect(model().attributeHasFieldErrors("createShipmentRequest", "senderName", "receiverName"));

        verify(shipmentService, never()).create(any());
    }

    @Test
    void detailUsesResponseDtoFromService() throws Exception {
        when(shipmentService.getDetail(42L)).thenReturn(new ShipmentDetailResponse(42L, "UTX-001", ShipmentStatus.CREATED,
                "Owner", "0900000001", "A", "Receiver", "0900000002", "B",
                LocalDateTime.of(2026, 10, 1, 10, 0), List.of()));

        mockMvc.perform(get("/user/shipments/42"))
                .andExpect(status().isOk())
                .andExpect(view().name("shipment/detail"))
                .andExpect(model().attributeExists("shipment"));

        verify(shipmentService).getDetail(42L);
    }
}
