package vn.iotstar.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.entity.PricingPolicy;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.repository.PricingPolicyRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminPricingServiceTest {
    private final PricingPolicyRepository policies = mock(PricingPolicyRepository.class);
    private final AdminPricingService service = new AdminPricingService(policies);

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void adminRepricesAndDeactivatesARow() {
        login(UserRole.ROLE_ADMIN);
        PricingPolicy policy = org.springframework.beans.BeanUtils.instantiateClass(PricingPolicy.class);
        when(policies.findById(7L)).thenReturn(Optional.of(policy));
        service.update(7L, new BigDecimal("25000"), new BigDecimal("4000"), new BigDecimal("0.5"), new BigDecimal("0.8"), false);
        assertThat(policy.getBasePrice()).isEqualByComparingTo("25000");
        assertThat(policy.getStatus()).isEqualTo("INACTIVE");
    }

    @Test void rejectsNegativePriceOverHundredPercentAndNonAdmins() {
        login(UserRole.ROLE_ADMIN);
        assertThatThrownBy(() -> service.update(7L, new BigDecimal("-1"), BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.update(7L, BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("101"), BigDecimal.ZERO, true))
                .isInstanceOf(IllegalArgumentException.class);
        login(UserRole.ROLE_MANAGER);
        assertThatThrownBy(service::all).isInstanceOf(AccessDeniedException.class);
    }

    private static void login(UserRole role) {
        User actor = new User(1L, "actor", "x", role, "ACTIVE");
        ReflectionTestUtils.setField(actor, "id", 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, null, actor.getAuthorities()));
    }
}
