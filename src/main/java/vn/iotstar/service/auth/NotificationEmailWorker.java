package vn.iotstar.service.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "uteexpress.mail.enabled", havingValue = "true")
public class NotificationEmailWorker {
    private static final Logger log = LoggerFactory.getLogger(NotificationEmailWorker.class);
    private final JdbcTemplate jdbc;
    private final AccountEmailSender mail;
    public NotificationEmailWorker(JdbcTemplate jdbc, AccountEmailSender mail) { this.jdbc = jdbc; this.mail = mail; }
    public record Delivery(String id, String lease, long userId, String email, String subject, String body, int attempts) { }

    @Scheduled(fixedDelayString = "${uteexpress.mail.poll-ms:5000}")
    public void deliverPending() {
        // Claim one row at a time so messages do not wait for SMTP while their lease expires.
        for (int i = 0; i < 10; i++) {
            var delivery = claim();
            if (delivery == null) return;
            deliver(delivery);
        }
    }
    public Delivery claim() {
        jdbc.update("""
                UPDATE notification_email_outbox SET status='FAILED',completed_at=SYSUTCDATETIME(),last_error='LeaseExpired'
                WHERE status='SENDING' AND attempts=8 AND available_at<=SYSUTCDATETIME()
                """);
        String lease = UUID.randomUUID().toString();
        var rows = jdbc.query("""
                ;WITH candidate AS (
                    SELECT TOP (1) * FROM notification_email_outbox WITH (UPDLOCK,READPAST,READCOMMITTEDLOCK)
                    WHERE status IN ('PENDING','SENDING') AND attempts<8 AND available_at<=SYSUTCDATETIME()
                    ORDER BY available_at,id
                )
                UPDATE candidate SET status='SENDING',attempts=attempts+1,lease_token=?,available_at=DATEADD(minute,2,SYSUTCDATETIME())
                OUTPUT inserted.id,inserted.user_id,inserted.email,inserted.subject,inserted.body,inserted.attempts
                """, (row, index) -> new Delivery(row.getString("id"), lease, row.getLong("user_id"),
                row.getString("email"), row.getString("subject"), row.getString("body"), row.getInt("attempts")), lease);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public void deliver(Delivery delivery) {
        try {
            Integer owned = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM notification_email_outbox WHERE id=? AND status='SENDING' AND lease_token=?
                    AND available_at>SYSUTCDATETIME()
                    """, Integer.class, delivery.id(), delivery.lease());
            if (owned == null || owned != 1) return;
            Integer eligible = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM users WHERE id=? AND email=? AND email_verified_at IS NOT NULL
                    AND status='ACTIVE' AND role='ROLE_USER'
                    """, Integer.class, delivery.userId(), delivery.email());
            if (eligible == null || eligible != 1) { complete(delivery, "CANCELLED"); return; }
            mail.send(delivery.email(), delivery.subject(), delivery.body());
            complete(delivery, "SENT");
        } catch (RuntimeException failure) {
            String error = failure.getClass().getSimpleName();
            int delay = Math.min(3600, 60 * (1 << Math.min(delivery.attempts() - 1, 6)));
            jdbc.update("""
                    UPDATE notification_email_outbox SET status=?,available_at=DATEADD(second,?,SYSUTCDATETIME()),
                    completed_at=CASE WHEN attempts=8 THEN SYSUTCDATETIME() ELSE NULL END,last_error=?,lease_token=NULL
                    WHERE id=? AND status='SENDING' AND lease_token=?
                    """, delivery.attempts() >= 8 ? "FAILED" : "PENDING", delay, error, delivery.id(), delivery.lease());
            log.warn("Notification email {} failed on attempt {} ({})", delivery.id(), delivery.attempts(), error);
        }
    }
    private void complete(Delivery delivery, String status) {
        jdbc.update("""
                UPDATE notification_email_outbox SET status=?,completed_at=SYSUTCDATETIME(),lease_token=NULL,last_error=NULL,
                email='',subject='',body='' WHERE id=? AND status='SENDING' AND lease_token=?
                """, status, delivery.id(), delivery.lease());
    }
    @Scheduled(fixedDelayString = "${uteexpress.mail.cleanup-ms:86400000}")
    public void cleanup() {
        jdbc.update("""
                DELETE FROM notification_email_outbox WHERE status IN ('SENT','FAILED','CANCELLED')
                AND completed_at<DATEADD(day,-30,SYSUTCDATETIME())
                """);
    }
}
