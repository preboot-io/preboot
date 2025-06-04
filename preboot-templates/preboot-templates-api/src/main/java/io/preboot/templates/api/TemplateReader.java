package io.preboot.templates.api;

import java.util.Map;

public interface TemplateReader {
    String getContent(String templateName, Map<String, Object> props);
}
