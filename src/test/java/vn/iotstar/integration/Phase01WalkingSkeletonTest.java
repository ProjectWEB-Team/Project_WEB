package vn.iotstar.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.controller.ShipmentController;
import vn.iotstar.dto.realtime.ShipmentCreatedEventResponse;
import vn.iotstar.entity.Shipment;
import vn.iotstar.entity.ShipmentEvent;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.realtime.ShipmentEventPublisher;
import vn.iotstar.entity.PricingPolicy;
import vn.iotstar.repository.PaymentRepository;
import vn.iotstar.repository.PricingPolicyRepository;
import vn.iotstar.repository.ShipmentEventRepository;
import vn.iotstar.repository.ShipmentRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.impl.ShipmentCancellation;
import vn.iotstar.service.impl.ShipmentServiceImpl;

import java.util.List;
import java.util.Optional;
import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** MVC/security/service wiring evidence; real SQL Server persistence remains separately environment-gated. */
@WebMvcTest(ShipmentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ShipmentServiceImpl.class})
class Phase01WalkingSkeletonTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private ShipmentRepository shipmentRepository;
    @MockBean private ShipmentEventRepository shipmentEventRepository;
    @MockBean private UserRepository userRepository;
    @MockBean private PricingPolicyRepository pricingPolicyRepository;
    @MockBean private PaymentRepository paymentRepository;
    @MockBean private ShipmentEventPublisher shipmentEventPublisher;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private Clock clock;
    @MockBean private ShipmentCancellation shipmentCancellation;
    @MockBean private vn.iotstar.repository.HubRepository hubRepository;

    @Test
    void authenticatedOwnerCreatesReadsAndPublishesTheMinimalShipmentTracer() throws Exception {
        when(clock.instant()).thenReturn(Instant.parse("2026-10-23T00:00:00Z"));
        var destination=vn.iotstar.entity.Hub.active("DEST", "Destination", 1, "DELIVERY_HUB", "SOUTH", "HCM");
        ReflectionTestUtils.setField(destination,"id",99L);
        when(hubRepository.findByStatusAndTypeAndProvinceAndDistrict("ACTIVE","DELIVERY_HUB","HCM","Q1"))
                .thenReturn(List.of(destination));
        User owner = new User(7L, "owner", "hash", UserRole.ROLE_USER, "ACTIVE");
        Authentication actor = new UsernamePasswordAuthenticationToken(owner, null, owner.getAuthorities());
        PricingPolicy policy = mock(PricingPolicy.class);
        when(policy.getBasePrice()).thenReturn(new java.math.BigDecimal("20000"));
        when(policy.getBaseWeightG()).thenReturn(500);
        when(policy.getExtraWeightStepG()).thenReturn(500);
        when(policy.getExtraWeightPrice()).thenReturn(new java.math.BigDecimal("5000"));
        when(pricingPolicyRepository.findFirstByServiceCodeAndFromRegionAndToRegionAndStatusOrderByIdDesc(
                "STANDARD", "SOUTH", "SOUTH", "ACTIVE")).thenReturn(Optional.of(policy));
        when(userRepository.getReferenceById(7L)).thenReturn(owner);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> {
            Shipment shipment = invocation.getArgument(0);
            ReflectionTestUtils.setField(shipment, "id", 42L);
            return shipment;
        });
        when(shipmentEventRepository.save(any(ShipmentEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockHttpSession session = new MockHttpSession();
        var quoted = mockMvc.perform(validForm("/user/shipments/quote").session(session)
                        .with(authentication(actor)).with(csrf()))
                .andExpect(status().isOk()).andExpect(model().attributeExists("quoteReference")).andReturn();
        String reference = (String) quoted.getModelAndView().getModel().get("quoteReference");
        mockMvc.perform(validForm("/user/shipments").session(session).param("quoteReference", reference)
                        .with(authentication(actor)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/shipments/42"));

        org.mockito.ArgumentCaptor<Shipment> shipmentCaptor = org.mockito.ArgumentCaptor.forClass(Shipment.class);
        org.mockito.ArgumentCaptor<ShipmentEvent> eventCaptor = org.mockito.ArgumentCaptor.forClass(ShipmentEvent.class);
        org.mockito.ArgumentCaptor<ShipmentCreatedEventResponse> publicationCaptor =
                org.mockito.ArgumentCaptor.forClass(ShipmentCreatedEventResponse.class);
        verify(shipmentRepository).save(shipmentCaptor.capture());
        verify(shipmentEventRepository).save(eventCaptor.capture());
        verify(shipmentEventPublisher).publishAfterCommit(any(), publicationCaptor.capture());
        assertThat(shipmentCaptor.getValue().getCurrentStatus()).isEqualTo(ShipmentStatus.WAITING_FOR_PAYMENT);
        assertThat(shipmentCaptor.getValue().getDestinationHubId()).isEqualTo(99L);
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("SHIPMENT_CREATED");
        assertThat(publicationCaptor.getValue().trackingNumber()).isEqualTo(shipmentCaptor.getValue().getTrackingNumber());
        assertThat(publicationCaptor.getValue().currentStatus()).isEqualTo(ShipmentStatus.WAITING_FOR_PAYMENT);

        when(shipmentRepository.findById(42L)).thenReturn(Optional.of(shipmentCaptor.getValue()));
        when(shipmentEventRepository.findByShipmentIdOrderByCreatedAtAsc(42L)).thenReturn(List.of(eventCaptor.getValue()));

        mockMvc.perform(get("/user/shipments/42").with(authentication(actor)))
                .andExpect(status().isOk())
                .andExpect(view().name("shipment/detail"))
                .andExpect(model().attributeExists("shipment"));
    }

    private MockHttpServletRequestBuilder validForm(String path) {
        return post(path).param("senderName", "Owner").param("senderPhone", "0900000001")
                .param("senderAddress", "1 Owner Street").param("receiverName", "Receiver")
                .param("receiverPhone", "0900000002").param("receiverAddress", "2 Receiver Street")
                .param("receiverProvince", "HCM").param("receiverDistrict", "Q1")
                .param("fromRegion", "SOUTH").param("toRegion", "SOUTH").param("weightGrams", "800")
                .param("lengthCm", "10").param("widthCm", "10").param("heightCm", "10");
    }
}
