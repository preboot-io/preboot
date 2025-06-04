package io.preboot.eventbus.tasks;

import java.time.Instant;

public interface DeadQueuePolicy {
    boolean isDead(int failCount, String errorMessage, String errorStackTrace, String type, Instant createdAt);
}
