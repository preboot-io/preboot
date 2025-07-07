package io.preboot.query.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.preboot.exporters.api.DataExporter;
import io.preboot.query.FilterableUuidRepository;
import io.preboot.query.HasUuid;
import io.preboot.query.web.spi.QueryControllersPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Base controller providing CRUD operations on top of UuidFilterableController.
 *
 * @param <T> Entity type that must implement HasUuid
 */
public abstract class CrudUuidFilterableController<T extends HasUuid, ID> extends UuidFilterableController<T, ID> {

    private final FilterableUuidRepository<T, ID> repository;
    private final ObjectMapper objectMapper;

    protected CrudUuidFilterableController(FilterableUuidRepository<T, ID> repository) {
        this(repository, false, Collections.emptyList());
    }

    protected CrudUuidFilterableController(
            FilterableUuidRepository<T, ID> repository, boolean supportsProjections, List<DataExporter> dataExporters) {
        this(repository, supportsProjections, dataExporters, null);
    }

    protected CrudUuidFilterableController(
            FilterableUuidRepository<T, ID> repository,
            boolean supportsProjections,
            List<DataExporter> dataExporters,
            QueryControllersPort controllersPort) {
        super(repository, supportsProjections, dataExporters, controllersPort);
        this.repository = repository;
        this.objectMapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(new JavaTimeModule()) // Add support for Java 8 date/time types
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    // CREATE
    @Operation(summary = "Create a new entity")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public T create(@RequestBody @Valid T entity) {
        validateCreate(entity);
        beforeCreate(entity);
        if (entity.getUuid() == null) {
            entity.setUuid(UUID.randomUUID());
        }
        T savedEntity = repository.save(entity);
        afterCreate(savedEntity);
        return savedEntity;
    }

    // UPDATE
    @Operation(
            summary = "Update an existing entity",
            parameters = {@Parameter(name = "uuid", in = ParameterIn.PATH, required = true, description = "Entity UUID")
            })
    @PutMapping("/{uuid}")
    public ResponseEntity<T> update(@PathVariable("uuid") UUID uuid, @RequestBody @Valid T entity) {
        if (!repository.existsByUuid(uuid)) {
            return ResponseEntity.notFound().build();
        }

        entity.setUuid(uuid);
        validateUpdate(uuid, entity);
        beforeUpdate(uuid, entity);
        T savedEntity = repository.save(entity);
        afterUpdate(uuid, savedEntity);
        return ResponseEntity.ok(savedEntity);
    }

    // PATCH
    @Operation(
            summary = "Partially update an entity",
            parameters = {@Parameter(name = "uuid", in = ParameterIn.PATH, required = true, description = "Entity UUID")
            })
    @PatchMapping("/{uuid}")
    public ResponseEntity<T> patch(@PathVariable("uuid") UUID uuid, @RequestBody T partialEntity) {
        return repository
                .findByUuid(uuid)
                .map(existingEntity -> {
                    validatePatch(uuid, partialEntity);
                    beforePatch(uuid, existingEntity, partialEntity);
                    T mergedEntity = merge(existingEntity, partialEntity);
                    T savedEntity = repository.save(mergedEntity);
                    afterPatch(uuid, savedEntity);
                    return ResponseEntity.ok(savedEntity);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE
    @Operation(
            summary = "Delete entity by UUID",
            parameters = {@Parameter(name = "uuid", in = ParameterIn.PATH, required = true, description = "Entity UUID")
            })
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> delete(@PathVariable("uuid") UUID uuid) {
        if (!repository.existsByUuid(uuid)) {
            return ResponseEntity.notFound().build();
        }

        validateDelete(uuid);
        beforeDelete(uuid);
        repository.deleteByUuid(uuid);
        afterDelete(uuid);
        return ResponseEntity.noContent().build();
    }

    // Hook methods for CRUD operations
    protected void validateCreate(T entity) {}

    protected void beforeCreate(T entity) {}

    protected void afterCreate(T entity) {}

    protected void validateUpdate(UUID uuid, T entity) {}

    protected void beforeUpdate(UUID uuid, T entity) {}

    protected void afterUpdate(UUID uuid, T entity) {}

    protected void validatePatch(UUID uuid, T partialEntity) {}

    protected void beforePatch(UUID uuid, T existingEntity, T partialEntity) {}

    protected void afterPatch(UUID uuid, T entity) {}

    protected void validateDelete(UUID uuid) {}

    protected void beforeDelete(UUID uuid) {}

    protected void afterDelete(UUID uuid) {}

    /**
     * Merge the partial entity into the existing entity. Uses Jackson's ObjectMapper to perform a deep merge of
     * non-null properties. Override this method to implement custom merging logic if needed.
     */
    protected T merge(T existingEntity, T partialEntity) {
        try {
            // Convert existing entity to a tree
            JsonNode existingNode = objectMapper.valueToTree(existingEntity);

            // Convert partial update to a tree
            JsonNode partialNode = objectMapper.valueToTree(partialEntity);

            // Perform deep merge
            JsonNode merged = merge(existingNode, partialNode);

            // Convert back to entity
            return objectMapper.treeToValue(merged, (Class<T>) existingEntity.getClass());
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Error merging entities: " + e.getMessage());
        }
    }

    /** Recursively merge two JsonNodes, updating only non-null values from the update node. */
    private JsonNode merge(JsonNode mainNode, JsonNode updateNode) {
        Iterator<String> fieldNames = updateNode.fieldNames();

        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode jsonNode = mainNode.get(fieldName);
            JsonNode updateValue = updateNode.get(fieldName);

            // Skip null update values
            if (updateValue.isNull()) {
                continue;
            }

            // If this is an object and both nodes have the field, recursive merge
            if (jsonNode != null && jsonNode.isObject() && updateValue.isObject()) {
                ((ObjectNode) mainNode).replace(fieldName, merge(jsonNode, updateValue));
            }
            // If it's an array, replace the entire array
            else if (updateValue.isArray()) {
                ((ObjectNode) mainNode).replace(fieldName, updateValue);
            }
            // For all other cases, just update the value if it's not null
            else if (!updateValue.isNull()) {
                ((ObjectNode) mainNode).replace(fieldName, updateValue);
            }
        }

        return mainNode;
    }
}
