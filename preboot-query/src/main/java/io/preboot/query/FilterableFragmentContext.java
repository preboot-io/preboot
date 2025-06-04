package io.preboot.query;

import lombok.Getter;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Context class holding all necessary dependencies for FilterableFragment implementation. This reduces constructor
 * parameter count and improves maintainability.
 */
@Service
@Getter
public class FilterableFragmentContext {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SqlBuilder sqlBuilder;
    private final RelationalMappingContext mappingContext;
    private final JdbcConverter jdbcConverter;
    private final ConversionService conversionService;
    private final JdbcAggregateTemplate aggregateTemplate;
    private final PropertyResolver propertyResolver;

    FilterableFragmentContext(
            final NamedParameterJdbcTemplate jdbcTemplate,
            final SqlBuilder sqlBuilder,
            final RelationalMappingContext mappingContext,
            final JdbcConverter jdbcConverter,
            final ConversionService conversionService,
            final JdbcAggregateTemplate aggregateTemplate,
            final PropertyResolver propertyResolver) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlBuilder = sqlBuilder;
        this.mappingContext = mappingContext;
        this.jdbcConverter = jdbcConverter;
        this.conversionService = conversionService;
        this.aggregateTemplate = aggregateTemplate;
        this.propertyResolver = propertyResolver;

        validateDependencies();
    }

    private void validateDependencies() {
        Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");
        Assert.notNull(sqlBuilder, "SqlBuilder must not be null");
        Assert.notNull(mappingContext, "MappingContext must not be null");
        Assert.notNull(jdbcConverter, "JdbcConverter must not be null");
        Assert.notNull(conversionService, "ConversionService must not be null");
        Assert.notNull(aggregateTemplate, "AggregateTemplate must not be null");
        Assert.notNull(propertyResolver, "PropertyResolver must not be null");
    }
}
