package io.preboot.query;

import io.preboot.query.exception.InvalidFilterCriteriaException;
import io.preboot.query.exception.PropertyNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JoinResolver {
    private final PropertyResolver propertyResolver;
    private final RelationalMappingContext mappingContext;

    public Map<String, JoinInfo> analyzeJoins(RelationalPersistentEntity<?> entity, List<FilterCriteria> criteria) {
        // Create a set to collect all required joins
        Set<String> requiredJoins = new HashSet<>();

        // Add joins from filter criteria
        criteria.stream()
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

        // Create JoinInfo for each required join
        return requiredJoins.stream()
                .distinct()
                .collect(Collectors.toMap(
                        prefix -> prefix,
                        prefix -> createJoinInfo(entity, prefix),
                        (existing, replacement) -> existing));
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
            String sourceColumn = reference.sourceColumn().isEmpty()
                    ? property.getColumnName().getReference()
                    : reference.sourceColumn();

            return JoinInfo.forAggregateReference(
                    targetEntity.getTableName().getReference(),
                    reference.alias(),
                    sourceColumn,
                    reference.targetColumn());
        }
    }
}
