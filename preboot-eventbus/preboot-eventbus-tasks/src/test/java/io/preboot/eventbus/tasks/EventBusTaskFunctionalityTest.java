package io.preboot.eventbus.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.db.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.preboot.core.json.JsonMapper;
import io.preboot.core.json.JsonMapperFactory;
import io.preboot.eventbus.EventHandler;
import io.preboot.eventbus.EventPublisher;
import io.preboot.eventbus.ExceptionIfNoHandler;
import io.preboot.eventbus.LocalEventHandlerRepository;
import io.preboot.eventbus.LocalEventPublisher;
import java.time.Duration;
import java.util.Map;
import javax.sql.DataSource;
import org.assertj.db.type.AssertDbConnection;
import org.assertj.db.type.AssertDbConnectionFactory;
import org.assertj.db.type.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EventBusTaskFunctionalityTest {
    public static final String EVENTBUS_TASKS = "eventbus_tasks";
    final JsonMapper jsonMapper = JsonMapperFactory.createJsonMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    private TaskPublisher taskPublisher;
    private Table tasksTable;

    private EventPublisher eventPublisher;
    private static int testTaskHandlerCalled;
    private TaskRunner taskRunner;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS %s".formatted(EVENTBUS_TASKS));
        TaskTableInitializer taskTableInitializer = new TaskTableInitializer(jdbcTemplate, EVENTBUS_TASKS);
        taskTableInitializer.createTables();

        TaskRepository taskRepository = new TaskRepositoryH2(jdbcTemplate, EVENTBUS_TASKS);
        taskPublisher = new TaskPublisherImpl(taskRepository, jsonMapper);

        final AssertDbConnection assertDbConnection =
                AssertDbConnectionFactory.of(dataSource).create();
        tasksTable = assertDbConnection.table(EVENTBUS_TASKS).build();

        final ApplicationContext applicationContext = mock(ApplicationContext.class);
        TestTaskHandler handler = new TestTaskHandler();

        // Mock the bean definition names
        when(applicationContext.getBeanDefinitionNames()).thenReturn(new String[] {"testTaskHandler"});

        // Mock the getBean call
        when(applicationContext.getBean("testTaskHandler")).thenReturn(handler);

        LocalEventHandlerRepository localEventHandlerRepository = new LocalEventHandlerRepository(applicationContext);
        eventPublisher = new LocalEventPublisher(localEventHandlerRepository);
        testTaskHandlerCalled = 0;

        taskRunner = new TaskRunnerImpl(
                eventPublisher,
                taskRepository,
                jsonMapper,
                new TimeBasedDeadQueuePolicy(Duration.ofDays(1)),
                new ConstantBackOffPolicy(Duration.ofMinutes(5)));
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS %s".formatted(EVENTBUS_TASKS));
    }

    @Test
    void tableInitializerShouldCreateTable() {
        // check if eventbus_tasks table exists
        assertThat(tasksTable).hasNumberOfRows(0);
    }

    @Test
    void shouldPublishTask() {
        taskPublisher.publishTask(new TestTask("test"));

        assertThat(tasksTable).hasNumberOfRows(1);
        assertThat(tasksTable)
                .row(0)
                .value("type")
                .isEqualTo(TestTask.class.getName())
                .value("payload")
                .isEqualTo("{\"name\":\"test\"}")
                .value("completed")
                .isEqualTo(false)
                .value("dead")
                .isEqualTo(false)
                .value("fail_count")
                .isEqualTo(0)
                .value("next_run_at")
                .isNotNull()
                .value("created_at")
                .isNotNull()
                .value("started_at")
                .isNull();
    }

    @Test
    void shouldStartPublishedTask() {
        taskPublisher.publishTask(new TestTask("test"));
        final String taskFound = taskRunner.runTask();

        assertThat(taskFound).isNotEqualTo("name");
        assertThat(tasksTable)
                .row(0)
                .value("fail_count")
                .isEqualTo(0)
                .value("completed")
                .isEqualTo(true)
                .value("started_at")
                .isNotNull();

        assertThat(testTaskHandlerCalled).isEqualTo(1);
    }

    @Test
    void shouldFailIfNoHandlerFound() {
        taskPublisher.publishTask(new NoHandlerTask("test"));
        taskRunner.runTask();

        assertThat(tasksTable)
                .row(0)
                .value("fail_count")
                .isEqualTo(1)
                .value("completed")
                .isEqualTo(false)
                .value("started_at")
                .isNull();
    }

    @Test
    void shouldNotCallRunningTask() {
        taskPublisher.publishTask(new TestTask("test"));
        final String taskFound = taskRunner.runTask();
        final String taskFound2 = taskRunner.runTask();

        assertThat(taskFound).isEqualTo("io.preboot.eventbus.tasks.EventBusTaskFunctionalityTest$TestTask");
        assertThat(taskFound2).isNull();
        assertThat(testTaskHandlerCalled).isEqualTo(1);
    }

    @Test
    void shouldNotDoublePublishTheSameHash() {
        final String mapHash = HashUtils.getHash(Map.of("key", "value"));
        taskPublisher.publishTask(new TestTask("test"), mapHash);
        assertThatExceptionOfType(TaskHashExistsException.class)
                .isThrownBy(() -> taskPublisher.publishTask(new TestTask("test"), mapHash));
    }

    @Test
    void shouldAllowAddingManyNullHashes() {
        taskPublisher.publishTask(new TestTask("test"));
        taskPublisher.publishTask(new TestTask("test"));

        assertThat(tasksTable).hasNumberOfRows(2);
    }

    public static class TestTaskHandler {
        @EventHandler
        public void onTestTask(TestTask task) {
            testTaskHandlerCalled += 1;
        }
    }

    public static class TestTask {
        private final String name;

        @JsonCreator
        public TestTask(@JsonProperty("name") String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @ExceptionIfNoHandler
    public static class NoHandlerTask {
        private final String name;

        @JsonCreator
        public NoHandlerTask(@JsonProperty("name") String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
