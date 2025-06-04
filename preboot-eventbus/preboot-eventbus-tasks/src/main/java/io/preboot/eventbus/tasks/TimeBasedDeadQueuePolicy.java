package io.preboot.eventbus.tasks;

import java.time.Instant;
import java.time.temporal.TemporalAmount;

public class TimeBasedDeadQueuePolicy implements DeadQueuePolicy {

    private final TemporalAmount timeToLivePeriod;

    public TimeBasedDeadQueuePolicy(TemporalAmount timeToLivePeriod) {
        this.timeToLivePeriod = timeToLivePeriod;
    }

    @Override
    public boolean isDead(
            final int failCount,
            final String errorMessage,
            final String errorStackTrace,
            final String type,
            final Instant createdAt) {
        return createdAt.plus(timeToLivePeriod).isBefore(Instant.now());
    }
}
