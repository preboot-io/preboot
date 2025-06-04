package io.preboot.exporters.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class DefaultValueTranslatorTest {

    private DefaultValueTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new DefaultValueTranslator();
    }

    @Test
    void translate_WithRegularValue_ShouldReturnUnchangedValue() {
        // Arrange
        String column = "testColumn";
        String value = "testValue";
        Locale locale = Locale.getDefault();

        // Act
        String result = translator.translate(column, value, locale);

        // Assert
        assertThat(result).isEqualTo(value);
    }

    @ParameterizedTest
    @CsvSource({"status, COMPLETED, COMPLETED", "amount, 100.50, 100.50", "orderNumber, ORD001, ORD001"})
    void translate_WithVariousValues_ShouldReturnUnchangedValues(String column, String value, String expected) {
        // Act
        String result = translator.translate(column, value, Locale.getDefault());

        // Assert
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void translate_WithNullOrEmptyValue_ShouldReturnUnchangedValue(String value) {
        // Act
        String result = translator.translate("testColumn", value, Locale.getDefault());

        // Assert
        assertThat(result).isEqualTo(value);
    }

    @Test
    void translate_WithDifferentLocales_ShouldReturnUnchangedValue() {
        // Arrange
        String column = "testColumn";
        String value = "testValue";
        Locale[] locales = {Locale.US, Locale.GERMANY, new Locale("pl", "PL")};

        for (Locale locale : locales) {
            // Act
            String result = translator.translate(column, value, locale);

            // Assert
            assertThat(result).isEqualTo(value);
        }
    }
}
