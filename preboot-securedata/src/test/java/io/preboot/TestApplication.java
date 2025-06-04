package io.preboot;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.dialect.PostgresDialect;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan("io.preboot")
@EnableJdbcRepositories(basePackages = "io.preboot")
public class TestApplication {
    @Bean
    public ConversionService conversionService() {
        return new DefaultFormattingConversionService();
    }

    @Bean
    public Dialect jdbcDialect(JdbcTemplate jdbcTemplate) {
        // For now we're using PostgreSQL dialect
        // In a real application, this could be determined from the database metadata
        return PostgresDialect.INSTANCE;
    }
}
