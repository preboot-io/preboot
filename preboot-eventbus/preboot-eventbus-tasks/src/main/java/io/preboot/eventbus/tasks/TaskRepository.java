package io.preboot.eventbus.tasks;

import java.time.Instant;
import java.util.Optional;

public interface TaskRepository {
    void save(Task task);

    Optional<Task> findTaskToRun(String runnerId);

    void markAsCompleted(Long taskId);

    void updateHeartbeat(String executorInstanceId);

    void retrieveStalledTasks(Instant heartbeatThreshold);

    boolean hasPendingTasks();
}
