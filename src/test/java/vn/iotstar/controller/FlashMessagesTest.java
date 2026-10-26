package vn.iotstar.controller;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class FlashMessagesTest {
    @Test
    void businessRuleMessagesAreShownAndInfrastructureTextIsHidden() {
        assertThat(FlashMessages.of(new IllegalArgumentException("Minimum withdrawal"))).isEqualTo("Minimum withdrawal");
        assertThat(FlashMessages.of(new IllegalStateException("Insufficient balance"))).isEqualTo("Insufficient balance");
        assertThat(FlashMessages.of(new AccessDeniedException("Not assigned"))).isEqualTo("Not assigned");
        assertThat(FlashMessages.of(new DataIntegrityViolationException("Violation of UNIQUE KEY constraint"))).isEqualTo(FlashMessages.GENERIC);
        assertThat(FlashMessages.of(new RuntimeException("jdbc details"))).isEqualTo(FlashMessages.GENERIC);
    }
}
