package io.preboot.files.inmemory;

import io.preboot.eventbus.EventPublisher;
import io.preboot.files.api.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "preboot.files.storage.type", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryFileStorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(FileStorageService.class)
    public FileStorageService inMemoryFileStorageService(
            EventPublisher eventPublisher, @Value("${preboot.files.max-file-size:52428800}") long maxFileSize) {
        return new InMemoryFileStorageService(eventPublisher, maxFileSize);
    }

    @Bean
    @ConditionalOnMissingBean
    public FileEventHandler fileEventHandler() {
        return new FileEventHandler();
    }
}
