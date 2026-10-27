package vn.iotstar.service.auth;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import vn.iotstar.exception.RegistrationException;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountEmailSenderTest {
    private final JavaMailSender transport = mock(JavaMailSender.class);
    private final AccountEmailSender sender = new AccountEmailSender(transport, "sender@example.test");
    @Test void messageUsesConfiguredSenderAndHasNoOtherRecipients() {
        Instant expiry = Instant.parse("2026-10-28T00:05:00Z");
        sender.sendOtp("owner@example.test", "123456", expiry);
        var capture = ArgumentCaptor.forClass(SimpleMailMessage.class); verify(transport).send(capture.capture());
        assertThat(capture.getValue().getFrom()).isEqualTo("sender@example.test");
        assertThat(capture.getValue().getTo()).containsExactly("owner@example.test");
        assertThat(capture.getValue().getCc()).isNull(); assertThat(capture.getValue().getBcc()).isNull();
        assertThat(capture.getValue().getText()).contains("123456", expiry.toString(), "một lần");
    }
    @Test void smtpFailureIsSafeForThePublicForm() {
        doThrow(new MailAuthenticationException("password=private-value")).when(transport).send(any(SimpleMailMessage.class));
        assertThatThrownBy(() -> sender.sendOtp("owner@example.test", "123456", Instant.now()))
                .isInstanceOf(RegistrationException.class).hasMessageNotContaining("private-value").hasMessageNotContaining("123456");
    }
}
