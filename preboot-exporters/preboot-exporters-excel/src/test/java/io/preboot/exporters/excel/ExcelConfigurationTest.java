package io.preboot.exporters.excel;

import static org.assertj.core.api.Assertions.assertThat;

import io.preboot.exporters.api.ValueTranslator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = ExcelConfiguration.class)
class ExcelConfigurationTest {

    @Autowired
    private ValueTranslator translator;

    @Test
    void excelValueTranslator_ShouldProvideDefaultValueTranslator() {
        // Assert
        assertThat(translator).isNotNull().isInstanceOf(DefaultValueTranslator.class);
    }
}
