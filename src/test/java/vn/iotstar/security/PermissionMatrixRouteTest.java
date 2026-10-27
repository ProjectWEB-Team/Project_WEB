package vn.iotstar.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import vn.iotstar.config.SecurityConfig;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * HARD-01 route matrix (D-01, D-03): every web route x six roles + anonymous x CSRF against the real
 * SecurityConfig chain. A catch-all NoopController answers 200 for every path, so the status alone shows
 * the chain's decision: 200 allowed, 403 denied role or missing CSRF, 3xx to /login anonymous.
 * Expected roles are authored from the documented policy, never computed from SecurityConfig.
 */
@WebMvcTest(PermissionMatrixRouteTest.NoopController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, PermissionMatrixRouteTest.NoopController.class})
class PermissionMatrixRouteTest {
    static final List<String> ROLES = List.of("USER", "SHIPPER", "HUB_STAFF", "LINEHAUL_DRIVER", "MANAGER", "ADMIN");
    static final String ANONYMOUS = "ANONYMOUS";
    static final Set<String> PUBLIC = Set.of("PUBLIC");
    static final Set<String> ALL = Set.copyOf(ROLES);
    static final Set<String> ADMIN = Set.of("ADMIN");
    static final Set<String> MANAGER = Set.of("ADMIN", "MANAGER");
    static final Set<String> HUB = Set.of("ADMIN", "HUB_STAFF");
    static final Set<String> DRIVER = Set.of("ADMIN", "LINEHAUL_DRIVER");
    static final Set<String> SHIPPER = Set.of("ADMIN", "SHIPPER");

    /** A route row; filterHandled rows are served by Spring Security filters, not controllers. */
    record Route(String method, String template, Set<String> roles, boolean filterHandled, boolean controllerRoute) {
        Route(String method, String template, Set<String> roles) { this(method, template, roles, false, true); }
        String key() { return method + " " + template; }
        String path() { return template.replaceAll("\\{[^}]+}", "1"); }
    }

