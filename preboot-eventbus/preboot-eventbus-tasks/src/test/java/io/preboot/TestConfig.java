package io.preboot;

import io.preboot.core.json.JsonMapper;
import io.preboot.core.json.JsonMapperFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
class TestConfig {
    @Bean
    JsonMapper jsonMapper() {
        return JsonMapperFactory.createJsonMapper();
    }
}
