package io.preboot.query;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Slf4j
class ProjectionHelper {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ProjectionFactory projectionFactory;
    private final RelationalMappingContext mappingContext;
    private final ConversionService conversionService;
    private final PropertyResolver propertyResolver;
    private final Map<String, List<Map<String, Object>>> collectionCache;

    ProjectionHelper(
            final NamedParameterJdbcTemplate jdbcTemplate,
            final ProjectionFactory projectionFactory,
            final RelationalMappingContext mappingContext,
            final ConversionService conversionService,
            final PropertyResolver propertyResolver,
            final Map<String, List<Map<String, Object>>> collectionCache) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectionFactory = projectionFactory;
        this.mappingContext = mappingContext;
        this.conversionService = conversionService;
        this.propertyResolver = propertyResolver;
        this.collectionCache = collectionCache;
    }

    Map<String, Object> processProjectionRow(
            ResultSet rs, Class<?> projectionType, RelationalPersistentEntity<?> entity) throws SQLException {
        log.debug("Starting projection mapping for type: {}", projectionType.getName());

        Map<String, Object> propertyValues = new HashMap<>();
        processDirectProperties(rs, projectionType, propertyValues);
        processSpELExpressions(rs, projectionType, entity, propertyValues);

        log.debug("Final property values map: {}", propertyValues);
        return propertyValues;
    }

    private void processDirectProperties(ResultSet rs, Class<?> projectionType, Map<String, Object> propertyValues)
            throws SQLException {
        log.debug("Starting direct property processing");
        for (Method method : projectionType.getMethods()) {
            if (isGetter(method) && method.getAnnotation(Value.class) == null) {
                String propertyName = getPropertyNameFromGetter(method);
                try {
                    String columnName = toSnakeCase(propertyName);
                    Object value = getValueFromResultSet(rs, columnName, method.getReturnType());
                    log.debug("Direct property mapping: {} -> {} = {}", propertyName, columnName, value);
                    if (value != null) {
                        propertyValues.put(propertyName, value);
                    }
                } catch (SQLException e) {
                    log.error("Failed to get column {}: {}", propertyName, e.getMessage());
                    throw e;
                }
            }
        }
    }

    private void processSpELExpressions(
            ResultSet rs,
            Class<?> projectionType,
            RelationalPersistentEntity<?> entity,
            Map<String, Object> propertyValues)
            throws SQLException {
        log.debug("Starting SpEL expression processing");
        for (Method method : projectionType.getMethods()) {
            Value valueAnn = method.getAnnotation(Value.class);
            if (valueAnn != null) {
                String spelExpr = valueAnn.value();
                log.debug("Processing SpEL expression method: {} with expression: {}", method.getName(), spelExpr);

                if (spelExpr.startsWith("#{target.")) {
                    String propPath = spelExpr.replace("#{target.", "").replace("}", "");
                    String[] parts = propPath.split("\\.");

                    if (parts.length > 0) {
                        String prefix = parts[0];
                        log.debug("Processing property path prefix: {}", prefix);

                        // Skip arithmetic expressions
                        if (!prefix.contains("+")
                                && !prefix.contains("-")
                                && !prefix.contains("*")
                                && !prefix.contains("/")
                                && !prefix.contains("?")
                                && !prefix.contains(">")
                                && !prefix.contains("<")) {

                            log.debug("Looking up property: {}", prefix);
                            RelationalPersistentProperty property = entity.getPersistentProperty(prefix);
                            if (property == null) {
                                log.debug("Property not found directly, trying by reference alias: {}", prefix);
                                property = propertyResolver.findPropertyByReferenceAlias(entity, prefix);
                            }

                            if (property != null) {
                                log.debug(
                                        "Found property of type: {}",
                                        property.getType().getSimpleName());

                                if (property.isCollectionLike()) {
                                    log.debug("Processing collection property: {}", prefix);
                                    Long id = rs.getLong("id");
                                    List<Map<String, Object>> items = loadCollectionWithCache(id, property, entity);

                                    if (method.getReturnType().equals(List.class)) {
                                        Class<?> itemType = getCollectionItemType(method);
                                        if (itemType != null && itemType.isInterface()) {
                                            // Check if the item type has any @Value annotations
                                            Method[] itemMethods = itemType.getMethods();
                                            String referenceName = Arrays.stream(itemMethods)
                                                    .map(itemMethod -> itemMethod.getAnnotation(Value.class))
                                                    .filter(Objects::nonNull)
                                                    .map(Value::value)
                                                    .filter(itemSpel -> itemSpel.startsWith("#{target."))
                                                    .map(itemSpel -> itemSpel.replace("#{target.", "")
                                                            .replace("}", "")
                                                            .split("\\."))
                                                    .filter(itemParts -> itemParts.length > 1)
                                                    .findFirst()
                                                    .map(itemParts -> itemParts[0])
                                                    .orElse(null);

                                            // Find the reference name from @Value annotations

                                            if (referenceName != null) {
                                                // Find the AggregateReference info from collection entity
                                                RelationalPersistentEntity<?> collectionEntity =
                                                        mappingContext.getRequiredPersistentEntity(
                                                                property.getActualType());
                                                AggregateReference collectionReference = null;

                                                for (RelationalPersistentProperty prop : collectionEntity) {
                                                    collectionReference = prop.findAnnotation(AggregateReference.class);
                                                    if (collectionReference != null) {
                                                        break;
                                                    }
                                                }

                                                if (collectionReference != null) {
                                                    final AggregateReference finalCollectionReference =
                                                            collectionReference;
                                                    items = items.stream()
                                                            .map(item -> {
                                                                Map<String, Object> restructured = new HashMap<>();
                                                                Map<String, Object> refData = new HashMap<>();

                                                                // Put all values in refData first
                                                                item.forEach((key, value) -> refData.put(key, value));

                                                                // Keep ID and the source column in the root
                                                                restructured.put("id", item.get("id"));
                                                                restructured.put(
                                                                        finalCollectionReference.sourceColumn(),
                                                                        item.get(
                                                                                finalCollectionReference
                                                                                        .sourceColumn()));

                                                                // Add reference data under the alias
                                                                restructured.put(referenceName, refData);

                                                                return restructured;
                                                            })
                                                            .collect(Collectors.toList());
                                                }
                                            }

                                            List<Object> projectedItems = new ArrayList<>();
                                            for (Map<String, Object> item : items) {
                                                Object projection = projectionFactory.createProjection(itemType, item);
                                                projectedItems.add(projection);
                                            }
                                            propertyValues.put(prefix, projectedItems);
                                            log.debug(
                                                    "Added projected collection: {} -> {} items",
                                                    prefix,
                                                    projectedItems.size());
                                        } else {
                                            propertyValues.put(prefix, items);
                                            log.debug("Added raw collection: {} items", items.size());
                                        }
                                    }
                                } else {
                                    // Handle aggregate reference
                                    AggregateReference reference = property.findAnnotation(AggregateReference.class);
                                    if (reference != null) {
                                        log.debug(
                                                "Processing aggregate reference for {} with alias {}",
                                                prefix,
                                                reference.alias());

                                        Map<String, Object> refMap = new HashMap<>();
                                        RelationalPersistentEntity<?> targetEntity =
                                                mappingContext.getRequiredPersistentEntity(reference.target());

                                        for (RelationalPersistentProperty targetProp : targetEntity) {
                                            String columnAlias = reference.alias() + "_"
                                                    + targetProp.getColumnName().getReference();
                                            try {
                                                Object value = rs.getObject(columnAlias);
                                                if (value != null) {
                                                    refMap.put(targetProp.getName(), value);
                                                    log.debug(
                                                            "Added reference value: {} -> {}",
                                                            targetProp.getName(),
                                                            value);
                                                }
                                            } catch (SQLException e) {
                                                log.error("Failed to read column {}: {}", columnAlias, e.getMessage());
                                            }
                                        }

                                        if (!refMap.isEmpty()) {
                                            propertyValues.put(prefix, refMap);
                                            log.debug("Added reference to property values: {} -> {}", prefix, refMap);
                                        }
                                    }
                                }
                            } else {
                                log.error("Property not found by name or alias: {}", prefix);
                            }
                        } else {
                            log.debug("Skipping arithmetic/conditional expression: {}", prefix);
                        }
                    }
                }
            }
        }
    }

    private Class<?> getCollectionItemType(Method method) {
        try {
            // Extract collection item type from generic parameter
            String genericTypeName = method.getGenericReturnType().getTypeName();
            if (genericTypeName.contains("<")) {
                String itemTypeName =
                        genericTypeName.substring(genericTypeName.indexOf("<") + 1, genericTypeName.lastIndexOf(">"));
                return Class.forName(itemTypeName);
            }
        } catch (Exception e) {
            log.error("Failed to get collection item type: {}", e.getMessage());
        }
        return null;
    }

    private List<Map<String, Object>> loadCollectionWithCache(
            Long id, RelationalPersistentProperty property, final RelationalPersistentEntity<?> entity) {
        if (collectionCache != null) {
            final String propertyName = property.getName();
            final String cacheKey = entity.getType().getSimpleName() + "." + propertyName + "." + id;
            log.debug("Loading collection items for property: {} with cache key: {}", propertyName, cacheKey);
            return collectionCache.computeIfAbsent(cacheKey, key -> loadCollection(id, property, entity));
        } else {
            return loadCollection(id, property, entity);
        }
    }

    private List<Map<String, Object>> loadCollection(
            Long id, RelationalPersistentProperty property, final RelationalPersistentEntity<?> entity) {
        log.debug("Loading collection items for property: {}", property.getName());

        RelationalPersistentEntity<?> collectionEntity =
                mappingContext.getRequiredPersistentEntity(property.getActualType());

        // Check if this collection has any AggregateReferences
        AggregateReference reference = null;
        for (RelationalPersistentProperty prop : collectionEntity) {
            reference = prop.findAnnotation(AggregateReference.class);
            if (reference != null) {
                break;
            }
        }

        StringBuilder sql = new StringBuilder();

        // If we have an aggregate reference, join with its table
        if (reference != null) {
            sql.append("SELECT collection.*, ref.* FROM \"")
                    .append(collectionEntity.getTableName().getReference())
                    .append("\" collection");

            RelationalPersistentEntity<?> targetEntity = mappingContext.getRequiredPersistentEntity(reference.target());

            sql.append(" LEFT JOIN \"")
                    .append(targetEntity.getTableName().getReference())
                    .append("\" ref ON collection.\"")
                    .append(reference.sourceColumn())
                    .append("\" = ref.\"")
                    .append(reference.targetColumn())
                    .append("\"");
        } else {
            sql.append("SELECT collection.* FROM \"")
                    .append(collectionEntity.getTableName().getReference())
                    .append("\" collection");
        }

        sql.append(" WHERE collection.\"")
                .append(property.getReverseColumnName(entity).getReference())
                .append("\" = :entityId ORDER BY collection.\"id\"");

        String sqlString = sql.toString();
        log.debug("Collection loading SQL: {}", sqlString);

        var params = new MapSqlParameterSource("entityId", id);
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlString, params);

        log.debug("Loaded {} collection items", results.size());

        // Convert column names from snake_case to camelCase
        return results.stream()
                .map(row -> {
                    Map<String, Object> converted = new HashMap<>();

                    row.forEach((key, value) -> {
                        String camelKey = toCamelCase(key);
                        converted.put(camelKey, value);
                    });

                    return converted;
                })
                .collect(Collectors.toList());
    }

    private String toCamelCase(String snake) {
        if (snake == null) return null;
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;

        for (int i = 0; i < snake.length(); i++) {
            char ch = snake.charAt(i);
            if (ch == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    result.append(Character.toUpperCase(ch));
                    nextUpper = false;
                } else {
                    result.append(Character.toLowerCase(ch));
                }
            }
        }
        return result.toString();
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

    private Object getValueFromResultSet(ResultSet rs, String columnName, Class<?> targetType) throws SQLException {
        Object value = rs.getObject(columnName);
        if (value == null || rs.wasNull()) {
            return null;
        }

        if (!targetType.isInstance(value) && conversionService.canConvert(value.getClass(), targetType)) {
            return conversionService.convert(value, targetType);
        }

        return value;
    }
}
