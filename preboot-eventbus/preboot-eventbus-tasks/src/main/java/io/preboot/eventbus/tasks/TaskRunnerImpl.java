package io.preboot.eventbus.tasks;

import io.preboot.core.json.JsonMapper;
import io.preboot.eventbus.EventPublisher;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

class TaskRunnerImpl implements TaskRunner {
    private final EventPublisher eventPublisher;
    private final TaskRepository taskRepository;
    private final ClassLoader classLoader;
    private final JsonMapper jsonMapper;
    private final DeadQueuePolicy deadQueuePolicy;
    private final BackOffPolicy backOffPolicy;
    private final String runnerId = UUID.randomUUID().toString();

    public TaskRunnerImpl(
            final EventPublisher eventPublisher,
            final TaskRepository taskRepository,
            final JsonMapper jsonMapper,
            final DeadQueuePolicy deadQueuePolicy,
            final BackOffPolicy backOffPolicy) {
        this.eventPublisher = eventPublisher;
        this.taskRepository = taskRepository;
        this.jsonMapper = jsonMapper;
        this.deadQueuePolicy = deadQueuePolicy;
        this.backOffPolicy = backOffPolicy;
        this.classLoader = getClass().getClassLoader();
    }

    @Override
    public String getRunnerId() {
        return runnerId;
    }

    @Override
    public String runTask() {
        final Optional<Task> taskToRun = taskRepository.findTaskToRun(runnerId);
        return taskToRun
                .map(task -> {
                    handleTask(task);
                    return task.getType();
                })
                .orElse(null);
    }

    @Override
    public void updateHeartbeat() {
        taskRepository.updateHeartbeat(runnerId);
    }

    @Override
    public void retrieveStalledTasks(Instant heartbeatThreshold) {
        taskRepository.retrieveStalledTasks(heartbeatThreshold);
    }

    @Override
    public boolean hasPendingTasks() {
        return taskRepository.hasPendingTasks();
    }

    private void handleTask(final Task task) {
        try {
            final Class<?> payloadClass = classLoader.loadClass(task.getType());
            final Object payload = jsonMapper.fromJson(task.getPayload(), payloadClass);
            eventPublisher.publish(payload);
            taskRepository.markAsCompleted(task.getId());
        } catch (Exception e) {
            handleTaskFailure(task, e);
        }
    }

    private void handleTaskFailure(final Task task, final Exception e) {
        task.setFailCount(task.getFailCount() + 1);
        task.setErrorMessage(e.getMessage());
        task.setErrorStackTrace(Arrays.toString(e.getStackTrace()));
        task.setDead(deadQueuePolicy.isDead(
                task.getFailCount(),
                task.getErrorMessage(),
                task.getErrorStackTrace(),
                task.getType(),
                task.getCreatedAt()));
        task.setNextRunAt(
                task.isDead()
                        ? null
                        : backOffPolicy.calculateNextRunAt(
                                task.getFailCount(),
                                task.getErrorMessage(),
                                task.getErrorStackTrace(),
                                task.getType(),
                                task.getCreatedAt()));
        task.setStartedAt(null);
        taskRepository.save(task);
    }
}
