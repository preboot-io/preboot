package io.preboot.exporters.excel;

import io.preboot.exporters.api.ValueTranslator;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Default implementation of ValueTranslator that returns the original value unchanged. This class can be extended or
 * replaced to customize value translation.
 */
@Component
public class DefaultValueTranslator implements ValueTranslator {
    @Override
    public String translate(String column, String value, Locale locale) {
        return value;
    }
}
