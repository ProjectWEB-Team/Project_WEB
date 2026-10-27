package vn.iotstar.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.impl.PublicPricingService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicPricingController.class)
@Import(SecurityConfig.class)
class PublicPricingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicPricingService publicPricingService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void testCalculatorRedirectsToPricing() throws Exception {
        mockMvc.perform(get("/calculator"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pricing"));
    }

    @Test
    void testPricingPageReturnsPublicPricingView() throws Exception {
        when(publicPricingService.active()).thenReturn(List.of());
        mockMvc.perform(get("/pricing"))
                .andExpect(status().isOk())
                .andExpect(view().name("pricing/public"))
                .andExpect(model().attributeExists("policies"));
    }
}
