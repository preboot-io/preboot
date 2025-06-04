package io.preboot.eventbus.tasks;

import java.time.Instant;

public interface BackOffPolicy {
    Instant calculateNextRunAt(
            int failCount, String errorMessage, String errorStackTrace, String type, Instant createdAt);
}
