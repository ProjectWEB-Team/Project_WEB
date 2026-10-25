package vn.iotstar.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.iotstar.config.SecurityConfig;
import vn.iotstar.dto.driver.VehicleOptionResponse;
import vn.iotstar.dto.hub.*;
import vn.iotstar.dto.operations.*;
import vn.iotstar.security.JwtAuthenticationFilter;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.*;

import java.net.*;
import java.math.BigDecimal;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Real Jasper/EL compilation, populated production DTOs and real session/CSRF forms. */
@SpringBootTest(classes = Phase05OperationsJspRenderingTest.RenderingApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.address=127.0.0.1", "spring.mvc.view.prefix=/WEB-INF/views/", "spring.mvc.view.suffix=.jsp",
        "uteexpress.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=", "spring.main.banner-mode=off"})
class Phase05OperationsJspRenderingTest {
    @LocalServerPort int port;
    @Autowired PasswordEncoder passwords;
    @MockBean UserDetailsService users;
    @MockBean JwtService jwt;
    @MockBean ManagerOperationsService managers;
    @MockBean LinehaulTripService trips;
    @MockBean DestinationReceiptService receipts;
    @MockBean HubOperationsService hubs;
    @MockBean OperationsAuditService audits;
    @MockBean vn.iotstar.service.impl.LastMileAssignmentService assignments;
    @MockBean vn.iotstar.service.impl.ShipmentFeeService fees;
    @MockBean vn.iotstar.service.impl.ShipmentReturnServiceImpl returns;
    @MockBean LastMileDeliveryService delivery;
    @MockBean WalletLedgerService wallets;
    @MockBean WithdrawalService withdrawals;
    @MockBean FinanceReconciliationService reconciliation;
    @MockBean ShipmentFinanceService finances;
    HttpClient browser;

