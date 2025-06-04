package io.preboot.templates.thymeleaf;

import io.preboot.templates.api.TemplateNotFoundException;
import io.preboot.templates.api.TemplateReader;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateInputException;

@RequiredArgsConstructor
@Slf4j
@Service
public class ThymeleafTemplateService implements TemplateReader {

    private final TemplateEngine templateEngine;

    @Override
    public String getContent(String templateName, Map<String, Object> props) {
        final Context templateContext = new Context();
        templateContext.setVariables(props);
        try {
            return templateEngine.process(templateName, templateContext);
        } catch (TemplateInputException e) {
            try {
                return templateEngine.process(templateName, templateContext);
            } catch (TemplateInputException ex) {
                log.error("Cannot find notification template: {}", templateName);
                throw new TemplateNotFoundException(templateName);
            }
        }
    }
}
