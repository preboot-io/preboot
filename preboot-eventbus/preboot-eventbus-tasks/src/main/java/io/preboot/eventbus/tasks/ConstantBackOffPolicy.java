package io.preboot.eventbus.tasks;

import java.time.Instant;
import java.time.temporal.TemporalAmount;

public class ConstantBackOffPolicy implements BackOffPolicy {

    private final TemporalAmount backOffPeriod;

    public ConstantBackOffPolicy(TemporalAmount backOffPeriod) {
        this.backOffPeriod = backOffPeriod;
    }

    @Override
    public Instant calculateNextRunAt(
            final int failCount,
            final String errorMessage,
            final String errorStackTrace,
            final String type,
            final Instant createdAt) {
        return Instant.now().plus(backOffPeriod);
    }
}
