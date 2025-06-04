package io.preboot.eventbus.tasks;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class TaskRepositoryH2 extends TaskRepositoryPostgres {
    private final JdbcTemplate jdbcTemplate;
    private final String taskTableName;

    public TaskRepositoryH2(JdbcTemplate jdbcTemplate, String taskTableName) {
        super(jdbcTemplate, taskTableName);
        this.jdbcTemplate = jdbcTemplate;
        this.taskTableName = taskTableName;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Task> findTaskToRun(String runnerId) {
        final List<Task> tasks = jdbcTemplate.query(
                """
                SELECT * FROM %s WHERE next_run_at <= NOW() and started_at IS NULL ORDER BY next_run_at LIMIT 1
                """
                        .formatted(taskTableName),
                new TaskRowMapper());

        if (tasks.isEmpty()) {
            return Optional.empty();
        }
        final Task task = tasks.getFirst();
        task.setStartedAt(Instant.now());
        save(task);
        return Optional.of(task);
    }
}
