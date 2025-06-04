package io.preboot.exporters.excel;

import io.preboot.exporters.api.ValueTranslator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration of Excel handling components. Defines beans to be used if no custom implementations are defined. */
@Configuration
public class ExcelConfiguration {

    /**
     * Creates a value translator if no other is defined.
     *
     * @return default value translator
     */
    @Bean
    @ConditionalOnMissingBean
    public ValueTranslator excelValueTranslator() {
        return new DefaultValueTranslator();
    }
}
