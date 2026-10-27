package vn.iotstar.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.realtime.ShipmentEventPublisher;
import vn.iotstar.repository.HubRepository;
import vn.iotstar.repository.PaymentRepository;
import vn.iotstar.repository.PricingPolicyRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.impl.ShipmentCancellation;
import vn.iotstar.service.impl.ShipmentServiceImpl;

import java.time.Clock;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** HARD-01 ownership cells through filter chain, controller, exception mapping and the real ShipmentServiceImpl. */
@WebMvcTest(ShipmentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ShipmentServiceImpl.class})
class PermissionMatrixShipmentOwnershipSliceTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private ShipmentRepository shipmentRepository;
    @MockBean private ShipmentEventRepository shipmentEventRepository;
    @MockBean private UserRepository userRepository;
    @MockBean private ShipmentEventPublisher shipmentEventPublisher;
    @MockBean private PricingPolicyRepository pricingPolicyRepository;
    @MockBean private PaymentRepository paymentRepository;
    @MockBean private Clock clock;
    @MockBean private ShipmentCancellation cancellation;
    @MockBean private HubRepository hubRepository;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    private final User owner = new User(1L, "owner", "hash", UserRole.ROLE_USER, "ACTIVE");
    private final User stranger = new User(2L, "stranger", "hash", UserRole.ROLE_USER, "ACTIVE");

    @BeforeEach
    void shipmentOwnedByUserOne() {
        when(userDetailsService.loadUserByUsername(owner.getUsername())).thenReturn(owner);
        when(userDetailsService.loadUserByUsername(stranger.getUsername())).thenReturn(stranger);
        Shipment shipment = Shipment.create("UTX-OWNED", owner, "Owner", "0900000001", "A", "R", "0900000002", "B");
        ReflectionTestUtils.setField(shipment, "id", 42L);
        ReflectionTestUtils.setField(shipment, "currentStatus", ShipmentStatus.WAITING_FOR_HANDOVER);
        when(shipmentRepository.findById(42L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(shipment));
    }

    @Test
    void ownerReadsTheDetail() throws Exception {
        mockMvc.perform(get("/user/shipments/42").with(as(owner)))
                .andExpect(status().isOk()).andExpect(view().name("shipment/detail"));
    }

    @Test
    void strangerCannotReadTheDetail() throws Exception {
        mockMvc.perform(get("/user/shipments/42").with(as(stranger)))
                .andExpect(status().isForbidden()).andExpect(view().name("error/403"))
                .andExpect(model().attributeDoesNotExist("shipment"));
    }

    @Test
    void strangerCannotCancel() throws Exception {
        mockMvc.perform(post("/user/shipments/42/cancel").with(csrf()).with(as(stranger)))
                .andExpect(status().isForbidden());
        verify(cancellation, never()).cancel(any(), any(), any());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void ownerCanCancel() throws Exception {
        mockMvc.perform(post("/user/shipments/42/cancel").with(csrf()).with(as(owner)))
                .andExpect(status().is3xxRedirection());
        verify(cancellation).cancel(any(), eq("SENDER_REQUEST"), any());
    }

    @Test
    void strangerCannotConfirmPayment() throws Exception {
        mockMvc.perform(post("/user/shipments/42/confirm-payment").with(csrf()).with(as(stranger)))
                .andExpect(status().isForbidden());
        verify(paymentRepository, never()).findFirstByShipmentIdAndPaymentType(anyLong(), any());
        verify(paymentRepository, never()).save(any());
        verify(shipmentEventRepository, never()).save(any());
    }

    @Test
    void listQueriesOnlyTheCallersOwnShipments() throws Exception {
        when(shipmentRepository.findBySenderIdOrderByCreatedAtDesc(eq(2L), any(Pageable.class))).thenReturn(Page.empty());
        mockMvc.perform(get("/user/shipments").with(as(stranger))).andExpect(status().isOk());
        verify(shipmentRepository).findBySenderIdOrderByCreatedAtDesc(eq(2L), any(Pageable.class));
        verify(shipmentRepository, never()).findBySenderIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class));
    }

    private static RequestPostProcessor as(User user) {
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
