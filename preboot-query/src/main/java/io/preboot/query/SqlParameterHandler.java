package io.preboot.query;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SqlParameterHandler {
    private final PropertyResolver propertyResolver;
    private final ConversionService conversionService;

    public Object convertValue(RelationalPersistentEntity<?> entity, String field, Object value) {
        RelationalPersistentProperty property = propertyResolver.getPropertyByPath(entity, field);
        return conversionService.convert(value, property.getType());
    }

    public SqlParameterSource createParameterSource(CriteriaExpression expression) {
        CriteriaParameterSource paramSource = new CriteriaParameterSource();
        expression.addParameters(paramSource);
        return paramSource;
    }
}
