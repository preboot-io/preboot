package io.preboot.eventbus.tasks;

import java.time.Instant;
import lombok.Data;

@Data
class Task {
    private Long id;
    private String type;
    private String payload;
    private Instant createdAt;
    private Instant nextRunAt;
    private Instant startedAt;
    private int failCount;
    private String errorMessage;
    private String errorStackTrace;
    private boolean completed;
    private Instant completedAt;
    private boolean dead;
    private String optionalHash;
    private String executorInstanceId;
    private Instant heartbeat;
}
