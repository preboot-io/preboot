package io.preboot.eventbus.tasks;

import java.time.Instant;

public interface TaskRunner {
    String getRunnerId();

    String runTask();

    void updateHeartbeat();

    void retrieveStalledTasks(Instant heartbeatThreshold);

    boolean hasPendingTasks();
}
