package vn.iotstar.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.HubRoutingRule;
import vn.iotstar.entity.User;
import vn.iotstar.entity.UserRole;
import vn.iotstar.repository.HubRepository;
import vn.iotstar.repository.HubRoutingRuleRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminNetworkServiceTest {
    private final HubRepository hubs = mock(HubRepository.class);
    private final HubRoutingRuleRepository rules = mock(HubRoutingRuleRepository.class);
    private final AdminNetworkService service = new AdminNetworkService(hubs, rules);
    private final Hub soc = hub(1L, "SOC-1", 3, "SOC");
    private final Hub region = hub(2L, "REG-S", 2, "REGIONAL_HUB");
    private final Hub delivery = hub(3L, "DEL-1", 1, "DELIVERY_HUB");

    @BeforeEach void setUp() {
        login(UserRole.ROLE_ADMIN);
        for (Hub h : List.of(soc, region, delivery)) when(hubs.findById(h.getId())).thenReturn(Optional.of(h));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void createsDeliveryHubUnderARegionalParent() {
        service.createHub("DEL-2", "Bưu cục mới", "DELIVERY_HUB", "SOUTH", "TP HCM", null, null, 2L);
        verify(hubs).save(org.mockito.ArgumentMatchers.argThat(h -> h.getLevel() == 1 && h.getParentHub() == region));
    }

    @Test void parentMustBeExactlyOneLevelUpSoNoCycleOrSelfParent() {
        assertThatThrownBy(() -> service.createHub("DEL-2", "X", "DELIVERY_HUB", "SOUTH", "P", null, null, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.editHub(2L, "Reg", "P", null, null, 2L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.editHub(1L, "Soc", "P", null, null, 2L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createHub("bad code", "X", "SOC", "SOUTH", "P", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        when(hubs.existsByCodeIgnoreCase("DEL-1")).thenReturn(true);
        assertThatThrownBy(() -> service.createHub("DEL-1", "X", "DELIVERY_HUB", "SOUTH", "P", null, null, 2L))
                .isInstanceOf(IllegalArgumentException.class);
        verify(hubs, never()).save(any());
    }

    @Test void deactivationIsBlockedWhileAnythingDependsOnTheHub() {
        when(rules.hubDependencies(3L)).thenReturn(List.<Object[]>of(new Object[]{0L, 2L, 0L, 1L, 0L, 0L, 0L}));
        assertThatThrownBy(() -> service.setHubActive(3L, false)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 nhân viên").hasMessageContaining("1 đơn");
        assertThat(delivery.getStatus()).isEqualTo("ACTIVE");
        when(rules.hubDependencies(3L)).thenReturn(List.<Object[]>of(new Object[]{0L, 0L, 0L, 0L, 0L, 0L, 0L}));
        service.setHubActive(3L, false);
        assertThat(delivery.getStatus()).isEqualTo("INACTIVE");
    }

    @Test void ruleNeedsDistinctActiveHubsAndNoDuplicateKey() {
        assertThatThrownBy(() -> service.addRule(3L, "SOUTH", null, 3L)).isInstanceOf(IllegalArgumentException.class);
        when(rules.countActiveSameKey(3L, "NORTH", null)).thenReturn(1L);
        assertThatThrownBy(() -> service.addRule(3L, "NORTH", " ", 2L)).isInstanceOf(IllegalArgumentException.class);
        region.changeStatus("INACTIVE");
        assertThatThrownBy(() -> service.addRule(3L, "CENTRAL", null, 2L)).isInstanceOf(IllegalArgumentException.class);
        region.changeStatus("ACTIVE");
        service.addRule(3L, "CENTRAL", null, 2L);
        verify(rules).save(any(HubRoutingRule.class));
    }

    @Test void otherRolesAreDenied() {
        login(UserRole.ROLE_MANAGER);
        assertThatThrownBy(service::hubs).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.setHubActive(3L, false)).isInstanceOf(AccessDeniedException.class);
    }

    private static Hub hub(Long id, String code, int level, String type) {
        Hub hub = Hub.active(code, code, level, type, "SOUTH", "P");
        ReflectionTestUtils.setField(hub, "id", id);
        return hub;
    }

    private static void login(UserRole role) {
        User actor = new User(1L, "actor", "x", role, "ACTIVE");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, null, actor.getAuthorities()));
    }
}
