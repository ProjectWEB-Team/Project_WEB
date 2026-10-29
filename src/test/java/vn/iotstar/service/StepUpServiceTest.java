package vn.iotstar.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import vn.iotstar.config.OtpProperties;
import vn.iotstar.entity.User;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.service.auth.AccountEmailSender;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class StepUpServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-29T08:00:00Z");
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final AccountEmailSender sender = mock(AccountEmailSender.class);
    private StepUpService service;
    private MockHttpSession session;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        ObjectProvider<AccountEmailSender> mail = mock(ObjectProvider.class);
        when(mail.getIfAvailable()).thenReturn(sender);
        service = new StepUpService(encoder, mail, Clock.fixed(NOW, ZoneOffset.UTC), new OtpProperties());
        session = new MockHttpSession();
    }

    private User local(boolean phoneVerified, boolean emailVerified) {
        User u = User.registerPending("alice", encoder.encode("Password1!"), "Alice", "+84901234567", "a@example.com", NOW);
        if (phoneVerified) u.verifyPhone(NOW);
        if (emailVerified) u.verifyEmail(NOW);
        return u;
    }

    @Test
    void wrongPasswordIsRejectedAndLocksAfterFiveTries() {
        User u = local(true, false);
        for (int i = 0; i < 5; i++) assertThatThrownBy(() -> service.start(session, u, "nope")).hasMessageContaining("không chính xác");
        assertThatThrownBy(() -> service.start(session, u, "Password1!")).hasMessageContaining("quá nhiều");
        assertThat(service.isUnlocked(session)).isFalse();
    }

    @Test
    void oneLinkedChannelNeedsOnlyThePassword() {
        User u = local(true, false);
        assertThat(service.start(session, u, "Password1!")).isFalse();
        assertThat(service.isUnlocked(session)).isTrue();
        verifyNoInteractions(sender);
    }

    @Test
    void phoneAndEmailBothVerifiedNeedPasswordThenEmailCode() {
        User u = local(true, true);
        assertThat(service.start(session, u, "Password1!")).isTrue();
        assertThat(service.isUnlocked(session)).isFalse();
        var code = forClass(String.class);
        verify(sender).sendOtp(eq("a@example.com"), code.capture(), any());
        assertThat(service.verify(session, "000000".equals(code.getValue()) ? "111111" : "000000")).isFalse();
        assertThat(service.verify(session, code.getValue())).isTrue();
        assertThat(service.isUnlocked(session)).isTrue();
    }

    @Test
    void googleAccountNeedsNoPasswordButAlwaysAnEmailCode() {
        User g = User.googleAccount("g_1", "x", "G", null, "g@example.com", "sub", NOW);
        assertThat(service.needsPassword(g)).isFalse();
        assertThat(service.start(session, g, "")).isTrue();
        verify(sender).sendOtp(eq("g@example.com"), any(), any());
    }

    @Test
    void resendWithoutAPendingCodeFails() {
        assertThatThrownBy(() -> service.resend(session, local(true, true))).isInstanceOf(RegistrationException.class);
    }
}
