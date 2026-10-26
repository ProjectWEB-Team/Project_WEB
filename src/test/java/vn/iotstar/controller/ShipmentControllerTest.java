package vn.iotstar.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.validation.BindingResult;
import org.springframework.mock.web.MockHttpSession;
import vn.iotstar.dto.shipment.QuoteResponse;
import vn.iotstar.dto.shipment.ShipmentDetailResponse;
import vn.iotstar.entity.ShipmentStatus;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.ShipmentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
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
                .andExpect(model().attributeExists("createShipmentRequest", "addressProvinces"))
                .andExpect(model().attribute("addressProvinces", org.hamcrest.Matchers.hasSize(34)));
    }

    @Test
    void validFormUsesServiceAndRedirectsToDetail() throws Exception {
        QuotedForm quoted = quoteForm();
        when(shipmentService.create(any(), eq(new BigDecimal("35000")))).thenReturn(42L);

        mockMvc.perform(quotedCreate(quoted).param("pickupMethod", "HOME_PICKUP").param("shippingFee", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/shipments/42"));

        verify(shipmentService).create(any(), eq(new BigDecimal("35000")));
    }

    @Test
    void invalidFormReturnsValidationFeedbackWithoutPersistence() throws Exception {
        mockMvc.perform(post("/user/shipments").param("senderName", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("shipment/create"))
                .andExpect(model().attributeHasFieldErrors("createShipmentRequest", "senderName", "receiverName"));

        verifyNoInteractions(shipmentService);
    }

    @Test
    void validQuoteKeepsTheFormAndDisplaysTheServerPriceWithoutCreatingShipment() throws Exception {
        QuoteResponse quote = new QuoteResponse(new BigDecimal("35000"));
        when(shipmentService.quote(any())).thenReturn(quote);

        mockMvc.perform(validForm("/user/shipments/quote").param("shippingFee", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("shipment/create"))
                .andExpect(model().attribute("quote", quote))
                .andExpect(model().attribute("createShipmentRequest", hasProperty("senderName", is("Owner"))))
                .andExpect(model().hasNoErrors());

        verify(shipmentService).quote(any());
        verify(shipmentService, never()).create(any());
    }

    @Test
    void invalidQuoteFieldsReturnFeedbackBeforeCallingService() throws Exception {
        mockMvc.perform(post("/user/shipments/quote").param("weightGrams", "0").param("lengthCm", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("shipment/create"))
                .andExpect(model().attributeHasFieldErrors("createShipmentRequest", "weightGrams", "lengthCm"))
                .andExpect(model().attributeDoesNotExist("quote"));

        verifyNoInteractions(shipmentService);
    }

    @Test void rejectsWardFromAnotherProvinceBeforeCallingQuoteService() throws Exception {
        var request = validForm("/user/shipments/quote");
        request.with(servletRequest -> { servletRequest.setParameter("receiverWardCode", "00004"); return servletRequest; });
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("createShipmentRequest"))
                .andExpect(model().attributeDoesNotExist("quote"));
        verifyNoInteractions(shipmentService);
    }

    @Test void derivesQuoteRegionsEvenWhenClientSubmitsOtherValidRegions() throws Exception {
        when(shipmentService.quote(any())).thenReturn(new QuoteResponse(new BigDecimal("22000")));
        var request = validForm("/user/shipments/quote");
        request.with(servletRequest -> { servletRequest.setParameter("fromRegion", "NORTH"); servletRequest.setParameter("toRegion", "CENTRAL"); return servletRequest; });
        mockMvc.perform(request).andExpect(model().hasNoErrors());
        verify(shipmentService).quote(org.mockito.ArgumentMatchers.argThat(r -> "SOUTH".equals(r.getFromRegion()) && "SOUTH".equals(r.getToRegion())));
    }

    @Test
    void missingPricePolicyReturnsSafeFeedbackAndPreservesForm() throws Exception {
        String message = "Chưa có bảng giá cho dịch vụ và tuyến này.";
        when(shipmentService.quote(any())).thenThrow(new IllegalArgumentException(message));

        var result = mockMvc.perform(validForm("/user/shipments/quote"))
                .andExpect(status().isOk())
                .andExpect(view().name("shipment/create"))
                .andExpect(model().attributeHasErrors("createShipmentRequest"))
                .andExpect(model().attribute("createShipmentRequest", hasProperty("receiverName", is("Receiver"))))
                .andExpect(model().attributeDoesNotExist("quote"))
                .andReturn();

        BindingResult errors = (BindingResult) result.getModelAndView().getModel()
                .get(BindingResult.MODEL_KEY_PREFIX + "createShipmentRequest");
        assertThat(errors.getGlobalErrors()).singleElement().satisfies(error -> {
            assertThat(error.getCode()).isEqualTo("quote.rejected");
            assertThat(error.getDefaultMessage()).isEqualTo(message);
        });
        verify(shipmentService, never()).create(any());
    }

    @Test
    void successfulPaymentConfirmationRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/user/shipments/42/confirm-payment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/shipments/42"))
                .andExpect(flash().attributeCount(0));

        verify(shipmentService).confirmPayment(42L);
    }

    @Test
    void rejectedPaymentRedirectsWithDomainFeedback() throws Exception {
        String message = "Đơn này không thanh toán trực tuyến.";
        doThrow(new IllegalArgumentException(message)).when(shipmentService).confirmPayment(42L);

        mockMvc.perform(post("/user/shipments/42/confirm-payment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/shipments/42"))
                .andExpect(flash().attribute("error", message));

        verify(shipmentService).confirmPayment(42L);
    }

    @Test
    void detailUsesResponseDtoFromService() throws Exception {
        when(shipmentService.getDetail(42L)).thenReturn(new ShipmentDetailResponse(42L, "UTX-001", ShipmentStatus.CREATED,
                "Owner", "0900000001", "A", "Receiver", "0900000002", "B",
                LocalDateTime.of(2026, 10, 1, 10, 0), List.of(),
                new java.math.BigDecimal("25000"), "UNPAID", "CASH", "SELF_DROPOFF", "SENDER", null));

        mockMvc.perform(get("/user/shipments/42"))
                .andExpect(status().isOk())
                .andExpect(view().name("shipment/detail"))
                .andExpect(model().attributeExists("shipment"));

        verify(shipmentService).getDetail(42L);
    }

    @Test
    void creatingWithoutAReviewedQuoteReturnsFeedback() throws Exception {
        mockMvc.perform(validForm("/user/shipments"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("createShipmentRequest"))
                .andExpect(view().name("shipment/create"));
        verifyNoInteractions(shipmentService);
    }

    @ParameterizedTest
    @CsvSource({"weightGrams,900", "receiverName,Changed", "feePayer,RECEIVER", "paymentMethod,CASH", "serviceCode,FAST", "receiverWardCode,26743"})
    void changingReviewedInputsRequiresANewQuote(String field, String value) throws Exception {
        QuotedForm quoted = quoteForm();
        var request = quotedCreate(quoted);
        request.with(servletRequest -> { servletRequest.setParameter(field, value); return servletRequest; });
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("createShipmentRequest"))
                .andExpect(model().attributeDoesNotExist("quote"));
        verify(shipmentService, never()).create(any(), any());
    }

    @Test
    void quoteReferenceBelongsToItsSession() throws Exception {
        QuotedForm quoted = quoteForm();
        mockMvc.perform(validForm("/user/shipments").session(new MockHttpSession())
                        .param("quoteReference", quoted.reference()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("createShipmentRequest"));
        verify(shipmentService, never()).create(any(), any());
    }

    @Test
    void successfulCreationConsumesQuoteAndRepeatedSubmitCannotCreateAgain() throws Exception {
        QuotedForm quoted = quoteForm();
        when(shipmentService.create(any(), any())).thenReturn(42L);
        mockMvc.perform(quotedCreate(quoted)).andExpect(redirectedUrl("/user/shipments/42"));
        mockMvc.perform(quotedCreate(quoted)).andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("createShipmentRequest"));
        verify(shipmentService, times(1)).create(any(), any());
    }

    @Test
    void changedServerPriceRequiresReviewWithoutDiscardingFormFields() throws Exception {
        QuotedForm quoted = quoteForm();
        when(shipmentService.create(any(), any())).thenThrow(new IllegalArgumentException("Bảng giá đã thay đổi."));
        mockMvc.perform(quotedCreate(quoted)).andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("createShipmentRequest"))
                .andExpect(model().attribute("createShipmentRequest", hasProperty("receiverName", is("Receiver"))))
                .andExpect(model().attributeDoesNotExist("quote"));
        mockMvc.perform(quotedCreate(quoted)).andExpect(status().isOk());
        verify(shipmentService, times(1)).create(any(), any());
    }

    @Test
    void missingRegionsAndOutOfRangeMoneyReturnFieldFeedback() throws Exception {
        var request = validForm("/user/shipments/quote");
        request.with(servletRequest -> {
            servletRequest.setParameter("fromRegion", ""); servletRequest.setParameter("toRegion", "");
            servletRequest.setParameter("declaredValue", "10000000000000"); servletRequest.setParameter("codAmount", "0.001");
            return servletRequest;
        });
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("createShipmentRequest", "fromRegion", "toRegion", "declaredValue", "codAmount"));
        verifyNoInteractions(shipmentService);
    }

    private QuotedForm quoteForm() throws Exception {
        when(shipmentService.quote(any())).thenReturn(new QuoteResponse(new BigDecimal("35000")));
        MockHttpSession session = new MockHttpSession();
        var result = mockMvc.perform(validForm("/user/shipments/quote").session(session))
                .andExpect(model().attributeExists("quoteReference")).andReturn();
        return new QuotedForm(session, (String) result.getModelAndView().getModel().get("quoteReference"));
    }

    private MockHttpServletRequestBuilder quotedCreate(QuotedForm quoted) {
        return validForm("/user/shipments").session(quoted.session()).param("quoteReference", quoted.reference());
    }

    private record QuotedForm(MockHttpSession session, String reference) { }

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
