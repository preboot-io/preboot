package io.preboot.eventbus.tasks;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class TaskRepositoryPostgres implements TaskRepository {
    private final JdbcTemplate jdbcTemplate;
    private final String taskTableName;

    public TaskRepositoryPostgres(JdbcTemplate jdbcTemplate, String taskTableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.taskTableName = taskTableName;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(Task task) {
        try {
            if (task.getId() == null) {
                jdbcTemplate.update(
                        """
                                INSERT INTO %s (type, payload, next_run_at, started_at, fail_count, error_message, error_stack_trace, completed, completed_at, dead, created_at, optional_hash, heartbeat, executor_instance_id)
                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                                """
                                .formatted(taskTableName),
                        task.getType(),
                        task.getPayload(),
                        task.getNextRunAt(),
                        task.getStartedAt(),
                        task.getFailCount(),
                        task.getErrorMessage(),
                        task.getErrorStackTrace(),
                        task.isCompleted(),
                        task.getCompletedAt(),
                        task.isDead(),
                        task.getCreatedAt(),
                        task.getOptionalHash(),
                        task.getHeartbeat(),
                        task.getExecutorInstanceId());
            } else {
                jdbcTemplate.update(
                        """
                                UPDATE %s
                                SET type = ?, payload = ?, next_run_at = ?, started_at = ?, fail_count = ?, error_message = ?, error_stack_trace = ?, completed = ?, completed_at = ?, dead = ?, created_at = ?, optional_hash = ?, heartbeat = ?, executor_instance_id = ?
                                WHERE id = ?
                                """
                                .formatted(taskTableName),
                        task.getType(),
                        task.getPayload(),
                        task.getNextRunAt(),
                        task.getStartedAt(),
                        task.getFailCount(),
                        task.getErrorMessage(),
                        task.getErrorStackTrace(),
                        task.isCompleted(),
                        task.getCompletedAt(),
                        task.isDead(),
                        task.getCreatedAt(),
                        task.getOptionalHash(),
                        task.getHeartbeat(),
                        task.getExecutorInstanceId(),
                        task.getId());
            }
        } catch (DuplicateKeyException e) {
            throw new TaskHashExistsException();
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Task> findTaskToRun(String runnerId) {
        final List<Task> tasks = jdbcTemplate.query(
                """
                        UPDATE %s SET started_at = NOW(), heartbeat = NOW(), executor_instance_id = ? WHERE id = (
                            SELECT id FROM %s WHERE next_run_at <= NOW() and started_at is null ORDER BY next_run_at FOR UPDATE SKIP LOCKED LIMIT 1
                        )
                        RETURNING *
                        """
                        .formatted(taskTableName, taskTableName),
                new TaskRowMapper(),
                runnerId);
        return Optional.ofNullable(tasks.isEmpty() ? null : tasks.getFirst());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsCompleted(final Long taskId) {
        jdbcTemplate.update(
                """
                        UPDATE %s
                        SET completed = true, completed_at = NOW(), heartbeat = NULL, executor_instance_id = NULL
                        WHERE id = ?
                        """
                        .formatted(taskTableName),
                taskId);
    }

    @Override
    public void updateHeartbeat(final String executorInstanceId) {
        jdbcTemplate.update(
                """
                        UPDATE %s
                        SET heartbeat = NOW()
                        WHERE executor_instance_id = ?
                        """
                        .formatted(taskTableName),
                executorInstanceId);
    }

    @Override
    public void retrieveStalledTasks(final Instant heartbeatThreshold) {
        jdbcTemplate.update(
                """
                        UPDATE %s
                        SET heartbeat = NULL, executor_instance_id = NULL, started_at = NULL
                        WHERE heartbeat < ?
                        """
                        .formatted(taskTableName),
                Timestamp.from(heartbeatThreshold));
    }

    @Override
    public boolean hasPendingTasks() {
        String sql =
                """
                    SELECT EXISTS (
                        SELECT 1
                        FROM %s
                        WHERE next_run_at <= ?
                        AND started_at IS NULL
                    )
                """
                        .formatted(taskTableName);

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, Timestamp.from(Instant.now())));
    }

    protected static class TaskRowMapper implements RowMapper<Task> {
        @Override
        public Task mapRow(ResultSet rs, int rowNum) throws SQLException {
            Task task = new Task();
            task.setId(rs.getLong("id"));
            task.setType(rs.getString("type"));
            task.setPayload(rs.getString("payload"));
            task.setNextRunAt(rs.getTimestamp("next_run_at").toInstant());
            task.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            task.setStartedAt(Optional.ofNullable(rs.getTimestamp("started_at"))
                    .map(Timestamp::toInstant)
                    .orElse(null));
            task.setFailCount(rs.getInt("fail_count"));
            task.setErrorMessage(rs.getString("error_message"));
            task.setErrorStackTrace(rs.getString("error_stack_trace"));
            task.setCompleted(rs.getBoolean("completed"));
            task.setCompletedAt(Optional.ofNullable(rs.getTimestamp("completed_at"))
                    .map(Timestamp::toInstant)
                    .orElse(null));
            task.setDead(rs.getBoolean("dead"));
            task.setOptionalHash(rs.getString("optional_hash"));
            task.setHeartbeat(Optional.ofNullable(rs.getTimestamp("heartbeat"))
                    .map(Timestamp::toInstant)
                    .orElse(null));
            task.setExecutorInstanceId(rs.getString("executor_instance_id"));
            return task;
        }
    }
}
