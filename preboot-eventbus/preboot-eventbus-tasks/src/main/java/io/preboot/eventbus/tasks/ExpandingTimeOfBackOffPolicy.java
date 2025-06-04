package io.preboot.eventbus.tasks;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Random;

public class ExpandingTimeOfBackOffPolicy implements BackOffPolicy {

    private final TemporalAmount beginBackoff;
    private final int randomnessInSeconds;
    private final int backoffMultiplier;
    private final int maxFallbackInMinutes;
    private final Random random = new Random();

    public ExpandingTimeOfBackOffPolicy(
            final TemporalAmount beginBackoff,
            final int randomnessInSeconds,
            final int backoffMultiplier,
            final int maxFallbackInMinutes) {
        this.beginBackoff = beginBackoff;
        this.randomnessInSeconds = randomnessInSeconds;
        this.backoffMultiplier = backoffMultiplier;
        this.maxFallbackInMinutes = maxFallbackInMinutes;
    }

    @Override
    public Instant calculateNextRunAt(
            final int failCount,
            final String errorMessage,
            final String errorStackTrace,
            final String type,
            final Instant createdAt) {
        return Instant.now()
                .plus(beginBackoff)
                .plus(random.nextInt(randomnessInSeconds), ChronoUnit.SECONDS)
                .plus(Math.max(failCount * backoffMultiplier, maxFallbackInMinutes), ChronoUnit.MINUTES);
    }
}
