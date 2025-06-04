package io.preboot.eventbus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

@ExtendWith(MockitoExtension.class)
class LocalEventHandlerRepositoryTest {

    private LocalEventHandlerRepository localEventHandlerRepository;
    private static int testEventEventHandler1CallCount;
    private static int testEventEventHandler2CallCount;
    private static int testEvent2EventHandlerCallCount;

    @BeforeEach
    void setUp() {
        final ApplicationContext applicationContext = mock(ApplicationContext.class);

        // Create handler instances
        TestEventEventHandler1 handler1 = new TestEventEventHandler1();
        TestEventEventHandler2 handler2 = new TestEventEventHandler2();
        TestEvent2EventHandler handler3 = new TestEvent2EventHandler();

        // Mock the bean definition names
        when(applicationContext.getBeanDefinitionNames()).thenReturn(new String[] {"handler1", "handler2", "handler3"});

        // Mock the getBean calls
        when(applicationContext.getBean("handler1")).thenReturn(handler1);
        when(applicationContext.getBean("handler2")).thenReturn(handler2);
        when(applicationContext.getBean("handler3")).thenReturn(handler3);

        localEventHandlerRepository = new LocalEventHandlerRepository(applicationContext);
        testEventEventHandler1CallCount = 0;
        testEventEventHandler2CallCount = 0;
        testEvent2EventHandlerCallCount = 0;
    }

    @Test
    void shouldPublishToMultipleListeners() {
        // when
        localEventHandlerRepository.publish(new TestEvent());

        // then
        assertThat(testEventEventHandler1CallCount).isEqualTo(1);
        assertThat(testEventEventHandler2CallCount).isEqualTo(1);
        assertThat(testEvent2EventHandlerCallCount).isEqualTo(0);
    }

    @Test
    void shouldPublishToCorrectHandler() {
        // when
        localEventHandlerRepository.publish(new TestEvent2());

        // then
        assertThat(testEventEventHandler1CallCount).isEqualTo(0);
        assertThat(testEventEventHandler2CallCount).isEqualTo(0);
        assertThat(testEvent2EventHandlerCallCount).isEqualTo(1);
    }

    @Test
    void shouldRespectHandlerPriority() {
        // Reset call sequence tracking
        testEventEventHandler1CallCount = 0;
        testEventEventHandler2CallCount = 0;

        // when
        localEventHandlerRepository.publish(new TestEvent());

        // Verify handlers were called in priority order
        // Handler1 has priority 100, Handler2 has default priority 0
        assertThat(testEventEventHandler1CallCount).isEqualTo(1);
        assertThat(testEventEventHandler2CallCount).isEqualTo(1);
    }

    @Test
    void shouldThrowExceptionIfNoHandlerProvided() {
        LocalEventPublisher localEventPublisher = new LocalEventPublisher(localEventHandlerRepository);

        assertThatExceptionOfType(NoEventHandlerException.class)
                .isThrownBy(() -> localEventPublisher.publish(new TestEvent3()));
    }

    public static class TestEvent {}

    public static class TestEvent2 {}

    @ExceptionIfNoHandler
    public static class TestEvent3 {}

    public static class TestEventEventHandler1 {
        @EventHandler(priority = 100) // Higher priority
        public void onTestEvent(TestEvent event) {
            testEventEventHandler1CallCount += 1;
        }
    }

    public static class TestEventEventHandler2 {
        @EventHandler // Default priority = 0
        public void onTestEvent(TestEvent event) {
            testEventEventHandler2CallCount += 1;
        }
    }

    public static class TestEvent2EventHandler {
        @EventHandler
        public void onTestEvent2(TestEvent2 event) {
            testEvent2EventHandlerCallCount += 1;
        }
    }
}
