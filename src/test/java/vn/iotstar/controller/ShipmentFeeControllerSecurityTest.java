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
import vn.iotstar.service.impl.ShipmentFeeService;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(ShipmentFeeController.class)
@Import({SecurityConfig.class,JwtAuthenticationFilter.class})
class ShipmentFeeControllerSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean ShipmentFeeService fees;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;
    @Test @WithMockUser(roles="USER") void walletUsesServerSelectedMethod() throws Exception {
        mvc.perform(post("/user/shipment-fees/10/wallet").with(csrf()).param("obligation","WEIGHT_SURCHARGE").param("amount","1")).andExpect(redirectedUrl("/user/shipment-fees/10"));
        verify(fees).paySurcharge(10L,"E_WALLET",false);
    }
    @Test @WithMockUser(roles="USER") void explicitDemoBindsConfirmation() throws Exception {
        mvc.perform(post("/user/shipment-fees/10/demo-vnpay").with(csrf()).param("obligation","RETURN").param("confirmDemo","true")).andExpect(redirectedUrl("/user/shipment-fees/10"));verify(fees).payReturn(10L,"VNPAY",true);
    }
    @Test @WithMockUser(roles="USER") void csrfRequired() throws Exception {
        mvc.perform(post("/user/shipment-fees/10/wallet").param("obligation","RETURN")).andExpect(status().isForbidden());verifyNoInteractions(fees);
    }
    @Test @WithMockUser(roles="USER") void senderCannotClaimStaffCash() throws Exception {
        mvc.perform(post("/hub/return-collection/cash").with(csrf()).param("shipmentId","10")).andExpect(status().isForbidden());verifyNoInteractions(fees);
    }
    @Test @WithMockUser(roles="HUB_STAFF") void staffCashHasNoClientAmount() throws Exception {
        mvc.perform(post("/hub/return-collection/cash").with(csrf()).param("shipmentId","10").param("amount","1")).andExpect(status().is3xxRedirection());verify(fees).payReturn(10L,"CASH",false);
    }
}
