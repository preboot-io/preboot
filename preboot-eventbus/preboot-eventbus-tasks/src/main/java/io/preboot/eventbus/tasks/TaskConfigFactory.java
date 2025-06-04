package io.preboot.eventbus.tasks;

import io.preboot.core.json.JsonMapper;
import io.preboot.eventbus.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskConfigFactory {
    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper;

    public TaskRepository createTaskRepository(String taskTableName) {
        TaskTableInitializer taskTableInitializer = new TaskTableInitializer(jdbcTemplate, taskTableName);
        taskTableInitializer.createTables();
        return new TaskRepositoryPostgres(jdbcTemplate, taskTableName);
    }

    public TaskRepository createTaskRepositoryOnH2(String taskTableName) {
        TaskTableInitializer taskTableInitializer = new TaskTableInitializer(jdbcTemplate, taskTableName);
        taskTableInitializer.createTables();
        return new TaskRepositoryH2(jdbcTemplate, taskTableName);
    }

    public TaskPublisher createTaskPublisher(TaskRepository taskRepository) {
        return new TaskPublisherImpl(taskRepository, jsonMapper);
    }

    public TaskRunner createTaskRunner(
            EventPublisher eventPublisher,
            TaskRepository taskRepository,
            DeadQueuePolicy deadQueuePolicy,
            BackOffPolicy backOffPolicy) {
        return new TaskRunnerImpl(eventPublisher, taskRepository, jsonMapper, deadQueuePolicy, backOffPolicy);
    }
}
