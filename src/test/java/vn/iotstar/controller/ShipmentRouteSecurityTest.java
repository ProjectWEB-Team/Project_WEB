package vn.iotstar.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.dto.shipment.QuoteResponse;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.exception.ShipmentAccessDeniedException;
import vn.iotstar.security.AuthenticatedActor;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.ShipmentService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Verifies the real session/CSRF SecurityFilterChain around the JSP shipment routes. */
@WebMvcTest(ShipmentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ShipmentRouteSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private ShipmentService shipmentService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    @Test
    void unauthenticatedUserIsRedirectedToTheJspLoginRoute() throws Exception {
        mockMvc.perform(get("/user/shipments/create"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "shipment-user", roles = "USER")
    void authenticatedUserCanOpenTheCreatePage() throws Exception {
        mockMvc.perform(get("/user/shipments/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("shipment/create"));
    }

    @Test
    @WithMockUser(username = "shipment-user", roles = "USER")
    void shipmentPostWithoutCsrfIsRejectedBeforeTheController() throws Exception {
        mockMvc.perform(post("/user/shipments")
                        .param("senderName", "Owner")
                        .param("senderPhone", "0900000001")
                        .param("senderAddress", "1 Owner Street")
                        .param("receiverName", "Receiver")
                        .param("receiverPhone", "0900000002")
                        .param("receiverAddress", "2 Receiver Street"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(shipmentService);
    }

    @Test
    @WithMockUser(username = "shipment-user", roles = "USER")
    void shipmentPostWithCsrfReachesTheMvcServiceBoundary() throws Exception {
        when(shipmentService.quote(any())).thenReturn(new QuoteResponse(new BigDecimal("35000")));
        when(shipmentService.create(any(), any())).thenReturn(42L);
        MockHttpSession session = new MockHttpSession();
        var quote = mockMvc.perform(validForm("/user/shipments/quote").session(session).with(csrf())).andReturn();
        String reference = (String) quote.getModelAndView().getModel().get("quoteReference");

        mockMvc.perform(validForm("/user/shipments").session(session).param("quoteReference", reference).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/shipments/42"));

        verify(shipmentService).create(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/user/shipments/quote", "/user/shipments/42/confirm-payment"})
    @WithMockUser(username = "shipment-user", roles = "USER")
    void quoteAndPaymentWithoutCsrfAreRejectedBeforeService(String path) throws Exception {
        mockMvc.perform(post(path)).andExpect(status().isForbidden());

        verifyNoInteractions(shipmentService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/user/shipments/quote", "/user/shipments/42/confirm-payment"})
    void quoteAndPaymentRequireAuthenticationEvenWithCsrf(String path) throws Exception {
        mockMvc.perform(post(path).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(shipmentService);
    }

    @Test
    void authenticatedQuoteWithCsrfUsesTheSessionPrincipalAndServerPrice() throws Exception {
        User owner = new User(7L, "shipment-owner", "test-hash", UserRole.ROLE_USER, "ACTIVE");
        when(userDetailsService.loadUserByUsername(owner.getUsername())).thenReturn(owner);
        QuoteResponse quote = new QuoteResponse(new BigDecimal("35000"));
        when(shipmentService.quote(any())).thenAnswer(invocation -> {
            assertThat(AuthenticatedActor.requireCurrent().userId()).isEqualTo(7L);
            return quote;
        });

        mockMvc.perform(validForm("/user/shipments/quote").with(user(owner)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("shipment/create"))
                .andExpect(model().attribute("quote", quote));

        verify(shipmentService).quote(any());
    }

    @Test
    void paymentWithCsrfUsesAuthenticatedActorInsteadOfSubmittedOwner() throws Exception {
        User owner = new User(7L, "shipment-owner", "test-hash", UserRole.ROLE_USER, "ACTIVE");
        when(userDetailsService.loadUserByUsername(owner.getUsername())).thenReturn(owner);
        doAnswer(invocation -> {
            assertThat(AuthenticatedActor.requireCurrent().userId()).isEqualTo(7L);
            return null;
        }).when(shipmentService).confirmPayment(42L);

        mockMvc.perform(post("/user/shipments/42/confirm-payment").with(user(owner)).with(csrf())
                        .param("senderUserId", "99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/shipments/42"));

        verify(shipmentService).confirmPayment(42L);
    }

    @Test
    @WithMockUser(username = "another-user", roles = "USER")
    void ownershipDenialFromPaymentServiceRemainsForbidden() throws Exception {
        doThrow(new ShipmentAccessDeniedException()).when(shipmentService).confirmPayment(42L);

        mockMvc.perform(post("/user/shipments/42/confirm-payment").with(csrf()))
                .andExpect(status().isForbidden());

        verify(shipmentService).confirmPayment(42L);
    }

    private MockHttpServletRequestBuilder validForm(String path) {
        return post(path).param("senderProvinceCode", "79").param("senderWardCode", "26740")
                .param("receiverProvinceCode", "79").param("receiverWardCode", "26740").param("senderName", "Owner").param("senderPhone", "0900000001")
                .param("senderAddress", "1 Owner Street").param("receiverName", "Receiver")
                .param("receiverPhone", "0900000002").param("receiverAddress", "2 Receiver Street")
                .param("receiverProvince", "HCM").param("receiverDistrict", "Q1")
                .param("fromRegion", "SOUTH").param("toRegion", "SOUTH")
                .param("weightGrams", "800").param("lengthCm", "10").param("widthCm", "10").param("heightCm", "10");
    }
}
