package vn.iotstar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Canonical logistics configuration from the Workflow scheduler section.
 *
 * <p>Only the settings owned by the last-mile and finance slice are bound here;
 * the delivery attempt cap and the abandoned-return grace period are read from
 * configuration instead of being hardcoded in services.
 */
@ConfigurationProperties(prefix = "uteexpress")
@Validated
public class LogisticsProperties {

    private final Delivery delivery = new Delivery();
    private final Timeout timeout = new Timeout();
    private final Finance finance = new Finance();

    public Delivery getDelivery() { return delivery; }
    public Timeout getTimeout() { return timeout; }
    public Finance getFinance() { return finance; }

    public static class Delivery {

        /** Maximum failed delivery attempts for the retryable reason group. */
        private int maxAttempts = 3;

        /** Whether at most one failed attempt is accepted per calendar day. */
        private boolean attemptDayLock = true;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public boolean isAttemptDayLock() { return attemptDayLock; }
        public void setAttemptDayLock(boolean attemptDayLock) { this.attemptDayLock = attemptDayLock; }
    }

    public static class Timeout {

        /** Grace period in days before an uncollected returned parcel may be disposed. */
        private int disposedGraceDays = 7;

        /** Demo profile override in minutes; when greater than zero it wins over the days value. */
        private int disposedGraceMinutes = 0;

        public int getDisposedGraceDays() { return disposedGraceDays; }
        public void setDisposedGraceDays(int disposedGraceDays) { this.disposedGraceDays = disposedGraceDays; }
        public int getDisposedGraceMinutes() { return disposedGraceMinutes; }
        public void setDisposedGraceMinutes(int disposedGraceMinutes) { this.disposedGraceMinutes = disposedGraceMinutes; }
    }

    public static class Finance {

        /**
         * Fallback return fee used when a shipment has no priced return fee yet.
         * The pricing policy of the intake slice supersedes this value once the
         * regional pricing table is wired.
         */
        private BigDecimal defaultReturnFee = new BigDecimal("25000.00");

        public BigDecimal getDefaultReturnFee() { return defaultReturnFee; }
        public void setDefaultReturnFee(BigDecimal defaultReturnFee) { this.defaultReturnFee = defaultReturnFee; }
    }
}