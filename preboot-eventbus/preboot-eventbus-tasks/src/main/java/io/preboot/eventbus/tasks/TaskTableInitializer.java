package io.preboot.eventbus.tasks;

import org.springframework.jdbc.core.JdbcTemplate;

class TaskTableInitializer {
    private final JdbcTemplate jdbcTemplate;
    private final String taskTableName;

    public TaskTableInitializer(JdbcTemplate jdbcTemplate, String taskTableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.taskTableName = taskTableName;
    }

    public void createTables() {
        jdbcTemplate.execute(
                """
            CREATE TABLE IF NOT EXISTS %s (
                id SERIAL PRIMARY KEY,
                type VARCHAR(255),
                payload TEXT,
                created_at TIMESTAMP WITH TIME ZONE,
                next_run_at TIMESTAMP WITH TIME ZONE,
                started_at TIMESTAMP WITH TIME ZONE,
                fail_count INT,
                error_message TEXT,
                error_stack_trace TEXT,
                completed BOOLEAN,
                completed_at TIMESTAMP WITH TIME ZONE,
                dead BOOLEAN,
                optional_hash TEXT,
                executor_instance_id TEXT,
                heartbeat TIMESTAMP WITH TIME ZONE
            )
        """
                        .formatted(taskTableName));

        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_%s_next_run_at ON %s(next_run_at)"
                .formatted(taskTableName, taskTableName));
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_%s_started_at ON %s(started_at)"
                .formatted(taskTableName, taskTableName));
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_idx_%s_optional_hash ON %s(optional_hash)"
                .formatted(taskTableName, taskTableName));
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_%s_executor_instance_id ON %s(executor_instance_id)"
                .formatted(taskTableName, taskTableName));
    }
}