    @BeforeEach void browser() {
        browser = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Test void managerDashboardAndWorkItemRenderEscapedNonemptyRecords() throws Exception {
        login("MANAGER");
        var item = new OperationalWorkItemResponse("MISSING", 7L, 42L, 3L, "Missing <parcel>");
        when(managers.getDashboard()).thenReturn(new ManagerOperationsDashboardResponse("Hub <North>", false,
                2, 3, 4, 5, 6, 7, 8));
        when(managers.getWorkItems()).thenReturn(List.of(item));
        when(managers.getWorkItem("MISSING", 7L)).thenReturn(item);
        assertThat(html("/manager/operations")).contains("Hub &lt;North&gt;", "Missing &lt;parcel&gt;",
                "/manager/operations/work/MISSING/7").doesNotContain("Missing <parcel>");
        assertThat(html("/manager/operations/work/MISSING/7"))
                .contains("/manager/incidents/missing/7", "Missing &lt;parcel&gt;");
    }

    @Test void missingCaseRendersAndSubmitsTheActualInvestigationForm() throws Exception {
        login("MANAGER");
        var item = new MissingParcelCaseResponse(7L, 42L, 9L, 3L, "handover=11, container=12", null);
        when(receipts.missingCases()).thenReturn(List.of(item));
        when(receipts.missingCase(7L)).thenReturn(item);
        assertThat(html("/manager/incidents/missing")).contains("/manager/incidents/missing/7");
        String form = html("/manager/incidents/missing/7");
        assertThat(form).contains("name=\"incidentId\" value=\"7\"", "CONFIRMED_LOST", "handover=11, container=12");
        assertThat(post("/manager/incidents/missing/resolve", form, Map.of("incidentId", "7", "shipmentId", "42",
                "foundHubId", "3", "handoverId", "11", "containerId", "12", "outcome", "FOUND",
                "evidence", "Physical inventory verified")).statusCode()).isEqualTo(302);
        verify(receipts).resolveMissing(new MissingParcelResolutionCommand(7L, 11L, 12L, 42L, 3L,
                "FOUND", "Physical inventory verified"));
    }

    @Test void driverVehicleSelectionAndTripFormsSurviveRealJasper() throws Exception {
        login("LINEHAUL_DRIVER");
        when(trips.availableVehicles()).thenReturn(List.of(new VehicleOptionResponse(13L, "51A-12345", "TRUCK")));
        String form = html("/driver");
        assertThat(form).contains("value=\"13\"", "51A-12345", "name=\"vehicleId\"", "/driver/trips/arrive");
        assertThat(post("/driver/handovers/accept", form, Map.of("handoverId", "11", "vehicleId", "13"))
                .statusCode()).isEqualTo(302);
        verify(trips).acceptHandover(11L, 13L);
        assertThat(post("/driver/trips/start", form, Map.of("tripId", "9")).statusCode()).isEqualTo(302);
        assertThat(post("/driver/trips/arrive", form, Map.of("tripId", "9")).statusCode()).isEqualTo(302);
        verify(trips).startTrip(9L);
        verify(trips).arriveTrip(9L);
    }

    @Test void sorterFlashDecisionAndAuditNestedRecordsRender() throws Exception {
        login("HUB_STAFF");
        when(hubs.sortAtHub(any())).thenReturn(new SortingDecisionResponse("NEXT-HUB", "DISTRICT-1", true, false));
        String form = html("/hub/sorter");
        assertThat(post("/hub/sorter", form, Map.of("shipmentId", "42")).statusCode()).isEqualTo(302);
        assertThat(html("/hub/sorter")).contains("NEXT-HUB", "DISTRICT-1");
        when(audits.getTimeline(42L)).thenReturn(new OperationsAuditTimelineResponse(42L,
                List.of(new OperationsAuditTimelineEntry("HUB_RECEIVED", LocalDateTime.of(2026, 10, 25, 9, 0),
                        "ORIGIN", "SYSTEM", 12L, 11L, 9L))));
        assertThat(html("/hub/audit/shipments/42")).contains("HUB_RECEIVED", "ORIGIN", "SYSTEM");
    }

    @Test void actualPostWithoutCsrfIsRejectedBeforeCallingService() throws Exception {
        login("LINEHAUL_DRIVER");
        var response = browser.send(HttpRequest.newBuilder(uri("/driver/trips/start"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("tripId=9")).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(403);
        verifyNoInteractions(trips);
    }

    @Test void populatedAssignmentAndReturnCollectionFormsBindProductionRequests() throws Exception {
        login("HUB_STAFF");
        when(assignments.candidates()).thenReturn(List.of(new vn.iotstar.dto.delivery.DeliveryAssignmentCandidate(42L,"UTX-42","FORWARD")));
        when(assignments.shippers()).thenReturn(List.of(new vn.iotstar.dto.firstmile.ShipperOptionResponse(88L,"Shipper <A>")));
        String form=html("/hub/delivery-assignments");
        assertThat(form).contains("UTX-42","Shipper &lt;A&gt;","value=\"88\"");
        assertThat(post("/hub/delivery-assignments",form,Map.of("shipmentId","42","shipperId","88")).statusCode()).isEqualTo(302);
        verify(assignments).assign(new vn.iotstar.dto.delivery.DeliveryAssignmentRequest(42L,88L));
        String collection=html("/hub/return-collection");
        assertThat(collection).contains("value=\"SELF_PICKUP\"","/hub/return-collection/cash");
        assertThat(post("/hub/return-collection",collection,Map.of("shipmentId","42","handoverType","SELF_PICKUP","identityProof","Verified sender")).statusCode()).isEqualTo(302);
        verify(delivery).processReturnHandover(argThat(r->r.getShipmentId().equals(42L)&&"Verified sender".equals(r.getIdentityProof())));
    }

    @Test void senderFeeObligationsRenderAndUseExplicitDemoConfirmation() throws Exception {
        login("USER");
        when(fees.view(42L)).thenReturn(new vn.iotstar.dto.returnflow.ShipmentFeeResponse(42L,"UTX-42",money("30000"),money("25000"),money("5000")));
        String form=html("/user/shipment-fees/42");
        assertThat(form).contains("UTX-42","25000","value=\"RETURN\"","value=\"WEIGHT_SURCHARGE\"","name=\"confirmDemo\"");
        assertThat(post("/user/shipment-fees/42/demo-vnpay",form,Map.of("obligation","RETURN","confirmDemo","true")).statusCode()).isEqualTo(302);
        verify(fees).payReturn(42L,"VNPAY",true);
        assertThat(post("/user/shipment-fees/42/wallet",form,Map.of("obligation","WEIGHT_SURCHARGE")).statusCode()).isEqualTo(302);
        verify(fees).paySurcharge(42L,"E_WALLET",false);
    }

    @Test void populatedDeliveryAndHomeReturnFormsBindAmountsAndIndependentMethods() throws Exception {
        login("SHIPPER");
        when(delivery.getAssignedDeliveryTasks()).thenReturn(List.of(new vn.iotstar.dto.delivery.DeliveryTaskResponse(1L,42L,"UTX-42",
                "IN_PROGRESS","OUT_FOR_DELIVERY","Receiver <A>","0900000000","Street",money("100000"),money("30000"),"RECEIVER",false,false,true)));
        String form=html("/shipper/delivery-tasks");
        assertThat(form).contains("Receiver &lt;A&gt;","name=\"codAmountCollected\"","name=\"shippingFeeCollected\"","name=\"codPaymentMethod\"","name=\"shippingFeePaymentMethod\"");
        assertThat(post("/shipper/delivery-tasks/collect",form,Map.of("shipmentId","42","codAmountCollected","100000","shippingFeeCollected","30000",
                "codPaymentMethod","TRANSFER","shippingFeePaymentMethod","CASH")).statusCode()).isEqualTo(302);
        verify(delivery).recordCollection(argThat(r->"TRANSFER".equals(r.getCodPaymentMethod())&&"CASH".equals(r.getShippingFeePaymentMethod())
                &&r.getCodAmountCollected().compareTo(money("100000"))==0));
        when(delivery.getAssignedReturnTasks()).thenReturn(List.of(new vn.iotstar.dto.returnflow.ReturnTaskResponse(42L,"UTX-42","RETURN_TO_SENDER",
                "RETURN",3L,"Sender","0900000001",money("25000"),money("55000"),false,false)));
        String returning=html("/shipper/return-tasks");
        assertThat(returning).contains("UTX-42","/shipper/return-tasks/scan","/shipper/return-tasks/cash");
        assertThat(post("/shipper/return-tasks/scan",returning,Map.of("shipmentId","42")).statusCode()).isEqualTo(302);
        verify(delivery).scanReturnDelivery(42L);
    }

    @Test void populatedOverdueInventoryRendersItsArrivalAndExplicitDisposalForm() throws Exception {
        login("MANAGER");
        when(returns.disposalCandidates()).thenReturn(List.of(new vn.iotstar.service.impl.ShipmentReturnServiceImpl.ReturnDisposalCandidate(
                42L,"UTX-42",LocalDateTime.of(2026,10,1,9,0))));
        String form=html("/manager/overdue-returns");
        assertThat(form).contains("UTX-42","2026-10-01","name=\"reason\"");
        assertThat(post("/manager/overdue-returns",form,Map.of("shipmentId","42","reason","Approved overdue inventory")).statusCode()).isEqualTo(302);
        verify(returns).disposeOverdueReturn(42L,"Approved overdue inventory");
    }

    @Test void walletWithRealJavaTimeLedgerAndWithdrawalRowsRenders() throws Exception {
        login("USER");
        when(wallets.getWalletResponse(77L)).thenReturn(new vn.iotstar.dto.wallet.WalletResponse(77L,money("100000"),money("20000"),"ACTIVE"));
        when(wallets.getTransactions(77L)).thenReturn(List.of(vn.iotstar.entity.WalletTransaction.create(77L,"COD_PAYOUT",money("100000"),
                BigDecimal.ZERO,money("100000"),42L,"SHIPMENT","COD <payout>")));
        var withdrawal=vn.iotstar.entity.WithdrawalRequest.create(77L,money("20000"),"Test bank","012345","Fixture holder","fixture-key");
        withdrawal.setId(91L);
        when(withdrawals.getUserWithdrawals(77L)).thenReturn(List.of(withdrawal));
        String form=html("/user/wallet");
        assertThat(form).contains("COD_PAYOUT","Test bank",withdrawal.getRequestedAt().toLocalDate().toString(),"/user/wallet/withdraw");
    }

    @Test void administratorWithdrawalListRendersJavaTimeAndReviewForms() throws Exception {
        login("ADMIN");
        when(withdrawals.getPendingWithdrawalSummaries()).thenReturn(List.of(new vn.iotstar.dto.wallet.WithdrawalSummaryResponse(
                91L,77L,"fixture-owner",money("20000"),"Test bank","012345","Fixture holder","PENDING",null,LocalDateTime.of(2026,10,25,9,0))));
        String form=html("/admin/withdrawals");
        assertThat(form).contains("fixture-owner","2026-10-25T09:00","/admin/withdrawals/91/approve","/admin/withdrawals/91/reject");
    }

    @Test void populatedFinanceMethodsAndDetailsRenderAndSubmitSourceReferences() throws Exception {
        login("MANAGER");
        var date=java.time.LocalDate.of(2026,10,25);
        var item=new vn.iotstar.dto.finance.ShiftExpectedItemResponse(42L,"UTX-42","COD_TRANSACTION",66L,money("100000"),"COLLECTED","Recorded COD");
        when(reconciliation.getReconciliationsByShipper(88L)).thenReturn(List.of(new vn.iotstar.dto.finance.ReconciliationSummaryResponse(55L,date,
                money("100000"),money("100000"),BigDecimal.ZERO,"COMPLETED","VNPAY")));
        when(reconciliation.getReadyPayouts(88L)).thenReturn(List.of(item));
        when(finances.listPendingRefundCandidates()).thenReturn(List.of(new vn.iotstar.dto.finance.RefundCandidateResponse(12L,42L,"UTX-REFUND",money("30000"))));
        when(finances.listConfirmedLossCandidates()).thenReturn(List.of(new vn.iotstar.dto.finance.LossCompensationCandidate(7L,43L,"UTX-LOSS")));
        var header=mock(vn.iotstar.entity.ShipperReconciliation.class);
        when(header.getId()).thenReturn(55L); when(header.getStatus()).thenReturn("COMPLETED");
        when(reconciliation.reconcile(any())).thenReturn(header);
        for(String method:List.of("CASH","VNPAY","TRANSFER")) {
            when(reconciliation.getShiftExpectedItems(88L,date,method)).thenReturn(List.of(item));
            String form=html("/manager/reconciliation?shipperId=88&date=2026-10-25&method="+method);
            assertThat(form).contains("UTX-42","UTX-REFUND","UTX-LOSS","items[0].sourceId","items[0].actualAmount","/manager/reconciliation/payout");
            if(!"CASH".equals(method)) assertThat(form).contains("name=\"settlementEvidence\"");
            assertThat(post("/manager/reconciliation/submit",form,Map.of("shipperId","88","reconciliationDate","2026-10-25",
                    "collectionMethod",method,"settlementEvidence","Reviewed demo receipt","items[0].shipmentId","42",
                    "items[0].sourceType","COD_TRANSACTION","items[0].sourceId","66","items[0].actualAmount","100000")).statusCode()).isEqualTo(302);
            verify(reconciliation).reconcile(argThat(r->method.equals(r.getCollectionMethod()) && r.getItems().size()==1
                    && r.getItems().getFirst().getSourceId().equals(66L) && r.getItems().getFirst().getExpectedAmount()==null));
        }
        when(reconciliation.getReconciliationItems(55L)).thenReturn(List.of(new vn.iotstar.dto.finance.ReconciliationItemResponse(42L,"UTX-42",
                "COD_TRANSACTION",66L,money("100000"),money("90000"),money("-10000"),"SHORTAGE")));
        assertThat(html("/manager/reconciliation/detail?reconciliationId=55")).contains("UTX-42","SHORTAGE","-10000");
    }

    BigDecimal money(String amount) { return new BigDecimal(amount); }

    void login(String role) throws Exception {
        when(users.loadUserByUsername("render-test")).thenReturn(new vn.iotstar.entity.User(77L,"render-test",
                passwords.encode("fixture-password"),vn.iotstar.entity.UserRole.valueOf("ROLE_"+role),"ACTIVE"));
        assertThat(post("/login", html("/login"), Map.of("username", "render-test", "password", "fixture-password"))
                .statusCode()).isEqualTo(302);
    }
    String html(String path) throws Exception {
        var response = browser.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(30)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).as("GET %s body %s", path, response.body()).isEqualTo(200);
        assertThat(response.body()).contains("<!DOCTYPE html>").doesNotContain("PropertyNotFoundException", "<%@");
        return response.body();
    }
    HttpResponse<String> post(String path, String html, Map<String, String> fields) throws Exception {
        var matcher = Pattern.compile("<input[^>]*name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertThat(matcher.find()).as("real CSRF token").isTrue();
        var body = new LinkedHashMap<>(fields);
        body.put("_csrf", matcher.group(1));
        String encoded = body.entrySet().stream().map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
        return browser.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encoded)).build(), HttpResponse.BodyHandlers.ofString());
    }
    URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }

    @TestComponent @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class})
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, HomeController.class, AuthenticationViewAdvice.class,
            ManagerOperationsController.class, DriverOperationsController.class, MissingParcelController.class,
            HubOperationsController.class, OperationsAuditController.class, LastMileAssignmentController.class,
            ShipmentFeeController.class, LastMileDeliveryController.class, ReturnDisposalController.class,
            WalletController.class, AdminWithdrawalController.class, FinanceReconciliationController.class})
    static class RenderingApplication {}
}
