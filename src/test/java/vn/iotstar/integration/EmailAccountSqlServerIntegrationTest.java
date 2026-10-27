package vn.iotstar.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import vn.iotstar.dto.auth.RegisterUserRequest;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.service.auth.AccountEmailSender;
import vn.iotstar.service.impl.EmailAccountService;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIf("configured")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = "uteexpress.mail.enabled=true")
@ActiveProfiles("sqlserver-verification")
@Sql(scripts = "classpath:db/migration/V20__email_otp.sql", config = @SqlConfig(separator = ScriptUtils.EOF_STATEMENT_SEPARATOR))
class EmailAccountSqlServerIntegrationTest {
    @Autowired EmailAccountService accounts;
    @Autowired JdbcTemplate jdbc;
    @MockBean AccountEmailSender mail;
    private RegisterUserRequest form;
    @BeforeEach void identity() {
        form = new RegisterUserRequest(); String suffix = UUID.randomUUID().toString().replace("-", "");
        form.setUsername("email-it-" + suffix); form.setEmail("email-it-" + suffix + "@example.test");
        form.setFullName("Kiểm thử email"); form.setPassword("IntegrationOnly-123!");
        String phone;
        do { phone = "+849" + String.format(java.util.Locale.ROOT, "%08d", ThreadLocalRandom.current().nextInt(100_000_000)); }
        while (jdbc.queryForObject("select count(*) from users where phone = ?", Integer.class, phone) > 0);
        form.setPhone(phone);
    }
    @AfterEach void removeOnlyThisIdentity() {
        if (form == null) return;
        jdbc.update("delete from email_otp_challenges where user_id in (select id from users where username = ? and email = ?)", form.getUsername(), form.getEmail());
        jdbc.update("delete from users where username = ? and email = ?", form.getUsername(), form.getEmail());
    }
    @Test void failedAttemptsPersistAndEmailActivationDoesNotVerifyPhone() {
        AtomicReference<String> code = new AtomicReference<>();
        doAnswer(call -> { code.set(call.getArgument(1)); return null; }).when(mail).sendOtp(anyString(), anyString(), any());
        var challenge = accounts.register(form());
        String wrong = "000000".equals(code.get()) ? "000001" : "000000";
        assertThat(accounts.verify(challenge.id(), wrong, "REGISTRATION", null, null)).isFalse();
        assertThat(jdbc.queryForObject("select failed_attempts from email_otp_challenges where id = ?", Integer.class, challenge.id().toString())).isEqualTo(1);
        assertThat(accounts.verify(challenge.id(), code.get(), "REGISTRATION", null, null)).isTrue();
        var account = jdbc.queryForMap("select status, email_verified_at, phone_verified_at from users where username = ?", form.getUsername());
        assertThat(account.get("status")).isEqualTo("ACTIVE"); assertThat(account.get("email_verified_at")).isNotNull();
        assertThat(account.get("phone_verified_at")).isNull();
        assertThat(accounts.verify(challenge.id(), code.get(), "REGISTRATION", null, null)).isFalse();
    }
    @Test void smtpFailureRollsBackBothNewAccountAndChallenge() {
        doThrow(new RegistrationException("Simulated delivery failure")).when(mail).sendOtp(anyString(), anyString(), any());
        assertThatThrownBy(() -> accounts.register(form)).isInstanceOf(RegistrationException.class);
        assertThat(jdbc.queryForObject("select count(*) from users where username = ?", Integer.class, form.getUsername())).isZero();
    }
    private RegisterUserRequest form() { return form; }
    static boolean configured() {
        return java.util.stream.Stream.of("UTEEXPRESS_SQLSERVER_URL", "UTEEXPRESS_SQLSERVER_USERNAME", "UTEEXPRESS_SQLSERVER_PASSWORD")
                .allMatch(name -> System.getenv(name) != null && !System.getenv(name).isBlank());
    }
}
