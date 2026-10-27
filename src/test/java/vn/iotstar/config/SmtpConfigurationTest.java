package vn.iotstar.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import static org.assertj.core.api.Assertions.*;

class SmtpConfigurationTest {
    @Test void smtpProfileBindsExternalCredentialsAndRequiresVerifiedTlsWithBoundedTimeouts() throws Exception {
        var properties = new ResourcePropertySource("classpath:application-smtp.properties");
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
                .withInitializer(context -> context.getEnvironment().getPropertySources()
                        .addLast(properties))
                .withPropertyValues("MAIL_USERNAME=sender@example.test", "MAIL_PASSWORD=test-only-password")
                .run(context -> {
                    assertThat(context).hasSingleBean(JavaMailSenderImpl.class);
                    var sender = context.getBean(JavaMailSenderImpl.class);
                    assertThat(sender.getHost()).isEqualTo("smtp.gmail.com"); assertThat(sender.getPort()).isEqualTo(587);
                    assertThat(sender.getUsername()).isEqualTo("sender@example.test");
                    assertThat(sender.getJavaMailProperties())
                            .containsEntry("mail.smtp.starttls.required", "true")
                            .containsEntry("mail.smtp.ssl.checkserveridentity", "true")
                            .containsEntry("mail.smtp.connectiontimeout", "5000")
                            .containsEntry("mail.smtp.timeout", "5000")
                            .containsEntry("mail.smtp.writetimeout", "5000");
                });
    }
}
