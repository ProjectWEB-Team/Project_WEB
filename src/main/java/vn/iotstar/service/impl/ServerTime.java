package vn.iotstar.service.impl;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** Operational DATETIME2(0) fields use server-local wall time truncated to seconds, not UTC. */
final class ServerTime {
    private ServerTime() { }

    static LocalDateTime now(Clock clock) {
        return LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS);
    }
}
