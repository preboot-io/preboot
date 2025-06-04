package io.preboot.query;

import io.preboot.query.exception.InvalidFilterCriteriaException;
import io.preboot.query.exception.PropertyNotFoundException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Slf4j
@Component
class PropertyResolver {
    private final RelationalMappingContext mappingContext;
    private final ConcurrentMap<PropertyCacheKey, RelationalPersistentProperty> propertyCache;

    PropertyResolver(RelationalMappingContext mappingContext) {
        this.mappingContext = mappingContext;
        this.propertyCache = new ConcurrentHashMap<>();
    }

    public RelationalPersistentProperty getPropertyByPath(RelationalPersistentEntity<?> entity, String path) {
        Assert.notNull(entity, "Entity must not be null!");
        Assert.hasText(path, "Property path must not be empty!");

        PropertyCacheKey cacheKey = new PropertyCacheKey(entity.getType(), path);
        return propertyCache.computeIfAbsent(cacheKey, key -> resolveProperty(entity, path));
    }

    private RelationalPersistentProperty resolveProperty(RelationalPersistentEntity<?> entity, String path) {
        // First check if this is a direct aggregate reference without nesting
        if (!path.contains(".")) {
            RelationalPersistentProperty property = findPropertyByReferenceAlias(entity, path);
            if (property != null && property.findAnnotation(AggregateReference.class) != null) {
                return property;
            }
        }

        String[] parts = path.split("\\.");
        RelationalPersistentEntity<?> currentEntity = entity;
        RelationalPersistentProperty property = null;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            log.error(
                    "Resolving property part: {} for entity: {}",
                    part,
                    currentEntity.getType().getSimpleName());

            // First try to find by reference alias
            property = findPropertyByReferenceAlias(currentEntity, part);

            // If not found by alias, try direct property
            if (property == null) {
                property = currentEntity.getPersistentProperty(part);
            }

            if (property == null) {
                throw new PropertyNotFoundException(path);
            }

            // If this is not the last part, get the next entity
            if (i < parts.length - 1) {
                if (property.isCollectionLike()) {
                    // For collections, get the element type
                    currentEntity = mappingContext.getRequiredPersistentEntity(property.getActualType());
                    log.error(
                            "Navigating collection to type: {}",
                            currentEntity.getType().getSimpleName());
                } else if (property.findAnnotation(AggregateReference.class) != null) {
                    // For aggregate references, get the target type
                    AggregateReference reference = property.findAnnotation(AggregateReference.class);
                    currentEntity = mappingContext.getRequiredPersistentEntity(reference.target());
                    log.error(
                            "Navigating reference to type: {}",
                            currentEntity.getType().getSimpleName());
                    // Clear property as we're now in a new context
                    property = null;
                } else {
                    throw new PropertyNotFoundException("Cannot resolve nested path: " + path);
                }
            }
        }

        // If the last navigation was to an aggregate reference target, we need to find the final property
        if (property == null) {
            String lastPart = parts[parts.length - 1];
            property = currentEntity.getPersistentProperty(lastPart);
            if (property == null) {
                throw new PropertyNotFoundException(path);
            }
        }

        return property;
    }

    public boolean isNestedProperty(String field) {
        Assert.hasText(field, "Field must not be empty!");
        return field.contains(".");
    }

    public RelationalPersistentEntity<?> getNestedEntity(
            RelationalPersistentEntity<?> rootEntity, String collectionName) {
        Assert.notNull(rootEntity, "Root entity must not be null!");
        Assert.hasText(collectionName, "Collection name must not be empty!");

        RelationalPersistentProperty property = rootEntity.getPersistentProperty(collectionName);

        if (property == null) {
            throw new PropertyNotFoundException(collectionName);
        }

        if (!property.isCollectionLike()) {
            throw new InvalidFilterCriteriaException(
                    collectionName, "collection_access", "Property is not a collection");
        }

        return mappingContext.getRequiredPersistentEntity(property.getActualType());
    }

    public RelationalPersistentProperty findPropertyByReferenceAlias(
            RelationalPersistentEntity<?> entity, String alias) {
        for (RelationalPersistentProperty property : entity) {
            AggregateReference reference = property.findAnnotation(AggregateReference.class);
            if (reference != null && reference.alias().equals(alias)) {
                return property;
            }
        }
        return null;
    }

    private record PropertyCacheKey(Class<?> entityType, String propertyPath) {}
}
