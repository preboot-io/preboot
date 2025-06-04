package io.preboot.query;

import java.util.Map;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;

public record SqlContext(
        RelationalPersistentEntity<?> entity,
        Map<String, JoinInfo> joins,
        PropertyResolver propertyResolver,
        RelationalMappingContext mappingContext,
        int nextParamIndex) {}
