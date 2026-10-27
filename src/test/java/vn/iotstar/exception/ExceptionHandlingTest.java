package vn.iotstar.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.controller.ShipmentController;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.ShipmentService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ShipmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebExceptionHandler.class)
class ExceptionHandlingTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private ShipmentService shipmentService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    @Test
    void missingShipmentUsesSafeNotFoundView() throws Exception {
        when(shipmentService.getDetail(404L)).thenThrow(new ResourceNotFoundException("Shipment"));

        mockMvc.perform(get("/user/shipments/404"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    void ownershipFailureUsesSafeForbiddenView() throws Exception {
        when(shipmentService.getDetail(42L)).thenThrow(new ShipmentAccessDeniedException());

        mockMvc.perform(get("/user/shipments/42"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
    }

    @Test
    void plainServiceDenialUsesSafeForbiddenView() throws Exception {
        when(shipmentService.getDetail(43L)).thenThrow(new org.springframework.security.access.AccessDeniedException("Sender ownership required"));

        mockMvc.perform(get("/user/shipments/43"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
    }

    @Test
    void staleConcurrentUpdateUsesConflictView() throws Exception {
        when(shipmentService.getDetail(409L)).thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException("Shipment", 409L));

        mockMvc.perform(get("/user/shipments/409"))
                .andExpect(status().isConflict())
                .andExpect(view().name("error/409"));
    }

    @Test
    void unexpectedFailureUsesGenericServerErrorView() throws Exception {
        when(shipmentService.getDetail(500L)).thenThrow(new IllegalStateException("jdbc password leaked"));

        mockMvc.perform(get("/user/shipments/500"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/500"));
    }
}
