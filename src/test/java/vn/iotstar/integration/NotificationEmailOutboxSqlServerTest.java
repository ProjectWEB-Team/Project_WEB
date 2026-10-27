package vn.iotstar.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.iotstar.service.auth.*;
import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "UTEEXPRESS_CI_DATABASE", matches = "true")
@SpringBootTest(properties = "uteexpress.mail.enabled=true")
@ActiveProfiles("sqlserver-verification")
class NotificationEmailOutboxSqlServerTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource source;
    @Autowired AccountEmailNotificationListener outbox;
    @Autowired NotificationEmailWorker worker;
    @Autowired PlatformTransactionManager transactions;
    @MockBean AccountEmailSender mail;
    String username;
    long userId;
    @BeforeEach void identity() throws Exception {
        try (var connection = source.getConnection()) { SqlServerMigrationSupport.requireDisposableDatabase(connection); }
        username = "outbox-" + UUID.randomUUID().toString().replace("-", "");
        String phone = "+849" + String.format(java.util.Locale.ROOT, "%08d", java.util.concurrent.ThreadLocalRandom.current().nextInt(100_000_000));
        jdbc.update("""
                INSERT INTO users(username,password,full_name,phone,email,role,status,email_verified_at)
                VALUES(?, 'test-hash',N'Outbox test',?,?,'ROLE_USER','ACTIVE',SYSUTCDATETIME())
                """, username, phone, username + "@example.test");
        userId = jdbc.queryForObject("SELECT id FROM users WHERE username=?", Long.class, username);
    }
    @AfterEach void cleanup() {
        if (username == null) return;
        jdbc.update("DELETE FROM notification_email_outbox WHERE user_id=?", userId);
        jdbc.update("DELETE FROM users WHERE id=? AND username=?", userId, username);
    }
    private void enqueue(boolean rollback) {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            outbox.notify(new AccountEmailNotification(username, "Update", "Shipment updated"));
            if (rollback) status.setRollbackOnly();
        });
    }
    @Test void obligationRollsBackWithBusinessTransactionAndNeverSendsInline() {
        enqueue(true);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification_email_outbox WHERE user_id=?", Integer.class, userId)).isZero();
        enqueue(false);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification_email_outbox WHERE user_id=?", Integer.class, userId)).isEqualTo(1);
        verifyNoInteractions(mail);
    }
    @Test void retriesTransientFailureAndClearsPayloadOnSuccess() {
        enqueue(false); var first = worker.claim(); assertThat(first).isNotNull();
        doThrow(new MailSendException("private provider detail")).when(mail).send(anyString(), anyString(), anyString());
        worker.deliver(first);
        assertThat(jdbc.queryForObject("SELECT status FROM notification_email_outbox WHERE id=?", String.class, first.id())).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT last_error FROM notification_email_outbox WHERE id=?", String.class, first.id())).isEqualTo("MailSendException");
        assertThat(worker.claim()).isNull();
        jdbc.update("UPDATE notification_email_outbox SET available_at=DATEADD(second,-1,SYSUTCDATETIME()) WHERE id=?", first.id());
        var second = worker.claim(); assertThat(second.attempts()).isEqualTo(2);
        doNothing().when(mail).send(anyString(), anyString(), anyString());
        worker.deliver(second);
        var row = jdbc.queryForMap("SELECT status,email,body FROM notification_email_outbox WHERE id=?", first.id());
        assertThat(row).containsEntry("status", "SENT").containsEntry("email", "").containsEntry("body", "");
    }
    @Test void concurrentWorkersCannotClaimTheSameRowAndStaleWorkerCannotSend() throws Exception {
        enqueue(false);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var a = workers.submit(worker::claim); var b = workers.submit(worker::claim);
            var first = a.get(); var second = b.get();
            assertThat((first == null ? 0 : 1) + (second == null ? 0 : 1)).isEqualTo(1);
            var old = first == null ? second : first;
            jdbc.update("UPDATE notification_email_outbox SET available_at=DATEADD(second,-1,SYSUTCDATETIME()) WHERE id=?", old.id());
            var reclaimed = worker.claim(); assertThat(reclaimed.lease()).isNotEqualTo(old.lease());
            worker.deliver(old); verifyNoInteractions(mail);
            worker.deliver(reclaimed); verify(mail).send(anyString(), anyString(), anyString());
        }
    }
    @Test void lockedRecipientIsCancelledAndFinalFailureIsRetainedForOperations() {
        enqueue(false); var cancelled = worker.claim();
        jdbc.update("UPDATE users SET status='LOCKED' WHERE id=?", userId);
        worker.deliver(cancelled); verifyNoInteractions(mail);
        assertThat(jdbc.queryForObject("SELECT status FROM notification_email_outbox WHERE id=?", String.class, cancelled.id())).isEqualTo("CANCELLED");
        jdbc.update("UPDATE users SET status='ACTIVE' WHERE id=?", userId);
        enqueue(false);
        jdbc.update("UPDATE notification_email_outbox SET attempts=7 WHERE user_id=? AND status='PENDING'", userId);
        var last = worker.claim(); assertThat(last.attempts()).isEqualTo(8);
        doThrow(new MailSendException("offline")).when(mail).send(anyString(), anyString(), anyString());
        worker.deliver(last);
        assertThat(jdbc.queryForObject("SELECT status FROM notification_email_outbox WHERE id=?", String.class, last.id())).isEqualTo("FAILED");
        assertThat(worker.claim()).isNull();
    }
}
