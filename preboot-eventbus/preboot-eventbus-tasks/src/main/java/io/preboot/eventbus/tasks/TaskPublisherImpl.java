package io.preboot.eventbus.tasks;

import io.preboot.core.json.JsonMapper;
import java.time.Instant;

class TaskPublisherImpl implements TaskPublisher {
    private final TaskRepository taskRepository;
    private final JsonMapper jsonMapper;

    public TaskPublisherImpl(final TaskRepository taskRepository, final JsonMapper jsonMapper) {
        this.taskRepository = taskRepository;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public <T> void publishTask(final T task) {
        final Instant now = Instant.now();

        Task taskEntity = new Task();
        taskEntity.setType(task.getClass().getName());
        taskEntity.setPayload(jsonMapper.toJson(task));
        taskEntity.setNextRunAt(now);
        taskEntity.setCreatedAt(now);
        taskEntity.setFailCount(0);
        taskEntity.setCompleted(false);
        taskEntity.setDead(false);
        taskEntity.setHeartbeat(null);
        taskEntity.setExecutorInstanceId(null);
        taskRepository.save(taskEntity);
    }

    @Override
    public <T> void publishTask(final T task, String hash) {
        final Instant now = Instant.now();

        Task taskEntity = new Task();
        taskEntity.setType(task.getClass().getName());
        taskEntity.setPayload(jsonMapper.toJson(task));
        taskEntity.setNextRunAt(now);
        taskEntity.setCreatedAt(now);
        taskEntity.setFailCount(0);
        taskEntity.setCompleted(false);
        taskEntity.setDead(false);
        taskEntity.setOptionalHash(hash);
        taskEntity.setHeartbeat(null);
        taskEntity.setExecutorInstanceId(null);

        taskRepository.save(taskEntity);
    }
}
