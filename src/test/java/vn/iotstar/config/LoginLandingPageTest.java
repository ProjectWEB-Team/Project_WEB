package vn.iotstar.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.authentication.TestingAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;

class LoginLandingPageTest {
    @ParameterizedTest
    @CsvSource({"ROLE_USER,/user/shipments/create", "ROLE_SHIPPER,/shipper/tasks", "ROLE_HUB_STAFF,/hub/sorter",
            "ROLE_LINEHAUL_DRIVER,/driver", "ROLE_MANAGER,/manager/operations", "ROLE_ADMIN,/manager/operations"})
    void eachRoleLandsOnItsOwnWorkspace(String role, String page) {
        assertThat(SecurityConfig.landingPage(new TestingAuthenticationToken("u", null, role))).isEqualTo(page);
    }
}