    static final List<Route> ROUTES = List.of(
            // Public pages and onboarding.
            new Route("GET", "/", PUBLIC),
            new Route("GET", "/home", PUBLIC),
            new Route("GET", "/login", PUBLIC),
            new Route("GET", "/register", PUBLIC),
            new Route("POST", "/register", PUBLIC),
            new Route("GET", "/auth/phone-login", PUBLIC),
            new Route("POST", "/auth/phone-login", PUBLIC),
            new Route("GET", "/auth/verify-phone", PUBLIC),
            new Route("POST", "/auth/verify-phone", PUBLIC),
            new Route("POST", "/auth/verify-phone/resend", PUBLIC),
            new Route("GET", "/auth/forgot-password", PUBLIC),
            new Route("POST", "/auth/forgot-password", PUBLIC),
            new Route("GET", "/auth/reset-password", PUBLIC),
            new Route("POST", "/auth/reset-password", PUBLIC),
            new Route("GET", "/auth/google/complete", PUBLIC),
            new Route("POST", "/auth/google/complete", PUBLIC),
            new Route("GET", "/tracking", PUBLIC),
            // Every authenticated role; the service layer decides ownership (ADR-015).
            new Route("GET", "/user/shipments", ALL),
            new Route("GET", "/user/shipments/create", ALL),
            new Route("GET", "/user/shipments/address-catalog", ALL),
            new Route("POST", "/user/shipments/quote", ALL),
            new Route("POST", "/user/shipments", ALL),
            new Route("GET", "/user/shipments/{shipmentId}", ALL),
            new Route("POST", "/user/shipments/{shipmentId}/confirm-payment", ALL),
            new Route("POST", "/user/shipments/{shipmentId}/cancel", ALL),
            new Route("GET", "/user/incoming", ALL),
            new Route("GET", "/user/incoming/{shipmentId}", ALL),
            new Route("POST", "/user/incoming/{shipmentId}/unlock", ALL),
            new Route("GET", "/user/shipment-fees/{shipmentId}", ALL),
            new Route("POST", "/user/shipment-fees/{shipmentId}/wallet", ALL),
            new Route("POST", "/user/shipment-fees/{shipmentId}/demo-vnpay", ALL),
            new Route("GET", "/user/wallet", ALL),
            new Route("POST", "/user/wallet/withdraw", ALL),
            // Administrator only.
            new Route("GET", "/admin/withdrawals", ADMIN),
            new Route("GET", "/admin/users", ADMIN),
            new Route("POST", "/admin/users", ADMIN),
            new Route("POST", "/admin/users/{userId}/lock", ADMIN),
            new Route("POST", "/admin/users/{userId}/assign", ADMIN),
            new Route("POST", "/admin/withdrawals/{requestId}/approve", ADMIN),
            new Route("POST", "/admin/withdrawals/{requestId}/reject", ADMIN),
            new Route("GET", "/admin/overdue-returns", ADMIN),
            new Route("POST", "/admin/overdue-returns", ADMIN),
            // Manager workspace.
            new Route("GET", "/manager/operations", MANAGER),
            new Route("GET", "/manager/operations/work/{kind}/{id}", MANAGER),
            new Route("GET", "/manager/reconciliation", MANAGER),
            new Route("GET", "/manager/reconciliation/detail", MANAGER),
            new Route("POST", "/manager/reconciliation/refund", MANAGER),
            new Route("POST", "/manager/reconciliation/compensate", MANAGER),
            new Route("POST", "/manager/reconciliation/submit", MANAGER),
            new Route("POST", "/manager/reconciliation/payout", MANAGER),
            new Route("GET", "/manager/incidents/missing", MANAGER),
            new Route("GET", "/manager/incidents/missing/{incidentId}", MANAGER),
            new Route("POST", "/manager/incidents/missing/resolve", MANAGER),
            new Route("GET", "/manager/audit/shipments/{shipmentId}", MANAGER),
            new Route("GET", "/manager/overdue-returns", MANAGER),
            new Route("POST", "/manager/overdue-returns", MANAGER),
            // Hub staff workspace.
            new Route("GET", "/hub/sorter", HUB),
            new Route("POST", "/hub/inbound", HUB),
            new Route("POST", "/hub/sorter", HUB),
            new Route("GET", "/hub/pickups", HUB),
            new Route("POST", "/hub/pickups/{shipmentId}/assign", HUB),
            new Route("POST", "/hub/pickups/{shipmentId}/service-failure", HUB),
            new Route("GET", "/hub/inbound", HUB),
            new Route("POST", "/hub/inbound/receive", HUB),
            new Route("POST", "/hub/inbound/reconcile", HUB),
            new Route("POST", "/hub/inbound/reconcile-empty", HUB),
            new Route("GET", "/hub/delivery-assignments", HUB),
            new Route("POST", "/hub/delivery-assignments", HUB),
            new Route("POST", "/hub/delivery-returns/receive", HUB),
            new Route("GET", "/hub/return-collection", HUB),
            new Route("POST", "/hub/return-collection", HUB),
            new Route("POST", "/hub/return-collection/cash", HUB),
            new Route("GET", "/hub/outbound", HUB),
            new Route("POST", "/hub/outbound/containers", HUB),
            new Route("POST", "/hub/outbound/load", HUB),
            new Route("POST", "/hub/outbound/handovers", HUB),
            new Route("POST", "/hub/outbound/handovers/correct", HUB),
            new Route("GET", "/hub/audit/shipments/{shipmentId}", HUB),
            // Linehaul driver workspace.
            new Route("GET", "/driver", DRIVER),
            new Route("POST", "/driver/handovers/accept", DRIVER),
            new Route("POST", "/driver/handovers/scan", DRIVER),
            new Route("POST", "/driver/trips/start", DRIVER),
            new Route("POST", "/driver/trips/arrive", DRIVER),
            new Route("POST", "/driver/trips/delay", DRIVER),
            new Route("GET", "/driver/audit/shipments/{shipmentId}", DRIVER),
            // Shipper workspace.
            new Route("GET", "/shipper/tasks", SHIPPER),
            new Route("POST", "/shipper/tasks/{assignmentId}/attempt", SHIPPER),
            new Route("POST", "/shipper/tasks/{assignmentId}/complete", SHIPPER),
            new Route("GET", "/shipper/delivery-tasks", SHIPPER),
            new Route("POST", "/shipper/delivery-tasks/scan", SHIPPER),
            new Route("POST", "/shipper/delivery-tasks/collect", SHIPPER),
            new Route("POST", "/shipper/delivery-tasks/confirm", SHIPPER),
            new Route("POST", "/shipper/delivery-tasks/failure", SHIPPER),
            new Route("GET", "/shipper/return-tasks", SHIPPER),
            new Route("POST", "/shipper/return-tasks/scan", SHIPPER),
            new Route("POST", "/shipper/return-tasks/confirm", SHIPPER),
            new Route("POST", "/shipper/return-tasks/cash", SHIPPER),
            // Served by Spring Security filters; covered by LoginRouteSecurityTest,
            // GoogleOAuthRouteSecurityTest and WebSocketEndpointSecurityTest.
            new Route("POST", "/login", PUBLIC, true, false),
            new Route("POST", "/logout", PUBLIC, true, false),
            new Route("GET", "/ws", ALL, true, false),
            new Route("GET", "/oauth2/authorization/google", PUBLIC, true, false),
            new Route("GET", "/login/oauth2/code/google", PUBLIC, true, false),
            // Default deny for anonymous callers on a path SecurityConfig does not list.
            new Route("GET", "/internal/unlisted", ALL, false, false),
            new Route("POST", "/internal/unlisted", ALL, false, false));

