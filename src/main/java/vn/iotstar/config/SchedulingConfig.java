package vn.iotstar.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables the scheduled checks of the Workflow: the grace period for a returned
 * parcel that nobody collects.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}