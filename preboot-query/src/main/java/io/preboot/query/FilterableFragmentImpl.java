package io.preboot.query;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.jdbc.core.convert.EntityRowMapper;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.Assert;

@Slf4j
public abstract class FilterableFragmentImpl<T, ID> implements FilterableFragment<T>, CrudRepository<T, ID> {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SqlBuilder sqlBuilder;
    private final JdbcConverter jdbcConverter;
    private final RelationalPersistentEntity<T> entity;
    private final ProjectionFactory projectionFactory;
    private final ConversionService conversionService;
    private final RelationalMappingContext mappingContext;
    private final Class<T> entityClass;
    private final JdbcAggregateTemplate aggregateTemplate;
    private final PropertyResolver propertyResolver;

    protected FilterableFragmentImpl(FilterableFragmentContext context, final Class<T> entityClass) {
        this.jdbcTemplate = context.getJdbcTemplate();
        this.sqlBuilder = context.getSqlBuilder();
        this.jdbcConverter = context.getJdbcConverter();
        this.conversionService = context.getConversionService();
        this.mappingContext = context.getMappingContext();
        this.aggregateTemplate = context.getAggregateTemplate();
        this.projectionFactory = new SpelAwareProxyProjectionFactory();
        this.propertyResolver = context.getPropertyResolver();
        this.entityClass = entityClass;

        Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");
        Assert.notNull(sqlBuilder, "SqlBuilder must not be null");
        Assert.notNull(mappingContext, "MappingContext must not be null");
        Assert.notNull(jdbcConverter, "JdbcConverter must not be null");
        Assert.notNull(conversionService, "ConversionService must not be null");

        @SuppressWarnings("unchecked")
        RelationalPersistentEntity<T> entity =
                (RelationalPersistentEntity<T>) mappingContext.getRequiredPersistentEntity(getEntityType());
        this.entity = entity;
    }

    @Override
    public Page<T> findAll(SearchParams params) {
        Assert.notNull(params, "SearchParams must not be null!");

        // Create specification with criteria
        JdbcSpecification<T> spec = new JdbcSpecification<>();
        spec.withCriteria(convertToRequestParams(params));

        // Build SQL query
        Pageable pageable = createPageable(params);
        String sql = sqlBuilder.buildSelectSql(entity, spec, pageable);

        // Execute query
        List<T> results =
                jdbcTemplate.query(sql, spec.getParameterSource(), new EntityRowMapper<>(entity, jdbcConverter));

        // Get total count for pagination
        long total = count(params);
        return new PageImpl<>(results, pageable, total);
    }

    @Override
    public Stream<T> findAllAsStream(SearchParams params) {
        Assert.notNull(params, "SearchParams must not be null!");

        JdbcSpecification<T> spec = new JdbcSpecification<>();
        spec.withCriteria(convertToRequestParams(params));

        SearchParams streamParams = SearchParams.builder()
                .filters(params.getFilters())
                .sortField(params.getSortField())
                .sortDirection(params.getSortDirection())
                .unpaged(true)
                .build();

        Pageable pageable = createPageable(streamParams);

        String sql = sqlBuilder.buildSelectSql(entity, spec, pageable);

        return jdbcTemplate.queryForStream(
                sql, spec.getParameterSource(), new EntityRowMapper<>(entity, jdbcConverter));
    }

