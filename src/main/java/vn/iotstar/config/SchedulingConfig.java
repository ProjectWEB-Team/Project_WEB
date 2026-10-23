package vn.iotstar.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Runs the intake timeout job; tests switch it off with uteexpress.timeout.scheduler-enabled=false. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "uteexpress.timeout.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
