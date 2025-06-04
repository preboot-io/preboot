package io.preboot.auth.test;

import io.preboot.eventbus.EventPublisher;
import io.preboot.eventbus.LocalEventHandlerRepository;
import io.preboot.eventbus.LocalEventPublisher;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

@TestConfiguration
public class TestConfig {

    @Bean
    @Lazy
    public EventPublisher eventPublisher(ApplicationContext applicationContext) {
        LocalEventHandlerRepository localEventHandlerRepository = new LocalEventHandlerRepository(applicationContext);
        return new LocalEventPublisher(localEventHandlerRepository);
    }
}