    @Override
    public Optional<T> findOne(SearchParams params) {
        Assert.notNull(params, "SearchParams must not be null!");

        params.setPage(0);
        params.setSize(1);

        List<T> results = findAll(params).getContent();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public long count(SearchParams params) {
        Assert.notNull(params, "SearchParams must not be null!");

        JdbcSpecification<T> spec = new JdbcSpecification<>();
        spec.withCriteria(convertToRequestParams(params));

        String sql = sqlBuilder.buildCountSql(entity, spec);

        return jdbcTemplate.queryForObject(sql, spec.getParameterSource(), Long.class);
    }

    @Override
    public <P> Page<P> findAllProjectedBy(SearchParams params, Class<P> projectionType) {
        Assert.notNull(params, "SearchParams must not be null!");
        Assert.notNull(projectionType, "Projection type must not be null!");

        // Create specification with criteria
        JdbcSpecification<T> spec = new JdbcSpecification<>();
        spec.withCriteria(convertToRequestParams(params));

        Pageable pageable = createPageable(params);
        String sql = sqlBuilder.buildProjectionSql(entity, spec, projectionType, pageable);

        // Use the parameter source from the specification
        List<P> results = jdbcTemplate.query(
                sql, spec.getParameterSource(), createProjectionMapper(projectionType, new HashMap<>()));

        long total = count(params);
        return new PageImpl<>(results, pageable, total);
    }

    @Override
    public <P> Stream<P> findAllProjectedByAsStream(SearchParams params, Class<P> projectionType) {
        Assert.notNull(params, "SearchParams must not be null!");
        Assert.notNull(projectionType, "Projection type must not be null!");

        JdbcSpecification<T> spec = new JdbcSpecification<>();
        spec.withCriteria(convertToRequestParams(params));

        SearchParams streamParams = SearchParams.builder()
                .filters(params.getFilters())
                .sortField(params.getSortField())
                .sortDirection(params.getSortDirection())
                .unpaged(true)
                .build();

        Pageable pageable = createPageable(streamParams);

        String sql = sqlBuilder.buildProjectionSql(entity, spec, projectionType, pageable);

        return jdbcTemplate.queryForStream(
                sql, spec.getParameterSource(), createProjectionMapper(projectionType, new HashMap<>()));
    }

    @Override
    public <P> Optional<P> findOneProjectedBy(SearchParams params, Class<P> projectionType) {
        Assert.notNull(params, "SearchParams must not be null!");
        Assert.notNull(projectionType, "Projection type must not be null!");

        params.setPage(0);
        params.setSize(1);

        List<P> results = findAllProjectedBy(params, projectionType).getContent();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    protected <P> RowMapper<P> createProjectionMapper(
            final Class<P> projectionType, final Map<String, List<Map<String, Object>>> collectionCache) {
        ProjectionHelper helper = new ProjectionHelper(
                jdbcTemplate, projectionFactory, mappingContext, conversionService, propertyResolver, collectionCache);

        return (rs, rowNum) -> {
            try {
                Map<String, Object> propertyValues = helper.processProjectionRow(rs, projectionType, entity);
                return projectionFactory.createProjection(projectionType, propertyValues);
            } catch (SQLException e) {
                throw new RuntimeException("Error creating projection", e);
            }
        };
    }

    private List<FilterCriteria> convertToRequestParams(SearchParams params) {
        if (params.getFilters() == null) {
            return List.of();
        }

        return params.getFilters().stream()
                .filter(criteria -> criteria.isCompound() || criteria.getValue() != null || criteria.isNullOperation())
                .toList();
    }

    protected Pageable createPageable(SearchParams params) {
        Sort sort = params.getSortField() != null
                ? Sort.by(
                        params.getSortDirection() != null ? params.getSortDirection() : SearchParams.DEFAULT_DIRECTION,
                        params.getSortField())
                : Sort.unsorted();

        if (params.isUnpaged()) {
            return sort.isSorted() ? Pageable.unpaged(sort) : Pageable.unpaged();
        }

        return PageRequest.of(
                ObjectUtils.defaultIfNull(params.getPage(), SearchParams.DEFAULT_PAGE),
                ObjectUtils.defaultIfNull(params.getSize(), SearchParams.DEFAULT_SIZE),
                sort);
    }

    protected Class<T> getEntityType() {
        return entityClass;
    }

    // CRUD implementation
    @Override
    public <S extends T> S save(S entity) {
        Assert.notNull(entity, "Entity must not be null");
        return aggregateTemplate.save(entity);
    }

    @Override
    public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
        Assert.notNull(entities, "Entities must not be null");
        List<S> result = new ArrayList<>();
        entities.forEach(entity -> result.add(save(entity)));
        return result;
    }

    @Override
    public Optional<T> findById(ID id) {
        Assert.notNull(id, "Id must not be null");
        return Optional.ofNullable(aggregateTemplate.findById(id, entity.getType()));
    }

    @Override
    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }

    @Override
    public Iterable<T> findAll() {
        return aggregateTemplate.findAll(entity.getType());
    }

    @Override
    public Iterable<T> findAllById(Iterable<ID> ids) {
        Assert.notNull(ids, "Ids must not be null");
        List<T> result = new ArrayList<>();
        ids.forEach(id -> findById(id).ifPresent(result::add));
        return result;
    }

    @Override
    public long count() {
        return aggregateTemplate.count(entity.getType());
    }

    @Override
    public void deleteById(ID id) {
        Assert.notNull(id, "Id must not be null");
        findById(id).ifPresent(this::delete);
    }

    @Override
    public void delete(T entity) {
        Assert.notNull(entity, "Entity must not be null");
        aggregateTemplate.delete(entity);
    }

    @Override
    public void deleteAllById(Iterable<? extends ID> ids) {
        Assert.notNull(ids, "Ids must not be null");
        ids.forEach(this::deleteById);
    }

    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        Assert.notNull(entities, "Entities must not be null");
        entities.forEach(this::delete);
    }

    @Override
    public void deleteAll() {
        Iterable<T> all = findAll();
        all.forEach(this::delete);
    }
}
