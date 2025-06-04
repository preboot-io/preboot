package io.preboot.exporters.api;

import java.util.Locale;

/**
 * Interface for translating values displayed in an Excel sheet. Allows customization of values before saving to Excel,
 * e.g. for localization or domain-specific formatting.
 */
public interface ValueTranslator {
    /**
     * Translates a value for a specific column, taking into account locale settings.
     *
     * @param column Name of the column for which translation is performed
     * @param value Value to translate
     * @param locale Locale settings
     * @return Translated value
     */
    String translate(String column, String value, Locale locale);
}