    @Autowired private MockMvc mockMvc;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    static Stream<Arguments> cells() {
        List<String> principals = new ArrayList<>(ROLES);
        principals.add(ANONYMOUS);
        return ROUTES.stream().filter(route -> !route.filterHandled())
                .flatMap(route -> principals.stream().map(principal -> Arguments.of(route, principal)));
    }

    @ParameterizedTest(name = "{0} as {1}")
    @MethodSource("cells")
    void eachRoleGetsTheDocumentedOutcome(Route route, String principal) throws Exception {
        MockHttpServletRequestBuilder request = as(request(HttpMethod.valueOf(route.method()), route.path()), principal);
        if ("POST".equals(route.method())) request.with(csrf());
        MvcResult result = mockMvc.perform(request).andReturn();
        assertOutcome(route, principal, result);
    }

    static Stream<Arguments> postCells() {
        return cells().filter(arguments -> "POST".equals(((Route) arguments.get()[0]).method()));
    }

    @ParameterizedTest(name = "{0} without CSRF as {1}")
    @MethodSource("postCells")
    void everyWebPostWithoutACsrfTokenIsForbidden(Route route, String principal) throws Exception {
        int status = mockMvc.perform(as(request(HttpMethod.valueOf(route.method()), route.path()), principal)).andReturn().getResponse().getStatus();
        assertThat(status).as(route.key() + " as " + principal).isEqualTo(403);
    }

    @Test
    void everyControllerRouteHasAMatrixRow() throws Exception {
        Set<String> documented = Stream.concat(ROUTES.stream(), PermissionMatrixApiChainTest.ROUTES.stream()
                        .map(r -> new Route(r.method(), r.template(), r.roles(), false, r.controllerRoute())))
                .filter(Route::controllerRoute).map(Route::key).collect(Collectors.toCollection(TreeSet::new));
        assertThat(controllerRoutes()).isEqualTo(documented);
    }

    /** METHOD template for every handler in vn.iotstar.controller, class and method mappings merged. */
    static Set<String> controllerRoutes() throws ClassNotFoundException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        Set<String> routes = new TreeSet<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("vn.iotstar.controller")) {
            Class<?> type = ClassUtils.forName(definition.getBeanClassName(), PermissionMatrixRouteTest.class.getClassLoader());
            RequestMapping typeMapping = AnnotatedElementUtils.findMergedAnnotation(type, RequestMapping.class);
            String[] prefixes = typeMapping == null || typeMapping.path().length == 0 ? new String[]{""} : typeMapping.path();
            for (Method method : type.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) continue;
                String[] paths = mapping.path().length == 0 ? new String[]{""} : mapping.path();
                for (RequestMethod verb : mapping.method()) {
                    for (String prefix : prefixes) {
                        for (String path : paths) {
                            String template = prefix + path;
                            routes.add(verb.name() + " " + (template.isEmpty() ? "/" : template));
                        }
                    }
                }
            }
        }
        return routes;
    }

    private static MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, String principal) {
        return ANONYMOUS.equals(principal) ? request : request.with(user("matrix").roles(principal));
    }

    private static void assertOutcome(Route route, String principal, MvcResult result) {
        int status = result.getResponse().getStatus();
        String cell = route.key() + " as " + principal;
        if (route.roles().equals(PUBLIC) || route.roles().contains(principal)) {
            assertThat(status).as(cell).isEqualTo(200);
        } else if (ANONYMOUS.equals(principal)) {
            assertThat(status).as(cell).isEqualTo(302);
            assertThat(result.getResponse().getRedirectedUrl()).as(cell).endsWith("/login");
        } else {
            assertThat(status).as(cell).isEqualTo(403);
        }
    }

    @RestController
    static class NoopController {
        @RequestMapping("/**")
        String ok() { return "ok"; }
    }
}
