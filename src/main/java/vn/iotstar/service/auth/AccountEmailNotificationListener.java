package vn.iotstar.service.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "uteexpress.mail.enabled", havingValue = "true")
public class AccountEmailNotificationListener {
    private final JdbcTemplate jdbc;
    public AccountEmailNotificationListener(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void notify(AccountEmailNotification notification) {
        jdbc.update("""
                INSERT INTO notification_email_outbox (id,user_id,email,subject,body)
                SELECT ?,id,email,?,? FROM users
                WHERE username=? AND status='ACTIVE' AND role='ROLE_USER' AND email_verified_at IS NOT NULL
                """, UUID.randomUUID().toString(), notification.subject(),
                notification.message() + "\n\nĐăng nhập UTEExpress để xem chi tiết.", notification.username());
    }
}
