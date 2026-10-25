package vn.iotstar.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables Workflow timeout and return-grace scheduled checks; tests can switch them off together. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "uteexpress.timeout.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
