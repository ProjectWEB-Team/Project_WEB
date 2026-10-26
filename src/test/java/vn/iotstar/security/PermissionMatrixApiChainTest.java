package vn.iotstar.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.iotstar.config.SecurityConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static vn.iotstar.security.PermissionMatrixRouteTest.ALL;
import static vn.iotstar.security.PermissionMatrixRouteTest.ANONYMOUS;
import static vn.iotstar.security.PermissionMatrixRouteTest.DRIVER;
import static vn.iotstar.security.PermissionMatrixRouteTest.HUB;
import static vn.iotstar.security.PermissionMatrixRouteTest.PUBLIC;
import static vn.iotstar.security.PermissionMatrixRouteTest.ROLES;
import static vn.iotstar.security.PermissionMatrixRouteTest.SHIPPER;

/**
 * HARD-01 matrix for the stateless /api/** JWT chain (D-03): anonymous gets a 401 JSON error, a wrong role a
 * 403 JSON error, an allowed role 200, and POST needs no CSRF token on this chain.
 */
@WebMvcTest(PermissionMatrixApiChainTest.NoopApiController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, PermissionMatrixApiChainTest.NoopApiController.class})
class PermissionMatrixApiChainTest {

    record Route(String method, String template, Set<String> roles, boolean controllerRoute) {
        String key() { return method + " " + template; }
        String path() { return template.replaceAll("\\{[^}]+}", "1"); }
    }

    static final List<Route> ROUTES = List.of(
            new Route("POST", "/api/driver/trips/{tripId}/delay", DRIVER, true),
            new Route("POST", "/api/auth/login", PUBLIC, true),
            new Route("GET", "/api/security/actor", ALL, true),
            new Route("GET", "/api/tracking/{trackingNumber}", PUBLIC, true),
            // Prefix rules without a controller today; probes prove the rule itself.
            new Route("GET", "/api/shipper/probe", SHIPPER, false),
            new Route("GET", "/api/hub/probe", HUB, false),
            new Route("GET", "/api/calculator/probe", ALL, false),
            new Route("GET", "/api/unlisted", ALL, false));

    @Autowired private MockMvc mockMvc;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    static Stream<Arguments> cells() {
        List<String> principals = new ArrayList<>(ROLES);
        principals.add(ANONYMOUS);
        return ROUTES.stream().flatMap(route -> principals.stream().map(principal -> Arguments.of(route, principal)));
    }

    @ParameterizedTest(name = "{0} as {1}")
    @MethodSource("cells")
    void eachRoleGetsTheDocumentedJsonOutcomeWithoutCsrf(Route route, String principal) throws Exception {
        MockHttpServletRequestBuilder request = request(HttpMethod.valueOf(route.method()), route.path());
        if (!ANONYMOUS.equals(principal)) request.with(user("matrix").roles(principal));
        MvcResult result = mockMvc.perform(request).andReturn();
        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();
        String cell = route.key() + " as " + principal;
        if (route.roles().equals(PUBLIC) || route.roles().contains(principal)) {
            assertThat(status).as(cell).isEqualTo(200);
        } else if (ANONYMOUS.equals(principal)) {
            assertThat(status).as(cell).isEqualTo(401);
            assertThat(body).as(cell).isEqualTo("{\"error\":\"Authentication required\"}");
        } else {
            assertThat(status).as(cell).isEqualTo(403);
            assertThat(body).as(cell).isEqualTo("{\"error\":\"Access denied\"}");
        }
    }

    @RestController
    static class NoopApiController {
        @RequestMapping("/**")
        String ok() { return "ok"; }
    }
}
