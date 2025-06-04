package io.preboot.query;

import io.preboot.query.exception.InvalidFilterCriteriaException;
import io.preboot.query.exception.PropertyNotFoundException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
@Slf4j
public class SqlBuilder {
    private final PropertyResolver propertyResolver;
    private final JoinResolver joinResolver;
    private final RelationalMappingContext mappingContext;

    public String buildSelectSql(RelationalPersistentEntity<?> entity, JdbcSpecification<?> spec, Pageable pageable) {
        Assert.notNull(entity, "Entity must not be null");
        Assert.notNull(spec, "Specification must not be null");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT \"base\".* FROM \"")
                .append(entity.getTableName().getReference())
                .append("\" \"base\"");

        Map<String, JoinInfo> joins = joinResolver.analyzeJoins(entity, spec.getFilterCriteria());
        appendJoins(sql, entity, joins);
        appendWhere(sql, entity, spec, joins);
        appendOrderBy(sql, entity, pageable, joins);
        appendPagination(sql, pageable);

        log.debug("Generated SQL: {}", sql);

        return sql.toString();
    }

    public String buildCountSql(RelationalPersistentEntity<?> entity, JdbcSpecification<?> spec) {
        Assert.notNull(entity, "Entity must not be null");
        Assert.notNull(spec, "Specification must not be null");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(DISTINCT \"base\".\"id\") FROM \"")
                .append(entity.getTableName().getReference())
                .append("\" \"base\"");

        Map<String, JoinInfo> joins = joinResolver.analyzeJoins(entity, spec.getFilterCriteria());
        appendJoins(sql, entity, joins);
        appendWhere(sql, entity, spec, joins);

        return sql.toString();
    }

    private void appendWhere(
            StringBuilder sql,
            RelationalPersistentEntity<?> entity,
            JdbcSpecification<?> spec,
            Map<String, JoinInfo> joins) {
        if (spec.hasCriteria() && spec.getExpression() != null) {
            SqlContext context = new SqlContext(entity, joins, propertyResolver, mappingContext, 0);
            sql.append(" WHERE ").append(spec.getExpression().toSql(context));
            spec.getExpression().addParameters(spec.getParameterSource());
        }
    }

    private void appendJoins(StringBuilder sql, RelationalPersistentEntity<?> entity, Map<String, JoinInfo> joins) {
        joins.forEach((path, joinInfo) -> {
            sql.append(" LEFT JOIN \"")
                    .append(joinInfo.targetTable())
                    .append("\" \"")
                    .append(joinInfo.alias())
                    .append("\" ON \"");

            if (joinInfo.isAggregateReference()) {
                // Aggregate reference join: base.source_column = joined.target_column
                sql.append("base")
                        .append("\".\"")
                        .append(joinInfo.sourceColumn())
                        .append("\" = \"")
                        .append(joinInfo.alias())
                        .append("\".\"")
                        .append(joinInfo.targetColumn())
                        .append("\"");
            } else {
                // Collection join: joined.foreign_key = base.id
                sql.append(joinInfo.alias())
                        .append("\".\"")
                        .append(joinInfo.sourceColumn())
                        .append("\" = \"base\".\"")
                        .append(joinInfo.targetColumn())
                        .append("\"");

                // Check if this is a collection with aggregate reference
                RelationalPersistentProperty collectionProperty = entity.getPersistentProperty(path);
                if (collectionProperty != null && collectionProperty.isCollectionLike()) {
                    RelationalPersistentEntity<?> collectionEntity =
                            mappingContext.getRequiredPersistentEntity(collectionProperty.getActualType());

                    for (RelationalPersistentProperty prop : collectionEntity) {
                        AggregateReference ref = prop.findAnnotation(AggregateReference.class);
                        if (ref != null) {
                            // Add join for referenced table
                            RelationalPersistentEntity<?> targetEntity =
                                    mappingContext.getRequiredPersistentEntity(ref.target());
                            sql.append(" LEFT JOIN \"")
                                    .append(targetEntity.getTableName().getReference())
                                    .append("\" \"")
                                    .append(ref.alias())
                                    .append("\" ON \"")
                                    .append(joinInfo.alias())
                                    .append("\".\"")
                                    .append(ref.sourceColumn())
                                    .append("\" = \"")
                                    .append(ref.alias())
                                    .append("\".\"")
                                    .append(ref.targetColumn())
                                    .append("\"");
                        }
                    }
                }
            }
        });
    }

    private void appendOrderBy(
            StringBuilder sql, RelationalPersistentEntity<?> entity, Pageable pageable, Map<String, JoinInfo> joins) {
        if (pageable != null && pageable.getSort().isSorted()) {
            sql.append(" ORDER BY ");
            List<String> orderClauses = new ArrayList<>();

            pageable.getSort().forEach(order -> {
                String columnRef = buildOrderByColumnReference(entity, order.getProperty(), joins);
                orderClauses.add(columnRef + " " + order.getDirection().name());
            });

            sql.append(String.join(", ", orderClauses));
        }
    }

    private String buildOrderByColumnReference(
            RelationalPersistentEntity<?> entity, String property, Map<String, JoinInfo> joins) {
        if (propertyResolver.isNestedProperty(property)) {
            String[] parts = property.split("\\.");
            String collectionName = parts[0];
            String fieldName = parts[1];

            JoinInfo joinInfo = joins.get(collectionName);
            if (joinInfo != null) {
                RelationalPersistentEntity<?> nestedEntity = propertyResolver.getNestedEntity(entity, collectionName);
                RelationalPersistentProperty nestedProperty = nestedEntity.getPersistentProperty(fieldName);

                return buildColumnReference(joinInfo.alias(), nestedProperty);
            }
            throw new IllegalArgumentException("Invalid sort property: " + property);
        } else {
            RelationalPersistentProperty entityProperty = entity.getPersistentProperty(property);
            if (entityProperty != null) {
                return buildColumnReference("base", entityProperty);
            }
            throw new IllegalArgumentException("Invalid sort property: " + property);
        }
    }

    private void appendPagination(StringBuilder sql, Pageable pageable) {
        if (pageable != null && pageable.isPaged()) {
            sql.append(" LIMIT ")
                    .append(pageable.getPageSize())
                    .append(" OFFSET ")
                    .append(pageable.getOffset());
        }
    }

    private String buildColumnReference(String tableAlias, RelationalPersistentProperty property) {
        return String.format(
                "\"%s\".\"%s\"", tableAlias, property.getColumnName().getReference());
    }

    // projections
    public String buildProjectionSql(
            RelationalPersistentEntity<?> entity,
            JdbcSpecification<?> spec,
            Class<?> projectionType,
            Pageable pageable) {
        Assert.notNull(entity, "Entity must not be null");
        Assert.notNull(spec, "Specification must not be null");
        Assert.notNull(projectionType, "Projection type must not be null");

        log.debug(
                "Building projection SQL for entity: {}, projection: {}",
                entity.getType().getSimpleName(),
                projectionType.getName());

        StringBuilder sql = new StringBuilder();
        List<String> columns = getProjectionColumns(entity, projectionType);

        // Add any columns used in ORDER BY if not already present
        if (pageable != null && pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                String columnRef = buildOrderByColumnReference(entity, order.getProperty(), Map.of());
                // Add sortable column to projection if not already there
                String columnName = order.getProperty();
                if (!columns.stream().anyMatch(col -> col.contains(" as \"" + columnName + "\""))) {
                    columns.add(columnRef + " as \"" + columnName + "\"");
                }
            });
        }

        sql.append("SELECT DISTINCT ");
        sql.append(String.join(", ", columns));
        sql.append(" FROM \"").append(entity.getTableName().getReference()).append("\" \"base\"");

        // Get required joins from both filter criteria and projections
        Set<String> requiredJoins = new HashSet<>();

        // Add joins from filter criteria
        if (spec.getFilterCriteria() != null) {
            spec.getFilterCriteria().stream()
                    .flatMap(criterion -> {
                        if (criterion.isCompound()) {
                            return criterion.getChildren().stream();
                        }
                        return Stream.of(criterion);
                    })
                    .map(FilterCriteria::getField)
                    .filter(field -> field != null && propertyResolver.isNestedProperty(field))
                    .map(field -> field.split("\\.")[0])
                    .forEach(requiredJoins::add);
        }

        // Add joins from projection methods
        for (Method method : projectionType.getMethods()) {
            Value valueAnn = method.getAnnotation(Value.class);
            if (valueAnn != null) {
                String spelExpression = valueAnn.value();
                extractPropertyPaths(spelExpression).stream()
                        .filter(path -> path.contains("."))
                        .map(path -> path.split("\\.")[0])
                        .forEach(requiredJoins::add);
            }
        }

        // Create join info for all required joins
        Map<String, JoinInfo> joins = requiredJoins.stream()
                .filter(prefix -> !prefix.contains("+")
                        && !prefix.contains("-")
                        && !prefix.contains("*")
                        && !prefix.contains("/"))
                .filter(prefix -> {
                    try {
                        // Verify this is a valid property or reference before creating join
                        return entity.getPersistentProperty(prefix) != null
                                || propertyResolver.findPropertyByReferenceAlias(entity, prefix) != null;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toMap(
                        prefix -> prefix,
                        prefix -> createJoinInfo(entity, prefix),
                        (existing, replacement) -> existing));

        appendJoins(sql, entity, joins);
        appendWhere(sql, entity, spec, joins);
        appendOrderBy(sql, entity, pageable, joins);
        appendPagination(sql, pageable);

        log.debug("Generated SQL: {}", sql);

        return sql.toString();
    }

    private Set<String> extractPropertyPaths(String spelExpression) {
        Set<String> paths = new HashSet<>();
        if (spelExpression == null || !spelExpression.contains("target.")) {
            return paths;
        }

        // Remove the SpEL wrapper
        String expr = spelExpression.replace("#{", "").replace("}", "");

        // Split on common operators while preserving the parts
        String[] parts = expr.split("\\s*[+\\-*/]\\s*");

        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("target.")) {
                String propertyPath = part.substring("target.".length());
                // Only add if it looks like a valid property path
                if (!propertyPath.contains(" ") && !propertyPath.contains("(")) {
                    paths.add(propertyPath);
                }
            }
        }

        return paths;
    }

    private JoinInfo createJoinInfo(RelationalPersistentEntity<?> entity, String prefix) {
        // First try to find property directly
        RelationalPersistentProperty property = entity.getPersistentProperty(prefix);

        // If not found directly, try to find by reference alias
        if (property == null) {
            property = propertyResolver.findPropertyByReferenceAlias(entity, prefix);
        }

        if (property == null) {
            throw new PropertyNotFoundException("Property not found: " + prefix);
        }

        if (property.isCollectionLike()) {
            // Collection case
            RelationalPersistentEntity<?> nestedEntity =
                    mappingContext.getRequiredPersistentEntity(property.getActualType());
            String tableName = nestedEntity.getTableName().getReference();
            String alias = prefix + "_table";
            String foreignKeyColumn = property.getReverseColumnName(entity).getReference();
            return JoinInfo.forCollection(tableName, alias, foreignKeyColumn);
        } else {
            // Aggregate reference case
            AggregateReference reference = property.findAnnotation(AggregateReference.class);
            if (reference == null) {
                throw new InvalidFilterCriteriaException(
                        prefix, "reference", "Property is neither a collection nor an aggregate reference");
            }

            RelationalPersistentEntity<?> targetEntity = mappingContext.getRequiredPersistentEntity(reference.target());

            // Use the column name for joining
            String sourceColumn = property.getColumnName().getReference();

            return JoinInfo.forAggregateReference(
                    targetEntity.getTableName().getReference(),
                    reference.alias(),
                    sourceColumn,
                    reference.targetColumn());
        }
    }

    private List<String> getProjectionColumns(RelationalPersistentEntity<?> entity, Class<?> projectionType) {
        List<String> columns = new ArrayList<>();
        Set<String> processedColumns = new HashSet<>();
        Set<String> collectionProperties = new HashSet<>();
        Set<String> referenceProperties = new HashSet<>();

        log.debug("Getting projection columns for {}", projectionType.getName());

        // First pass: Find all reference paths
        for (Method method : projectionType.getMethods()) {
            Value valueAnn = method.getAnnotation(Value.class);
            if (valueAnn != null) {
                String spelExpr = valueAnn.value();
                log.debug("Analyzing SpEL expression: {}", spelExpr);
                if (spelExpr.startsWith("#{target.")) {
                    String propPath = spelExpr.replace("#{target.", "").replace("}", "");
                    String[] parts = propPath.split("\\.");

                    if (parts.length > 1) {
                        String prefix = parts[0];
                        // Skip arithmetic expressions
                        if (!prefix.contains("+")
                                && !prefix.contains("-")
                                && !prefix.contains("*")
                                && !prefix.contains("/")
                                && !prefix.contains("?")
                                && !prefix.contains(">")
                                && !prefix.contains("<")) {

                            RelationalPersistentProperty property = entity.getPersistentProperty(prefix);
                            if (property == null) {
                                property = propertyResolver.findPropertyByReferenceAlias(entity, prefix);
                            }

                            if (property != null) {
                                if (property.isCollectionLike()) {
                                    log.debug("Found collection property: {}", prefix);
                                    collectionProperties.add(prefix);
                                } else if (property.findAnnotation(AggregateReference.class) != null) {
                                    log.debug("Found aggregate reference: {}", prefix);
                                    referenceProperties.add(prefix);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add base properties
        for (Method method : projectionType.getMethods()) {
            if (isGetter(method) && method.getAnnotation(Value.class) == null) {
                String propertyName = getPropertyNameFromGetter(method);
                try {
                    RelationalPersistentProperty property = propertyResolver.getPropertyByPath(entity, propertyName);
                    if (property != null && !property.isCollectionLike()) {
                        String columnRef = buildColumnReference("base", property);
                        String columnAlias = toSnakeCase(propertyName);
                        if (!processedColumns.contains(columnAlias)) {
                            columns.add(columnRef + " as \"" + columnAlias + "\"");
                            processedColumns.add(columnAlias);
                            log.debug("Added base column: {}", columnRef);
                        }
                    }
                } catch (PropertyNotFoundException e) {
                    continue;
                }
            }
        }

        // Add all reference properties
        for (String prefix : referenceProperties) {
            RelationalPersistentProperty property = entity.getPersistentProperty(prefix);
            if (property == null) {
                property = propertyResolver.findPropertyByReferenceAlias(entity, prefix);
            }

            if (property != null) {
                AggregateReference reference = property.findAnnotation(AggregateReference.class);
                if (reference != null) {
                    RelationalPersistentEntity<?> targetEntity =
                            mappingContext.getRequiredPersistentEntity(reference.target());

                    log.debug(
                            "Processing aggregate reference: {} -> {}",
                            prefix,
                            targetEntity.getType().getSimpleName());

                    for (RelationalPersistentProperty targetProp : targetEntity) {
                        String columnRef = String.format(
                                "\"%s\".\"%s\"",
                                reference.alias(), targetProp.getColumnName().getReference());
                        String columnAlias = reference.alias() + "_"
                                + targetProp.getColumnName().getReference();

                        if (!processedColumns.contains(columnAlias)) {
                            columns.add(columnRef + " as \"" + columnAlias + "\"");
                            processedColumns.add(columnAlias);
                            log.debug("Added reference column: {}", columnRef);
                        }
                    }
                }
            }
        }

        // If we have collections, we need the base ID
        if (!collectionProperties.isEmpty() && !processedColumns.contains("id")) {
            columns.add("\"base\".\"id\" as \"id\"");
            log.debug("Added ID column for collections");
        }

        log.debug("Final column list: {}", columns);
        return columns;
    }

    private boolean isGetter(Method method) {
        String name = method.getName();
        return (name.startsWith("get") || name.startsWith("is"))
                && method.getParameterCount() == 0
                && !method.getReturnType().equals(void.class)
                && !name.equals("getClass");
    }

    private String getPropertyNameFromGetter(Method getter) {
        String name = getter.getName();
        if (name.startsWith("get")) {
            name = name.substring(3);
        } else if (name.startsWith("is")) {
            name = name.substring(2);
        }
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    private String toSnakeCase(String input) {
        if (input == null) return null;
        String regex = "([a-z])([A-Z])";
        String replacement = "$1_$2";
        return input.replaceAll(regex, replacement).toLowerCase();
    }
}
