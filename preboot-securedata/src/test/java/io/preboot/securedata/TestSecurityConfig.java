package io.preboot.securedata;

import io.preboot.eventbus.EventPublisher;
import io.preboot.eventbus.LocalEventHandlerRepository;
import io.preboot.eventbus.LocalEventPublisher;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestSecurityConfig {
    @Bean
    @Primary
    public TestSecurityContextHolder securityContextHolder() {
        return new TestSecurityContextHolder();
    }

    @Bean
    @Lazy
    LocalEventHandlerRepository localEventHandlerRepository(ApplicationContext applicationContext) {
        return new LocalEventHandlerRepository(applicationContext);
    }

    @Bean
    public EventPublisher eventPublisher(LocalEventHandlerRepository localEventHandlerRepository) {
        return new LocalEventPublisher(localEventHandlerRepository);
    }
}
